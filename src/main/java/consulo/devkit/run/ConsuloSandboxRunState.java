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

package consulo.devkit.run;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.jetbrains.idea.devkit.sdk.ConsuloSdkType;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.RunConfigurationExtension;
import com.intellij.execution.configurations.CommandLineState;
import com.intellij.execution.configurations.ParametersList;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.util.ObjectUtil;
import consulo.application.ApplicationProperties;
import consulo.devkit.module.library.ConsuloPluginLibraryType;
import consulo.java.execution.configurations.OwnJavaParameters;

/**
 * @author VISTALL
 * @since 29.05.14
 */
public class ConsuloSandboxRunState extends CommandLineState
{
	protected OwnJavaParameters myJavaParameters;
	protected ExecutionEnvironment myEnvironment;

	public ConsuloSandboxRunState(@Nonnull ExecutionEnvironment environment, @Nonnull Sdk javaSdk, @Nonnull String consuloSdkHome, @Nullable Artifact artifact) throws ExecutionException
	{
		super(environment);
		myEnvironment = environment;
		myJavaParameters = createJavaParameters(environment, javaSdk, consuloSdkHome, artifact);
	}

	@Nonnull
	@Override
	protected ProcessHandler startProcess() throws ExecutionException
	{
		return myJavaParameters.createOSProcessHandler();
	}

	@Nonnull
	public String getSandboxPath(ConsuloRunConfigurationBase configuration) throws ExecutionException
	{
		return configuration.getSandboxPath();
	}

	private OwnJavaParameters createJavaParameters(@Nonnull ExecutionEnvironment env, @Nonnull Sdk javaSdk, @Nonnull String consuloSdkHome, @Nullable Artifact artifact) throws ExecutionException
	{
		ConsuloRunConfigurationBase profile = (ConsuloRunConfigurationBase) env.getRunProfile();
		final String dataPath = getSandboxPath(profile);

		final OwnJavaParameters params = new OwnJavaParameters();

		ParametersList vm = params.getVMParametersList();

		vm.addParametersString(profile.VM_PARAMETERS);
		params.getProgramParametersList().addParametersString(profile.PROGRAM_PARAMETERS);

		String selectedBuildPath = ObjectUtil.notNull(ConsuloSdkType.selectBuild(consuloSdkHome), consuloSdkHome);

		vm.defineProperty(PathManager.PROPERTY_CONFIG_PATH, dataPath + "/config");
		vm.defineProperty(PathManager.PROPERTY_SYSTEM_PATH, dataPath + "/system");
		vm.defineProperty(PathManager.PROPERTY_HOME_PATH, selectedBuildPath);
		// define plugin installation to default path
		String installPluginPath = dataPath + "/config/plugins";
		vm.defineProperty(ApplicationProperties.CONSULO_INSTALL_PLUGINS_PATH, installPluginPath);

		List<String> pluginPaths = new ArrayList<>();
		pluginPaths.add(installPluginPath);

		VirtualFile baseDir = env.getProject().getBaseDir();
		assert baseDir != null;

		// if plugin name is not plugin - append dep directory
		if(artifact == null || !"plugin".contains(artifact.getName()))
		{
			VirtualFile dep = baseDir.findChild(ConsuloPluginLibraryType.DEP_LIBRARY);
			if(dep != null)
			{
				pluginPaths.add(dep.getPath());
			}
		}

		if(artifact != null)
		{
			pluginPaths.add(artifact.getOutputPath());
		}

		vm.defineProperty(ApplicationProperties.CONSULO_PLUGINS_PATHS, String.join(File.pathSeparator, pluginPaths));

		File logFile = new File(dataPath, ConsuloRunConfigurationBase.LOG_FILE);
		FileUtil.createIfDoesntExist(logFile);
		vm.defineProperty(PathManager.PROPERTY_LOG_PATH, logFile.getParent());

		if(SystemInfo.isMac)
		{
			vm.defineProperty("idea.smooth.progress", "false");
			vm.defineProperty("apple.laf.useScreenMenuBar", "true");
		}
		else if(SystemInfo.isXWindow)
		{
			if(profile.VM_PARAMETERS == null || !profile.VM_PARAMETERS.contains("-Dsun.awt.disablegrab"))
			{
				vm.defineProperty("sun.awt.disablegrab", "true");
			}
		}
		vm.defineProperty(ApplicationProperties.CONSULO_IN_SANDBOX, "true");
		// always define internal as sandbox, later just drop this flag
		vm.defineProperty(ApplicationProperties.IDEA_IS_INTERNAL, "true");

		params.setWorkingDirectory(consuloSdkHome);

		params.setJdk(javaSdk);

		addConsuloLibs(selectedBuildPath, params);

		params.setMainClass(getMainClass());
		for(RunConfigurationExtension ext : Extensions.getExtensions(RunConfigurationExtension.EP_NAME))
		{
			ext.updateJavaParameters(profile, params, getRunnerSettings());
		}
		return params;
	}

	@Nonnull
	public String getMainClass()
	{
		return "com.intellij.idea.Main";
	}

	protected void addConsuloLibs(@Nonnull String consuloHomePath, @Nonnull OwnJavaParameters params)
	{
		String libPath = consuloHomePath + "/lib";

		boolean isMavenDistribution = new File(libPath, "consulo-desktop-boot.jar").exists();

		if(isMavenDistribution)
		{
			params.getVMParametersList().add("-Xbootclasspath/a:" + libPath + "/consulo-desktop-boot.jar");

			params.getClassPath().addFirst(libPath + "/consulo-desktop-bootstrap.jar");
			params.getClassPath().addFirst(libPath + "/consulo-extensions.jar");
			params.getClassPath().addFirst(libPath + "/consulo-util.jar");
			params.getClassPath().addFirst(libPath + "/consulo-util-rt.jar");
			params.getClassPath().addFirst(libPath + "/jdom.jar");
			params.getClassPath().addFirst(libPath + "/trove4j.jar");
			params.getClassPath().addFirst(libPath + "/jna.jar");
			params.getClassPath().addFirst(libPath + "/jna-platform.jar");
		}
		else
		{
			params.getVMParametersList().add("-Xbootclasspath/a:" + libPath + "/boot.jar");

			params.getClassPath().addFirst(libPath + "/log4j.jar");
			params.getClassPath().addFirst(libPath + "/jdom.jar");
			params.getClassPath().addFirst(libPath + "/trove4j.jar");
			params.getClassPath().addFirst(libPath + "/util.jar");
			params.getClassPath().addFirst(libPath + "/extensions.jar");
			params.getClassPath().addFirst(libPath + "/bootstrap.jar");
			params.getClassPath().addFirst(libPath + "/jna.jar");
			params.getClassPath().addFirst(libPath + "/jna-platform.jar");
		}
	}

	public OwnJavaParameters getJavaParameters()
	{
		return myJavaParameters;
	}
}
