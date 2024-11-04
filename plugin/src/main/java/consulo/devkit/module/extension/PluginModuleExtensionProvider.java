package consulo.devkit.module.extension;

import consulo.annotation.component.ExtensionImpl;
import consulo.localize.LocalizeValue;
import consulo.module.content.layer.ModuleExtensionProvider;
import consulo.module.content.layer.ModuleRootLayer;
import consulo.module.extension.ModuleExtension;
import consulo.module.extension.MutableModuleExtension;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.image.Image;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 09/02/2023
 */
@ExtensionImpl
public class PluginModuleExtensionProvider implements ModuleExtensionProvider<PluginModuleExtension> {
    @Nonnull
    @Override
    public String getId() {
        return "consulo-plugin";
    }

    @Nullable
    @Override
    public String getParentId() {
        return "java";
    }

    @Nonnull
    @Override
    public LocalizeValue getName() {
        return LocalizeValue.localizeTODO("Consulo Plugin");
    }

    @Nonnull
    @Override
    public Image getIcon() {
        return PlatformIconGroup.icon16();
    }

    @Nonnull
    @Override
    public ModuleExtension<PluginModuleExtension> createImmutableExtension(@Nonnull ModuleRootLayer moduleRootLayer) {
        return new PluginModuleExtension(getId(), moduleRootLayer);
    }

    @Nonnull
    @Override
    public MutableModuleExtension<PluginModuleExtension> createMutableExtension(@Nonnull ModuleRootLayer moduleRootLayer) {
        return new PluginMutableModuleExtension(getId(), moduleRootLayer);
    }
}
