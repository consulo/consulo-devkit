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

import consulo.application.dumb.DumbAware;
import consulo.document.util.TextRange;
import consulo.language.editor.annotation.AnnotationHolder;
import consulo.language.editor.annotation.Annotator;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.psi.PsiElement;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.util.lang.Pair;
import org.intellij.grammar.KnownAttribute;
import org.intellij.grammar.psi.BnfAttr;
import org.intellij.grammar.psi.BnfExpression;
import org.intellij.grammar.psi.BnfFile;
import org.intellij.grammar.psi.BnfRule;
import org.intellij.grammar.psi.impl.GrammarUtil;

import javax.annotation.Nonnull;
import java.util.ArrayList;

/**
 * @author gregsh
 */
public class BnfPinMarkerAnnotator implements Annotator, DumbAware {
  @Override
  public void annotate(@Nonnull PsiElement psiElement, @Nonnull AnnotationHolder annotationHolder) {
    if (!(psiElement instanceof BnfRule)) return;
    BnfRule rule = (BnfRule) psiElement;
    final BnfFile bnfFile = (BnfFile)rule.getContainingFile();
    final ArrayList<Pair<BnfExpression, BnfAttr>> pinned = new ArrayList<Pair<BnfExpression, BnfAttr>>();
    GrammarUtil.processPinnedExpressions(rule, (bnfExpression, pinMatcher) -> {
      BnfAttr attr = bnfFile.findAttribute(null, pinMatcher.rule, KnownAttribute.PIN, pinMatcher.funcName);
      return pinned.add(Pair.create(bnfExpression, attr));
    });
    for (int i = 0, len = pinned.size(); i < len; i++) {
      BnfExpression e = pinned.get(i).first;
      BnfExpression prev = i == 0? null : pinned.get(i - 1).first;
      BnfAttr attr = pinned.get(i).second;
      boolean fullRange = prev == null || !PsiTreeUtil.isAncestor(e, prev, true);
      TextRange textRange = e.getTextRange();
      TextRange infoRange = fullRange ? textRange : TextRange.create(prev.getTextRange().getEndOffset() + 1, textRange.getEndOffset());
      String message = attr == null ? (fullRange ? "pinned" : "pinned again") : attr.getText();

      annotationHolder.newAnnotation(HighlightSeverity.INFORMATION, message)
                      .range(infoRange)
                      .textAttributes(BnfSyntaxHighlighter.PIN_MARKER)
                      .create();
    }
  }

}
