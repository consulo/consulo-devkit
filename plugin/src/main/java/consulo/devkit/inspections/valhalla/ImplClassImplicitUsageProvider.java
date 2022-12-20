package consulo.devkit.inspections.valhalla;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInsight.daemon.ImplicitUsageProvider;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import consulo.annotation.access.RequiredReadAction;

/**
 * @author VISTALL
 * @since 23-Aug-22
 */
public class ImplClassImplicitUsageProvider implements ImplicitUsageProvider
{
	@Override
	@RequiredReadAction
	public boolean isImplicitUsage(PsiElement psiElement)
	{
		if(psiElement instanceof PsiClass)
		{
			if(ExtensionImplUtil.isTargetClass((PsiClass) psiElement) && AnnotationUtil.isAnnotated((PsiClass)psiElement, ValhallaClasses.Impl, 0))
			{
				return true;
			}
		}
		return false;
	}

	@Override
	@RequiredReadAction
	public boolean isImplicitRead(PsiElement psiElement)
	{
		return isImplicitUsage(psiElement);
	}

	@Override
	public boolean isImplicitWrite(PsiElement psiElement)
	{
		return false;
	}
}
