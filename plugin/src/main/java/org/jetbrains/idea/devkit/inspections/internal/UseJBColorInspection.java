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
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.devkit.localize.DevKitLocalize;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.ex.JBColor;
import jakarta.annotation.Nonnull;
import org.jetbrains.idea.devkit.inspections.quickfix.ConvertToJBColorConstantQuickFix;
import org.jetbrains.idea.devkit.inspections.quickfix.ConvertToJBColorQuickFix;

import java.awt.*;

/**
 * @author Konstantin Bulenkov
 */
@ExtensionImpl
public class UseJBColorInspection extends InternalInspection {
    @Nonnull
    @Override
    public LocalizeValue getDisplayName() {
        return DevKitLocalize.inspectionUseJBColorDisplayName();
    }

    @Nonnull
    @Override
    public String getShortName() {
        return "UseJBColor";
    }

    @Nonnull
    @Override
    public PsiElementVisitor buildInternalVisitor(@Nonnull final ProblemsHolder holder, final boolean isOnTheFly) {
        return new JavaElementVisitor() {
            @Override
            public void visitNewExpression(@Nonnull PsiNewExpression expression) {
                checkNewExpression(holder, expression, isOnTheFly);
                super.visitNewExpression(expression);
            }

            @Override
            @RequiredReadAction
            public void visitReferenceExpression(@Nonnull PsiReferenceExpression expression) {
                super.visitReferenceExpression(expression);
                if (expression.resolve() instanceof PsiField colorField && colorField.isStatic()) {
                    PsiClass colorClass = colorField.getContainingClass();
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
                        holder.newProblem(DevKitLocalize.inspectionUseJBColorMessageConst(text.toUpperCase()))
                            .range((PsiElement)expression)
                            .onTheFly(isOnTheFly)
                            .withFix(new ConvertToJBColorConstantQuickFix(text.toUpperCase()))
                            .create();
                    }
                }
            }
        };
    }

    private static void checkNewExpression(ProblemsHolder holder, PsiNewExpression expression, boolean isOnTheFly) {
        Project project = holder.getProject();
        JavaPsiFacade facade = JavaPsiFacade.getInstance(project);
        PsiClass jbColorClass = facade.findClass(JBColor.class.getName(), GlobalSearchScope.allScope(project));
        PsiType type = expression.getType();
        if (type == null || jbColorClass == null
            || !PsiResolveHelper.getInstance(project).isAccessible(jbColorClass, expression, jbColorClass)) {
            return;
        }

        PsiExpressionList arguments = expression.getArgumentList();
        if (arguments != null && "java.awt.Color".equals(type.getCanonicalText())) {
            if (expression.getParent() instanceof PsiExpressionList expressionList
                && expressionList.getParent() instanceof PsiNewExpression newExpression) {
                PsiType parentType = newExpression.getType();
                if (parentType == null || JBColor.class.getName().equals(parentType.getCanonicalText())) {
                    return;
                }
            }
            holder.newProblem(DevKitLocalize.inspectionUseJBColorMessageNew())
                .range(expression)
                .onTheFly(isOnTheFly)
                .withFix(new ConvertToJBColorQuickFix())
                .create();
        }
    }
}
