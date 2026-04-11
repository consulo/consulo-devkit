package consulo.devkit.requires.dom;

import consulo.xml.dom.DefinesXml;
import consulo.xml.dom.DomElement;
import consulo.xml.dom.SubTagList;

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
