package consulo.devkit.inspections.valhalla;

import consulo.annotation.component.ExtensionImpl;
import consulo.devkit.localize.DevKitLocalize;
import consulo.devkit.requires.dom.PluginRequires;
import consulo.xml.util.xml.highlighting.BasicDomElementsInspection;
import org.jetbrains.annotations.Nls;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 28-Aug-22
 */
@ExtensionImpl
public class PluginRequiresXmlDomElementsInspection extends BasicDomElementsInspection<PluginRequires, Object> {
  public PluginRequiresXmlDomElementsInspection() {
    super(PluginRequires.class);
  }

  @Nls
  @Nonnull
  public String getGroupDisplayName() {
    return DevKitLocalize.inspectionsGroupName().get();
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
