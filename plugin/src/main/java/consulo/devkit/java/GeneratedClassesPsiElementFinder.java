package consulo.devkit.java;

import com.intellij.java.language.psi.PsiClass;
import com.intellij.java.language.psi.PsiElementFinder;
import com.intellij.java.language.psi.PsiJavaPackage;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.devkit.localize.index.LocalizeFileIndexExtension;
import consulo.devkit.localize.index.LocalizeFilePackageIndexExtension;
import consulo.devkit.localize.java.LocalizeClassBuilder;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.psi.PsiPackageManager;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.project.Project;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.StringUtil;
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
    private static final String SUFFIX = ".localize";

    private final Project myProject;
    private final FileBasedIndex myFileBasedIndex;
    private final PsiManager myPsiManager;
    private final GeneratedCachingService myGeneratedCachingService;

    @Inject
    public GeneratedClassesPsiElementFinder(Project project,
                                            FileBasedIndex fileBasedIndex,
                                            PsiManager psiManager,
                                            GeneratedCachingService generatedCachingService) {
        myProject = project;
        myFileBasedIndex = fileBasedIndex;
        myPsiManager = psiManager;
        myGeneratedCachingService = generatedCachingService;
    }

    @Nullable
    @Override
    @RequiredReadAction
    public PsiClass findClass(@Nonnull String className, @Nonnull GlobalSearchScope globalSearchScope) {
        return myGeneratedCachingService.getClass(className, globalSearchScope, () -> {
            Collection<VirtualFile> containingFiles = myFileBasedIndex.getContainingFiles(
                LocalizeFileIndexExtension.INDEX,
                className,
                globalSearchScope
            );

            if (containingFiles.isEmpty()) {
                return null;
            }

            VirtualFile file = ContainerUtil.getFirstItem(containingFiles);

            PsiFile psiFile = myPsiManager.findFile(file);
            if (psiFile instanceof YAMLFile yamlFile) {
                return new LocalizeClassBuilder(yamlFile, className);
            }
            return null;
        });
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
        if (StringUtil.isEmpty(qualifiedName)) {
            return null;
        }

        if (qualifiedName.endsWith(SUFFIX)) {
            String localizePackage = qualifiedName.substring(0, qualifiedName.length() - SUFFIX.length());

            return myGeneratedCachingService.getPackage(qualifiedName, GlobalSearchScope.projectScope(myProject), () -> {
                Collection<String> keys = myFileBasedIndex.getAllKeys(LocalizeFilePackageIndexExtension.INDEX, myProject);
                if (keys.contains(localizePackage)) {
                    return new GeneratedPackageImpl(myPsiManager, PsiPackageManager.getInstance(myProject), qualifiedName);
                }
                return null;
            });
        }
        return null;
    }

    @Nonnull
    @Override
    public PsiJavaPackage[] getSubPackages(@Nonnull PsiJavaPackage psiPackage, @Nonnull GlobalSearchScope scope) {
        String qName = psiPackage.getQualifiedName();
        if (StringUtil.isEmpty(qName)) {
            return PsiJavaPackage.EMPTY_ARRAY;
        }

        PsiJavaPackage javaPackage = myGeneratedCachingService.getPackage(qName, scope, () -> {
            Collection<VirtualFile> files = myFileBasedIndex.getContainingFiles(LocalizeFilePackageIndexExtension.INDEX, qName, scope);
            if (!files.isEmpty()) {
                String childPackage = qName + SUFFIX;
                return new GeneratedPackageImpl(myPsiManager, PsiPackageManager.getInstance(myProject), childPackage);
            }
            return null;
        });

        if (javaPackage != null) {
            return new PsiJavaPackage[]{javaPackage};
        }

        return PsiJavaPackage.EMPTY_ARRAY;
    }
}
