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

package consulo.devkit.inspections;

import org.jetbrains.annotations.NotNull;
import com.intellij.codeInsight.intention.AddAnnotationFix;
import com.intellij.codeInspection.AnnotateMethodFix;
import com.intellij.codeInspection.InspectionsBundle;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.searches.OverridingMethodsSearch;
import com.intellij.psi.util.ClassUtil;
import com.intellij.util.Query;

/**
 * @author VISTALL
 * @since 18.05.2015
 */
public class PlaceXActionAnnotationInspection extends LocalInspectionTool
{
	private static class MyAnnotateMethodFix extends AnnotateMethodFix
	{
		public MyAnnotateMethodFix(String fqn, String... annotationsToRemove)
		{
			super(fqn, annotationsToRemove);
		}

		@Override
		protected boolean annotateOverriddenMethods()
		{
			return true;
		}

		@Override
		public int shouldAnnotateBaseMethod(PsiMethod method, PsiMethod superMethod, Project project)
		{
			return 1;
		}

		@Override
		@NotNull
		public String getName()
		{
			return InspectionsBundle.message("annotate.overridden.methods.as.notnull", ClassUtil.extractClassName(myAnnotation));
		}
	}

	@NotNull
	@Override
	public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder holder, boolean isOnTheFly)
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

				RequiredXActionInspection.ActionType selfActionType = RequiredXActionInspection.ActionType.findSelfActionType(method);
				if(selfActionType != RequiredXActionInspection.ActionType.NONE)
				{
					Query<PsiMethod> query = OverridingMethodsSearch.search(method);
					for(PsiMethod itMethod : query)
					{
						if(RequiredXActionInspection.ActionType.findSelfActionType(itMethod) == RequiredXActionInspection.ActionType.NONE)
						{
							Class<?> actionClass = selfActionType.getActionClass();
							holder.registerProblem(nameIdentifier, "Overriden methods are not annotated by @" + actionClass
									.getSimpleName(), new MyAnnotateMethodFix(actionClass.getName()));
							break;
						}
					}
				}
				else
				{
					PsiMethod[] superMethods = method.findSuperMethods();
					for(PsiMethod superMethod : superMethods)
					{
						RequiredXActionInspection.ActionType superActionType = RequiredXActionInspection.ActionType.findSelfActionType(superMethod);
						if(superActionType != RequiredXActionInspection.ActionType.NONE)
						{
							Class<?> actionClass = superActionType.getActionClass();
							holder.registerProblem(nameIdentifier, "Missed annotation @" + actionClass.getSimpleName() + ", " +
									"provided by super method", new AddAnnotationFix(actionClass.getName(), method));
						}
					}
				}
			}
		};
	}
}
