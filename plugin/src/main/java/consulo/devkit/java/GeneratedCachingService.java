package consulo.devkit.java;

import com.intellij.java.language.psi.PsiClass;
import com.intellij.java.language.psi.PsiJavaPackage;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.disposer.Disposable;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiModificationTrackerListener;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.project.Project;
import consulo.util.lang.ObjectUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2025-10-04
 */
@Singleton
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public class GeneratedCachingService implements Disposable {
    public record CachingKey(String qName, GlobalSearchScope searchScope) {

    }

    private Map<CachingKey, Object> myCachedClasses = new ConcurrentHashMap<>();
    private Map<CachingKey, Object> myCachedPackages = new ConcurrentHashMap<>();

    @Inject
    public GeneratedCachingService(Project project) {
        project.getMessageBus().connect().subscribe(PsiModificationTrackerListener.class, this::dropCache);
    }

    public PsiClass getClass(String className, GlobalSearchScope searchScope, @RequiredReadAction Supplier<PsiClass> builder) {
        return getOrBuild(myCachedClasses, className, searchScope, builder);
    }

    public PsiJavaPackage getPackage(String className, GlobalSearchScope searchScope, @RequiredReadAction Supplier<PsiJavaPackage> builder) {
        return getOrBuild(myCachedPackages, className, searchScope, builder);
    }

    @SuppressWarnings("unchecked")
    private static <T extends PsiElement> T getOrBuild(Map<CachingKey, Object> map,
                                                       String qName,
                                                       GlobalSearchScope searchScope,
                                                       @RequiredReadAction Supplier<T> builder) {
        CachingKey key = new CachingKey(qName, searchScope);
        Object element = map.get(key);
        if (element != null) {
            return element == ObjectUtil.NULL ? null : (T) element;
        }

        element = builder.get();
        map.putIfAbsent(key, element == null ? ObjectUtil.NULL : element);
        return (T) element;
    }

    private void dropCache() {
        myCachedClasses.clear();
        myCachedPackages.clear();
    }

    @Override
    public void dispose() {
        dropCache();
    }
}
