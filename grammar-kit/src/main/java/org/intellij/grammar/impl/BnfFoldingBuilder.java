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

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.dumb.DumbAware;
import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.language.Language;
import consulo.language.ast.ASTNode;
import consulo.language.editor.folding.FoldingBuilderEx;
import consulo.language.editor.folding.FoldingDescriptor;
import consulo.language.psi.PsiElement;
import consulo.language.psi.util.PsiTreeUtil;
import org.intellij.grammar.BnfLanguage;
import org.intellij.grammar.BnfParserDefinition;
import org.intellij.grammar.psi.*;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;

/**
 * @author gregsh
 */
@ExtensionImpl
public class BnfFoldingBuilder extends FoldingBuilderEx implements DumbAware {
    @Nonnull
    @Override
    @RequiredReadAction
    public FoldingDescriptor[] buildFoldRegions(@Nonnull PsiElement root, @Nonnull Document document, boolean quick) {
        if (!(root instanceof BnfFile file)) {
            return FoldingDescriptor.EMPTY;
        }

        final ArrayList<FoldingDescriptor> result = new ArrayList<>();
        for (BnfAttrs attrs : file.getAttributes()) {
            TextRange textRange = attrs.getTextRange();
            if (textRange.getLength() <= 2) {
                continue;
            }
            result.add(new FoldingDescriptor(attrs, textRange));
            for (BnfAttr attr : attrs.getAttrList()) {
                BnfExpression attrValue = attr.getExpression();
                if (attrValue instanceof BnfValueList && attrValue.getTextLength() > 2) {
                    result.add(new FoldingDescriptor(attrValue, attrValue.getTextRange()));
                }
            }
        }
        for (BnfRule rule : file.getRules()) {
            //result.add(new FoldingDescriptor(rule, rule.getTextRange()));
            BnfAttrs attrs = rule.getAttrs();
            if (attrs != null) {
                result.add(new FoldingDescriptor(attrs, attrs.getTextRange()));
            }
        }
        if (!quick) {
            PsiTreeUtil.processElements(
                file,
                element -> {
                    if (element.getNode().getElementType() == BnfParserDefinition.BNF_BLOCK_COMMENT) {
                        result.add(new FoldingDescriptor(element, element.getTextRange()));
                    }
                    return true;
                }
            );
        }

        return result.toArray(new FoldingDescriptor[result.size()]);
    }

    @Nullable
    @Override
    @RequiredReadAction
    public String getPlaceholderText(@Nonnull ASTNode node) {
        PsiElement psi = node.getPsi();
        if (psi instanceof BnfAttrs) {
            return "{..}";
        }
        if (psi instanceof BnfRule rule) {
            return rule.getName() + " ::= ...";
        }
        if (psi instanceof BnfValueList) {
            return "[..]";
        }
        if (node.getElementType() == BnfParserDefinition.BNF_BLOCK_COMMENT) {
            return "/*..*/";
        }
        return null;
    }

    @Override
    @RequiredReadAction
    public boolean isCollapsedByDefault(@Nonnull ASTNode node) {
        PsiElement psi = node.getPsi();
        return psi instanceof BnfValueList
            || psi instanceof BnfAttrs && !(psi.getParent() instanceof BnfRule);
    }

    @Nonnull
    @Override
    public Language getLanguage() {
        return BnfLanguage.INSTANCE;
    }
}
