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

package org.mustbe.consulo.devkit.vfs.backgroundTask;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.consulo.java.module.extension.JavaModuleExtension;
import org.consulo.vfs.backgroundTask.BackgroundTaskByVfsParameters;
import org.intellij.lang.jflex.vfs.backgroundTask.JFlexBackgroundTaskByVfsChangeProvider;
import org.jetbrains.annotations.NotNull;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.ide.plugins.cl.PluginClassLoader;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.JavaSdkType;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkTable;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;

/**
 * @author VISTALL
 * @since 01.05.14
 */
public class JFlexBackgroundTaskProvider extends JFlexBackgroundTaskByVfsChangeProvider
{
	@Override
	public void setDefaultParameters(
			@NotNull Project project,
			@NotNull VirtualFile virtualFile,
			@NotNull BackgroundTaskByVfsParameters backgroundTaskByVfsParameters)
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

		List<String> parameters = new ArrayList<String>();
		if(sdk != null)
		{
			GeneralCommandLine generalCommandLine = new GeneralCommandLine();

			((JavaSdkType) sdk.getSdkType()).setupCommandLine(generalCommandLine, sdk);
			backgroundTaskByVfsParameters.setExePath(generalCommandLine.getExePath());
			parameters.addAll(generalCommandLine.getParametersList().getList());
		}
		else
		{
			backgroundTaskByVfsParameters.setExePath(SystemInfo.isWindows ? "java.exe" : "java");
		}

		PluginClassLoader classLoader = (PluginClassLoader) JFlexBackgroundTaskProvider.class.getClassLoader();
		IdeaPluginDescriptor plugin = PluginManager.getPlugin(classLoader.getPluginId());

		parameters.add("-jar");
		parameters.add(new File(plugin.getPath(), "jflex/jflex.jar").getAbsolutePath());
		parameters.add("--charat");
		parameters.add("--nobak");
		parameters.add("--skel");
		parameters.add(new File(plugin.getPath(), "jflex/idea-flex.skeleton").getAbsolutePath());
		parameters.add("$FilePath$");

		backgroundTaskByVfsParameters.setProgramParameters(StringUtil.join(parameters, " "));
		backgroundTaskByVfsParameters.setWorkingDirectory("$FileParentPath$");
		backgroundTaskByVfsParameters.setOutPath("$FileParentPath$");

	}

	@NotNull
	@Override
	public String getTemplateName()
	{
		return "JFlex (Consulo Specific)";
	}
}
