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

package consulo.devkit.inspections.requiredXAction;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.devkit.DevKitBundle;
import com.intellij.codeInspection.AnnotateMethodFix;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiCall;
import com.intellij.psi.PsiCallExpression;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiMethod;
import com.intellij.util.ThreeState;
import consulo.annotations.RequiredDispatchThread;
import consulo.devkit.inspections.requiredXAction.stateResolver.AnonymousClassStateResolver;
import consulo.devkit.inspections.requiredXAction.stateResolver.LambdaStateResolver;
import consulo.devkit.inspections.requiredXAction.stateResolver.StateResolver;
import consulo.ui.RequiredUIAccess;

/**
 * @author VISTALL
 * @since 18.02.2015
 */
public class RequiredXActionInspection extends LocalInspectionTool
{
	public static class RequiredXActionVisitor extends JavaElementVisitor
	{
		private static StateResolver[] ourStateResolvers = new StateResolver[]{
				new AnonymousClassStateResolver(),
				new LambdaStateResolver()
		};

		private final ProblemsHolder myHolder;

		public RequiredXActionVisitor(ProblemsHolder holder)
		{
			myHolder = holder;
		}

		@Override
		public void visitCallExpression(PsiCallExpression expression)
		{
			PsiMethod psiMethod = expression.resolveMethod();
			if(psiMethod == null)
			{
				return;
			}

			CallStateType actionType = CallStateType.findActionType(psiMethod);
			if(actionType == CallStateType.NONE)
			{
				return;
			}

			for(StateResolver stateResolver : ourStateResolvers)
			{
				Pair<ThreeState, CallStateType> handled = stateResolver.resolveState(actionType, expression);
				if(handled.getFirst() != ThreeState.NO)
				{
					return;
				}
			}

			reportError(expression, actionType);
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
					quickFixes = new LocalQuickFix[]{new AnnotateMethodFix(RequiredUIAccess.class.getName())};
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
