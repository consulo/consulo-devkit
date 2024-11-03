package consulo.devkit.dom;

import consulo.xml.util.xml.DomElement;

/**
 * @author VISTALL
 * @since 2021-11-01
 */
public interface PermissionOption extends DomElement {
    String getValue();

    void setValue(String value);
}
