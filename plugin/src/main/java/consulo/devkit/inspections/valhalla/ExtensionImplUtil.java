package consulo.devkit.inspections.valhalla;

import com.intellij.java.language.psi.PsiClass;
import consulo.annotation.access.RequiredReadAction;

/**
 * @author VISTALL
 * @since 2022-12-19
 */
public class ExtensionImplUtil {
    @RequiredReadAction
    public static boolean isTargetClass(PsiClass aClass) {
        return !(aClass.isAbstract() || aClass.isInterface() || aClass.isAnnotationType() || aClass.isEnum() || aClass.isRecord())
            && aClass.getContainingClass() == null;
    }
}
