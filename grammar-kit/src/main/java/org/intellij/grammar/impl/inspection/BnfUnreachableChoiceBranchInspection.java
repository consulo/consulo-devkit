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
import consulo.document.util.TextRange;
import consulo.language.ast.ASTNode;
import consulo.language.editor.inspection.LocalInspectionTool;
import consulo.language.editor.inspection.LocalInspectionToolSession;
import consulo.language.editor.inspection.ProblemBuilder;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.impl.ast.TreeUtil;
import consulo.language.psi.PsiElementVisitor;
import consulo.localize.LocalizeValue;
import jakarta.annotation.Nonnull;
import org.intellij.grammar.analysis.BnfFirstNextAnalyzer;
import org.intellij.grammar.psi.BnfChoice;
import org.intellij.grammar.psi.BnfExpression;
import org.intellij.grammar.psi.BnfTypes;
import org.intellij.grammar.psi.BnfVisitor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author gregsh
 */
@ExtensionImpl
public class BnfUnreachableChoiceBranchInspection extends LocalInspectionTool {
    @Nonnull
    @Override
    public LocalizeValue getGroupDisplayName() {
        return BnfLocalize.inspectionsGroupName();
    }

    @Nonnull
    @Override
    public LocalizeValue getDisplayName() {
        return BnfLocalize.unreachableChoiceBranchInspectionDisplayName();
    }

    @Nonnull
    @Override
    public String getShortName() {
        return "BnfUnreachableChoiceBranchInspection";
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
    public PsiElementVisitor buildVisitor(
        @Nonnull ProblemsHolder problemsHolder,
        boolean isOnTheFly,
        @Nonnull LocalInspectionToolSession session,
        @Nonnull Object state
    ) {
        return new BnfVisitor<Void>() {
            @Override
            @RequiredReadAction
            public Void visitChoice(@Nonnull BnfChoice choice) {
                checkChoice(choice, problemsHolder);
                return null;
            }
        };
    }

    @RequiredReadAction
    private static void checkChoice(BnfChoice choice, ProblemsHolder problemsHolder) {
        Set<BnfExpression> visited = new HashSet<>();
        Set<BnfExpression> first = new HashSet<>();
        BnfFirstNextAnalyzer analyzer = new BnfFirstNextAnalyzer().setPredicateLookAhead(true);
        List<BnfExpression> list = choice.getExpressionList();
        for (int i = 0, listSize = list.size() - 1; i < listSize; i++) {
            BnfExpression child = list.get(i);
            Set<BnfExpression> firstSet = analyzer.calcFirstInner(child, first, visited);
            if (firstSet.contains(BnfFirstNextAnalyzer.BNF_MATCHES_NOTHING)) {
                ProblemBuilder problemBuilder =
                    problemsHolder.newProblem(BnfLocalize.unreachableChoiceBranchInspectionMessageMatchesNothing());
                setRange(problemBuilder, choice, child)
                    .create();
            }
            else if (firstSet.contains(BnfFirstNextAnalyzer.BNF_MATCHES_EOF)) {
                ProblemBuilder problemBuilder =
                    problemsHolder.newProblem(BnfLocalize.unreachableChoiceBranchInspectionMessageMatchesEof());
                setRange(problemBuilder, choice, child)
                    .create();
                break;
            }
            first.clear();
            visited.clear();
        }
    }

    @RequiredReadAction
    static ProblemBuilder setRange(ProblemBuilder problemBuilder, BnfExpression choice, BnfExpression branch) {
        TextRange textRange = branch.getTextRange();
        if (textRange.isEmpty()) {
            ASTNode nextOr = TreeUtil.findSibling(branch.getNode(), BnfTypes.BNF_OP_OR);
            ASTNode prevOr = TreeUtil.findSiblingBackward(branch.getNode(), BnfTypes.BNF_OP_OR);

            int shift = choice.getTextRange().getStartOffset();
            int startOffset = prevOr != null ? prevOr.getStartOffset() - shift : 0;
            TextRange range = new TextRange(
                startOffset,
                nextOr != null ? nextOr.getStartOffset() + 1 - shift : Math.min(startOffset + 2, choice.getTextLength())
            );
            return problemBuilder.range(choice, range);
        }
        else {
            return problemBuilder.range(branch);
        }
    }
}
