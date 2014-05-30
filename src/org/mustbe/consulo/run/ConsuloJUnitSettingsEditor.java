/*
 * Copyright 2013-2014 must-be.org
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

package org.mustbe.consulo.run;

import java.awt.BorderLayout;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;

import org.apache.commons.lang.ObjectUtils;
import org.consulo.sdk.SdkUtil;
import org.consulo.util.pointers.Named;
import org.consulo.util.pointers.NamedPointer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.devkit.sdk.ConsuloSdkType;
import com.intellij.execution.junit.JUnitConfiguration;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkTable;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactManager;
import com.intellij.packaging.artifacts.ArtifactPointer;
import com.intellij.packaging.artifacts.ArtifactPointerUtil;
import com.intellij.packaging.impl.artifacts.PlainArtifactType;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.SimpleTextAttributes;

/**
 * @author VISTALL
 * @since 29.05.14
 */
@SuppressWarnings("unchecked")
public class ConsuloJUnitSettingsEditor extends SettingsEditor<JUnitConfiguration>
{
	private final Project myProject;
	private ComboBox myConsuloSdkBox = new ComboBox();
	private ComboBox myArtifactBox = new ComboBox();

	public ConsuloJUnitSettingsEditor(Project project)
	{
		myProject = project;
		myArtifactBox.addItem(ObjectUtils.NULL);
		myConsuloSdkBox.addItem(ObjectUtils.NULL);

		myConsuloSdkBox.setRenderer(new ColoredListCellRenderer()
		{
			@Override
			protected void customizeCellRenderer(JList list, Object value, int index, boolean selected, boolean hasFocus)
			{
				if(value instanceof Artifact)
				{
					setIcon(((Artifact) value).getArtifactType().getIcon());
					append(((Artifact) value).getName());
				}
				else if(value instanceof Sdk)
				{
					setIcon(SdkUtil.getIcon((Sdk) value));
					append(((Sdk) value).getName());
				}
				else if(value instanceof ArtifactPointer)
				{
					setIcon(AllIcons.Nodes.Artifact);
					append(String.valueOf(((ArtifactPointer) value).getName()), SimpleTextAttributes.ERROR_ATTRIBUTES);
				}
				else if(value == ObjectUtils.NULL)
				{
					append("<None>");
				}
				else if(value instanceof NamedPointer) // sdk pointer dont have class
				{
					setIcon(AllIcons.Toolbar.Unknown);
					append(String.valueOf(((NamedPointer) value).getName()), SimpleTextAttributes.ERROR_ATTRIBUTES);
				}
			}
		});
		myArtifactBox.setRenderer(new ColoredListCellRenderer()
		{
			@Override
			protected void customizeCellRenderer(JList list, Object value, int index, boolean selected, boolean hasFocus)
			{
				if(value instanceof Artifact)
				{
					setIcon(((Artifact) value).getArtifactType().getIcon());
					append(((Artifact) value).getName());
				}
				else if(value == ObjectUtils.NULL)
				{
					append("<None>");
				}
				else if(value instanceof String)
				{
					setIcon(AllIcons.Nodes.Artifact);
					append(String.valueOf(value), SimpleTextAttributes.ERROR_ATTRIBUTES);
				}
			}
		});

		Artifact[] artifacts = ArtifactManager.getInstance(myProject).getArtifacts();
		for(Artifact artifact : artifacts)
		{
			if(artifact.getArtifactType() == PlainArtifactType.getInstance())
			{
				myConsuloSdkBox.addItem(artifact);
				myArtifactBox.addItem(artifact);
			}
		}

		List<Sdk> sdksOfType = SdkTable.getInstance().getSdksOfType(ConsuloSdkType.getInstance());
		for(Sdk sdk : sdksOfType)
		{
			myConsuloSdkBox.addItem(sdk);
		}

		myConsuloSdkBox.setSelectedItem(null);
	}

	@Override
	protected void resetEditorFrom(JUnitConfiguration s)
	{
		ConsuloJUnitData data = s.getUserData(ConsuloJUnitData.KEY);
		if(data == null)
		{
			data = new ConsuloJUnitData();
		}

		NamedPointer<?> consuloPointer = data.getConsuloPointer();
		if(consuloPointer == null)
		{
			myConsuloSdkBox.setSelectedItem(ObjectUtils.NULL);
		}
		else
		{
			Named named = consuloPointer.get();
			if(named != null)
			{
				myConsuloSdkBox.setSelectedItem(named);
			}
			else
			{
				myConsuloSdkBox.addItem(consuloPointer);
				myConsuloSdkBox.setSelectedItem(consuloPointer);
			}
		}

		NamedPointer<Artifact> pluginPointer = data.getPluginPointer();
		if(pluginPointer == null)
		{
			myArtifactBox.setSelectedItem(ObjectUtils.NULL);
		}
		else
		{
			Artifact artifact = pluginPointer.get();
			if(artifact == null)
			{
				myArtifactBox.addItem(pluginPointer.getName());
				myArtifactBox.setSelectedItem(pluginPointer.getName());
			}
			else
			{
				myArtifactBox.setSelectedItem(artifact);
			}
		}
	}

	@Override
	protected void applyEditorTo(JUnitConfiguration s) throws ConfigurationException
	{
		ConsuloJUnitData data = s.getUserData(ConsuloJUnitData.KEY);
		if(data == null)
		{
			s.putUserData(ConsuloJUnitData.KEY, data = new ConsuloJUnitData());
		}

		Object consuloItem = myConsuloSdkBox.getSelectedItem();
		if(consuloItem == ObjectUtils.NULL)
		{
			data.setConsuloPointer(ConsuloJUnitData.Model.none, null);
		}
		else if(consuloItem instanceof Sdk)
		{
			data.setConsuloPointer(ConsuloJUnitData.Model.sdk, SdkUtil.createPointer((Sdk) consuloItem));
		}
		else if(consuloItem instanceof Artifact)
		{
			data.setConsuloPointer(ConsuloJUnitData.Model.artifact, ArtifactPointerUtil.getPointerManager(myProject).create((Artifact) consuloItem));
		}
		else if(consuloItem instanceof ArtifactPointer)
		{
			data.setConsuloPointer(ConsuloJUnitData.Model.artifact, (NamedPointer<?>) consuloItem);
		}
		else if(consuloItem instanceof NamedPointer)
		{
			// sdk pointer dont have class
			data.setConsuloPointer(ConsuloJUnitData.Model.sdk, (NamedPointer<?>) consuloItem);
		}

		Object artifactItem = myArtifactBox.getSelectedItem();
		if(artifactItem == ObjectUtils.NULL)
		{
			data.setPluginPointer(null);
		}
		else if(artifactItem instanceof Artifact)
		{
			data.setPluginPointer(ArtifactPointerUtil.getPointerManager(myProject).create((Artifact) artifactItem));
		}
		else
		{
			assert artifactItem instanceof ArtifactPointer;
			data.setPluginPointer((NamedPointer<Artifact>) artifactItem);
		}
	}

	@NotNull
	@Override
	protected JComponent createEditor()
	{
		JPanel panel = new JPanel(new VerticalFlowLayout());
		panel.add(LabeledComponent.create(myConsuloSdkBox, "Consulo").setLabelLocation(BorderLayout.WEST));
		panel.add(LabeledComponent.create(myArtifactBox, "Artifact").setLabelLocation(BorderLayout.WEST));

		return panel;
	}
}
