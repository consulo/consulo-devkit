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

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.RawCommandLineEditor;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.util.ui.FormBuilder;
import consulo.roots.ui.configuration.SdkComboBox;
import org.jetbrains.idea.devkit.DevKitBundle;
import org.jetbrains.idea.devkit.sdk.ConsuloSdkType;

import javax.annotation.Nonnull;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;

public abstract class ConsuloRunConfigurationEditorBase<T extends ConsuloRunConfigurationBase> extends SettingsEditor<T>
{
	private SdkComboBox myJavaSdkComboBox;
	private SdkComboBox myConsuloSdkComboBox;
	private RawCommandLineEditor myProgramParameters;
	private RawCommandLineEditor myVMParameters;

	private JPanel myRoot;
	private JCheckBox myAlternativeConsuloSdkCheckBox;
	private JCheckBox myEnableJava9Modules;
	private TextFieldWithBrowseButton myPluginsHomePath;
	private TextFieldWithBrowseButton myAltConsuloSdkTextField;

	private final Project myProject;

	public ConsuloRunConfigurationEditorBase(Project project)
	{
		myProject = project;
	}

	protected void initPanel()
	{
		FormBuilder builder = FormBuilder.createFormBuilder();

		setupPanel(builder);

		myRoot = new JPanel(new BorderLayout());
		myRoot.add(builder.getPanel(), BorderLayout.NORTH);
	}

	protected void setupPanel(@Nonnull FormBuilder builder)
	{
		ProjectSdksModel projectSdksModel = new ProjectSdksModel();
		if(!projectSdksModel.isInitialized())
		{
			projectSdksModel.reset();
		}
		myJavaSdkComboBox = new SdkComboBox(projectSdksModel, sdkTypeId -> sdkTypeId instanceof JavaSdk, false);
		builder.addLabeledComponent("Java SDK", myJavaSdkComboBox);

		myConsuloSdkComboBox = new SdkComboBox(projectSdksModel, sdkTypeId -> sdkTypeId instanceof ConsuloSdkType, true);
		builder.addLabeledComponent("Consulo SDK", myConsuloSdkComboBox);

		myAlternativeConsuloSdkCheckBox = new JBCheckBox("Alt Consulo SDK:");
		myAlternativeConsuloSdkCheckBox.addChangeListener(new ChangeListener()
		{
			@Override
			public void stateChanged(ChangeEvent e)
			{
				myAltConsuloSdkTextField.setEditable(myAlternativeConsuloSdkCheckBox.isSelected());
				myConsuloSdkComboBox.setEnabled(!myAlternativeConsuloSdkCheckBox.isSelected());
			}
		});
		builder.addComponent(myAlternativeConsuloSdkCheckBox);

		myAltConsuloSdkTextField = new TextFieldWithBrowseButton();
		myAltConsuloSdkTextField.addBrowseFolderListener("Select SDK", "Select alternative consulo sdk for run", myProject,
				FileChooserDescriptorFactory.createSingleFolderDescriptor());
		myAltConsuloSdkTextField.setEditable(myAlternativeConsuloSdkCheckBox.isSelected());
		builder.addComponent(myAltConsuloSdkTextField);


		myPluginsHomePath = new TextFieldWithBrowseButton();
		myPluginsHomePath.addBrowseFolderListener("Select Plugins Home Path", "Select plugins home path", myProject, FileChooserDescriptorFactory.createSingleFolderDescriptor());

		builder.addLabeledComponent("Plugins Home Path", myPluginsHomePath);

		myProgramParameters = new RawCommandLineEditor();
		builder.addLabeledComponent("Program Parameters", myProgramParameters);
		myVMParameters = new RawCommandLineEditor();
		builder.addLabeledComponent("VM Parameters", myVMParameters);

		myEnableJava9Modules = new JBCheckBox("Enable Java 9 modules?");

		builder.addComponent(myEnableJava9Modules);
	}

	@Override
	public void resetEditorFrom(T prc)
	{
		myVMParameters.setText(prc.VM_PARAMETERS);
		myAlternativeConsuloSdkCheckBox.setSelected(prc.USE_ALT_CONSULO_SDK);
		myEnableJava9Modules.setSelected(prc.ENABLED_JAVA9_MODULES);
		if(prc.ALT_CONSULO_SDK_PATH != null)
		{
			myAltConsuloSdkTextField.setText(FileUtil.toSystemDependentName(prc.ALT_CONSULO_SDK_PATH));
		}

		if(prc.PLUGINS_HOME_PATH != null)
		{
			myPluginsHomePath.setText(FileUtil.toSystemDependentName(prc.PLUGINS_HOME_PATH));
		}

		myVMParameters.setDialogCaption(DevKitBundle.message("label.vm.parameters"));
		myProgramParameters.setText(prc.PROGRAM_PARAMETERS);
		myProgramParameters.setDialogCaption(DevKitBundle.message("label.program.parameters"));

		myJavaSdkComboBox.setSelectedSdk(prc.getJavaSdkName());
		myConsuloSdkComboBox.setSelectedSdk(prc.getConsuloSdkName());
	}

	@Override
	public void applyEditorTo(T prc) throws ConfigurationException
	{
		prc.setJavaSdkName(myJavaSdkComboBox.getSelectedSdkName());
		prc.setConsuloSdkName(myConsuloSdkComboBox.getSelectedSdkName());
		prc.ENABLED_JAVA9_MODULES = myEnableJava9Modules.isSelected();

		prc.VM_PARAMETERS = myVMParameters.getText();
		prc.PROGRAM_PARAMETERS = myProgramParameters.getText();
		prc.PLUGINS_HOME_PATH = StringUtil.nullize(FileUtil.toSystemIndependentName(myPluginsHomePath.getText()));
		prc.ALT_CONSULO_SDK_PATH = StringUtil.nullize(FileUtil.toSystemIndependentName(myAltConsuloSdkTextField.getText()));
		prc.USE_ALT_CONSULO_SDK = myAlternativeConsuloSdkCheckBox.isSelected();
	}

	@Override
	@Nonnull
	public JComponent createEditor()
	{
		return myRoot;
	}
}
