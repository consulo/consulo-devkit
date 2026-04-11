package consulo.devkit.dom;

import consulo.xml.dom.DomElement;
import consulo.xml.dom.Required;
import consulo.xml.dom.SubTagList;

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
