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
import com.intellij.java.language.psi.JavaElementVisitor;
import com.intellij.java.language.psi.PsiClass;
import com.intellij.java.language.psi.PsiField;
import com.intellij.java.language.psi.PsiModifier;
import com.intellij.java.language.psi.util.InheritanceUtil;
import consulo.annotation.component.ExtensionImpl;
import consulo.devkit.inspections.valhalla.ValhallaClasses;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.psi.PsiReference;
import consulo.project.Project;
import org.jetbrains.idea.devkit.inspections.internal.InternalInspection;

import javax.annotation.Nonnull;

@ExtensionImpl
public class StatefulEpInspection extends InternalInspection {
    @Nonnull
    @Override
    public String getDisplayName() {
        return "Stateful Extension";
    }

    @Override
    public PsiElementVisitor buildInternalVisitor(@Nonnull ProblemsHolder holder, boolean isOnTheFly) {
        return new JavaElementVisitor() {
            @Override
            public void visitField(@Nonnull PsiField field) {
                checkField(field, holder);
            }
        };
    }

    private void checkField(PsiField field, ProblemsHolder holder) {
        PsiClass psiClass = field.getContainingClass();
        if (psiClass == null) {
            return;
        }

        final boolean isQuickFix = InheritanceUtil.isInheritor(psiClass, LocalQuickFix.class.getCanonicalName());
        if (isQuickFix || AnnotationUtil.isAnnotated(psiClass, ValhallaClasses.Impl, 0)) {
            final boolean isProjectComponent = isProjectServiceOrComponent(psiClass);

            for (Class c : new Class[]{PsiElement.class, PsiReference.class, Project.class}) {
                if (c == Project.class && (field.hasModifierProperty(PsiModifier.FINAL) || isProjectComponent)) {
                    continue;
                }
                String message;
                if (c != PsiElement.class) {
                    message = "Don't use " + c.getSimpleName() + " as a field in extension";
                }
                else {
                    message = "Potential memory leak: don't hold PsiElement, use SmartPsiElementPointer instead" +
                        (isQuickFix ? "; also see LocalQuickFixOnPsiElement" : "");
                }

                if (InheritanceUtil.isInheritor(field.getType(), c.getCanonicalName())) {
                    holder.registerProblem(field, message, ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
                }
            }
        }
    }

    private static boolean isProjectServiceOrComponent(PsiClass psiClass) {
        // TODO check ServiceAPI
        return true;
    }
}