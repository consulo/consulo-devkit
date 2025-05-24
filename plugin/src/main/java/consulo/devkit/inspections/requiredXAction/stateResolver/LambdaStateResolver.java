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
import com.intellij.java.language.psi.PsiLambdaExpression;
import consulo.annotation.access.RequiredReadAction;
import consulo.devkit.inspections.requiredXAction.CallStateType;
import consulo.language.psi.PsiElement;
import consulo.language.psi.util.PsiTreeUtil;

import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2016-10-01
 */
public class LambdaStateResolver extends StateResolver {
    public static final StateResolver INSTANCE = new LambdaStateResolver();

    @Nullable
    @Override
    @RequiredReadAction
    public Boolean resolveState(CallStateType actionType, PsiExpression expression) {
        PsiLambdaExpression lambdaExpression = PsiTreeUtil.getParentOfType(expression, PsiLambdaExpression.class);
        if (lambdaExpression == null) {
            return null;
        }

        PsiElement maybeParameterListOrVariable = lambdaExpression.getParent();
        return resolveByMaybeParameterListOrVariable(maybeParameterListOrVariable, actionType)
            || isAllowedFunctionCall(lambdaExpression, actionType);
    }
}
