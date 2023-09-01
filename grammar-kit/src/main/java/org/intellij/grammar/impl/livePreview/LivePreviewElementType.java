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

import consulo.language.ast.IElementType;
import consulo.project.Project;
import consulo.util.lang.Comparing;
import consulo.util.lang.ObjectUtil;
import org.intellij.grammar.psi.BnfFile;
import org.intellij.grammar.psi.BnfRule;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static org.intellij.grammar.impl.livePreview.LivePreviewParserDefinition.KEYWORD;

/**
 * @author gregsh
 */
public class LivePreviewElementType extends IElementType {

  public LivePreviewElementType(@Nonnull String debugName, @Nonnull LivePreviewLanguage language) {
    super(debugName, language, false);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    LivePreviewElementType t = (LivePreviewElementType)o;

    return Comparing.equal(toString(), o.toString()) && getLanguage() == t.getLanguage();
  }

  @Override
  public int hashCode() {
    return 31 * toString().hashCode() + getLanguage().hashCode();
  }



  public static class TokenType extends LivePreviewElementType {
    final IElementType delegate;

    TokenType(@Nullable IElementType delegate, @Nonnull String name, @Nonnull LivePreviewLanguage language) {
      super(name, language);
      this.delegate = ObjectUtil.chooseNotNull(delegate, this);
    }
  }

  public static class KeywordType extends TokenType {
    KeywordType(@Nonnull String name, @Nonnull LivePreviewLanguage language) {
      super(KEYWORD, name, language);
    }
  }

  public static class RuleType extends LivePreviewElementType {
    final String ruleName;

    RuleType(@Nonnull String elementType, @Nonnull BnfRule rule, @Nonnull LivePreviewLanguage language) {
      super(elementType, language);
      ruleName = rule.getName();
    }

    @Nullable
    public BnfRule getRule(Project project) {
      BnfFile file = ((LivePreviewLanguage)getLanguage()).getGrammar(project);
      return file != null ? file.getRule(ruleName) : null;
    }

  }
}
