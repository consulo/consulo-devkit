/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
import consulo.devkit.localize.DevKitLocalize;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.ex.awt.JBInsets;
import consulo.ui.ex.awt.JBUI;
import jakarta.annotation.Nonnull;
import org.jetbrains.idea.devkit.inspections.quickfix.ConvertToJBInsetsQuickFix;

/**
 * @author Konstantin Bulenkov
 */
@ExtensionImpl
public class UseDPIAwareInsetsInspection extends InternalInspection {
    @Nonnull
    @Override
    public LocalizeValue getDisplayName() {
        return DevKitLocalize.inspectionUseDpiAwareInsetsDisplayName();
    }

    @Override
    public PsiElementVisitor buildInternalVisitor(@Nonnull final ProblemsHolder holder, final boolean isOnTheFly) {
        return new JavaElementVisitor() {
            @Override
            public void visitNewExpression(@Nonnull PsiNewExpression expression) {
                checkNewExpression(holder, expression, isOnTheFly);
                super.visitNewExpression(expression);
            }
        };
    }

    private static void checkNewExpression(ProblemsHolder holder, PsiNewExpression expression, boolean isOnTheFly) {
        Project project = holder.getProject();
        PsiType type = expression.getType();
        PsiExpressionList arguments = expression.getArgumentList();
        if (type != null && arguments != null && type.equalsToText("java.awt.Insets")) {
            if (expression.getParent() instanceof PsiExpressionList expressionList
                && expressionList.getParent() instanceof PsiMethodCallExpression methodCallExpression) {
                PsiType methodType = methodCallExpression.getType();
                if (methodType != null && methodType.equalsToText(JBInsets.class.getName())) {
                    return;
                }
            }
            JavaPsiFacade facade = JavaPsiFacade.getInstance(project);
            PsiResolveHelper resolveHelper = PsiResolveHelper.getInstance(project);
            PsiClass jbuiClass = facade.findClass(JBUI.class.getName(), GlobalSearchScope.allScope(project));
            if (jbuiClass != null && resolveHelper.isAccessible(jbuiClass, expression, jbuiClass)) {
                if (expression.getParent() instanceof PsiExpressionList expressionList
                    && expressionList.getParent() instanceof PsiNewExpression newExpression) {

                    PsiType parentType = newExpression.getType();
                    if (parentType == null || JBInsets.class.getName().equals(parentType.getCanonicalText())) {
                        return;
                    }
                }
                if (arguments.getExpressions().length == 4) {
                    holder.newProblem(DevKitLocalize.inspectionUseDpiAwareInsetsMessage())
                        .range(expression)
                        .withFix(new ConvertToJBInsetsQuickFix())
                        .onTheFly(isOnTheFly)
                        .create();
                }
            }
        }
    }
}