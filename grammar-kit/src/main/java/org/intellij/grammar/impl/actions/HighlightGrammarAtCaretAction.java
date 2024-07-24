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

package org.intellij.grammar.impl.actions;

import consulo.codeEditor.Editor;
import consulo.language.Language;
import consulo.language.editor.DaemonCodeAnalyzer;
import consulo.language.editor.LangDataKeys;
import consulo.language.editor.PlatformDataKeys;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.util.lang.ObjectUtil;
import org.intellij.grammar.impl.livePreview.GrammarAtCaretPassFactory;
import org.intellij.grammar.impl.livePreview.LivePreviewLanguage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * @author gregsh
 */
public class HighlightGrammarAtCaretAction extends AnAction {

    @Nullable
    private static Editor getPreviewEditor(@Nonnull AnActionEvent e) {
        Editor editor = e.getData(PlatformDataKeys.EDITOR);
        PsiFile psiFile = e.getData(LangDataKeys.PSI_FILE);
        Language language = psiFile == null ? null : psiFile.getLanguage();
        LivePreviewLanguage livePreviewLanguage = language instanceof LivePreviewLanguage ? (LivePreviewLanguage)language : null;
        if (livePreviewLanguage == null) {
            return null;
        }
        List<Editor> editors = livePreviewLanguage.getGrammarEditors(psiFile.getProject());
        return editors.isEmpty() ? null : editor;
    }

    @Override
    @RequiredUIAccess
    public void update(@Nonnull AnActionEvent e) {
        Editor editor = getPreviewEditor(e);
        boolean enabled = editor != null;
        String command = !enabled ? "" : GrammarAtCaretPassFactory.GRAMMAR_AT_CARET_KEY.get(editor) != null ? "Stop " : "Start ";
        e.getPresentation().setText(command + getTemplatePresentation().getText());
        e.getPresentation().setEnabledAndVisible(enabled);
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        Editor editor = getPreviewEditor(e);
        if (editor == null) {
            return;
        }
        Boolean value = GrammarAtCaretPassFactory.GRAMMAR_AT_CARET_KEY.get(editor);
        GrammarAtCaretPassFactory.GRAMMAR_AT_CARET_KEY.set(editor, value == null ? Boolean.TRUE : null);

        Project project = ObjectUtil.assertNotNull(e.getData(Project.KEY));
        DaemonCodeAnalyzer.getInstance(project).restart();
    }
}
