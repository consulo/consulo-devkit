package consulo.devkit.java;

import com.intellij.java.language.psi.PsiClass;
import com.intellij.java.language.psi.PsiElementFinder;
import com.intellij.java.language.psi.PsiJavaPackage;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.devkit.localize.index.LocalizeFileIndexExtension;
import consulo.devkit.localize.java.LocalizeClassBuilder;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.psi.PsiPackageManager;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.project.Project;
import consulo.util.collection.ContainerUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import org.jetbrains.yaml.psi.YAMLFile;

import java.util.Collection;

/**
 * @author VISTALL
 * @since 2025-10-02
 */
@ExtensionImpl
public class GeneratedClassesPsiElementFinder extends PsiElementFinder {
    private final Project myProject;
    private final FileBasedIndex myFileBasedIndex;
    private final PsiManager myPsiManager;

    @Inject
    public GeneratedClassesPsiElementFinder(Project project,
                                            FileBasedIndex fileBasedIndex,
                                            PsiManager psiManager) {
        myProject = project;
        myFileBasedIndex = fileBasedIndex;
        myPsiManager = psiManager;
    }

    @Nullable
    @Override
    @RequiredReadAction
    public PsiClass findClass(@Nonnull String className, @Nonnull GlobalSearchScope globalSearchScope) {

        Collection<VirtualFile> containingFiles = myFileBasedIndex.getContainingFiles(
            LocalizeFileIndexExtension.INDEX,
            className,
            globalSearchScope
        );

        if (!containingFiles.isEmpty()) {
            VirtualFile file = ContainerUtil.getFirstItem(containingFiles);

            PsiFile psiFile = myPsiManager.findFile(file);
            if (psiFile instanceof YAMLFile yamlFile) {
                return new LocalizeClassBuilder(yamlFile, className);
            }
        }

        return null;
    }

    @Nonnull
    @Override
    @RequiredReadAction
    public PsiClass[] findClasses(@Nonnull String className, @Nonnull GlobalSearchScope globalSearchScope) {
        PsiClass psiClass = findClass(className, globalSearchScope);
        if (psiClass != null) {
            return new PsiClass[]{psiClass};
        }
        return PsiClass.EMPTY_ARRAY;
    }

    @Nullable
    @Override
    public PsiJavaPackage findPackage(@Nonnull String qualifiedName) {
        Collection<String> keys = myFileBasedIndex.getAllKeys(LocalizeFileIndexExtension.INDEX, myProject);
        for (String key : keys) {
            if (key.startsWith(qualifiedName)) {
                return new GeneratedPackageImpl(myPsiManager, PsiPackageManager.getInstance(myProject), key);
            }
        }
        return super.findPackage(qualifiedName);
    }
}
