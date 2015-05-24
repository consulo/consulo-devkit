/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.devkit.util.PluginModuleUtil;
import com.intellij.codeInspection.LocalInspectionToolSession;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.psi.PsiElementVisitor;

public abstract class DevKitOnlyInspectionBase extends DevKitInspectionBase
{
	private static final PsiElementVisitor EMPTY_VISITOR = new PsiElementVisitor()
	{
	};

	@NotNull
	@Override
	public final PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly)
	{
		final Module module = ModuleUtilCore.findModuleForPsiElement(holder.getFile());
		if(module == null || !PluginModuleUtil.isPluginModuleOrDependency(module))
		{
			return EMPTY_VISITOR;
		}
		return buildInternalVisitor(holder, isOnTheFly);
	}

	@NotNull
	@Override
	public final PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly, @NotNull LocalInspectionToolSession session)
	{
		final Module module = ModuleUtilCore.findModuleForPsiElement(holder.getFile());
		if(module == null || !PluginModuleUtil.isPluginModuleOrDependency(module))
		{
			return EMPTY_VISITOR;
		}
		return buildInternalVisitor(holder, isOnTheFly);
	}


	public abstract PsiElementVisitor buildInternalVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly);
}
