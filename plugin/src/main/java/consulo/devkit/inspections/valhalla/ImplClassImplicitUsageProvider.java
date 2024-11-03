package consulo.devkit.inspections.valhalla;

import com.intellij.java.language.codeInsight.AnnotationUtil;
import com.intellij.java.language.psi.PsiClass;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.ImplicitUsageProvider;
import consulo.language.psi.PsiElement;

/**
 * @author VISTALL
 * @since 2022-08-23
 */
@ExtensionImpl
public class ImplClassImplicitUsageProvider implements ImplicitUsageProvider {
    @Override
    @RequiredReadAction
    public boolean isImplicitUsage(PsiElement psiElement) {
        return psiElement instanceof PsiClass psiClass
            && ExtensionImplUtil.isTargetClass(psiClass)
            && AnnotationUtil.isAnnotated(psiClass, ValhallaClasses.Impl, 0);
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
