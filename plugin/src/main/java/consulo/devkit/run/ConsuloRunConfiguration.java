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

import com.intellij.java.coverage.CoverageConfigurable;
import consulo.content.bundle.Sdk;
import consulo.execution.configuration.ConfigurationFactory;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.configuration.log.ui.LogConfigurationPanel;
import consulo.execution.configuration.ui.SettingsEditor;
import consulo.execution.configuration.ui.SettingsEditorGroup;
import consulo.execution.executor.Executor;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.java.debugger.impl.GenericDebugRunnerConfiguration;
import consulo.process.ExecutionException;
import consulo.project.Project;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ConsuloRunConfiguration extends ConsuloRunConfigurationBase implements GenericDebugRunnerConfiguration {
  public ConsuloRunConfiguration(Project project, ConfigurationFactory factory, String name) {
    super(project, factory, name);
  }

  @Nonnull
  @Override
  @SuppressWarnings("unchecked")
  public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
    SettingsEditorGroup settingsEditorGroup = new SettingsEditorGroup<RunConfiguration>();
    settingsEditorGroup.addEditor("General", new ConsuloRunConfigurationEditor(getProject()));
    settingsEditorGroup.addEditor("Coverage", new CoverageConfigurable(this));
    settingsEditorGroup.addEditor("Log", new LogConfigurationPanel<ConsuloRunConfiguration>());
    return settingsEditorGroup;
  }

  @Nonnull
  @Override
  public ConsuloSandboxRunState createState(Executor executor, @Nonnull ExecutionEnvironment env,
                                            @Nonnull Sdk javaSdk,
                                            @Nonnull String consuloHome,
                                            @Nullable String pluginsHomePath) throws ExecutionException {
    return new ConsuloSandboxRunState(env, javaSdk, consuloHome, pluginsHomePath);
  }
}
