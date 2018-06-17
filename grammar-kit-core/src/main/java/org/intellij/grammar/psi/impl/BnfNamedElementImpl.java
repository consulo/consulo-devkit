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

import static org.intellij.grammar.psi.BnfTypes.BNF_ID;

import javax.annotation.Nonnull;

import org.intellij.grammar.psi.BnfModifier;
import org.intellij.grammar.psi.BnfNamedElement;
import org.intellij.grammar.psi.BnfRule;
import org.jetbrains.annotations.NonNls;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.util.IncorrectOperationException;

/**
 * Created by IntelliJ IDEA.
 * User: gregory
 * Date: 14.07.11
 * Time: 20:04
 */
public abstract class BnfNamedElementImpl extends BnfCompositeElementImpl implements BnfNamedElement {
  
  private volatile String myCachedName;
  
  public BnfNamedElementImpl(ASTNode node) {
    super(node);
  }

  @Override
  public void subtreeChanged() {
    super.subtreeChanged();
    myCachedName = null;
  }

  @Nonnull
  @Override
  public String getName() {
    if (myCachedName == null) {
      myCachedName = GrammarUtil.getIdText(getId());
    }
    return myCachedName;
  }

  @Override
  public PsiElement setName(@NonNls @Nonnull String s) throws IncorrectOperationException {
    getId().replace(BnfElementFactory.createLeafFromText(getProject(), s));
    return this;
  }

  @Override
  public int getTextOffset() {
    return getId().getTextOffset();
  }

  @Nonnull
  @Override
  public SearchScope getUseScope() {
    return new LocalSearchScope(getContainingFile());
  }

  @Override
  public PsiElement getNameIdentifier() {
    return getId();
  }

  public static boolean hasModifier(BnfRule rule, String modifier) {
    for (BnfModifier o : rule.getModifierList()) {
      if (modifier.equals(o.getText())) return true;
    }
    return false;
  }

  @Override
  public String toString() {
    // AE fix in LOG.toString in inconsistent state
    PsiElement nullableId = findChildByType(BNF_ID);
    return super.toString() + ":" + (nullableId == null? null : nullableId.getText());
  }
}
