package consulo.devkit.requires.dom;

import com.intellij.openapi.util.Iconable;
import com.intellij.util.xml.DomFileDescription;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.image.Image;

/**
 * @author VISTALL
 * @since 28-Aug-22
 */
public class PluginRequiresDomDescriptor extends DomFileDescription<PluginRequires>
{
	public PluginRequiresDomDescriptor()
	{
		super(PluginRequires.class, "consulo-plugin-requires");
	}

	@Override
	public Image getFileIcon(@Iconable.IconFlags int flags)
	{
		return PlatformIconGroup.actionsImport();
	}
}
