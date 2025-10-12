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

package org.intellij.grammar.impl.inspection;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.devkit.grammarKit.localize.BnfLocalize;
import consulo.language.editor.inspection.LocalInspectionTool;
import consulo.language.editor.inspection.LocalInspectionToolSession;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import jakarta.annotation.Nonnull;
import org.intellij.grammar.psi.BnfFile;
import org.intellij.grammar.psi.BnfRule;
import org.intellij.grammar.psi.impl.GrammarUtil;
import org.jetbrains.annotations.Nls;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author Vadim Romansky
 * @since 2011-08-29
 */
@ExtensionImpl
public class BnfDuplicateRuleInspection extends LocalInspectionTool {
    @Nonnull
    @Override
    public LocalizeValue getGroupDisplayName() {
        return BnfLocalize.inspectionsGroupName();
    }

    @Nls
    @Nonnull
    @Override
    public LocalizeValue getDisplayName() {
        return BnfLocalize.duplicateRuleInspectionDisplayName();
    }

    @Nonnull
    @Override
    public String getShortName() {
        return "BnfDuplicateRuleInspection";
    }

    @Nonnull
    @Override
    public HighlightDisplayLevel getDefaultLevel() {
        return HighlightDisplayLevel.WARNING;
    }

    @Override
    public boolean isEnabledByDefault() {
        return true;
    }

    @Nonnull
    @Override
    public PsiElementVisitor buildVisitor(@Nonnull ProblemsHolder holder, boolean isOnTheFly, @Nonnull LocalInspectionToolSession session, @Nonnull Object state) {
        return new PsiElementVisitor() {
            @Override
            @RequiredReadAction
            public void visitFile(PsiFile file) {
                checkFile(file, holder);
            }
        };
    }

    @RequiredReadAction
    private static void checkFile(PsiFile file, ProblemsHolder problemsHolder) {
        if (!(file instanceof BnfFile bnfFile)) {
            return;
        }

        Set<BnfRule> rules = new LinkedHashSet<>();
        for (BnfRule r : GrammarUtil.bnfTraverser(bnfFile).filter(BnfRule.class)) {
            BnfRule t = bnfFile.getRule(r.getName());
            if (r != t) {
                rules.add(t);
                rules.add(r);
            }
        }
        for (BnfRule rule : rules) {
            problemsHolder.newProblem(BnfLocalize.duplicateRuleInspectionMessage(rule.getName()))
                .range(rule.getId())
                .create();
        }
    }
}
