package consulo.devkit.icon;

import com.intellij.java.language.psi.JavaPsiFacade;
import com.intellij.java.language.psi.PsiClass;
import com.intellij.java.language.psi.PsiMethod;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.find.FindUsagesHandler;
import consulo.find.FindUsagesHandlerFactory;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.util.QualifiedName;
import consulo.module.content.DirectoryIndex;
import consulo.module.content.DirectoryInfo;
import consulo.util.io.FileUtil;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Locale;

/**
 * @author VISTALL
 * @since 2024-09-08
 */
@ExtensionImpl
public class IconFindUsagesHanderFactory extends FindUsagesHandlerFactory {
    private final DirectoryIndex myDirectoryIndex;

    @Inject
    public IconFindUsagesHanderFactory(DirectoryIndex directoryIndex) {
        myDirectoryIndex = directoryIndex;
    }

    @Override
    @RequiredReadAction
    public boolean canFindUsages(@Nonnull PsiElement element) {
        return findIconReference(myDirectoryIndex, element, true) != null;
    }

    @Nullable
    @RequiredReadAction
    public static Object findIconReference(@Nonnull DirectoryIndex directoryIndex, @Nonnull PsiElement element, boolean fast) {
        if (!(element instanceof PsiFile psiFile)) {
            return null;
        }

        String extension = FileUtil.getExtension(psiFile.getName());
        if (!IconLibraryIconDescriptorUpdater.ourAllowedExtensions.contains(extension)) {
            return null;
        }

        if (!IconLibraryChecker.containsImageApi(psiFile)) {
            return null;
        }

        VirtualFile virtualFile = psiFile.getVirtualFile();
        assert virtualFile != null;
        DirectoryInfo info = directoryIndex.getInfoForFile(virtualFile);

        if (!IconLibraryIconDescriptorUpdater.insideICON_LIB(info, virtualFile)) {
            return null;
        }

        if (fast) {
            // if fast - return file
            return virtualFile;
        }

        VirtualFile sourceRoot = info.getSourceRoot();
        if (sourceRoot == null) {
            return null;
        }

        String relativePath = VirtualFileUtil.getRelativePath(virtualFile, sourceRoot);
        if (StringUtil.isEmptyOrSpaces(relativePath)) {
            return null;
        }

        List<String> parts = StringUtil.split(relativePath, "/");
        if (parts.size() < 2) {
            return null;
        }

        QualifiedName qualifiedName = QualifiedName.fromDottedString(parts.get(2));

        String className = qualifiedName.getLastComponent();

        QualifiedName parent = qualifiedName.getParent();
        if (parent == null) {
            return null;
        }

        PsiClass iconGroupClass = JavaPsiFacade.getInstance(element.getProject())
            .findClass(parent.toString() + ".icon." + className, element.getResolveScope());

        if (iconGroupClass != null) {
            String prefix = "ICON-LIB/";
            String partWithTheme = relativePath.substring(prefix.length(), relativePath.length());
            // light/consulo.handlebars.HandlebarsIconGroup/handlebars_icon.svg
            int first = partWithTheme.indexOf('/', partWithTheme.indexOf('/') + 1);
            // handlebars_icon.svg
            String relativeInLibrary = partWithTheme.substring(first + 1, partWithTheme.length());

            // remove extension
            relativeInLibrary = relativeInLibrary.substring(0, relativeInLibrary.indexOf('.'));

            relativeInLibrary = relativeInLibrary.toLowerCase(Locale.ENGLISH);

            List<String> relativePaths = StringUtil.split(relativeInLibrary, "/");

            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < relativePaths.size(); i++) {
                if (i != 0) {
                    builder.append(StringUtil.capitalize(relativePaths.get(i)));
                }
                else {
                    builder.append(relativePaths.get(i));
                }
            }

            PsiMethod[] psiMethods = iconGroupClass.findMethodsByName(builder.toString(), false);
            if (psiMethods.length > 0) {
                PsiMethod method = psiMethods[0];

                return method;
            }
        }

        return null;
    }

    @Nullable
    @Override
    public FindUsagesHandler createFindUsagesHandler(@Nonnull PsiElement element, boolean forHighlightUsages) {
        return new IconFindUsagesHandler(myDirectoryIndex, element);
    }
}
