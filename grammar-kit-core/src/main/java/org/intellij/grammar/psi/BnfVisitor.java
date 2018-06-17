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
package org.intellij.grammar.psi;

import javax.annotation.Nonnull;

import com.intellij.psi.PsiElementVisitor;

public class BnfVisitor<R> extends PsiElementVisitor {

  public R visitAttr(@Nonnull BnfAttr o) {
    return visitNamedElement(o);
  }

  public R visitAttrPattern(@Nonnull BnfAttrPattern o) {
    return visitCompositeElement(o);
  }

  public R visitAttrs(@Nonnull BnfAttrs o) {
    return visitCompositeElement(o);
  }

  public R visitChoice(@Nonnull BnfChoice o) {
    return visitExpression(o);
  }

  public R visitExpression(@Nonnull BnfExpression o) {
    return visitCompositeElement(o);
  }

  public R visitExternalExpression(@Nonnull BnfExternalExpression o) {
    return visitExpression(o);
  }

  public R visitListEntry(@Nonnull BnfListEntry o) {
    return visitCompositeElement(o);
  }

  public R visitLiteralExpression(@Nonnull BnfLiteralExpression o) {
    return visitExpression(o);
  }

  public R visitModifier(@Nonnull BnfModifier o) {
    return visitCompositeElement(o);
  }

  public R visitParenExpression(@Nonnull BnfParenExpression o) {
    return visitParenthesized(o);
  }

  public R visitParenOptExpression(@Nonnull BnfParenOptExpression o) {
    return visitParenthesized(o);
  }

  public R visitParenthesized(@Nonnull BnfParenthesized o) {
    return visitExpression(o);
  }

  public R visitPredicate(@Nonnull BnfPredicate o) {
    return visitExpression(o);
  }

  public R visitPredicateSign(@Nonnull BnfPredicateSign o) {
    return visitCompositeElement(o);
  }

  public R visitQuantified(@Nonnull BnfQuantified o) {
    return visitExpression(o);
  }

  public R visitQuantifier(@Nonnull BnfQuantifier o) {
    return visitCompositeElement(o);
  }

  public R visitReferenceOrToken(@Nonnull BnfReferenceOrToken o) {
    return visitExpression(o);
  }

  public R visitRule(@Nonnull BnfRule o) {
    return visitNamedElement(o);
  }

  public R visitSequence(@Nonnull BnfSequence o) {
    return visitExpression(o);
  }

  public R visitStringLiteralExpression(@Nonnull BnfStringLiteralExpression o) {
    return visitLiteralExpression(o);
  }

  public R visitValueList(@Nonnull BnfValueList o) {
    return visitExpression(o);
  }

  public R visitNamedElement(@Nonnull BnfNamedElement o) {
    return visitCompositeElement(o);
  }

  public R visitCompositeElement(@Nonnull BnfCompositeElement o) {
    visitElement(o);
    return null;
  }

}
