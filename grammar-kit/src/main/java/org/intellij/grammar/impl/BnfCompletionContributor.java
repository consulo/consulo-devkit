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

package org.intellij.grammar.impl;

import com.intellij.java.language.psi.util.PsiUtil;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.document.util.TextRange;
import consulo.language.Language;
import consulo.language.ast.ASTNode;
import consulo.language.editor.completion.CompletionContributor;
import consulo.language.editor.completion.CompletionInitializationContext;
import consulo.language.editor.completion.CompletionType;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.language.editor.completion.lookup.TailType;
import consulo.language.editor.completion.lookup.TailTypeDecorator;
import consulo.language.impl.ast.TreeUtil;
import consulo.language.impl.parser.GeneratedParserUtilBase;
import consulo.language.pattern.PlatformPatterns;
import consulo.language.pattern.PsiElementPattern;
import consulo.language.psi.*;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.StringUtil;
import org.intellij.grammar.BnfIcons;
import org.intellij.grammar.BnfLanguage;
import org.intellij.grammar.KnownAttribute;
import org.intellij.grammar.generator.RuleGraphHelper;
import org.intellij.grammar.psi.*;
import org.intellij.grammar.psi.impl.GrammarUtil;
import org.jetbrains.annotations.Contract;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Set;

import static consulo.language.pattern.PlatformPatterns.psiElement;
import static org.intellij.grammar.psi.BnfTypes.BNF_ID;

/**
 * @author gregsh
 */
@ExtensionImpl(order = "before javaClassName")
public class BnfCompletionContributor extends CompletionContributor {
  public BnfCompletionContributor() {
    PsiElementPattern.Capture<PsiElement> placePattern =
      psiElement().inFile(PlatformPatterns.instanceOf(BnfFile.class)).andNot(psiElement().inside(PsiComment.class));
    extend(CompletionType.BASIC, placePattern, (parameters, context, result) -> {
      PsiElement position = parameters.getPosition();
      BnfComposite parent = PsiTreeUtil.getParentOfType(position, BnfAttrs.class, BnfAttr.class, BnfParenExpression.class);
      boolean attrCompletion;
      if (parent instanceof BnfAttrs || isPossibleEmptyAttrs(parent)) {
        attrCompletion = true;
      }
      else if (parent instanceof BnfAttr attr) {
        attrCompletion = position == attr.getId() || isOneAfterAnother(attr.getExpression(), position);
      }
      else {
        attrCompletion = false;
      }
      if (attrCompletion) {
        boolean inRule = PsiTreeUtil.getParentOfType(parent, BnfRule.class) != null;
        ASTNode closingBrace = TreeUtil.findSiblingBackward(parent.getNode().getLastChildNode(), BnfTypes.BNF_RIGHT_BRACE);
        attrCompletion = closingBrace == null || position.getTextOffset() <= closingBrace.getStartOffset();
        if (attrCompletion) {
          for (KnownAttribute attribute : KnownAttribute.getAttributes()) {
            if (inRule && attribute.isGlobal()) {
              continue;
            }
            result.addElement(LookupElementBuilder.create(attribute.getName()).withIcon(BnfIcons.ATTRIBUTE));
          }
        }
      }
      if (!attrCompletion && parameters.getInvocationCount() < 2) {
        for (String keywords : suggestKeywords(parameters.getPosition())) {
          result.addElement(TailTypeDecorator.withTail(LookupElementBuilder.create(keywords), TailType.SPACE));
        }
      }
    });
    extend(CompletionType.BASIC, placePattern.andNot(psiElement().inside(false, psiElement(BnfAttr.class))), (parameters, context, result) -> {
      BnfFile file = (BnfFile)parameters.getOriginalFile();
      PsiElement
        positionRefOrToken = PsiTreeUtil.getParentOfType(parameters.getOriginalPosition(), BnfReferenceOrToken.class);
      Set<String> explicitTokens = RuleGraphHelper.getTokenNameToTextMap(file).keySet();
      for (String s : explicitTokens) {
        result.addElement(LookupElementBuilder.create(s));
      }
      for (BnfRule rule : file.getRules()) {
        for (BnfReferenceOrToken element : SyntaxTraverser.psiTraverser(rule.getExpression()).filter(BnfReferenceOrToken.class)) {
          if (element == positionRefOrToken) {
            continue;
          }
          if (element.resolveRule() == null) {
            result.addElement(LookupElementBuilder.create(element.getText()));
          }
        }
      }
    });
  }

