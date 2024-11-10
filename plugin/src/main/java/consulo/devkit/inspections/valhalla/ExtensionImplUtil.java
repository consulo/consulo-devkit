package consulo.devkit.inspections.valhalla;

import com.intellij.java.language.psi.PsiClass;
import consulo.annotation.access.RequiredReadAction;

/**
 * @author VISTALL
 * @since 2022-12-19
 */
public class ExtensionImplUtil {
    @RequiredReadAction
    public static boolean isTargetClass(PsiClass psiClass) {
        return !psiClass.isAbstract()
            && !psiClass.isInterface()
            && !psiClass.isAnnotationType()
            && !psiClass.isEnum()
            && !psiClass.isRecord()
            && psiClass.getContainingClass() == null;
    }
}
