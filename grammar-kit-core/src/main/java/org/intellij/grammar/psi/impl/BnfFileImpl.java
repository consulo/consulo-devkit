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
package org.intellij.grammar.psi.impl;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.util.CachedValue;
import consulo.application.util.CachedValueProvider;
import consulo.application.util.CachedValuesManager;
import consulo.document.util.TextRange;
import consulo.language.file.FileViewProvider;
import consulo.language.impl.psi.PsiFileBase;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.fileType.FileType;
import org.intellij.grammar.BnfFileType;
import org.intellij.grammar.BnfLanguage;
import org.intellij.grammar.KnownAttribute;
import org.intellij.grammar.generator.ParserGeneratorUtil;
import org.intellij.grammar.psi.*;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.*;
import java.util.regex.Pattern;

/**
 * User: gregory
 * Date: 13.07.11
 * Time: 23:55
 */
public class BnfFileImpl extends PsiFileBase implements BnfFile {
    private final CachedValue<Map<String, BnfRule>> myRules;
    private final CachedValue<List<BnfAttrs>> myGlobalAttributes;
    private final CachedValue<Map<String, List<AttributeInfo>>> myAttributeValues;

    public BnfFileImpl(FileViewProvider fileViewProvider) {
        super(fileViewProvider, BnfLanguage.INSTANCE);
        myRules = CachedValuesManager.getManager(getProject()).createCachedValue(
            () -> CachedValueProvider.Result.create(calcRules(), BnfFileImpl.this),
            false
        );
        myGlobalAttributes = CachedValuesManager.getManager(getProject()).createCachedValue(
            () -> CachedValueProvider.Result.create(calcAttributes(), BnfFileImpl.this),
            false
        );
        //noinspection RequiredXAction
        myAttributeValues = CachedValuesManager.getManager(getProject()).createCachedValue(
            () -> CachedValueProvider.Result.create(calcAttributeValues(), BnfFileImpl.this),
            false
        );
    }

    @Nonnull
    @Override
    public List<BnfRule> getRules() {
        return new ArrayList<>(myRules.getValue().values());
    }

    @Nullable
    @Override
    public BnfRule getRule(@Nullable String ruleName) {
        return ruleName == null ? null : myRules.getValue().get(ruleName);
    }

    @Nonnull
    @Override
    public List<BnfAttrs> getAttributes() {
        return myGlobalAttributes.getValue();
    }

    @Override
    @Nullable
    @RequiredReadAction
    public BnfAttr findAttribute(
        @Nullable String version,
        @Nullable BnfRule rule,
        @Nonnull KnownAttribute<?> knownAttribute,
        @Nullable String match
    ) {
        AttributeInfo result = findAttributeInfo(rule, knownAttribute, match);
        if (result == null) {
            return null;
        }
        return PsiTreeUtil.getParentOfType(findElementAt(result.attrOffset), BnfAttr.class);
    }

    @RequiredReadAction
    public <T> T findAttributeValue(
        @Nullable String version,
        @Nullable BnfRule rule,
        @Nonnull KnownAttribute<T> knownAttribute,
        @Nullable String match
    ) {
        AttributeInfo result = findAttributeInfo(rule, knownAttribute, match);
        return result == null
            ? knownAttribute.getDefaultValue(version)
            : knownAttribute.ensureValue(result.value, version);
    }

    private static final Pattern SUB_EXPRESSION = Pattern.compile(".*(_\\d+)+");