  @Override
  public void beforeCompletion(@Nonnull CompletionInitializationContext context) {
    BnfFile file = ObjectUtil.tryCast(context.getFile(), BnfFile.class);
    if (file == null) {
      return;
    }
    int offset = context.getStartOffset();
    PsiElement element = file.findElementAt(offset);
    if (PsiUtil.getElementType(element) == BNF_ID) {
      context.setDummyIdentifier("");
    }
  }

  @Contract("null -> false")
  private static boolean isPossibleEmptyAttrs(PsiElement attrs) {
    if (!(attrs instanceof BnfParenExpression)) {
      return false;
    }
    if (attrs.getFirstChild().getNode().getElementType() != BnfTypes.BNF_LEFT_BRACE) {
      return false;
    }
    if (!(((BnfParenExpression)attrs).getExpression() instanceof BnfReferenceOrToken)) {
      return false;
    }
    return isLastInRuleOrFree(attrs);
  }

  @RequiredReadAction
  private static boolean isOneAfterAnother(@Nullable PsiElement e1, @Nullable PsiElement e2) {
    return !(e1 == null || e2 == null)
      && e1.getTextRange().getEndOffset() < e2.getTextRange().getStartOffset();
  }

  @RequiredReadAction
  private static boolean isLastInRuleOrFree(PsiElement element) {
    PsiElement parent = PsiTreeUtil.getParentOfType(element, BnfRule.class, GeneratedParserUtilBase.DummyBlock.class);
    if (parent instanceof GeneratedParserUtilBase.DummyBlock) {
      return true;
    }
    if (!(parent instanceof BnfRule)) {
      return false;
    }
    for (PsiElement cur = element, next = cur.getNextSibling(); next == null || next instanceof PsiComment || next instanceof PsiWhiteSpace;
         cur = next, next = cur.getNextSibling()) {
      if (next == null) {
        PsiElement curParent = cur.getParent();
        while (next == null && curParent != parent) {
          next = curParent.getNextSibling();
          curParent = curParent.getParent();
        }
        if (curParent == parent) {
          return true;
        }
        next = PsiTreeUtil.getDeepestFirst(next);
      }
    }
    return false;
  }

  @RequiredReadAction
  private static Collection<String> suggestKeywords(PsiElement position) {
    TextRange posRange = position.getTextRange();
    BnfFile posFile = (BnfFile)position.getContainingFile();
    BnfRule statement = PsiTreeUtil.getTopmostParentOfType(position, BnfRule.class);
    final TextRange range;
    if (statement != null) {
      range = new TextRange(statement.getTextRange().getStartOffset(), posRange.getStartOffset());
    }
    else {
      int offset = posRange.getStartOffset();
      for (PsiElement cur = GrammarUtil.getDummyAwarePrevSibling(position); cur != null; cur = GrammarUtil.getDummyAwarePrevSibling(cur)) {
        if (cur instanceof BnfAttrs) {
          offset = cur.getTextRange().getEndOffset();
        }
        else if (cur instanceof BnfRule) {
          offset = cur.getTextRange().getStartOffset();
        }
        else {
          continue;
        }
        break;
      }
      range = new TextRange(offset, posRange.getStartOffset());
    }
    String headText = range.substring(posFile.getText());
    int completionOffset = StringUtil.isEmptyOrSpaces(headText) ? 0 : headText.length();
    String text = completionOffset == 0 ? CompletionInitializationContext.DUMMY_IDENTIFIER : headText;

    GeneratedParserUtilBase.CompletionState state = new GeneratedParserUtilBase.CompletionState(completionOffset) {
      @Override
      public String convertItem(Object o) {
        // we do not have other keywords
        return o instanceof String str ? str : null;
      }
    };
    PsiFileFactory psiFileFactory = PsiFileFactory.getInstance(posFile.getProject());
    PsiFile file = psiFileFactory.createFileFromText("a.bnf", BnfLanguage.INSTANCE, text, true, false);
    file.putUserData(GeneratedParserUtilBase.COMPLETION_STATE_KEY, state);
    TreeUtil.ensureParsed(file.getNode());

    if (completionOffset != 0) {
      TextRange altRange = TextRange.create(posRange.getEndOffset(), Math.min(posRange.getEndOffset() + 100, posFile.getTextLength()));
      String tailText = altRange.substring(posFile.getText());
      String text2 = text + (StringUtil.isEmptyOrSpaces(tailText) ? "a ::= " : tailText);
      PsiFile file2 = psiFileFactory.createFileFromText("a.bnf", BnfLanguage.INSTANCE, text2, true, false);
      file2.putUserData(GeneratedParserUtilBase.COMPLETION_STATE_KEY, state);
      TreeUtil.ensureParsed(file2.getNode());
    }
    return state.items;
  }

  @Nonnull
  @Override
  public Language getLanguage() {
    return BnfLanguage.INSTANCE;
  }
}
