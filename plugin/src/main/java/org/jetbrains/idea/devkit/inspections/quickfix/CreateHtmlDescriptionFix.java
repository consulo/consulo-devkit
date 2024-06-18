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

import consulo.application.AllIcons;
import consulo.application.ApplicationManager;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorPopupHelper;
import consulo.component.util.Iconable;
import consulo.devkit.localize.DevKitLocalize;
import consulo.fileEditor.FileEditorManager;
import consulo.fileTemplate.FileTemplate;
import consulo.fileTemplate.FileTemplateManager;
import consulo.fileTemplate.FileTemplateUtil;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.module.Module;
import consulo.project.Project;
import consulo.ui.ex.awt.JBList;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.util.collection.ArrayUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.idea.devkit.inspections.InspectionDescriptionNotFoundInspection;
import org.jetbrains.idea.devkit.inspections.IntentionDescriptionNotFoundInspection;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Konstantin Bulenkov
 */
public class CreateHtmlDescriptionFix implements LocalQuickFix, Iconable {
  private final String myFilename;
  private final Module myModule;
  @NonNls
  private static final String TEMPLATE_NAME = "InspectionDescription.html";
  private final boolean isIntention;

  public CreateHtmlDescriptionFix(String filename, Module module, boolean isIntention) {
    myModule = module;
    this.isIntention = isIntention;
    myFilename = isIntention ? filename : filename + ".html";
  }

  @Override
  @Nonnull
  public String getName() {
    return DevKitLocalize.createDescriptionFile().get();
  }

  @Override
  @Nonnull
  public String getFamilyName() {
    return "DevKit";
  }

  @Override
  public void applyFix(@Nonnull Project project, @Nonnull ProblemDescriptor descriptor) {
    final List<VirtualFile> virtualFiles =
      isIntention ? IntentionDescriptionNotFoundInspection.getPotentialRoots(myModule) : InspectionDescriptionNotFoundInspection.getPotentialRoots(
        myModule);
    final VirtualFile[] roots = prepare(VirtualFileUtil.toVirtualFileArray(virtualFiles));
    if (roots.length == 1) {
      ApplicationManager.getApplication().runWriteAction(() -> createDescription(roots[0]));

    }
    else {
      List<String> options = new ArrayList<>();
      for (VirtualFile file : roots) {
        String path = file.getPresentableUrl() + File.separator + getDescriptionFolderName() + File.separator + myFilename;
        if (isIntention) {
          path += File.separator + "description.html";
        }
        options.add(path);
      }
      final JBList files = new JBList(ArrayUtil.toStringArray(options));
      files.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      final JBPopup popup = JBPopupFactory.getInstance()
        .createPopupChooserBuilder(options)
        .setTitle(DevKitLocalize.selectTargetLocationOfDescription(myFilename).get())
        .setItemChosenCallback(desc -> {
          final int index = files.getSelectedIndex();
          if (0 <= index && index < roots.length) {
            ApplicationManager.getApplication().runWriteAction(() -> createDescription(roots[index]));
          }
        })
        .createPopup();
      final Editor editor = FileEditorManager.getInstance(myModule.getProject()).getSelectedTextEditor();
      if (editor == null) {
        return;
      }
      EditorPopupHelper.getInstance().showPopupInBestPositionFor(editor, popup);
    }
  }

  private void createDescription(VirtualFile root) {
    if (!root.isDirectory()) {
      return;
    }
    final PsiManager psiManager = PsiManager.getInstance(myModule.getProject());
    final PsiDirectory psiRoot = psiManager.findDirectory(root);
    PsiDirectory descrRoot = null;
    if (psiRoot == null) {
      return;
    }
    for (PsiDirectory dir : psiRoot.getSubdirectories()) {
      if (getDescriptionFolderName().equals(dir.getName())) {
        descrRoot = dir;
        break;
      }
    }

    try {
      descrRoot = descrRoot == null ? psiRoot.createSubdirectory(getDescriptionFolderName()) : descrRoot;
      if (isIntention) {
        PsiDirectory dir = descrRoot.findSubdirectory(myFilename);
        if (dir == null) {
          descrRoot = descrRoot.createSubdirectory(myFilename);
        }
      }
      final FileTemplate descrTemplate = FileTemplateManager.getInstance(myModule.getProject()).getJ2eeTemplate(TEMPLATE_NAME);
      final PsiElement template = FileTemplateUtil.createFromTemplate(descrTemplate,
                                                                      isIntention ? "description.html" : myFilename,
                                                                      (Map<String, Object>)null,
                                                                      descrRoot);
      if (template instanceof PsiFile) {
        final VirtualFile file = ((PsiFile)template).getVirtualFile();
        if (file != null) {
          FileEditorManager.getInstance(myModule.getProject()).openFile(file, true);
        }
      }
    }
    catch (Exception e) {//
    }
  }

  @Override
  public Image getIcon(int flags) {
    return ImageEffects.layered(AllIcons.FileTypes.Html, AllIcons.Actions.New);
  }

  private VirtualFile[] prepare(VirtualFile[] roots) {
    List<VirtualFile> found = new ArrayList<>();
    for (VirtualFile root : roots) {
      if (containsDescriptionDir(root)) {
        found.add(root);
      }
    }
    return found.size() > 0 ? VirtualFileUtil.toVirtualFileArray(found) : roots;
  }

  private boolean containsDescriptionDir(VirtualFile root) {
    if (!root.isDirectory()) {
      return false;
    }
    for (VirtualFile file : root.getChildren()) {
      if (file.isDirectory() && getDescriptionFolderName().equals(file.getName())) {
        return true;
      }
    }
    return false;
  }

  private String getDescriptionFolderName() {
    return isIntention ? "intentionDescriptions" : "inspectionDescriptions";
  }
}
