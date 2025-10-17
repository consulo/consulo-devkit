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
import com.intellij.java.language.psi.PsiElementFactory;
import com.intellij.java.language.psi.*;
import com.intellij.java.language.psi.util.PropertyUtil;
import com.intellij.lang.properties.psi.Property;
import com.intellij.lang.properties.references.PropertyReference;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.access.RequiredWriteAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.devkit.localize.DevKitLocalize;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.language.editor.inspection.LocalInspectionToolSession;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.psi.*;
import consulo.language.util.IncorrectOperationException;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.Messages;
import consulo.util.collection.ArrayUtil;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.ReadonlyStatusHandler;
import consulo.virtualFileSystem.VirtualFile;
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

    @Nonnull
    @Override
    public LocalizeValue getGroupDisplayName() {
        return DevKitLocalize.inspectionsGroupName();
    }

    @Nonnull
    @Override
    public LocalizeValue getDisplayName() {
        return DevKitLocalize.inspectionTitleCapitalizationDisplayName();
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
                        holder.newProblem(LocalizeValue.of(
                                "Dialog title '" + titleValue + "' is not properly capitalized. It should have title capitalization"
                            ))
                            .range(args[0])
                            .withFix(new TitleCapitalizationFix(titleValue))
                            .create();
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
                                holder.newProblem(DevKitLocalize.inspectionTitleCapitalizationMessage(titleValue))
                                    .range(args[i])
                                    .withFix(new TitleCapitalizationFix(titleValue))
                                    .create();
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
            for (PsiReference reference : args[0].getReferences()) {
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
        public LocalizeValue getName() {
            return DevKitLocalize.inspectionTitleCapitalizationQuickfixName(myTitleValue);
        }

        @Override
        @RequiredWriteAction
        public final void applyFix(@Nonnull Project project, @Nonnull ProblemDescriptor descriptor) {
            PsiElement problemElement = descriptor.getPsiElement();
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
                Class<? extends TitleCapitalizationFix> aClass = getClass();
                Logger logger = Logger.getInstance(aClass.getName());
                logger.error(e);
            }
        }

        @RequiredWriteAction
        protected void doFix(Project project, PsiElement element) throws IncorrectOperationException {
            if (element instanceof PsiLiteralExpression literalExpression) {
                if (literalExpression.getValue() instanceof String strValue) {
                    PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);
                    PsiExpression newExpression =
                        factory.createExpressionFromText('"' + StringUtil.wordsToBeginFromUpperCase(strValue) + '"', element);
                    literalExpression.replace(newExpression);
                }
            }
            else if (element instanceof PsiMethodCallExpression methodCallExpression) {
                PsiMethod method = methodCallExpression.resolveMethod();
                PsiExpression returnValue = PropertyUtil.getGetterReturnExpression(method);
                if (returnValue != null) {
                    doFix(project, returnValue);
                }
                Property property = getPropertyArgument(methodCallExpression);
                if (property == null) {
                    return;
                }
                String value = property.getUnescapedValue();
                if (value == null) {
                    return;
                }
                String capitalizedString = StringUtil.wordsToBeginFromUpperCase(value);
                property.setValue(capitalizedString);
            }
            else if (element instanceof PsiReferenceExpression referenceExpression
                && referenceExpression.resolve() instanceof PsiVariable variable
                && variable.hasModifierProperty(PsiModifier.FINAL)) {
                doFix(project, variable.getInitializer());
            }
        }

        protected static boolean isQuickFixOnReadOnlyFile(PsiElement problemElement) {
            PsiFile containingPsiFile = problemElement.getContainingFile();
            if (containingPsiFile == null) {
                return false;
            }
            VirtualFile virtualFile = containingPsiFile.getVirtualFile();
            if (virtualFile == null) {
                return false;
            }
            Project project = problemElement.getProject();
            ReadonlyStatusHandler handler = ReadonlyStatusHandler.getInstance(project);
            ReadonlyStatusHandler.OperationStatus status = handler.ensureFilesWritable(virtualFile);
            return status.hasReadonlyFiles();
        }
    }
}
