/*
 * Copyright 2013-2015 must-be.org
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

package org.mustbe.consulo.devkit.inspections;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.devkit.DevKitBundle;
import org.mustbe.consulo.RequiredDispatchThread;
import org.mustbe.consulo.RequiredReadAction;
import org.mustbe.consulo.RequiredWriteAction;
import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.BaseActionRunnable;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.*;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ArrayUtil;
import com.intellij.util.CommonProcessors;
import com.intellij.util.ThreeState;

/**
 * @author VISTALL
 * @since 18.02.2015
 */
public class RequiredXActionInspection extends LocalInspectionTool
{
	public static enum ActionType
	{
		NONE(null, null, null),
		READ(RequiredReadAction.class, "runReadAction", ReadAction.class),
		WRITE(RequiredWriteAction.class, "runWriteAction", WriteAction.class);

		private final Class<?> myActionClass;
		private final String myApplicationMethod;
		private final Class<? extends BaseActionRunnable> myActionRunnable;

		ActionType(Class<? extends Annotation> actionClass, String applicationMethod, Class<? extends BaseActionRunnable> actionRunnable)
		{
			myActionClass = actionClass;
			myApplicationMethod = applicationMethod;
			myActionRunnable = actionRunnable;
		}

		@NotNull
		public static ActionType findActionType(@NotNull PsiMethod method)
		{
			PsiClass baseActionRunnable = JavaPsiFacade.getInstance(method.getProject()).findClass(BaseActionRunnable.class.getName(),
					method.getResolveScope());

			PsiMethod baseRunMethod = null;
			if(baseActionRunnable != null)
			{
				PsiMethod[] runMethods = baseActionRunnable.findMethodsByName("run", false);
				baseRunMethod = ArrayUtil.getFirstElement(runMethods);
			}

			for(ActionType actionType : values())
			{
				Class<?> actionClass = actionType.myActionClass;
				if(actionClass == null)
				{
					continue;
				}

				if(AnnotationUtil.isAnnotated(method, actionClass.getName(), false))
				{
					return actionType;
				}

				if(actionType == READ)
				{
					if(AnnotationUtil.isAnnotated(method, RequiredDispatchThread.class.getName(), false))
					{
						return actionType;
					}
				}

				if(baseRunMethod != null)
				{
					PsiMethod[] superMethods = method.findSuperMethods(baseActionRunnable);
					if(ArrayUtil.contains(baseRunMethod, superMethods))
					{
						PsiClass containingClass = method.getContainingClass();
						if(containingClass != null)
						{
							if(InheritanceUtil.isInheritor(containingClass, actionType.myActionRunnable.getName()))
							{
								return actionType;
							}
						}
					}
				}
			}
			return NONE;
		}
	}

	public static class RequiredXActionVisitor extends JavaElementVisitor
	{
		private static Map<String, String[]> ourInterfacees = new HashMap<String, String[]>()
		{
			{
				put("compute", new String[]{
						Computable.class.getName(),
						ThrowableComputable.class.getName()
				});
				put("run", new String[]{
						Runnable.class.getName()
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
			Pair<ThreeState, ActionType> handled = isHandled(expression);
			switch(handled.getFirst())
			{
				case NO:
					reportError(expression, handled.getSecond());
					break;
			}
		}

		@NotNull
		private Pair<ThreeState, ActionType> isHandled(PsiCall expression)
		{
			PsiMethod psiMethod = expression.resolveMethod();
			if(psiMethod == null)
			{
				return Pair.create(ThreeState.UNSURE, null);
			}

			ActionType actionType = ActionType.findActionType(psiMethod);
			if(actionType == ActionType.NONE)
			{
				return Pair.create(ThreeState.UNSURE, null);
			}

			PsiMethod callMethod = PsiTreeUtil.getParentOfType(expression, PsiMethod.class);
			if(callMethod == null)
			{
				return Pair.create(ThreeState.UNSURE, null);
			}

			// method annotated by annotation
			if(ActionType.findActionType(callMethod) == actionType)
			{
				return Pair.create(ThreeState.UNSURE, null);
			}

			if(callMethod.getParameterList().getParametersCount() == 0)
			{
				String[] qualifiedNames = ourInterfacees.get(callMethod.getName());
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
				for(String qualifiedName : qualifiedNames)
				{
					if(InheritanceUtil.isInheritor(containingClass, qualifiedName))
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
										if(isInsideRunAction((PsiExpressionList) maybeExpressionList, actionType))
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
							if(isInsideRunAction((PsiExpressionList) maybeParameterListOrVariable, actionType))
							{
								return Pair.create(ThreeState.YES, actionType);
							}
						}
					}
				}
			}

			return Pair.create(ThreeState.NO, actionType);
		}

		private boolean isInsideRunAction(@NotNull PsiExpressionList expressionList, @NotNull ActionType actionType)
		{
			PsiElement parent = expressionList.getParent();
			if(parent instanceof PsiMethodCallExpression)
			{
				PsiMethod psiMethod = ((PsiMethodCallExpression) parent).resolveMethod();
				if(psiMethod == null)
				{
					return false;
				}

				String applicationMethod = actionType.myApplicationMethod;
				assert applicationMethod != null;
				if(applicationMethod.equals(psiMethod.getName()))
				{
					PsiClass containingClass = psiMethod.getContainingClass();
					if(containingClass == null)
					{
						return false;
					}

					if(Application.class.getName().equals(containingClass.getQualifiedName()))
					{
						return true;
					}
				}
			}
			return false;
		}

		private void reportError(@NotNull PsiCall expression, @NotNull ActionType type)
		{
			String text;
			switch(type)
			{
				case READ:
				case WRITE:
					text = DevKitBundle.message("inspections.annotation.0.is.required.at.owner.or.app.run",
							StringUtil.capitalize(type.name().toLowerCase()));
					break;
				default:
					throw new IllegalArgumentException();
			}
			myHolder.registerProblem(expression, text, ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
		}
	}

	@NotNull
	@Override
	public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly)
	{
		return new RequiredXActionVisitor(holder);
	}
}
