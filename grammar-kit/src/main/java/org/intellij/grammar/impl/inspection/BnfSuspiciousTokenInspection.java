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
import consulo.language.editor.inspection.LocalInspectionTool;
import consulo.language.editor.inspection.LocalInspectionToolSession;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.psi.PsiReference;
import consulo.util.collection.JBIterable;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import org.intellij.grammar.generator.ParserGeneratorUtil;
import org.intellij.grammar.psi.BnfExternalExpression;
import org.intellij.grammar.psi.BnfRule;
import org.intellij.grammar.psi.BnfVisitor;
import org.intellij.grammar.psi.impl.BnfRefOrTokenImpl;
import org.intellij.grammar.psi.impl.GrammarUtil;
import org.jetbrains.annotations.Nls;

/**
 * Created by IntelliJ IDEA.
 * Date: 8/25/11
 * Time: 7:06 PM
 *
 * @author Vadim Romansky
 */
@ExtensionImpl
public class BnfSuspiciousTokenInspection extends LocalInspectionTool {
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
        return "Suspicious token";
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
                        holder.registerProblem(token,
                            "'" + text + "' token looks like a reference to a missing rule",
                            new CreateRuleFromTokenFix(text));
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
