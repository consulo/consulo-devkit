/*
 * Copyright 2011-present Gregory Shrago
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

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;

import static org.intellij.grammar.psi.BnfTypes.*;

import javax.annotation.Nonnull;

import org.intellij.grammar.psi.*;

public class BnfReferenceOrTokenImpl extends BnfRefOrTokenImpl implements BnfReferenceOrToken {

  public BnfReferenceOrTokenImpl(ASTNode node) {
    super(node);
  }

  public <R> R accept(@Nonnull BnfVisitor<R> visitor) {
    return visitor.visitReferenceOrToken(this);
  }

  public void accept(@Nonnull PsiElementVisitor visitor) {
    if (visitor instanceof BnfVisitor) accept((BnfVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @Nonnull
  public PsiElement getId() {
    return findNotNullChildByType(BNF_ID);
  }

}
