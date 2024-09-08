package consulo.devkit.localize;

import com.intellij.java.language.psi.PsiMethod;
import consulo.annotation.access.RequiredReadAction;
import consulo.find.FindUsagesHandler;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import org.jetbrains.yaml.psi.YAMLKeyValue;

/**
 * @author VISTALL
 * @since 2024-09-08
 */
public class LocalizeFindUsagesHandler extends FindUsagesHandler {
    public LocalizeFindUsagesHandler(@Nonnull PsiElement psiElement) {
        super(psiElement);
    }

    @Nonnull
    @Override
    @RequiredReadAction
    public PsiElement[] getPrimaryElements() {
        YAMLKeyValue element = (YAMLKeyValue) getPsiElement();

        PsiMethod method = LocalizeUtil.findMethodByYAMLKey(element);
        if (method != null) {
            return new PsiElement[]{element, method};
        }

        return super.getPrimaryElements();
    }
}
