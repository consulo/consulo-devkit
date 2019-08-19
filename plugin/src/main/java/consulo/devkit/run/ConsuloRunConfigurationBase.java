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
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.LocatableConfigurationBase;
import com.intellij.execution.configurations.LogFileOptions;
import com.intellij.execution.configurations.PredefinedLogFile;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.util.*;
import com.intellij.openapi.util.text.StringUtil;
import consulo.annotations.RequiredReadAction;
import consulo.bundle.SdkPointerManager;
import consulo.bundle.SdkUtil;
import consulo.java.debugger.impl.GenericDebugRunnerConfiguration;
import consulo.util.pointers.NamedPointer;
import consulo.util.pointers.NamedPointerManager;
import org.jdom.Element;
import org.jetbrains.idea.devkit.DevKitBundle;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 12.04.2015
 */
public abstract class ConsuloRunConfigurationBase extends LocatableConfigurationBase implements GenericDebugRunnerConfiguration
{
	public static final PredefinedLogFile CONSULO_LOG = new PredefinedLogFile("CONSULO_LOG", true);

	public static final String LOG_FILE = "/system/log/consulo.log";
	private static final String JAVA_SDK = "java-sdk";
	private static final String CONSULO_SDK = "consulo-sdk";

	public String VM_PARAMETERS;
	public String PROGRAM_PARAMETERS;
	public String PLUGINS_HOME_PATH;
	protected NamedPointer<Sdk> myJavaSdkPointer;
	protected NamedPointer<Sdk> myConsuloSdkPointer;
	public boolean USE_ALT_CONSULO_SDK;
	public String ALT_CONSULO_SDK_PATH;

	public ConsuloRunConfigurationBase(Project project, ConfigurationFactory factory, String name)
	{
		super(project, factory, name);
	}

	@Nonnull
	public String getSandboxPath()
	{
		return getProject().getBasePath() + "/" + Project.DIRECTORY_STORE_FOLDER + "/sandbox";
	}

	@Nullable
	@Override
	public LogFileOptions getOptionsForPredefinedLogFile(PredefinedLogFile predefinedLogFile)
	{
		if(CONSULO_LOG.equals(predefinedLogFile))
		{
			String sandboxPath = getSandboxPath();
			return new LogFileOptions("consulo.log", sandboxPath + LOG_FILE, true, false, true);
		}
		else
		{
			return null;
		}
	}

	public String getConsuloSdkHome()
	{
		if(USE_ALT_CONSULO_SDK)
		{
			if(StringUtil.isEmpty(ALT_CONSULO_SDK_PATH))
			{
				return null;
			}
			return ALT_CONSULO_SDK_PATH;
		}
		Sdk sdk = myConsuloSdkPointer == null ? null : myConsuloSdkPointer.get();
		return sdk == null ? null : sdk.getHomePath();
	}

	@Nullable
	@Override
	public final ConsuloSandboxRunState getState(@Nonnull Executor executor, @Nonnull ExecutionEnvironment env) throws ExecutionException
	{
		final Sdk javaSdk = myJavaSdkPointer == null ? null : myJavaSdkPointer.get();
		if(javaSdk == null)
		{
			throw new ExecutionException(DevKitBundle.message("run.configuration.no.java.sdk"));
		}

		final String consuloSdkHome = getConsuloSdkHome();
		if(consuloSdkHome == null)
		{
			throw new ExecutionException(DevKitBundle.message("run.configuration.no.consulo.sdk"));
		}

		return createState(executor, env, javaSdk, consuloSdkHome, PLUGINS_HOME_PATH);
	}

	@Nonnull
	public abstract ConsuloSandboxRunState createState(Executor executor, @Nonnull ExecutionEnvironment env,
			@Nonnull Sdk javaSdk,
			@Nonnull String consuloHome,
			@Nullable String pluginsHomePath) throws ExecutionException;

	@Override
	public void readExternal(Element element) throws InvalidDataException
	{
		DefaultJDOMExternalizer.readExternal(this, element);

		myJavaSdkPointer = PluginRunXmlConfigurationUtil.readPointer(JAVA_SDK, element, new NotNullFactory<NamedPointerManager<Sdk>>()
		{
			@Nonnull
			@Override
			public NamedPointerManager<Sdk> create()
			{
				return ServiceManager.getService(SdkPointerManager.class);
			}
		});

		myConsuloSdkPointer = PluginRunXmlConfigurationUtil.readPointer(CONSULO_SDK, element, new NotNullFactory<NamedPointerManager<Sdk>>()
		{
			@Nonnull
			@Override
			public NamedPointerManager<Sdk> create()
			{
				return ServiceManager.getService(SdkPointerManager.class);
			}
		});

		super.readExternal(element);
	}

	@Override
	public void writeExternal(Element element) throws WriteExternalException
	{
		DefaultJDOMExternalizer.writeExternal(this, element);

		PluginRunXmlConfigurationUtil.writePointer(JAVA_SDK, element, myJavaSdkPointer);
		PluginRunXmlConfigurationUtil.writePointer(CONSULO_SDK, element, myConsuloSdkPointer);

		super.writeExternal(element);
	}

	@Override
	public boolean isGeneratedName()
	{
		return Comparing.equal(getName(), suggestedName());
	}


	@Nullable
	public String getJavaSdkName()
	{
		return myJavaSdkPointer == null ? null : myJavaSdkPointer.getName();
	}

	public void setJavaSdkName(@Nullable String name)
	{
		myJavaSdkPointer = name == null ? null : SdkUtil.createPointer(name);
	}

	@Nullable
	public String getConsuloSdkName()
	{
		return myConsuloSdkPointer == null ? null : myConsuloSdkPointer.getName();
	}

	public void setConsuloSdkName(@Nullable String name)
	{
		myConsuloSdkPointer = name == null ? null : SdkUtil.createPointer(name);
	}

	@Nonnull
	@Override
	@RequiredReadAction
	public Module[] getModules()
	{
		if(USE_ALT_CONSULO_SDK)
		{
			return ModuleManager.getInstance(getProject()).getModules();
		}
		return Module.EMPTY_ARRAY;
	}
}
