/*
 * Copyright 2011-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.intellij.grammar.generator;

import consulo.application.util.CachedValue;
import consulo.application.util.CachedValueProvider;
import consulo.application.util.CachedValuesManager;
import consulo.application.util.function.CommonProcessors;
import consulo.language.Language;
import consulo.language.ast.IElementType;
import consulo.language.impl.psi.LeafPsiElement;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiUtilCore;
import consulo.language.psi.SyntaxTraverser;
import consulo.language.psi.util.LanguageCachedValueUtil;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.util.collection.HashingStrategy;
import consulo.util.collection.Maps;
import consulo.util.collection.MultiMap;
import consulo.util.dataholder.Key;
import consulo.util.lang.ObjectUtil;
import org.intellij.grammar.KnownAttribute;
import org.intellij.grammar.analysis.BnfFirstNextAnalyzer;
import org.intellij.grammar.psi.*;
import org.intellij.grammar.psi.impl.GrammarUtil;
import org.intellij.grammar.psi.impl.GrammarUtil.FakeBnfExpression;
import org.intellij.grammar.psi.impl.GrammarUtil.FakeElementType;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.*;

import static org.intellij.grammar.generator.ParserGeneratorUtil.*;
import static org.intellij.grammar.generator.RuleGraphHelper.Cardinality.*;
import static org.intellij.grammar.psi.impl.GrammarUtil.collectMetaParameters;
import static org.intellij.grammar.psi.impl.GrammarUtil.isDoubleAngles;

/**
 * @author gregory
 * Date: 16.07.11 10:41
 */
public class RuleGraphHelper {
    private static final HashingStrategy<PsiElement> CARDINALITY_HASHING_STRATEGY = new HashingStrategy<>() {
        @Override
        public int hashCode(PsiElement e) {
            if (e instanceof BnfReferenceOrToken || e instanceof BnfLiteralExpression) {
                return e.getText().hashCode();
            }
            return Objects.hashCode(e);
        }

        @Override
        public boolean equals(PsiElement e1, PsiElement e2) {
            if (e1 instanceof BnfReferenceOrToken && e2 instanceof BnfReferenceOrToken ||
                e1 instanceof BnfLiteralExpression && e2 instanceof BnfLiteralExpression) {
                return e1.getText().equals(e2.getText());
            }
            return Objects.equals(e1, e2);
        }
    };
    private final BnfFile myFile;
    private final String myVersion;
    private final MultiMap<BnfRule, BnfRule> myRuleExtendsMap;
    private final MultiMap<BnfRule, BnfRule> myRulesGraph = newMultiMap();
    private final Map<BnfRule, Map<PsiElement, Cardinality>> myRuleContentsMap = new HashMap<>();
    private final MultiMap<BnfRule, PsiElement> myRulesCollapseMap = newMultiMap();
    private final Set<BnfRule> myRulesWithTokens = new HashSet<>();
    private final Map<String, PsiElement> myExternalElements = new HashMap<>();

    private static final IElementType EXTERNAL_TYPE = new FakeElementType("EXTERNAL_TYPE", Language.ANY);
    private static final IElementType MARKER_TYPE = new FakeElementType("MARKER_TYPE", Language.ANY);
    private static final PsiElement LEFT_MARKER = new FakeBnfExpression(MARKER_TYPE, "LEFT_MARKER");
    private static final PsiElement NOT_EMPTY_MARKER = new FakeBnfExpression(MARKER_TYPE, "NOT_EMPTY_MARKER");
    private static final Object RECURSION_MARKER = "RECURSION_DETECTED";

    public static String getCardinalityText(Cardinality cardinality) {
        if (cardinality == AT_LEAST_ONE) {
            return "+";
        }
        else if (cardinality == ANY_NUMBER) {
            return "*";
        }
        else if (cardinality == OPTIONAL) {
            return "?";
        }
        return "";
    }

    public enum Cardinality {
        NONE,
        OPTIONAL,
        REQUIRED,
        AT_LEAST_ONE,
        ANY_NUMBER;

        public boolean optional() {
            return this == OPTIONAL || this == ANY_NUMBER || this == NONE;
        }

        public boolean many() {
            return this == AT_LEAST_ONE || this == ANY_NUMBER;
        }

        public Cardinality single() {
            return this == AT_LEAST_ONE ? REQUIRED : this == ANY_NUMBER ? OPTIONAL : this;
        }

        public static Cardinality fromNodeType(IElementType type) {
            if (type == BnfTypes.BNF_OP_OPT) {
                return OPTIONAL;
            }
            else if (type == BnfTypes.BNF_SEQUENCE || type == BnfTypes.BNF_REFERENCE_OR_TOKEN) {
                return REQUIRED;
            }
            else if (type == BnfTypes.BNF_CHOICE) {
                return OPTIONAL;
            }
            else if (type == BnfTypes.BNF_OP_ONEMORE) {
                return AT_LEAST_ONE;
            }
            else if (type == BnfTypes.BNF_OP_ZEROMORE) {
                return ANY_NUMBER;
            }
            else {
                throw new AssertionError("unexpected: " + type);
            }
        }

