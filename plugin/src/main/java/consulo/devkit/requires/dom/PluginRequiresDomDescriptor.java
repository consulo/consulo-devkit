package consulo.devkit.requires.dom;

import consulo.annotation.component.ExtensionImpl;
import consulo.component.util.Iconable;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.image.Image;
import consulo.xml.util.xml.DomFileDescription;

/**
 * @author VISTALL
 * @since 28-Aug-22
 */
@ExtensionImpl
public class PluginRequiresDomDescriptor extends DomFileDescription<PluginRequires> {
  public PluginRequiresDomDescriptor() {
    super(PluginRequires.class, "consulo-plugin-requires");
  }

  @Override
  public Image getFileIcon(@Iconable.IconFlags int flags) {
    return PlatformIconGroup.actionsImport();
  }
}
