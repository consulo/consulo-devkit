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

import com.intellij.java.language.psi.PsiClass;
import com.intellij.java.language.psi.PsiIdentifier;
import com.intellij.java.language.psi.PsiMethod;
import com.intellij.java.language.psi.PsiModifier;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.content.LanguageContentFolderScopes;
import consulo.language.editor.inspection.InspectionTool;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.editor.inspection.scheme.InspectionManager;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.util.ModuleUtilCore;
import consulo.module.Module;
import consulo.module.content.ModuleRootManager;
import consulo.project.Project;
import consulo.util.collection.ContainerUtil;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.annotations.Nls;
import org.jetbrains.idea.devkit.inspections.quickfix.CreateHtmlDescriptionFix;
import org.jetbrains.idea.devkit.util.PsiUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author Konstantin Bulenkov
 */
@ExtensionImpl
public class InspectionDescriptionNotFoundInspection extends DevKitInspectionBase {
  @Override
  public ProblemDescriptor[] checkClass(@Nonnull PsiClass aClass, @Nonnull InspectionManager manager, boolean isOnTheFly, Object state) {
    final Project project = aClass.getProject();
    final PsiIdentifier nameIdentifier = aClass.getNameIdentifier();
    final Module module = ModuleUtilCore.findModuleForPsiElement(aClass);

    if (nameIdentifier == null || module == null || !PsiUtil.isInstantiable(aClass)) {
      return null;
    }

    final PsiClass base = DescriptionType.INSPECTION.findClass(project, GlobalSearchScope.allScope(project));

    if (base == null || !aClass.isInheritor(base, true) || isPathMethodsAreOverridden(aClass)) {
      return null;
    }

    PsiMethod method = findNearestMethod("getShortName", aClass);
    if (method != null && DescriptionType.INSPECTION.getClassNames().contains(method.getContainingClass().getQualifiedName())) {
      method = null;
    }
    final String filename =
      method == null ? InspectionTool.getShortName(aClass.getName()) : PsiUtil.getReturnedLiteral(method, aClass);
    if (filename == null) {
      return null;
    }

    for (PsiDirectory description : getInspectionDescriptionsDirs(module)) {
      final PsiFile file = description.findFile(filename + ".html");
      if (file == null) {
        continue;
      }
      final VirtualFile vf = file.getVirtualFile();
      if (vf == null) {
        continue;
      }
      if (vf.getNameWithoutExtension().equals(filename)) {
        return null;
      }
    }

    final PsiElement problem = getProblemElement(aClass, method);
    final ProblemDescriptor problemDescriptor = manager
      .createProblemDescriptor(problem == null ? nameIdentifier : problem, "Inspection does not have a description", isOnTheFly,
                               new LocalQuickFix[]{new CreateHtmlDescriptionFix(filename, module, false)},
                               ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
    return new ProblemDescriptor[]{problemDescriptor};
  }

  @Nullable
  private static PsiElement getProblemElement(PsiClass aClass, @Nullable PsiMethod method) {
    if (method != null && method.getContainingClass() == aClass) {
      return PsiUtil.getReturnedExpression(method);
    }
    else {
      return aClass.getNameIdentifier();
    }
  }

  private static boolean isPathMethodsAreOverridden(PsiClass aClass) {
    return !(isLastMethodDefinitionIn("getStaticDescription", DescriptionType.INSPECTION.getClassNames(), aClass) &&
      isLastMethodDefinitionIn("getDescriptionUrl", DescriptionType.INSPECTION.getClassNames(), aClass) &&
      isLastMethodDefinitionIn("getDescriptionContextClass", DescriptionType.INSPECTION.getClassNames(), aClass) &&
      isLastMethodDefinitionIn("getDescriptionFileName", DescriptionType.INSPECTION.getClassNames(), aClass));
  }

  private static boolean isLastMethodDefinitionIn(@Nonnull String methodName, @Nonnull Set<String> classFQN, PsiClass cls) {
    if (cls == null) {
      return false;
    }
    for (PsiMethod method : cls.getMethods()) {
      if (method.getName().equals(methodName)) {
        final PsiClass containingClass = method.getContainingClass();
        if (containingClass == null) {
          return false;
        }
        return classFQN.contains(containingClass.getQualifiedName());
      }
    }
    return isLastMethodDefinitionIn(methodName, classFQN, cls.getSuperClass());
  }

  public static List<VirtualFile> getPotentialRoots(Module module) {
    final PsiDirectory[] dirs = getInspectionDescriptionsDirs(module);
    final List<VirtualFile> result = new ArrayList<>();
    if (dirs.length != 0) {
      for (PsiDirectory dir : dirs) {
        final PsiDirectory parent = dir.getParentDirectory();
        if (parent != null) {
          result.add(parent.getVirtualFile());
        }
      }
    }
    else {
      ContainerUtil.addAll(result, ModuleRootManager.getInstance(module).getContentFolderFiles(LanguageContentFolderScopes.productionAndTest()));
    }
    return result;
  }

  @RequiredReadAction
  public static PsiDirectory[] getInspectionDescriptionsDirs(Module module) {
    return DescriptionCheckerUtil.getDescriptionsDirs(module, DescriptionType.INSPECTION);
  }

  @Nullable
  private static PsiMethod findNearestMethod(String name, @Nullable PsiClass cls) {
    if (cls == null) {
      return null;
    }
    for (PsiMethod method : cls.getMethods()) {
      if (method.getParameterList().getParametersCount() == 0 && method.getName().equals(name)) {
        return method.getModifierList().hasModifierProperty(PsiModifier.ABSTRACT) ? null : method;
      }
    }
    return findNearestMethod(name, cls.getSuperClass());
  }

  @Nls
  @Nonnull
  public String getDisplayName() {
    return "Inspection Description Checker";
  }

  @Nonnull
  public String getShortName() {
    return "InspectionDescriptionNotFoundInspection";
  }

  @Override
  public boolean isEnabledByDefault() {
    return true;
  }
}
