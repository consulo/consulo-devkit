package consulo.devkit.icon.inspection;

import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiReference;
import com.intellij.psi.XmlElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.xml.DomFileElement;
import com.intellij.util.xml.DomManager;
import consulo.annotation.access.RequiredReadAction;
import org.jetbrains.idea.devkit.dom.IdeaPlugin;
import org.jetbrains.idea.devkit.inspections.DevKitInspectionBase;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2020-10-15
 */
public class IconNotResolvedInspection extends DevKitInspectionBase
{
	@Nonnull
	@Override
	public PsiElementVisitor buildVisitor(@Nonnull ProblemsHolder holder, boolean isOnTheFly)
	{
		DomFileElement<IdeaPlugin> fileElement = DomManager.getDomManager(holder.getProject()).getFileElement((XmlFile) holder.getFile(), IdeaPlugin.class);
		if(fileElement == null)
		{
			return PsiElementVisitor.EMPTY_VISITOR;
		}

		return new XmlElementVisitor()
		{
			@Override
			@RequiredReadAction
			public void visitXmlAttributeValue(XmlAttributeValue value)
			{
				XmlAttribute xmlAttribute = PsiTreeUtil.getParentOfType(value, XmlAttribute.class);
				if(xmlAttribute == null || !"icon".equals(xmlAttribute.getLocalName()))
				{
					return;
				}

				for(PsiReference psiReference : value.getReferences())
				{
					PsiElement element = psiReference.resolve();
					if(element != null)
					{
						return;
					}
				}

				holder.registerProblem(value, "Icon not resolved", ProblemHighlightType.ERROR);
			}
		};
	}
}