        public Cardinality and(Cardinality c) {
            if (c == null) {
                return this;
            }
            if (this == NONE || c == NONE) {
                return NONE;
            }
            if (optional() || c.optional()) {
                return many() || c.many() ? ANY_NUMBER : OPTIONAL;
            }
            else {
                return many() || c.many() ? AT_LEAST_ONE : REQUIRED;
            }
        }

        public Cardinality or(Cardinality c) {
            if (c == null) {
                c = NONE;
            }
            if (this == NONE && c == NONE) {
                return NONE;
            }
            if (this == NONE) {
                return c;
            }
            if (c == NONE) {
                return this;
            }
            return optional() && c.optional() ? ANY_NUMBER : AT_LEAST_ONE;
        }

    }

    public static MultiMap<BnfRule, BnfRule> buildExtendsMap(BnfFile file) {
        String version = file.getVersion();
        MultiMap<BnfRule, BnfRule> ruleExtendsMap = newMultiMap();
        for (BnfRule rule : file.getRules()) {
            if (isPrivateOrNoType(rule, version)) {
                continue;
            }
            BnfRule superRule = file.getRule(getAttribute(file.getVersion(), rule, KnownAttribute.EXTENDS));
            if (superRule != null) {
                ruleExtendsMap.putValue(superRule, rule);
            }
            BnfRule target = getSynonymTargetOrSelf(version, rule);
            if (target != rule) {
                ruleExtendsMap.putValue(target, rule);
                if (superRule != null) {
                    ruleExtendsMap.putValue(superRule, target);
                }
            }
        }
        for (int i = 0, len = ruleExtendsMap.size(); i < len; i++) {
            boolean changed = false;
            for (BnfRule superRule : ruleExtendsMap.keySet()) {
                final Collection<BnfRule> rules = ruleExtendsMap.get(superRule);
                for (BnfRule rule : new ArrayList<>(rules)) {
                    changed |= rules.addAll(ruleExtendsMap.get(rule));
                }
            }
            if (!changed) {
                break;
            }
        }
        for (BnfRule rule : ruleExtendsMap.keySet()) {
            ruleExtendsMap.putValue(rule, rule); // add super to itself
        }
        return ruleExtendsMap;
    }

    private static <K, V> MultiMap<K, V> newMultiMap() {
        return MultiMap.createLinkedSet();
    }

    @Nonnull
    public static Map<String, String> getTokenNameToTextMap(final BnfFile file) {
        return LanguageCachedValueUtil.getCachedValue(
            file,
            () -> new CachedValueProvider.Result<>(
                computeTokens(file).asMap(file.getVersion()),
                file
            )
        );
    }

    @Nonnull
    public static Map<String, String> getTokenTextToNameMap(final BnfFile file) {
        return LanguageCachedValueUtil.getCachedValue(
            file,
            () -> new CachedValueProvider.Result<>(
                computeTokens(file).asInverseMap(file.getVersion()),
                file
            )
        );
    }

    // string value to constant name
    public static KnownAttribute.ListValue computeTokens(BnfFile file) {
        return getRootAttribute(file.getVersion(), file, KnownAttribute.TOKENS);
    }

    private static final Key<CachedValue<RuleGraphHelper>> RULE_GRAPH_HELPER_KEY = Key.create("RULE_GRAPH_HELPER_KEY");

    public static RuleGraphHelper getCached(final BnfFile file) {
        CachedValue<RuleGraphHelper> value = file.getUserData(RULE_GRAPH_HELPER_KEY);
        if (value == null) {
            file.putUserData(RULE_GRAPH_HELPER_KEY, value = CachedValuesManager.getManager(file.getProject()).createCachedValue(
                () -> new CachedValueProvider.Result<>(new RuleGraphHelper(file), file), false));
        }
        return value.getValue();
    }

    public RuleGraphHelper(BnfFile file) {
        this(file, buildExtendsMap(file));
    }

    public RuleGraphHelper(BnfFile file, MultiMap<BnfRule, BnfRule> ruleExtendsMap) {
        myFile = file;
        myVersion = file.getVersion();
        myRuleExtendsMap = ruleExtendsMap;

        buildRulesGraph();
        buildCollapseMap();
        buildContentsMap();
    }

    public MultiMap<BnfRule, BnfRule> getRuleExtendsMap() {
        return myRuleExtendsMap;
    }

    public BnfFile getFile() {
        return myFile;
    }

    public boolean canCollapse(@Nonnull BnfRule rule) {
        return myRulesCollapseMap.containsKey(rule);
    }

