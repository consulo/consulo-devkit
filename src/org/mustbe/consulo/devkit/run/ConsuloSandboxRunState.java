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

package org.mustbe.consulo.devkit.run;

import java.io.File;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.RunConfigurationExtension;
import com.intellij.execution.configurations.CommandLineState;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.configurations.ParametersList;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.packaging.artifacts.Artifact;

/**
 * @author VISTALL
 * @since 29.05.14
 */
public class ConsuloSandboxRunState extends CommandLineState
{
	protected JavaParameters myJavaParameters;
	protected ExecutionEnvironment myEnvironment;

	public ConsuloSandboxRunState(@NotNull ExecutionEnvironment environment, @NotNull Sdk javaSdk, @NotNull String consuloSdkHome,
			@Nullable Artifact artifact) throws ExecutionException
	{
		super(environment);
		myEnvironment = environment;
		myJavaParameters = createJavaParameters(environment, javaSdk, consuloSdkHome, artifact);
	}

	@NotNull
	@Override
	protected ProcessHandler startProcess() throws ExecutionException
	{
		return myJavaParameters.createOSProcessHandler();
	}

	private JavaParameters createJavaParameters(@NotNull ExecutionEnvironment env, @NotNull Sdk javaSdk, @NotNull String consuloSdkHome,
			@Nullable Artifact artifact) throws ExecutionException
	{
		ConsuloRunConfigurationBase profile = (ConsuloRunConfigurationBase) env.getRunProfile();
		final String dataPath = profile.getSandboxPath();

		final JavaParameters params = new JavaParameters();

		ParametersList vm = params.getVMParametersList();

		vm.addParametersString(profile.VM_PARAMETERS);
		params.getProgramParametersList().addParametersString(profile.PROGRAM_PARAMETERS);

		vm.defineProperty(PathManager.PROPERTY_CONFIG_PATH, dataPath + "/config");
		vm.defineProperty(PathManager.PROPERTY_SYSTEM_PATH, dataPath + "/system");

		if(artifact != null)
		{
			vm.defineProperty(PathManager.PROPERTY_PLUGINS_PATH, artifact.getOutputPath());
		}

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
				vm.defineProperty("sun.awt.disablegrab", "true"); // See http://devnet.jetbrains.net/docs/DOC-1142
			}
		}
		vm.defineProperty("consulo.in.sandbox", "true");
		params.setWorkingDirectory(consuloSdkHome + File.separator + "bin" + File.separator);

		params.setJdk(javaSdk);

		addConsuloLibs(consuloSdkHome, params);

		params.setMainClass(getMainClass());
		for(RunConfigurationExtension ext : Extensions.getExtensions(RunConfigurationExtension.EP_NAME))
		{
			ext.updateJavaParameters(profile, params, getRunnerSettings());
		}
		return params;
	}

	@NotNull
	public String getMainClass()
	{
		return "com.intellij.idea.Main";
	}

	protected void addConsuloLibs(@NotNull String consuloHomePath, @NotNull JavaParameters params)
	{
		String libPath = consuloHomePath + "/lib";

		params.getVMParametersList().add("-Xbootclasspath/a:" + libPath + "/boot.jar");

		params.getClassPath().addFirst(libPath + "/log4j.jar");
		params.getClassPath().addFirst(libPath + "/jdom.jar");
		params.getClassPath().addFirst(libPath + "/trove4j.jar");
		params.getClassPath().addFirst(libPath + "/util.jar");
		params.getClassPath().addFirst(libPath + "/extensions.jar");
		params.getClassPath().addFirst(libPath + "/bootstrap.jar");
		params.getClassPath().addFirst(libPath + "/jna.jar");
	}

	public JavaParameters getJavaParameters()
	{
		return myJavaParameters;
	}
}
