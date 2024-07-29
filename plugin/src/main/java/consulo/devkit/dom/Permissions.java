package consulo.devkit.dom;

import consulo.xml.util.xml.DomElement;
import consulo.xml.util.xml.SubTagList;

import java.util.List;

/**
 * @author VISTALL
 * @since 01/11/2021
 */
public interface Permissions extends DomElement {
    @SubTagList("permission")
    List<Permission> getPermissions();
}