    private void buildCollapseMap() {
        BnfFirstNextAnalyzer analyzer = new BnfFirstNextAnalyzer()
            .setPublicRuleOpaque(true).setParentFilter(o -> !(o instanceof BnfRule)).setPredicateLookAhead(true);

        for (BnfRule rule : myFile.getRules()) {
            if (!myRuleExtendsMap.containsScalarValue(rule)) {
                continue;
            }
            Set<BnfExpression> first = analyzer.calcFirst(rule);
            for (BnfExpression expression : first) {
                BnfRule r = expression instanceof BnfReferenceOrToken ? myFile.getRule(expression.getText()) : null;
                BnfRule commonSuper = r != null ? getCommonSuperRule(rule, r) : null;
                if (expression != BnfFirstNextAnalyzer.BNF_MATCHES_ANY && commonSuper == null) {
                    continue;
                }
                Map<BnfExpression, BnfExpression> map = analyzer.calcNext(expression);
                if (!map.containsKey(BnfFirstNextAnalyzer.BNF_MATCHES_EOF)) {
                    continue;
                }

                if (commonSuper != null) {
                    myRulesCollapseMap.putValue(rule, commonSuper);
                }
                myRulesCollapseMap.putValue(rule, ObjectUtil.notNull(r, rule));
            }
            if (myRulesCollapseMap.containsKey(rule)) {
                myRulesCollapseMap.putValue(rule, rule);
            }
        }
    }

    private void buildContentsMap() {
        List<BnfRule> rules = topoSort(myFile.getRules(), this);
        Set<Object> visited = new LinkedHashSet<>();
        for (BnfRule rule : rules) {
            collectMembers(rule, visited);
            visited.clear();
        }
    }

    private Map<PsiElement, Cardinality> collectMembers(@Nonnull BnfRule rule, Set<Object> visited) {
        Map<PsiElement, Cardinality> result = myRuleContentsMap.get(rule);
        if (result != null) {
            return result;
        }
        if (Rule.isExternal(rule)) {
            result = psiMap(newExternalPsi(rule.getName()), REQUIRED);
        }
        else {
            result = collectMembers(rule, rule.getExpression(), visited);
        }
        if (visited.size() > 1 && visited.contains(RECURSION_MARKER) && isPrivateOrNoType(rule, myVersion)) {
            return result;
        }
        result.remove(NOT_EMPTY_MARKER); // todo private rules should retain this
        myRuleContentsMap.put(rule, result);
        return result;
    }

    @Nullable
    private BnfRule getCommonSuperRule(BnfRule r1, BnfRule r2) {
        int count = Integer.MAX_VALUE;
        BnfRule result = null;
        for (BnfRule superRule : myRuleExtendsMap.keySet()) {
            Collection<BnfRule> set = myRuleExtendsMap.get(superRule);
            if (set.contains(r1) && set.contains(r2)) {
                if (count > set.size()) {
                    count = set.size();
                    result = superRule;
                }
            }
        }
        return result;
    }

    private void buildRulesGraph() {
        SyntaxTraverser<PsiElement> s = SyntaxTraverser.psiTraverser()
            .expand(o -> !(o instanceof BnfPredicate || o instanceof BnfExternalExpression));
        for (BnfRule rule : myFile.getRules()) {
            for (PsiElement e : s.withRoot(rule.getExpression()).filter(BnfExpression.class)) {
                BnfReferenceOrToken ruleRef = e instanceof BnfReferenceOrToken bnfReferenceOrToken
                    ? bnfReferenceOrToken
                    : e instanceof BnfExternalExpression
                    ? PsiTreeUtil.findChildOfType(e, BnfReferenceOrToken.class)
                    : null;
                BnfRule r = ruleRef != null ? ruleRef.resolveRule() : null;
                if (r != null) {
                    myRulesGraph.putValue(rule, r);
                }
                else if (e instanceof BnfReferenceOrToken || e instanceof BnfStringLiteralExpression) {
                    myRulesWithTokens.add(rule);
                }
            }
        }
        for (BnfRule rule : myFile.getRules()) {
            if (Rule.isLeft(rule) && !isPrivateOrNoType(rule, myVersion) && !Rule.isInner(rule)) {
                for (BnfRule r : getRulesToTheLeft(rule).keySet()) {
                    myRulesGraph.putValue(rule, r);
                }
            }
        }
    }

    public Collection<BnfRule> getExtendsRules(BnfRule rule) {
        return myRuleExtendsMap.get(rule);
    }

    public boolean containsTokens(BnfRule rule) {
        return myRulesWithTokens.contains(rule);
    }

    public Collection<BnfRule> getSubRules(BnfRule rule) {
        return myRulesGraph.get(rule);
    }

