/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package org.jetbrains.idea.devkit.inspections.quickfix;

import com.intellij.java.language.psi.PsiClass;
import consulo.devkit.localize.DevKitLocalize;
import consulo.devkit.module.extension.PluginModuleExtension;
import consulo.devkit.util.PluginModuleUtil;
import consulo.language.editor.FileModificationService;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.psi.PsiFile;
import consulo.language.util.IncorrectOperationException;
import consulo.language.util.ModuleUtilCore;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.project.Project;
import consulo.ui.ex.awt.Messages;
import consulo.undoRedo.CommandProcessor;
import consulo.xml.psi.xml.XmlFile;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.idea.devkit.util.ChooseModulesDialog;
import org.jetbrains.idea.devkit.util.DescriptorUtil;

import javax.annotation.Nonnull;
import java.util.List;

abstract class AbstractRegisterFix implements LocalQuickFix, DescriptorUtil.Patcher {
  protected final PsiClass myClass;
  private static final Logger LOG = Logger.getInstance(AbstractRegisterFix.class);

  public AbstractRegisterFix(PsiClass klass) {
    myClass = klass;
  }

  @Nonnull
  public String getFamilyName() {
    return DevKitLocalize.inspectionsComponentNotRegisteredQuickfixFamily().get();
  }

  @Nonnull
  public String getName() {
    return DevKitLocalize.inspectionsComponentNotRegisteredQuickfixName(getType()).get();
  }

  protected abstract String getType();

  // copy of com.intellij.ide.actions.CreateElementActionBase.filterMessage()
  protected static String filterMessage(String message) {
    if (message == null) return null;
    @NonNls final String ioExceptionPrefix = "java.io.IOException:";
    if (message.startsWith(ioExceptionPrefix)) {
      message = message.substring(ioExceptionPrefix.length());
    }
    return message;
  }

  public void applyFix(@Nonnull final Project project, @Nonnull ProblemDescriptor descriptor) {
    if (!FileModificationService.getInstance().preparePsiElementForWrite(descriptor.getPsiElement())) return;
    final PsiFile psiFile = myClass.getContainingFile();
    LOG.assertTrue(psiFile != null);
    final Module module = ModuleUtilCore.findModuleForFile(psiFile.getVirtualFile(), project);

    Runnable command = () -> {
      try {
        if (ModuleUtilCore.getExtension(module, PluginModuleExtension.class) != null) {
          final XmlFile pluginXml = PluginModuleUtil.getPluginXml(module);
          if (pluginXml != null) {
            DescriptorUtil.patchPluginXml(AbstractRegisterFix.this, myClass, pluginXml);
          }
        }
        else {
          List<Module> modules = PluginModuleUtil.getCandidateModules(module);
          if (modules.size() > 1) {
            final ChooseModulesDialog dialog = new ChooseModulesDialog(project, modules, getName());
            dialog.show();

            if (!dialog.isOK()) {
              return;
            }
            modules = dialog.getSelectedModules();
          }
          final XmlFile[] pluginXmls = new XmlFile[modules.size()];
          for (int i = 0; i < pluginXmls.length; i++) {
            pluginXmls[i] = PluginModuleUtil.getPluginXml(modules.get(i));
          }

          DescriptorUtil.patchPluginXml(AbstractRegisterFix.this, myClass, pluginXmls);
        }
        CommandProcessor.getInstance().markCurrentCommandAsGlobal(project);
      }
      catch (IncorrectOperationException e) {
        Messages.showMessageDialog(
          project,
          filterMessage(e.getMessage()),
          DevKitLocalize.inspectionsComponentNotRegisteredQuickfixError(getType()).get(),
          Messages.getErrorIcon()
        );
      }
    };
    CommandProcessor.getInstance().executeCommand(project, command, getName(), null);
  }
}
