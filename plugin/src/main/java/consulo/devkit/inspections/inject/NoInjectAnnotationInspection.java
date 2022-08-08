package consulo.devkit.inspections.inject;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.lang.jvm.JvmModifier;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiMethod;
import consulo.annotation.access.RequiredReadAction;
import consulo.devkit.inspections.util.service.ServiceInfo;
import consulo.devkit.inspections.util.service.ServiceLocator;
import consulo.devkit.inspections.valhalla.ValhallaAnnotations;
import jakarta.inject.Inject;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2018-09-02
 */
public class NoInjectAnnotationInspection extends LocalInspectionTool
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
			if(!isInjectionTarget(aClass))
			{
				return;
			}

			PsiMethod[] constructors = aClass.getConstructors();
			if(constructors.length == 0)
			{
				// default constructor
				if(aClass.hasModifier(JvmModifier.PUBLIC))
				{
					return;
				}
			}
			else
			{
				PsiMethod defaultConstructor = null;
				for(PsiMethod constructor : constructors)
				{
					if(constructor.hasModifier(JvmModifier.PUBLIC) && constructor.getParameterList().getParametersCount() == 0)
					{
						defaultConstructor = constructor;
					}

					if(AnnotationUtil.isAnnotated(constructor, Inject.class.getName(), 0))
					{
						return;
					}
				}

				if(constructors.length == 1 && defaultConstructor != null)
				{
					return;
				}
			}

			myHolder.registerProblem(aClass.getNameIdentifier(), "Missed @Inject annotation");
		}
	}

	@Nonnull
	@Override
	public PsiElementVisitor buildVisitor(@Nonnull ProblemsHolder holder, boolean isOnTheFly)
	{
		return new Visitor(holder);
	}

	@RequiredReadAction
	private static boolean isInjectionTarget(PsiClass psiClass)
	{
		ServiceInfo serviceInfo = ServiceLocator.findImplementationService(psiClass);
		if(serviceInfo != null)
		{
			// old XML service
			return true;
		}

		for(String annotation : ValhallaAnnotations.Impl)
		{
			if(AnnotationUtil.isAnnotated(psiClass, annotation, 0))
			{
				return true;
			}
		}
		return false;
	}
}
