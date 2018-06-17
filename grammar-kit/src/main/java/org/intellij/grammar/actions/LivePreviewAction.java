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

package org.intellij.grammar.actions;

import org.intellij.grammar.livePreview.LivePreviewHelper;
import org.intellij.grammar.psi.BnfFile;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.psi.PsiFile;

/**
 * @author gregsh
 */
public class LivePreviewAction extends DumbAwareAction {
  @Override
  public void update(AnActionEvent e) {
    PsiFile psiFile = e.getData(LangDataKeys.PSI_FILE);
    e.getPresentation().setEnabledAndVisible(psiFile instanceof BnfFile);
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    PsiFile psiFile = e.getData(LangDataKeys.PSI_FILE);
    if (!(psiFile instanceof BnfFile)) return;

    LivePreviewHelper.showFor((BnfFile)psiFile);
  }
}
