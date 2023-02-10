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

import consulo.annotation.component.ExtensionImpl;
import consulo.devkit.module.extension.PluginModuleExtension;
import consulo.devkit.run.ConsuloRunConfiguration;
import consulo.execution.configuration.ConfigurationFactoryEx;
import consulo.execution.configuration.ConfigurationTypeBase;
import consulo.execution.configuration.RunConfiguration;
import consulo.module.extension.ModuleExtensionHelper;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import org.jetbrains.idea.devkit.DevKitBundle;

import javax.annotation.Nonnull;

@ExtensionImpl
public class PluginConfigurationType extends ConfigurationTypeBase {
  @Nonnull
  public static PluginConfigurationType getInstance() {
    return EP_NAME.findExtensionOrFail(PluginConfigurationType.class);
  }

  private String myVmParameters;

  public PluginConfigurationType() {
    super("#org.jetbrains.idea.devkit.run.PluginConfigurationType",
          DevKitBundle.message("run.configuration.title"),
          DevKitBundle.message("run.configuration.type.description"),
          PlatformIconGroup.icon16_sandbox());
    addFactory(new ConfigurationFactoryEx(this) {
      @Override
      public RunConfiguration createTemplateConfiguration(Project project) {
        final ConsuloRunConfiguration runConfiguration = new ConsuloRunConfiguration(project, this, getDisplayName());

        if (runConfiguration.VM_PARAMETERS == null) {
          runConfiguration.VM_PARAMETERS = getVmParameters();
        }
        else {
          runConfiguration.VM_PARAMETERS += getVmParameters();
        }
        return runConfiguration;
      }

      @Override
      public boolean isApplicable(@Nonnull Project project) {
        return ModuleExtensionHelper.getInstance(project).hasModuleExtension(PluginModuleExtension.class);
      }

      @Override
      public void onNewConfigurationCreated(@Nonnull RunConfiguration configuration) {
        ConsuloRunConfiguration runConfiguration = (ConsuloRunConfiguration)configuration;

        runConfiguration.addPredefinedLogFile(ConsuloRunConfiguration.CONSULO_LOG);
      }
    });
  }

  @Nonnull
  private String getVmParameters() {
    if (myVmParameters == null) {
//      String vmOptions = VMOptions.read();
//      myVmParameters = vmOptions != null ? vmOptions.trim() : "";
      myVmParameters = "";
    }

    return myVmParameters;
  }
}
