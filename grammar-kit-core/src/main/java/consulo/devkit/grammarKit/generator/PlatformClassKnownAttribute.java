package consulo.devkit.grammarKit.generator;

import org.intellij.grammar.KnownAttribute;

/**
 * @author VISTALL
 * @since 2022-03-25
 */
public class PlatformClassKnownAttribute extends KnownAttribute<String> {
    private final PlatformClass myPlatformClass;

    public PlatformClassKnownAttribute(boolean global, String name, PlatformClass platformClass) {
        super(global, name, String.class, platformClass.select(null));
        myPlatformClass = platformClass;
    }

    @Override
    public String getDefaultValue(String version) {
        return myPlatformClass.select(version);
    }
}
