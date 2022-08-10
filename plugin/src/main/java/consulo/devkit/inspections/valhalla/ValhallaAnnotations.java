package consulo.devkit.inspections.valhalla;

import consulo.util.lang.Pair;

import java.util.List;

/**
 * @author VISTALL
 * @since 08-Aug-22
 */
public interface ValhallaAnnotations
{
	String ExtensionAPI = "consulo.annotation.component.ExtensionAPI";
	String ServiceAPI = "consulo.annotation.component.ServiceAPI";
	String ActionAPI = "consulo.annotation.component.ActionAPI";
	String TopicAPI = "consulo.annotation.component.TopicAPI";

	String ExtensionImpl = "consulo.annotation.component.ExtensionImpl";
	String ServiceImpl = "consulo.annotation.component.ServiceImpl";
	String ActionImpl = "consulo.annotation.component.ActionImpl";
	String TopicImpl = "consulo.annotation.component.TopicImpl";

	String[] Impl = {
			ExtensionImpl,
			ServiceImpl,
			ActionImpl,
			TopicImpl
	};

	List<Pair<String, String>> ApiToImpl = List.of(
			Pair.create(ActionAPI, ActionImpl),
			Pair.create(ServiceAPI, ServiceImpl),
			Pair.create(ExtensionAPI, ExtensionImpl),
			Pair.create(TopicAPI, TopicImpl));
}