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

import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.inspection.LocalInspectionTool;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.inspection.scheme.InspectionManager;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.psi.PsiFile;
import org.intellij.grammar.psi.BnfFile;
import org.intellij.grammar.psi.BnfRule;
import org.intellij.grammar.psi.impl.GrammarUtil;
import org.jetbrains.annotations.Nls;

import javax.annotation.Nonnull;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * Date: 8/29/11
 * Time: 1:54 PM
 *
 * @author Vadim Romansky
 */
@ExtensionImpl
public class BnfDuplicateRuleInspection extends LocalInspectionTool {
    @Nls
    @Nonnull
    @Override
    public String getGroupDisplayName() {
        return "Grammar/BNF";
    }

    @Nls
    @Nonnull
    @Override
    public String getDisplayName() {
        return "Duplicate rule";
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

    public boolean isEnabledByDefault() {
        return true;
    }

    public ProblemDescriptor[] checkFile(@Nonnull PsiFile file, @Nonnull InspectionManager manager, boolean isOnTheFly) {
        ProblemsHolder problemsHolder = new ProblemsHolder(manager, file, isOnTheFly);
        checkFile(file, problemsHolder);
        return problemsHolder.getResultsArray();
    }

    private static void checkFile(PsiFile file, ProblemsHolder problemsHolder) {
        if (!(file instanceof BnfFile)) {
            return;
        }
        BnfFile bnfFile = (BnfFile)file;

        Set<BnfRule> rules = new LinkedHashSet<>();
        for (BnfRule r : GrammarUtil.bnfTraverser(bnfFile).filter(BnfRule.class)) {
            BnfRule t = bnfFile.getRule(r.getName());
            if (r != t) {
                rules.add(t);
                rules.add(r);
            }
        }
        for (BnfRule rule : rules) {
            problemsHolder.registerProblem(rule.getId(), "'" + rule.getName() + "' rule is defined more than once");
        }
    }
}
