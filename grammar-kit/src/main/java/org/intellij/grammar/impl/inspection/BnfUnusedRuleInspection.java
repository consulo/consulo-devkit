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

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.devkit.grammarKit.localize.BnfLocalize;
import consulo.language.editor.inspection.*;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiReference;
import consulo.localize.LocalizeValue;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.JBIterable;
import consulo.util.lang.function.Condition;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.intellij.grammar.KnownAttribute;
import org.intellij.grammar.generator.ParserGeneratorUtil;
import org.intellij.grammar.psi.*;
import org.intellij.grammar.psi.impl.BnfReferenceImpl;
import org.jetbrains.annotations.Nls;

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
@ExtensionImpl
public class BnfUnusedRuleInspection extends LocalInspectionTool {
    @Nonnull
    @Override
    public String getDisplayName() {
        return BnfLocalize.unusedRuleInspectionDisplayName().get();
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
        return BnfLocalize.inspectionsGroupName().get();
    }

    @Override
    public boolean runForWholeFile() {
        return true;
    }

    @Nonnull
    @Override
    public PsiElementVisitor buildVisitor(@Nonnull ProblemsHolder holder, boolean isOnTheFly, @Nonnull LocalInspectionToolSession session, @Nonnull Object state) {
        PsiFile file = holder.getFile();
        if (!(file instanceof BnfFile)) {
            return PsiElementVisitor.EMPTY_VISITOR;
        }

        return new PsiElementVisitor() {
            @Override
            @RequiredReadAction
            public void visitFile(PsiFile file) {
                if (file instanceof BnfFile bnfFile) {
                    checkFile(holder, bnfFile, isOnTheFly);
                }
            }
        };
    }

    @RequiredReadAction
    private void checkFile(@Nonnull ProblemsHolder holder, @Nonnull BnfFile myFile, boolean isOnTheFly) {
        if (SuppressionUtil.inspectionResultSuppressed(myFile, this)) {
            return;
        }

        JBIterable<BnfRule> rules = JBIterable.from(myFile.getRules());
        if (rules.isEmpty()) {
            return;
        }

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
            bnfTraverserNoAttrs(myFile).expand(new Cond<>() {
                @Override
                public boolean value(PsiElement element) {
                    if (element instanceof BnfRule rule) {
                        // add recovery rules to calculation
                        BnfAttr recoverAttr = findAttribute(myFile.getVersion(), rule, KnownAttribute.RECOVER_WHILE);
                        value(recoverAttr == null ? null : recoverAttr.getExpression());
                        return inParsing.contains(rule) || inSuppressed.contains(rule);
                    }
                    else if (element instanceof BnfReferenceOrToken referenceOrToken) {
                        ContainerUtil.addIfNotNull(inParsing, referenceOrToken.resolveRule());
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
            LocalizeValue message = null;
            if (ParserGeneratorUtil.Rule.isFake(r)) {
                if (inExpr.contains(r)) {
                    message = BnfLocalize.unusedRuleInspectionMessageReachableFakeRule();
                }
                else if (!inAttrs.containsKey(r)) {
                    message = BnfLocalize.unusedRuleInspectionMessageUnusedFakeRule();
                }
            }
            else if (getCompatibleAttribute(inAttrs.get(r)) == RECOVER_WHILE) {
                if (!ParserGeneratorUtil.Rule.isPrivate(r)) {
                    message = BnfLocalize.unusedRuleInspectionMessageNonPrivateRecoveryRule();
                }
            }
            else if (!inExpr.contains(r)) {
                message = BnfLocalize.unusedRuleInspectionMessageUnusedRule();
            }
            else if (!inParsing.contains(r)) {
                message = BnfLocalize.unusedRuleInspectionMessageUnreachableRule();
            }
            if (message != null) {
                holder.newProblem(message)
                    .range(r.getId())
                    .create();
            }
        }
    }

    @Nullable
    @RequiredReadAction
    private static BnfRule resolveRule(@Nullable PsiElement o) {
        if (!(o instanceof BnfReferenceOrToken || o instanceof BnfStringLiteralExpression)) {
            return null;
        }
        PsiReference reference = ContainerUtil.findInstance(o.getReferences(), BnfReferenceImpl.class);
        PsiElement target = reference != null ? reference.resolve() : null;
        return target instanceof BnfRule rule ? rule : null;
    }
}