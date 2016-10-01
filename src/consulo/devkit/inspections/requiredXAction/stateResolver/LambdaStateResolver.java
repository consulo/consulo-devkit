/*
 * Copyright 2013-2016 must-be.org
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

import java.util.Collection;

import org.jetbrains.annotations.NotNull;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiCall;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpressionList;
import com.intellij.psi.PsiLambdaExpression;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.CommonProcessors;
import com.intellij.util.ThreeState;
import consulo.devkit.inspections.requiredXAction.CallStateType;

/**
 * @author VISTALL
 * @since 01-Oct-16
 */
public class LambdaStateResolver extends StateResolver
{
	@NotNull
	@Override
	public Pair<ThreeState, CallStateType> resolveState(CallStateType actionType, PsiCall expression)
	{
		PsiLambdaExpression lambdaExpression = PsiTreeUtil.getParentOfType(expression, PsiLambdaExpression.class);
		if(lambdaExpression == null)
		{
			return Pair.create(ThreeState.UNSURE, null);
		}

		PsiElement maybeParameterListOrVariable = lambdaExpression.getParent();
		// Runnable run = new Runnable() {};
		// ApplicationManager.getApplication().runReadAction(run);
		if(maybeParameterListOrVariable instanceof PsiVariable)
		{
			CommonProcessors.CollectProcessor<PsiReference> processor = new CommonProcessors.CollectProcessor<PsiReference>();
			ReferencesSearch.search(maybeParameterListOrVariable).forEach(processor);

			Collection<PsiReference> results = processor.getResults();
			if(results.isEmpty())
			{
				return Pair.create(ThreeState.NO, actionType);
			}

			boolean weFoundRunAction = false;
			for(PsiReference result : results)
			{
				if(result instanceof PsiReferenceExpression)
				{
					PsiElement maybeExpressionList = ((PsiReferenceExpression) result).getParent();
					if(maybeExpressionList instanceof PsiExpressionList)
					{
						if(acceptActionTypeFromCall((PsiExpressionList) maybeExpressionList, actionType))
						{
							weFoundRunAction = true;
							break;
						}
					}
				}
			}

			if(weFoundRunAction)
			{
				return Pair.create(ThreeState.YES, actionType);
			}
		}
		// ApplicationManager.getApplication().runReadAction(new Runnable() {});
		else if(maybeParameterListOrVariable instanceof PsiExpressionList)
		{
			if(acceptActionTypeFromCall((PsiExpressionList) maybeParameterListOrVariable, actionType))
			{
				return Pair.create(ThreeState.YES, actionType);
			}
		}
		return Pair.create(ThreeState.NO, actionType);
	}
}
