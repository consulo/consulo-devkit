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

import com.intellij.codeInspection.BaseJavaLocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.psi.PsiElementVisitor;
import consulo.annotation.access.RequiredReadAction;
import consulo.devkit.util.PluginModuleUtil;

import javax.annotation.Nonnull;

public abstract class InternalInspection extends BaseJavaLocalInspectionTool
{
	@Nonnull
	@Override
	@RequiredReadAction
	public final PsiElementVisitor buildVisitor(@Nonnull ProblemsHolder holder, boolean isOnTheFly)
	{
		return isAllowed(holder) ? buildInternalVisitor(holder, isOnTheFly) : new PsiElementVisitor()
		{
		};
	}

	@RequiredReadAction
	protected boolean isAllowed(ProblemsHolder holder)
	{
		//seems that internal inspection tests should test in most cases the inspection
		//and not that internal inspections are not available in non-idea non-plugin projects
		return isAllowedByDefault() || PluginModuleUtil.isConsuloOrPluginProject(holder.getProject(), ModuleUtilCore.findModuleForPsiElement(holder.getFile()));
	}

	protected boolean isAllowedByDefault()
	{
		return ApplicationManager.getApplication().isUnitTestMode();
	}

	public abstract PsiElementVisitor buildInternalVisitor(@Nonnull ProblemsHolder holder, boolean isOnTheFly);
}