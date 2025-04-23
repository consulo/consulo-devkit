package consulo.devkit.inspections.valhalla;

import consulo.util.lang.Couple;

import java.util.List;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2022-08-08
 */
public interface ValhallaClasses {
    String EXTENSION_API = "consulo.annotation.component.ExtensionAPI";
    String SERVICE_API = "consulo.annotation.component.ServiceAPI";
    String ACTION_API = "consulo.annotation.component.ActionAPI";
    String TOPIC_API = "consulo.annotation.component.TopicAPI";

    String EXTENSION_IMPL = "consulo.annotation.component.ExtensionImpl";
    String SERVICE_IMPL = "consulo.annotation.component.ServiceImpl";
    String ACTION_IMPL = "consulo.annotation.component.ActionImpl";
    String TOPIC_IMPL = "consulo.annotation.component.TopicImpl";

    String SYNTHETIC_INTENTION_ACTION = "consulo.language.editor.intention.SyntheticIntentionAction";
    String INTENTION_ACTION = "consulo.language.editor.intention.IntentionAction";
    String INTENTION_META_DATA = "consulo.language.editor.intention.IntentionMetaData";

    Set<String> IMPL = Set.of(EXTENSION_IMPL, SERVICE_IMPL, ACTION_IMPL, TOPIC_IMPL);

    List<Couple<String>> API_TO_IMPL = List.of(
        Couple.of(ACTION_API, ACTION_IMPL),
        Couple.of(SERVICE_API, SERVICE_IMPL),
        Couple.of(EXTENSION_API, EXTENSION_IMPL),
        Couple.of(TOPIC_API, TOPIC_IMPL)
    );
}
