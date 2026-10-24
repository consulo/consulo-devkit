/*
 * Copyright 2013-2026 consulo.io
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

import consulo.devkit.localize.DevKitLocalize;
import consulo.disposer.Disposable;
import consulo.execution.configuration.fragment.RunConfigurationFragment;
import consulo.execution.configuration.fragment.RunConfigurationFragmentEditor;
import consulo.execution.configuration.fragment.RunConfigurationFragmentLayout;
import consulo.execution.configuration.fragment.StandardRunConfigurationFragmentOrder;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.process.cmd.ParametersListUtil;
import consulo.project.Project;
import consulo.ui.TextBoxWithExpandAction;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.lang.StringUtil;

public class ConsuloVmParametersFragment<T extends ConsuloRunConfigurationBase> implements RunConfigurationFragment<T> {
    @Override
    public String getId() {
        return "consuloVmParameters";
    }

    @Override
    public LocalizeValue getName() {
        return DevKitLocalize.runConfigurationFragmentVmParameters();
    }

    @Override
    public int getOrder() {
        return StandardRunConfigurationFragmentOrder.PROGRAM_PARAMETERS - 10;
    }

    @Override
    public boolean isVisibleByDefault(T target) {
        return !StringUtil.isEmptyOrSpaces(target.getVmParameters());
    }

    @Override
    public void applyHidden(T target) {
        target.setVmParameters(null);
    }

    @RequiredUIAccess
    @Override
    public RunConfigurationFragmentEditor<T> createEditor(Project project, Disposable parent) {
        TextBoxWithExpandAction parameters = TextBoxWithExpandAction.create(
            PlatformIconGroup.actionsShow(),
            DevKitLocalize.labelVmParameters().get(),
            ParametersListUtil.DEFAULT_LINE_PARSER,
            ParametersListUtil.DEFAULT_LINE_JOINER
        );

        return new RunConfigurationFragmentEditor<>() {
            @RequiredUIAccess
            @Override
            public void addRows(RunConfigurationFragmentLayout layout) {
                layout.addLabeled(DevKitLocalize.labelVmParameters(), parameters);
            }

            @RequiredUIAccess
            @Override
            public void reset(T target) {
                parameters.setValue(StringUtil.notNullize(target.getVmParameters()), false);
            }

            @RequiredUIAccess
            @Override
            public void apply(T target) {
                target.setVmParameters(parameters.getValue());
            }
        };
    }
}
