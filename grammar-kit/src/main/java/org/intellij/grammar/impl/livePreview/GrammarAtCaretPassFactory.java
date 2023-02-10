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

package org.intellij.grammar.impl.livePreview;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.ApplicationManager;
import consulo.application.progress.ProgressIndicator;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorColors;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.TextAttributes;
import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.fileEditor.FileEditorManager;
import consulo.language.editor.Pass;
import consulo.language.editor.impl.highlight.TextEditorHighlightingPass;
import consulo.language.editor.impl.highlight.TextEditorHighlightingPassFactory;
import consulo.language.editor.impl.highlight.UpdateHighlightersUtil;
import consulo.language.editor.rawHighlight.HighlightInfo;
import consulo.language.editor.rawHighlight.HighlightInfoType;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.util.collection.ContainerUtil;
import consulo.util.dataholder.Key;
import consulo.util.lang.function.PairProcessor;
import consulo.virtualFileSystem.VirtualFile;
import org.intellij.grammar.psi.BnfExpression;
import org.intellij.grammar.psi.BnfFile;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author gregsh
 */
@ExtensionImpl
public class GrammarAtCaretPassFactory implements TextEditorHighlightingPassFactory {
  public static final Key<Boolean> GRAMMAR_AT_CARET_KEY = Key.create("GRAMMAR_AT_CARET_KEY");

  @Override
  public void register(@Nonnull Registrar registrar) {
    registrar.registerTextEditorHighlightingPass(this, null, new int[]{Pass.UPDATE_ALL}, false, -1);
  }

  @Override
  public TextEditorHighlightingPass createHighlightingPass(@Nonnull final PsiFile file, @Nonnull final Editor editor) {
    if (ApplicationManager.getApplication().isHeadlessEnvironment()) {
      return null;
    }

    if (editor.isOneLineMode()) {
      return null;
    }
    if (!(file instanceof BnfFile)) {
      return null;
    }

    final VirtualFile virtualFile = file.getVirtualFile();
    if (virtualFile == null || !FileEditorManager.getInstance(file.getProject()).isFileOpen(virtualFile)) {
      return null;
    }

    return new TextEditorHighlightingPass(file.getProject(), editor.getDocument(), false) {
      List<HighlightInfo> infos = ContainerUtil.newArrayList();

      @Override
      public void doCollectInformation(@Nonnull ProgressIndicator progress) {
        infos.clear();
        LivePreviewLanguage previewLanguage = LivePreviewLanguage.findInstance(file);
        if (previewLanguage == null) {
          return;
        }
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
        UpdateHighlightersUtil.setHighlightersToEditor(myProject,
                                                       document,
                                                       0,
                                                       file.getTextLength(),
                                                       infos,
                                                       getColorsScheme(),
                                                       getId());
      }
    };
  }

  private static void collectHighlighters(@Nonnull final Project project,
                                          @Nonnull Editor editor,
                                          @Nonnull LivePreviewLanguage livePreviewLanguage,
                                          @Nonnull List<HighlightInfo> result) {
    final Set<TextRange> trueRanges = new HashSet<>();
    final Set<TextRange> falseRanges = new HashSet<>();
    final Set<BnfExpression> visited = new HashSet<>();
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
