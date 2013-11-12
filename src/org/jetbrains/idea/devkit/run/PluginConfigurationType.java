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

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import javax.swing.Icon;

import org.consulo.java.module.extension.JavaModuleExtension;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.devkit.DevKitBundle;
import org.jetbrains.idea.devkit.module.extension.PluginModuleExtension;
import org.mustbe.consulo.module.extension.ModuleExtensionHelper;
import com.intellij.diagnostic.VMOptions;
import com.intellij.execution.configuration.ConfigurationFactoryEx;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactManager;
import com.intellij.packaging.impl.artifacts.ArtifactUtil;
import com.intellij.packaging.impl.artifacts.PlainArtifactType;

public class PluginConfigurationType implements ConfigurationType
{
	private final ConfigurationFactory myFactory;
	private String myVmParameters;

	PluginConfigurationType()
	{
		myFactory = new ConfigurationFactoryEx(this)
		{
			@Override
			public RunConfiguration createTemplateConfiguration(Project project)
			{
				final PluginRunConfiguration runConfiguration = new PluginRunConfiguration(project, this, "");

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
			public boolean isApplicable(@NotNull Project project)
			{
				return ModuleExtensionHelper.getInstance(project).hasModuleExtension(PluginModuleExtension.class);
			}

			@Override
			public void onNewConfigurationCreated(@NotNull RunConfiguration configuration)
			{
				PluginRunConfiguration runConfiguration = (PluginRunConfiguration) configuration;

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
		};
	}

	private static Pair<Module, Artifact> findArtifact(Project project)
	{
		ArtifactManager artifactManager = ArtifactManager.getInstance(project);
		if(artifactManager == null)
		{
			return null;
		}
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
						return new Pair<Module, Artifact>(module, artifact);
					}
				}
			}
		}
		return null;
	}

	@Override
	public String getDisplayName()
	{
		return DevKitBundle.message("run.configuration.title");
	}

	@Override
	public String getConfigurationTypeDescription()
	{
		return DevKitBundle.message("run.configuration.type.description");
	}

	@Override
	public Icon getIcon()
	{
		return AllIcons.Nodes.Plugin;
	}

	@Override
	public ConfigurationFactory[] getConfigurationFactories()
	{
		return new ConfigurationFactory[]{myFactory};
	}

	@Override
	@NotNull
	public String getId()
	{
		return "#org.jetbrains.idea.devkit.run.PluginConfigurationType";
	}

	@NotNull
	private String getVmParameters()
	{
		if(myVmParameters == null)
		{
			String vmOptions;
			try
			{
				vmOptions = FileUtil.loadFile(new File(PathManager.getBinPath(), "idea.plugins.vmoptions")).replaceAll("\\s+", " ");
			}
			catch(IOException e)
			{
				vmOptions = VMOptions.read();
			}
			myVmParameters = vmOptions != null ? vmOptions.trim() : "";
		}

		return myVmParameters;
	}
}
