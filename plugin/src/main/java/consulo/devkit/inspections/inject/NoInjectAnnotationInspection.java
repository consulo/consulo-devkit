package consulo.devkit.inspections.inject;

import com.intellij.java.language.codeInsight.AnnotationUtil;
import com.intellij.java.language.psi.JavaElementVisitor;
import com.intellij.java.language.psi.PsiClass;
import com.intellij.java.language.psi.PsiMethod;
import com.intellij.java.language.psi.PsiModifier;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.devkit.inspections.util.service.ServiceInfo;
import consulo.devkit.inspections.util.service.ServiceLocator;
import consulo.devkit.inspections.valhalla.ValhallaClasses;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.psi.PsiElementVisitor;
import org.jetbrains.idea.devkit.inspections.internal.InternalInspection;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * @author VISTALL
 * @since 2018-09-02
 */
@ExtensionImpl
public class NoInjectAnnotationInspection extends InternalInspection {
  public static final List<String> INJECT_ANNOTATIONS = List.of("jakarta.inject.Inject");

  private static class Visitor extends JavaElementVisitor {
    private final ProblemsHolder myHolder;

    public Visitor(ProblemsHolder holder) {
      myHolder = holder;
    }

    @Override
    @RequiredReadAction
    public void visitClass(PsiClass aClass) {
      if (!isInjectionTarget(aClass)) {
        return;
      }

      PsiMethod[] constructors = aClass.getConstructors();
      if (constructors.length == 0) {
        // default constructor
        if (aClass.hasModifierProperty(PsiModifier.PUBLIC)) {
          return;
        }
      }
      else {
        PsiMethod defaultConstructor = null;
        for (PsiMethod constructor : constructors) {
          if (constructor.hasModifierProperty(PsiModifier.PUBLIC) && constructor.getParameterList().getParametersCount() == 0) {
            defaultConstructor = constructor;
          }

          if (AnnotationUtil.isAnnotated(constructor, INJECT_ANNOTATIONS, 0)) {
            return;
          }
        }

        if (constructors.length == 1 && defaultConstructor != null) {
          return;
        }
      }

      myHolder.registerProblem(aClass.getNameIdentifier(), "Missed @Inject annotation");
    }
  }

  @Nonnull
  @Override
  public String getDisplayName() {
    return "Missed @Inject annotation for services & extensions";
  }

  @Override
  public PsiElementVisitor buildInternalVisitor(@Nonnull ProblemsHolder holder, boolean isOnTheFly) {
    return new Visitor(holder);
  }

  @RequiredReadAction
  private static boolean isInjectionTarget(PsiClass psiClass) {
    ServiceInfo serviceInfo = ServiceLocator.findImplementationService(psiClass);
    if (serviceInfo != null) {
      // old XML service
      return true;
    }

    for (String annotation : ValhallaClasses.Impl) {
      if (AnnotationUtil.isAnnotated(psiClass, annotation, 0)) {
        return true;
      }
    }
    return false;
  }
}
