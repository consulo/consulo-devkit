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
import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.psi.PsiReference;
import consulo.localize.LocalizeValue;
import jakarta.annotation.Nonnull;

import java.util.Set;

@ExtensionImpl
public class FileEqualsUsageInspection extends InternalInspection {
    private static final Set FILE_METHOD_NAMES = Set.of("equals", "compareTo", "hashCode");

    @Nonnull
    @Override
    public LocalizeValue getDisplayName() {
        return DevKitLocalize.inspectionFileEqualsUsageDisplayName();
    }

    @Override
    @Nonnull
    public PsiElementVisitor buildInternalVisitor(@Nonnull final ProblemsHolder holder, boolean isOnTheFly) {
        return new JavaElementVisitor() {
            @Override
            @RequiredReadAction
            public void visitMethodCallExpression(@Nonnull PsiMethodCallExpression expression) {
                if (expression.getMethodExpression().resolve() instanceof PsiMethod method) {
                    PsiClass clazz = method.getContainingClass();
                    if (clazz == null) {
                        return;
                    }

                    if (CommonClassNames.JAVA_IO_FILE.equals(clazz.getQualifiedName()) && FILE_METHOD_NAMES.contains(method.getName())) {
                        holder.newProblem(DevKitLocalize.inspectionFileEqualsUsageMessage())
                            .range((PsiReference) method)
                            .highlightType(ProblemHighlightType.LIKE_DEPRECATED)
                            .create();
                    }
                }
            }
        };
    }
}
