/*
 * Copyright 2011-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.intellij.grammar.psi.impl;

import consulo.application.util.function.Processor;
import consulo.language.Language;
import consulo.language.ast.IElementType;
import consulo.language.impl.parser.GeneratedParserUtilBase;
import consulo.language.impl.psi.LeafPsiElement;
import consulo.language.psi.PsiElement;
import consulo.language.psi.SyntaxTraverser;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.util.collection.SmartList;
import consulo.util.lang.StringUtil;
import consulo.util.lang.function.Conditions;
import consulo.util.lang.function.PairProcessor;
import consulo.util.lang.ref.Ref;
import org.intellij.grammar.KnownAttribute;
import org.intellij.grammar.generator.ParserGeneratorUtil;
import org.intellij.grammar.psi.*;
import org.jetbrains.annotations.Contract;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

import static consulo.language.psi.SyntaxTraverser.psiTraverser;
import static org.intellij.grammar.generator.ParserGeneratorUtil.*;
import static org.intellij.grammar.psi.BnfTypes.BNF_SEQUENCE;

/**
 * @author gregsh
 */
public class GrammarUtil {
    public static final BnfExpression[] EMPTY_EXPRESSIONS_ARRAY = new BnfExpression[0];

    public static PsiElement getDummyAwarePrevSibling(PsiElement child) {
        PsiElement prevSibling = child.getPrevSibling();
        while (prevSibling instanceof GeneratedParserUtilBase.DummyBlock) {
            prevSibling = prevSibling.getLastChild();
        }
        if (prevSibling != null) {
            return prevSibling;
        }
        PsiElement parent = child.getParent();
        while (parent instanceof GeneratedParserUtilBase.DummyBlock && parent.getPrevSibling() == null) {
            parent = parent.getParent();
        }
        return parent == null ? null : parent.getPrevSibling();
    }

    public static boolean equalsElement(BnfExpression e1, BnfExpression e2) {
        if (e1 == null) {
            return e2 == null;
        }
        if (e2 == null || ParserGeneratorUtil.getEffectiveType(e1) != ParserGeneratorUtil.getEffectiveType(e2)) {
            return false;
        }
        if (isOneTokenExpression(e1)) {
            return e1.getText().equals(e2.getText());
        }
        else {
            for (PsiElement c1 = e1.getFirstChild(), c2 = e2.getFirstChild(); ; ) {
                boolean f1 = c1 == null || c1 instanceof BnfExpression;
                boolean f2 = c2 == null || c2 instanceof BnfExpression;
                if (f1 && f2 && !equalsElement((BnfExpression)c1, (BnfExpression)c2)) {
                    return false;
                }
                if (!f1 || f2) {
                    c1 = c1 == null ? null : c1.getNextSibling();
                }
                if (f1 || !f2) {
                    c2 = c2 == null ? null : c2.getNextSibling();
                }
                if (c1 == null && c2 == null) {
                    return true;
                }
            }
        }
    }

    public static boolean isInAttributesReference(@Nullable PsiElement element) {
        return PsiTreeUtil.getParentOfType(element, BnfRule.class, BnfAttrs.class) instanceof BnfAttrs;
    }

    public static boolean isOneTokenExpression(@Nullable BnfExpression e1) {
        return e1 instanceof BnfLiteralExpression || e1 instanceof BnfReferenceOrToken;
    }

    public static boolean isExternalReference(@Nullable PsiElement psiElement) {
        PsiElement parent = psiElement == null ? null : psiElement.getParent();
        if (parent instanceof BnfExternalExpression bnfExternalExpression && bnfExternalExpression.getRefElement() == psiElement) {
            return true;
        }
        if (parent instanceof BnfSequence && parent.getFirstChild() == psiElement) {
            parent = parent.getParent();
        }
        return parent instanceof BnfRule bnfRule && ParserGeneratorUtil.Rule.isExternal(bnfRule);
    }

    public static List<BnfExpression> getExternalRuleExpressions(@Nonnull BnfRule subRule) {
        BnfExpression expression = subRule.getExpression();
        return expression instanceof BnfSequence bnfSequence ? bnfSequence.getExpressionList() : Collections.singletonList(expression);
    }

    public static List<String> collectMetaParameters(BnfRule rule, BnfExpression expression) {
        if (!ParserGeneratorUtil.Rule.isMeta(rule) && !ParserGeneratorUtil.Rule.isExternal(rule)) {
            return Collections.emptyList();
        }
        List<String> result = new SmartList<>();
        for (BnfExternalExpression o : bnfTraverserNoAttrs(expression).filter(BnfExternalExpression.class)) {
            if (o.getArguments().isEmpty()) {
                String text = "<<" + o.getRefElement().getText() + ">>";
                if (!result.contains(text)) {
                    result.add(text);
                }
            }
        }
        if (ParserGeneratorUtil.Rule.isMeta(rule)) {
            String attr = getAttribute(null, rule, KnownAttribute.RECOVER_WHILE);
            if (isDoubleAngles(attr) && !result.contains(attr)) {
                result.add(attr);
            }
        }
        return result;
    }

