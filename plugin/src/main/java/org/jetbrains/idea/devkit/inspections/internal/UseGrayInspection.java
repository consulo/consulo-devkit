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

import com.intellij.java.language.impl.psi.impl.JavaConstantExpressionEvaluator;
import com.intellij.java.language.psi.*;
import consulo.annotation.component.ExtensionImpl;
import consulo.devkit.localize.DevKitLocalize;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.ex.Gray;
import consulo.util.lang.NullUtils;
import jakarta.annotation.Nonnull;
import org.jetbrains.idea.devkit.inspections.quickfix.ConvertToGrayQuickFix;

/**
 * @author Konstantin Bulenkov
 */
@ExtensionImpl
public class UseGrayInspection extends InternalInspection {
    @Nonnull
    @Override
    public LocalizeValue getDisplayName() {
        return DevKitLocalize.useGrayInspectionDisplayName();
    }

    @Nonnull
    @Override
    public String getShortName() {
        return "InspectionUsingGrayColors";
    }

    @Nonnull
    @Override
    public PsiElementVisitor buildInternalVisitor(@Nonnull final ProblemsHolder holder, final boolean isOnTheFly) {
        return new JavaElementVisitor() {
            @Override
            public void visitNewExpression(@Nonnull PsiNewExpression expression) {
                checkNewExpression(holder, expression, isOnTheFly);
            }
        };
    }

    private static void checkNewExpression(ProblemsHolder holder, PsiNewExpression expression, boolean isOnTheFly) {
        final Project project = holder.getProject();
        final JavaPsiFacade facade = JavaPsiFacade.getInstance(project);
        final PsiClass grayClass = facade.findClass(Gray.class.getName(), GlobalSearchScope.allScope(project));
        final PsiType type = expression.getType();
        if (type != null && grayClass != null) {
            final PsiExpressionList arguments = expression.getArgumentList();
            if (arguments != null) {
                final PsiExpression[] expressions = arguments.getExpressions();
                if (expressions.length == 3 && "java.awt.Color".equals(type.getCanonicalText())) {
                    if (PsiResolveHelper.getInstance(project).isAccessible(grayClass, expression, grayClass)
                        && expressions[0] instanceof PsiLiteralExpression r
                        && expressions[1] instanceof PsiLiteralExpression g
                        && expressions[2] instanceof PsiLiteralExpression b) {

                        Object red = JavaConstantExpressionEvaluator.computeConstantExpression(r, false);
                        Object green = JavaConstantExpressionEvaluator.computeConstantExpression(g, false);
                        Object blue = JavaConstantExpressionEvaluator.computeConstantExpression(b, false);
                        if (NullUtils.notNull(red, green, blue)) {
                            try {
                                int rr = Integer.parseInt(red.toString());
                                int gg = Integer.parseInt(green.toString());
                                int bb = Integer.parseInt(blue.toString());
                                if (rr == gg && gg == bb && 0 <= rr && rr < 256) {
                                    holder.newProblem(DevKitLocalize.useGrayInspectionMessage(rr))
                                        .range(expression)
                                        .onTheFly(isOnTheFly)
                                        .withFix(new ConvertToGrayQuickFix(rr))
                                        .create();
                                }
                            }
                            catch (Exception ignore) {
                            }
                        }
                    }
                }
                else if (expressions.length == 1
                    && "com.intellij.ui.Gray".equals(type.getCanonicalText())
                    && expressions[0] instanceof PsiLiteralExpression literal) {

                    Object literalValue = JavaConstantExpressionEvaluator.computeConstantExpression(literal, false);
                    if (literalValue != null) {
                        try {
                            int num = Integer.parseInt(literalValue.toString());
                            if (0 <= num && num < 256) {
                                holder.newProblem(DevKitLocalize.useGrayInspectionMessage(num))
                                    .range(expression)
                                    .onTheFly(isOnTheFly)
                                    .withFix(new ConvertToGrayQuickFix(num))
                                    .create();
                            }
                        }
                        catch (Exception ignore) {
                        }
                    }
                }
            }
        }
    }
}
