package consulo.devkit.localize;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.find.FindUsagesHandler;
import consulo.find.FindUsagesHandlerFactory;
import consulo.language.psi.PsiElement;
import consulo.util.lang.ObjectUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jetbrains.yaml.psi.YAMLKeyValue;

/**
 * @author VISTALL
 * @since 2024-09-08
 */
@ExtensionImpl
public class LocalizeFindUsagesHandlerFactory extends FindUsagesHandlerFactory {
    @Override
    @RequiredReadAction
    public boolean canFindUsages(@Nonnull PsiElement element) {
        if (element instanceof YAMLKeyValue yamlKeyValue) {
            YAMLFile yamlFile = ObjectUtil.tryCast(yamlKeyValue.getContainingFile(), YAMLFile.class);
            if (yamlFile != null) {
                return LocalizeUtil.isLocalizeFile(yamlFile.getVirtualFile());
            }
        }
        return false;
    }

    @Nullable
    @Override
    public FindUsagesHandler createFindUsagesHandler(@Nonnull PsiElement element, boolean forHighlightUsages) {
        return new LocalizeFindUsagesHandler(element);
    }
}
