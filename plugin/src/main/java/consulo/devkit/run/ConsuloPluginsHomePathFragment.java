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
import consulo.fileChooser.FileChooserDescriptorFactory;
import consulo.fileChooser.FileChooserTextBoxBuilder;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.io.FileUtil;
import consulo.util.lang.StringUtil;

public class ConsuloPluginsHomePathFragment<T extends ConsuloRunConfigurationBase> implements RunConfigurationFragment<T> {
    @Override
    public String getId() {
        return "consuloPluginsHomePath";
    }

    @Override
    public LocalizeValue getName() {
        return DevKitLocalize.runConfigurationFragmentPluginsHomePath();
    }

    @Override
    public boolean isVisibleByDefault(T target) {
        return target.getPluginsHomePath() != null;
    }

    @Override
    public void applyHidden(T target) {
        target.setPluginsHomePath(null);
    }

    @RequiredUIAccess
    @Override
    public RunConfigurationFragmentEditor<T> createEditor(Project project, Disposable parent) {
        FileChooserTextBoxBuilder.Controller path = FileChooserTextBoxBuilder.create(project)
            .fileChooserDescriptor(FileChooserDescriptorFactory.createSingleFolderDescriptor())
            .dialogTitle(DevKitLocalize.runConfigurationPluginsHomePathChooserTitle())
            .dialogDescription(DevKitLocalize.runConfigurationPluginsHomePathChooserDescription())
            .uiDisposable(parent)
            .build();

        return new RunConfigurationFragmentEditor<>() {
            @RequiredUIAccess
            @Override
            public void addRows(RunConfigurationFragmentLayout layout) {
                layout.addLabeled(DevKitLocalize.labelPluginsHomePath(), path.getComponent());
            }

            @RequiredUIAccess
            @Override
            public void reset(T target) {
                path.setValue(FileUtil.toSystemDependentName(StringUtil.notNullize(target.getPluginsHomePath())), false);
            }

            @RequiredUIAccess
            @Override
            public void apply(T target) {
                target.setPluginsHomePath(FileUtil.toSystemIndependentName(StringUtil.notNullize(path.getValue()).trim()));
            }
        };
    }
}
