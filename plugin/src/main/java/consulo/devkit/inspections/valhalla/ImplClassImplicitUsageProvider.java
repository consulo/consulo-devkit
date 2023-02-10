package consulo.devkit.inspections.valhalla;

import com.intellij.java.language.codeInsight.AnnotationUtil;
import com.intellij.java.language.psi.PsiClass;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.ImplicitUsageProvider;
import consulo.language.psi.PsiElement;

/**
 * @author VISTALL
 * @since 23-Aug-22
 */
@ExtensionImpl
public class ImplClassImplicitUsageProvider implements ImplicitUsageProvider {
  @Override
  @RequiredReadAction
  public boolean isImplicitUsage(PsiElement psiElement) {
    if (psiElement instanceof PsiClass) {
      if (ExtensionImplUtil.isTargetClass((PsiClass)psiElement) && AnnotationUtil.isAnnotated((PsiClass)psiElement,
                                                                                              ValhallaClasses.Impl,
                                                                                              0)) {
        return true;
      }
    }
    return false;
  }

  @Override
  @RequiredReadAction
  public boolean isImplicitRead(PsiElement psiElement) {
    return isImplicitUsage(psiElement);
  }

  @Override
  public boolean isImplicitWrite(PsiElement psiElement) {
    return false;
  }
}
