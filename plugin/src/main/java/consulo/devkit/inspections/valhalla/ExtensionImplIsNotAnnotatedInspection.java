package consulo.devkit.inspections.valhalla;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInsight.intention.AddAnnotationFix;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.*;
import consulo.annotation.access.RequiredReadAction;
import org.jetbrains.idea.devkit.inspections.internal.InternalInspection;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 08-Aug-22
 */
public class ExtensionImplIsNotAnnotatedInspection extends InternalInspection
{
	@Override
	public PsiElementVisitor buildInternalVisitor(@Nonnull ProblemsHolder holder, boolean isOnTheFly)
	{
		return new JavaElementVisitor()
		{
			@Override
			@RequiredReadAction
			public void visitClass(PsiClass aClass)
			{
				PsiIdentifier nameIdentifier = aClass.getNameIdentifier();
				if(nameIdentifier == null)
				{
					return;
				}

				if(!ExtensionImplUtil.isTargetClass(aClass))
				{
					return;
				}

				// not annotated by @ExtensionAPI, and not annotated by @ExtensionImpl, but has @ExtensionAPI in class hierarchy
				if(!AnnotationUtil.isAnnotated(aClass, ValhallaClasses.ExtensionAPI, 0) &&
						!AnnotationUtil.isAnnotated(aClass, ValhallaClasses.ExtensionImpl, 0) &&
						AnnotationUtil.isAnnotated(aClass, ValhallaClasses.ExtensionAPI, AnnotationUtil.CHECK_HIERARCHY))
				{
					PsiClass syntheticAction = JavaPsiFacade.getInstance(aClass.getProject()).findClass(ValhallaClasses.SyntheticIntentionAction, aClass.getResolveScope());
					if(syntheticAction != null && aClass.isInheritor(syntheticAction, true))
					{
						return;
					}

					AddAnnotationFix addAnnotationFix = new AddAnnotationFix(ValhallaClasses.ExtensionImpl, aClass);
					holder.registerProblem(nameIdentifier, "Extension implementation not annotated by @ExtensionImpl", ProblemHighlightType.GENERIC_ERROR_OR_WARNING, addAnnotationFix);
				}
			}
		};
	}
}
