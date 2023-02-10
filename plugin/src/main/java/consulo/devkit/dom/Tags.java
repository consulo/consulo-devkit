package consulo.devkit.dom;

import consulo.xml.util.xml.DomElement;
import consulo.xml.util.xml.Required;
import consulo.xml.util.xml.SubTagList;

import java.util.List;

/**
 * @author VISTALL
 * @since 03/11/2021
 */
public interface Tags extends DomElement {
  @SubTagList("tag")
  @Required
  List<Tag> getTags();
}
