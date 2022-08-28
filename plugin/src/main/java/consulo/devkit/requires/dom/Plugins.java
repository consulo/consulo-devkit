package consulo.devkit.requires.dom;

import com.intellij.util.xml.DomElement;

import java.util.List;

/**
 * @author VISTALL
 * @since 28-Aug-22
 */
public interface Plugins extends DomElement
{
	List<Plugin> getPlugins();
}
