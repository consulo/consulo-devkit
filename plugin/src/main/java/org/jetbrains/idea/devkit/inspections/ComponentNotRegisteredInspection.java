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
package org.jetbrains.idea.devkit.inspections;

import com.intellij.java.language.psi.JavaPsiFacade;
import com.intellij.java.language.psi.PsiClass;
import com.intellij.java.language.psi.PsiIdentifier;
import com.intellij.java.language.psi.util.PsiUtil;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.devkit.util.PluginModuleUtil;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.editor.inspection.scheme.InspectionManager;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.util.ModuleUtilCore;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.project.Project;
import consulo.ui.ex.action.AnAction;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.idea.devkit.DevKitBundle;
import org.jetbrains.idea.devkit.inspections.quickfix.RegisterActionFix;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * @author swr
 */
@ExtensionImpl
public class ComponentNotRegisteredInspection extends DevKitInspectionBase {
  public boolean CHECK_ACTIONS = true;
  public boolean IGNORE_NON_PUBLIC = true;
  private static final Logger LOG = Logger.getInstance(ComponentNotRegisteredInspection.class);

  @Nonnull
  public String getDisplayName() {
    return DevKitBundle.message("inspections.component.not.registered.name");
  }

  @Nonnull
  @NonNls
  public String getShortName() {
    return "ComponentNotRegistered";
  }

  public boolean isEnabledByDefault() {
    return true;
  }

  @Nullable
  public JComponent createOptionsPanel() {
    final JPanel jPanel = new JPanel();
    jPanel.setLayout(new BoxLayout(jPanel, BoxLayout.Y_AXIS));

    final JCheckBox ignoreNonPublic =
      new JCheckBox(DevKitBundle.message("inspections.component.not.registered.option.ignore.non.public"), IGNORE_NON_PUBLIC);
    ignoreNonPublic.addChangeListener(new ChangeListener() {
      public void stateChanged(ChangeEvent e) {
        IGNORE_NON_PUBLIC = ignoreNonPublic.isSelected();
      }
    });

    final JCheckBox checkJavaActions =
      new JCheckBox(DevKitBundle.message("inspections.component.not.registered.option.check.actions"), CHECK_ACTIONS);
    checkJavaActions.addChangeListener(new ChangeListener() {
      public void stateChanged(ChangeEvent e) {
        final boolean selected = checkJavaActions.isSelected();
        CHECK_ACTIONS = selected;
        ignoreNonPublic.setEnabled(selected);
      }
    });

    jPanel.add(checkJavaActions);
    jPanel.add(ignoreNonPublic);
    return jPanel;
  }

  @Override
  @Nullable
  public ProblemDescriptor[] checkClass(@Nonnull PsiClass checkedClass, @Nonnull InspectionManager manager, boolean isOnTheFly) {
    final PsiFile psiFile = checkedClass.getContainingFile();
    final PsiIdentifier classIdentifier = checkedClass.getNameIdentifier();
    if (checkedClass.getQualifiedName() != null &&
      classIdentifier != null &&
      psiFile != null &&
      psiFile.getVirtualFile() != null &&
      !isAbstract(checkedClass)) {
      if (PsiUtil.isInnerClass(checkedClass)) {
        // don't check inner classes (make this an option?)
        return null;
      }

      final PsiManager psiManager = checkedClass.getManager();
      final GlobalSearchScope scope = checkedClass.getResolveScope();

      if (CHECK_ACTIONS) {
        final PsiClass actionClass = JavaPsiFacade.getInstance(psiManager.getProject()).findClass(AnAction.class.getName(), scope);
        if (actionClass == null) {
          // stop if action class cannot be found (non-devkit module/project)
          return null;
        }
        if (checkedClass.isInheritor(actionClass, true)) {
          if (IGNORE_NON_PUBLIC && !isPublic(checkedClass)) {
            return null;
          }
          if (!isActionRegistered(checkedClass) && canFix(checkedClass)) {
            final LocalQuickFix fix = new RegisterActionFix(checkedClass);
            final ProblemDescriptor
              problem =
              manager.createProblemDescriptor(classIdentifier,
                                              DevKitBundle.message("inspections.component.not.registered.message",
                                                                   DevKitBundle.message("new.menu.action.text")),
                                              fix,
                                              ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                                              isOnTheFly);
            return new ProblemDescriptor[]{problem};
          }
          else {
            // action IS registered, stop here
            return null;
          }
        }
      }
    }
    return null;
  }

  @RequiredReadAction
  private static boolean canFix(PsiClass psiClass) {
    final Project project = psiClass.getProject();
    final PsiFile psiFile = psiClass.getContainingFile();
    LOG.assertTrue(psiFile != null);
    final Module module = ModuleUtilCore.findModuleForFile(psiFile.getVirtualFile(), project);
    return PluginModuleUtil.isPluginModuleOrDependency(module);
  }
}
