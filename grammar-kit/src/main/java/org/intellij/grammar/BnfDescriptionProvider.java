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
package org.intellij.grammar;

import org.intellij.grammar.psi.BnfAttr;
import org.intellij.grammar.psi.BnfCompositeElement;
import org.intellij.grammar.psi.BnfRule;
import org.jetbrains.annotations.NotNull;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.ElementDescriptionLocation;
import com.intellij.psi.ElementDescriptionProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.codeStyle.NameUtil;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.usageView.UsageViewNodeTextLocation;
import com.intellij.usageView.UsageViewShortNameLocation;
import com.intellij.usageView.UsageViewTypeLocation;

/**
 * @author gregory
 *         Date: 17.07.11 18:46
 */
public class BnfDescriptionProvider implements ElementDescriptionProvider {
  @Override
  public String getElementDescription(@NotNull PsiElement psiElement, @NotNull ElementDescriptionLocation location) {
    if (location == UsageViewNodeTextLocation.INSTANCE && psiElement instanceof BnfCompositeElement) {
      return getElementDescription(psiElement, UsageViewTypeLocation.INSTANCE) + " " +
             "'" + getElementDescription(psiElement, UsageViewShortNameLocation.INSTANCE) + "'";
    }
    if (psiElement instanceof BnfRule) {
      if (location == UsageViewTypeLocation.INSTANCE) {
        return "Grammar Rule";
      }
      return ((BnfRule)psiElement).getName();
    }
    else if (psiElement instanceof BnfAttr) {
      if (location == UsageViewTypeLocation.INSTANCE) {
        BnfRule rule = PsiTreeUtil.getParentOfType(psiElement, BnfRule.class);
        return (rule == null ? "Grammar " : "Rule ") + "Attribute";
      }
      return ((BnfAttr)psiElement).getName();
    }
    else if (psiElement instanceof BnfCompositeElement) {
      if (location == UsageViewTypeLocation.INSTANCE) {
        return StringUtil.join(NameUtil.nameToWords(psiElement.getNode().getElementType().toString()), " ");
      }
      return psiElement instanceof PsiNamedElement? ((PsiNamedElement) psiElement).getName() : psiElement.getClass().getSimpleName();
    }
    return null;
  }
}
