package consulo.devkit.dom;

import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.Required;
import com.intellij.util.xml.SubTagList;

import java.util.List;

/**
 * @author VISTALL
 * @since 03/11/2021
 */
public interface Tags extends DomElement
{
	@SubTagList("tag")
	@Required
	List<Tag> getTags();
}
