package consulo.devkit.module.library;

import gnu.trove.THashMap;

import java.awt.BorderLayout;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.JPanel;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.RepositoryHelper;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.openapi.util.Ref;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.TextFieldWithAutoCompletion;
import consulo.ide.updateSettings.UpdateSettings;

/**
 * @author VISTALL
 * @since 29-Sep-16
 */
public class ChoosePluginPanel extends JPanel
{
	private EditorTextField myTextField;

	private Map<String, IdeaPluginDescriptor> myMap = new THashMap<>();

	public ChoosePluginPanel(@NotNull Project project)
	{
		super(new BorderLayout());

		Ref<List<IdeaPluginDescriptor>> listRef = Ref.create(Collections.emptyList());

		new Task.Modal(project, "Loading plugins", false)
		{
			@Override
			public void run(@NotNull ProgressIndicator progressIndicator)
			{
				try
				{
					listRef.set(RepositoryHelper.loadPluginsFromRepository(progressIndicator, UpdateSettings.getInstance().getChannel()));
				}
				catch(Exception e)
				{
					//
				}
			}
		}.queue();

		for(IdeaPluginDescriptor descriptor : listRef.get())
		{
			myMap.put(descriptor.getPluginId().getIdString(), descriptor);
		}

		myTextField = TextFieldWithAutoCompletion.create(project, myMap.values().stream().map(x -> x.getPluginId().getIdString()).collect(Collectors.toList()), false, null);
		myTextField.setPreferredWidth(200);
		add(LabeledComponent.left(myTextField, "Plugin ID"));
	}

	public EditorTextField getTextField()
	{
		return myTextField;
	}

	@Nullable
	public IdeaPluginDescriptor getPluginDescriptor()
	{
		return myMap.get(myTextField.getText());
	}
}
