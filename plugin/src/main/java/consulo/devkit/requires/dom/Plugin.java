package consulo.devkit.requires.dom;

import com.intellij.util.xml.Convert;
import com.intellij.util.xml.GenericDomValue;
import consulo.container.plugin.PluginDescriptor;
import consulo.devkit.dom.impl.PluginDescriptorResolver;

/**
 * @author VISTALL
 * @since 28-Aug-22
 */
@Convert(PluginDescriptorResolver.class)
public interface Plugin extends GenericDomValue<PluginDescriptor>
{
}
