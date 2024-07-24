/*
 * Copyright 2011-present Greg Shrago
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

package org.intellij.grammar.impl.intention;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.language.editor.intention.IntentionAction;
import consulo.language.editor.intention.IntentionMetaData;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.util.IncorrectOperationException;
import consulo.project.Project;
import consulo.util.lang.Pair;
import org.intellij.grammar.psi.BnfChoice;
import org.intellij.grammar.psi.BnfExpression;
import org.intellij.grammar.psi.impl.BnfElementFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Vadim Romansky
 * @author gregsh
 */
@ExtensionImpl
@IntentionMetaData(ignoreId = "bnf.flip.choice", fileExtensions = "bnf", categories = "Grammar/BNF")
public class BnfFlipChoiceIntention implements IntentionAction {
    @Nonnull
    @Override
    public String getText() {
        return "Flip arguments";
    }

    @Override
    public boolean isAvailable(@Nonnull Project project, Editor editor, PsiFile file) {
        return getArguments(file, editor.getCaretModel().getOffset()) != null;
    }

    @Override
    public void invoke(@Nonnull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        final Pair<PsiElement, PsiElement> arguments = getArguments(file, editor.getCaretModel().getOffset());
      if (arguments == null) {
        return;
      }
        PsiElement newFirst = BnfElementFactory.createRuleFromText(project, "a ::=" + arguments.second.getText()).getExpression();
        PsiElement newSecond = BnfElementFactory.createRuleFromText(project, "a ::=" + arguments.first.getText()).getExpression();
        arguments.second.replace(newSecond);
        arguments.first.replace(newFirst);
    }

    @Nullable
    private static Pair<PsiElement, PsiElement> getArguments(PsiFile file, int offset) {
        PsiElement element = file.getViewProvider().findElementAt(offset);
        final BnfChoice choice = PsiTreeUtil.getParentOfType(element, BnfChoice.class);
        if (choice == null) {
            return null;
        }
        for (PsiElement cur = choice.getFirstChild(), prev = null; cur != null; cur = cur.getNextSibling()) {
            if (!(cur instanceof BnfExpression)) {
                continue;
            }
            int start = prev == null ? choice.getTextRange().getStartOffset() : prev.getTextRange().getEndOffset();
            int end = cur.getTextRange().getStartOffset();
            if (start <= offset && offset <= end) {
                return prev == null ? null : Pair.create(cur, prev);
            }
            prev = cur;
        }
        return null;
    }

    @Override
    public boolean startInWriteAction() {
        return true;
    }
}
