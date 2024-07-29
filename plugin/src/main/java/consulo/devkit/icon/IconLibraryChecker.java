package consulo.devkit.icon;

import com.intellij.java.language.psi.JavaPsiFacade;
import com.intellij.java.language.psi.PsiClass;
import consulo.annotation.access.RequiredReadAction;
import consulo.application.util.CachedValueProvider;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.util.LanguageCachedValueUtil;
import consulo.module.content.ProjectRootManager;
import consulo.project.Project;
import consulo.ui.image.ImageKey;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 24-Jul-24
 */
public class IconLibraryChecker {
    @RequiredReadAction
    public static boolean containsImageApi(@Nonnull PsiElement psiElement) {
        PsiFile containingFile = psiElement.getContainingFile();
        if (containingFile == null) {
            return false;
        }

        return LanguageCachedValueUtil.getCachedValue(
            containingFile,
            () -> {
                Project project = containingFile.getProject();
                PsiClass psiClass =
                    JavaPsiFacade.getInstance(project).findClass(ImageKey.class.getName(), containingFile.getResolveScope());
                return CachedValueProvider.Result.create(psiClass != null, ProjectRootManager.getInstance(project));
            }
        );
    }
}
