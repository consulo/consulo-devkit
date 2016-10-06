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
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.execution.ConfigurationUtil;
import com.intellij.execution.DefaultExecutionResult;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.Executor;
import com.intellij.execution.JavaExecutionUtil;
import com.intellij.execution.junit.JUnitUtil;
import com.intellij.execution.junit.TestClassFilter;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.testframework.SourceScope;
import com.intellij.execution.testframework.TestConsoleProperties;
import com.intellij.execution.testframework.sm.SMTestRunnerConnectionUtil;
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties;
import com.intellij.execution.testframework.ui.BaseTestsOutputConsoleView;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;
import consulo.annotations.RequiredDispatchThread;
import consulo.application.ApplicationProperties;
import consulo.compiler.ModuleCompilerPathsManager;
import consulo.roots.impl.TestContentFolderTypeProvider;

/**
 * @author VISTALL
 * @since 12.04.2015
 */
public class ConsuloTestRunState extends ConsuloSandboxRunState
{
	@RequiredDispatchThread
	public ConsuloTestRunState(@NotNull ExecutionEnvironment environment, @NotNull Sdk javaSdk, @NotNull String consuloSdkHome, @Nullable Artifact artifact) throws ExecutionException
	{
		super(environment, javaSdk, consuloSdkHome, artifact);

		myJavaParameters.getVMParametersList().defineProperty(ApplicationProperties.CONSULO_IN_UNIT_TEST, "true");
		myJavaParameters.getVMParametersList().defineProperty("idea.test.project.dir", environment.getProject().getBasePath());

		StringBuilder builder = new StringBuilder();
		for(Module module : ModuleManager.getInstance(environment.getProject()).getModules())
		{
			VirtualFile compilerOutput = ModuleCompilerPathsManager.getInstance(module).getCompilerOutput(TestContentFolderTypeProvider.getInstance());
			if(compilerOutput != null)
			{
				builder.append(FileUtil.toSystemIndependentName(compilerOutput.getPath())).append(";");
			}
		}
		myJavaParameters.getVMParametersList().defineProperty("consulo.test.classpath", builder.toString());
	}

	@NotNull
	@Override
	public String getSandboxPath(ConsuloRunConfigurationBase configuration) throws ExecutionException
	{
		try
		{
			File file = FileUtil.createTempDirectory("consulo", "sandbox", true);
			return FileUtil.toSystemDependentName(file.getPath());
		}
		catch(IOException e)
		{
			throw new ExecutionException(e);
		}
	}

	@NotNull
	@Override
	@RequiredDispatchThread
	public ExecutionResult execute(@NotNull Executor executor, @NotNull ProgramRunner runner) throws ExecutionException
	{
		ConsuloTestRunConfiguration runProfile = (ConsuloTestRunConfiguration) myEnvironment.getRunProfile();
		TestConsoleProperties testConsoleProperties = new SMTRunnerConsoleProperties(runProfile, "ConsuloUnit", executor);

		testConsoleProperties.setIfUndefined(TestConsoleProperties.HIDE_PASSED_TESTS, false);

		final BaseTestsOutputConsoleView smtConsoleView = SMTestRunnerConnectionUtil.createConsole("ConsuloUnit", testConsoleProperties);

		try
		{
			File tempFile = FileUtil.createTempFile("consulo", "test_classes.txt");
			Module[] modules = runProfile.getModules();

			StringBuilder data = new StringBuilder();
			switch(runProfile.getTargetType())
			{
				case CLASS:
					data.append(runProfile.CLASS_NAME);
					break;
				case PACKAGE:
					GlobalSearchScope globalSearchScope = GlobalSearchScope.EMPTY_SCOPE;
					for(Module module : modules)
					{
						globalSearchScope = globalSearchScope.union(module.getModuleWithDependenciesAndLibrariesScope(true));
					}
					TestClassFilter testClassFilter = new TestClassFilter(JUnitUtil.getTestCaseClass(SourceScope.modules(modules)), globalSearchScope);
					Set<PsiClass> psiClasses = new LinkedHashSet<PsiClass>();
					ConfigurationUtil.findAllTestClasses(testClassFilter, psiClasses);
					String packageName = StringUtil.notNullize(runProfile.PACKAGE_NAME);
					for(PsiClass psiClass : psiClasses)
					{
						String qualifiedName = psiClass.getQualifiedName();
						assert qualifiedName != null;
						if(qualifiedName.startsWith(packageName))
						{
							data.append(JavaExecutionUtil.getRuntimeQualifiedName(psiClass)).append("\n");
						}
					}
					break;
			}
			FileUtil.writeToFile(tempFile, data.toString());

			myJavaParameters.getProgramParametersList().add(StringUtil.QUOTER.fun(FileUtil.toSystemIndependentName(tempFile.getAbsolutePath())));

			ProcessHandler osProcessHandler = startProcess();

			smtConsoleView.attachToProcess(osProcessHandler);

			return new DefaultExecutionResult(smtConsoleView, osProcessHandler);
		}
		catch(IOException e)
		{
			throw new ExecutionException(e);
		}
	}
}
