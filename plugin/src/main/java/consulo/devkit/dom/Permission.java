package consulo.devkit.dom;

import consulo.container.plugin.PluginPermissionType;
import consulo.xml.util.xml.*;

import java.util.List;

/**
 * @author VISTALL
 * @since 01/11/2021
 */
public interface Permission extends DomElement {
  @Required
  @Attribute("type")
  GenericAttributeValue<PluginPermissionType> getType();

  @SubTagList("permission-option")
  List<PermissionOption> getPermissionOptions();
}
