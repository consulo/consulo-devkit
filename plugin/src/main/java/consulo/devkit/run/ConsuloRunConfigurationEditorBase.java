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
import consulo.configurable.ConfigurationException;
import consulo.devkit.localize.DevKitLocalize;
import consulo.execution.configuration.ui.SettingsEditor;
import consulo.execution.ui.awt.RawCommandLineEditor;
import consulo.fileChooser.FileChooserDescriptorFactory;
import consulo.ide.setting.ShowSettingsUtil;
import consulo.ide.setting.bundle.SettingsSdksModel;
import consulo.module.ui.awt.SdkComboBox;
import consulo.project.Project;
import consulo.ui.ex.awt.FormBuilder;
import consulo.ui.ex.awt.JBCheckBox;
import consulo.ui.ex.awt.TextFieldWithBrowseButton;
import consulo.util.io.FileUtil;
import consulo.util.lang.StringUtil;

import jakarta.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;

public abstract class ConsuloRunConfigurationEditorBase<T extends ConsuloRunConfigurationBase> extends SettingsEditor<T> {
    private SdkComboBox myJavaSdkComboBox;
    private RawCommandLineEditor myProgramParameters;
    private RawCommandLineEditor myVMParameters;

    private JPanel myRoot;
    private JCheckBox myEnableJava9Modules;
    private TextFieldWithBrowseButton myPluginsHomePath;
    private TextFieldWithBrowseButton myConsuloSdkTextField;

    private final Project myProject;

    public ConsuloRunConfigurationEditorBase(Project project) {
        myProject = project;
    }

    protected void initPanel() {
        FormBuilder builder = FormBuilder.createFormBuilder();

        setupPanel(builder);

        myRoot = new JPanel(new BorderLayout());
        myRoot.add(builder.getPanel(), BorderLayout.NORTH);
    }

    protected void setupPanel(@Nonnull FormBuilder builder) {
        SettingsSdksModel projectSdksModel = ShowSettingsUtil.getInstance().getSdksModel();
        myJavaSdkComboBox = new SdkComboBox(projectSdksModel, it -> it instanceof JavaSdkType, false);
        builder.addLabeledComponent("Java SDK", myJavaSdkComboBox);

        myConsuloSdkTextField = new TextFieldWithBrowseButton();
        myConsuloSdkTextField.addBrowseFolderListener(
            "Select SDK",
            "Select alternative consulo sdk for run",
            myProject,
            FileChooserDescriptorFactory.createSingleFolderDescriptor()
        );
        myConsuloSdkTextField.setEditable(true);
        builder.addLabeledComponent("Consulo SDK", myConsuloSdkTextField);


        myPluginsHomePath = new TextFieldWithBrowseButton();
        myPluginsHomePath.addBrowseFolderListener(
            "Select Plugins Home Path",
            "Select plugins home path",
            myProject,
            FileChooserDescriptorFactory.createSingleFolderDescriptor()
        );

        builder.addLabeledComponent("Plugins Home Path", myPluginsHomePath);

        myProgramParameters = new RawCommandLineEditor();
        builder.addLabeledComponent("Program Parameters", myProgramParameters);
        myVMParameters = new RawCommandLineEditor();
        builder.addLabeledComponent("VM Parameters", myVMParameters);

        myEnableJava9Modules = new JBCheckBox("Enable Java 9 modules?");

        builder.addComponent(myEnableJava9Modules);
    }

    @Override
    public void resetEditorFrom(T prc) {
        myVMParameters.setText(prc.VM_PARAMETERS);
        myEnableJava9Modules.setSelected(prc.ENABLED_JAVA9_MODULES);
        if (prc.ALT_CONSULO_SDK_PATH != null) {
            myConsuloSdkTextField.setText(FileUtil.toSystemDependentName(prc.ALT_CONSULO_SDK_PATH));
        }

        if (prc.PLUGINS_HOME_PATH != null) {
            myPluginsHomePath.setText(FileUtil.toSystemDependentName(prc.PLUGINS_HOME_PATH));
        }

        myVMParameters.setDialogCaption(DevKitLocalize.labelVmParameters().get());
        myProgramParameters.setText(prc.PROGRAM_PARAMETERS);
        myProgramParameters.setDialogCaption(DevKitLocalize.labelProgramParameters().get());

        myJavaSdkComboBox.setSelectedSdk(prc.getJavaSdkName());
    }

    @Override
    public void applyEditorTo(T prc) throws ConfigurationException {
        prc.setJavaSdkName(myJavaSdkComboBox.getSelectedSdkName());
        prc.ENABLED_JAVA9_MODULES = myEnableJava9Modules.isSelected();

        prc.VM_PARAMETERS = myVMParameters.getText();
        prc.PROGRAM_PARAMETERS = myProgramParameters.getText();
        prc.PLUGINS_HOME_PATH = StringUtil.nullize(FileUtil.toSystemIndependentName(myPluginsHomePath.getText()));
        prc.ALT_CONSULO_SDK_PATH = StringUtil.nullize(FileUtil.toSystemIndependentName(myConsuloSdkTextField.getText()));
    }

    @Override
    @Nonnull
    public JComponent createEditor() {
        return myRoot;
    }
}
