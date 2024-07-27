/*
 * Copyright 2011-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package org.intellij.grammar.generator;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.application.util.matcher.NameUtil;
import consulo.devkit.grammarKit.generator.ErrorReporter;
import consulo.devkit.grammarKit.generator.PlatformClass;
import consulo.language.ast.IElementType;
import consulo.language.impl.psi.LeafPsiElement;
import consulo.language.psi.NavigatablePsiElement;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReference;
import consulo.language.psi.search.ReferencesSearch;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.project.Project;
import consulo.util.collection.*;
import consulo.util.lang.Couple;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.StringUtil;
import org.intellij.grammar.KnownAttribute;
import org.intellij.grammar.java.JavaHelper;
import org.intellij.grammar.psi.*;
import org.intellij.grammar.psi.impl.GrammarUtil;
import org.jetbrains.annotations.TestOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static java.lang.String.format;
import static org.intellij.grammar.generator.RuleGraphHelper.getSynonymTargetOrSelf;
import static org.intellij.grammar.generator.RuleGraphHelper.getTokenNameToTextMap;
import static org.intellij.grammar.psi.BnfTypes.BNF_SEQUENCE;

/**
 * @author gregory
 * Date: 16.07.11 10:41
 */
public class ParserGeneratorUtil {
    private static final String RESERVED_SUFFIX = "_$";
    private static final Set<String> JAVA_RESERVED = new HashSet<>(Arrays.asList(
        "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class",
        "const", "default", "do", "double", "else", "enum", "extends", "false", "final", "finally",
        "float", "for", "goto", "if", "implements", "import", "instanceof", "int", "interface", "long",
        "native", "new", "null", "package", "private", "protected", "public", "return", "short", "static",
        "strictfp", "super", "switch", "synchronized", "this", "throw", "throws", "transient", "true",
        "try", "void", "volatile", "while", "continue"
    ));

    enum ConsumeType {
        FAST,
        SMART,
        DEFAULT;

        @Nonnull
        public String getMethodSuffix() {
            return this == DEFAULT ? "" : StringUtil.capitalize(name().toLowerCase());
        }

        @Nonnull
        public String getMethodName(@Nullable String version) {
            return KnownAttribute.CONSUME_TOKEN_METHOD.getDefaultValue(version) + getMethodSuffix();
        }

        @Nonnull
        public static ConsumeType forRule(@Nullable String version, @Nonnull BnfRule rule) {
            String value = getAttribute(version, rule, KnownAttribute.CONSUME_TOKEN_METHOD);
            for (ConsumeType method : values()) {
                if (StringUtil.equalsIgnoreCase(value, method.name())) {
                    return method;
                }
            }
            return ObjectUtil.chooseNotNull(forMethod(value), DEFAULT);
        }

        @Nullable
        public static ConsumeType forMethod(String value) {
            if ("consumeTokenFast".equals(value)) {
                return FAST;
            }
            if ("consumeTokenSmart".equals(value)) {
                return SMART;
            }
            if ("consumeToken".equals(value)) {
                return DEFAULT;
            }
            return null;
        }

        @Nullable
        public static ConsumeType min(@Nullable ConsumeType a, @Nullable ConsumeType b) {
            if (a == null || b == null) {
                return null;
            }
            return a.compareTo(b) < 0 ? a : b;
        }

        @Nullable
        public static ConsumeType max(@Nullable ConsumeType a, @Nullable ConsumeType b) {
            if (a == null) {
                return b;
            }
            if (b == null) {
                return a;
            }
            return a.compareTo(b) < 0 ? b : a;
        }
    }

