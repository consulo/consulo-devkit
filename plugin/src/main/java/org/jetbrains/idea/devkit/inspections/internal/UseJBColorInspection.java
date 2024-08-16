/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package org.jetbrains.idea.devkit.inspections.internal;

import com.intellij.java.language.psi.*;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.inspection.scheme.InspectionManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.project.Project;
import consulo.ui.ex.JBColor;
import org.jetbrains.annotations.Nls;
import org.jetbrains.idea.devkit.inspections.quickfix.ConvertToJBColorConstantQuickFix;
import org.jetbrains.idea.devkit.inspections.quickfix.ConvertToJBColorQuickFix;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;

/**
 * @author Konstantin Bulenkov
 */
@ExtensionImpl
public class UseJBColorInspection extends InternalInspection {
    @Nonnull
    @Override
    public PsiElementVisitor buildInternalVisitor(@Nonnull final ProblemsHolder holder, final boolean isOnTheFly) {
        return new JavaElementVisitor() {
            @Override
            public void visitNewExpression(PsiNewExpression expression) {
                final ProblemDescriptor descriptor = checkNewExpression(expression, holder.getManager(), isOnTheFly);
                if (descriptor != null) {
                    holder.registerProblem(descriptor);
                }
                super.visitNewExpression(expression);
            }

            @Override
            public void visitReferenceExpression(PsiReferenceExpression expression) {
                super.visitReferenceExpression(expression);
                if (expression.resolve() instanceof PsiField colorField && colorField.hasModifierProperty(PsiModifier.STATIC)) {
                    final PsiClass colorClass = colorField.getContainingClass();
                    if (colorClass != null && Color.class.getName().equals(colorClass.getQualifiedName())) {
                        String text = expression.getText();
                        if (text.contains(".")) {
                            text = text.substring(text.lastIndexOf('.'));
                        }
                        if (text.startsWith(".")) {
                            text = text.substring(1);
                        }
                        if (text.equalsIgnoreCase("lightGray")) {
                            text = "LIGHT_GRAY";
                        }
                        else if (text.equalsIgnoreCase("darkGray")) {
                            text = "DARK_GRAY";
                        }
                        final ProblemDescriptor descriptor = holder.getManager().createProblemDescriptor(
                            expression,
                            "Change to JBColor." + text.toUpperCase(),
                            new ConvertToJBColorConstantQuickFix(text.toUpperCase()),
                            ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                            isOnTheFly
                        );
                        holder.registerProblem(descriptor);
                    }
                }
            }
        };
    }

    @Nullable
    private static ProblemDescriptor checkNewExpression(PsiNewExpression expression, InspectionManager manager, boolean isOnTheFly) {
        final Project project = manager.getProject();
        final JavaPsiFacade facade = JavaPsiFacade.getInstance(project);
        final PsiClass jbColorClass = facade.findClass(JBColor.class.getName(), GlobalSearchScope.allScope(project));
        final PsiType type = expression.getType();
        if (type != null && jbColorClass != null) {
            if (!facade.getResolveHelper().isAccessible(jbColorClass, expression, jbColorClass)) {
                return null;
            }
            final PsiExpressionList arguments = expression.getArgumentList();
            if (arguments != null && "java.awt.Color".equals(type.getCanonicalText())) {
                if (expression.getParent() instanceof PsiExpressionList expressionList
                    && expressionList.getParent() instanceof PsiNewExpression newExpression) {
                    final PsiType parentType = newExpression.getType();
                    if (parentType == null || JBColor.class.getName().equals(parentType.getCanonicalText())) {
                        return null;
                    }
                }
                return manager.createProblemDescriptor(
                    expression,
                    "Replace with JBColor",
                    new ConvertToJBColorQuickFix(),
                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                    isOnTheFly
                );
            }
        }
        return null;
    }

    @Nls
    @Nonnull
    @Override
    public String getDisplayName() {
        return "Use Darcula aware JBColor";
    }

    @Nonnull
    @Override
    public String getShortName() {
        return "UseJBColor";
    }
}
