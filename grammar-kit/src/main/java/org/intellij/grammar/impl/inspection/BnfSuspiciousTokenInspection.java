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
import consulo.language.psi.PsiReference;
import consulo.localize.LocalizeValue;
import consulo.util.collection.JBIterable;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import org.intellij.grammar.generator.ParserGeneratorUtil;
import org.intellij.grammar.psi.BnfExternalExpression;
import org.intellij.grammar.psi.BnfRule;
import org.intellij.grammar.psi.BnfVisitor;
import org.intellij.grammar.psi.impl.BnfRefOrTokenImpl;
import org.intellij.grammar.psi.impl.GrammarUtil;

/**
 * @author Vadim Romansky
 * @since 2011-08-25
 */
@ExtensionImpl
public class BnfSuspiciousTokenInspection extends LocalInspectionTool {
    @Nonnull
    @Override
    public LocalizeValue getGroupDisplayName() {
        return BnfLocalize.inspectionsGroupName();
    }

    @Nonnull
    @Override
    public LocalizeValue getDisplayName() {
        return BnfLocalize.suspiciousTokenInspectionDisplayName();
    }

    @Nonnull
    @Override
    public String getShortName() {
        return "BnfSuspiciousTokenInspection";
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
            @Override
            @RequiredReadAction
            public Void visitRule(@Nonnull BnfRule o) {
                if (ParserGeneratorUtil.Rule.isExternal(o)) return null;
                JBIterable<BnfRefOrTokenImpl> tokens = GrammarUtil.bnfTraverser(o.getExpression())
                    .expand(s -> !(s instanceof BnfExternalExpression))
                    .filter(BnfRefOrTokenImpl.class);
                for (BnfRefOrTokenImpl token : tokens) {
                    PsiReference reference = token.getReference();
                    Object resolve = reference == null ? null : reference.resolve();
                    String text = token.getText();
                    if (resolve == null && !tokens.contains(text) && isTokenTextSuspicious(text)) {
                        holder.newProblem(BnfLocalize.suspiciousTokenInspectionMessage(text))
                            .range(token)
                            .withFix(new CreateRuleFromTokenFix(text))
                            .create();
                    }
                }
                return null;
            }
        };
    }

    public static boolean isTokenTextSuspicious(String text) {
        boolean isLowercase = text.equals(text.toLowerCase());
        boolean isUppercase = !isLowercase && text.equals(text.toUpperCase());
        return !isLowercase && !isUppercase || isLowercase && StringUtil.containsAnyChar(text, "-_");
    }
}
