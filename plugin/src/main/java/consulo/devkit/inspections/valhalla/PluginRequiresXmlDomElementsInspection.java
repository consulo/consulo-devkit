package consulo.devkit.inspections.valhalla;

import consulo.annotation.component.ExtensionImpl;
import consulo.devkit.localize.DevKitLocalize;
import consulo.devkit.requires.dom.PluginRequires;
import consulo.localize.LocalizeValue;
import consulo.xml.util.xml.highlighting.BasicDomElementsInspection;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2022-08-28
 */
@ExtensionImpl
public class PluginRequiresXmlDomElementsInspection extends BasicDomElementsInspection<PluginRequires, Object> {
    public PluginRequiresXmlDomElementsInspection() {
        super(PluginRequires.class);
    }

    @Override
    @Nonnull
    public LocalizeValue getGroupDisplayName() {
        return DevKitLocalize.inspectionsGroupName();
    }

    @Override
    @Nonnull
    public LocalizeValue getDisplayName() {
        return DevKitLocalize.inspectionPluginRequiresXmlValidityDisplayName();
    }

    @Override
    @Nonnull
    public String getShortName() {
        return "PluginRequiresXmlValidity";
    }
}
