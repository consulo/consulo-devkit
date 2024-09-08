/*
 * Copyright 2011-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.intellij.grammar.generator;

import consulo.annotation.access.RequiredReadAction;
import consulo.language.ast.IElementType;
import consulo.language.psi.PsiElement;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import org.intellij.grammar.KnownAttribute;
import org.intellij.grammar.psi.BnfAttr;
import org.intellij.grammar.psi.BnfRule;
import org.intellij.grammar.psi.impl.GrammarUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

import static org.intellij.grammar.generator.ParserGeneratorUtil.*;
import static org.intellij.grammar.psi.BnfTypes.BNF_REFERENCE_OR_TOKEN;
import static org.intellij.grammar.psi.BnfTypes.BNF_STRING;

/**
 * @author gregsh
 */
public class RuleMethodsHelper {
    private final RuleGraphHelper myGraphHelper;
    private final ExpressionHelper myExpressionHelper;
    private final Map<String, String> mySimpleTokens;
    private final GenOptions G;
    private final String myVersion;

    private final Map<BnfRule, Pair<Map<String, MethodInfo>, Collection<MethodInfo>>> myMethods;

    public RuleMethodsHelper(
        RuleGraphHelper ruleGraphHelper,
        ExpressionHelper expressionHelper,
        Map<String, String> simpleTokens,
        GenOptions genOptions
    ) {
        myVersion = ruleGraphHelper.getFile().getVersion();
        myGraphHelper = ruleGraphHelper;
        myExpressionHelper = expressionHelper;
        mySimpleTokens = Collections.unmodifiableMap(simpleTokens);
        G = genOptions;

        myMethods = new LinkedHashMap<>();
    }

    @RequiredReadAction
    public void buildMaps(Collection<BnfRule> sortedPsiRules) {
        Map<String, String> tokensReversed = RuleGraphHelper.computeTokens(myGraphHelper.getFile()).asMap(myVersion);
        for (BnfRule rule : sortedPsiRules) {
            calcMethods(rule, tokensReversed);
        }
        for (BnfRule r0 : myGraphHelper.getRuleExtendsMap().keySet()) {
            if (!myMethods.containsKey(r0)) {
                continue;
            }
            Map<String, MethodInfo> p0 = myMethods.get(r0).first;
            for (BnfRule r : myGraphHelper.getRuleExtendsMap().get(r0)) {
                if (r0 == r) {
                    continue;
                }
                if (!myMethods.containsKey(r)) {
                    continue;
                }
                Map<String, MethodInfo> p = myMethods.get(r).first;
                for (String name : p.keySet()) {
                    MethodInfo m0 = p0.get(name);
                    if (m0 == null) {
                        continue;
                    }
                    MethodInfo m = p.get(name);
                    if (m0.cardinality != m.cardinality) {
                        continue;
                    }
                    m.name = ""; // suppress super method duplication
                }
            }
        }
    }

    @Nonnull
    public Collection<MethodInfo> getFor(@Nonnull BnfRule rule) {
        return myMethods.get(rule).second;
    }

    @Nullable
    public MethodInfo getMethodInfo(@Nonnull BnfRule rule, String name) {
        return myMethods.get(rule).first.get(name);
    }

    @Nullable
    public Collection<String> getMethodNames(@Nonnull BnfRule rule) {
        return myMethods.get(rule).first.keySet();
    }

