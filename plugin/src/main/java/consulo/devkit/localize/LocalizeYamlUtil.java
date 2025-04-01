package consulo.devkit.localize;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

/**
 * @author VISTALL
 * @since 2024-10-12
 */
public class LocalizeYamlUtil {
    public static Yaml create() {
        DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setPrettyFlow(true);
        dumperOptions.setIndent(4);
        dumperOptions.setAllowUnicode(true);
        dumperOptions.setDefaultScalarStyle(DumperOptions.ScalarStyle.PLAIN);
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        dumperOptions.setWidth(1024);
        dumperOptions.setMaxSimpleKeyLength(1024);
        return new Yaml(dumperOptions);
    }
}
