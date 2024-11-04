/*
 * Copyright 2000-2011 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.idea.devkit.inspections;

import com.intellij.java.analysis.impl.codeInspection.BaseJavaLocalInspectionTool;
import com.intellij.java.language.psi.*;
import com.intellij.java.language.psi.util.PropertyUtil;
import com.intellij.lang.properties.psi.Property;
import com.intellij.lang.properties.references.PropertyReference;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.access.RequiredWriteAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.language.editor.inspection.*;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiReference;
import consulo.language.psi.ResolveResult;
import consulo.language.util.IncorrectOperationException;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.Messages;
import consulo.util.collection.ArrayUtil;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.ReadonlyStatusHandler;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.annotations.Nls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author yole
 */
@ExtensionImpl
public class TitleCapitalizationInspection extends BaseJavaLocalInspectionTool {
    @Override
    public boolean isEnabledByDefault() {
        return true;
    }

    @Nls
    @Nonnull
    @Override
    public String getGroupDisplayName() {
        return "Plugin DevKit";
    }

    @Nls
    @Nonnull
    @Override
    public String getDisplayName() {
        return "Incorrect dialog title capitalization";
    }

    @Nonnull
    @Override
    public String getShortName() {
        return "DialogTitleCapitalization";
    }

    @Nonnull
    @Override
    public PsiElementVisitor buildVisitorImpl(
        @Nonnull ProblemsHolder holder,
        boolean isOnTheFly,
        LocalInspectionToolSession session,
        Object o
    ) {
        return new JavaElementVisitor() {
            @Override
            @RequiredReadAction
            public void visitMethodCallExpression(@Nonnull PsiMethodCallExpression expression) {
                PsiReferenceExpression methodExpression = expression.getMethodExpression();
                String calledName = methodExpression.getReferenceName();
                if (calledName == null) {
                    return;
                }
                if ("setTitle".equals(calledName)) {
                    if (!isMethodOfClass(expression, DialogWrapper.class.getName(), FileChooserDescriptor.class.getName())) {
                        return;
                    }
                    PsiExpression[] args = expression.getArgumentList().getExpressions();
                    if (args.length == 0) {
                        return;
                    }
                    String titleValue = getTitleValue(args[0]);
                    if (!hasTitleCapitalization(titleValue)) {
                        holder.registerProblem(
                            args[0],
                            "Dialog title '" + titleValue + "' is not properly capitalized. " +
                                "It should have title capitalization",
                            ProblemHighlightType.GENERIC_ERROR_OR_WARNING, new TitleCapitalizationFix(titleValue)
                        );
                    }
                }
                else if (calledName.startsWith("show")
                    && (calledName.endsWith("Dialog") || calledName.endsWith("Message"))) {
                    if (!isMethodOfClass(expression, Messages.class.getName())) {
                        return;
                    }
                    PsiExpression[] args = expression.getArgumentList().getExpressions();
                    PsiMethod psiMethod = expression.resolveMethod();
                    assert psiMethod != null;
                    PsiParameter[] parameters = psiMethod.getParameterList().getParameters();
                    for (int i = 0, parametersLength = parameters.length; i < parametersLength; i++) {
                        PsiParameter parameter = parameters[i];
                        if ("title".equals(parameter.getName()) && i < args.length) {
                            String titleValue = getTitleValue(args[i]);
                            if (!hasTitleCapitalization(titleValue)) {
                                holder.registerProblem(
                                    args[i],
                                    "Message title '" + titleValue + "' is not properly capitalized. " +
                                        "It should have title capitalization",
                                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                                    new TitleCapitalizationFix(titleValue)
                                );
                            }
                            break;
                        }
                    }
                }
            }
        };
    }

    private static boolean isMethodOfClass(PsiMethodCallExpression expression, String... classNames) {
        PsiMethod psiMethod = expression.resolveMethod();
        if (psiMethod == null) {
            return false;
        }
        PsiClass containingClass = psiMethod.getContainingClass();
        if (containingClass == null) {
            return false;
        }
        String name = containingClass.getQualifiedName();
        return ArrayUtil.contains(name, classNames);
    }

