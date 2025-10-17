/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
import consulo.annotation.access.RequiredWriteAction;
import consulo.devkit.localize.DevKitLocalize;
import consulo.language.editor.inspection.LocalQuickFixBase;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.psi.PsiElement;
import consulo.project.Project;
import consulo.ui.ex.JBColor;
import jakarta.annotation.Nonnull;

/**
 * @author Konstantin Bulenkov
 */
public class ConvertToJBColorConstantQuickFix extends LocalQuickFixBase {
    private final String myConstantName;

    public ConvertToJBColorConstantQuickFix(String constantName) {
        super(DevKitLocalize.inspectionUseJBColorQuickfixConstName(constantName));
        myConstantName = constantName;
    }

    @Override
    @RequiredWriteAction
    public void applyFix(@Nonnull Project project, @Nonnull ProblemDescriptor descriptor) {
        PsiElement element = descriptor.getPsiElement();
        PsiElementFactory factory = JavaPsiFacade.getInstance(project).getElementFactory();
        String jbColorConstant = String.format("%s.%s", JBColor.class.getName(), myConstantName);
        PsiExpression expression = factory.createExpressionFromText(jbColorConstant, element.getContext());
        PsiElement newElement = element.replace(expression);
        JavaCodeStyleManager.getInstance(project).shortenClassReferences(newElement);
    }
}
