package consulo.devkit.inspections.valhalla;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInsight.intention.AddAnnotationFix;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.lang.jvm.JvmModifier;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiIdentifier;
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
			public void visitClass(PsiClass aClass)
			{
				PsiIdentifier nameIdentifier = aClass.getNameIdentifier();
				if(nameIdentifier == null)
				{
					return;
				}

				if(aClass.hasModifier(JvmModifier.ABSTRACT) || aClass.isInterface() || aClass.isAnnotationType() || aClass.isEnum() || aClass.isRecord())
				{
					return;
				}

				// not annotated by @ExtensionAPI, and not annotated by @ExtensionImpl, but has @ExtensionAPI in class hierarchy
				if(!AnnotationUtil.isAnnotated(aClass, ValhallaAnnotations.ExtensionAPI, 0) &&
						!AnnotationUtil.isAnnotated(aClass, ValhallaAnnotations.ExtensionImpl, 0) &&
						AnnotationUtil.isAnnotated(aClass, ValhallaAnnotations.ExtensionAPI, AnnotationUtil.CHECK_HIERARCHY))
				{
					AddAnnotationFix addAnnotationFix = new AddAnnotationFix(ValhallaAnnotations.ExtensionImpl, aClass);
					holder.registerProblem(nameIdentifier, "Extension implementation not annotated by @ExtensionImpl", ProblemHighlightType.GENERIC_ERROR_OR_WARNING, addAnnotationFix);
				}
			}
		};
	}
}
