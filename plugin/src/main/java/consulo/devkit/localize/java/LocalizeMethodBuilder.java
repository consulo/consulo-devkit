package consulo.devkit.localize.java;

import com.intellij.java.language.JavaLanguage;
import com.intellij.java.language.impl.psi.impl.light.LightMethodBuilder;
import com.intellij.java.language.psi.PsiClass;
import com.intellij.java.language.psi.PsiModifier;
import com.intellij.java.language.psi.PsiType;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiManager;

/**
 * @author VISTALL
 * @since 2025-10-02
 */
public class LocalizeMethodBuilder extends LightMethodBuilder {
    private final String myLocalizeText;

    public LocalizeMethodBuilder(PsiClass constructedClass,
                                 PsiElement context,
                                 String methodName,
                                 String localizeText,
                                 PsiType returnType) {
        super(
            PsiManager.getInstance(constructedClass.getProject()),
            JavaLanguage.INSTANCE,
            methodName
        );
        myLocalizeText = localizeText;

        setNavigationElement(context);

        setMethodReturnType(returnType);

        addModifiers(PsiModifier.PUBLIC, PsiModifier.STATIC);
    }

    public String getLocalizeText() {
        return myLocalizeText;
    }
}