    @Nonnull
    public static <T extends Enum<T>> T enumFromString(@Nullable String value, @Nonnull T def) {
        try {
            return value == null ? def : Enum.valueOf(def.getDeclaringClass(), Case.UPPER.apply(value).replace('-', '_'));
        }
        catch (Exception e) {
            return def;
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T getGenerateOption(
        @Nullable String version,
        @Nonnull PsiElement node,
        @Nonnull KnownAttribute<T> attribute,
        @Nonnull Map<String, String> genOptions,
        String... genOptionKeys
    ) {
        String currentValue = JBIterable.of(genOptionKeys).map(genOptions::get).filter(Objects::nonNull).first();
        if (attribute.getDefaultValue(version) instanceof Boolean) {
            if ("yes".equals(currentValue)) {
                return (T)Boolean.TRUE;
            }
            if ("no".equals(currentValue)) {
                return (T)Boolean.FALSE;
            }
        }
        else if (attribute.getDefaultValue(version) instanceof Number) {
            int value = StringUtil.parseInt(currentValue, -1);
            if (value != -1) {
                return (T)Integer.valueOf(value);
            }
        }
        return getRootAttribute(version, node, attribute, null);
    }

    public static <T> T getRootAttribute(@Nullable String version, @Nonnull PsiElement node, @Nonnull KnownAttribute<T> attribute) {
        return getRootAttribute(version, node, attribute, null);
    }

    public static <T> T getRootAttribute(
        @Nullable String version,
        @Nonnull PsiElement node,
        @Nonnull KnownAttribute<T> attribute,
        @Nullable String match
    ) {
        return ((BnfFile)node.getContainingFile()).findAttributeValue(version, null, attribute, match);
    }

    public static <T> T getAttribute(@Nullable String version, @Nonnull BnfRule rule, @Nonnull KnownAttribute<T> attribute) {
        return getAttribute(version, rule, attribute, null);
    }

    @Nullable
    public static <T> BnfAttr findAttribute(@Nullable String version, @Nonnull BnfRule rule, @Nonnull KnownAttribute<T> attribute) {
        return ((BnfFile)rule.getContainingFile()).findAttribute(version, rule, attribute, null);
    }

    public static <T> T getAttribute(
        @Nullable String version,
        @Nonnull BnfRule rule,
        @Nonnull KnownAttribute<T> attribute,
        @Nullable String match
    ) {
        return ((BnfFile)rule.getContainingFile()).findAttributeValue(version, rule, attribute, match);
    }

    @RequiredReadAction
    public static Object getAttributeValue(BnfExpression value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BnfReferenceOrToken referenceOrToken) {
            return getTokenValue(referenceOrToken);
        }
        else if (value instanceof BnfLiteralExpression literalExpression) {
            return getLiteralValue(literalExpression);
        }
        else if (value instanceof BnfValueList) {
            KnownAttribute.ListValue pairs = new KnownAttribute.ListValue();
            for (BnfListEntry o : ((BnfValueList)value).getListEntryList()) {
                PsiElement id = o.getId();
                String v2 = getLiteralValue(o.getLiteralExpression());
                pairs.add(Couple.of(
                    id == null ? null : new KnownAttribute.ListValueObject(id.getText()),
                    v2 == null ? null : new KnownAttribute.ListValueObject(v2)
                ));
            }
            return pairs;
        }
        return null;
    }

    @Nullable
    @RequiredReadAction
    public static String getLiteralValue(BnfStringLiteralExpression child) {
        return getLiteralValue((BnfLiteralExpression)child);
    }

    @Nullable
    @RequiredReadAction
    @SuppressWarnings("unchecked")
    public static <T> T getLiteralValue(BnfLiteralExpression child) {
        if (child == null) {
            return null;
        }
        PsiElement literal = PsiTreeUtil.getDeepestFirst(child);
        String text = child.getText();
        IElementType elementType = literal.getNode().getElementType();
        if (elementType == BnfTypes.BNF_NUMBER) {
            return (T)Integer.valueOf(text);
        }
        if (elementType == BnfTypes.BNF_STRING) {
            String unquoted = GrammarUtil.unquote(text);
            // in double-quoted strings: un-escape quotes only leaving the rest \ manageable
            String result = text.charAt(0) == '"' ? unquoted.replaceAll("\\\\([\"'])", "$1") : unquoted;
            return (T)result;
        }
        return null;
    }

    @RequiredReadAction
    private static Object getTokenValue(BnfReferenceOrToken child) {
        String text = child.getText();
        if (text.equals("true")) {
            return true;
        }
        if (text.equals("false")) {
            return false;
        }
        return GrammarUtil.getIdText(child);
    }

    @RequiredReadAction
    public static boolean isTrivialNode(PsiElement element) {
        return getTrivialNodeChild(element) != null;
    }

    @RequiredReadAction
    public static BnfExpression getNonTrivialNode(BnfExpression initialNode) {
        BnfExpression nonTrivialNode = initialNode;
        for (BnfExpression e = initialNode, n = getTrivialNodeChild(e); n != null; e = n, n = getTrivialNodeChild(e)) {
            nonTrivialNode = n;
        }
        return nonTrivialNode;
    }

    @RequiredReadAction
    public static BnfExpression getTrivialNodeChild(PsiElement element) {
        PsiElement child = null;
        if (element instanceof BnfParenthesized) {
            BnfExpression e = ((BnfParenthesized)element).getExpression();
            if (element instanceof BnfParenExpression) {
                child = e;
            }
            else {
                BnfExpression c = e;
                while (c instanceof BnfParenthesized) {
                    c = ((BnfParenthesized)c).getExpression();
                }
                if (c.getFirstChild() == null) {
                    child = e;
                }
            }
        }
        else if (element.getFirstChild() == element.getLastChild() && element instanceof BnfExpression) {
            child = element.getFirstChild();
        }
        return child instanceof BnfExpression && !(child instanceof BnfLiteralExpression || child instanceof BnfReferenceOrToken)
            ? (BnfExpression)child : null;
    }

    @RequiredReadAction
    public static IElementType getEffectiveType(PsiElement tree) {
        if (tree instanceof BnfParenOptExpression) {
            return BnfTypes.BNF_OP_OPT;
        }
        else if (tree instanceof BnfQuantified) {
            final BnfQuantifier quantifier = ((BnfQuantified)tree).getQuantifier();
            return PsiTreeUtil.getDeepestFirst(quantifier).getNode().getElementType();
        }
        else if (tree instanceof BnfPredicate predicate) {
            return predicate.getPredicateSign().getFirstChild().getNode().getElementType();
        }
        else if (tree instanceof BnfStringLiteralExpression) {
            return BnfTypes.BNF_STRING;
        }
        else if (tree instanceof BnfLiteralExpression) {
            return tree.getFirstChild().getNode().getElementType();
        }
        else if (tree instanceof BnfParenExpression) {
            return BnfTypes.BNF_SEQUENCE;
        }
        else {
            return tree.getNode().getElementType();
        }
    }

    public static List<BnfExpression> getChildExpressions(@Nullable BnfExpression node) {
        return PsiTreeUtil.getChildrenOfTypeAsList(node, BnfExpression.class);
    }

    @Nonnull
    private static String getBaseName(@Nonnull String name) {
        return toIdentifier(name, null, Case.AS_IS);
    }

    public static String getFuncName(@Nonnull BnfRule r) {
        String name = getBaseName(r.getName());
        return JAVA_RESERVED.contains(name) ? name + RESERVED_SUFFIX : name;
    }

    @Nonnull
    static String getWrapperParserConstantName(@Nonnull String nextName) {
        return getBaseName(nextName) + "_parser_";
    }

    @Nonnull
    static String getWrapperParserMetaMethodName(@Nonnull String nextName) {
        return getBaseName(nextName) + RESERVED_SUFFIX;
    }

    public static String getNextName(@Nonnull String funcName, int i) {
        return StringUtil.trimEnd(funcName, RESERVED_SUFFIX) + "_" + i;
    }

    @Nonnull
    public static String getGetterName(@Nonnull String text) {
        return toIdentifier(text, NameFormat.from("get"), Case.CAMEL);
    }

    @Nonnull
    static String getTokenSetConstantName(@Nonnull String nextName) {
        return toIdentifier(nextName, null, Case.UPPER) + "_TOKENS";
    }

    @RequiredReadAction
    public static boolean isRollbackRequired(@Nullable String version, BnfExpression o, BnfFile file) {
        if (o instanceof BnfStringLiteralExpression) {
            return false;
        }
        if (!(o instanceof BnfReferenceOrToken)) {
            return true;
        }
        String value = GrammarUtil.stripQuotesAroundId(o.getText());
        BnfRule subRule = file.getRule(value);
        if (subRule == null) {
            return false;
        }
        return getAttribute(version, subRule, KnownAttribute.RECOVER_WHILE) != null
            || !getAttribute(version, subRule, KnownAttribute.HOOKS).isEmpty()
            || Rule.isExternal(subRule);
    }

    @TestOnly
    @Nonnull
    public static String toIdentifier(@Nonnull String text, @Nullable NameFormat format, @Nonnull Case cas) {
        if (text.isEmpty()) {
            return "";
        }
        String fixed = text.replaceAll("[^:\\p{javaJavaIdentifierPart}]", "_");
        boolean allCaps = Case.UPPER.apply(fixed).equals(fixed);
        StringBuilder sb = new StringBuilder();
        if (!Character.isJavaIdentifierStart(fixed.charAt(0)) && sb.length() == 0) {
            sb.append("_");
        }
        String[] strings = NameUtil.nameToWords(fixed);
        for (int i = 0, len = strings.length; i < len; i++) {
            String s = strings[i];
            if (cas == Case.CAMEL && s.startsWith("_") && !(i == 0 || i == len - 1)) {
                continue;
            }
            if (cas == Case.UPPER && !s.startsWith("_") && !(i == 0 || StringUtil.endsWith(sb, "_"))) {
                sb.append("_");
            }
            if (cas == Case.CAMEL && !allCaps && Case.UPPER.apply(s).equals(s)) {
                sb.append(s);
            }
            else {
                sb.append(cas.apply(s));
            }
        }
        return format == null ? sb.toString() : format.apply(sb.toString());
    }

    @Nonnull
    public static NameFormat getPsiClassFormat(BnfFile file) {
        String version = file.findAttributeValue(null, null, KnownAttribute.VERSION, null);
        return NameFormat.from(getRootAttribute(version, file, KnownAttribute.PSI_CLASS_PREFIX));
    }

    @Nonnull
    public static NameFormat getPsiImplClassFormat(BnfFile file) {
        String version = file.findAttributeValue(null, null, KnownAttribute.VERSION, null);

        String prefix = getRootAttribute(version, file, KnownAttribute.PSI_CLASS_PREFIX);
        String suffix = getRootAttribute(version, file, KnownAttribute.PSI_IMPL_CLASS_SUFFIX);
        return NameFormat.from(prefix + "/" + StringUtil.notNullize(suffix));
    }

    @Nonnull
    public static String getRulePsiClassName(@Nonnull BnfRule rule, @Nullable NameFormat format) {
        return toIdentifier(rule.getName(), format, Case.CAMEL);
    }

    public static Couple<String> getQualifiedRuleClassName(BnfRule rule) {
        BnfFile file = (BnfFile)rule.getContainingFile();
        String version = file.findAttributeValue(null, null, KnownAttribute.VERSION, null);

        String psiPackage = getAttribute(version, rule, KnownAttribute.PSI_PACKAGE);
        String psiImplPackage = getAttribute(version, rule, KnownAttribute.PSI_IMPL_PACKAGE);
        NameFormat psiFormat = getPsiClassFormat(file);
        NameFormat psiImplFormat = getPsiImplClassFormat(file);
        return Couple.of(
            psiPackage + "." + getRulePsiClassName(rule, psiFormat),
            psiImplPackage + "." + getRulePsiClassName(rule, psiImplFormat)
        );
    }

    @Nonnull
    public static List<NavigatablePsiElement> findRuleImplMethods(
        String version,
        @Nonnull JavaHelper helper,
        @Nullable String psiImplUtilClass,
        @Nullable String methodName,
        @Nullable BnfRule rule
    ) {
        if (rule == null) {
            return Collections.emptyList();
        }
        List<NavigatablePsiElement> methods = Collections.emptyList();
        String selectedSuperClass = null;
        main:
        for (String ruleClass : getRuleClasses(rule)) {
            for (String utilClass = psiImplUtilClass; utilClass != null; utilClass = helper.getSuperClassName(utilClass)) {
                methods = helper.findClassMethods(version, utilClass, JavaHelper.MethodType.STATIC, methodName, -1, ruleClass);
                selectedSuperClass = ruleClass;
                if (!methods.isEmpty()) {
                    break main;
                }
            }
        }
        return filterOutShadowedRuleImplMethods(version, selectedSuperClass, methods, helper);
    }

    @Nonnull
    private static List<NavigatablePsiElement> filterOutShadowedRuleImplMethods(
        @Nullable String version,
        String selectedClass,
        List<NavigatablePsiElement> methods,
        @Nonnull JavaHelper helper
    ) {
        if (methods.size() <= 1) {
            return methods;
        }

        // filter out less specific methods
        // todo move to JavaHelper
        List<NavigatablePsiElement> result = new ArrayList<>(methods);
        Map<String, NavigatablePsiElement> prototypes = new LinkedHashMap<>();
        for (NavigatablePsiElement m2 : methods) {
            List<String> types = helper.getMethodTypes(version, m2);
            String proto = m2.getName() + types.subList(3, types.size());
            NavigatablePsiElement m1 = prototypes.get(proto);
            if (m1 == null) {
                prototypes.put(proto, m2);
                continue;
            }
            String type1 = helper.getMethodTypes(version, m1).get(1);
            String type2 = types.get(1);
            if (Objects.equals(type1, type2)) {
                continue;
            }
            for (String s = selectedClass; s != null; s = helper.getSuperClassName(s)) {
                if (Objects.equals(type1, s)) {
                    result.remove(m2);
                }
                else if (Objects.equals(type2, s)) {
                    result.remove(m1);
                }
                else {
                    continue;
                }
                break;
            }
        }
        return result;
    }

    @Nonnull
    public static Set<String> getRuleClasses(@Nonnull BnfRule rule) {
        Set<String> result = new LinkedHashSet<>();
        BnfFile file = (BnfFile)rule.getContainingFile();

        String version = file.findAttributeValue(null, null, KnownAttribute.VERSION, null);

        BnfRule topSuper = getEffectiveSuperRule(file, rule);
        String superClassName = topSuper == null ? getRootAttribute(version, file, KnownAttribute.EXTENDS) :
            topSuper == rule ? getAttribute(version, rule, KnownAttribute.EXTENDS) :
                getAttribute(version, topSuper, KnownAttribute.PSI_PACKAGE) + "." +
                    getRulePsiClassName(topSuper, getPsiClassFormat(file));
        String implSuper = StringUtil.notNullize(getAttribute(version, rule, KnownAttribute.MIXIN), superClassName);
        Couple<String> names = getQualifiedRuleClassName(rule);
        result.add(names.first);
        result.add(names.second);
        result.add(superClassName);
        result.add(implSuper);
        result.addAll(getSuperInterfaceNames(file, rule, getPsiClassFormat(file)));
        return result;
    }

    @Nonnull
    static JBIterable<BnfRule> getSuperRules(@Nonnull BnfFile file, @Nullable BnfRule rule) {
        String version = file.findAttributeValue(null, null, KnownAttribute.VERSION, null);

        JBIterable<Object> result = JBIterable.generate(rule, new JBIterable.SFun<Object, Object>() {
            Set<BnfRule> visited;

            @Override
            public Object apply(Object o) {
                if (o == ObjectUtil.NULL) {
                    return null;
                }
                BnfRule cur = (BnfRule)o;
                if (visited == null) {
                    visited = new HashSet<>();
                }
                if (!visited.add(cur)) {
                    return ObjectUtil.NULL;
                }
                BnfRule next = getSynonymTargetOrSelf(version, cur);
                if (next != cur) {
                    return next;
                }
                if (cur != rule) {
                    return null; // do not search for elementType any further
                }
                String attr = getAttribute(version, cur, KnownAttribute.EXTENDS);
                //noinspection StringEquality
                BnfRule ext = attr != KnownAttribute.EXTENDS.getDefaultValue(version) ? file.getRule(attr) : null;
                return ext == null && attr != null ? null : ext;
            }
        }).map(o -> o == ObjectUtil.NULL ? null : o);
        return (JBIterable<BnfRule>)(JBIterable<?>)result;
    }

    @Nullable
    static BnfRule getEffectiveSuperRule(@Nonnull BnfFile file, @Nullable BnfRule rule) {
        return getSuperRules(file, rule).last();
    }

    @Nonnull
    static List<String> getSuperInterfaceNames(BnfFile file, BnfRule rule, NameFormat format) {
        String version = file.findAttributeValue(null, null, KnownAttribute.VERSION, null);

        List<String> strings = new ArrayList<>();
        List<String> topRuleImplements = Collections.emptyList();
        String topRuleClass = null;
        BnfRule topSuper = getEffectiveSuperRule(file, rule);
        if (topSuper != null && topSuper != rule) {
            topRuleImplements = getAttribute(version, topSuper, KnownAttribute.IMPLEMENTS).asStrings(version);
            topRuleClass = getAttribute(version, topSuper, KnownAttribute.PSI_PACKAGE) + "." + getRulePsiClassName(topSuper, format);
            if (!StringUtil.isEmpty(topRuleClass)) {
                strings.add(topRuleClass);
            }
        }
        List<String> rootImplements = getRootAttribute(version, file, KnownAttribute.IMPLEMENTS).asStrings(version);
        List<String> ruleImplements = getAttribute(version, rule, KnownAttribute.IMPLEMENTS).asStrings(version);
        for (String className : ruleImplements) {
            if (className == null) {
                continue;
            }
            BnfRule superIntfRule = file.getRule(className);
            if (superIntfRule != null) {
                strings.add(getAttribute(version, superIntfRule, KnownAttribute.PSI_PACKAGE) + "." + getRulePsiClassName(
                    superIntfRule,
                    format
                ));
            }
            else if (!topRuleImplements.contains(className) &&
                (topRuleClass == null || !rootImplements.contains(className))) {
                if (strings.size() == 1 && topSuper == null) {
                    strings.add(0, className);
                }
                else {
                    strings.add(className);
                }
            }
        }
        return strings;
    }

    @Nullable
    public static String getRuleDisplayName(String version, BnfRule rule, boolean force) {
        String s = getRuleDisplayNameRaw(version, rule, force);
        return StringUtil.isEmpty(s) ? null : "<" + s + ">";
    }

    @Nullable
    private static String getRuleDisplayNameRaw(String version, BnfRule rule, boolean force) {
        String name = getAttribute(version, rule, KnownAttribute.NAME);
        BnfRule realRule = rule;
        if (name != null) {
            realRule = ((BnfFile)rule.getContainingFile()).getRule(name);
            if (realRule != null && realRule != rule) {
                name = getAttribute(version, realRule, KnownAttribute.NAME);
            }
        }
        if (name != null || (!force && realRule == rule)) {
            return name;
        }
        else {
            String[] parts = NameUtil.splitNameIntoWords(getFuncName(realRule));
            return Case.LOWER.apply(StringUtil.join(parts, " "));
        }
    }

    public static String getElementType(String version, BnfRule rule, @Nonnull Case cas) {
        String elementType = getAttribute(version, rule, KnownAttribute.ELEMENT_TYPE);
        if ("".equals(elementType)) {
            return "";
        }
        NameFormat prefix = NameFormat.from(getAttribute(version, rule, KnownAttribute.ELEMENT_TYPE_PREFIX));
        return toIdentifier(elementType != null ? elementType : rule.getName(), prefix, cas);
    }

    public static String getTokenType(BnfFile file, String token, @Nonnull Case cas) {
        String version = file.findAttributeValue(null, null, KnownAttribute.VERSION, null);

        NameFormat format = NameFormat.from(getRootAttribute(version, file, KnownAttribute.ELEMENT_TYPE_PREFIX));
        String fixed = cas.apply(token.replaceAll("[^:\\p{javaJavaIdentifierPart}]", "_"));
        return format == null ? fixed : format.apply(fixed);
    }

    @RequiredReadAction
    public static Collection<BnfRule> getSortedPublicRules(Set<PsiElement> accessors) {
        Map<String, BnfRule> result = new TreeMap<>();
        for (PsiElement tree : accessors) {
            if (tree instanceof BnfRule rule && !Rule.isPrivate(rule)) {
                result.put(rule.getName(), rule);
            }
        }
        return result.values();
    }

    @RequiredReadAction
    public static Collection<BnfExpression> getSortedTokens(Set<PsiElement> accessors) {
        Map<String, BnfExpression> result = new TreeMap<>();
        for (PsiElement tree : accessors) {
            if (!(tree instanceof BnfReferenceOrToken || tree instanceof BnfLiteralExpression)) {
                continue;
            }
            result.put(tree.getText(), (BnfExpression)tree);
        }
        return result.values();
    }

    @RequiredReadAction
    public static Collection<LeafPsiElement> getSortedExternalRules(Set<PsiElement> accessors) {
        Map<String, LeafPsiElement> result = new TreeMap<>();
        for (PsiElement tree : accessors) {
            if (!(tree instanceof LeafPsiElement)) {
                continue;
            }
            result.put(tree.getText(), (LeafPsiElement)tree);
        }
        return result.values();
    }

    public static List<BnfRule> topoSort(@Nonnull Collection<BnfRule> rules, @Nonnull RuleGraphHelper ruleGraph) {
        Set<BnfRule> rulesSet = new HashSet<>(rules);
        return new JBTreeTraverser<BnfRule>(
            rule -> JBIterable.from(ruleGraph.getSubRules(rule)).filter(rulesSet::contains))
            .withRoots(ContainerUtil.reverse(new ArrayList<>(rules)))
            .withTraversal(TreeTraversal.POST_ORDER_DFS)
            .unique()
            .toList();
    }

    public static void addWarning(Project project, String s, Object... args) {
        addWarning(project, format(s, args));
    }

    public static void addWarning(Project project, String text) {
        if (Application.get().isUnitTestMode()) {
            //noinspection UseOfSystemOutOrSystemErr
            System.out.println(text);
        }
        else {
            ErrorReporter.ourInstance.reportWarning(project, text);
        }
    }

    public static void checkClassAvailability(@Nonnull BnfFile file, @Nullable String className, @Nullable String description) {
        if (StringUtil.isEmpty(className)) {
            return;
        }

        JavaHelper javaHelper = JavaHelper.getJavaHelper(file);
        if (javaHelper.findClass(className) == null) {
            String tail = StringUtil.isEmpty(description) ? "" : " (" + description + ")";
            addWarning(file.getProject(), className + " class not found" + tail);
        }
    }

    public static boolean isRegexpToken(@Nonnull String tokenText) {
        return tokenText.startsWith(BnfConstants.REGEXP_PREFIX);
    }

    public static String getRegexpTokenRegexp(@Nonnull String tokenText) {
        return tokenText.substring(BnfConstants.REGEXP_PREFIX.length());
    }

    @Nullable
    @RequiredReadAction
    static Collection<String> getTokenNames(@Nonnull BnfFile file, @Nonnull List<BnfExpression> expressions) {
        return getTokenNames(file, expressions, -1);
    }

    // when some expression is not a token or total tokens count is less than or equals threshold
    @Nullable
    @RequiredReadAction
    static Collection<String> getTokenNames(@Nonnull BnfFile file, @Nonnull List<BnfExpression> expressions, int threshold) {
        Set<String> tokens = new LinkedHashSet<>();
        for (BnfExpression expression : expressions) {
            String token = getTokenName(file, expression);
            if (token == null) {
                return null;
            }
            else {
                tokens.add(token);
            }
        }
        return tokens.size() > threshold ? tokens : null;
    }

    @RequiredReadAction
    private static String getTokenName(@Nonnull BnfFile file, @Nonnull BnfExpression expression) {
        String text = expression.getText();
        if (expression instanceof BnfStringLiteralExpression) {
            return RuleGraphHelper.getTokenTextToNameMap(file).get(GrammarUtil.unquote(text));
        }
        else if (expression instanceof BnfReferenceOrToken) {
            return file.getRule(text) == null ? text : null;
        }
        else {
            return null;
        }
    }

    @RequiredReadAction
    public static boolean isTokenSequence(@Nullable String version, @Nonnull BnfRule rule, @Nullable BnfExpression node) {
        if (node == null || ConsumeType.forRule(version, rule) != ConsumeType.DEFAULT) {
            return false;
        }
        if (getEffectiveType(node) != BNF_SEQUENCE) {
            return false;
        }
        BnfFile file = (BnfFile)rule.getContainingFile();
        return getTokenNames(file, getChildExpressions(node)) != null;
    }

    @RequiredReadAction
    private static boolean isTokenChoice(@Nonnull BnfFile file, @Nonnull BnfExpression choice) {
        return choice instanceof BnfChoice && getTokenNames(file, ((BnfChoice)choice).getExpressionList(), 2) != null;
    }

    @RequiredReadAction
    static boolean hasAtLeastOneTokenChoice(@Nonnull BnfFile file, @Nonnull Collection<String> ownRuleNames) {
        for (String ruleName : ownRuleNames) {
            final BnfRule rule = file.getRule(ruleName);
            if (rule == null) {
                continue;
            }
            final BnfExpression expression = rule.getExpression();
            if (isTokenChoice(file, expression)) {
                return true;
            }
        }
        return false;
    }

    public static void appendTokenTypes(StringBuilder sb, List<String> tokenTypes) {
        for (int count = 0, line = 0, size = tokenTypes.size(); count < size; count++) {
            boolean newLine = line == 0 && count == 2 || line > 0 && (count - 2) % 6 == 0;
            newLine &= (size - count) > 2;
            if (count > 0) {
                sb.append(",").append(newLine ? "\n" : " ");
            }
            sb.append(tokenTypes.get(count));
            if (newLine) {
                line++;
            }
        }
    }

    private static Collection<String> addNewLines(Collection<String> strings) {
        if (strings.size() < 5) {
            return strings;
        }
        List<String> result = new ArrayList<>();
        int counter = 0;
        for (String string : strings) {
            if (counter > 0 && counter % 4 == 0) {
                result.add("\n" + string);
            }
            else {
                result.add(string);
            }
            counter++;
        }
        return result;
    }

    static String tokenSetString(Collection<String> tokens) {
        String string = String.join(", ", addNewLines(tokens));
        if (tokens.size() < 5) {
            return string;
        }
        else {
            return "\n" + string + "\n";
        }
    }

    public static Map<String, String> collectTokenPattern2Name(
        @Nonnull final BnfFile file,
        final boolean createTokenIfMissing,
        @Nonnull final Map<String, String> map,
        @Nullable Set<String> usedInGrammar
    ) {
        final Set<String> usedNames = usedInGrammar != null ? usedInGrammar : new LinkedHashSet<>();
        final Map<String, String> origTokens = RuleGraphHelper.getTokenTextToNameMap(file);
        final Pattern pattern = getAllTokenPattern(origTokens);
        final int[] autoCount = {0};
        final Set<String> origTokenNames = getTokenNameToTextMap(file).keySet();

        BnfVisitor<Void> visitor = new BnfVisitor<>() {
            @Override
            @RequiredReadAction
            public Void visitStringLiteralExpression(@Nonnull BnfStringLiteralExpression o) {
                String text = o.getText();
                String tokenText = GrammarUtil.unquote(text);
                // add auto-XXX token for all unmatched strings to avoid BAD_CHARACTER's
                if (createTokenIfMissing &&
                    !usedNames.contains(tokenText) &&
                    !StringUtil.isJavaIdentifier(tokenText) &&
                    (pattern == null || !pattern.matcher(tokenText).matches())) {
                    String tokenName = "_AUTO_" + (autoCount[0]++);
                    usedNames.add(text);
                    map.put(tokenText, tokenName);
                }
                else {
                    ContainerUtil.addIfNotNull(usedNames, origTokens.get(tokenText));
                }
                return null;
            }

            @Override
            @RequiredReadAction
            public Void visitReferenceOrToken(@Nonnull BnfReferenceOrToken o) {
                if (GrammarUtil.isExternalReference(o)) {
                    return null;
                }
                BnfRule rule = o.resolveRule();
                if (rule != null) {
                    return null;
                }
                String tokenName = o.getText();
                if (usedNames.add(tokenName) && !origTokenNames.contains(tokenName)) {
                    map.put(tokenName, tokenName);
                }
                return null;
            }
        };
        for (BnfExpression o : GrammarUtil.bnfTraverserNoAttrs(file).filter(BnfExpression.class)) {
            o.accept(visitor);
        }
        // fix ordering: origTokens go _after_ to handle keywords correctly
        for (String tokenText : origTokens.keySet()) {
            String tokenName = origTokens.get(tokenText);
            map.remove(tokenText);
            map.put(tokenText, tokenName != null || !createTokenIfMissing ? tokenName : "_AUTO_" + (autoCount[0]++));
        }
        return map;
    }

    @RequiredReadAction
    static boolean isUsedAsArgument(@Nonnull BnfRule rule) {
        return !ReferencesSearch.search(rule, rule.getUseScope()).forEach(ref -> !isUsedAsArgument(ref));
    }

    @RequiredReadAction
    private static boolean isUsedAsArgument(@Nonnull PsiReference ref) {
        PsiElement element = ref.getElement();
        if (!(element instanceof BnfExpression)) {
            return false;
        }
        PsiElement parent = element.getParent();
        if (!(parent instanceof BnfExternalExpression externalExpression && externalExpression.getRefElement() == element)) {
            return false;
        }
        return isArgument((BnfExpression)parent);
    }

    static boolean isArgument(@Nonnull BnfExpression expr) {
        PsiElement parent = expr.getParent();
        return parent instanceof BnfExternalExpression externalExpression && externalExpression.getArguments().contains(expr);
    }

    public static class Rule {

        @RequiredReadAction
        public static boolean isPrivate(BnfRule node) {
            return hasModifier(node, "private");
        }

        @RequiredReadAction
        public static boolean isExternal(BnfRule node) {
            return hasModifier(node, "external");
        }

        @RequiredReadAction
        public static boolean isMeta(BnfRule node) {
            return hasModifier(node, "meta");
        }

        @RequiredReadAction
        public static boolean isLeft(BnfRule node) {
            return hasModifier(node, "left");
        }

        @RequiredReadAction
        public static boolean isInner(BnfRule node) {
            return hasModifier(node, "inner");
        }

        @RequiredReadAction
        public static boolean isFake(BnfRule node) {
            return hasModifier(node, "fake");
        }

        @RequiredReadAction
        public static boolean isUpper(BnfRule node) {
            return hasModifier(node, "upper");
        }

        @RequiredReadAction
        private static boolean hasModifier(@Nullable BnfRule rule, @Nonnull String s) {
            if (rule == null) {
                return false;
            }
            for (BnfModifier modifier : rule.getModifierList()) {
                if (s.equals(modifier.getText())) {
                    return true;
                }
            }
            return false;
        }

        @RequiredReadAction
        public static PsiElement firstNotTrivial(BnfRule rule) {
            for (PsiElement tree = rule.getExpression(); tree != null; tree = PsiTreeUtil.getChildOfType(tree, BnfExpression.class)) {
                if (!isTrivialNode(tree)) {
                    return tree;
                }
            }
            return null;
        }

        public static BnfRule of(BnfExpression expr) {
            return PsiTreeUtil.getParentOfType(expr, BnfRule.class);
        }
    }

    @Nullable
    public static String quote(@Nullable String text) {
        if (text == null) {
            return null;
        }
        return "\"" + text + "\"";
    }

    @Nullable
    public static Pattern compilePattern(String text) {
        try {
            return Pattern.compile(text);
        }
        catch (PatternSyntaxException e) {
            return null;
        }
    }

    public static boolean matchesAny(String regexp, String... text) {
        try {
            Pattern p = Pattern.compile(regexp);
            for (String s : text) {
                if (p.matcher(s).matches()) {
                    return true;
                }
            }
        }
        catch (PatternSyntaxException ignored) {
        }
        return false;
    }

    @Nullable
    public static Pattern getAllTokenPattern(Map<String, String> tokens) {
        StringBuilder sb = new StringBuilder();
        for (String pattern : tokens.keySet()) {
            if (!isRegexpToken(pattern)) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append("|");
            }
            sb.append(getRegexpTokenRegexp(pattern));
        }
        return compilePattern(sb.toString());
    }

