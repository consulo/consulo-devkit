package consulo.devkit;

import consulo.container.plugin.PluginId;
import consulo.container.plugin.PluginIds;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @author VISTALL
 * @see PluginIds from platform
 * @since 28-Aug-22
 */
public final class Consulo3PluginIds {
  public static final PluginId CONSULO_BASE = PluginId.getId("consulo");

  public static final PluginId CONSULO_DESKTOP_AWT = PluginId.getId("consulo.desktop.awt");

  public static final PluginId CONSULO_DESKTOP_SWT = PluginId.getId("consulo.desktop.swt");

  public static final PluginId CONSULO_WEB = PluginId.getId("consulo.web");

  /**
   * ID of repo analyzer, since it's running without AWT/SWT/Web dependencies
   */
  public static final PluginId CONSULO_REPO_ANALYZER = PluginId.getId("consulo.repo.analyzer");

  private static final Set<PluginId> ourMergedObsoletePlugins =
    new HashSet<PluginId>(Arrays.asList(PluginId.getId("org.intellij.intelliLang")));

  private static final Set<PluginId> ourPlatformIds =
    new HashSet<PluginId>(Arrays.asList(CONSULO_DESKTOP_AWT, CONSULO_DESKTOP_SWT, CONSULO_WEB, CONSULO_REPO_ANALYZER));

  public static boolean isPlatformImplementationPlugin(PluginId pluginId) {
    return ourPlatformIds.contains(pluginId);
  }

  public static boolean isPlatformPlugin(PluginId pluginId) {
    return CONSULO_BASE.equals(pluginId) || isPlatformImplementationPlugin(pluginId);
  }

  public static Set<PluginId> getObsoletePlugins() {
    return ourMergedObsoletePlugins;
  }

}
