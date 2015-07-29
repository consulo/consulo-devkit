/*
 * Copyright 2000-2011 JetBrains s.r.o.
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

import java.awt.BorderLayout;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.consulo.lombok.annotations.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.devkit.DevKitBundle;
import org.jetbrains.idea.devkit.sdk.ConsuloSdkType;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.SdkTypeId;
import com.intellij.openapi.roots.ui.configuration.SdkComboBox;
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactManager;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.RawCommandLineEditor;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.util.ObjectUtils;
import com.intellij.util.ui.FormBuilder;

@Logger
public abstract class ConsuloRunConfigurationEditorBase<T extends ConsuloRunConfigurationBase> extends SettingsEditor<T>
{
	private static class ArtifactItem
	{
		private final String myName;
		private final Artifact myArtifact;

		private ArtifactItem(String name, Artifact artifact)
		{
			myName = name;
			myArtifact = artifact;
		}
	}

	private SdkComboBox myJavaSdkComboBox;
	private SdkComboBox myConsuloSdkComboBox;
	private RawCommandLineEditor myProgramParameters;
	private RawCommandLineEditor myVMParameters;

	private JComboBox myArtifactComboBox;
	private JPanel myRoot;
	private JCheckBox myAlternativeConsuloSdkCheckBox;
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

	protected void setupPanel(@NotNull FormBuilder builder)
	{
		ProjectSdksModel projectSdksModel = new ProjectSdksModel();
		if(!projectSdksModel.isInitialized())
		{
			projectSdksModel.reset(myProject);
		}
		myJavaSdkComboBox = new SdkComboBox(projectSdksModel, new Condition<SdkTypeId>()
		{
			@Override
			public boolean value(SdkTypeId sdkTypeId)
			{
				return sdkTypeId instanceof JavaSdk;
			}
		}, false);
		builder.addLabeledComponent("Java SDK", myJavaSdkComboBox);

		myConsuloSdkComboBox = new SdkComboBox(projectSdksModel, new Condition<SdkTypeId>()
		{
			@Override
			public boolean value(SdkTypeId sdkTypeId)
			{
				return sdkTypeId instanceof ConsuloSdkType;
			}
		}, true);
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

		final Artifact[] sortedArtifacts = ArtifactManager.getInstance(myProject).getSortedArtifacts();
		myArtifactComboBox = new ComboBox();
		myArtifactComboBox.addItem(ObjectUtils.NULL);
		for(Artifact sortedArtifact : sortedArtifacts)
		{
			myArtifactComboBox.addItem(new ArtifactItem(sortedArtifact.getName(), sortedArtifact));
		}

		myArtifactComboBox.setRenderer(new ColoredListCellRenderer()
		{
			@Override
			protected void customizeCellRenderer(JList list, Object value, int index, boolean selected, boolean hasFocus)
			{
				if(value == ObjectUtils.NULL)
				{
					append("<None>", SimpleTextAttributes.REGULAR_ATTRIBUTES);
					setIcon(AllIcons.Actions.Help);
					return;
				}

				ArtifactItem artifactItem = (ArtifactItem) value;
				if(artifactItem == null)
				{
					return;
				}

				final Artifact artifact = artifactItem.myArtifact;
				if(artifact == null)
				{
					append(artifactItem.myName, SimpleTextAttributes.ERROR_ATTRIBUTES);
					setIcon(AllIcons.Nodes.Artifact);
				}
				else
				{
					append(artifactItem.myName, SimpleTextAttributes.REGULAR_ATTRIBUTES);
					setIcon(artifact.getArtifactType().getIcon());
				}
			}
		});

		builder.addLabeledComponent("Plugin Artifact", myArtifactComboBox);

		myProgramParameters = new RawCommandLineEditor();
		builder.addLabeledComponent("Program Parameters", myProgramParameters);
		myVMParameters = new RawCommandLineEditor();
		builder.addLabeledComponent("VM Parameters", myVMParameters);
	}

	@Override
	public void resetEditorFrom(T prc)
	{
		myVMParameters.setText(prc.VM_PARAMETERS);
		myAlternativeConsuloSdkCheckBox.setSelected(prc.USE_ALT_CONSULO_SDK);
		if(prc.ALT_CONSULO_SDK_PATH != null)
		{
			myAltConsuloSdkTextField.setText(FileUtil.toSystemDependentName(prc.ALT_CONSULO_SDK_PATH));
		}
		myVMParameters.setDialogCaption(DevKitBundle.message("label.vm.parameters"));
		myProgramParameters.setText(prc.PROGRAM_PARAMETERS);
		myProgramParameters.setDialogCaption(DevKitBundle.message("label.program.parameters"));

		final String artifactName = prc.getArtifactName();
		if(artifactName != null)
		{
			final Artifact artifact = ArtifactManager.getInstance(myProject).findArtifact(artifactName);
			if(artifact != null)
			{
				int i = -1;
				for(int l = 0; l < myArtifactComboBox.getItemCount(); l++)
				{
					final Object itemAt = myArtifactComboBox.getItemAt(l);
					if(!(itemAt instanceof ArtifactItem))
					{
						continue;
					}
					if(((ArtifactItem) itemAt).myArtifact == artifact)
					{
						i = l;
						break;
					}
				}

				if(i >= 0)
				{
					myArtifactComboBox.setSelectedIndex(i);
				}
				else
				{
					myArtifactComboBox.addItem(new ArtifactItem(artifactName, artifact));
				}
			}
			else
			{
				myArtifactComboBox.addItem(new ArtifactItem(artifactName, null));
			}
		}
		else
		{
			myArtifactComboBox.setSelectedItem(ObjectUtils.NULL);
		}

		myJavaSdkComboBox.setSelectedSdk(prc.getJavaSdkName());
		myConsuloSdkComboBox.setSelectedSdk(prc.getConsuloSdkName());
	}

	@Override
	public void applyEditorTo(T prc) throws ConfigurationException
	{
		prc.setArtifactName(myArtifactComboBox.getSelectedItem() == ObjectUtils.NULL ? null : ((ArtifactItem) myArtifactComboBox.getSelectedItem())
				.myName);
		prc.setJavaSdkName(myJavaSdkComboBox.getSelectedSdkName());
		prc.setConsuloSdkName(myConsuloSdkComboBox.getSelectedSdkName());

		prc.VM_PARAMETERS = myVMParameters.getText();
		prc.PROGRAM_PARAMETERS = myProgramParameters.getText();
		prc.ALT_CONSULO_SDK_PATH = StringUtil.nullize(FileUtil.toSystemIndependentName(myAltConsuloSdkTextField.getText()));
		prc.USE_ALT_CONSULO_SDK = myAlternativeConsuloSdkCheckBox.isSelected();
	}

	@Override
	@NotNull
	public JComponent createEditor()
	{
		return myRoot;
	}
}