    @Nullable
    @RequiredReadAction
    private static String getTitleValue(PsiExpression arg) {
        if (arg instanceof PsiLiteralExpression argLiteralExpr) {
            if (argLiteralExpr.getValue() instanceof String strValue) {
                return strValue;
            }
        }
        if (arg instanceof PsiMethodCallExpression argCallExpr) {
            PsiMethod psiMethod = argCallExpr.resolveMethod();
            PsiExpression returnValue = PropertyUtil.getGetterReturnExpression(psiMethod);
            if (returnValue != null) {
                return getTitleValue(returnValue);
            }
            Property propertyArgument = getPropertyArgument(argCallExpr);
            if (propertyArgument != null) {
                return propertyArgument.getUnescapedValue();
            }
        }
        if (arg instanceof PsiReferenceExpression argRefExpr
            && argRefExpr.resolve() instanceof PsiVariable resultVariable
            && resultVariable.hasModifierProperty(PsiModifier.FINAL)) {
            return getTitleValue(resultVariable.getInitializer());
        }
        return null;
    }

    @Nullable
    private static Property getPropertyArgument(PsiMethodCallExpression arg) {
        PsiExpression[] args = arg.getArgumentList().getExpressions();
        if (args.length > 0) {
            PsiReference[] references = args[0].getReferences();
            for (PsiReference reference : references) {
                if (reference instanceof PropertyReference propertyReference) {
                    ResolveResult[] resolveResults = propertyReference.multiResolve(false);
                    if (resolveResults.length == 1 && resolveResults[0].isValidResult()
                        && resolveResults[0].getElement() instanceof Property property) {
                        return property;
                    }
                }
            }
        }
        return null;
    }

    private static boolean hasTitleCapitalization(String value) {
        if (value == null) {
            return true;
        }
        value = value.replace("&", "");
        return StringUtil.wordsToBeginFromUpperCase(value).equals(value);
    }

    private static class TitleCapitalizationFix implements LocalQuickFix {

        private final String myTitleValue;

        public TitleCapitalizationFix(String titleValue) {
            myTitleValue = titleValue;
        }

        @Nonnull
        @Override
        public String getName() {
            return "Properly capitalize '" + myTitleValue + '\'';
        }

        @Override
        @RequiredWriteAction
        public final void applyFix(@Nonnull Project project, @Nonnull ProblemDescriptor descriptor) {
            final PsiElement problemElement = descriptor.getPsiElement();
            if (problemElement == null || !problemElement.isValid()) {
                return;
            }
            if (isQuickFixOnReadOnlyFile(problemElement)) {
                return;
            }
            try {
                doFix(project, problemElement);
            }
            catch (IncorrectOperationException e) {
                final Class<? extends TitleCapitalizationFix> aClass = getClass();
                final String className = aClass.getName();
                final Logger logger = Logger.getInstance(className);
                logger.error(e);
            }
        }

        @RequiredWriteAction
        protected void doFix(Project project, PsiElement element) throws IncorrectOperationException {
            if (element instanceof PsiLiteralExpression literalExpression) {
                if (literalExpression.getValue() instanceof String strValue) {
                    final PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);
                    final PsiExpression newExpression =
                        factory.createExpressionFromText('"' + StringUtil.wordsToBeginFromUpperCase(strValue) + '"', element);
                    literalExpression.replace(newExpression);
                }
            }
            else if (element instanceof PsiMethodCallExpression methodCallExpression) {
                final PsiMethod method = methodCallExpression.resolveMethod();
                final PsiExpression returnValue = PropertyUtil.getGetterReturnExpression(method);
                if (returnValue != null) {
                    doFix(project, returnValue);
                }
                final Property property = getPropertyArgument(methodCallExpression);
                if (property == null) {
                    return;
                }
                final String value = property.getUnescapedValue();
                if (value == null) {
                    return;
                }
                final String capitalizedString = StringUtil.wordsToBeginFromUpperCase(value);
                property.setValue(capitalizedString);
            }
            else if (element instanceof PsiReferenceExpression referenceExpression) {
                if (referenceExpression.resolve() instanceof PsiVariable variable && variable.hasModifierProperty(PsiModifier.FINAL)) {
                    doFix(project, variable.getInitializer());
                }
            }
        }

        protected static boolean isQuickFixOnReadOnlyFile(PsiElement problemElement) {
            final PsiFile containingPsiFile = problemElement.getContainingFile();
            if (containingPsiFile == null) {
                return false;
            }
            final VirtualFile virtualFile = containingPsiFile.getVirtualFile();
            if (virtualFile == null) {
                return false;
            }
            final Project project = problemElement.getProject();
            final ReadonlyStatusHandler handler = ReadonlyStatusHandler.getInstance(project);
            final ReadonlyStatusHandler.OperationStatus status = handler.ensureFilesWritable(virtualFile);
            return status.hasReadonlyFiles();
        }

        @Nonnull
        @Override
        public String getFamilyName() {
            return "Properly capitalize";
        }
    }
}
