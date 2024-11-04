/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
import consulo.annotation.access.RequiredReadAction;
import consulo.language.content.LanguageContentFolderScopes;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiManager;
import consulo.module.Module;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.layer.ContentFolder;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class DescriptionCheckerUtil {
    @Nonnull
    @RequiredReadAction
    public static PsiDirectory[] getDescriptionsDirs(Module module, DescriptionType descriptionType) {
        List<PsiDirectory> dirs = new ArrayList<>();
        ModuleRootManager manager = ModuleRootManager.getInstance(module);
        PsiManager psiManager = PsiManager.getInstance(module.getProject());

        for (ContentFolder folder : manager.getContentFolders(LanguageContentFolderScopes.production())) {
            VirtualFile file = folder.getFile();
            if (file == null) {
                continue;
            }

            VirtualFile childDir = file.findFileByRelativePath(descriptionType.getDescriptionFolder());
            if (childDir != null) {
                PsiDirectory dir = psiManager.findDirectory(childDir);
                if (dir != null) {
                    dirs.add(dir);
                }
            }
        }

        return dirs.toArray(PsiDirectory.EMPTY_ARRAY);
    }

    @Nullable
    @RequiredReadAction
    public static String getDescriptionDirName(PsiClass aClass) {
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
}