    public static class PinMatcher {

        public final BnfRule rule;
        public final String funcName;
        public final Object pinValue;
        private final int pinIndex;
        private final Pattern pinPattern;

        public PinMatcher(@Nullable String version, BnfRule rule, IElementType type, String funcName) {
            this.rule = rule;
            this.funcName = funcName;
            pinValue = type == BNF_SEQUENCE ? getAttribute(version, rule, KnownAttribute.PIN, funcName) : null;
            pinIndex = pinValue instanceof Integer intValue ? intValue : -1;
            pinPattern = pinValue instanceof String strValue ? compilePattern(strValue) : null;
        }

        public boolean active() {
            return pinIndex > -1 || pinPattern != null;
        }

        @RequiredReadAction
        public boolean matches(int i, BnfExpression child) {
            return i == pinIndex - 1 || pinPattern != null && pinPattern.matcher(child.getText()).matches();
        }

        @RequiredReadAction
        public boolean shouldGenerate(List<BnfExpression> children) {
            // do not check last expression, last item pin is trivial
            for (int i = 0, size = children.size(); i < size - 1; i++) {
                if (matches(i, children.get(i))) {
                    return true;
                }
            }
            return false;
        }
    }

    public static String getParametersString(
        ParserGenerator parserGenerator,
        List<String> paramsTypes,
        int offset,
        int mask,
        Function<String, String> substitutor,
        Function<Integer, List<String>> annoProvider,
        NameShortener shortener
    ) {
        StringBuilder sb = new StringBuilder();
        for (int i = offset; i < paramsTypes.size(); i += 2) {
            if (i > offset) {
                sb.append(", ");
            }
            String type = paramsTypes.get(i);
            String name = paramsTypes.get(i + 1);
            if (type.startsWith("<") && type.endsWith(">")) {
                type = substitutor.apply(type);
            }
            if (type.endsWith(parserGenerator.getClassName(PlatformClass.AST_NODE))) {
                name = "node";
            }
            if (type.endsWith("ElementType")) {
                name = "type";
            }
            if (type.endsWith("Stub")) {
                name = "stub";
            }
            if ((mask & 1) == 1) {
                List<String> annos = annoProvider.apply(i);
                for (String s : annos) {
                    if (s.startsWith("kotlin.")) {
                        continue;
                    }
                    sb.append("@").append(shortener.shorten(s)).append(" ");
                }
                sb.append(shortener.shorten(type));
            }
            if ((mask & 3) == 3) {
                sb.append(" ");
            }
            if ((mask & 2) == 2) {
                sb.append(name);
            }
        }
        return sb.toString();
    }

