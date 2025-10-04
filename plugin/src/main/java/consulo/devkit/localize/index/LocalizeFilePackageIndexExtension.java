package consulo.devkit.localize.index;

import consulo.annotation.component.ExtensionImpl;
import consulo.index.io.ID;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2025-10-04
 */
@ExtensionImpl
public class LocalizeFilePackageIndexExtension extends BaseLocalizeFileIndexExtension {
    public static final ID<String, Void> INDEX = ID.create("consulo.localize.file.package.index");

    @Override
    protected String getFileId(VirtualFile file) {
        String fileName = file.getNameWithoutExtension();
        String packageName = StringUtil.getPackageName(fileName);
        return packageName;
    }

    @Nonnull
    @Override
    public ID<String, Void> getName() {
        return INDEX;
    }
}
