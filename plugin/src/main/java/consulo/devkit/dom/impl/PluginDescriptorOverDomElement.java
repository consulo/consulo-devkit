package consulo.devkit.dom.impl;

import com.intellij.openapi.util.text.StringUtil;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginDescriptorStub;
import consulo.container.plugin.PluginId;
import org.jetbrains.idea.devkit.dom.IdeaPlugin;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 28-Aug-22
 */
public class PluginDescriptorOverDomElement extends PluginDescriptorStub implements PluginDescriptor
{
	private final IdeaPlugin myPlugin;
	private final PluginId myPluginId;

	public PluginDescriptorOverDomElement(PluginId pluginId, IdeaPlugin plugin)
	{
		myPluginId = pluginId;
		myPlugin = plugin;
	}

	public IdeaPlugin getPlugin()
	{
		return myPlugin;
	}

	@Override
	public String getName()
	{
		return StringUtil.notNullize(myPlugin.getName().getValue());
	}

	@Nonnull
	@Override
	public PluginId getPluginId()
	{
		return myPluginId;
	}
}
