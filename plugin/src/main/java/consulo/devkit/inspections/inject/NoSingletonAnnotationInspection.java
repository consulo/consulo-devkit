package consulo.devkit.inspections.inject;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInsight.intention.AddAnnotationFix;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementVisitor;
import consulo.annotation.access.RequiredReadAction;
import consulo.devkit.inspections.util.service.ServiceInfo;
import consulo.devkit.inspections.util.service.ServiceLocator;
import org.jetbrains.idea.devkit.inspections.internal.InternalInspection;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * @author VISTALL
 * @since 2018-08-16
 */
public class NoSingletonAnnotationInspection extends InternalInspection
{
	private static final List<String> SINGLETON_ANNOTATIONS = List.of("jakarta.inject.Singleton", "javax.inject.Singleton");

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
			if(isSingleton(aClass) && !AnnotationUtil.isAnnotated(aClass, SINGLETON_ANNOTATIONS, 0))
			{
				myHolder.registerProblem(aClass.getNameIdentifier(), "Missed @Singleton annotation", new AddAnnotationFix(SINGLETON_ANNOTATIONS.get(0), aClass));
			}
		}
	}

	@Override
	public PsiElementVisitor buildInternalVisitor(@Nonnull ProblemsHolder holder, boolean isOnTheFly)
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
