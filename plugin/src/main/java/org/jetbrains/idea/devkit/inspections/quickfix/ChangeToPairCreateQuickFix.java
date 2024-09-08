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
import com.intellij.java.language.psi.codeStyle.JavaCodeStyleManager;
import consulo.annotation.access.RequiredReadAction;
import consulo.language.editor.inspection.LocalQuickFixBase;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.psi.PsiElement;
import consulo.project.Project;
import consulo.util.lang.Pair;

import javax.annotation.Nonnull;

/**
 * @author Konstantin Bulenkov
 */
public class ChangeToPairCreateQuickFix extends LocalQuickFixBase {
    public ChangeToPairCreateQuickFix() {
        super("Change to Pair.create(..., ...)");
    }

    @Override
    @RequiredReadAction
    public void applyFix(@Nonnull Project project, @Nonnull ProblemDescriptor descriptor) {
        PsiElement element = descriptor.getPsiElement();
        if (element == null) {
            return;
        }
        String text = element.getText();
        String newText = Pair.class.getName() + ".create(" + text.substring(text.indexOf('(') + 1);
        PsiElementFactory factory = JavaPsiFacade.getInstance(project).getElementFactory();
        PsiExpression expression = factory.createExpressionFromText(newText, element.getContext());
        PsiElement newElement = element.replace(expression);
        JavaCodeStyleManager.getInstance(project).shortenClassReferences(newElement);
    }
}