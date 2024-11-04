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
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.editor.inspection.scheme.InspectionManager;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.psi.PsiFile;
import org.intellij.grammar.analysis.BnfFirstNextAnalyzer;
import org.intellij.grammar.generator.ExpressionGeneratorHelper;
import org.intellij.grammar.generator.ExpressionHelper;
import org.intellij.grammar.generator.ParserGeneratorUtil;
import org.intellij.grammar.psi.BnfFile;
import org.intellij.grammar.psi.BnfRule;
import org.jetbrains.annotations.Nls;

import jakarta.annotation.Nonnull;

import java.util.ArrayList;

/**
 * @author gregsh
 */
@ExtensionImpl
public class BnfLeftRecursionInspection extends LocalInspectionTool {
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
        return "Left recursion";
    }

    @Nonnull
    @Override
    public String getShortName() {
        return "BnfLeftRecursionInspection";
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
        if (file instanceof BnfFile bnfFile) {
            ExpressionHelper expressionHelper = ExpressionHelper.getCached(bnfFile);
            BnfFirstNextAnalyzer analyzer = new BnfFirstNextAnalyzer();
            ArrayList<ProblemDescriptor> list = new ArrayList<>();
            for (BnfRule rule : bnfFile.getRules()) {
                if (ParserGeneratorUtil.Rule.isFake(rule)) {
                    continue;
                }
                String ruleName = rule.getName();
                boolean exprParsing = ExpressionGeneratorHelper.getInfoForExpressionParsing(expressionHelper, rule) != null;

                if (!exprParsing && analyzer.asStrings(analyzer.calcFirst(rule)).contains(ruleName)) {
                    list.add(manager.createProblemDescriptor(
                        rule.getId(),
                        "'" + ruleName + "' employs left-recursion unsupported by generator",
                        isOnTheFly,
                        LocalQuickFix.EMPTY_ARRAY,
                        ProblemHighlightType.GENERIC_ERROR_OR_WARNING
                    ));
                }
            }
            if (!list.isEmpty()) {
                return list.toArray(new ProblemDescriptor[list.size()]);
            }
        }

        return ProblemDescriptor.EMPTY_ARRAY;
    }
}