    public static String getGenericClauseString(List<JavaHelper.TypeParameterInfo> genericParameters, NameShortener shortener) {
        if (genericParameters.isEmpty()) {
            return "";
        }

        StringBuilder buffer = new StringBuilder();
        buffer.append('<');
        for (int i = 0; i < genericParameters.size(); i++) {
            if (i > 0) {
                buffer.append(", ");
            }

            JavaHelper.TypeParameterInfo parameter = genericParameters.get(i);
            buffer.append(parameter.getName());

            List<String> extendsList = parameter.getExtendsList();
            if (!extendsList.isEmpty()) {
                buffer.append(" extends ");
                for (int i1 = 0; i1 < extendsList.size(); i1++) {
                    if (i1 > 0) {
                        buffer.append(" & ");
                    }
                    String superType = extendsList.get(i1);
                    String shortened = shortener.shorten(superType);
                    buffer.append(shortened);
                }
            }
        }

        buffer.append("> ");
        return buffer.toString();
    }

    @Nonnull
    public static String getThrowsString(List<String> exceptionList, NameShortener shortener) {
        if (exceptionList.isEmpty()) {
            return "";
        }

        List<String> shortened = ContainerUtil.map(exceptionList, shortener::shorten);

        StringBuilder buffer = new StringBuilder();
        buffer.append(" throws ");
        for (int i = 0; i < shortened.size(); i++) {
            if (i != 0) {
                buffer.append(", ");
            }
            buffer.append(shortened.get(i));
        }
        return buffer.toString();
    }

