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
import consulo.language.editor.inspection.LocalInspectionToolSession;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.psi.PsiElementVisitor;
import jakarta.annotation.Nonnull;
import org.intellij.grammar.psi.BnfChoice;
import org.intellij.grammar.psi.BnfExpression;
import org.intellij.grammar.psi.BnfVisitor;
import org.intellij.grammar.psi.impl.GrammarUtil;
import org.jetbrains.annotations.Nls;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * Date: 9/2/11
 * Time: 7:20 PM
 *
 * @author Vadim Romansky
 */
@ExtensionImpl
public class BnfIdenticalChoiceBranchesInspection extends LocalInspectionTool {
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
        return "Identical choice branches";
    }

    @Nonnull
    @Override
    public String getShortName() {
        return "BnfIdenticalChoiceBranchesInspection";
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
        return new BnfVisitor<Void>() {
            final HashSet<BnfExpression> set = new HashSet<>();

            @Override
            public Void visitChoice(@Nonnull BnfChoice o) {
                checkChoice(o, set);
                for (BnfExpression e : set) {
                    BnfUnreachableChoiceBranchInspection.registerProblem(
                        o, e, "Duplicate choice branch", holder, new BnfRemoveExpressionFix());
                }
                set.clear();
                return null;
            }
        };
    }

    private static void checkChoice(BnfChoice choice, Set<BnfExpression> set) {
        List<BnfExpression> list = choice.getExpressionList();
        for (BnfExpression e1 : list) {
            for (BnfExpression e2 : list) {
                if (e1 != e2 && GrammarUtil.equalsElement(e1, e2)) {
                    set.add(e1);
                    set.add(e2);
                }
            }
        }
    }
}
