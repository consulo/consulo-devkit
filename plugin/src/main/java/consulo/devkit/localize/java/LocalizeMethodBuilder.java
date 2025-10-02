package consulo.devkit.localize.java;

import com.intellij.java.language.JavaLanguage;
import com.intellij.java.language.impl.psi.impl.light.LightMethodBuilder;
import com.intellij.java.language.psi.PsiClass;
import com.intellij.java.language.psi.PsiModifier;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiManager;
import consulo.localize.LocalizeValue;

/**
 * @author VISTALL
 * @since 2025-10-02
 */
public class LocalizeMethodBuilder extends LightMethodBuilder {
    public LocalizeMethodBuilder(PsiClass constructedClass,
                                 PsiElement context,
                                 String methodName) {
        super(
            PsiManager.getInstance(constructedClass.getProject()),
            JavaLanguage.INSTANCE,
            methodName
        );

        setNavigationElement(context);

        setMethodReturnType(LocalizeValue.class.getName());

        addModifiers(PsiModifier.PUBLIC, PsiModifier.STATIC);
    }
}
