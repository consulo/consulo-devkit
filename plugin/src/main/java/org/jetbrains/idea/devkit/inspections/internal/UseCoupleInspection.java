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
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.devkit.localize.DevKitLocalize;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.psi.PsiReference;
import consulo.localize.LocalizeValue;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import org.jetbrains.idea.devkit.inspections.quickfix.UseCoupleQuickFix;

import java.util.List;
import java.util.Objects;

/**
 * @author Konstantin Bulenkov
 */
@ExtensionImpl
public class UseCoupleInspection extends InternalInspection {
    private static final String PAIR_FQN = Pair.class.getName();

    @Nonnull
    @Override
    public LocalizeValue getDisplayName() {
        return DevKitLocalize.inspectionUseCoupleDisplayName();
    }

    @Nonnull
    @Override
    public String getShortName() {
        return "UseCouple";
    }

    @Override
    public PsiElementVisitor buildInternalVisitor(@Nonnull final ProblemsHolder holder, boolean isOnTheFly) {
        return new JavaElementVisitor() {
            @Override
            public void visitTypeElement(@Nonnull PsiTypeElement type) {
                String canonicalText = type.getType().getCanonicalText();
                if (canonicalText.startsWith(PAIR_FQN) && canonicalText.contains("<") && canonicalText.endsWith(">")) {
                    String genericTypes = canonicalText.substring(canonicalText.indexOf('<') + 1, canonicalText.length() - 1);
                    List<String> types = StringUtil.split(genericTypes, ",");
                    if (types.size() == 2 && StringUtil.equals(types.get(0), types.get(1))) {
                        List<String> parts = StringUtil.split(types.get(0), ".");
                        String typeName = parts.get(parts.size() - 1);
                        LocalizeValue name = DevKitLocalize.inspectionUseCoupleMessageType(typeName);
                        holder.newProblem(name)
                            .range(type)
                            .withFix(new UseCoupleQuickFix(name))
                            .create();
                    }
                }
                super.visitTypeElement(type);
            }

            @Override
            @RequiredReadAction
            public void visitMethodCallExpression(@Nonnull PsiMethodCallExpression expression) {
                if (expression.getText().startsWith("Pair.create")) {
                    PsiReference reference = expression.getMethodExpression().getReference();
                    if (reference != null && reference.resolve() instanceof PsiMethod method) {
                        PsiClass psiClass = method.getContainingClass();
                        if (psiClass != null && PAIR_FQN.equals(psiClass.getQualifiedName())) {
                            PsiType[] types = expression.getArgumentList().getExpressionTypes();
                            if (types.length == 2 && Objects.equals(types[0], types[1])) {
                                LocalizeValue name = DevKitLocalize.inspectionUseCoupleMessageConstructor();
                                holder.newProblem(name)
                                    .range(expression)
                                    .withFix(new UseCoupleQuickFix(name))
                                    .create();
                            }
                        }
                    }
                }
                super.visitMethodCallExpression(expression);
            }
        };
    }
}