    @Nonnull
    public static String unquote(@Nonnull String str) {
        return StringUtil.unquoteString(str);
    }

    public static boolean isDoubleAngles(@Nullable String str) {
        return str != null && str.startsWith("<<") && str.endsWith(">>");
    }

    public static boolean processExpressionNames(
        BnfRule rule,
        String funcName,
        BnfExpression expression,
        PairProcessor<String, BnfExpression> processor
    ) {
        if (isAtomicExpression(expression)) {
            return true;
        }
        BnfExpression nonTrivialExpression = expression;
        for (BnfExpression e = expression, n = getTrivialNodeChild(e); n != null; e = n, n = getTrivialNodeChild(e)) {
            if (!processor.process(funcName, e)) {
                return false;
            }
            nonTrivialExpression = n;
        }
        boolean isMeta = nonTrivialExpression instanceof BnfExternalExpression;
        List<BnfExpression> children = getChildExpressions(nonTrivialExpression);
        for (int i = isMeta ? 1 : 0, size = children.size(); i < size; i++) {
            BnfExpression child = children.get(i);
            if (isAtomicExpression(child)) {
                continue;
            }
            String nextName = ParserGeneratorUtil.isTokenSequence(null, rule, child)
                ? funcName
                : getNextName(funcName, isMeta ? i - 1 : i);
            if (!processExpressionNames(rule, nextName, child, processor)) {
                return false;
            }
        }
        return processor.process(funcName, nonTrivialExpression);
    }

    public static boolean processPinnedExpressions(final BnfRule rule, final Processor<BnfExpression> processor) {
        return processPinnedExpressions(rule, (bnfExpression, pinMatcher) -> processor.process(bnfExpression));
    }

    public static boolean processPinnedExpressions(final BnfRule rule, final PairProcessor<BnfExpression, PinMatcher> processor) {
        return processExpressionNames(rule, getFuncName(rule), rule.getExpression(), (funcName, expression) -> {
            if (!(expression instanceof BnfSequence)) {
                return true;
            }
            List<BnfExpression> children = getChildExpressions(expression);
            if (children.size() < 2) {
                return true;
            }
            PinMatcher pinMatcher = new PinMatcher(null, rule, BNF_SEQUENCE, funcName);
            boolean pinApplied = false;
            for (int i = 0, childExpressionsSize = children.size(); i < childExpressionsSize; i++) {
                BnfExpression child = children.get(i);
                if (!pinApplied && pinMatcher.matches(i, child)) {
                    pinApplied = true;
                    if (!processor.process(child, pinMatcher)) {
                        return false;
                    }
                }
            }
            return true;
        });
    }

    public static boolean isAtomicExpression(BnfExpression tree) {
        return tree instanceof BnfReferenceOrToken ||
            tree instanceof BnfLiteralExpression;
    }

    public static SyntaxTraverser<PsiElement> bnfTraverser(PsiElement root) {
        return psiTraverser().withRoot(root)
            .forceDisregardTypes(Conditions.equalTo(GeneratedParserUtilBase.DUMMY_BLOCK))
            .filter(Conditions.instanceOf(BnfComposite.class));
    }

    public static SyntaxTraverser<PsiElement> bnfTraverserNoAttrs(PsiElement root) {
        return bnfTraverser(root).forceIgnore(Conditions.instanceOf(BnfAttrs.class));
    }

    public static String getMethodName(BnfRule rule, PsiElement element) {
        final BnfExpression target = PsiTreeUtil.getParentOfType(element, BnfExpression.class, false);
        String funcName = getFuncName(rule);
        if (target == null) {
            return funcName;
        }
        final Ref<String> ref = Ref.create(null);
        processExpressionNames(
            rule,
            funcName,
            rule.getExpression(),
            (funcName1, expression) -> {
                if (target == expression) {
                    ref.set(funcName1);
                    return false;
                }
                return true;
            }
        );
        return ref.get();
    }

    @Nonnull
    public static String getIdText(@Nullable PsiElement id) {
        return id == null ? "" : stripQuotesAroundId(id.getText());
    }

    @Contract("!null->!null")
    public static String stripQuotesAroundId(String text) {
        return isIdQuoted(text) ? text.substring(1, text.length() - 1) : text;
    }

    public static boolean isIdQuoted(@Nullable String text) {
        return text != null && text.startsWith("<") && text.endsWith(">");
    }

    public static class FakeElementType extends IElementType {
        public FakeElementType(String debugName, Language language) {
            super(debugName, language, false);
        }
    }

    public static class FakeBnfExpression extends LeafPsiElement implements BnfExpression {
        public FakeBnfExpression(@Nonnull String text) {
            this(BnfTypes.BNF_EXPRESSION, text);
        }

        public FakeBnfExpression(@Nonnull IElementType elementType, @Nonnull String text) {
            super(elementType, text);
        }

        @Override
        public <R> R accept(@Nonnull BnfVisitor<R> visitor) {
            return visitor.visitExpression(this);
        }

        @Override
        public String toString() {
            return getText();
        }
    }
}
