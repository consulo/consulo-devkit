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

import com.intellij.java.language.psi.JavaPsiFacade;
import com.intellij.java.language.psi.PsiElementFactory;
import com.intellij.java.language.psi.PsiExpression;
import com.intellij.java.language.psi.PsiTypeElement;
import com.intellij.java.language.psi.codeStyle.JavaCodeStyleManager;
import consulo.language.editor.inspection.LocalQuickFixBase;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.psi.PsiElement;
import consulo.project.Project;

import javax.annotation.Nonnull;

/**
 * @author Konstantin Bulenkov
 */
public class UseCoupleQuickFix extends LocalQuickFixBase {
  private static final String COUPLE_FQN = "com.intellij.openapi.util.Couple";

  public UseCoupleQuickFix(String text) {
    super(text);
  }

  @Override
  public void applyFix(@Nonnull Project project, @Nonnull ProblemDescriptor descriptor) {
    final PsiElement element = descriptor.getPsiElement();
    final PsiElementFactory factory = JavaPsiFacade.getInstance(project).getElementFactory();
    final PsiElement newElement;
    if (element instanceof PsiTypeElement) {
      final String canonicalText = ((PsiTypeElement)element).getType().getCanonicalText();
      final String type = canonicalText.substring(canonicalText.indexOf('<') + 1, canonicalText.indexOf(','));
      final PsiTypeElement newType = factory.createTypeElementFromText(COUPLE_FQN + "<" + type + ">", element.getContext());
      newElement = element.replace(newType);
    }
    else {
      final String text = COUPLE_FQN + ".of" + element.getText().substring("Pair.create".length());
      final PsiExpression expression = factory.createExpressionFromText(text, element.getContext());
      newElement = element.replace(expression);
    }
    JavaCodeStyleManager.getInstance(project).shortenClassReferences(newElement);
  }
}