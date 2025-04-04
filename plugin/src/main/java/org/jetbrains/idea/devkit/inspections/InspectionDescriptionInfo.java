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
import com.intellij.java.language.psi.PsiMethod;
import consulo.annotation.access.RequiredReadAction;
import consulo.language.editor.inspection.InspectionTool;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.module.Module;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.idea.devkit.util.PsiUtil;

import jakarta.annotation.Nullable;

public class InspectionDescriptionInfo {

    private final String myFilename;
    private final PsiMethod myMethod;
    private final PsiFile myDescriptionFile;

    private InspectionDescriptionInfo(String filename, @Nullable PsiMethod method, @Nullable PsiFile descriptionFile) {
        myFilename = filename;
        myMethod = method;
        myDescriptionFile = descriptionFile;
    }

    @RequiredReadAction
    public static InspectionDescriptionInfo create(Module module, PsiClass psiClass) {
        PsiMethod method = PsiUtil.findNearestMethod("getShortName", psiClass);
        if (method != null && DescriptionType.INSPECTION.getClassNames().contains(method.getContainingClass().getQualifiedName())) {
            method = null;
        }

        final String filename = method == null
            ? InspectionTool.getShortName(psiClass.getName())
            : PsiUtil.getReturnedLiteral(method, psiClass);

        PsiFile descriptionFile = resolveInspectionDescriptionFile(module, filename);
        return new InspectionDescriptionInfo(filename, method, descriptionFile);
    }

    @Nullable
    @RequiredReadAction
    private static PsiFile resolveInspectionDescriptionFile(Module module, @Nullable String filename) {
        if (filename == null) {
            return null;
        }

        for (PsiDirectory description : DescriptionCheckerUtil.getDescriptionsDirs(module, DescriptionType.INSPECTION)) {
            final PsiFile file = description.findFile(filename + ".html");
            if (file == null) {
                continue;
            }
            final VirtualFile vf = file.getVirtualFile();
            if (vf == null) {
                continue;
            }
            if (vf.getNameWithoutExtension().equals(filename)) {
                return PsiManager.getInstance(module.getProject()).findFile(vf);
            }
        }
        return null;
    }

    public boolean isValid() {
        return myFilename != null;
    }

    public String getFilename() {
        assert isValid();
        return myFilename;
    }

    @Nullable
    public PsiMethod getShortNameMethod() {
        return myMethod;
    }

    @Nullable
    public PsiFile getDescriptionFile() {
        return myDescriptionFile;
    }

    public boolean hasDescriptionFile() {
        return getDescriptionFile() != null;
    }
}
