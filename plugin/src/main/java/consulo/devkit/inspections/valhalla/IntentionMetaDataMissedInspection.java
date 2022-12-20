package consulo.devkit.inspections.valhalla;

import com.intellij.codeHighlighting.HighlightDisplayLevel;
import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInsight.intention.AddAnnotationFix;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.*;
import consulo.annotation.access.RequiredReadAction;
import org.jetbrains.idea.devkit.inspections.internal.InternalInspection;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 18/12/2022
 */
public class IntentionMetaDataMissedInspection extends InternalInspection
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

				if(!AnnotationUtil.isAnnotated(aClass, ValhallaClasses.ExtensionImpl, 0))
				{
					return;
				}

				// already annotated
				if(AnnotationUtil.isAnnotated(aClass, ValhallaClasses.IntentionMetaData, 0))
				{
					return;
				}

				JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(aClass.getProject());
				PsiClass intentionAction = javaPsiFacade.findClass(ValhallaClasses.IntentionAction, aClass.getResolveScope());
				if(intentionAction == null)
				{
					return;
				}

				if(!aClass.isInheritor(intentionAction, true))
				{
					return;
				}

				PsiClass syntheticAction = javaPsiFacade.findClass(ValhallaClasses.SyntheticIntentionAction, aClass.getResolveScope());
				if(syntheticAction == null)
				{
					return;
				}


				if(aClass.isInheritor(syntheticAction, true))
				{
					return;
				}

				holder.registerProblem(nameIdentifier, "Missed @IntentionMetaData annotation", new AddAnnotationFix(ValhallaClasses.IntentionMetaData, aClass));
			}
		};
	}

	@Nonnull
	@Override
	public HighlightDisplayLevel getDefaultLevel()
	{
		return HighlightDisplayLevel.ERROR;
	}
}
