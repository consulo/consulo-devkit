package consulo.devkit.dom;

import consulo.xml.util.xml.DomElement;
import consulo.xml.util.xml.Required;
import consulo.xml.util.xml.SubTagList;

import java.util.List;

/**
 * @author VISTALL
 * @since 2021-11-03
 */
public interface Tags extends DomElement {
    @SubTagList("tag")
    @Required
    List<Tag> getTags();
}
