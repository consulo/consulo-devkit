package consulo.devkit.dom;

import com.intellij.util.xml.*;
import consulo.container.plugin.PluginPermissionType;

import java.util.List;

/**
 * @author VISTALL
 * @since 01/11/2021
 */
public interface Permission extends DomElement
{
	@Required
	@Attribute("type")
	GenericAttributeValue<PluginPermissionType> getType();

	@SubTagList("permission-option")
	List<PermissionOption> getPermissionOptions();
}