    @Nonnull
    public Map<PsiElement, Cardinality> getFor(BnfRule rule) {
        Map<PsiElement, Cardinality> map = myRuleContentsMap.get(rule); // null for duplicate
        return map == null ? Collections.emptyMap() : map;
    }

    Map<PsiElement, Cardinality> collectMembers(BnfRule rule, BnfExpression tree, Set<Object> visited) {
        if (tree instanceof BnfPredicate) {
            return Collections.emptyMap();
        }
        if (tree instanceof BnfLiteralExpression) {
            return psiMap(tree, REQUIRED);
        }

        if (!visited.add(tree)) {
            visited.add(RECURSION_MARKER);
            return psiMap(tree, REQUIRED);
        }
        try {
            return collectMembersInner(rule, tree, visited);
        }
        finally {
            visited.remove(tree);
        }
    }

    private Map<PsiElement, Cardinality> collectMembersInner(BnfRule rule, BnfExpression tree, Set<Object> visited) {
        boolean firstNonTrivial = tree == Rule.firstNotTrivial(rule);
        boolean outerLeft = (firstNonTrivial || rule.getExpression() == tree) &&
            Rule.isLeft(rule) && !isPrivateOrNoType(rule, myVersion) && !Rule.isInner(rule);
        boolean tryCollapse = firstNonTrivial && !outerLeft && !isPrivateOrNoType(rule, myVersion) && !Rule.isFake(rule);

        Map<PsiElement, Cardinality> result;
        if (tree instanceof BnfReferenceOrToken) {
            BnfRule targetRule = ((BnfReferenceOrToken)tree).resolveRule();
            if (targetRule != null) {
                if (Rule.isExternal(targetRule)) {
                    result = psiMap(newExternalPsi(targetRule.getName()), REQUIRED);
                }
                else if (Rule.isLeft(targetRule)) {
                    if (!Rule.isInner(targetRule) && !isPrivateOrNoType(targetRule, myVersion)) {
                        result = psiMap();
                        result.put(getSynonymTargetOrSelf(myVersion, targetRule), REQUIRED);
                        result.put(LEFT_MARKER, REQUIRED);
                    }
                    else {
                        result = Collections.emptyMap();
                    }
                }
                else if (isPrivateOrNoType(targetRule, myVersion)) {
                    result = collectMembers(targetRule, visited);
                }
                else if (Rule.isUpper(targetRule)) {
                    result = Collections.emptyMap();
                }
                else {
                    result = psiMap(getSynonymTargetOrSelf(myVersion, targetRule), REQUIRED);
                }
            }
            else {
                result = psiMap(tree, REQUIRED);
            }
            if (tryCollapse && willCollapse(rule, result)) {
                result = Collections.emptyMap();
            }
        }
        else if (tree instanceof BnfExternalExpression) {
            BnfExternalExpression expression = (BnfExternalExpression)tree;
            List<BnfExpression> arguments = expression.getArguments();
            if (arguments.isEmpty() && Rule.isMeta(rule)) {
                result = psiMap(newExternalPsi(tree.getText()), REQUIRED);
            }
            else {
                BnfExpression ruleRef = expression.getRefElement();
                BnfRule metaRule = ((BnfReferenceOrToken)ruleRef).resolveRule();
                if (metaRule == null) {
                    result = psiMap(newExternalPsi("#" + ruleRef.getText()), REQUIRED);
                }
                else if (isPrivateOrNoType(metaRule, myVersion)) {
                    result = psiMap();
                    Map<PsiElement, Cardinality> metaResults = collectMembers(metaRule, visited);
                    List<String> params = null;
                    for (PsiElement member : metaResults.keySet()) {
                        Cardinality cardinality = metaResults.get(member);
                        if (!isExternalPsi(member)) {
                            result.put(member, cardinality);
                        }
                        else {
                            if (params == null) {
                                params = collectMetaParameters(metaRule, metaRule.getExpression());
                            }
                            int idx = params.indexOf(member.getText());
                            if (idx > -1 && idx < arguments.size()) {
                                Map<PsiElement, Cardinality> argMap = collectMembers(rule, arguments.get(idx), visited);
                                for (PsiElement element : argMap.keySet()) {
                                    Cardinality existing = ObjectUtil.notNull(result.get(element), NONE);
                                    result.put(element, existing.or(cardinality.and(argMap.get(element))));
                                }
                            }
                        }
                    }
                }
                else {
                    result = psiMap(metaRule, REQUIRED);
                }
            }
            if (tryCollapse && willCollapse(rule, result)) {
                result = Collections.emptyMap();
            }
        }
        else {
            List<BnfExpression> pinned = new ArrayList<>();
            GrammarUtil.processPinnedExpressions(rule, new CommonProcessors.CollectProcessor<>(pinned));
            boolean pinApplied = false;

            IElementType type = getEffectiveType(tree);

            List<Map<PsiElement, Cardinality>> list = new ArrayList<>();
            List<BnfExpression> childExpressions = getChildExpressions(tree);
            for (BnfExpression child : childExpressions) {
                Map<PsiElement, Cardinality> nextMap = collectMembers(rule, child, visited);
                if (pinApplied) {
                    nextMap = joinMaps(rule, false, BnfTypes.BNF_OP_OPT, Collections.singletonList(nextMap));
                }
                list.add(nextMap);
                if (!pinApplied && pinned.contains(child)) {
                    pinApplied = true;
                }
            }
            result = joinMaps(rule, tryCollapse, type, list);
            result = type == BnfTypes.BNF_SEQUENCE && visited.contains(RECURSION_MARKER) && result.remove(rule.getExpression()) != null ?
                joinMaps(rule, false, type, Arrays.asList(result, result)) : result;
        }
        if (outerLeft && rule.getExpression() == tree) {
            List<Map<PsiElement, Cardinality>> list = new ArrayList<>();
            Map<BnfRule, Cardinality> rulesToTheLeft = getRulesToTheLeft(rule);
            for (BnfRule r : rulesToTheLeft.keySet()) {
                Cardinality cardinality = rulesToTheLeft.get(r);
                Map<PsiElement, Cardinality> leftMap = psiMap(r, REQUIRED);
                if (cardinality.many()) {
                    list.add(joinMaps(rule, false, BnfTypes.BNF_CHOICE, Arrays.asList(leftMap, psiMap(rule, REQUIRED))));
                }
                else {
                    list.add(leftMap);
                }
            }
            Map<PsiElement, Cardinality> combinedLeftMap = joinMaps(rule, false, BnfTypes.BNF_CHOICE, list);
            result = joinMaps(rule, true, BnfTypes.BNF_SEQUENCE, Arrays.asList(result, combinedLeftMap));
        }
        return result;
    }

