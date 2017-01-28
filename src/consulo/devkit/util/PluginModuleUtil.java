/*
 * Copyright 2013-2016 consulo.io
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

package consulo.devkit.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.devkit.build.PluginBuildUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.containers.HashSet;
import consulo.annotations.RequiredReadAction;
import consulo.devkit.module.extension.PluginModuleExtension;
import consulo.java.roots.SpecialDirUtil;

/**
 * @author VISTALL
 * @since 13:48/23.05.13
 */
public class PluginModuleUtil
{
	public static final String PLUGIN_XML = "plugin.xml";

	public static Module[] getAllPluginModules(final Project project)
	{
		List<Module> modules = new ArrayList<Module>();
		Module[] allModules = ModuleManager.getInstance(project).getModules();
		for(Module module : allModules)
		{
			if(ModuleUtil.getExtension(module, PluginModuleExtension.class) != null)
			{
				modules.add(module);
			}
		}
		return modules.toArray(new Module[modules.size()]);
	}

	@Nullable
	public static XmlFile getPluginXml(Module module)
	{
		if(module == null)
		{
			return null;
		}
		if(ModuleUtil.getExtension(module, PluginModuleExtension.class) == null)
		{
			return null;
		}

		PsiManager psiManager = PsiManager.getInstance(module.getProject());

		List<VirtualFile> virtualFiles = SpecialDirUtil.collectSpecialDirs(module, SpecialDirUtil.META_INF);
		for(VirtualFile virtualFile : virtualFiles)
		{
			VirtualFile child = virtualFile.findChild(PLUGIN_XML);
			if(child == null)
			{
				continue;
			}

			PsiFile file = psiManager.findFile(child);
			if(file instanceof XmlFile)
			{
				return (XmlFile) file;
			}
		}
		return null;
	}

	@RequiredReadAction
	public static boolean isPluginModuleOrDependency(@Nullable Module module)
	{
		if(module == null)
		{
			return false;
		}

		if(ModuleUtil.getExtension(module, PluginModuleExtension.class) != null)
		{
			return true;
		}

		return getCandidateModules(module).size() > 0;
	}

	@RequiredReadAction
	@NotNull
	public static List<Module> getCandidateModules(Module module)
	{
		final Module[] modules = ModuleManager.getInstance(module.getProject()).getModules();
		final List<Module> candidates = new ArrayList<Module>(modules.length);
		final Set<Module> deps = new HashSet<Module>(modules.length);
		for(Module m : modules)
		{
			if(ModuleUtil.getExtension(module, PluginModuleExtension.class) != null)
			{
				deps.clear();
				PluginBuildUtil.getDependencies(m, deps);

				if(deps.contains(module) && getPluginXml(m) != null)
				{
					candidates.add(m);
				}
			}
		}
		return candidates;
	}

	public static boolean isConsuloProject(Project project)
	{
		return project.getName().equals("consulo");
	}

	@RequiredReadAction
	public static boolean isConsuloOrPluginProject(@NotNull Project project, @Nullable Module module)
	{
		if(PluginModuleUtil.isConsuloProject(project))
		{
			return true;
		}

		if(module != null)
		{
			if(PluginModuleUtil.isPluginModuleOrDependency(module))
			{
				return true;
			}
		}

		for(Module temp : ModuleManager.getInstance(project).getModules())
		{
			if(PluginModuleUtil.isPluginModuleOrDependency(temp))
			{
				return true;
			}
		}
		return false;
	}
}
