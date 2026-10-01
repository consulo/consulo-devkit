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

import com.intellij.java.language.projectRoots.JavaSdkType;
import consulo.devkit.localize.DevKitLocalize;
import consulo.execution.configuration.ui.SettingsEditor;
import consulo.fileChooser.FileChooserDescriptorFactory;
import consulo.fileChooser.FileChooserTextBoxBuilder;
import consulo.localize.LocalizeValue;
import consulo.module.ui.BundleBox;
import consulo.module.ui.BundleBoxBuilder;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.process.cmd.ParametersListUtil;
import consulo.project.Project;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.TextBoxWithExpandAction;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.util.FormBuilder;
import consulo.util.io.FileUtil;
import consulo.util.lang.StringUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public abstract class ConsuloRunConfigurationEditorBase<T extends ConsuloRunConfigurationBase> extends SettingsEditor<T> {
    @Nullable
    private BundleBox myJavaSdkBox;
    @Nullable
    private TextBoxWithExpandAction myProgramParameters;
    @Nullable
    private TextBoxWithExpandAction myVMParameters;

    @Nullable
    private CheckBox myEnableJava9Modules;
    @Nullable
    private FileChooserTextBoxBuilder.Controller myPluginsHomePath;
    @Nullable
    private FileChooserTextBoxBuilder.Controller myConsuloSdkPath;

    private final Project myProject;

    public ConsuloRunConfigurationEditorBase(Project project) {
        myProject = project;
    }

    @Override
    @RequiredUIAccess
    protected Component createUIComponent() {
        FormBuilder builder = FormBuilder.create();

        setupPanel(builder);

        return builder.build();
    }

    @RequiredUIAccess
    protected void setupPanel(@Nonnull FormBuilder builder) {
        BundleBox javaSdkBox = BundleBoxBuilder.create(this)
            .withSdkTypeFilterByClass(JavaSdkType.class)
            .build();
        myJavaSdkBox = javaSdkBox;
        builder.addLabeled(DevKitLocalize.labelJavaSdk(), javaSdkBox.getComponent());

        FileChooserTextBoxBuilder.Controller consuloSdkPath = createFolderChooser(
            DevKitLocalize.runConfigurationConsuloSdkChooserTitle(),
            DevKitLocalize.runConfigurationConsuloSdkChooserDescription()
        );
        myConsuloSdkPath = consuloSdkPath;
        builder.addLabeled(DevKitLocalize.labelConsuloSdk(), consuloSdkPath.getComponent());

        FileChooserTextBoxBuilder.Controller pluginsHomePath = createFolderChooser(
            DevKitLocalize.runConfigurationPluginsHomePathChooserTitle(),
            DevKitLocalize.runConfigurationPluginsHomePathChooserDescription()
        );
        myPluginsHomePath = pluginsHomePath;
        builder.addLabeled(DevKitLocalize.labelPluginsHomePath(), pluginsHomePath.getComponent());

        TextBoxWithExpandAction programParameters = createParametersBox(DevKitLocalize.labelProgramParameters());
        myProgramParameters = programParameters;
        builder.addLabeled(DevKitLocalize.labelProgramParameters(), programParameters);

        TextBoxWithExpandAction vmParameters = createParametersBox(DevKitLocalize.labelVmParameters());
        myVMParameters = vmParameters;
        builder.addLabeled(DevKitLocalize.labelVmParameters(), vmParameters);

        CheckBox enableJava9Modules = CheckBox.create(DevKitLocalize.runConfigurationEnableJava9Modules());
        myEnableJava9Modules = enableJava9Modules;
        builder.addBottom(enableJava9Modules);
    }

    @RequiredUIAccess
    private FileChooserTextBoxBuilder.Controller createFolderChooser(@Nonnull LocalizeValue title, @Nonnull LocalizeValue description) {
        return FileChooserTextBoxBuilder.create(myProject)
            .fileChooserDescriptor(FileChooserDescriptorFactory.createSingleFolderDescriptor())
            .dialogTitle(title)
            .dialogDescription(description)
            .uiDisposable(this)
            .build();
    }

    @RequiredUIAccess
    private static TextBoxWithExpandAction createParametersBox(@Nonnull LocalizeValue dialogTitle) {
        return TextBoxWithExpandAction.create(
            PlatformIconGroup.actionsShow(),
            dialogTitle.get(),
            ParametersListUtil.DEFAULT_LINE_PARSER,
            ParametersListUtil.DEFAULT_LINE_JOINER
        );
    }

    @Override
    @RequiredUIAccess
    protected void resetEditorFrom(T configuration) {
        BundleBox javaSdkBox = myJavaSdkBox;
        if (javaSdkBox != null) {
            javaSdkBox.setSelectedBundle(configuration.getJavaSdkName());
        }

        FileChooserTextBoxBuilder.Controller consuloSdkPath = myConsuloSdkPath;
        if (consuloSdkPath != null) {
            consuloSdkPath.setValue(FileUtil.toSystemDependentName(StringUtil.notNullize(configuration.ALT_CONSULO_SDK_PATH)));
        }

        FileChooserTextBoxBuilder.Controller pluginsHomePath = myPluginsHomePath;
        if (pluginsHomePath != null) {
            pluginsHomePath.setValue(FileUtil.toSystemDependentName(StringUtil.notNullize(configuration.PLUGINS_HOME_PATH)));
        }

        TextBoxWithExpandAction programParameters = myProgramParameters;
        if (programParameters != null) {
            programParameters.setValue(StringUtil.notNullize(configuration.PROGRAM_PARAMETERS));
        }

        TextBoxWithExpandAction vmParameters = myVMParameters;
        if (vmParameters != null) {
            vmParameters.setValue(StringUtil.notNullize(configuration.VM_PARAMETERS));
        }

        CheckBox enableJava9Modules = myEnableJava9Modules;
        if (enableJava9Modules != null) {
            enableJava9Modules.setValue(configuration.ENABLED_JAVA9_MODULES);
        }
    }

    @Override
    @RequiredUIAccess
    protected void applyEditorTo(T configuration) {
        BundleBox javaSdkBox = myJavaSdkBox;
        if (javaSdkBox != null) {
            configuration.setJavaSdkName(javaSdkBox.getSelectedBundleName());
        }

        CheckBox enableJava9Modules = myEnableJava9Modules;
        if (enableJava9Modules != null) {
            configuration.ENABLED_JAVA9_MODULES = Boolean.TRUE.equals(enableJava9Modules.getValue());
        }

        TextBoxWithExpandAction vmParameters = myVMParameters;
        if (vmParameters != null) {
            configuration.VM_PARAMETERS = StringUtil.notNullize(vmParameters.getValue());
        }

        TextBoxWithExpandAction programParameters = myProgramParameters;
        if (programParameters != null) {
            configuration.PROGRAM_PARAMETERS = StringUtil.notNullize(programParameters.getValue());
        }

        FileChooserTextBoxBuilder.Controller pluginsHomePath = myPluginsHomePath;
        if (pluginsHomePath != null) {
            configuration.PLUGINS_HOME_PATH = StringUtil.nullize(FileUtil.toSystemIndependentName(pluginsHomePath.getValue()));
        }

        FileChooserTextBoxBuilder.Controller consuloSdkPath = myConsuloSdkPath;
        if (consuloSdkPath != null) {
            configuration.ALT_CONSULO_SDK_PATH = StringUtil.nullize(FileUtil.toSystemIndependentName(consuloSdkPath.getValue()));
        }
    }
}
