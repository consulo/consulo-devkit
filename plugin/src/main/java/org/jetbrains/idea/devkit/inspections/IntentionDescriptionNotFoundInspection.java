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

import com.intellij.java.language.psi.JavaElementVisitor;
import com.intellij.java.language.psi.JavaPsiFacade;
import com.intellij.java.language.psi.PsiClass;
import com.intellij.java.language.psi.PsiIdentifier;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.content.LanguageContentFolderScopes;
import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.intention.IntentionAction;
import consulo.language.editor.intention.SyntheticIntentionAction;
import consulo.language.psi.*;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.module.Module;
import consulo.module.content.ModuleRootManager;
import consulo.project.Project;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.annotations.Nls;
import org.jetbrains.idea.devkit.inspections.internal.InternalInspection;
import org.jetbrains.idea.devkit.inspections.quickfix.CreateHtmlDescriptionFix;
import org.jetbrains.idea.devkit.util.PsiUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Konstantin Bulenkov
 */
@ExtensionImpl
public class IntentionDescriptionNotFoundInspection extends InternalInspection {
    private static final String INTENTION = IntentionAction.class.getName();
    private static final String SYNTHETIC_INTENTION = SyntheticIntentionAction.class.getName();
    private static final String INSPECTION_DESCRIPTIONS = "intentionDescriptions";

    @Override
    public PsiElementVisitor buildInternalVisitor(@Nonnull ProblemsHolder holder, boolean isOnTheFly) {
        return new JavaElementVisitor() {
            @Override
            @RequiredReadAction
            public void visitClass(@Nonnull PsiClass aClass) {
                checkClass(aClass, holder, isOnTheFly);
            }
        };
    }

    @RequiredReadAction
    private void checkClass(PsiClass psiClass, ProblemsHolder holder, boolean isOnTheFly) {
        final Project project = psiClass.getProject();
        final PsiIdentifier nameIdentifier = psiClass.getNameIdentifier();
        final Module module = psiClass.getModule();

        if (nameIdentifier == null || module == null || !PsiUtil.isInstantiable(psiClass)) {
            return;
        }

        final PsiClass base = JavaPsiFacade.getInstance(project).findClass(INTENTION, GlobalSearchScope.allScope(project));

        if (base == null || !psiClass.isInheritor(base, true)) {
            return;
        }

        final PsiClass ignoredBase = JavaPsiFacade.getInstance(project).findClass(SYNTHETIC_INTENTION, GlobalSearchScope.allScope(project));
        if (ignoredBase != null && psiClass.isInheritor(ignoredBase, true)) {
            return;
        }

        String descriptionDir = getDescriptionDirName(psiClass);
        if (StringUtil.isEmptyOrSpaces(descriptionDir)) {
            return;
        }

        for (PsiDirectory description : getIntentionDescriptionsDirs(module)) {
            PsiDirectory dir = description.findSubdirectory(descriptionDir);
            if (dir == null) {
                continue;
            }
            final PsiFile descr = dir.findFile("description.html");
            if (descr != null) {
                if (!hasBeforeAndAfterTemplate(dir.getVirtualFile())) {
                    PsiElement problem = psiClass.getNameIdentifier();
                    holder.registerProblem(
                        problem == null ? nameIdentifier : problem,
                        "Intention must have 'before.*.template' and 'after.*.template' beside 'description.html'",
                        ProblemHighlightType.GENERIC_ERROR_OR_WARNING
                    );
                }

                return;
            }
        }

        final PsiElement problem = psiClass.getNameIdentifier();
        holder.registerProblem(
            problem == null ? nameIdentifier : problem,
            "Intention does not have a description",
            ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
            new CreateHtmlDescriptionFix(descriptionDir, module, true)
        );
    }

    @Nullable
    @RequiredReadAction
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
            ContainerUtil.addAll(
                result,
                ModuleRootManager.getInstance(module).getContentFolderFiles(LanguageContentFolderScopes.productionAndTest())
            );
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
    @Override
    public String getDisplayName() {
        return "Intention Description Checker";
    }

    @Nonnull
    @Override
    public String getShortName() {
        return "IntentionDescriptionNotFoundInspection";
    }
}
