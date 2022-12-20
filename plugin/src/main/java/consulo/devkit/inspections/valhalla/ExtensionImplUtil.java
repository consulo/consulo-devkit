package consulo.devkit.inspections.valhalla;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.util.PsiUtil;
import consulo.annotation.access.RequiredReadAction;

/**
 * @author VISTALL
 * @since 19/12/2022
 */
public class ExtensionImplUtil
{
	@RequiredReadAction
	public static boolean isTargetClass(PsiClass psiClass)
	{
		if(psiClass.hasModifierProperty(PsiModifier.ABSTRACT) || psiClass.isInterface() || psiClass.isAnnotationType() || psiClass.isEnum() || psiClass.isRecord() || PsiUtil.isInnerClass(psiClass))
		{
			return false;
		}

		return true;
	}
}
