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

import com.intellij.java.language.projectRoots.JavaSdkType;
import consulo.devkit.localize.DevKitLocalize;
import consulo.disposer.Disposable;
import consulo.execution.configuration.fragment.RunConfigurationFragment;
import consulo.execution.configuration.fragment.RunConfigurationFragmentEditor;
import consulo.execution.configuration.fragment.RunConfigurationFragmentLayout;
import consulo.execution.configuration.fragment.StandardRunConfigurationFragmentOrder;
import consulo.fileChooser.FileChooserDescriptorFactory;
import consulo.fileChooser.FileChooserTextBoxBuilder;
import consulo.localize.LocalizeValue;
import consulo.module.ui.BundleBox;
import consulo.module.ui.BundleBoxBuilder;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.io.FileUtil;
import consulo.util.lang.StringUtil;

/**
 * The Java SDK the sandbox runs on and the Consulo build it starts.
 */
public class ConsuloSdkFragment<T extends ConsuloRunConfigurationBase> implements RunConfigurationFragment<T> {
    @Override
    public String getId() {
        return "consuloSdk";
    }

    @Override
    public LocalizeValue getName() {
        return DevKitLocalize.runConfigurationFragmentSdk();
    }

    @Override
    public int getOrder() {
        return StandardRunConfigurationFragmentOrder.FIRST;
    }

    @Override
    public boolean isRemovable() {
        return false;
    }

    @RequiredUIAccess
    @Override
    public RunConfigurationFragmentEditor<T> createEditor(Project project, Disposable parent) {
        BundleBox javaSdk = BundleBoxBuilder.create(parent)
            .withSdkTypeFilterByClass(JavaSdkType.class)
            .build();
        FileChooserTextBoxBuilder.Controller consuloSdk = FileChooserTextBoxBuilder.create(project)
            .fileChooserDescriptor(FileChooserDescriptorFactory.createSingleFolderDescriptor())
            .dialogTitle(DevKitLocalize.runConfigurationConsuloSdkChooserTitle())
            .dialogDescription(DevKitLocalize.runConfigurationConsuloSdkChooserDescription())
            .uiDisposable(parent)
            .build();

        return new RunConfigurationFragmentEditor<>() {
            @RequiredUIAccess
            @Override
            public void addRows(RunConfigurationFragmentLayout layout) {
                layout.addLabeled(DevKitLocalize.labelJavaSdk(), javaSdk.getComponent());
                layout.addLabeled(DevKitLocalize.labelConsuloSdk(), consuloSdk.getComponent());
            }

            @RequiredUIAccess
            @Override
            public void reset(T target) {
                javaSdk.setSelectedBundle(target.getJavaSdkName());
                consuloSdk.setValue(FileUtil.toSystemDependentName(StringUtil.notNullize(target.getConsuloSdkHome())), false);
            }

            @RequiredUIAccess
            @Override
            public void apply(T target) {
                target.setJavaSdkName(javaSdk.getSelectedBundleName());
                target.setConsuloSdkHome(FileUtil.toSystemIndependentName(StringUtil.notNullize(consuloSdk.getValue()).trim()));
            }
        };
    }
}
