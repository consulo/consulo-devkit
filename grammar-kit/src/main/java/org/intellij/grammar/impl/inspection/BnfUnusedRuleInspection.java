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

import consulo.language.editor.inspection.LocalInspectionTool;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.inspection.SuppressionUtil;
import consulo.language.editor.inspection.scheme.InspectionManager;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiReference;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.JBIterable;
import consulo.util.lang.function.Condition;
import org.intellij.grammar.KnownAttribute;
import org.intellij.grammar.generator.ParserGeneratorUtil;
import org.intellij.grammar.psi.*;
import org.intellij.grammar.psi.impl.BnfReferenceImpl;
import org.jetbrains.annotations.Nls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static consulo.util.lang.function.Condition.NOT_NULL;
import static org.intellij.grammar.KnownAttribute.RECOVER_WHILE;
import static org.intellij.grammar.KnownAttribute.getCompatibleAttribute;
import static org.intellij.grammar.generator.ParserGeneratorUtil.findAttribute;
import static org.intellij.grammar.psi.impl.GrammarUtil.bnfTraverser;
import static org.intellij.grammar.psi.impl.GrammarUtil.bnfTraverserNoAttrs;

/**
 * @author gregsh
 */
public class BnfUnusedRuleInspection extends LocalInspectionTool {
  @Nonnull
  @Override
  public String getDisplayName() {
    return "Unused rule";
  }

  @Nonnull
  @Override
  public HighlightDisplayLevel getDefaultLevel() {
    return HighlightDisplayLevel.WARNING;
  }

  @Nls
  @Nonnull
  @Override
  public String getGroupDisplayName() {
    return "Grammar/BNF";
  }

  @Override
  public boolean runForWholeFile() {
    return true;
  }

  @Nullable
  @Override
  public ProblemDescriptor[] checkFile(@Nonnull PsiFile file, @Nonnull InspectionManager manager, boolean isOnTheFly) {
    if (!(file instanceof BnfFile)) {
      return null;
    }
    if (SuppressionUtil.inspectionResultSuppressed(file, this)) {
      return null;
    }
    BnfFile myFile = (BnfFile)file;
    JBIterable<BnfRule> rules = JBIterable.from(myFile.getRules());
    if (rules.isEmpty()) {
      return null;
    }

    ProblemsHolder
      holder = new ProblemsHolder(manager, file, isOnTheFly);

    //noinspection LimitedScopeInnerClass,EmptyClass
    abstract class Cond<T> extends JBIterable.Stateful<Cond> implements Condition<T> {
    }

    Set<BnfRule> inExpr = new HashSet<>();
    Set<BnfRule> inParsing = new HashSet<>();
    Set<BnfRule> inSuppressed = new HashSet<>();
    Map<BnfRule, String> inAttrs = new HashMap<>();

    bnfTraverserNoAttrs(myFile).traverse().map(BnfUnusedRuleInspection::resolveRule).filter(NOT_NULL).addAllTo(inExpr);

    rules.filter(r -> SuppressionUtil.inspectionResultSuppressed(r, this)).addAllTo(inSuppressed);

    inParsing.add(rules.first()); // add root rule
    for (int size = 0, prev = -1; size != prev; prev = size, size = inParsing.size()) {
      bnfTraverserNoAttrs(myFile).expand(new Cond<PsiElement>() {
        @Override
        public boolean value(PsiElement element) {
          if (element instanceof BnfRule) {
            BnfRule rule = (BnfRule)element;
            // add recovery rules to calculation
            BnfAttr recoverAttr = findAttribute(myFile.getVersion(), rule, KnownAttribute.RECOVER_WHILE);
            value(recoverAttr == null ? null : recoverAttr.getExpression());
            return inParsing.contains(rule) || inSuppressed.contains(rule);
          }
          else if (element instanceof BnfReferenceOrToken) {
            ContainerUtil.addIfNotNull(inParsing, ((BnfReferenceOrToken)element).resolveRule());
            return false;
          }
          return true;
        }
      }).traverse().size();
    }

    for (BnfAttr attr : bnfTraverser(myFile).filter(BnfAttr.class)) {
      BnfRule target = resolveRule(attr.getExpression());
      if (target != null) {
        inAttrs.put(target, attr.getName());
      }
    }

    for (BnfRule r : rules.skip(1).filter(o -> !inSuppressed.contains(o))) {
      String message = null;
      if (ParserGeneratorUtil.Rule.isFake(r)) {
        if (inExpr.contains(r)) {
          message = "Reachable fake rule";
        }
        else if (!inAttrs.containsKey(r)) {
          message = "Unused fake rule";
        }
      }
      else if (getCompatibleAttribute(inAttrs.get(r)) == RECOVER_WHILE) {
        if (!ParserGeneratorUtil.Rule.isPrivate(r)) {
          message = "Non-private recovery rule";
        }
      }
      else if (!inExpr.contains(r)) {
        message = "Unused rule";
      }
      else if (!inParsing.contains(r)) {
        message = "Unreachable rule";
      }
      if (message != null) {
        holder.registerProblem(r.getId(), message);
      }
    }
    return holder.getResultsArray();
  }

  @Nullable
  private static BnfRule resolveRule(@Nullable PsiElement o) {
    if (!(o instanceof BnfReferenceOrToken || o instanceof BnfStringLiteralExpression)) {
      return null;
    }
    PsiReference reference = ContainerUtil.findInstance(o.getReferences(), BnfReferenceImpl.class);
    PsiElement target = reference != null ? reference.resolve() : null;
    return target instanceof BnfRule ? (BnfRule)target : null;
  }
}