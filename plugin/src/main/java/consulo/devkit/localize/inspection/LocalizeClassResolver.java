package consulo.devkit.localize.inspection;

import com.intellij.java.language.codeInsight.AnnotationUtil;
import com.intellij.java.language.psi.*;
import com.intellij.java.language.psi.search.PsiShortNamesCache;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.internal.MigratedExtensionsTo;
import consulo.application.util.CachedValueProvider;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiModificationTracker;
import consulo.language.psi.util.LanguageCachedValueUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Resolving xLocalize class by xBundle class using cache.
 *
 * @author <a href="mailto:nikolay@yurchenko.su">Nikolay Yurchenko</a>
 * @since 16.06.2024
 */
public class LocalizeClassResolver implements CachedValueProvider<PsiClass> {
  @Nonnull
  private final PsiClass myBundlePsiClass;
  @Nonnull
  private PsiElement myContext;

  private LocalizeClassResolver(@Nonnull PsiClass bundlePsiClass, @Nonnull PsiElement context) {
    myBundlePsiClass = bundlePsiClass;
    myContext = context;
  }

  public static PsiClass resolveByBundle(PsiClass bundlePsiClass, PsiElement context) {
    return LanguageCachedValueUtil.getCachedValue(bundlePsiClass, new LocalizeClassResolver(bundlePsiClass, context));
  }

  @Nullable
  @Override
  @RequiredReadAction
  public Result<PsiClass> compute() {
    return Result.create(getLocalizeClass(), PsiModificationTracker.MODIFICATION_COUNT);
  }

  @RequiredReadAction
  private PsiClass getLocalizeClass() {
    PsiClass localizePsiClass = getLocalizeClassByAnnotation();
    if (localizePsiClass != null) {
      return localizePsiClass;
    }

    String bundleClassName = myBundlePsiClass.getName();
    if (bundleClassName == null) {
      return null;
    }

    String localizeClassName =
      bundleClassName.substring(0, bundleClassName.length() - BundleMessageToLocalizeInspection.BUNDLE_SUFFIX.length())
        + BundleMessageToLocalizeInspection.LOCALIZE_SUFFIX;

    PsiClass[] classes =
      PsiShortNamesCache.getInstance(myBundlePsiClass.getProject()).getClassesByName(localizeClassName, myContext.getResolveScope());
    return (classes.length == 1) ? classes[0] : null;
  }

  @RequiredReadAction
  private PsiClass getLocalizeClassByAnnotation() {
    PsiAnnotation migrateAnnotation = AnnotationUtil.findAnnotation(myBundlePsiClass, MigratedExtensionsTo.class.getName());
    if (migrateAnnotation == null) {
      return null;
    }

    PsiNameValuePair[] attributes = migrateAnnotation.getParameterList().getAttributes();
    if (attributes.length != 1) {
      return null;
    }

    PsiAnnotationMemberValue value = attributes[0].getValue();

    if (value instanceof PsiClassObjectAccessExpression classObjectAccessExpression) {
      PsiType type = classObjectAccessExpression.getOperand().getType();
      if (type instanceof PsiClassType classType) {
        return classType.resolve();
      }
    }
    return null;
  }
}
