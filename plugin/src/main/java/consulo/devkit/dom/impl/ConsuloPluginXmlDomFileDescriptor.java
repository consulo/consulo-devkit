package consulo.devkit.dom.impl;

import consulo.annotation.component.ExtensionImpl;
import consulo.component.util.Iconable;
import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.image.Image;
import consulo.xml.util.xml.DomElement;
import consulo.xml.util.xml.DomFileDescription;
import consulo.xml.util.xml.highlighting.DomElementsAnnotator;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import org.jetbrains.idea.devkit.dom.IdeaPlugin;
import org.jetbrains.idea.devkit.dom.Vendor;

/**
 * @author VISTALL
 * @since 2018-08-16
 */
@ExtensionImpl
public class ConsuloPluginXmlDomFileDescriptor extends DomFileDescription<IdeaPlugin> {
    private static final DomElementsAnnotator ANNOTATOR = (element, holder) -> {
        if (element instanceof Vendor vendor) {
            DomElement parent = vendor.getParent();
            if (parent instanceof IdeaPlugin) {
                holder.createProblem(
                    vendor,
                    ProblemHighlightType.LIKE_DEPRECATED,
                    "Deprecated tag. Use 'vendors'",
                    null
                );
            }
        }
    };

    @Inject
    public ConsuloPluginXmlDomFileDescriptor() {
        super(IdeaPlugin.class, "consulo-plugin");
    }

    @Override
    public Image getFileIcon(@Iconable.IconFlags int flags) {
        return PlatformIconGroup.nodesPlugin();
    }

    @Nullable
    @Override
    public DomElementsAnnotator createAnnotator() {
        return ANNOTATOR;
    }

    @Override
    public boolean hasStubs() {
        return true;
    }

    @Override
    public int getStubVersion() {
        return 3;
    }
}