    public static class NameFormat {
        final static NameFormat EMPTY = new NameFormat("");

        final String prefix;
        final String suffix;

        public static NameFormat from(@Nullable String format) {
            return StringUtil.isEmpty(format) ? EMPTY : new NameFormat(format);
        }

        private NameFormat(@Nullable String format) {
            JBIterable<String> parts = JBIterable.of(format == null ? null : format.split("/"));
            prefix = parts.get(0);
            suffix = StringUtil.join(parts.skip(1), "");
        }

        public String apply(String s) {
            if (prefix != null) {
                s = prefix + s;
            }
            if (suffix != null) {
                s += suffix;
            }
            return s;
        }

        public String strip(String s) {
            if (prefix != null && s.startsWith(prefix)) {
                s = s.substring(prefix.length());
            }
            if (suffix != null && s.endsWith(suffix)) {
                s = s.substring(0, s.length() - suffix.length());
            }
            return s;
        }

    }

    @Nonnull
    static String staticStarImport(@Nonnull String fqn) {
        return "static " + fqn + ".*";
    }

    private static final HashingStrategy<PsiElement> TEXT_STRATEGY = new HashingStrategy<>() {
        @Override
        @RequiredReadAction
        public int hashCode(PsiElement e) {
            return e.getText().hashCode();
        }

        @Override
        @RequiredReadAction
        public boolean equals(PsiElement e1, PsiElement e2) {
            return Objects.equals(e1.getText(), e2.getText());
        }
    };

    public static <T extends PsiElement> HashingStrategy<T> textStrategy() {
        return (HashingStrategy<T>)TEXT_STRATEGY;
    }

    @Nonnull
    static <K extends Comparable<? super K>, V> Map<K, V> take(@Nonnull Map<K, V> map) {
        Map<K, V> result = new TreeMap<>(map);
        map.clear();
        return result;
    }
}
