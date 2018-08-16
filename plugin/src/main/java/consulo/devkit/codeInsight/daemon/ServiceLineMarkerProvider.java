package consulo.devkit.codeInsight.daemon;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.intellij.codeHighlighting.Pass;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.psi.JavaTokenType;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.PsiNavigateUtil;
import consulo.annotations.RequiredReadAction;
import consulo.devkit.inspections.util.service.ServiceInfo;
import consulo.devkit.inspections.util.service.ServiceLocator;

/**
 * @author VISTALL
 * @since 2018-08-16
 */
public class ServiceLineMarkerProvider implements LineMarkerProvider
{
	@RequiredReadAction
	@Nullable
	@Override
	public LineMarkerInfo getLineMarkerInfo(@Nonnull PsiElement psiElement)
	{
		if(PsiUtilCore.getElementType(psiElement) == JavaTokenType.IDENTIFIER && psiElement.getParent() instanceof PsiClass)
		{
			PsiClass psiClass = (PsiClass) psiElement.getParent();

			ServiceInfo info = ServiceLocator.findAnyService(psiClass);
			if(info != null)
			{
				return new LineMarkerInfo<>(psiElement, psiElement.getTextRange(), AllIcons.Nodes.Plugin, Pass.LINE_MARKERS, element -> "Service", (mouseEvent, element) ->
				{
					ServiceInfo info2 = ServiceLocator.findAnyService((PsiClass) element.getParent());
					if(info2 != null)
					{
						PsiNavigateUtil.navigate(info2.getNavigatableElement());
					}
				}, GutterIconRenderer.Alignment.RIGHT);
			}
		}
		return null;
	}
}
