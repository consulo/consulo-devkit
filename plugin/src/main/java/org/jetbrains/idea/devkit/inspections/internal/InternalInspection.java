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
import consulo.application.ApplicationManager;
import consulo.devkit.util.PluginModuleUtil;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.util.ModuleUtilCore;
import org.jetbrains.idea.devkit.DevKitBundle;

import javax.annotation.Nonnull;

public abstract class InternalInspection extends BaseJavaLocalInspectionTool {
  @Nonnull
  @Override
  public String getGroupDisplayName() {
    return DevKitBundle.message("inspections.group.name");
  }

  @Override
  public boolean isEnabledByDefault() {
    return true;
  }

  @Nonnull
  @Override
  @RequiredReadAction
  public final PsiElementVisitor buildVisitor(@Nonnull ProblemsHolder holder, boolean isOnTheFly) {
    return isAllowed(holder) ? buildInternalVisitor(holder, isOnTheFly) : new PsiElementVisitor() {
    };
  }

  @RequiredReadAction
  protected boolean isAllowed(ProblemsHolder holder) {
    //seems that internal inspection tests should test in most cases the inspection
    //and not that internal inspections are not available in non-idea non-plugin projects
    return isAllowedByDefault() || PluginModuleUtil.isConsuloOrPluginProject(holder.getProject(),
                                                                             ModuleUtilCore.findModuleForPsiElement(holder.getFile()));
  }

  protected boolean isAllowedByDefault() {
    return ApplicationManager.getApplication().isUnitTestMode();
  }

  public abstract PsiElementVisitor buildInternalVisitor(@Nonnull ProblemsHolder holder, boolean isOnTheFly);
}