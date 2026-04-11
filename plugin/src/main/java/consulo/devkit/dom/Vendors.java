package consulo.devkit.dom;

import consulo.xml.dom.DomElement;
import consulo.xml.dom.Required;
import consulo.xml.dom.SubTagList;
import org.jetbrains.idea.devkit.dom.Vendor;

import java.util.List;

/**
 * @author VISTALL
 * @since 2026-01-04
 */
public interface Vendors extends DomElement {
    @SubTagList("vendor")
    @Required
    List<Vendor> getVendors();
}
