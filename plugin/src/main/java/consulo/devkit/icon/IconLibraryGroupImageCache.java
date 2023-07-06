package consulo.devkit.icon;

import com.intellij.java.impl.psi.util.ProjectIconsAccessor;
import com.intellij.java.language.psi.PsiClass;
import com.intellij.java.language.psi.PsiMethod;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.disposer.Disposable;
import consulo.language.content.ProductionResourceContentFolderTypeProvider;
import consulo.language.psi.PsiUtilCore;
import consulo.language.psi.util.QualifiedName;
import consulo.module.Module;
import consulo.module.content.ModuleRootManager;
import consulo.project.Project;
import consulo.ui.image.Image;
import consulo.util.collection.ArrayUtil;
import consulo.util.lang.Couple;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.archive.ArchiveVfsUtil;
import consulo.virtualFileSystem.event.BulkFileListener;
import consulo.virtualFileSystem.event.VFileEvent;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author VISTALL
 * @since 2020-10-15
 */
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
@Singleton
public class IconLibraryGroupImageCache implements Disposable {
  public static IconLibraryGroupImageCache getInstance(@Nonnull Project project) {
    return project.getInstance(IconLibraryGroupImageCache.class);
  }

  private final Map<Couple<String>, Pair<Image, VirtualFile>> myImageCache = new ConcurrentHashMap<>();

  private final ProjectIconsAccessor myProjectIconsAccessor;

  @Inject
  public IconLibraryGroupImageCache(Project project, ProjectIconsAccessor projectIconsAccessor) {
    myProjectIconsAccessor = projectIconsAccessor;

    project.getApplication().getMessageBus().connect(this).subscribe(BulkFileListener.class, new BulkFileListener() {
      @Override
      public void after(@Nonnull List<? extends VFileEvent> events) {
        myImageCache.clear();
      }
    });
  }

  @Nullable
  @RequiredReadAction
  public Pair<Image, VirtualFile> getImage(@Nonnull PsiMethod psiMethod) {
    Couple<String> iconRef = getIconRef(psiMethod);
    if (iconRef == null) {
      return null;
    }

    Pair<Image, VirtualFile> pair = myImageCache.computeIfAbsent(iconRef, ref -> {
      Pair<Image, VirtualFile> result = findImage(psiMethod, ref);
      return result == null ? Pair.empty() : result;
    });
    return pair == Pair.<Image, VirtualFile>empty() ? null : pair;
  }

  /**
   * @return pair icon path and name
   */
  @RequiredReadAction
  private Couple<String> getIconRef(@Nonnull PsiMethod psiMethod) {
    PsiClass containingClass = psiMethod.getContainingClass();
    if (containingClass == null) {
      return null;
    }

    String name = containingClass.getName();
    if (name == null || !name.endsWith(IconLibraryLineMarkerProvider.ICON_GROUP_SUFFIX)) {
      return null;
    }

    String qualifiedName = containingClass.getQualifiedName();
    if (qualifiedName == null) {
      return null;
    }

    QualifiedName qName = QualifiedName.fromDottedString(qualifiedName);
    String className = qName.getLastComponent();
    // remove className and icon package
    QualifiedName packageNoIcon = qName.removeTail(2);

    QualifiedName iconGroupDirectory = packageNoIcon.append(className);

    String iconLink = psiMethod.getName();
    if (!Objects.equals(qName.removeLastComponent().getLastComponent(), "icon")) {
      return null;
    }

    String[] iconRefs = iconLink.split("(?=\\p{Upper})");
    String iconId = ArrayUtil.getLastElement(iconRefs);
    // remove last
    iconRefs = ArrayUtil.remove(iconRefs, iconRefs.length - 1);

    String iconPath = "ICON-LIB/light/" + iconGroupDirectory + "/" + StringUtil.join(iconRefs, "/");
    return Couple.of(iconPath, iconId);
  }

  @RequiredReadAction
  @Nullable
  private Pair<Image, VirtualFile> findImage(@Nonnull PsiMethod psiMethod, @Nonnull Couple<String> iconRef) {
    Module module = psiMethod.getModule();
    VirtualFile[] contentFolders = VirtualFile.EMPTY_ARRAY;
    if (module == null) {
      VirtualFile holderFile = PsiUtilCore.getVirtualFile(psiMethod);
      // get root jar file
      holderFile = holderFile == null ? null : ArchiveVfsUtil.getVirtualFileForArchive(holderFile);
      // get root jar as directory
      holderFile = holderFile == null ? null : ArchiveVfsUtil.getArchiveRootForLocalFile(holderFile);
      if (holderFile != null) {
        contentFolders = new VirtualFile[]{holderFile};
      }
    }
    else {
      contentFolders = ModuleRootManager.getInstance(module)
                                        .getContentFolderFiles(it -> it == ProductionResourceContentFolderTypeProvider
                                          .getInstance());
    }

    for (VirtualFile contentFolderFile : contentFolders) {
      VirtualFile iconHolderDir = contentFolderFile.findFileByRelativePath(iconRef.getFirst());
      if (iconHolderDir == null) {
        continue;
      }

      VirtualFile iconFile = findChildByName(iconHolderDir, iconRef.getSecond());
      if (iconFile != null) {
        Image icon = myProjectIconsAccessor.getIcon(iconFile, psiMethod);
        return icon == null ? null : Pair.create(icon, iconFile);
      }
    }
    return null;
  }

  /**
   * Try find icon by name without case, since methods was always lower case
   */
  @Nullable
  private VirtualFile findChildByName(VirtualFile owner, String iconId) {
    VirtualFile[] children = owner.getChildren();

    VirtualFile pngIcon = null;
    for (VirtualFile child : children) {
      String name = child.getNameWithoutExtension();

      if (!iconId.equalsIgnoreCase(name)) {
        continue;
      }

      String extension = child.getExtension();
      if ("svg".equals(extension)) {
        return child;
      }

      if ("png".equals(extension)) {
        pngIcon = child;
      }
    }

    return pngIcon;
  }

  @Override
  public void dispose() {
    myImageCache.clear();
  }
}
