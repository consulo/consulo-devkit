package consulo.devkit.codeInsight.daemon;

import com.intellij.codeHighlighting.Pass;
import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.JavaTokenType;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.PsiNavigateUtil;
import consulo.annotation.access.RequiredReadAction;
import consulo.devkit.inspections.valhalla.ValhallaAnnotations;
import consulo.util.lang.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;

/**
 * @author VISTALL
 * @since 08-Aug-22
 */
public class ImplToAPILineMarkerProvider implements LineMarkerProvider
{
	@RequiredReadAction
	@Nullable
	@Override
	public LineMarkerInfo getLineMarkerInfo(@Nonnull PsiElement psiElement)
	{
		if(PsiUtilCore.getElementType(psiElement) == JavaTokenType.IDENTIFIER && psiElement.getParent() instanceof PsiClass)
		{
			PsiClass psiClass = (PsiClass) psiElement.getParent();

			Pair<PsiElement, String> apiInfo = findAPIElement(psiClass);
			if(apiInfo != null)
			{
				String navigationText = "Navigate to @" + StringUtil.getShortName(apiInfo.getSecond());
				return new LineMarkerInfo<>(psiElement, psiElement.getTextRange(), AllIcons.Nodes.Plugin, Pass.LINE_MARKERS, element -> navigationText, (mouseEvent, element) ->
				{
					Pair<PsiElement, String> target = findAPIElement(psiClass);
					if(target != null)
					{
						PsiNavigateUtil.navigate(target.getFirst());
					}
				}, GutterIconRenderer.Alignment.RIGHT);
			}
		}
		return null;
	}

	@Nullable
	private Pair<PsiElement, String> findAPIElement(PsiClass psiClass)
	{
		for(Pair<String, String> apiPair : ValhallaAnnotations.ApiToImpl)
		{
			if(AnnotationUtil.isAnnotated(psiClass, apiPair.getSecond(), 0))
			{
				PsiAnnotation annotationInHierarchy = AnnotationUtil.findAnnotationInHierarchy(psiClass, Set.of(apiPair.getFirst()));
				if(annotationInHierarchy != null)
				{
					//					PsiClass apiType = PsiTreeUtil.getParentOfType(annotationInHierarchy, PsiClass.class);
					//					if(apiType != null)
					//					{
					//						return apiType;
					//					}
					return Pair.create(annotationInHierarchy, apiPair.getFirst());
				}
			}
		}

		return null;
	}
}
