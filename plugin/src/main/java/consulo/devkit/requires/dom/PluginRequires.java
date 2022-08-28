package consulo.devkit.requires.dom;

import com.intellij.util.xml.DefinesXml;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.SubTagList;

import java.util.List;

/**
 * @author VISTALL
 * @since 28-Aug-22
 */
@DefinesXml
public interface PluginRequires extends DomElement
{
	@SubTagList("plugins")
	List<Plugins> getPlugins();
}