    @Nullable
    @RequiredReadAction
    public <T> AttributeInfo findAttributeInfo(
        @Nullable BnfRule rule,
        @Nonnull KnownAttribute<T> knownAttribute,
        @Nullable String match
    ) {
        List<AttributeInfo> list = myAttributeValues.getValue().get(knownAttribute.getName());
        if (list == null) {
            return null;
        }
        BnfAttrs globalAttrs = rule == null ? ContainerUtil.getFirstItem(getAttributes()) : null;
        int offset = rule == null
            ? globalAttrs == null
            ? 0
            : globalAttrs.getTextRange().getEndOffset()
            : rule.getTextRange().getEndOffset();
        if (offset == 0) {
            return null;
        }
        AttributeInfo key = new AttributeInfo(0, offset, true, null, null);
        int index = Collections.binarySearch(list, key);
        int ruleStartOffset = rule == null ? offset : rule.getTextRange().getStartOffset();
        String toMatch = match == null ? rule == null ? null : rule.getName() : match;
        AttributeInfo result = null;
        for (int i = Math.min(list.size() - 1, index < 0 ? -index - 1 : index); i >= 0; i--) {
            AttributeInfo info = list.get(i);
            if (offset < info.offset || !info.global && ruleStartOffset > info.offset) {
                continue;
            }
            if (info.pattern == null ||
                toMatch != null && info.pattern.matcher(toMatch).matches()) {
                result = info;
                break;
            }
        }
        if (result != null && result.pattern == null && match != null && SUB_EXPRESSION.matcher(match).matches()) {
            // do not pin nested sequences
            result = null;
        }
        return result;
    }

    @Nonnull
    @Override
    public FileType getFileType() {
        return BnfFileType.INSTANCE;
    }

    @Override
    @RequiredReadAction
    public String toString() {
        return "BnfFile:" + getName();
    }

    private Map<String, BnfRule> calcRules() {
        Map<String, BnfRule> result = new LinkedHashMap<>();
        for (BnfRule o : GrammarUtil.bnfTraverser(this).filter(BnfRule.class)) {
            if (!result.containsKey(o.getName())) {
                result.put(o.getName(), o);
            }
        }
        return result;
    }

    private List<BnfAttrs> calcAttributes() {
        return GrammarUtil.bnfTraverser(this).filter(BnfAttrs.class).toList();
    }

    @RequiredReadAction
    private Map<String, List<AttributeInfo>> calcAttributeValues() {
        Map<String, List<AttributeInfo>> result = new HashMap<>();
        for (BnfAttrs attrs : GrammarUtil.bnfTraverser(this).filter(BnfAttrs.class)) {
            boolean isRule = attrs.getParent() instanceof BnfRule;
            TextRange baseRange = attrs.getTextRange();
            List<BnfAttr> attrList = attrs.getAttrList();
            for (int pass = 0; pass < 2; pass++) {
                for (int i = attrList.size() - 1; i >= 0; i--) {
                    BnfAttr attr = attrList.get(i);
                    BnfAttrPattern attrPattern = attr.getAttrPattern();
                    // pass 0: w/o patterns, pass 1: w/ pattern
                    if ((pass == 0) == (attrPattern != null)) {
                        continue;
                    }

                    Pattern pattern = null;
                    if (attrPattern != null) {
                        BnfLiteralExpression expression = attrPattern.getLiteralExpression();
                        pattern = expression == null ? null
                            : ParserGeneratorUtil.compilePattern(StringUtil.stripQuotesAroundValue(expression.getText()));
                    }
                    List<AttributeInfo> list = result.get(attr.getName());
                    if (list == null) {
                        result.put(attr.getName(), list = new ArrayList<>());
                    }
                    Object value = ParserGeneratorUtil.getAttributeValue(attr.getExpression());
                    int offset = attr.getTextRange().getStartOffset();
                    int infoOffset = pattern == null
                        ? baseRange.getStartOffset() + 1
                        : baseRange.getStartOffset() + (baseRange.getEndOffset() - offset);
                    list.add(new AttributeInfo(offset, infoOffset, !isRule, pattern, value));
                }
            }
        }
        return result;
    }

    private static class AttributeInfo implements Comparable<AttributeInfo> {
        final int attrOffset;
        final int offset;
        final boolean global;
        final Pattern pattern;
        final Object value;

        private AttributeInfo(int attrOffset, int offset, boolean global, Pattern pattern, Object value) {
            this.attrOffset = attrOffset;
            this.offset = offset;
            this.global = global;
            this.pattern = pattern;
            this.value = value;
        }

        @Override
        public int compareTo(AttributeInfo o) {
            return offset - o.offset;
        }

        @Override
        public String toString() {
            return (global ? "" : "rule:") + offset + (pattern == null ? "" : " (" + pattern + ")") + " = " + value;
        }
    }
}
