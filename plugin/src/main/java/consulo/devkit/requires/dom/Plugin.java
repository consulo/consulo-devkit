package consulo.devkit.requires.dom;

import consulo.container.plugin.PluginDescriptor;
import consulo.devkit.dom.impl.PluginDescriptorResolver;
import consulo.xml.util.xml.Convert;
import consulo.xml.util.xml.GenericDomValue;

/**
 * @author VISTALL
 * @since 2022-08-28
 */
@Convert(PluginDescriptorResolver.class)
public interface Plugin extends GenericDomValue<PluginDescriptor> {
}
