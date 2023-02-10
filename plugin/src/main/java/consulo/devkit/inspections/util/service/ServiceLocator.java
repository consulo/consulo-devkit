package consulo.devkit.inspections.util.service;

import com.intellij.java.language.codeInsight.AnnotationUtil;
import com.intellij.java.language.psi.PsiAnnotation;
import com.intellij.java.language.psi.PsiClass;
import consulo.annotation.access.RequiredReadAction;
import consulo.application.util.CachedValueProvider;
import consulo.application.util.CachedValuesManager;
import consulo.devkit.inspections.valhalla.ValhallaClasses;
import consulo.language.psi.PsiModificationTracker;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.project.Project;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2018-08-16
 */
public class ServiceLocator {
  @Nullable
  @RequiredReadAction
  public static ServiceInfo findAnyService(@Nonnull PsiClass psiClass) {
    String qualifiedName = psiClass.getQualifiedName();
    if (qualifiedName == null) {
      return null;
    }

    List<ServiceInfo> services = ServiceLocator.getServices(psiClass.getProject());
    for (ServiceInfo service : services) {
      if (qualifiedName.equals(service.getImplementation()) || qualifiedName.equals(service.getInterface())) {
        return service;
      }
    }
    return null;
  }

  @Nullable
  @RequiredReadAction
  public static ServiceInfo findImplementationService(@Nonnull PsiClass psiClass) {
    String qualifiedName = psiClass.getQualifiedName();
    if (qualifiedName == null) {
      return null;
    }

    if (AnnotationUtil.isAnnotated(psiClass, ValhallaClasses.ServiceImpl, 0)) {
      PsiAnnotation apiAnnotation = AnnotationUtil.findAnnotationInHierarchy(psiClass, Set.of(ValhallaClasses.ServiceAPI));
      if (apiAnnotation != null) {
        PsiClass apiClass = PsiTreeUtil.getParentOfType(apiAnnotation, PsiClass.class);
        if (apiClass != null) {
          return new ServiceInfo(apiClass.getQualifiedName(), psiClass.getQualifiedName(), apiClass);
        }
      }
    }

    List<ServiceInfo> services = ServiceLocator.getServices(psiClass.getProject());
    for (ServiceInfo service : services) {
      if (qualifiedName.equals(service.getImplementation())) {
        return service;
      }
    }
    return null;
  }

  @RequiredReadAction
  public static List<ServiceInfo> getServices(Project project) {
    return CachedValuesManager.getManager(project).getCachedValue(project, () ->
      CachedValueProvider.Result.create(getServicesImpl(project), PsiModificationTracker.MODIFICATION_COUNT));
  }

  @Nonnull
  @RequiredReadAction
  private static List<ServiceInfo> getServicesImpl(Project project) {
    return List.of();
  }
}
