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

package consulo.devkit.inspections.requiredXAction.stateResolver;

import com.intellij.java.language.psi.PsiExpression;
import com.intellij.java.language.psi.PsiMethodReferenceExpression;
import consulo.annotation.access.RequiredReadAction;
import consulo.devkit.inspections.requiredXAction.CallStateType;

import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2016-10-01
 */
public class MethodReferenceResolver extends StateResolver {
    public static final StateResolver INSTANCE = new MethodReferenceResolver();

    @RequiredReadAction
    @Nullable
    @Override
    public Boolean resolveState(CallStateType actionType, PsiExpression expression) {
        PsiMethodReferenceExpression methodReferenceExpression = (PsiMethodReferenceExpression)expression;
        return resolveByMaybeParameterListOrVariable(methodReferenceExpression.getParent(), actionType)
            || isAllowedFunctionCall(methodReferenceExpression, actionType);
    }
}