    private Map<BnfRule, Cardinality> getRulesToTheLeft(BnfRule rule) {
        Map<BnfRule, Cardinality> result = new LinkedHashMap<>();
        Map<BnfExpression, BnfExpression> nextMap = new BnfFirstNextAnalyzer().setBackward(true).setPublicRuleOpaque(true).calcNext(rule);
        for (BnfExpression e : nextMap.keySet()) {
            if (!(e instanceof BnfReferenceOrToken)) {
                continue;
            }
            BnfRule r = ((BnfReferenceOrToken)e).resolveRule();
            if (r == null || isPrivateOrNoType(r, myVersion)) {
                continue;
            }
            BnfExpression context = nextMap.get(e);
            Cardinality cardinality = REQUIRED;
            for (PsiElement cur = context; !(cur instanceof BnfRule); cur = cur.getParent()) {
                if (PsiTreeUtil.isAncestor(cur, e, true)) {
                    break;
                }
                IElementType curType = getEffectiveType(cur);
                if (curType == BnfTypes.BNF_OP_OPT || curType == BnfTypes.BNF_OP_ONEMORE || curType == BnfTypes.BNF_OP_ZEROMORE) {
                    cardinality = cardinality.and(Cardinality.fromNodeType(curType));
                }
            }
            Cardinality prev = result.get(r);
            result.put(r, prev == null ? cardinality : cardinality.and(prev));
        }
        return result;
    }

