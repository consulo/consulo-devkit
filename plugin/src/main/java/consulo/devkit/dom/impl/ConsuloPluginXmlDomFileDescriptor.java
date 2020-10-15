package consulo.devkit.dom.impl;

import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.icons.AllIcons;
import com.intellij.lang.annotation.Annotation;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.util.Iconable;
import com.intellij.psi.PsiClass;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomFileDescription;
import com.intellij.util.xml.DomUtil;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.highlighting.DomElementAnnotationHolder;
import com.intellij.util.xml.highlighting.DomElementsAnnotator;
import consulo.ui.image.Image;
import org.jetbrains.idea.devkit.dom.Extension;
import org.jetbrains.idea.devkit.dom.ExtensionPoint;
import org.jetbrains.idea.devkit.dom.IdeaPlugin;

import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2018-08-16
 */
public class ConsuloPluginXmlDomFileDescriptor extends DomFileDescription<IdeaPlugin>
{
	private static final DomElementsAnnotator ANNOTATOR = new DomElementsAnnotator()
	{
		@Override
		public void annotate(DomElement element, DomElementAnnotationHolder holder)
		{
			if(element instanceof Extension)
			{
				annotateExtension((Extension) element, holder);
			}
			else if(element instanceof ExtensionPoint)
			{
				highlightDeprecatedArea((ExtensionPoint) element, holder);
			}
		}

		private void highlightDeprecatedArea(ExtensionPoint element, DomElementAnnotationHolder holder)
		{
			GenericAttributeValue<ExtensionPoint.Area> area = element.getArea();
			if(!DomUtil.hasXml(area))
			{
				return;
			}
			ExtensionPoint.Area value = area.getValue();
			if(value == ExtensionPoint.Area.CONSULO_MODULE || value == ExtensionPoint.Area.CONSULO_PROJECT)
			{
				final Annotation annotation = holder.createAnnotation(area, HighlightSeverity.WARNING, "This area is deprecated. Use without 'CONSULO_' prefix");
				annotation.setHighlightType(ProblemHighlightType.LIKE_DEPRECATED);
			}
		}

		private void annotateExtension(Extension extension, DomElementAnnotationHolder holder)
		{
			final ExtensionPoint extensionPoint = extension.getExtensionPoint();
			if(extensionPoint == null)
			{
				return;
			}
			final GenericAttributeValue<PsiClass> interfaceAttribute = extensionPoint.getInterface();
			if(!DomUtil.hasXml(interfaceAttribute))
			{
				return;
			}

			final PsiClass value = interfaceAttribute.getValue();
			if(value != null && value.isDeprecated())
			{
				final Annotation annotation = holder.createAnnotation(extension, HighlightSeverity.WARNING, "Deprecated EP");
				annotation.setHighlightType(ProblemHighlightType.LIKE_DEPRECATED);
			}
		}

		private void highlightNotUsedAnymore(GenericAttributeValue attributeValue, DomElementAnnotationHolder holder)
		{
			if(!DomUtil.hasXml(attributeValue))
			{
				return;
			}

			final Annotation annotation = holder.createAnnotation(attributeValue, HighlightSeverity.WARNING, "Not used anymore");
			annotation.setHighlightType(ProblemHighlightType.LIKE_DEPRECATED);
		}
	};

	public ConsuloPluginXmlDomFileDescriptor()
	{
		this("consulo-plugin");
	}

	public ConsuloPluginXmlDomFileDescriptor(String tagName)
	{
		super(IdeaPlugin.class, tagName);
	}

	@Override
	public Image getFileIcon(@Iconable.IconFlags int flags)
	{
		return AllIcons.Nodes.Plugin;
	}

	@Nullable
	@Override
	public DomElementsAnnotator createAnnotator()
	{
		return ANNOTATOR;
	}

	@Override
	public boolean hasStubs()
	{
		return true;
	}

	@Override
	public int getStubVersion()
	{
		return 3;
	}
}