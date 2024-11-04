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
package org.jetbrains.idea.devkit.inspections.internal;

import com.intellij.java.analysis.impl.codeInspection.BaseJavaLocalInspectionTool;
import consulo.annotation.access.RequiredReadAction;
import consulo.devkit.localize.DevKitLocalize;
import consulo.devkit.util.PluginModuleUtil;
import consulo.language.editor.inspection.LocalInspectionToolSession;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.psi.PsiElementVisitor;

import jakarta.annotation.Nonnull;

public abstract class InternalInspection extends BaseJavaLocalInspectionTool<Object> {
    @Nonnull
    @Override
    public String getGroupDisplayName() {
        return DevKitLocalize.inspectionsGroupName().get();
    }

    @Override
    public boolean isEnabledByDefault() {
        return true;
    }

    @Nonnull
    @Override
    @RequiredReadAction
    public PsiElementVisitor buildVisitorImpl(
        @Nonnull ProblemsHolder holder,
        boolean isOnTheFly,
        LocalInspectionToolSession session,
        Object o
    ) {
        return isAllowed(holder) ? buildInternalVisitor(holder, isOnTheFly) : PsiElementVisitor.EMPTY_VISITOR;
    }

    @RequiredReadAction
    protected boolean isAllowed(ProblemsHolder holder) {
        return PluginModuleUtil.isConsuloOrPluginProject(holder.getFile());
    }

    public abstract PsiElementVisitor buildInternalVisitor(@Nonnull ProblemsHolder holder, boolean isOnTheFly);
}