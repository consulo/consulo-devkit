/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package org.jetbrains.idea.devkit.inspections.internal;

import java.util.Arrays;

import javax.annotation.Nonnull;

import org.jetbrains.idea.devkit.inspections.quickfix.ChangeToPairCreateQuickFix;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiExpressionList;
import com.intellij.psi.PsiNewExpression;
import com.intellij.psi.PsiType;

/**
 * @author Konstantin Bulenkov
 */
public class DontUseNewPairInspection extends InternalInspection
{
	private static final String PAIR_FQN = "com.intellij.openapi.util.Pair";

	@Override
	public PsiElementVisitor buildInternalVisitor(@Nonnull final ProblemsHolder holder, boolean isOnTheFly)
	{
		return new JavaElementVisitor()
		{
			@Override
			public void visitNewExpression(PsiNewExpression expression)
			{
				final PsiType type = expression.getType();
				final PsiExpressionList params = expression.getArgumentList();
				if(type instanceof PsiClassType && ((PsiClassType) type).rawType().equalsToText(PAIR_FQN) && params != null && expression.getArgumentList() != null
					//&& !PsiUtil.getLanguageLevel(expression).isAtLeast(LanguageLevel.JDK_1_7) //diamonds
						)
				{
					final PsiType[] types = ((PsiClassType) type).getParameters();
					if(Arrays.equals(types, params.getExpressionTypes()))
					{
						holder.registerProblem(expression, "Replace to Pair.create()", ProblemHighlightType.GENERIC_ERROR_OR_WARNING, new ChangeToPairCreateQuickFix());
					}
				}
				super.visitNewExpression(expression);
			}
		};
	}

	@Nonnull
	@Override
	public String getShortName()
	{
		return "DontUsePairConstructor";
	}
}
