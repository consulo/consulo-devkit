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

package org.jetbrains.idea.devkit.run;

import java.io.File;

import org.jetbrains.annotations.NotNull;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.CommandLineState;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.configurations.ParametersList;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.application.PathManager;
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
	private JavaParameters myJavaParameters;

	public ConsuloSandboxRunState(
			ExecutionEnvironment environment,
			Sdk javaSdk,
			Sdk consuloSdk,
			Artifact artifact)
	{
		super(environment);
		myJavaParameters = createJavaParameters(environment, javaSdk, consuloSdk, artifact);
	}

	@NotNull
	@Override
	protected ProcessHandler startProcess() throws ExecutionException
	{
		return myJavaParameters.createOSProcessHandler();
	}

	private static JavaParameters createJavaParameters(
			ExecutionEnvironment env,
			Sdk javaSdk,
			Sdk consuloSdk,
			Artifact artifact)
	{
		PluginRunConfiguration profile = (PluginRunConfiguration) env.getRunProfile();
		final String dataPath = profile.getSandboxPath();

		final JavaParameters params = new JavaParameters();

		ParametersList vm = params.getVMParametersList();

		vm.addParametersString(profile.VM_PARAMETERS);
		params.getProgramParametersList().addParametersString(profile.PROGRAM_PARAMETERS);

		vm.defineProperty(PathManager.PROPERTY_CONFIG_PATH, dataPath + "/config");
		vm.defineProperty(PathManager.PROPERTY_SYSTEM_PATH, dataPath + "/system");
		vm.defineProperty(PathManager.PROPERTY_PLUGINS_PATH, artifact.getOutputPath());

		File logFile = new File(dataPath, PluginRunConfiguration.LOG_FILE);
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
		params.setWorkingDirectory(consuloSdk.getHomePath() + File.separator + "bin" + File.separator);

		params.setJdk(javaSdk);

		addConsuloLibs(consuloSdk.getHomePath(), params);

		params.setMainClass("com.intellij.idea.Main");
		return params;
	}

	public static void addConsuloLibs(String consuloHomePath, JavaParameters params)
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
