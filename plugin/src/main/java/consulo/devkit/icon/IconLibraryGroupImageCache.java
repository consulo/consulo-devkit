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
import consulo.language.psi.util.QualifiedName;
import consulo.language.util.ModuleUtilCore;
import consulo.module.Module;
import consulo.module.content.ModuleRootManager;
import consulo.project.Project;
import consulo.ui.image.Image;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
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

  private final Map<String, Pair<Image, VirtualFile>> myImageCache = new ConcurrentHashMap<>();

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
    String iconPath = getIconPath(psiMethod);
    if (iconPath == null) {
      return null;
    }

    Pair<Image, VirtualFile> pair = myImageCache.computeIfAbsent(iconPath, s -> {
      Pair<Image, VirtualFile> result = findImage(psiMethod, s);
      return result == null ? Pair.empty() : result;
    });
    return pair == Pair.<Image, VirtualFile>empty() ? null : pair;
  }

  @RequiredReadAction
  private String getIconPath(@Nonnull PsiMethod psiMethod) {
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

    Module module = ModuleUtilCore.findModuleForPsiElement(containingClass);
    if (module == null) {
      return null;
    }

    String iconPathNoExt = StringUtil.join(iconLink.split("(?=\\p{Upper})"), "/");

    String libPath = "ICON-LIB/light/" + iconGroupDirectory;
    return libPath + "/" + iconPathNoExt;
  }

  @RequiredReadAction
  @Nullable
  private Pair<Image, VirtualFile> findImage(@Nonnull PsiMethod psiMethod, @Nonnull String iconPath) {
    Module module = Objects.requireNonNull(ModuleUtilCore.findModuleForPsiElement(psiMethod));

    VirtualFile[] contentFolderFiles = ModuleRootManager.getInstance(module)
                                                        .getContentFolderFiles(it -> it == ProductionResourceContentFolderTypeProvider
                                                          .getInstance());


    for (VirtualFile contentFolderFile : contentFolderFiles) {
      VirtualFile iconFile = contentFolderFile.findFileByRelativePath(iconPath + ".png");
      if (iconFile == null) {
        iconFile = contentFolderFile.findFileByRelativePath(iconPath + ".svg");
      }

      if (iconFile == null) {
        continue;
      }

      Image icon = myProjectIconsAccessor.getIcon(iconFile, psiMethod);
      return icon == null ? null : Pair.create(icon, iconFile);
    }

    return null;
  }

  @Override
  public void dispose() {
    myImageCache.clear();
  }
}
