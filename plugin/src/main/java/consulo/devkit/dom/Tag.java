package consulo.devkit.dom;

import consulo.xml.util.xml.DomElement;
import consulo.xml.util.xml.Required;

/**
 * @author VISTALL
 * @since 2021-11-03
 */
public interface Tag extends DomElement {
    @Required
    String getValue();

    void setValue(String text);
}
