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
import consulo.language.psi.PsiElement;
import consulo.application.util.matcher.NameUtil;
import consulo.localize.LocalizeValue;
import consulo.usage.UsageViewNodeTextLocation;
import consulo.usage.UsageViewShortNameLocation;
import consulo.usage.UsageViewTypeLocation;
import consulo.language.psi.ElementDescriptionLocation;
import consulo.language.psi.ElementDescriptionProvider;
import consulo.language.psi.PsiNamedElement;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.util.lang.StringUtil;
import org.intellij.grammar.psi.BnfAttr;
import org.intellij.grammar.psi.BnfComposite;
import org.intellij.grammar.psi.BnfRule;

import jakarta.annotation.Nonnull;

/**
 * @author gregory
 * @since 2011-07-17
 */
@ExtensionImpl
public class BnfDescriptionProvider implements ElementDescriptionProvider {
    @Override
    @RequiredReadAction
    public LocalizeValue getElementDescription(@Nonnull PsiElement psiElement, @Nonnull ElementDescriptionLocation location) {
        if (location == UsageViewNodeTextLocation.INSTANCE && psiElement instanceof BnfComposite) {
            return LocalizeValue.of(
                getElementDescription(psiElement, UsageViewTypeLocation.INSTANCE).get() + " " +
                    "'" + getElementDescription(psiElement, UsageViewShortNameLocation.INSTANCE).get() + "'"
            );
        }
        if (psiElement instanceof BnfRule rule) {
            if (location == UsageViewTypeLocation.INSTANCE) {
                return LocalizeValue.localizeTODO("Grammar Rule");
            }
            return LocalizeValue.of(rule.getName());
        }
        else if (psiElement instanceof BnfAttr attr) {
            if (location == UsageViewTypeLocation.INSTANCE) {
                BnfRule rule = PsiTreeUtil.getParentOfType(psiElement, BnfRule.class);
                return rule == null ? LocalizeValue.localizeTODO("Grammar Attribute") : LocalizeValue.localizeTODO("Rule Attribute");
            }
            return LocalizeValue.of(attr.getName());
        }
        else if (psiElement instanceof BnfComposite) {
            if (location == UsageViewTypeLocation.INSTANCE) {
                return LocalizeValue.of(StringUtil.join(NameUtil.nameToWords(psiElement.getNode().getElementType().toString()), " "));
            }
            return psiElement instanceof PsiNamedElement namedElement
                ? LocalizeValue.ofNullable(namedElement.getName())
                : LocalizeValue.of(psiElement.getClass().getSimpleName());
        }
        return LocalizeValue.empty();
    }
}