    private Map<PsiElement, Cardinality> joinMaps(
        @Nonnull BnfRule rule,
        boolean tryCollapse,
        IElementType type,
        List<Map<PsiElement, Cardinality>> list
    ) {
        if (list.isEmpty()) {
            return Collections.emptyMap();
        }
        if (type == BnfTypes.BNF_OP_OPT || type == BnfTypes.BNF_OP_ZEROMORE || type == BnfTypes.BNF_OP_ONEMORE) {
            ParserGenerator.LOG.assertTrue(list.size() == 1);
            list = compactInheritors(rule, list);
            Map<PsiElement, Cardinality> m = list.get(0);
            if (tryCollapse && willCollapse(rule, m) && type == BnfTypes.BNF_OP_OPT) {
                return Collections.emptyMap();
            }
            Map<PsiElement, Cardinality> map = psiMap();
            boolean leftMarker = m.containsKey(LEFT_MARKER);
            for (PsiElement t : m.keySet()) {
                Cardinality joinedCard = fromNodeType(type).and(m.get(t));
                if (leftMarker) {
                    joinedCard = joinedCard.single();
                }
                map.put(t, joinedCard);
            }
            return map;
        }
        else if (type == BnfTypes.BNF_SEQUENCE || type == BnfTypes.BNF_EXPRESSION || type == BnfTypes.BNF_REFERENCE_OR_TOKEN) {
            list = new ArrayList<>(compactInheritors(rule, list));
            list.removeIf(Map::isEmpty);
            Map<PsiElement, Cardinality> map = psiMap();
            for (Map<PsiElement, Cardinality> m : list) {
                Cardinality leftMarker = m.get(LEFT_MARKER);
                if (leftMarker == REQUIRED) {
                    map.clear();
                    leftMarker = null;
                }
                else if (leftMarker == OPTIONAL) {
                    for (PsiElement t : map.keySet()) {
                        if (!m.containsKey(t)) {
                            map.put(t, map.get(t).and(Cardinality.OPTIONAL));
                        }
                    }
                }
                for (PsiElement t : m.keySet()) {
                    if (t == LEFT_MARKER && m != list.get(0)) {
                        continue;
                    }
                    Cardinality c1 = map.get(t);
                    Cardinality c2 = m.get(t);
                    Cardinality joinedCard;
                    if (leftMarker == null) {
                        joinedCard = c2.or(c1);

                    }
                    // handle left semantic in a choice-like way
                    else if (c1 == null) {
                        joinedCard = c2;
                    }
                    else {
                        if (c1 == REQUIRED) {
                            joinedCard = c2.many() ? AT_LEAST_ONE : REQUIRED;
                        }
                        else if (c1 == AT_LEAST_ONE) {
                            joinedCard = ANY_NUMBER;
                        }
                        else {
                            joinedCard = c1;
                        }
                    }
                    map.put(t, joinedCard);
                }
            }
            if (tryCollapse && willCollapse(rule, map)) {
                return Collections.emptyMap();
            }
            return map;
        }
        else if (type == BnfTypes.BNF_CHOICE) {
            Map<PsiElement, Cardinality> map = psiMap();
            list = compactInheritors(rule, list);
            if (tryCollapse) {
                for (int i = 0, newListSize = list.size(); i < newListSize; i++) {
                    Map<PsiElement, Cardinality> m = list.get(i);
                    if (willCollapse(rule, m)) {
                        list.set(i, Collections.emptyMap());
                    }
                }
            }
            Map<PsiElement, Cardinality> m0 = list.get(0);
            map.putAll(m0);
            for (Map<PsiElement, Cardinality> m : list) {
                map.keySet().retainAll(m.keySet());
            }
            for (PsiElement t : new ArrayList<>(map.keySet())) {
                map.put(t, REQUIRED.and(m0.get(t)));
                for (Map<PsiElement, Cardinality> m : list) {
                    if (m == list.get(0)) {
                        continue;
                    }
                    map.put(t, map.get(t).and(m.get(t)));
                }
            }
            for (Map<PsiElement, Cardinality> m : list) {
                if (tryCollapse && willCollapse(rule, m)) {
                    continue;
                }
                for (PsiElement t : m.keySet()) {
                    if (map.containsKey(t)) {
                        continue;
                    }
                    map.put(t, OPTIONAL.and(m.get(t)));
                }
            }
            boolean notEmpty = true;
            empty:
            for (Map<PsiElement, Cardinality> m : list) {
                for (Cardinality c : m.values()) {
                    if (!c.optional()) {
                        continue empty;
                    }
                }
                notEmpty = false;
            }
            if (notEmpty) {
                map.put(NOT_EMPTY_MARKER, REQUIRED);
            }
            return map;
        }
        else {
            throw new AssertionError("unexpected: " + type);
        }
    }

    private boolean canCollapseBy(BnfRule rule, PsiElement t) {
        if (myRulesCollapseMap.get(rule).contains(t)) {
            return true;
        }
        if (rule == t || t instanceof BnfRule && getCommonSuperRule(rule, (BnfRule)t) != null) {
            myRulesCollapseMap.putValue(rule, t);
            return true;
        }
        else if (isExternalPsi(t) && myRuleExtendsMap.containsScalarValue(rule)) {
            myRulesCollapseMap.putValue(rule, rule);
            return true;
        }
        return false;
    }

    private static <V> Map<PsiElement, V> psiMap(PsiElement k, V v) {
        Map<PsiElement, V> map = Maps.newHashMap(1, 1, CARDINALITY_HASHING_STRATEGY);
        map.put(k, v);
        return map;
    }

    private static <V> Map<PsiElement, V> psiMap(Map<PsiElement, V> map) {
        return Maps.newHashMap(map, CARDINALITY_HASHING_STRATEGY);
    }

    private static <V> Map<PsiElement, V> psiMap() {
        return Maps.newHashMap(3, CARDINALITY_HASHING_STRATEGY);
    }

