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

package org.intellij.grammar.impl.editor;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.AllIcons;
import consulo.application.progress.ProgressManager;
import consulo.codeEditor.markup.GutterIconRenderer;
import consulo.language.Language;
import consulo.language.editor.Pass;
import consulo.language.editor.gutter.LineMarkerInfo;
import consulo.language.editor.gutter.LineMarkerProvider;
import consulo.language.psi.PsiElement;
import consulo.ui.ex.action.AnAction;
import org.intellij.grammar.BnfLanguage;
import org.intellij.grammar.generator.RuleGraphHelper;
import org.intellij.grammar.psi.BnfFile;
import org.intellij.grammar.psi.BnfRule;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author gregsh
 */
@ExtensionImpl
public class BnfRecursionLineMarkerProvider implements LineMarkerProvider {
  @Nullable
  @Override
  public LineMarkerInfo getLineMarkerInfo(@Nonnull PsiElement element) {
    return null;
  }

  @Override
  public void collectSlowLineMarkers(@Nonnull List<PsiElement> elements, @Nonnull Collection<LineMarkerInfo> result) {
    for (PsiElement element : elements) {
      if (!(element instanceof BnfRule)) continue;
      BnfRule rule = (BnfRule)element;

      ProgressManager.checkCanceled();

      RuleGraphHelper helper = RuleGraphHelper.getCached((BnfFile)rule.getContainingFile());
      Map<PsiElement, RuleGraphHelper.Cardinality> map = helper.getFor(rule);
      if (map.containsKey(rule)) {
        result.add(new MyMarkerInfo(rule));
      }
    }
  }

  @Nonnull
  @Override
  public Language getLanguage() {
    return BnfLanguage.INSTANCE;
  }

  private static class MyMarkerInfo extends LineMarkerInfo<BnfRule> {
    private MyMarkerInfo(@Nonnull BnfRule rule) {
      super(rule,
            rule.getTextRange(),
            AllIcons.Gutter.RecursiveMethod,
            Pass.LINE_MARKERS,
            (e) -> "Recursive rule",
            null,
            GutterIconRenderer.Alignment.RIGHT
      );
    }

    @Override
    public GutterIconRenderer createGutterRenderer() {
      if (myIcon == null) return null;
      return new LineMarkerGutterIconRenderer<BnfRule>(this) {
        @Override
        public AnAction getClickAction() {
          return null;
        }
      };
    }
  }
}
