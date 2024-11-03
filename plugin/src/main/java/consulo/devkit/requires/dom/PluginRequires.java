package consulo.devkit.requires.dom;

import consulo.xml.util.xml.DefinesXml;
import consulo.xml.util.xml.DomElement;
import consulo.xml.util.xml.SubTagList;

import java.util.List;

/**
 * @author VISTALL
 * @since 2022-08-28
 */
@DefinesXml
public interface PluginRequires extends DomElement {
    @SubTagList("plugins")
    List<Plugins> getPlugins();
}