    /**
     * @noinspection UnusedParameters
     */
    private List<Map<PsiElement, Cardinality>> compactInheritors(
        @Nullable BnfRule forRule,
        @Nonnull List<Map<PsiElement, Cardinality>> mapList
    ) {
        Map<BnfRule, BnfRule> rulesAndAlts = new LinkedHashMap<>();
        Map<PsiElement, BnfRule> externalMap = new LinkedHashMap<>();
        for (Map<PsiElement, Cardinality> map : mapList) {
            for (PsiElement element : map.keySet()) {
                BnfRule r = null;
                if (element instanceof BnfRule) {
                    r = (BnfRule)element;
                }
                else if (isExternalPsi(element) && !element.getText().startsWith("#") && !isDoubleAngles(element.getText())) {
                    String text = element.getText();
                    BnfRule rule = myFile.getRule(text);
                    if (Rule.isExternal(rule)) {
                        r = myFile.getRule(getAttribute(myFile.getVersion(), rule, KnownAttribute.EXTENDS));
                        if (r != null) {
                            externalMap.put(element, r);
                        }
                    }
                }
                if (r != null) {
                    rulesAndAlts.put(r, r);
                }
            }
        }
        //if (forRule != null && "".equals(forRule.getName())) {
        //  int gotcha = 1;
        //}

        boolean hasSynonyms = collectSynonymsAndCollapseAlternatives(rulesAndAlts);
        if (rulesAndAlts.size() < 2) {
            return !hasSynonyms ? mapList : replaceRulesInMaps(mapList, rulesAndAlts, externalMap);
        }
        Set<BnfRule> allRules = new LinkedHashSet<>();
        allRules.addAll(rulesAndAlts.keySet());
        allRules.addAll(rulesAndAlts.values());

        List<Map.Entry<BnfRule, Collection<BnfRule>>> applicableSupers = new ArrayList<>();
        for (Map.Entry<BnfRule, Collection<BnfRule>> e : myRuleExtendsMap.entrySet()) {
            int count = 0;
            for (BnfRule rule : allRules) {
                if (e.getValue().contains(rule)) {
                    count++;
                }
            }
            if (count > 1) {
                applicableSupers.add(e);
            }
        }
        if (applicableSupers.isEmpty()) {
            return !hasSynonyms ? mapList : replaceRulesInMaps(mapList, rulesAndAlts, externalMap);
        }

        findTheBestReplacement(rulesAndAlts, applicableSupers);

        return replaceRulesInMaps(mapList, rulesAndAlts, externalMap);
    }

    private boolean collectSynonymsAndCollapseAlternatives(Map<BnfRule, BnfRule> rulesAndAlts) {
        boolean hasSynonyms = false;
        for (Map.Entry<BnfRule, BnfRule> e : new ArrayList<>(rulesAndAlts.entrySet())) {
            BnfRule rule = e.getKey();
            e.setValue(getSynonymTargetOrSelf(myVersion, rule));
            hasSynonyms |= rule != e.getValue();
            for (PsiElement r : myRulesCollapseMap.get(rule)) {
                if (r instanceof BnfRule && !rulesAndAlts.containsKey(r)) {
                    rulesAndAlts.put((BnfRule)r, (BnfRule)r);
                }
            }
        }
        return hasSynonyms;
    }

    private boolean willCollapse(BnfRule rule, Map<PsiElement, Cardinality> map) {
        if (!canCollapse(rule, map)) {
            return false;
        }

        boolean requiredFound = false;
        for (PsiElement t : map.keySet()) {
            if (PsiUtilCore.getElementType(t) == MARKER_TYPE) {
                continue;
            }
            if (requiredFound || map.get(t) != REQUIRED) {
                return false;
            }
            if (isExternalPsi(t)) {
                return false;
            }
            requiredFound = true;
        }
        return requiredFound;
    }

    private boolean canCollapse(BnfRule rule, Map<PsiElement, Cardinality> map) {
        boolean result = false;
        boolean maybeCollapsed = true;
        PsiElement required = null;
        for (PsiElement t : map.keySet()) {
            if (PsiUtilCore.getElementType(t) == MARKER_TYPE) {
                continue;
            }
            if (!map.get(t).optional()) {
                if (required == null) {
                    required = t;
                    maybeCollapsed = required instanceof BnfRule || isExternalPsi(required);
                }
                else {
                    maybeCollapsed = false;
                }
                if (!maybeCollapsed) {
                    break;
                }
            }
        }
        if (maybeCollapsed) {
            for (PsiElement t : required != null ? Collections.singleton(required) : map.keySet()) {
                result |= canCollapseBy(rule, t);
            }
        }
        return result;
    }

