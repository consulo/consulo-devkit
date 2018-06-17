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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.jetbrains.idea.devkit.DevKitBundle;
import com.intellij.diagnostic.VMOptions;
import com.intellij.execution.configuration.ConfigurationFactoryEx;
import com.intellij.execution.configurations.ConfigurationTypeBase;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.util.Pair;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactManager;
import com.intellij.packaging.impl.artifacts.ArtifactUtil;
import com.intellij.packaging.impl.artifacts.PlainArtifactType;
import consulo.devkit.ConsuloSandboxIcons;
import consulo.devkit.module.extension.PluginModuleExtension;
import consulo.devkit.run.ConsuloRunConfiguration;
import consulo.java.module.extension.JavaModuleExtension;
import consulo.module.extension.ModuleExtensionHelper;

public class PluginConfigurationType extends ConfigurationTypeBase
{
	@Nonnull
	public static PluginConfigurationType getInstance()
	{
		return CONFIGURATION_TYPE_EP.findExtension(PluginConfigurationType.class);
	}

	private String myVmParameters;

	public PluginConfigurationType()
	{
		super("#org.jetbrains.idea.devkit.run.PluginConfigurationType", DevKitBundle.message("run.configuration.title"), DevKitBundle.message("run.configuration.type.description"), ConsuloSandboxIcons.Icon16_Sandbox);
		addFactory(new ConfigurationFactoryEx(this)
		{
			@Override
			public RunConfiguration createTemplateConfiguration(Project project)
			{
				final ConsuloRunConfiguration runConfiguration = new ConsuloRunConfiguration(project, this, getDisplayName());

				if(runConfiguration.VM_PARAMETERS == null)
				{
					runConfiguration.VM_PARAMETERS = getVmParameters();
				}
				else
				{
					runConfiguration.VM_PARAMETERS += getVmParameters();
				}
				return runConfiguration;
			}

			@Override
			public boolean isApplicable(@Nonnull Project project)
			{
				return ModuleExtensionHelper.getInstance(project).hasModuleExtension(PluginModuleExtension.class);
			}

			@Override
			public void onNewConfigurationCreated(@Nonnull RunConfiguration configuration)
			{
				ConsuloRunConfiguration runConfiguration = (ConsuloRunConfiguration) configuration;

				runConfiguration.addPredefinedLogFile(ConsuloRunConfiguration.CONSULO_LOG);

				Pair<Module, Artifact> pair = findArtifact(configuration.getProject());
				if(pair != null)
				{
					Sdk sdk = ModuleUtilCore.getSdk(pair.getFirst(), JavaModuleExtension.class);
					if(sdk != null)
					{
						runConfiguration.setJavaSdkName(sdk.getName());
					}

					sdk = ModuleUtilCore.getSdk(pair.getFirst(), PluginModuleExtension.class);
					if(sdk != null)
					{
						runConfiguration.setConsuloSdkName(sdk.getName());
					}

					runConfiguration.setArtifactName(pair.second.getName());
				}
			}
		});
	}

	@Nullable
	public static Pair<Module, Artifact> findArtifact(Project project)
	{
		ArtifactManager artifactManager = ArtifactManager.getInstance(project);

		for(Artifact artifact : artifactManager.getArtifacts())
		{
			if(artifact.getArtifactType() == PlainArtifactType.getInstance())
			{
				Set<Module> modulesIncludedInArtifacts = ArtifactUtil.getModulesIncludedInArtifacts(Collections.singletonList(artifact), project);
				if(modulesIncludedInArtifacts.isEmpty())
				{
					continue;
				}

				for(Module module : modulesIncludedInArtifacts)
				{
					PluginModuleExtension extension = ModuleUtilCore.getExtension(module, PluginModuleExtension.class);
					if(extension != null)
					{
						return Pair.create(module, artifact);
					}
				}
			}
		}
		return null;
	}

	@Nonnull
	private String getVmParameters()
	{
		if(myVmParameters == null)
		{
			String vmOptions = VMOptions.read();
			myVmParameters = vmOptions != null ? vmOptions.trim() : "";
		}

		return myVmParameters;
	}
}