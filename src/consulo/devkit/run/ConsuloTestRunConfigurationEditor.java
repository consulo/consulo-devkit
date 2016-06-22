/*
 * Copyright 2013-2016 must-be.org
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

import java.awt.CardLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import org.jetbrains.annotations.NotNull;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;

/**
 * @author VISTALL
 * @since 12.04.2015
 */
public class ConsuloTestRunConfigurationEditor extends ConsuloRunConfigurationEditorBase<ConsuloTestRunConfiguration>
{
	public interface SubPanel
	{
		String getId();

		ConsuloTestRunConfiguration.TargetType getTargetType();

		void resetEditorFrom(ConsuloTestRunConfiguration prc);

		void applyEditorTo(ConsuloTestRunConfiguration prc) throws ConfigurationException;

		JComponent getComponent();
	}

	public class ClassSubPanel implements SubPanel
	{
		private JTextField myClassNameField;

		public ClassSubPanel()
		{
			myClassNameField = new JBTextField();
		}

		@Override
		public String getId()
		{
			return "Class";
		}

		@Override
		public ConsuloTestRunConfiguration.TargetType getTargetType()
		{
			return ConsuloTestRunConfiguration.TargetType.CLASS;
		}

		@Override
		public void resetEditorFrom(ConsuloTestRunConfiguration prc)
		{
			myClassNameField.setText(prc.CLASS_NAME);
		}

		@Override
		public void applyEditorTo(ConsuloTestRunConfiguration prc) throws ConfigurationException
		{
			prc.CLASS_NAME = StringUtil.nullize(myClassNameField.getText(), true);
		}

		@Override
		public JComponent getComponent()
		{
			FormBuilder builder = FormBuilder.createFormBuilder();
			builder.addLabeledComponent("Class Name", myClassNameField);
			return builder.getPanel();
		}
	}

	public class PackageSubPanel implements SubPanel
	{
		private JTextField myPackageField;

		public PackageSubPanel()
		{
			myPackageField = new JBTextField();
		}

		@Override
		public String getId()
		{
			return "Package";
		}

		@Override
		public ConsuloTestRunConfiguration.TargetType getTargetType()
		{
			return ConsuloTestRunConfiguration.TargetType.PACKAGE;
		}

		@Override
		public void resetEditorFrom(ConsuloTestRunConfiguration prc)
		{
			myPackageField.setText(prc.PACKAGE_NAME);
		}

		@Override
		public void applyEditorTo(ConsuloTestRunConfiguration prc) throws ConfigurationException
		{
			prc.PACKAGE_NAME = StringUtil.nullize(myPackageField.getText(), true);
		}

		@Override
		public JComponent getComponent()
		{
			FormBuilder builder = FormBuilder.createFormBuilder();
			builder.addLabeledComponent("Package", myPackageField);
			return builder.getPanel();
		}
	}

	private JPanel mySettingPanel;

	private Map<SubPanel, JRadioButton> myMap = new LinkedHashMap<SubPanel, JRadioButton>();

	public ConsuloTestRunConfigurationEditor(Project project)
	{
		super(project);
		initPanel();
	}

	@Override
	protected void setupPanel(@NotNull FormBuilder builder)
	{
		super.setupPanel(builder);

		JPanel headerPanel = new JPanel();

		final CardLayout cardLayout = new CardLayout();
		mySettingPanel = new JPanel(cardLayout);

		ButtonGroup buttonGroup = new ButtonGroup();
		for(final SubPanel subPanel : new SubPanel[] {new PackageSubPanel(), new ClassSubPanel()})
		{
			JRadioButton radioButton = new JRadioButton(subPanel.getId());
			radioButton.addItemListener(new ItemListener()
			{
				@Override
				public void itemStateChanged(ItemEvent e)
				{
					if(e.getStateChange() == ItemEvent.SELECTED)
					{
						cardLayout.show(mySettingPanel, subPanel.getId());
					}
				}
			});
			buttonGroup.add(radioButton);
			headerPanel.add(radioButton);

			JComponent component = subPanel.getComponent();

			mySettingPanel.add(component, subPanel.getId());

			myMap.put(subPanel, radioButton);
		}

		builder.addComponent(headerPanel);
		builder.addComponent(mySettingPanel);
	}

	@Override
	public void resetEditorFrom(ConsuloTestRunConfiguration prc)
	{
		super.resetEditorFrom(prc);

		for(Map.Entry<SubPanel, JRadioButton> entry : myMap.entrySet())
		{
			if(entry.getKey().getTargetType() == prc.getTargetType())
			{
				entry.getValue().setSelected(true);
			}
			entry.getKey().resetEditorFrom(prc);
		}
	}

	@Override
	public void applyEditorTo(ConsuloTestRunConfiguration prc) throws ConfigurationException
	{
		super.applyEditorTo(prc);

		for(Map.Entry<SubPanel, JRadioButton> entry : myMap.entrySet())
		{
			if(entry.getValue().isSelected())
			{
				prc.setTargetType(entry.getKey().getTargetType());
			}
			entry.getKey().applyEditorTo(prc);
		}
	}
}
