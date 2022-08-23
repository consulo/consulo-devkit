package consulo.devkit.inspections.valhalla;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInsight.daemon.ImplicitUsageProvider;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;

/**
 * @author VISTALL
 * @since 23-Aug-22
 */
public class ImplClassImplicitUsageProvider implements ImplicitUsageProvider
{
	@Override
	public boolean isImplicitUsage(PsiElement psiElement)
	{
		if(psiElement instanceof PsiClass)
		{
			if(AnnotationUtil.isAnnotated((PsiClass)psiElement, ValhallaAnnotations.Impl, 0))
			{
				return true;
			}
		}
		return false;
	}

	@Override
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
