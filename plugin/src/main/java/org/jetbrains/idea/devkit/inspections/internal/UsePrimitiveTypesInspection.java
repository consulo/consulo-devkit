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
import com.siyeh.ig.PsiReplacementUtil;
import com.siyeh.localize.IntentionPowerPackLocalize;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.devkit.localize.DevKitLocalize;
import consulo.language.ast.IElementType;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import jakarta.annotation.Nonnull;

@ExtensionImpl
public class UsePrimitiveTypesInspection extends InternalInspection {
    @Nonnull
    @Override
    public LocalizeValue getDisplayName() {
        return DevKitLocalize.inspectionUsePrimitiveTypesDisplayName();
    }

    @Override
    public PsiElementVisitor buildInternalVisitor(@Nonnull final ProblemsHolder holder, boolean isOnTheFly) {
        return new JavaElementVisitor() {
            @Override
            @RequiredReadAction
            public void visitBinaryExpression(@Nonnull PsiBinaryExpression expression) {
                super.visitBinaryExpression(expression);
                IElementType tokenType = expression.getOperationTokenType();
                if (JavaTokenType.EQEQ.equals(tokenType) || JavaTokenType.NE.equals(tokenType)) {
                    PsiExpression lOperand = expression.getLOperand();
                    PsiExpression rOperand = expression.getROperand();
                    if (rOperand != null && (isPrimitiveTypeRef(lOperand) || isPrimitiveTypeRef(rOperand))) {
                        LocalizeValue fixName;
                        if (JavaTokenType.NE.equals(tokenType)) {
                            fixName = DevKitLocalize.inspectionUsePrimitiveTypesQuickfixNameNe();
                        }
                        else {
                            fixName = DevKitLocalize.inspectionUsePrimitiveTypesQuickfixNameEq();
                        }
                        holder.newProblem(DevKitLocalize.inspectionUsePrimitiveTypesMessage())
                            .range(expression.getOperationSign())
                            .withFix(new ReplaceEqualityWithEqualsFix(fixName))
                            .create();
                    }
                }
            }
        };
    }

    @RequiredReadAction
    private static boolean isPrimitiveTypeRef(PsiExpression expression) {
        if (expression instanceof PsiReferenceExpression referenceExpression && referenceExpression.resolve() instanceof PsiField field) {
            PsiClass containingClass = field.getContainingClass();
            return containingClass != null
                && PsiType.class.getName().equals(containingClass.getQualifiedName())
                && !"NULL".equals(field.getName());
        }
        return false;
    }

    private static class ReplaceEqualityWithEqualsFix implements LocalQuickFix {
        private final LocalizeValue myName;

        public ReplaceEqualityWithEqualsFix(LocalizeValue name) {
            myName = name;
        }

        @Nonnull
        @Override
        public LocalizeValue getName() {
            return myName;
        }

        @Override
        @RequiredReadAction
        public void applyFix(@Nonnull Project project, @Nonnull ProblemDescriptor descriptor) {
            PsiElement psiElement = descriptor.getPsiElement();
            if (psiElement instanceof PsiJavaToken javaToken) {
                IElementType tokenType = javaToken.getTokenType();
                String prefix;
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
                    PsiExpression rOperand = binaryExpression.getROperand();
                    PsiExpression lOperand = binaryExpression.getLOperand();
                    if (rOperand != null) {
                        boolean flip = isPrimitiveTypeRef(rOperand);
                        if (flip || isPrimitiveTypeRef(lOperand)) {
                            String rText = PsiUtil.skipParenthesizedExprUp(rOperand).getText();
                            String lText = PsiUtil.skipParenthesizedExprUp(lOperand).getText();

                            String lhText = flip ? rText : lText;
                            String rhText = flip ? lText : rText;

                            String expString = prefix + lhText + ".equals(" + rhText + ')';
                            PsiReplacementUtil.replaceExpression(binaryExpression, expString);
                        }
                    }
                }
            }
        }
    }
}