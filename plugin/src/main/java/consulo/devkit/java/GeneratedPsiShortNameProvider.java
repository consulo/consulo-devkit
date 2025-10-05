package consulo.devkit.java;

import com.intellij.java.language.psi.JavaPsiFacade;
import com.intellij.java.language.psi.PsiClass;
import com.intellij.java.language.psi.PsiField;
import com.intellij.java.language.psi.PsiMethod;
import com.intellij.java.language.psi.search.PsiShortNameProvider;
import consulo.annotation.component.ExtensionImpl;
import consulo.devkit.localize.index.LocalizeFileIndexExtension;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.language.psi.stub.IdFilter;
import consulo.project.Project;
import consulo.util.collection.ArrayUtil;
import consulo.util.lang.StringUtil;
import consulo.util.lang.ref.SimpleReference;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 2025-10-05
 */
@ExtensionImpl
public class GeneratedPsiShortNameProvider implements PsiShortNameProvider {
    private final Project myProject;
    private final FileBasedIndex myFileBasedIndex;
    private final JavaPsiFacade myJavaPsiFacade;

    @Inject
    public GeneratedPsiShortNameProvider(Project project,
                                         FileBasedIndex fileBasedIndex,
                                         JavaPsiFacade javaPsiFacade) {
        myProject = project;
        myFileBasedIndex = fileBasedIndex;
        myJavaPsiFacade = javaPsiFacade;
    }

    @Nonnull
    @Override
    public PsiClass[] getClassesByName(@Nonnull String name, @Nonnull GlobalSearchScope scope) {
        List<PsiClass> psiClasses = new ArrayList<>();
        processClassesWithName(name, psiClasses::add, scope, IdFilter.getProjectIdFilter(myProject, true));
        return psiClasses.toArray(PsiClass.EMPTY_ARRAY);
    }

    @Override
    public boolean processClassesWithName(@Nonnull String name,
                                          @Nonnull Predicate<? super PsiClass> processor,
                                          @Nonnull GlobalSearchScope scope,
                                          @Nullable IdFilter filter) {
        SimpleReference<String> fullNameRef = new SimpleReference<>();
        myFileBasedIndex.processAllKeys(LocalizeFileIndexExtension.INDEX, key -> {
            String shortName = StringUtil.getShortName(key);

            if (name.equals(shortName)) {
                fullNameRef.set(key);
                return false;
            }
            return true;
        }, scope, filter);

        String targetLocalize = fullNameRef.get();
        if (StringUtil.isEmpty(targetLocalize)) {
            return true;
        }

        PsiClass psiClass = myJavaPsiFacade.findClass(targetLocalize, scope);
        return psiClass == null || processor.test(psiClass);
    }

    @Nonnull
    @Override
    public String[] getAllClassNames() {
        HashSet<String> set = new HashSet<>();
        getAllClassNames(set);
        return ArrayUtil.toStringArray(set);
    }

    @Override
    public void getAllClassNames(@Nonnull HashSet<String> dest) {
        Collection<String> keys = myFileBasedIndex.getAllKeys(LocalizeFileIndexExtension.INDEX, myProject);
        for (String key : keys) {
            String shortName = StringUtil.getShortName(key);
            if (StringUtil.isEmpty(shortName)) {
                continue;
            }

            dest.add(shortName);
        }
    }

    @Nonnull
    @Override
    public PsiMethod[] getMethodsByName(@Nonnull String name, @Nonnull GlobalSearchScope scope) {
        return PsiMethod.EMPTY_ARRAY;
    }

    @Nonnull
    @Override
    public PsiMethod[] getMethodsByNameIfNotMoreThan(@Nonnull String name, @Nonnull GlobalSearchScope scope, int maxCount) {
        return PsiMethod.EMPTY_ARRAY;
    }

    @Nonnull
    @Override
    public PsiField[] getFieldsByNameIfNotMoreThan(@Nonnull String name, @Nonnull GlobalSearchScope scope, int maxCount) {
        return PsiField.EMPTY_ARRAY;
    }

    @Override
    public boolean processMethodsWithName(@Nonnull String name, @Nonnull GlobalSearchScope scope, @Nonnull Predicate<PsiMethod> processor) {
        return true;
    }

    @Override
    public boolean processMethodsWithName(@Nonnull String name, @Nonnull Predicate<? super PsiMethod> processor, @Nonnull GlobalSearchScope scope, @Nullable IdFilter filter) {
        return true;
    }

    @Nonnull
    @Override
    public String[] getAllMethodNames() {
        return ArrayUtil.EMPTY_STRING_ARRAY;
    }

    @Override
    public void getAllMethodNames(@Nonnull HashSet<String> set) {

    }

    @Nonnull
    @Override
    public PsiField[] getFieldsByName(@Nonnull String name, @Nonnull GlobalSearchScope scope) {
        return PsiField.EMPTY_ARRAY;
    }

    @Nonnull
    @Override
    public String[] getAllFieldNames() {
        return ArrayUtil.EMPTY_STRING_ARRAY;
    }

    @Override
    public void getAllFieldNames(@Nonnull HashSet<String> set) {

    }

    @Override
    public boolean processFieldsWithName(@Nonnull String name, @Nonnull Predicate<? super PsiField> processor, @Nonnull GlobalSearchScope scope, @Nullable IdFilter filter) {
        return true;
    }
}
