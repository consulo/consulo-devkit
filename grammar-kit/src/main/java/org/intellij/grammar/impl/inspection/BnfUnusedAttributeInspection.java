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
import org.intellij.grammar.KnownAttribute;
import org.intellij.grammar.psi.BnfAttr;
import org.intellij.grammar.psi.BnfVisitor;
import org.jetbrains.annotations.Nls;

import javax.annotation.Nonnull;

import static org.intellij.grammar.KnownAttribute.getAttribute;
import static org.intellij.grammar.KnownAttribute.getCompatibleAttribute;

/**
 * @author gregsh
 */
@ExtensionImpl
public class BnfUnusedAttributeInspection extends LocalInspectionTool {
  @Nonnull
  @Override
  public String getDisplayName() {
    return "Unused attribute";
  }

  @Nonnull
  @Override
  public HighlightDisplayLevel getDefaultLevel() {
    return HighlightDisplayLevel.WARNING;
  }

  @Nls
  @Nonnull
  @Override
  public String getGroupDisplayName() {
    return "Grammar/BNF";
  }

  @Nonnull
  @Override
  public PsiElementVisitor buildVisitor(@Nonnull ProblemsHolder holder, boolean isOnTheFly, @Nonnull LocalInspectionToolSession session) {
    return new BnfVisitor<Void>() {
      @Override
      public Void visitAttr(@Nonnull BnfAttr o) {
        final String name = o.getName();
        if (!name.toUpperCase().equals(name) && getAttribute(name) == null) {
          KnownAttribute newAttr = getCompatibleAttribute(name);
          String text = newAttr == null ? "Unused attribute" : "Deprecated attribute, use '" + newAttr.getName() + "' instead";
          holder.registerProblem(o.getId(), text);
        }
        return null;
      }
    };
  }
}