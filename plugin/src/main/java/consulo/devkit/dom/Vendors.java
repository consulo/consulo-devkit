package consulo.devkit.dom;

import consulo.xml.util.xml.DomElement;
import consulo.xml.util.xml.Required;
import consulo.xml.util.xml.SubTagList;
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
