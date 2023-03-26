/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
import consulo.annotation.component.ExtensionImpl;
import consulo.language.content.LanguageContentFolderScopes;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.editor.inspection.scheme.InspectionManager;
import consulo.language.editor.intention.IntentionAction;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiPackage;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.util.ModuleUtilCore;
import consulo.module.Module;
import consulo.module.content.ModuleRootManager;
import consulo.project.Project;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.annotations.Nls;
import org.jetbrains.idea.devkit.inspections.quickfix.CreateHtmlDescriptionFix;
import org.jetbrains.idea.devkit.util.PsiUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Konstantin Bulenkov
 */
@ExtensionImpl
public class IntentionDescriptionNotFoundInspection extends DevKitInspectionBase {
  private static final String INTENTION = IntentionAction.class.getName();
  private static final String INSPECTION_DESCRIPTIONS = "intentionDescriptions";

  @Override
  public ProblemDescriptor[] checkClass(@Nonnull PsiClass aClass, @Nonnull InspectionManager manager, boolean isOnTheFly, Object state) {
    final Project project = aClass.getProject();
    final PsiIdentifier nameIdentifier = aClass.getNameIdentifier();
    final Module module = ModuleUtilCore.findModuleForPsiElement(aClass);

    if (nameIdentifier == null || module == null || !PsiUtil.isInstantiable(aClass)) return null;

    final PsiClass base = JavaPsiFacade.getInstance(project).findClass(INTENTION, GlobalSearchScope.allScope(project));

    if (base == null || !aClass.isInheritor(base, true)) return null;

    String descriptionDir = getDescriptionDirName(aClass);
    if (StringUtil.isEmptyOrSpaces(descriptionDir)) {
      return null;
    }

    for (PsiDirectory description : getIntentionDescriptionsDirs(module)) {
      PsiDirectory dir = description.findSubdirectory(descriptionDir);
      if (dir == null) continue;
      final PsiFile descr = dir.findFile("description.html");
      if (descr != null) {
        if (!hasBeforeAndAfterTemplate(dir.getVirtualFile())) {
          PsiElement problem = aClass.getNameIdentifier();
          ProblemDescriptor problemDescriptor = manager.createProblemDescriptor(problem == null ? nameIdentifier : problem,
                                                                                "Intention must have 'before.*.template' and 'after.*.template' beside 'description.html'",
                                                                                isOnTheFly,
                                                                                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                                                                                false);
          return new ProblemDescriptor[]{problemDescriptor};
        }

        return null;
      }
    }


    final PsiElement problem = aClass.getNameIdentifier();
    final ProblemDescriptor problemDescriptor = manager
      .createProblemDescriptor(problem == null ? nameIdentifier : problem, "Intention does not have a description", isOnTheFly,
                               new LocalQuickFix[]{new CreateHtmlDescriptionFix(descriptionDir, module, true)},
                               ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
    return new ProblemDescriptor[]{problemDescriptor};
  }

  @Nullable
  private static String getDescriptionDirName(PsiClass aClass) {
    String descriptionDir = "";
    PsiClass each = aClass;
    while (each != null) {
      String name = each.getName();
      if (StringUtil.isEmptyOrSpaces(name)) {
        return null;
      }
      descriptionDir = name + descriptionDir;
      each = each.getContainingClass();
    }
    return descriptionDir;
  }

  private static boolean hasBeforeAndAfterTemplate(@Nonnull VirtualFile dir) {
    boolean hasBefore = false;
    boolean hasAfter = false;

    for (VirtualFile file : dir.getChildren()) {
      String name = file.getName();
      if (name.endsWith(".template")) {
        if (name.startsWith("before.")) {
          hasBefore = true;
        }
        else if (name.startsWith("after.")) {
          hasAfter = true;
        }
      }
    }

    return hasBefore && hasAfter;
  }

  public static List<VirtualFile> getPotentialRoots(Module module) {
    final PsiDirectory[] dirs = getIntentionDescriptionsDirs(module);
    final List<VirtualFile> result = new ArrayList<VirtualFile>();
    if (dirs.length != 0) {
      for (PsiDirectory dir : dirs) {
        final PsiDirectory parent = dir.getParentDirectory();
        if (parent != null) result.add(parent.getVirtualFile());
      }
    }
    else {
      ContainerUtil.addAll(result,
                           ModuleRootManager.getInstance(module).getContentFolderFiles(LanguageContentFolderScopes.productionAndTest()));
    }
    return result;
  }

  public static PsiDirectory[] getIntentionDescriptionsDirs(Module module) {
    final PsiPackage aPackage = JavaPsiFacade.getInstance(module.getProject()).findPackage(INSPECTION_DESCRIPTIONS);
    if (aPackage != null) {
      return aPackage.getDirectories(GlobalSearchScope.moduleWithDependenciesScope(module));
    }
    else {
      return PsiDirectory.EMPTY_ARRAY;
    }
  }

  @Nls
  @Nonnull
  public String getDisplayName() {
    return "Intention Description Checker";
  }

  @Nonnull
  public String getShortName() {
    return "IntentionDescriptionNotFoundInspection";
  }

  @Override
  public boolean isEnabledByDefault() {
    return true;
  }
}
