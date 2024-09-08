package consulo.devkit.localize;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.findUsage.FindUsagesProvider;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import org.jetbrains.yaml.YAMLLanguage;
import org.jetbrains.yaml.psi.YAMLKeyValue;

/**
 * @author VISTALL
 * @since 2024-09-08
 */
@ExtensionImpl
public class LocalizeFindUsagesProvider implements FindUsagesProvider {
    @Override
    public boolean canFindUsagesFor(@Nonnull PsiElement psiElement) {
        if (psiElement instanceof YAMLKeyValue keyValue && LocalizeUtil.isLocalizeFile(psiElement.getContainingFile().getVirtualFile())) {
            return true;
        }
        return false;
    }

    @Nonnull
    @Override
    public String getType(@Nonnull PsiElement psiElement) {
        return "yaml key";
    }

    @Nonnull
    @Override
    public String getDescriptiveName(@Nonnull PsiElement psiElement) {
        return "yaml key";
    }

    @Nonnull
    @Override
    public String getNodeText(@Nonnull PsiElement psiElement, boolean b) {
        return "yaml key";
    }

    @Nonnull
    @Override
    public Language getLanguage() {
        return YAMLLanguage.INSTANCE;
    }
}
