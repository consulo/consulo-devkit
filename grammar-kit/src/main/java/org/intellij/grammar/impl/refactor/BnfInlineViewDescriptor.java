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
package org.intellij.grammar.impl.refactor;

import consulo.language.editor.refactoring.localize.RefactoringLocalize;
import consulo.language.psi.PsiElement;
import consulo.usage.UsageViewBundle;
import consulo.usage.UsageViewDescriptor;
import jakarta.annotation.Nonnull;
import org.intellij.grammar.psi.BnfRule;

/**
 * @author Vadim Romansky
 * @since 2011-08-11
 */
public class BnfInlineViewDescriptor implements UsageViewDescriptor {
    private final BnfRule myElement;

    public BnfInlineViewDescriptor(BnfRule myElement) {
        this.myElement = myElement;
    }

    @Nonnull
    @Override
    public PsiElement[] getElements() {
        return new PsiElement[]{myElement};
    }

    @Override
    public String getProcessedElementsHeader() {
        return "Rule";
    }

    @Override
    public String getCodeReferencesText(int usagesCount, int filesCount) {
        return RefactoringLocalize.invocationsToBeInlined(UsageViewBundle.getReferencesString(usagesCount, filesCount)).get();
    }

    @Override
    public String getCommentReferencesText(int usagesCount, int filesCount) {
        return RefactoringLocalize.commentsElementsHeader(UsageViewBundle.getOccurencesString(usagesCount, filesCount)).get();
    }
}
