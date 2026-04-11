package consulo.devkit.dom;

import consulo.xml.dom.DomElement;

/**
 * @author VISTALL
 * @since 2021-11-01
 */
public interface PermissionOption extends DomElement {
    String getValue();

    void setValue(String value);
}
