/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package org.jetbrains.idea.devkit.inspections.quickfix;

import javax.annotation.Nonnull;
import com.intellij.codeInspection.LocalQuickFixBase;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;

/**
 * @author Konstantin Bulenkov
 */
public class ChangeToPairCreateQuickFix extends LocalQuickFixBase
{
	public ChangeToPairCreateQuickFix()
	{
		super("Change to Pair.create(..., ...)");
	}

	@Override
	public void applyFix(@Nonnull Project project, @Nonnull ProblemDescriptor descriptor)
	{
		PsiElement element = descriptor.getPsiElement();
		if(element == null)
		{
			return;
		}
		String text = element.getText();
		String newText = "com.intellij.openapi.util.Pair.create(" + text.substring(text.indexOf('(') + 1);
		PsiElementFactory factory = JavaPsiFacade.getInstance(project).getElementFactory();
		PsiExpression expression = factory.createExpressionFromText(newText, element.getContext());
		PsiElement newElement = element.replace(expression);
		JavaCodeStyleManager.getInstance(project).shortenClassReferences(newElement);
	}
}