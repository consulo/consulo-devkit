package consulo.devkit.dom;

import consulo.xml.dom.DomElement;
import consulo.xml.dom.Required;

/**
 * @author VISTALL
 * @since 2021-11-03
 */
public interface Tag extends DomElement {
    @Required
    String getValue();

    void setValue(String text);
}
