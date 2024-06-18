/*
 * Copyright 2013-2016 consulo.io
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

package consulo.devkit.inspections.requiredXAction;

import com.intellij.java.language.psi.PsiClass;
import com.intellij.java.language.psi.PsiMethod;
import com.intellij.java.language.psi.PsiMethodCallExpression;
import consulo.language.psi.PsiElement;

/**
 * @author VISTALL
 * @since 22-Jun-16
 */
public class AcceptableMethodCallCheck {
  private final String myParentClass;
  private final String myMethodName;

  public AcceptableMethodCallCheck(String parentClass, String methodName) {
    myParentClass = parentClass;
    myMethodName = methodName;
  }

  public AcceptableMethodCallCheck(Class<?> parentClass, String methodName) {
    this(parentClass.getName(), methodName);
  }

  public boolean accept(PsiElement parent) {
    if (parent instanceof PsiMethodCallExpression methodCallExpression) {
      PsiMethod psiMethod = methodCallExpression.resolveMethod();
      if (psiMethod == null) {
        return false;
      }

      if (myMethodName.equals(psiMethod.getName())) {
        PsiClass containingClass = psiMethod.getContainingClass();
        if (containingClass == null) {
          return false;
        }

        if (myParentClass.equals(containingClass.getQualifiedName())) {
          return true;
        }
      }
    }
    return false;
  }
}
