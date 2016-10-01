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
import com.intellij.psi.*;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.CommonProcessors;
import com.intellij.util.ThreeState;
import consulo.devkit.inspections.requiredXAction.CallStateType;

/**
 * @author VISTALL
 * @since 01-Oct-16
 */
public class AnonymousClassStateResolver extends StateResolver
{
	@Override
	@NotNull
	public Pair<ThreeState, CallStateType> resolveState(CallStateType actionType, PsiCall expression)
	{
		PsiMethod callMethod = PsiTreeUtil.getParentOfType(expression, PsiMethod.class);
		if(callMethod == null)
		{
			return Pair.create(ThreeState.UNSURE, null);
		}

		// method annotated by annotation
		CallStateType callMethodActionType = CallStateType.findActionType(callMethod);
		if(actionType.isAcceptableActionType(callMethodActionType))
		{
			return Pair.create(ThreeState.UNSURE, null);
		}

		if(callMethod.getParameterList().getParametersCount() == 0)
		{
			Class[] qualifiedNames = ourInterfaces.get(callMethod.getName());
			if(qualifiedNames == null)
			{
				return Pair.create(ThreeState.NO, actionType);
			}

			PsiClass containingClass = callMethod.getContainingClass();
			if(containingClass == null)
			{
				return Pair.create(ThreeState.NO, actionType);
			}

			boolean inherit = false;
			for(Class clazz : qualifiedNames)
			{
				if(InheritanceUtil.isInheritor(containingClass, clazz.getName()))
				{
					inherit = true;
					break;
				}
			}

			if(inherit)
			{
				// non anonym class - cant handle
				if(!(containingClass instanceof PsiAnonymousClass))
				{
					return Pair.create(ThreeState.NO, actionType);
				}

				PsiElement parent = containingClass.getParent();
				if(parent instanceof PsiNewExpression)
				{
					PsiElement maybeParameterListOrVariable = parent.getParent();
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
				}
			}
		}

		return Pair.create(ThreeState.NO, actionType);
	}
}
