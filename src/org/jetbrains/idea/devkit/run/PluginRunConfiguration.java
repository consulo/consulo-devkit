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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mustbe.consulo.devkit.run.ConsuloRunConfigurationBase;
import org.mustbe.consulo.devkit.run.ConsuloSandboxRunState;
import com.intellij.debugger.impl.GenericDebugRunnerConfiguration;
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

public class PluginRunConfiguration extends ConsuloRunConfigurationBase implements GenericDebugRunnerConfiguration
{
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

	@NotNull
	@Override
	public ConsuloSandboxRunState createState(Executor executor, @NotNull ExecutionEnvironment env,
			@NotNull Sdk javaSdk,
			@NotNull String consuloHome,
			@Nullable Artifact artifact)
	{
		return new ConsuloSandboxRunState(env, javaSdk, consuloHome, artifact);
	}
}
