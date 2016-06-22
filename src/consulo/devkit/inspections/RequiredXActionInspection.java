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

package consulo.devkit.inspections;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.devkit.DevKitBundle;
import org.mustbe.consulo.RequiredDispatchThread;
import com.intellij.codeInspection.AnnotateMethodFix;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.*;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.CommonProcessors;
import com.intellij.util.ThreeState;
import com.intellij.util.ThrowableRunnable;
import consulo.devkit.codeInsight.ConsuloUI;

/**
 * @author VISTALL
 * @since 18.02.2015
 */
public class RequiredXActionInspection extends LocalInspectionTool
{
	public static class RequiredXActionVisitor extends JavaElementVisitor
	{
		private static Map<String, Class[]> ourInterfaces = new HashMap<String, Class[]>()
		{
			{
				put("compute", new Class[]{
						Computable.class,
						ThrowableComputable.class
				});
				put("run", new Class[]{
						Runnable.class,
						ThrowableRunnable.class
				});
			}
		};

		private final ProblemsHolder myHolder;

		public RequiredXActionVisitor(ProblemsHolder holder)
		{
			myHolder = holder;
		}

		@Override
		public void visitCallExpression(PsiCallExpression expression)
		{
			Pair<ThreeState, CallStateType> handled = isHandled(expression);
			switch(handled.getFirst())
			{
				case NO:
					reportError(expression, handled.getSecond());
					break;
			}
		}

		@NotNull
		private Pair<ThreeState, CallStateType> isHandled(PsiCall expression)
		{
			PsiMethod psiMethod = expression.resolveMethod();
			if(psiMethod == null)
			{
				return Pair.create(ThreeState.UNSURE, null);
			}

			CallStateType actionType = CallStateType.findActionType(psiMethod);
			if(actionType == CallStateType.NONE)
			{
				return Pair.create(ThreeState.UNSURE, null);
			}

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

		private boolean acceptActionTypeFromCall(@NotNull PsiExpressionList expressionList, @NotNull CallStateType actionType)
		{
			for(CallStateType type : CallStateType.values())
			{
				if(actionType.isAcceptableActionType(type))
				{
					PsiElement parent = expressionList.getParent();

					for(AcceptableMethodCallCheck acceptableMethodCallCheck : type.getAcceptableMethodCallChecks())
					{
						if(acceptableMethodCallCheck.accept(parent))
						{
							return true;
						}
					}
				}
			}
			return false;
		}

		private void reportError(@NotNull PsiCall expression, @NotNull CallStateType type)
		{
			LocalQuickFix[] quickFixes = LocalQuickFix.EMPTY_ARRAY;
			String text;
			switch(type)
			{
				case READ:
				case WRITE:
					text = DevKitBundle.message("inspections.annotation.0.is.required.at.owner.or.app.run", StringUtil.capitalize(type.name().toLowerCase()));
					break;
				case DISPATCH_THREAD:
					text = DevKitBundle.message("inspections.annotation.0.is.required.at.owner.or.app.run.dispath", StringUtil.capitalize(type.name().toLowerCase()));
					quickFixes = new LocalQuickFix[]{new AnnotateMethodFix(RequiredDispatchThread.class.getName())};
					break;
				case UI_ACCESS:
					text = DevKitBundle.message("inspections.annotation.0.is.required.at.owner.or.app.run.ui", StringUtil.capitalize(type.name().toLowerCase()));
					quickFixes = new LocalQuickFix[]{new AnnotateMethodFix(ConsuloUI.RequiredUIAccess)};
					break;
				default:
					throw new IllegalArgumentException();
			}
			myHolder.registerProblem(expression, text, ProblemHighlightType.GENERIC_ERROR_OR_WARNING, quickFixes);
		}
	}

	@NotNull
	@Override
	public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly)
	{
		return new RequiredXActionVisitor(holder);
	}
}
