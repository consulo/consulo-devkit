package consulo.devkit.inspections.valhalla;

import com.intellij.java.language.psi.PsiAnnotationMemberValue;
import com.intellij.java.language.psi.PsiClass;
import com.intellij.java.language.psi.PsiEnumConstant;
import com.intellij.java.language.psi.PsiReferenceExpression;
import consulo.annotation.access.RequiredReadAction;
import consulo.devkit.DevKitComponentScope;
import jakarta.annotation.Nullable;

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

    @Nullable
    @RequiredReadAction
    public static DevKitComponentScope resolveScope(@Nullable PsiAnnotationMemberValue value) {
        if (value instanceof PsiReferenceExpression valueRefExpr && valueRefExpr.resolve() instanceof PsiEnumConstant enumConstant) {
            String name = enumConstant.getName();

            PsiClass containingClass = enumConstant.getContainingClass();

            if (containingClass != null && ValhallaClasses.COMPONENT_SCOPE.equals(containingClass.getQualifiedName())) {
                try {
                    return DevKitComponentScope.valueOf(name);
                }
                catch (IllegalArgumentException ignored) {
                }
            }
        }

        return null;
    }
}
