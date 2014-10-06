/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

import java.util.Collections;
import java.util.Set;

import org.consulo.sdk.SdkPointerManager;
import org.consulo.util.pointers.NamedPointer;
import org.consulo.util.pointers.NamedPointerManager;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.devkit.DevKitBundle;
import org.mustbe.consulo.sdk.SdkUtil;
import com.intellij.diagnostic.logging.LogConfigurationPanel;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.LogFileOptions;
import com.intellij.execution.configurations.ModuleRunProfile;
import com.intellij.execution.configurations.PredefinedLogFile;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.options.SettingsEditorGroup;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.util.DefaultJDOMExternalizer;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.NotNullFactory;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactPointerUtil;
import com.intellij.packaging.impl.artifacts.ArtifactUtil;

public class PluginRunConfiguration extends RunConfigurationBase implements ModuleRunProfile
{
	public static final PredefinedLogFile IDEA_LOG = new PredefinedLogFile("IDEA_LOG", true);

	public static final String LOG_FILE = "/system/log/idea.log";
	private static final String JAVA_SDK = "java-sdk";
	private static final String CONSULO_SDK = "consulo-sdk";
	private static final String ARTIFACT = "artifact";

	public String VM_PARAMETERS;
	public String PROGRAM_PARAMETERS;
	private NamedPointer<Sdk> myJavaSdkPointer;
	private NamedPointer<Sdk> myConsuloSdkPointer;
	private NamedPointer<Artifact> myArtifactPointer;
	public boolean USE_ALT_CONSULO_SDK;
	public String ALT_CONSULO_SDK_PATH;

	protected PluginRunConfiguration(Project project, ConfigurationFactory factory, String name)
	{
		super(project, factory, name);
	}

	@NotNull
	@Override
	@SuppressWarnings("unchecked")
	public SettingsEditor<? extends RunConfiguration> getConfigurationEditor()
	{
		SettingsEditorGroup settingsEditorGroup = new SettingsEditorGroup<RunConfiguration>();
		settingsEditorGroup.addEditor("General", new PluginRunConfigurationEditor(getProject()));
		settingsEditorGroup.addEditor("Log", new LogConfigurationPanel<PluginRunConfiguration>());
		return settingsEditorGroup;
	}

	@Nullable
	@Override
	public LogFileOptions getOptionsForPredefinedLogFile(PredefinedLogFile predefinedLogFile)
	{
		if(IDEA_LOG.equals(predefinedLogFile))
		{
			String sandboxPath = getSandboxPath();
			return new LogFileOptions("idea.log", sandboxPath + LOG_FILE, true, false, true);
		}
		else
		{
			return null;
		}
	}

	public String getSandboxPath()
	{
		return getProject().getBasePath() + "/" + Project.DIRECTORY_STORE_FOLDER + "/sandbox";
	}

	@Override
	public RunProfileState getState(@NotNull final Executor executor, @NotNull final ExecutionEnvironment env) throws ExecutionException
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

		final Artifact artifact = myArtifactPointer == null ? null : myArtifactPointer.get();
		return new ConsuloSandboxRunState(env, javaSdk, consuloSdkHome, artifact);
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

	@Override
	public void checkConfiguration() throws RuntimeConfigurationException
	{

	}

	@Override
	public void readExternal(Element element) throws InvalidDataException
	{
		DefaultJDOMExternalizer.readExternal(this, element);

		myJavaSdkPointer = PluginRunXmlConfigurationUtil.readPointer(JAVA_SDK, element, new NotNullFactory<NamedPointerManager<Sdk>>()
		{
			@NotNull
			@Override
			public NamedPointerManager<Sdk> create()
			{
				return ServiceManager.getService(SdkPointerManager.class);
			}
		});

		myConsuloSdkPointer = PluginRunXmlConfigurationUtil.readPointer(CONSULO_SDK, element, new NotNullFactory<NamedPointerManager<Sdk>>()
		{
			@NotNull
			@Override
			public NamedPointerManager<Sdk> create()
			{
				return ServiceManager.getService(SdkPointerManager.class);
			}
		});

		myArtifactPointer = PluginRunXmlConfigurationUtil.readPointer(ARTIFACT, element, new NotNullFactory<NamedPointerManager<Artifact>>()
		{
			@NotNull
			@Override
			public NamedPointerManager<Artifact> create()
			{
				return ArtifactPointerUtil.getPointerManager(getProject());
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
		PluginRunXmlConfigurationUtil.writePointer(ARTIFACT, element, myArtifactPointer);

		super.writeExternal(element);
	}

	@Nullable
	public String getArtifactName()
	{
		return myArtifactPointer == null ? null : myArtifactPointer.getName();
	}

	public void setArtifactName(@Nullable String name)
	{
		myArtifactPointer = name == null ? null : ArtifactPointerUtil.getPointerManager(getProject()).create(name);
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

	@NotNull
	@Override
	public Module[] getModules()
	{
		Artifact artifact = myArtifactPointer == null ? null : myArtifactPointer.get();
		if(artifact == null)
		{
			return Module.EMPTY_ARRAY;
		}
		final Set<Module> modules = ArtifactUtil.getModulesIncludedInArtifacts(Collections.singletonList(artifact), getProject());

		return modules.isEmpty() ? Module.EMPTY_ARRAY : modules.toArray(new Module[modules.size()]);
	}
}
