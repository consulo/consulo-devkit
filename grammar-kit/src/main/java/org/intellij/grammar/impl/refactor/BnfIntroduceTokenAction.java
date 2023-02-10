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

package org.intellij.grammar.impl.refactor;

import consulo.language.editor.refactoring.RefactoringSupportProvider;
import consulo.language.editor.refactoring.action.BasePlatformRefactoringAction;
import consulo.language.editor.refactoring.action.RefactoringActionHandler;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import org.intellij.grammar.psi.BnfFile;

import javax.annotation.Nonnull;

/**
 * @author greg
 */
public class BnfIntroduceTokenAction extends BasePlatformRefactoringAction {
  public BnfIntroduceTokenAction() {
  }

  protected boolean isAvailableInEditorOnly() {
    return true;
  }

  @Override
  protected boolean isAvailableForFile(PsiFile file) {
    return file instanceof BnfFile;
  }

  protected boolean isEnabledOnElements(@Nonnull PsiElement[] elements) {
    return false;
  }

  @Override
  protected RefactoringActionHandler getRefactoringHandler(@Nonnull RefactoringSupportProvider provider) {
    return new BnfIntroduceTokenHandler();
  }
}

