package consulo.devkit.inspections.valhalla;

import consulo.util.lang.Pair;

import java.util.List;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2022-08-08
 */
public interface ValhallaClasses {
    String ExtensionAPI = "consulo.annotation.component.ExtensionAPI";
    String ServiceAPI = "consulo.annotation.component.ServiceAPI";
    String ActionAPI = "consulo.annotation.component.ActionAPI";
    String TopicAPI = "consulo.annotation.component.TopicAPI";

    String ExtensionImpl = "consulo.annotation.component.ExtensionImpl";
    String ServiceImpl = "consulo.annotation.component.ServiceImpl";
    String ActionImpl = "consulo.annotation.component.ActionImpl";
    String TopicImpl = "consulo.annotation.component.TopicImpl";

    String SyntheticIntentionAction = "consulo.language.editor.intention.SyntheticIntentionAction";
    String IntentionAction = "consulo.language.editor.intention.IntentionAction";
    String IntentionMetaData = "consulo.language.editor.intention.IntentionMetaData";

    Set<String> Impl = Set.of(ExtensionImpl, ServiceImpl, ActionImpl, TopicImpl);

    List<Pair<String, String>> ApiToImpl = List.of(
        Pair.create(ActionAPI, ActionImpl),
        Pair.create(ServiceAPI, ServiceImpl),
        Pair.create(ExtensionAPI, ExtensionImpl),
        Pair.create(TopicAPI, TopicImpl)
    );
}
