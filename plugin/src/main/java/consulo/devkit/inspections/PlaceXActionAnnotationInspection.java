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

package consulo.devkit.inspections;

import com.intellij.codeInsight.intention.AddAnnotationFix;
import com.intellij.codeInspection.AnnotateMethodFix;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.searches.OverridingMethodsSearch;
import com.intellij.util.Query;
import consulo.devkit.inspections.requiredXAction.CallStateType;
import org.jetbrains.idea.devkit.inspections.internal.InternalInspection;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 18.05.2015
 */
public class PlaceXActionAnnotationInspection extends InternalInspection
{
	private static class MyAnnotateMethodFix extends AnnotateMethodFix
	{
		public MyAnnotateMethodFix(String fqn, String... annotationsToRemove)
		{
			super(fqn, annotationsToRemove);
		}

		@Nonnull
		@Override
		protected String getPreposition()
		{
			return "as";
		}

		@Override
		protected boolean annotateOverriddenMethods()
		{
			return true;
		}

		@Override
		protected boolean annotateSelf()
		{
			return false;
		}
	}

	@Nonnull
	@Override
	public PsiElementVisitor buildInternalVisitor(@Nonnull final ProblemsHolder holder, boolean isOnTheFly)
	{
		return new JavaElementVisitor()
		{
			@Override
			public void visitMethod(PsiMethod method)
			{
				if(method.isConstructor())
				{
					return;
				}

				PsiIdentifier nameIdentifier = method.getNameIdentifier();
				if(nameIdentifier == null)
				{
					return;
				}

				CallStateType selfActionType = CallStateType.findSelfActionType(method);
				if(selfActionType != CallStateType.NONE)
				{
					Query<PsiMethod> query = OverridingMethodsSearch.search(method);
					for(PsiMethod itMethod : query)
					{
						if(CallStateType.findSelfActionType(itMethod) == CallStateType.NONE)
						{
							String actionClass = selfActionType.getActionClass();
							holder.registerProblem(nameIdentifier, "Overriden methods are not annotated by @" + StringUtil.getShortName(actionClass), new MyAnnotateMethodFix(actionClass));
							break;
						}
					}
				}
				else
				{
					PsiMethod[] superMethods = method.findSuperMethods();
					for(PsiMethod superMethod : superMethods)
					{
						CallStateType superActionType = CallStateType.findSelfActionType(superMethod);
						if(superActionType != CallStateType.NONE)
						{
							String actionClass = superActionType.getActionClass();
							holder.registerProblem(nameIdentifier, "Missed annotation @" + StringUtil.getShortName(actionClass) + ", provided by super method", new AddAnnotationFix(actionClass,
									method));
						}
					}
				}
			}
		};
	}
}
