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

import com.intellij.execution.ExecutionException;
import com.intellij.execution.RunConfigurationExtension;
import com.intellij.execution.configurations.CommandLineState;
import com.intellij.execution.configurations.ParametersList;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.projectRoots.OwnJdkVersionDetector;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ObjectUtil;
import consulo.application.ApplicationProperties;
import consulo.java.execution.configurations.OwnJavaParameters;
import consulo.logging.Logger;
import org.jetbrains.idea.devkit.sdk.ConsuloSdkType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipFile;

/**
 * @author VISTALL
 * @since 29.05.14
 */
public class ConsuloSandboxRunState extends CommandLineState
{
	protected ExecutionEnvironment myEnvironment;
	@Nonnull
	private final Sdk myJavaSdk;
	@Nonnull
	private final String myConsuloSdkHome;
	@Nullable
	private final String myPluginsHomePath;

	private final List<String> myAdditionalVMParameters = new ArrayList<>();

	public ConsuloSandboxRunState(@Nonnull ExecutionEnvironment environment, @Nonnull Sdk javaSdk, @Nonnull String consuloSdkHome, @Nullable String pluginsHomePath) throws ExecutionException
	{
		super(environment);
		myEnvironment = environment;
		myJavaSdk = javaSdk;
		myConsuloSdkHome = consuloSdkHome;
		myPluginsHomePath = pluginsHomePath;
	}

	public void addAdditionalVMParameter(String param)
	{
		myAdditionalVMParameters.add(param);
	}

	@Nonnull
	public Sdk getJavaSdk()
	{
		return myJavaSdk;
	}

	@Nonnull
	@Override
	protected ProcessHandler startProcess() throws ExecutionException
	{
		return createJavaParameters(getEnvironment(), myJavaSdk, myConsuloSdkHome, myPluginsHomePath).createOSProcessHandler();
	}

	@Nonnull
	public String getSandboxPath(ConsuloRunConfigurationBase configuration) throws ExecutionException
	{
		return configuration.getSandboxPath();
	}

	private OwnJavaParameters createJavaParameters(@Nonnull ExecutionEnvironment env, @Nonnull Sdk javaSdk, @Nonnull String consuloSdkHome, @Nullable String pluginsHomePath) throws ExecutionException
	{
		ConsuloRunConfigurationBase profile = (ConsuloRunConfigurationBase) env.getRunProfile();
		final String dataPath = getSandboxPath(profile);

		final OwnJavaParameters params = new OwnJavaParameters();

		ParametersList vm = params.getVMParametersList();

		vm.addParametersString(profile.VM_PARAMETERS);
		params.getProgramParametersList().addParametersString(profile.PROGRAM_PARAMETERS);

		String selectedBuildPath = ObjectUtil.notNull(ConsuloSdkType.selectBuild(consuloSdkHome), consuloSdkHome);

		vm.defineProperty("idea.config.path", dataPath + "/config");
		vm.defineProperty("idea.system.path", dataPath + "/system");
		vm.defineProperty("consulo.home.path", selectedBuildPath);
		// define plugin installation to default path
		String installPluginPath = dataPath + "/config/plugins";
		vm.defineProperty(ApplicationProperties.CONSULO_INSTALL_PLUGINS_PATH, installPluginPath);

		List<String> pluginPaths = new ArrayList<>();
		pluginPaths.add(installPluginPath);
		if(!StringUtil.isEmptyOrSpaces(pluginsHomePath))
		{
			pluginPaths.add(pluginsHomePath);
		}

		VirtualFile baseDir = env.getProject().getBaseDir();
		assert baseDir != null;

		vm.defineProperty(ApplicationProperties.CONSULO_PLUGINS_PATHS, String.join(File.pathSeparator, pluginPaths));

		File logFile = new File(dataPath, ConsuloRunConfigurationBase.LOG_FILE);
		FileUtil.createIfDoesntExist(logFile);
		// deprecated
		vm.defineProperty("idea.log.path", logFile.getParent());
		vm.defineProperty("consulo.log.path", logFile.getParent());

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

		OwnJdkVersionDetector.JdkVersionInfo versionInfo = OwnJdkVersionDetector.getInstance().detectJdkVersionInfo(javaSdk.getHomePath());

		boolean isJava9 = versionInfo != null && versionInfo.version.isAtLeast(9) && profile.ENABLED_JAVA9_MODULES;

		ConsuloPlatform platform = addBootLibraries(selectedBuildPath, params, isJava9);

		if(!params.getModulePath().isEmpty())
		{
			params.setModuleName(platform.getModuleName());
		}

		params.setMainClass(platform.getMainClass());

		for(String additionalVMParameter : myAdditionalVMParameters)
		{
			params.getVMParametersList().addParametersString(additionalVMParameter);
		}

		for(RunConfigurationExtension ext : RunConfigurationExtension.EP_NAME.getExtensionList())
		{
			ext.updateJavaParameters(profile, params, getRunnerSettings());
		}
		return params;
	}

	protected ConsuloPlatform addBootLibraries(@Nonnull String consuloHomePath, @Nonnull OwnJavaParameters params, boolean isJava9)
	{
		ConsuloPlatform platform = ConsuloPlatform.DESKTOP_AWT;

		File bootDirectory = new File(consuloHomePath + "/boot");
		if(bootDirectory.exists())
		{
			File[] files = bootDirectory.listFiles();
			for(File file : files)
			{
				if(FileUtil.isJarOrZip(file))
				{
					boolean modular = false;
					if(isJava9)
					{
						try (ZipFile zipFile = new ZipFile(file, ZipFile.OPEN_READ))
						{
							modular = zipFile.getEntry("module-info.class") != null;
						}
						catch(IOException e)
						{
							Logger.getInstance(ConsuloSandboxRunState.class).error(e);
						}
					}

					if(modular)
					{
						params.getModulePath().addFirst(file.getPath());
					}
					else
					{
						params.getClassPath().addFirst(file.getPath());
					}

					if(file.getName().contains("-web-"))
					{
						platform = ConsuloPlatform.WEB;
					}
					else if(file.getName().contains("desktop-swt"))
					{
						platform = ConsuloPlatform.DESKTOP_SWT;
					}
				}
			}

			File spiDir = new File(bootDirectory, "spi");
			if(spiDir.exists())
			{
				for(File file : spiDir.listFiles())
				{
					if(FileUtil.isJarOrZip(file))
					{
						if(isJava9)
						{
							params.getModulePath().addFirst(file.getPath());
						}
						else
						{
							params.getClassPath().addFirst(file.getPath());
						}
					}
				}
			}
		}
		return platform;
	}
}
