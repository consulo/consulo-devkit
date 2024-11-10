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
import com.intellij.java.language.psi.util.InheritanceUtil;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.devkit.localize.DevKitLocalize;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.project.Project;
import consulo.util.lang.ref.SimpleReference;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

@ExtensionImpl
public class UnsafeVfsRecursionInspection extends InternalInspection {
    private static final String VIRTUAL_FILE_CLASS_NAME = VirtualFile.class.getName();
    private static final String GET_CHILDREN_METHOD_NAME = "getChildren";

    @Nonnull
    @Override
    public String getDisplayName() {
        return DevKitLocalize.unsafeVfsRecursionInspectionDisplayName().get();
    }

    @Nonnull
    @Override
    public PsiElementVisitor buildInternalVisitor(@Nonnull final ProblemsHolder holder, final boolean isOnTheFly) {
        return new JavaElementVisitor() {
            @Override
            @RequiredReadAction
            public void visitMethodCallExpression(@Nonnull final PsiMethodCallExpression expression) {
                final PsiReferenceExpression methodRef = expression.getMethodExpression();
                if (GET_CHILDREN_METHOD_NAME.equals(methodRef.getReferenceName())
                    && methodRef.resolve() instanceof PsiMethod method) {

                    PsiClass aClass = method.getContainingClass();
                    Project project = expression.getProject();
                    JavaPsiFacade facade = JavaPsiFacade.getInstance(project);
                    PsiClass virtualFileClass = facade.findClass(VIRTUAL_FILE_CLASS_NAME, GlobalSearchScope.allScope(project));

                    if (!InheritanceUtil.isInheritorOrSelf(aClass, virtualFileClass, true)) {
                        return;
                    }

                    final PsiMethod containingMethod = PsiTreeUtil.getParentOfType(expression, PsiMethod.class);
                    if (containingMethod == null) {
                        return;
                    }
                    final String containingMethodName = containingMethod.getName();
                    final SimpleReference<Boolean> result = SimpleReference.create();
                    containingMethod.accept(new JavaRecursiveElementVisitor() {
                        @Override
                        public void visitMethodCallExpression(@Nonnull final PsiMethodCallExpression expression2) {
                            if (expression2 != expression
                                && containingMethodName.equals(expression2.getMethodExpression().getReferenceName())
                                && expression2.resolveMethod() == containingMethod) {
                                result.set(true);
                            }
                        }
                    });

                    if (!result.isNull()) {
                        holder.newProblem(DevKitLocalize.unsafeVfsRecursionInspectionMessage())
                            .range(expression)
                            .create();
                    }
                }
            }
        };
    }
}
