package consulo.devkit.dom;

import consulo.xml.util.xml.DomElement;
import consulo.xml.util.xml.Required;

/**
 * @author VISTALL
 * @since 03/11/2021
 */
public interface Tag extends DomElement {
  @Required
  String getValue();

  void setValue(String text);
}
