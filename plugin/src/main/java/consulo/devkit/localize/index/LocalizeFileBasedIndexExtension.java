package consulo.devkit.localize.index;

import consulo.annotation.component.ExtensionImpl;
import consulo.index.io.*;
import consulo.index.io.data.DataExternalizer;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.language.psi.stub.FileBasedIndexExtension;
import consulo.language.psi.stub.FileContent;
import consulo.logging.Logger;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import org.jetbrains.yaml.YAMLFileType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;

/**
 * @author VISTALL
 * @since 2020-06-01
 */
@ExtensionImpl
public class LocalizeFileBasedIndexExtension extends FileBasedIndexExtension<String, Void> {
  private static final Logger LOG = Logger.getInstance(LocalizeFileBasedIndexExtension.class);

  public static final ID<String, Void> INDEX = ID.create("consulo.localize.file.index");

  @Nonnull
  @Override
  public ID<String, Void> getName() {
    return INDEX;
  }

  @Nonnull
  @Override
  public DataIndexer<String, Void, FileContent> getIndexer() {
    return fileContent ->
    {
      VirtualFile file = fileContent.getFile();

      if (isNewLocalize(file)) {
        String fileName = file.getNameWithoutExtension();

        String packageName = StringUtil.getPackageName(fileName);

        String id = packageName + ".localize." + StringUtil.getShortName(fileName);
        return Collections.singletonMap(id, null);
      }
      else {
        VirtualFile localizeDirectory = findLocalizeDirectory(file);
        if (localizeDirectory == null) {
          return Collections.emptyMap();
        }

        String relativeLocation = VirtualFileUtil.getRelativeLocation(file.getParent(), localizeDirectory);

        if (relativeLocation == null) {
          return Collections.emptyMap();
        }

        // com/intellij/images

        relativeLocation = relativeLocation.replace("/", ".");

        String id = relativeLocation + ".localize." + file.getNameWithoutExtension();

        return Collections.singletonMap(id, null);
      }
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
    return (project, file) ->
    {
      if (file.getFileType() != YAMLFileType.YML) {
        return false;
      }

      CharSequence nameSequence = file.getNameSequence();
      if (!StringUtil.endsWith(nameSequence, "Localize.yaml")) {
        return false;
      }

      VirtualFile localizeDirectory = findLocalizeDirectory(file);
      if (localizeDirectory != null) {
        VirtualFile idTxt = localizeDirectory.findChild("id.txt");
        if (idTxt == null) {
          return false;
        }

        CharSequence text = idTxt.loadText();
        if (StringUtil.equals("en", text)) {
          return true;
        }
      }
      else {
        return isNewLocalize(file);
      }
      return false;
    };
  }

  private boolean isNewLocalize(VirtualFile file) {
    VirtualFile parentDir = file.getParent();
    if (parentDir != null && "en_US".equals(parentDir.getName())) {
      VirtualFile localizeLibParent = parentDir.getParent();
      if (localizeLibParent != null && "LOCALIZE-LIB".equals(localizeLibParent.getName())) {
        return true;
      }
    }
    return false;
  }

  @Nullable
  private static VirtualFile findLocalizeDirectory(VirtualFile file) {
    VirtualFile parent = file;
    while ((parent = parent.getParent()) != null) {
      if (StringUtil.equals(parent.getNameSequence(), "localize")) {
        return parent;
      }
    }

    return null;
  }

  @Override
  public boolean dependsOnFileContent() {
    return false;
  }
}
