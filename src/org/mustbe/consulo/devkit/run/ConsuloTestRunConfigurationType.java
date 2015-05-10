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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.devkit.DevKitBundle;
import org.jetbrains.idea.devkit.module.extension.PluginModuleExtension;
import org.jetbrains.idea.devkit.run.PluginConfigurationType;
import org.mustbe.consulo.devkit.ConsuloSandboxIcons;
import org.mustbe.consulo.java.module.extension.JavaModuleExtension;
import org.mustbe.consulo.module.extension.ModuleExtensionHelper;
import com.intellij.execution.configuration.ConfigurationFactoryEx;
import com.intellij.execution.configurations.ConfigurationTypeBase;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.icons.AllIcons;
import com.intellij.ide.IconDescriptor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.util.Pair;
import com.intellij.packaging.artifacts.Artifact;

/**
 * @author VISTALL
 * @since 12.04.2015
 */
public class ConsuloTestRunConfigurationType extends ConfigurationTypeBase
{
	public ConsuloTestRunConfigurationType()
	{
		super("#ConsuloTestRunConfigurationType", DevKitBundle.message("test.run.configuration.title"), null,
				new IconDescriptor(ConsuloSandboxIcons.Icon16_Sandbox).addLayerIcon(AllIcons.Nodes.JunitTestMark).toIcon());

		addFactory(new ConfigurationFactoryEx(this)
		{
			@Override
			public RunConfiguration createTemplateConfiguration(Project project)
			{
				return new ConsuloTestRunConfiguration(project, this, "Unnamed");
			}

			@Override
			public boolean isApplicable(@NotNull Project project)
			{
				return ModuleExtensionHelper.getInstance(project).hasModuleExtension(PluginModuleExtension.class);
			}

			@Override
			public void onNewConfigurationCreated(@NotNull RunConfiguration configuration)
			{
				ConsuloTestRunConfiguration runConfiguration = (ConsuloTestRunConfiguration) configuration;

				runConfiguration.addPredefinedLogFile(ConsuloRunConfiguration.IDEA_LOG);

				Pair<Module, Artifact> pair = PluginConfigurationType.findArtifact(configuration.getProject());
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
}
