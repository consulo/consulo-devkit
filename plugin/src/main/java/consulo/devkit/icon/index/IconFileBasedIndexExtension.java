package consulo.devkit.icon.index;

import consulo.annotation.component.ExtensionImpl;
import consulo.index.io.*;
import consulo.index.io.data.DataExternalizer;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.language.psi.stub.FileBasedIndexExtension;
import consulo.language.psi.stub.FileContent;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2020-10-15
 */
@ExtensionImpl
public class IconFileBasedIndexExtension extends FileBasedIndexExtension<String, Void> {
  public static final ID<String, Void> INDEX = ID.create("consulo.icon.file.index");

  private static final Set<String> ourAllowedExtensions = Set.of("svg", "png");

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
      VirtualFile lightDirectory = findLightDirectory(fileContent.getFile());
      if (lightDirectory == null) {
        return Map.of();
      }

      String relativeLocation = VirtualFileUtil.getRelativeLocation(fileContent.getFile(), lightDirectory);
      if (relativeLocation == null) {
        return Map.of();
      }

      for (String extension : ourAllowedExtensions) {
        relativeLocation = StringUtil.trimEnd(relativeLocation, "." + extension);
      }

      if (relativeLocation.endsWith("@2x")) {
        return Map.of();
      }

      String groupId = relativeLocation.substring(0, relativeLocation.indexOf('/'));

      String imageId = relativeLocation.substring(groupId.length() + 1, relativeLocation.length()).replace("/", ".").replace("-", "_");

      return Collections.singletonMap(groupId + "@" + imageId.toLowerCase(Locale.ROOT), null);
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
    return 6;
  }

  @Nonnull
  @Override
  public FileBasedIndex.InputFilter getInputFilter() {
    return (project, virtualFile) -> {
      String extension = virtualFile.getExtension();
      return extension != null && ourAllowedExtensions.contains(extension);
    };
  }

  @Override
  public boolean dependsOnFileContent() {
    return false;
  }

  @Nullable
  private static VirtualFile findLightDirectory(VirtualFile file) {
    VirtualFile parent = file;
    while ((parent = parent.getParent()) != null) {
      if (StringUtil.equals(parent.getNameSequence(), "_light")) {
        break;
      }
    }

    if (parent != null && StringUtil.equals(parent.getNameSequence(), "_light")) {
      VirtualFile iconDirectory = parent.getParent();
      if (iconDirectory != null && StringUtil.equals(iconDirectory.getNameSequence(), "icon")) {
        VirtualFile markerFile = iconDirectory.findChild("marker.txt");
        if (markerFile != null) {
          return parent;
        }
      }
    }

    return null;
  }
}