    @RequiredReadAction
    protected void calcMethods(BnfRule rule, Map<String, String> tokensReversed) {
        List<MethodInfo> result = new ArrayList<>();

        Map<PsiElement, RuleGraphHelper.Cardinality> cardMap = myGraphHelper.getFor(rule);

        for (PsiElement element : cardMap.keySet()) {
            RuleGraphHelper.Cardinality c = myExpressionHelper.fixCardinality(rule, element, cardMap.get(element));
            String pathName = getRuleOrTokenNameForPsi(element, c);
            if (pathName == null) {
                continue;
            }
            if (element instanceof BnfRule resultType) {
                if (!ParserGeneratorUtil.Rule.isPrivate(rule)) {
                    result.add(new MethodInfo(MethodType.RULE, pathName, pathName, resultType, c));
                }
            }
            else {
                result.add(new MethodInfo(MethodType.TOKEN, pathName, pathName, null, c));
            }
        }
        Collections.sort(result);

        BnfAttr attr = findAttribute(myVersion, rule, KnownAttribute.GENERATE_TOKEN_ACCESSORS);
        boolean generateTokens = attr == null ? G.generateTokenAccessors :
            Boolean.TRUE.equals(getAttributeValue(attr.getExpression()));
        boolean generateTokensSet = attr != null || G.generateTokenAccessorsSet;
        Map<String, MethodInfo> basicMethods = new LinkedHashMap<>();

        for (MethodInfo methodInfo : result) {
            basicMethods.put(methodInfo.name, methodInfo);
            if (methodInfo.type == MethodType.TOKEN) {
                boolean registered = tokensReversed.containsKey(methodInfo.name);
                String pattern = tokensReversed.get(methodInfo.name);
                // only regexp and lowercase tokens accessors are generated by default
                if (!(generateTokens || !generateTokensSet && registered
                    && (pattern == null || ParserGeneratorUtil.isRegexpToken(pattern)))) {
                    methodInfo.name = ""; // disable token
                }
            }
        }

        KnownAttribute.ListValue methods = getAttribute(myVersion, rule, KnownAttribute.METHODS);
        for (Map.Entry<String, String> pair : methods.asMap(myVersion).entrySet()) {
            if (StringUtil.isEmpty(pair.getKey())) {
                continue;
            }
            MethodInfo methodInfo = basicMethods.get(pair.getKey());
            if (methodInfo != null) {
                methodInfo.name = ""; // suppress or user method override
            }
            if (StringUtil.isNotEmpty(pair.getValue())) {
                MethodInfo basicInfo = basicMethods.get(pair.getValue());
                if (basicInfo != null && (basicInfo.name.equals(pair.getValue()) || basicInfo.name.isEmpty())) {
                    basicInfo.name = pair.getKey(); // simple rename, fix order anyway
                    result.remove(basicInfo);
                    result.add(basicInfo);
                }
                else {
                    result.add(new MethodInfo(MethodType.USER, pair.getKey(), pair.getValue(), null, null));
                }
            }
            else if (methodInfo == null) {
                result.add(new MethodInfo(MethodType.MIXIN, pair.getKey(), null, null, null));
            }
        }
        myMethods.put(rule, Pair.create(basicMethods, result));
    }

    @Nullable
    @RequiredReadAction
    private String getRuleOrTokenNameForPsi(@Nonnull PsiElement tree, @Nonnull RuleGraphHelper.Cardinality type) {
        String result;

        if (!(tree instanceof BnfRule)) {
            if (type.many()) {
                return null; // do not generate token lists
            }

            IElementType effectiveType = getEffectiveType(tree);
            if (effectiveType == BNF_STRING) {
                result = mySimpleTokens.get(GrammarUtil.unquote(tree.getText()));
            }
            else if (effectiveType == BNF_REFERENCE_OR_TOKEN) {
                result = tree.getText();
            }
            else {
                result = null;
            }
        }
        else {
            BnfRule asRule = (BnfRule)tree;
            result = asRule.getName();
            if (StringUtil.isEmpty(getElementType(myVersion, asRule, G.generateElementCase))) {
                return null;
            }
        }
        return result;
    }

    public enum MethodType {
        RULE,
        TOKEN,
        USER,
        MIXIN
    }

    public static class MethodInfo implements Comparable<MethodInfo> {
        final MethodType type;
        final String originalName;
        final String path;
        final BnfRule rule;
        final RuleGraphHelper.Cardinality cardinality;

        String name;

        private MethodInfo(MethodType type, String name, String path, BnfRule rule, RuleGraphHelper.Cardinality cardinality) {
            this.type = type;
            this.name = originalName = name;
            this.path = path;
            this.rule = rule;
            this.cardinality = cardinality;
        }

        @Override
        public int compareTo(@Nonnull MethodInfo o) {
            if (type != o.type) {
                return type.compareTo(o.type);
            }
            return name.compareTo(o.name);
        }

        @Nonnull
        public String generateGetterName() {
            boolean many = cardinality.many();

            boolean renamed = !Objects.equals(name, originalName);
            String getterNameBody = ParserGeneratorUtil.getGetterName(name);
            return getterNameBody + (many && !renamed ? "List" : "");
        }

        @Override
        public String toString() {
            return "MethodInfo{" +
                "type=" + type +
                ", name='" + name + '\'' +
                ", path='" + path + '\'' +
                ", rule=" + rule +
                ", cardinality=" + cardinality +
                '}';
        }
    }
}
