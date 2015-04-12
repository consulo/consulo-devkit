/*
 * Copyright 2013-2015 must-be.org
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
import java.io.IOException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.execution.DefaultExecutionResult;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.testframework.TestConsoleProperties;
import com.intellij.execution.testframework.sm.SMTestRunnerConnectionUtil;
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties;
import com.intellij.execution.testframework.ui.BaseTestsOutputConsoleView;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.ide.plugins.cl.PluginClassLoader;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.packaging.artifacts.Artifact;

/**
 * @author VISTALL
 * @since 12.04.2015
 */
public class ConsuloTestRunState extends ConsuloSandboxRunState
{
	public ConsuloTestRunState(@NotNull ExecutionEnvironment environment,
			@NotNull Sdk javaSdk,
			@NotNull String consuloSdkHome,
			@Nullable Artifact artifact)
	{
		super(environment, javaSdk, consuloSdkHome, artifact);

		myJavaParameters.getVMParametersList().defineProperty("idea.test.project.dir", environment.getProject().getBasePath());
	}

	@Override
	protected void addConsuloLibs(@NotNull String consuloHomePath, @NotNull JavaParameters params)
	{
		super.addConsuloLibs(consuloHomePath, params);

		PluginClassLoader classLoader = (PluginClassLoader) ConsuloTestRunState.class.getClassLoader();
		IdeaPluginDescriptor plugin = PluginManager.getPlugin(classLoader.getPluginId());
		assert plugin != null;
		params.getClassPath().add(new File(plugin.getPath(), "consulo-test-starter.jar"));
	}

	@NotNull
	@Override
	public ExecutionResult execute(@NotNull Executor executor, @NotNull ProgramRunner runner) throws ExecutionException
	{
		ConsuloTestRunConfiguration runProfile = (ConsuloTestRunConfiguration) myEnvironment.getRunProfile();
		TestConsoleProperties testConsoleProperties = new SMTRunnerConsoleProperties(runProfile,
				"ConsuloUnit", executor);

		testConsoleProperties.setIfUndefined(TestConsoleProperties.HIDE_PASSED_TESTS, false);

		final BaseTestsOutputConsoleView smtConsoleView = SMTestRunnerConnectionUtil.createConsoleWithCustomLocator("ConsuloUnit",
				testConsoleProperties, myEnvironment, null);

		try
		{
			File file = FileUtil.createTempFile("consulo", "test_classes.txt");
			FileUtil.writeToFile(file, runProfile.PLUGIN_ID + "," + runProfile.CLASS_NAME);

			myJavaParameters.getProgramParametersList().add(StringUtil.QUOTER.fun(FileUtil.toSystemIndependentName(file.getAbsolutePath())));

			ProcessHandler osProcessHandler = startProcess();

			smtConsoleView.attachToProcess(osProcessHandler);

			return new DefaultExecutionResult(smtConsoleView, osProcessHandler);
		}
		catch(IOException e)
		{
			throw new ExecutionException(e);
		}
	}

	@Override
	@NotNull
	public String getMainClass()
	{
		return "org.mustbe.consulo.devkit.test.Main";
	}
}
