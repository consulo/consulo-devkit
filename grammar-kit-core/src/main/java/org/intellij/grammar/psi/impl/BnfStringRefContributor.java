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

import consulo.language.Language;
import consulo.language.pattern.PatternCondition;
import consulo.language.psi.*;
import consulo.language.util.ProcessingContext;
import jakarta.annotation.Nonnull;
import org.intellij.grammar.BnfLanguage;
import org.intellij.grammar.KnownAttribute;
import org.intellij.grammar.java.JavaHelper;
import org.intellij.grammar.psi.BnfAttr;
import org.intellij.grammar.psi.BnfAttrPattern;

import java.util.Set;

import static consulo.language.pattern.PlatformPatterns.or;
import static consulo.language.pattern.PlatformPatterns.string;
import static consulo.language.pattern.StandardPatterns.psiElement;
import static org.intellij.grammar.KnownAttribute.*;

/**
 * @author gregsh
 */
public class BnfStringRefContributor extends PsiReferenceContributor {
    private static final Set<KnownAttribute> RULE_ATTRIBUTES = Set.of(EXTENDS, IMPLEMENTS, RECOVER_WHILE, NAME);

    private static final Set<KnownAttribute> JAVA_CLASS_ATTRIBUTES = Set.of(EXTENDS, IMPLEMENTS, MIXIN);

    @Override
    public void registerReferenceProviders(@Nonnull PsiReferenceRegistrar registrar) {
        registrar.registerReferenceProvider(
            psiElement(BnfStringImpl.class).withParent(psiElement(BnfAttrPattern.class)), new PsiReferenceProvider() {

                @Nonnull
                @Override
                public PsiReference[] getReferencesByElement(@Nonnull PsiElement element, @Nonnull ProcessingContext context) {
                    return new PsiReference[]{BnfStringImpl.createPatternReference((BnfStringImpl)element)};
                }
            }
        );

        registrar.registerReferenceProvider(
            psiElement(BnfStringImpl.class).withParent(psiElement(BnfAttr.class).withName(string().with(oneOf(RULE_ATTRIBUTES)))),
            new PsiReferenceProvider() {

                @Nonnull
                @Override
                public PsiReference[] getReferencesByElement(@Nonnull PsiElement element, @Nonnull ProcessingContext context) {
                    return new PsiReference[]{BnfStringImpl.createRuleReference((BnfStringImpl)element)};
                }
            }
        );

        registrar.registerReferenceProvider(
            psiElement(BnfStringImpl.class).withParent(psiElement(BnfAttr.class).withName(
                or(string().endsWith("Class"), string().endsWith("Package"), string().with(oneOf(JAVA_CLASS_ATTRIBUTES))))),
            new PsiReferenceProvider() {

                @Nonnull
                @Override
                public PsiReference[] getReferencesByElement(@Nonnull PsiElement element, @Nonnull ProcessingContext context) {
                    PsiReferenceProvider provider = JavaHelper.getJavaHelper(element).getClassReferenceProvider();
                    return provider == null ? PsiReference.EMPTY_ARRAY : provider.getReferencesByElement(element, new ProcessingContext());
                }
            }
        );
    }

    private static PatternCondition<String> oneOf(final Set<KnownAttribute> attributes) {
        return new PatternCondition<>("oneOf") {
            @Override
            public boolean accepts(@Nonnull String s, ProcessingContext context) {
                return attributes.contains(KnownAttribute.getCompatibleAttribute(s));
            }
        };
    }

    @Nonnull
    @Override
    public Language getLanguage() {
        return BnfLanguage.INSTANCE;
    }
}
