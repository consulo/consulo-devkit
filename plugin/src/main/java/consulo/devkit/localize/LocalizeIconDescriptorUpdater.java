package consulo.devkit.localize;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.devkit.icon.DevKitIconGroup;
import consulo.language.icon.IconDescriptor;
import consulo.language.icon.IconDescriptorUpdater;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import org.jetbrains.yaml.psi.YAMLFile;

import java.util.Locale;

/**
 * @author VISTALL
 * @since 2024-12-02
 */
@ExtensionImpl
public class LocalizeIconDescriptorUpdater implements IconDescriptorUpdater {
    @RequiredReadAction
    @Override
    public void updateIcon(@Nonnull IconDescriptor iconDescriptor, @Nonnull PsiElement psiElement, int flags) {
        if (!(psiElement instanceof YAMLFile yamlFile)) {
            return;
        }

        Locale locale = LocalizeUtil.extractLocaleFromFile(yamlFile.getVirtualFile());
        if (locale == null) {
            return;
        }

        iconDescriptor.setMainIcon(DevKitIconGroup.localize());
    }
}
