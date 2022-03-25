package consulo.devkit.grammarKit.generator;

import org.intellij.grammar.KnownAttribute;

/**
 * @author VISTALL
 * @since 25-Mar-22
 */
public class PlatformClassKnownAttribute extends KnownAttribute<String>
{
	public PlatformClassKnownAttribute(boolean global, String name, PlatformClass platformClass)
	{
		super(global, name, String.class, platformClass.select(null));
	}
}