    private static void findTheBestReplacement(
        Map<BnfRule, BnfRule> rulesAndAlts,
        List<Map.Entry<BnfRule, Collection<BnfRule>>> supers
    ) {
        BitSet bits = new BitSet(rulesAndAlts.size());
        int minI = -1, minC = -1, minS = -1;
        for (int len = Math.min(16, supers.size()), i = (1 << len) - 1; i > 0; i--) {
            if (minC != -1 && Integer.bitCount(i) > minC) {
                continue;
            }
            int curC = 0, curS = 0;
            bits.set(0, rulesAndAlts.size(), true);
            for (int j = 0, bit = 1; j < len; j++, bit <<= 1) {
                if ((i & bit) == 0) {
                    continue;
                }
                Collection<BnfRule> vals = supers.get(j).getValue();
                curC += 1;
                curS += vals.size();
                if (bits.isEmpty()) {
                    continue;
                }

                int k = 0;
                for (Map.Entry<BnfRule, BnfRule> e : rulesAndAlts.entrySet()) {
                    if (bits.get(k)) {
                        if (vals.contains(e.getKey()) || vals.contains(e.getValue())) {
                            bits.set(k, false);
                        }
                    }
                    k++;
                }
                if (!bits.isEmpty()) {
                    curC += bits.cardinality();
                    curS += bits.cardinality();
                }
            }
            if (minC == -1 || minC > curC || minC == curC && minS > curS) {
                minC = curC;
                minS = curS;
                minI = i;
            }
        }
        for (Map.Entry<BnfRule, BnfRule> e : rulesAndAlts.entrySet()) {
            for (int len = supers.size(), j = 0, bit = 1; j < len; j++, bit <<= 1) {
                if ((minI & bit) == 0) {
                    continue;
                }
                Collection<BnfRule> vals = supers.get(j).getValue();
                if (vals.contains(e.getKey()) || vals.contains(e.getValue())) {
                    e.setValue(supers.get(j).getKey());
                }
            }
        }
    }

    private static List<Map<PsiElement, Cardinality>> replaceRulesInMaps(
        List<Map<PsiElement, Cardinality>> mapList,
        Map<BnfRule, BnfRule> replacementMap,
        Map<PsiElement, BnfRule> externalMap
    ) {
        List<Map<PsiElement, Cardinality>> result = new ArrayList<>(mapList.size());
        for (Map<PsiElement, Cardinality> map : mapList) {
            Map<PsiElement, Cardinality> copy = psiMap(map);
            result.add(copy);
            for (PsiElement element : map.keySet()) {
                BnfRule rule = isExternalPsi(element) ? externalMap.get(element) : null;
                BnfRule replacement = rule != null ? replacementMap.get(rule) : null;
                if (replacement != null) {
                    copy.put(rule, copy.remove(element));
                }
            }
            for (Map.Entry<BnfRule, BnfRule> e : replacementMap.entrySet()) {
                Cardinality card = copy.remove(e.getKey());
                if (card == null) {
                    continue;
                }
                Cardinality cur = copy.get(e.getValue());
                copy.put(e.getValue(), cur == null ? card : cur.or(card));
            }
        }
        return result;
    }

    public static BnfRule getSynonymTargetOrSelf(String version, BnfRule rule) {
        String attr = getAttribute(version, rule, KnownAttribute.ELEMENT_TYPE);
        if (attr != null) {
            BnfRule realRule = ((BnfFile)rule.getContainingFile()).getRule(attr);
            if (realRule != null && shouldGeneratePsi(realRule, false)) {
                return realRule;
            }
        }
        return rule;
    }

    public static boolean hasPsiClass(BnfRule rule) {
        return shouldGeneratePsi(rule, true);
    }

    public static boolean hasElementType(BnfRule rule) {
        return shouldGeneratePsi(rule, false);
    }

    public static boolean isPrivateOrNoType(BnfRule rule, String version) {
        return Rule.isPrivate(rule) || "".equals(getAttribute(version, rule, KnownAttribute.ELEMENT_TYPE));
    }

    private static boolean shouldGeneratePsi(BnfRule rule, boolean psiClasses) {
        BnfFile containingFile = (BnfFile)rule.getContainingFile();
        BnfRule grammarRoot = containingFile.getRules().get(0);
        if (grammarRoot == rule) {
            return false;
        }
        if (Rule.isPrivate(rule) || Rule.isExternal(rule)) {
            return false;
        }
        String attr = getAttribute(containingFile.getVersion(), rule, KnownAttribute.ELEMENT_TYPE);
        if (!psiClasses) {
            return !"".equals(attr);
        }
        BnfRule thatRule = containingFile.getRule(attr);
        return thatRule == null || thatRule == grammarRoot || Rule.isPrivate(thatRule) || Rule.isExternal(thatRule);
    }

    @Nonnull
    private PsiElement newExternalPsi(String name) {
        PsiElement e = myExternalElements.get(name);
        if (e == null) {
            myExternalElements.put(name, e = new FakeBnfExpression(EXTERNAL_TYPE, name));
        }
        return e;
    }

    private static boolean isExternalPsi(PsiElement t) {
        return t instanceof LeafPsiElement leafPsiElement && leafPsiElement.getElementType() == EXTERNAL_TYPE;
    }
}
