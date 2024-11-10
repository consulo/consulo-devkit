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
import consulo.devkit.grammarKit.localize.BnfLocalize;
import consulo.language.editor.inspection.LocalInspectionTool;
import consulo.language.editor.inspection.LocalInspectionToolSession;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.psi.PsiElementVisitor;
import consulo.localize.LocalizeValue;
import org.intellij.grammar.KnownAttribute;
import org.intellij.grammar.psi.BnfAttr;
import org.intellij.grammar.psi.BnfVisitor;

import jakarta.annotation.Nonnull;

import static org.intellij.grammar.KnownAttribute.getAttribute;
import static org.intellij.grammar.KnownAttribute.getCompatibleAttribute;

/**
 * @author gregsh
 */
@ExtensionImpl
public class BnfUnusedAttributeInspection extends LocalInspectionTool {
    @Nonnull
    @Override
    public String getGroupDisplayName() {
        return BnfLocalize.inspectionsGroupName().get();
    }

    @Nonnull
    @Override
    public String getDisplayName() {
        return BnfLocalize.unusedAttributeInspectionDisplayName().get();
    }

    @Nonnull
    @Override
    public HighlightDisplayLevel getDefaultLevel() {
        return HighlightDisplayLevel.WARNING;
    }

    @Nonnull
    @Override
    public PsiElementVisitor buildVisitor(
        @Nonnull ProblemsHolder holder,
        boolean isOnTheFly,
        @Nonnull LocalInspectionToolSession session,
        @Nonnull Object state
    ) {
        return new BnfVisitor<Void>() {
            @Override
            public Void visitAttr(@Nonnull BnfAttr o) {
                final String name = o.getName();
                if (!name.toUpperCase().equals(name) && getAttribute(name) == null) {
                    KnownAttribute newAttr = getCompatibleAttribute(name);
                    LocalizeValue text = newAttr == null
                        ? BnfLocalize.unusedAttributeInspectionMessageUnused()
                        : BnfLocalize.unusedAttributeInspectionMessageDeprecated(newAttr.getName());
                    holder.newProblem(text)
                        .range(o.getId())
                        .create();
                }
                return null;
            }
        };
    }
}