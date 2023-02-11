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
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.psi.PsiRecursiveElementWalkingVisitor;
import consulo.language.editor.inspection.LocalInspectionTool;
import consulo.language.editor.inspection.scheme.InspectionManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import org.intellij.grammar.psi.BnfChoice;
import org.intellij.grammar.psi.BnfExpression;
import org.intellij.grammar.psi.impl.GrammarUtil;
import org.jetbrains.annotations.Nls;

import javax.annotation.Nonnull;
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

  @Override
  public ProblemDescriptor[] checkFile(@Nonnull PsiFile file, @Nonnull InspectionManager manager, boolean isOnTheFly) {
    ProblemsHolder problemsHolder = new ProblemsHolder(manager, file, isOnTheFly);
    checkFile(file, problemsHolder);
    return problemsHolder.getResultsArray();
  }

  private static void checkFile(PsiFile file, final ProblemsHolder problemsHolder) {
    final Set<BnfExpression> set = new HashSet<BnfExpression>();
    file.accept(new PsiRecursiveElementWalkingVisitor() {
      @Override
      public void visitElement(PsiElement element) {
        if (element instanceof BnfChoice) {
          BnfChoice choice = (BnfChoice)element;
          checkChoice(choice, set);
          for (BnfExpression e : set) {
            BnfUnreachableChoiceBranchInspection.registerProblem(choice, e, "Duplicate choice branch", problemsHolder, new BnfRemoveExpressionFix());
          }
          set.clear();
        }
        super.visitElement(element);
      }
    });
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
