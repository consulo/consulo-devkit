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

package org.intellij.grammar.livePreview;

import java.util.List;
import java.util.Set;

import org.intellij.grammar.psi.BnfExpression;
import org.intellij.grammar.psi.BnfFile;
import org.jetbrains.annotations.NotNull;
import com.intellij.codeHighlighting.Pass;
import com.intellij.codeHighlighting.TextEditorHighlightingPass;
import com.intellij.codeHighlighting.TextEditorHighlightingPassFactory;
import com.intellij.codeHighlighting.TextEditorHighlightingPassRegistrar;
import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.daemon.impl.HighlightInfoType;
import com.intellij.codeInsight.daemon.impl.UpdateHighlightersUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.AbstractProjectComponent;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.PairProcessor;
import com.intellij.util.containers.ContainerUtil;

/**
 * @author gregsh
 */
public class GrammarAtCaretPassFactory extends AbstractProjectComponent implements TextEditorHighlightingPassFactory {
  public static final Key<Boolean> GRAMMAR_AT_CARET_KEY = Key.create("GRAMMAR_AT_CARET_KEY");

  public GrammarAtCaretPassFactory(Project project, TextEditorHighlightingPassRegistrar highlightingPassRegistrar) {
    super(project);
    highlightingPassRegistrar.registerTextEditorHighlightingPass(this, null, new int[]{Pass.UPDATE_ALL}, false, -1);
  }

  @Override
  public TextEditorHighlightingPass createHighlightingPass(@NotNull final PsiFile file, @NotNull final Editor editor) {
    if (ApplicationManager.getApplication().isHeadlessEnvironment()) return null;

    if (editor.isOneLineMode()) return null;
    if (!(file instanceof BnfFile)) return null;

    final VirtualFile virtualFile = file.getVirtualFile();
    if (virtualFile == null || !FileEditorManager.getInstance(myProject).isFileOpen(virtualFile)) return null;

    return new TextEditorHighlightingPass(file.getProject(), editor.getDocument(), false) {
      List<HighlightInfo> infos = ContainerUtil.newArrayList();

      @Override
      public void doCollectInformation(@NotNull ProgressIndicator progress) {
        infos.clear();
        LivePreviewLanguage previewLanguage = LivePreviewLanguage.findInstance(file);
        if (previewLanguage == null) return;
        List<Editor> previewEditors = previewLanguage.getPreviewEditors(myProject);
        for (Editor e : previewEditors) {
          if (Boolean.TRUE.equals(GRAMMAR_AT_CARET_KEY.get(e))) {
            collectHighlighters(myProject, previewEditors.get(0), previewLanguage, infos);
          }
        }
      }

      @Override
      public void doApplyInformationToEditor() {
        Document document = editor.getDocument();
        UpdateHighlightersUtil.setHighlightersToEditor(myProject, document, 0, file.getTextLength(), infos, getColorsScheme(), getId());
      }
    };
  }

  private static void collectHighlighters(@NotNull final Project project,
                                          @NotNull Editor editor,
                                          @NotNull LivePreviewLanguage livePreviewLanguage,
                                          @NotNull List<HighlightInfo> result) {
    final Set<TextRange> trueRanges = ContainerUtil.newHashSet();
    final Set<TextRange> falseRanges = ContainerUtil.newHashSet();
    final Set<BnfExpression> visited = ContainerUtil.newHashSet();
    LivePreviewHelper.collectExpressionsAtOffset(project, editor, livePreviewLanguage, new PairProcessor<BnfExpression, Boolean>() {
        @Override
        public boolean process(BnfExpression bnfExpression, Boolean result) {
          for (PsiElement parent = bnfExpression.getParent();
               parent instanceof BnfExpression && visited.add((BnfExpression)parent); ) {
            parent = parent.getParent();
          }
          if (visited.add(bnfExpression)) {
            (result ? trueRanges : falseRanges).add(bnfExpression.getTextRange());
          }
          return true;
        }
      });
    createHighlights(trueRanges, falseRanges, result);
  }

  private static void createHighlights(Set<TextRange> trueRanges,
                                       Set<TextRange> falseRanges,
                                       List<HighlightInfo> result) {
    EditorColorsManager manager = EditorColorsManager.getInstance();
    TextAttributes trueAttrs = manager.getGlobalScheme().getAttributes(EditorColors.SEARCH_RESULT_ATTRIBUTES);
    TextAttributes falseAttrs = manager.getGlobalScheme().getAttributes(EditorColors.WRITE_SEARCH_RESULT_ATTRIBUTES);

    for (TextRange range : trueRanges) {
      HighlightInfo info = HighlightInfo.newHighlightInfo(HighlightInfoType.INFORMATION)
        .range(range)
        .textAttributes(trueAttrs)
        .createUnconditionally();
      result.add(info);
    }
    for (TextRange range : falseRanges) {
      HighlightInfo info = HighlightInfo.newHighlightInfo(HighlightInfoType.INFORMATION)
        .range(range)
        .textAttributes(falseAttrs)
        .createUnconditionally();
      result.add(info);
    }
  }

}
