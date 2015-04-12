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
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.devkit.run.PluginRunConfigurationEditor;
import com.intellij.diagnostic.logging.LogConfigurationPanel;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.options.SettingsEditorGroup;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.packaging.artifacts.Artifact;

/**
 * @author VISTALL
 * @since 12.04.2015
 */
public class ConsuloTestRunConfiguration extends ConsuloRunConfigurationBase
{
	public ConsuloTestRunConfiguration(Project project, ConfigurationFactory factory, String name)
	{
		super(project, factory, name);
	}

	@NotNull
	@Override
	public SettingsEditor<? extends RunConfiguration> getConfigurationEditor()
	{
		SettingsEditorGroup settingsEditorGroup = new SettingsEditorGroup<RunConfiguration>();
		settingsEditorGroup.addEditor("General", new PluginRunConfigurationEditor(getProject()));
		settingsEditorGroup.addEditor("Log", new LogConfigurationPanel<ConsuloRunConfigurationBase>());
		return settingsEditorGroup;
	}

	@NotNull
	@Override
	public ConsuloSandboxRunState createState(Executor executor, @NotNull ExecutionEnvironment env,
			@NotNull Sdk javaSdk,
			@NotNull String consuloHome,
			@Nullable Artifact artifact)
	{
		return new ConsuloTestRunState(env, javaSdk, consuloHome, artifact);
	}
}

