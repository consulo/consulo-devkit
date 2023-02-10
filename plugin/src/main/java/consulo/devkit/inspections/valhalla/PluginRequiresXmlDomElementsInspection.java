package consulo.devkit.inspections.valhalla;

import consulo.annotation.component.ExtensionImpl;
import consulo.devkit.requires.dom.PluginRequires;
import consulo.xml.util.xml.highlighting.BasicDomElementsInspection;
import org.jetbrains.annotations.Nls;
import org.jetbrains.idea.devkit.DevKitBundle;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 28-Aug-22
 */
@ExtensionImpl
public class PluginRequiresXmlDomElementsInspection extends BasicDomElementsInspection<PluginRequires> {
  public PluginRequiresXmlDomElementsInspection() {
    super(PluginRequires.class);
  }

  @Nls
  @Nonnull
  public String getGroupDisplayName() {
    return DevKitBundle.message("inspections.group.name");
  }

  @Nls
  @Nonnull
  public String getDisplayName() {
    return "Plugin Requires.xml Validity";
  }

  @Nonnull
  public String getShortName() {
    return "PluginRequiresXmlValidity";
  }
}
