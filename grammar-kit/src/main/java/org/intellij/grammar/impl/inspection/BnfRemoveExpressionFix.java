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
import consulo.localize.LocalizeValue;
import jakarta.annotation.Nonnull;

import consulo.language.ast.ASTNode;
import consulo.language.impl.ast.TreeUtil;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.psi.PsiElement;
import consulo.project.Project;
import org.intellij.grammar.psi.BnfChoice;
import org.intellij.grammar.psi.BnfExpression;
import org.intellij.grammar.psi.BnfTypes;
import org.intellij.grammar.impl.refactor.BnfExpressionOptimizer;

/**
 * @author gregsh
 */
public class BnfRemoveExpressionFix implements LocalQuickFix {
    @Nonnull
    @Override
    public LocalizeValue getName() {
        return LocalizeValue.localizeTODO("Remove expression");
    }

    @Override
    @RequiredReadAction
    public void applyFix(@Nonnull Project project, @Nonnull ProblemDescriptor descriptor) {
        PsiElement element = descriptor.getPsiElement();
        if (!element.isValid()) {
            return;
        }
        PsiElement parent = element.getParent();
        if (element instanceof BnfExpression && parent instanceof BnfChoice choice) {
            ASTNode node = element.getNode();
            ASTNode nextOr = TreeUtil.findSibling(node, BnfTypes.BNF_OP_OR);
            ASTNode prevOr = TreeUtil.findSiblingBackward(node, BnfTypes.BNF_OP_OR);
            assert nextOr != null || prevOr != null : "'|' missing in choice";
            if (nextOr != null && prevOr != null) {
                choice.deleteChildRange(prevOr.getTreeNext().getPsi(), nextOr.getPsi());
            }
            else {
                choice.deleteChildRange(prevOr == null ? element : prevOr.getPsi(), prevOr == null ? nextOr.getPsi() : element);
            }
        }
        else {
            element.delete();
        }
        BnfExpressionOptimizer.optimize(parent);
    }
}
