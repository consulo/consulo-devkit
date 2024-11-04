package consulo.devkit.requires.dom;

import consulo.xml.util.xml.DomElement;

import java.util.List;

/**
 * @author VISTALL
 * @since 2022-08-28
 */
public interface Plugins extends DomElement {
    List<Plugin> getPlugins();
}
