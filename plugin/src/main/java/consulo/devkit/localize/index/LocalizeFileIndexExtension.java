package consulo.devkit.localize.index;

import consulo.annotation.component.ExtensionImpl;
import consulo.devkit.localize.LocalizeUtil;
import consulo.index.io.*;
import consulo.index.io.data.DataExternalizer;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.language.psi.stub.FileBasedIndexExtension;
import consulo.language.psi.stub.FileContent;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.yaml.YAMLFileType;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2020-06-01
 */
@ExtensionImpl
public class LocalizeFileIndexExtension extends FileBasedIndexExtension<String, Void> {
    public static final ID<String, Void> INDEX = ID.create("consulo.localize.file.index");

    @Nonnull
    @Override
    public ID<String, Void> getName() {
        return INDEX;
    }

    @Nonnull
    @Override
    public DataIndexer<String, Void, FileContent> getIndexer() {
        return fileContent -> {
            VirtualFile file = fileContent.getFile();

            if (LocalizeUtil.isLocalizeFile(file)) {
                String fileName = file.getNameWithoutExtension();
                String packageName = StringUtil.getPackageName(fileName);
                String id = packageName + ".localize." + StringUtil.getShortName(fileName);
                return Collections.singletonMap(id, null);
            }

            return Map.of();
        };
    }

    @Nonnull
    @Override
    public KeyDescriptor<String> getKeyDescriptor() {
        return EnumeratorStringDescriptor.INSTANCE;
    }

    @Nonnull
    @Override
    public DataExternalizer<Void> getValueExternalizer() {
        return VoidDataExternalizer.INSTANCE;
    }

    @Override
    public int getVersion() {
        return 4;
    }

    @Nonnull
    @Override
    public FileBasedIndex.InputFilter getInputFilter() {
        return (project, file) -> {
            return file.getFileType() == YAMLFileType.YML && LocalizeUtil.isLocalizeFile(file);
        };
    }

    @Override
    public boolean dependsOnFileContent() {
        return false;
    }
}
