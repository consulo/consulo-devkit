package consulo.devkit.dom;

import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.Required;

/**
 * @author VISTALL
 * @since 03/11/2021
 */
public interface Tag extends DomElement
{
	@Required
	String getValue();

	void setValue(String text);
}
