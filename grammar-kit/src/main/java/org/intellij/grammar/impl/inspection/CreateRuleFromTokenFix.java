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

package org.intellij.grammar.impl.inspection;

import javax.annotation.Nonnull;

import consulo.application.AccessToken;
import consulo.codeEditor.ScrollType;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.TextEditor;
import consulo.language.psi.PsiElement;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.application.WriteAction;
import consulo.codeEditor.Editor;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.project.Project;
import org.intellij.grammar.psi.BnfRule;
import org.intellij.grammar.impl.refactor.BnfIntroduceRuleHandler;

/**
 * Created by IntelliJ IDEA.
 * Date: 8/25/11
 * Time: 7:45 PM
 *
 * @author Vadim Romansky
 */
public class CreateRuleFromTokenFix implements LocalQuickFix {
    private final String myName;

    public CreateRuleFromTokenFix(String name) {
        myName = name;
    }

    @Nonnull
    @Override
    public String getName() {
        return "Create '" + myName + "' rule";
    }

    @Nonnull
    @Override
    public String getFamilyName() {
        return "Create rule from usage";
    }

    @Override
    public void applyFix(final @Nonnull Project project, @Nonnull ProblemDescriptor descriptor) {
        final AccessToken token = WriteAction.start();
        try {
            final PsiElement element = descriptor.getPsiElement();
            final BnfRule rule = PsiTreeUtil.getParentOfType(element, BnfRule.class);
            if (rule == null) {
                return;
            }

            final BnfRule addedRule = BnfIntroduceRuleHandler.addNextRule(project, rule, "private " + myName + " ::= ");
            final FileEditor selectedEditor =
                FileEditorManager.getInstance(project).getSelectedEditor(rule.getContainingFile().getVirtualFile());
            if (selectedEditor instanceof TextEditor textEditor) {
                final Editor editor = textEditor.getEditor();
                editor.getCaretModel().moveToOffset(
                    addedRule.getTextRange().getEndOffset() - (BnfIntroduceRuleHandler.endsWithSemicolon(addedRule) ? 1 : 0)
                );
                editor.getScrollingModel().scrollToCaret(ScrollType.MAKE_VISIBLE);
            }
        }
        finally {
            token.finish();
        }
    }
}
