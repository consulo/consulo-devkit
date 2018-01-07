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

import org.jetbrains.annotations.Nullable;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiLambdaExpression;
import com.intellij.psi.util.PsiTreeUtil;
import consulo.annotations.RequiredReadAction;
import consulo.devkit.inspections.requiredXAction.CallStateType;

/**
 * @author VISTALL
 * @since 01-Oct-16
 */
public class LambdaStateResolver extends StateResolver
{
	public static final StateResolver INSTANCE = new LambdaStateResolver();

	@RequiredReadAction
	@Nullable
	@Override
	public Boolean resolveState(CallStateType actionType, PsiExpression expression)
	{
		PsiLambdaExpression lambdaExpression = PsiTreeUtil.getParentOfType(expression, PsiLambdaExpression.class);
		if(lambdaExpression == null)
		{
			return null;
		}

		PsiElement maybeParameterListOrVariable = lambdaExpression.getParent();
		return resolveByMaybeParameterListOrVariable(maybeParameterListOrVariable, actionType) || isAllowedFunctionCall(lambdaExpression, actionType);
	}
}
