/*
 * Copyright 2011-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package org.intellij.grammar.psi;

import consulo.language.psi.PsiElement;

import javax.annotation.Nonnull;

/**
 * User: gregory
 * Date: 13.07.11
 * Time: 19:02
 */
public interface BnfComposite extends PsiElement {
    <R> R accept(@Nonnull BnfVisitor<R> visitor);
}
