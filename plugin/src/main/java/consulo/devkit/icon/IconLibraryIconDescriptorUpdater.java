package consulo.devkit.icon;

import com.intellij.java.impl.psi.util.ProjectIconsAccessor;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.icon.IconDescriptor;
import consulo.language.icon.IconDescriptorUpdater;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.module.content.DirectoryIndex;
import consulo.module.content.DirectoryInfo;
import consulo.ui.ex.IconDeferrer;
import consulo.ui.image.Image;
import consulo.util.io.FileUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

import java.util.Set;

/**
 * @author VISTALL
 * @since 2024-05-26
 */
@ExtensionImpl
public class IconLibraryIconDescriptorUpdater implements IconDescriptorUpdater {
    public static final Set<String> ourAllowedExtensions = Set.of("svg", "png");

    private final DirectoryIndex myDirectoryIndex;
    private final IconDeferrer myIconDeferrer;
    private final ProjectIconsAccessor myProjectIconsAccessor;

    @Inject
    public IconLibraryIconDescriptorUpdater(
        DirectoryIndex directoryIndex,
        IconDeferrer iconDeferrer,
        ProjectIconsAccessor projectIconsAccessor
    ) {
        myDirectoryIndex = directoryIndex;
        myIconDeferrer = iconDeferrer;
        myProjectIconsAccessor = projectIconsAccessor;
    }

    @RequiredReadAction
    @Override
    public void updateIcon(@Nonnull IconDescriptor iconDescriptor, @Nonnull PsiElement psiElement, int flags) {
        if (psiElement instanceof PsiFile psiFile) {
            String extension = FileUtil.getExtension(psiFile.getName());
            if (!ourAllowedExtensions.contains(extension)) {
                return;
            }

            if (!IconLibraryChecker.containsImageApi(psiElement)) {
                return;
            }

            VirtualFile virtualFile = psiFile.getVirtualFile();
            assert virtualFile != null;
            DirectoryInfo info = myDirectoryIndex.getInfoForFile(virtualFile);

            if (!insideICON_LIB(info, virtualFile)) {
                return;
            }

            Image mainIcon = iconDescriptor.getMainIcon();
            if (mainIcon == null) {
                mainIcon = virtualFile.getFileType().getIcon();
            }

            final Image finalMainIcon = mainIcon;
            iconDescriptor.setMainIcon(myIconDeferrer.defer(mainIcon, virtualFile, file ->
            {
                Image image = myProjectIconsAccessor.getIcon(file, psiElement);
                if (image == null) {
                    image = finalMainIcon;
                }
                return image;
            }));
        }
    }

    public static boolean insideICON_LIB(DirectoryInfo info, VirtualFile virtualFile) {
        VirtualFile contentRoot = info.getSourceRoot();
        if (contentRoot != null) {
            String relativePath = VirtualFileUtil.getRelativePath(virtualFile, contentRoot);
            return relativePath != null && relativePath.startsWith("ICON-LIB/");
        }

        VirtualFile libraryClassRoot = info.getLibraryClassRoot();
        if (libraryClassRoot != null) {
            String relativePath = VirtualFileUtil.getRelativePath(virtualFile, libraryClassRoot);
            return relativePath != null && relativePath.startsWith("ICON-LIB/");
        }

        return false;
    }
}
