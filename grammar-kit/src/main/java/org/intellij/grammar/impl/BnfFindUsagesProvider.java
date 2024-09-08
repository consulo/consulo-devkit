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

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.findUsage.FindUsagesProvider;
import consulo.language.psi.ElementDescriptionUtil;
import consulo.language.psi.PsiElement;
import consulo.usage.UsageViewLongNameLocation;
import consulo.usage.UsageViewNodeTextLocation;
import consulo.usage.UsageViewTypeLocation;
import org.intellij.grammar.BnfLanguage;
import org.intellij.grammar.psi.BnfAttr;
import org.intellij.grammar.psi.BnfRule;

import javax.annotation.Nonnull;

/**
 * @author gregsh
 */
@ExtensionImpl
public class BnfFindUsagesProvider implements FindUsagesProvider {
    @Override
    public boolean canFindUsagesFor(@Nonnull PsiElement psiElement) {
        return psiElement instanceof BnfRule || psiElement instanceof BnfAttr;
    }

    @Nonnull
    @Override
    public String getType(@Nonnull PsiElement element) {
        return ElementDescriptionUtil.getElementDescription(element, UsageViewTypeLocation.INSTANCE);
    }

    @Nonnull
    @Override
    public String getDescriptiveName(@Nonnull PsiElement element) {
        return ElementDescriptionUtil.getElementDescription(element, UsageViewLongNameLocation.INSTANCE);
    }

    @Nonnull
    @Override
    public String getNodeText(@Nonnull PsiElement element, boolean useFullName) {
        return ElementDescriptionUtil.getElementDescription(element, UsageViewNodeTextLocation.INSTANCE);
    }

    @Nonnull
    @Override
    public Language getLanguage() {
        return BnfLanguage.INSTANCE;
    }
}
