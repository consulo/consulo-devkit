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

package org.intellij.grammar.psi;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.util.function.Processor;
import consulo.content.scope.SearchScope;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiReference;
import consulo.language.psi.scope.LocalSearchScope;
import consulo.language.psi.search.ReferencesSearch;
import consulo.language.psi.search.ReferencesSearchQueryExecutor;
import consulo.project.util.query.QueryExecutorBase;
import org.intellij.grammar.psi.impl.BnfStringImpl;

import jakarta.annotation.Nonnull;

/**
 * @author gregsh
 */
@ExtensionImpl
public class BnfAttrPatternRefSearcher extends QueryExecutorBase<PsiReference, ReferencesSearch.SearchParameters>
    implements ReferencesSearchQueryExecutor {
    public BnfAttrPatternRefSearcher() {
        super(true);
    }

    @Override
    public void processQuery(
        @Nonnull ReferencesSearch.SearchParameters queryParameters,
        @Nonnull final Processor<? super PsiReference> consumer
    ) {
        final PsiElement target = queryParameters.getElementToSearch();
        if (!(target instanceof BnfRule)) {
            return;
        }

        SearchScope scope = queryParameters.getEffectiveSearchScope();
        if (!(scope instanceof LocalSearchScope)) {
            return;
        }

        PsiFile file = target.getContainingFile();
        if (!(file instanceof BnfFile)) {
            return;
        }

        for (BnfAttrs attrs : ((BnfFile)file).getAttributes()) {
            for (BnfAttr attr : attrs.getAttrList()) {
                BnfAttrPattern pattern = attr.getAttrPattern();
                if (pattern == null) {
                    continue;
                }
                BnfStringLiteralExpression patternExpression = pattern.getLiteralExpression();

                PsiReference ref = BnfStringImpl.matchesElement(patternExpression, target) ? patternExpression.getReference() : null;
                if (ref != null && ref.isReferenceTo(target) && !consumer.process(ref)) {
                    return;
                }
            }
        }
    }
}
