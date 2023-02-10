package consulo.devkit.dom;

import consulo.xml.util.xml.DomElement;

/**
 * @author VISTALL
 * @since 01/11/2021
 */
public interface PermissionOption extends DomElement {
  String getValue();

  void setValue(String value);
}
