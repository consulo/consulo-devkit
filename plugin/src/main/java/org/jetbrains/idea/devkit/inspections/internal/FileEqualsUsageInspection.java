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
import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;

import javax.annotation.Nonnull;

@ExtensionImpl
public class FileEqualsUsageInspection extends InternalInspection {
  private static final String MESSAGE =
    "Do not use File.equals/hashCode/compareTo as they don't honor case-sensitivity on MacOS. " + "Please use " +
      "FileUtil.filesEquals/fileHashCode/compareFiles instead";

  @Override
  @Nonnull
  public PsiElementVisitor buildInternalVisitor(@Nonnull final ProblemsHolder holder, boolean isOnTheFly) {
    return new JavaElementVisitor() {
      @Override
      public void visitMethodCallExpression(PsiMethodCallExpression expression) {
        PsiReferenceExpression methodExpression = expression.getMethodExpression();
        PsiElement resolved = methodExpression.resolve();
        if (!(resolved instanceof PsiMethod)) {
          return;
        }

        PsiMethod method = (PsiMethod)resolved;

        PsiClass clazz = method.getContainingClass();
        if (clazz == null) {
          return;
        }

        String methodName = method.getName();
        if (CommonClassNames.JAVA_IO_FILE.equals(clazz.getQualifiedName()) && ("equals".equals(methodName) || "compareTo".equals(methodName)
          || "hashCode".equals(methodName))) {
          holder.registerProblem(methodExpression, MESSAGE, ProblemHighlightType.LIKE_DEPRECATED);
        }
      }
    };
  }

  @Nonnull
  @Override
  public String getDisplayName() {
    return "File.equals() usage";
  }
}
