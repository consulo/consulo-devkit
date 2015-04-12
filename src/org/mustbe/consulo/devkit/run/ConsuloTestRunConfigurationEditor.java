package org.mustbe.consulo.devkit.run;

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
	private JTextField myPluginIdTextField;
	private JTextField myClassNameField;

	public ConsuloTestRunConfigurationEditor(Project project)
	{
		super(project);
	}

	@Override
	protected void setupPanel(@NotNull Project project, @NotNull FormBuilder builder)
	{
		super.setupPanel(project, builder);

		myPluginIdTextField = new JBTextField();
		myClassNameField = new JBTextField();

		builder.addLabeledComponent("Plugin ID", myPluginIdTextField);
		builder.addLabeledComponent("Class Name", myClassNameField);
	}

	@Override
	public void resetEditorFrom(ConsuloTestRunConfiguration prc)
	{
		super.resetEditorFrom(prc);

		myPluginIdTextField.setText(prc.PLUGIN_ID);
		myClassNameField.setText(prc.CLASS_NAME);
	}

	@Override
	public void applyEditorTo(ConsuloTestRunConfiguration prc) throws ConfigurationException
	{
		super.applyEditorTo(prc);

		prc.PLUGIN_ID = StringUtil.nullize(myPluginIdTextField.getText(), true);
		prc.CLASS_NAME = StringUtil.nullize(myClassNameField.getText(), true);
	}
}
