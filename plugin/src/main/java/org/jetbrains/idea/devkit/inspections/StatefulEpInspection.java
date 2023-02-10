/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

import com.intellij.java.language.codeInsight.AnnotationUtil;
import com.intellij.java.language.psi.PsiClass;
import com.intellij.java.language.psi.PsiField;
import com.intellij.java.language.psi.PsiModifier;
import com.intellij.java.language.psi.util.InheritanceUtil;
import consulo.annotation.component.ExtensionImpl;
import consulo.devkit.inspections.valhalla.ValhallaClasses;
import consulo.ide.ServiceManager;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.editor.inspection.scheme.InspectionManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReference;
import consulo.project.Project;
import consulo.util.collection.ContainerUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

@ExtensionImpl
public class StatefulEpInspection extends DevKitInspectionBase {
  @Nonnull
  @Override
  public String getDisplayName() {
    return "Stateful Extension";
  }

  @Nullable
  @Override
  public ProblemDescriptor[] checkClass(@Nonnull PsiClass psiClass, @Nonnull InspectionManager manager, boolean isOnTheFly) {
    PsiField[] fields = psiClass.getFields();
    if (fields.length == 0) {
      return super.checkClass(psiClass, manager, isOnTheFly);
    }

    final boolean isQuickFix = InheritanceUtil.isInheritor(psiClass, LocalQuickFix.class.getCanonicalName());
    if (isQuickFix || AnnotationUtil.isAnnotated(psiClass, ValhallaClasses.Impl, 0)) {
      final boolean isProjectComponent = isProjectServiceOrComponent(psiClass);

      List<ProblemDescriptor> result = ContainerUtil.newArrayList();
      for (final PsiField field : fields) {
        for (Class c : new Class[]{
          PsiElement.class,
          PsiReference.class,
          Project.class
        }) {
          if (c == Project.class && (field.hasModifierProperty(PsiModifier.FINAL) || isProjectComponent)) {
            continue;
          }
          String message = c == PsiElement.class ? "Potential memory leak: don't hold PsiElement, use SmartPsiElementPointer instead" +
            (isQuickFix ? "; also see LocalQuickFixOnPsiElement" : "") : "Don't use " + c.getSimpleName() + " as a field in " +
            "extension";
          if (InheritanceUtil.isInheritor(field.getType(), c.getCanonicalName())) {
            result.add(manager.createProblemDescriptor(field, message, true, ProblemHighlightType.GENERIC_ERROR_OR_WARNING, isOnTheFly));
          }
        }
      }
      return result.toArray(new ProblemDescriptor[result.size()]);
    }
    return super.checkClass(psiClass, manager, isOnTheFly);
  }

  private static boolean isProjectServiceOrComponent(PsiClass psiClass) {

    return true;
  }
}