package consulo.devkit;

/**
 * @author VISTALL
 * @since 02-Apr-22
 */
public interface DevKitConstants
{
	// v2 - don't change it to v3 version, without dropping old check code
	String BASE_PLUGIN_ID = Consulo2PluginIds.COM_INTELLIJ.getIdString();

	// v3 - remove in future by placing it to base_plugin field
	String BASE_PLUGIN_ID_V3 = Consulo3PluginIds.CONSULO_BASE.getIdString();
}
