package consulo.devkit.requires.dom;

import consulo.annotation.component.ExtensionImpl;
import consulo.component.util.Iconable;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.image.Image;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.xml.psi.xml.XmlFile;
import consulo.xml.util.xml.DomFileDescription;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2022-08-28
 */
@ExtensionImpl
public class PluginRequiresDomDescriptor extends DomFileDescription<PluginRequires> {
    public PluginRequiresDomDescriptor() {
        super(PluginRequires.class, "consulo-plugin-requires");
    }

    @Override
    public boolean isMyFile(@Nonnull XmlFile file) {
        VirtualFile vFile = file.getVirtualFile();
        return vFile != null && StringUtil.equal(vFile.getNameSequence(), "plugin-requires.xml", false);
    }

    @Override
    public Image getFileIcon(@Iconable.IconFlags int flags) {
        return PlatformIconGroup.actionsImport();
    }
}
