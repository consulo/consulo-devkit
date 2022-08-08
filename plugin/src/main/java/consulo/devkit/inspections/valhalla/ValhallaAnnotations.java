package consulo.devkit.inspections.valhalla;

/**
 * @author VISTALL
 * @since 08-Aug-22
 */
public interface ValhallaAnnotations
{
	String ExtensionAPI = "consulo.annotation.component.ExtensionAPI";

	String ExtensionImpl = "consulo.annotation.component.ExtensionImpl";
	String ServiceImpl = "consulo.annotation.component.ServiceImpl";
	String ActionImpl = "consulo.annotation.component.ActionImpl";

	String[] Impl = {
			ExtensionImpl,
			ServiceImpl,
			ActionImpl
	};
}
