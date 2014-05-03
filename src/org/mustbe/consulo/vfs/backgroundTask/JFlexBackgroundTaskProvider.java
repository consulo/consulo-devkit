/*
 * Copyright 2013-2014 must-be.org
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

package org.mustbe.consulo.vfs.backgroundTask;

import java.io.File;

import org.consulo.java.module.extension.JavaModuleExtension;
import org.consulo.vfs.backgroundTask.BackgroundTaskByVfsParameters;
import org.intellij.lang.jflex.vfs.backgroundTask.JFlexBackgroundTaskByVfsChangeProvider;
import org.jetbrains.annotations.NotNull;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.ide.plugins.cl.PluginClassLoader;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkTable;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.vfs.VirtualFile;

/**
 * @author VISTALL
 * @since 01.05.14
 */
public class JFlexBackgroundTaskProvider extends JFlexBackgroundTaskByVfsChangeProvider
{
	@Override
	public void setDefaultParameters(@NotNull Project project, @NotNull VirtualFile virtualFile, @NotNull BackgroundTaskByVfsParameters parameters)
	{
		Sdk sdk = null;
		Module module = ModuleUtilCore.findModuleForFile(virtualFile, project);
		if(module != null)
		{
			sdk = ModuleUtilCore.getSdk(module, JavaModuleExtension.class);
		}

		if(sdk == null)
		{
			sdk = SdkTable.getInstance().findBundleSdkByType(JavaSdk.class);
		}

		if(sdk == null)
		{
			sdk = SdkTable.getInstance().findMostRecentSdkOfType(JavaSdk.getInstance());
		}

		if(sdk != null)
		{
			String vmExecutablePath = JavaSdk.getInstance().getVMExecutablePath(sdk);
			parameters.setExePath(vmExecutablePath);
		}
		else
		{
			parameters.setExePath(SystemInfo.isWindows ? "java.exe" : "java");
		}

		PluginClassLoader classLoader = (PluginClassLoader) JFlexBackgroundTaskProvider.class.getClassLoader();
		IdeaPluginDescriptor plugin = PluginManager.getPlugin(classLoader.getPluginId());

		StringBuilder builder = new StringBuilder();
		builder.append("-jar ");
		builder.append(new File(plugin.getPath(), "jflex/jflex.jar"));
		builder.append(" --charat --nobak --skel ");
		builder.append(new File(plugin.getPath(), "jflex/idea-flex.skeleton"));
		builder.append(" $FilePath$");

		parameters.setProgramParameters(builder.toString());
		parameters.setWorkingDirectory("$FileParentPath$");
		parameters.setOutPath("$FileParentPath$");

	}

	@NotNull
	@Override
	public String getTemplateName()
	{
		return "JFlex (Consulo Specific)";
	}
}
