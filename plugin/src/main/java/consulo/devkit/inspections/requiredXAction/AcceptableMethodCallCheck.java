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
import com.intellij.java.language.psi.PsiExpressionList;
import com.intellij.java.language.psi.PsiMethod;
import consulo.annotation.access.RequiredReadAction;
import jakarta.annotation.Nonnull;

import java.util.Set;

/**
 * @author VISTALL
 * @since 2016-06-22
 */
public class AcceptableMethodCallCheck {
    @Nonnull
    private final String myParentClass;
    @Nonnull
    private final Set<String> myMethodNames;

    public AcceptableMethodCallCheck(@Nonnull String parentClass, @Nonnull Set<String> methodNames) {
        myParentClass = parentClass;
        myMethodNames = methodNames;
    }

    public AcceptableMethodCallCheck(@Nonnull Class<?> parentClass, @Nonnull Set<String> methodNames) {
        this(parentClass.getName(), methodNames);
    }

    @RequiredReadAction
    public boolean accept(PsiMethod method, PsiExpressionList expressionList) {
        if (!myMethodNames.contains(method.getName())) {
            return false;
        }

        PsiClass containingClass = method.getContainingClass();
        return containingClass != null && myParentClass.equals(containingClass.getQualifiedName());
    }
}
