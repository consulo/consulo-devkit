package consulo.devkit.dom.impl;

import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;
import consulo.devkit.Consulo2PluginIds;
import consulo.devkit.Consulo3PluginIds;
import consulo.devkit.localize.DevKitLocalize;
import consulo.devkit.util.PluginModuleUtil;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.language.psi.PsiElement;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.util.lang.StringUtil;
import consulo.xml.util.xml.ConvertContext;
import consulo.xml.util.xml.GenericDomValue;
import consulo.xml.util.xml.ResolvingConverter;
import org.jetbrains.idea.devkit.dom.IdeaPlugin;
import org.jetbrains.idea.devkit.dom.impl.IdeaPluginConverter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author VISTALL
 * @since 28-Aug-22
 */
public class PluginDescriptorResolver extends ResolvingConverter<PluginDescriptor> {
  @Override
  @Nonnull
  public Collection<? extends PluginDescriptor> getVariants(ConvertContext convertContext) {
    return getAllPlugins(convertContext);
  }

  @Override
  public LocalizeValue buildUnresolvedMessage(@Nullable String s, ConvertContext context) {
    return DevKitLocalize.errorCannotResolvePlugin(s);
  }

  @Nullable
  @Override
  public PsiElement resolve(PluginDescriptor o, ConvertContext context) {
    if (o instanceof PluginDescriptorOverDomElement) {
      IdeaPlugin plugin = ((PluginDescriptorOverDomElement)o).getPlugin();
      return plugin.getXmlElement();
    }
    return super.resolve(o, context);
  }

  @Override
  @Nullable
  public PluginDescriptor fromString(@Nullable String string, ConvertContext convertContext) {
    if (string == null) {
      return null;
    }

    for (PluginDescriptor descriptor : getAllPlugins(convertContext)) {
      if (descriptor.getPluginId().toString().equals(string)) {
        return descriptor;
      }
    }
    return null;
  }

  @Override
  @Nullable
  public String toString(@Nullable PluginDescriptor pluginDescriptor, ConvertContext convertContext) {
    if (pluginDescriptor == null) {
      return null;
    }
    return pluginDescriptor.getPluginId().toString();
  }

  @Nullable
  @Override
  public LookupElement createLookupElement(PluginDescriptor pluginDescriptor) {
    if (pluginDescriptor == null) {
      return null;
    }
    return LookupElementBuilder.create(pluginDescriptor.getPluginId().toString())
                               .withTypeText(pluginDescriptor.getName(), true)
                               .withIcon(PlatformIconGroup.nodesPlugin());
  }

  private List<PluginDescriptor> getAllPlugins(ConvertContext context) {
    Collection<IdeaPlugin> allPlugins = IdeaPluginConverter.getAllPlugins(context.getProject());
    boolean consuloV3 = PluginModuleUtil.isConsuloV3(context.getInvocationElement());

    PluginId basePluginId = consuloV3 ? Consulo3PluginIds.CONSULO_BASE : Consulo2PluginIds.COM_INTELLIJ;

    List<PluginDescriptor> pluginDescriptors = new ArrayList<>(allPlugins.size() + 3);
    for (IdeaPlugin plugin : allPlugins) {
      PluginId pluginId;
      String pluginIdStr = plugin.getPluginId();
      if (StringUtil.isEmpty(pluginIdStr)) {
        GenericDomValue<String> name = plugin.getName();

        String value = name.getValue();
        // it's just optional config
        if (StringUtil.isEmpty(value)) {
          continue;
        }

        pluginId = PluginId.getId(StringUtil.notNullize(value));
      }
      else {
        pluginId = PluginId.getId(pluginIdStr);
      }

      if (basePluginId.equals(pluginId)) {
        continue;
      }

      pluginDescriptors.add(new PluginDescriptorOverDomElement(pluginId, plugin));
    }

    if (consuloV3) {
      pluginDescriptors.addAll(PlatformPluginDescriptor.getPluginsV3());
    }

    return pluginDescriptors;
  }
}
