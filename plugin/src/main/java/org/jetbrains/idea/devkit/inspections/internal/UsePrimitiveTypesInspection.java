/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
import com.intellij.java.language.psi.util.PsiUtil;
import com.siyeh.IntentionPowerPackBundle;
import com.siyeh.ig.PsiReplacementUtil;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.ast.IElementType;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.Nls;

@ExtensionImpl
public class UsePrimitiveTypesInspection extends InternalInspection {
    @Override
    public PsiElementVisitor buildInternalVisitor(@Nonnull final ProblemsHolder holder, final boolean isOnTheFly) {
        return new JavaElementVisitor() {
            @Override
            public void visitBinaryExpression(@Nonnull PsiBinaryExpression expression) {
                super.visitBinaryExpression(expression);
                final IElementType tokenType = expression.getOperationTokenType();
                if (JavaTokenType.EQEQ.equals(tokenType) || JavaTokenType.NE.equals(tokenType)) {
                    final PsiExpression lOperand = expression.getLOperand();
                    final PsiExpression rOperand = expression.getROperand();
                    if (rOperand != null && (isPrimitiveTypeRef(lOperand) || isPrimitiveTypeRef(rOperand))) {
                        final String name;
                        if (JavaTokenType.NE.equals(tokenType)) {
                            name = IntentionPowerPackBundle.message("replace.equality.with.not.equals.intention.name");
                        }
                        else {
                            name = IntentionPowerPackBundle.message("replace.equality.with.equals.intention.name");
                        }
                        holder.registerProblem(
                            expression.getOperationSign(),
                            "Primitive types should be compared with .equals",
                            new ReplaceEqualityWithEqualsFix(name)
                        );
                    }
                }
            }
        };
    }

    @RequiredReadAction
    private static boolean isPrimitiveTypeRef(PsiExpression expression) {
        if (expression instanceof PsiReferenceExpression referenceExpression && referenceExpression.resolve() instanceof PsiField field) {
            final PsiClass containingClass = field.getContainingClass();
            return containingClass != null
                && PsiType.class.getName().equals(containingClass.getQualifiedName())
                && !"NULL".equals(field.getName());
        }
        return false;
    }

    @Nonnull
    @Override
    public String getDisplayName() {
        return "Use .equals with primitive types";
    }

    private static class ReplaceEqualityWithEqualsFix implements LocalQuickFix {
        private final String myName;

        public ReplaceEqualityWithEqualsFix(String name) {
            myName = name;
        }

        @Nls
        @Nonnull
        @Override
        public String getName() {
            return myName;
        }

        @Nls
        @Nonnull
        @Override
        public String getFamilyName() {
            return "Replace equality with .equals";
        }

        @Override
        @RequiredReadAction
        public void applyFix(@Nonnull Project project, @Nonnull ProblemDescriptor descriptor) {
            final PsiElement psiElement = descriptor.getPsiElement();
            if (psiElement instanceof PsiJavaToken javaToken) {
                final IElementType tokenType = javaToken.getTokenType();
                final String prefix;
                if (JavaTokenType.EQEQ.equals(tokenType)) {
                    prefix = "";
                }
                else if (JavaTokenType.NE.equals(tokenType)) {
                    prefix = "!";
                }
                else {
                    return;
                }

                if (psiElement.getParent() instanceof PsiBinaryExpression binaryExpression) {
                    final PsiExpression rOperand = binaryExpression.getROperand();
                    final PsiExpression lOperand = binaryExpression.getLOperand();
                    if (rOperand != null) {
                        final boolean flip = isPrimitiveTypeRef(rOperand);
                        if (flip || isPrimitiveTypeRef(lOperand)) {
                            final String rText = PsiUtil.skipParenthesizedExprUp(rOperand).getText();
                            final String lText = PsiUtil.skipParenthesizedExprUp(lOperand).getText();

                            final String lhText = flip ? rText : lText;
                            final String rhText = flip ? lText : rText;

                            final String expString = prefix + lhText + ".equals(" + rhText + ')';
                            PsiReplacementUtil.replaceExpression(binaryExpression, expString);
                        }
                    }
                }
            }
        }
    }
}