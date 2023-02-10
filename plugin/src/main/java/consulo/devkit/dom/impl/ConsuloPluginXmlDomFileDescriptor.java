package consulo.devkit.dom.impl;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.AllIcons;
import consulo.component.util.Iconable;
import consulo.ui.image.Image;
import consulo.xml.util.xml.DomElement;
import consulo.xml.util.xml.DomFileDescription;
import consulo.xml.util.xml.highlighting.DomElementAnnotationHolder;
import consulo.xml.util.xml.highlighting.DomElementsAnnotator;
import jakarta.inject.Inject;
import org.jetbrains.idea.devkit.dom.IdeaPlugin;

import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2018-08-16
 */
@ExtensionImpl
public class ConsuloPluginXmlDomFileDescriptor extends DomFileDescription<IdeaPlugin> {
  private static final DomElementsAnnotator ANNOTATOR = new DomElementsAnnotator() {
    @Override
    public void annotate(DomElement element, DomElementAnnotationHolder holder) {

    }
  };

  @Inject
  public ConsuloPluginXmlDomFileDescriptor() {
    super(IdeaPlugin.class, "consulo-plugin");
  }

  @Override
  public Image getFileIcon(@Iconable.IconFlags int flags) {
    return AllIcons.Nodes.Plugin;
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