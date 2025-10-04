package consulo.devkit.localize.index;

import consulo.devkit.localize.LocalizeUtil;
import consulo.index.io.DataIndexer;
import consulo.index.io.EnumeratorStringDescriptor;
import consulo.index.io.KeyDescriptor;
import consulo.index.io.VoidDataExternalizer;
import consulo.index.io.data.DataExternalizer;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.language.psi.stub.FileBasedIndexExtension;
import consulo.language.psi.stub.FileContent;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import org.jetbrains.yaml.YAMLFileType;

import java.util.Collections;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2025-10-04
 */
public abstract class BaseLocalizeFileIndexExtension extends FileBasedIndexExtension<String, Void> {

    @Nonnull
    @Override
    public DataIndexer<String, Void, FileContent> getIndexer() {
        return fileContent -> {
            VirtualFile file = fileContent.getFile();

            if (LocalizeUtil.isDefaultLocalizeFile(file)) {
                return Collections.singletonMap(getFileId(file), null);
            }

            return Map.of();
        };
    }

    protected abstract String getFileId(VirtualFile file);

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
            return file.getFileType() == YAMLFileType.YML && LocalizeUtil.isDefaultLocalizeFile(file);
        };
    }

    @Override
    public boolean dependsOnFileContent() {
        return false;
    }
}