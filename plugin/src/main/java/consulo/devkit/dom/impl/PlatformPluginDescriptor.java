package consulo.devkit.dom.impl;

import consulo.container.plugin.PluginDescriptorStub;
import consulo.container.plugin.PluginId;
import consulo.devkit.Consulo3PluginIds;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author VISTALL
 * @since 28-Aug-22
 */
public class PlatformPluginDescriptor extends PluginDescriptorStub
{
	private static final Map<PluginId, PlatformPluginDescriptor> ourPlatformsV3 = new HashMap<>();

	static
	{
		ourPlatformsV3.put(Consulo3PluginIds.CONSULO_DESKTOP_AWT, new PlatformPluginDescriptor(Consulo3PluginIds.CONSULO_DESKTOP_AWT, "Consulo [desktop] [AWT]"));
		ourPlatformsV3.put(Consulo3PluginIds.CONSULO_DESKTOP_SWT, new PlatformPluginDescriptor(Consulo3PluginIds.CONSULO_DESKTOP_SWT, "Consulo [desktop] [SWT]"));
		ourPlatformsV3.put(Consulo3PluginIds.CONSULO_WEB, new PlatformPluginDescriptor(Consulo3PluginIds.CONSULO_WEB, "Consulo [web]"));
		ourPlatformsV3.put(Consulo3PluginIds.CONSULO_REPO_ANALYZER, new PlatformPluginDescriptor(Consulo3PluginIds.CONSULO_REPO_ANALYZER, "Consulo [repo analyzer]"));
	}

	@Nonnull
	public static Collection<PlatformPluginDescriptor> getPluginsV3()
	{
		return ourPlatformsV3.values();
	}

	private final PluginId myPluginId;
	private final String myName;

	public PlatformPluginDescriptor(PluginId pluginId, String name)
	{
		myPluginId = pluginId;
		myName = name;
	}

	@Override
	public String getName()
	{
		return myName;
	}

	@Nonnull
	@Override
	public PluginId getPluginId()
	{
		return myPluginId;
	}
}
