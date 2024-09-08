package consulo.devkit.localize.folding;

import org.jetbrains.yaml.psi.YAMLFile;

/**
 * @author VISTALL
 * @since 2024-09-08
 */
public record LocalizeResolveInfo(YAMLFile file, String key, String value) {
}
