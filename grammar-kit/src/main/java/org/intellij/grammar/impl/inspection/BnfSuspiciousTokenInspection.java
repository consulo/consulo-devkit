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
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiRecursiveElementWalkingVisitor;
import consulo.language.editor.inspection.LocalInspectionTool;
import consulo.language.editor.inspection.scheme.InspectionManager;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiReference;
import consulo.util.lang.StringUtil;
import org.intellij.grammar.generator.ParserGeneratorUtil;
import org.intellij.grammar.generator.RuleGraphHelper;
import org.intellij.grammar.psi.BnfExternalExpression;
import org.intellij.grammar.psi.BnfFile;
import org.intellij.grammar.psi.BnfRule;
import org.intellij.grammar.psi.impl.BnfRefOrTokenImpl;
import org.jetbrains.annotations.Nls;
import javax.annotation.Nonnull;

import java.util.Set;

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

  @Override
  public ProblemDescriptor[] checkFile(@Nonnull PsiFile file, @Nonnull InspectionManager manager, boolean isOnTheFly) {
    ProblemsHolder problemsHolder = new ProblemsHolder(manager, file, isOnTheFly);
    checkFile(file, problemsHolder);
    return problemsHolder.getResultsArray();
  }

  private static void checkFile(final PsiFile file, final ProblemsHolder problemsHolder) {
    if (!(file instanceof BnfFile)) return;
    final Set<String> tokens = RuleGraphHelper.getTokenNameToTextMap((BnfFile)file).keySet();
    file.accept(new PsiRecursiveElementWalkingVisitor() {
      @Override
      public void visitElement(PsiElement element) {
        if (element instanceof BnfRule) {
          // do not check external rules
          if (ParserGeneratorUtil.Rule.isExternal((BnfRule)element)) return;
        }
        else if (element instanceof BnfExternalExpression) {
          // do not check external expressions
          return;
        }
        else if (element instanceof BnfRefOrTokenImpl) {
          PsiReference reference = element.getReference();
          Object resolve = reference == null ? null : reference.resolve();
          final String text = element.getText();
          if (resolve == null && !tokens.contains(text) && isTokenTextSuspicious(text)) {
            problemsHolder.registerProblem(element, "'"+text+"' token looks like a reference to a missing rule", new CreateRuleFromTokenFix(text));
          }
        }
        super.visitElement(element);
      }
    });
  }

  public static boolean isTokenTextSuspicious(String text) {
    boolean isLowercase = text.equals(text.toLowerCase());
    boolean isUppercase = !isLowercase && text.equals(text.toUpperCase());
    return !isLowercase && !isUppercase || isLowercase && StringUtil.containsAnyChar(text, "-_");
  }
}
