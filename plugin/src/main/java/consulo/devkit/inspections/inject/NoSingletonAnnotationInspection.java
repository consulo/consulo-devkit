package consulo.devkit.inspections.inject;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInsight.intention.AddAnnotationFix;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementVisitor;
import consulo.annotation.access.RequiredReadAction;
import consulo.devkit.inspections.util.service.ServiceInfo;
import consulo.devkit.inspections.util.service.ServiceLocator;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2018-08-16
 */
public class NoSingletonAnnotationInspection extends LocalInspectionTool
{
	private static class Visitor extends JavaElementVisitor
	{
		private final ProblemsHolder myHolder;

		public Visitor(ProblemsHolder holder)
		{
			myHolder = holder;
		}

		@Override
		@RequiredReadAction
		public void visitClass(PsiClass aClass)
		{
			if(isSingleton(aClass) && !AnnotationUtil.isAnnotated(aClass, Singleton.class.getName(), 0))
			{
				myHolder.registerProblem(aClass.getNameIdentifier(), "Missed @Singleton annotation", new AddAnnotationFix(Singleton.class.getName(), aClass));
			}
		}
	}

	@Nonnull
	@Override
	public PsiElementVisitor buildVisitor(@Nonnull ProblemsHolder holder, boolean isOnTheFly)
	{
		return new Visitor(holder);
	}

	@RequiredReadAction
	private static boolean isSingleton(PsiClass psiClass)
	{
		ServiceInfo serviceInfo = ServiceLocator.findImplementationService(psiClass);
		return serviceInfo != null;
	}
}
