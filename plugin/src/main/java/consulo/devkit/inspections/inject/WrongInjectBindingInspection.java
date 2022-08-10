package consulo.devkit.inspections.inject;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.*;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiFormatUtil;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.util.containers.ContainerUtil;
import consulo.annotation.access.RequiredReadAction;
import consulo.devkit.DevKitComponentScope;
import consulo.devkit.inspections.valhalla.ValhallaAnnotations;
import consulo.util.lang.Pair;
import org.jetbrains.idea.devkit.inspections.internal.InternalInspection;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;

/**
 * @author VISTALL
 * @since 10-Aug-22
 */
public class WrongInjectBindingInspection extends InternalInspection
{
	private static final Set<String> PROVIDERS = Set.of("jakarta.inject.Provider", "javax.inject.Provider");

	private static final String FILE_TYPE_REGISTRY = "consulo.virtualFileSystem.fileType.FileTypeRegistry";

	private static final Set<String> APPLICATION_IMPLICIT = Set.of("consulo.application.Application", FILE_TYPE_REGISTRY);
	private static final Set<String> PROJECT_IMPLICIT = Set.of("consulo.application.Application", "consulo.project.Project", FILE_TYPE_REGISTRY);
	private static final Set<String> MODULE_IMPLICIT = Set.of("consulo.application.Application", "consulo.project.Project", "consulo.module.Module", FILE_TYPE_REGISTRY);

	@Override
	public PsiElementVisitor buildInternalVisitor(@Nonnull ProblemsHolder holder, boolean isOnTheFly)
	{
		return new JavaElementVisitor()
		{
			@Override
			@RequiredReadAction
			public void visitParameter(PsiParameter parameter)
			{
				PsiElement declarationScope = parameter.getDeclarationScope();
				if(!(declarationScope instanceof PsiMethod))
				{
					return;
				}

				DevKitComponentScope thisScope = resolveComponentScope((PsiMethod) declarationScope);

				if(thisScope != null)
				{
					DevKitComponentScope injectScope = resolveServiceScope(parameter.getType(), thisScope, true);
					if(injectScope == null || injectScope.ordinal() > thisScope.ordinal())
					{
						holder.registerProblem(parameter.getTypeElement(), getDescription(thisScope, injectScope, parameter.getType()));
					}
				}
			}
		};
	}

	private String getDescription(@Nonnull DevKitComponentScope scope, @Nullable DevKitComponentScope targetScope, @Nonnull PsiType targetType)
	{
		if(targetScope == null)
		{
			return "Unknown inject binding " + StringUtil.SINGLE_QUOTER.apply(PsiFormatUtil.formatType(targetType, 0, PsiSubstitutor.EMPTY));
		}

		return "Target injecting scope too high. Current: " + scope + ", target: " + targetScope;
	}

	@Nullable
	@RequiredReadAction
	private static DevKitComponentScope resolveServiceScope(@Nullable PsiType type, @Nonnull DevKitComponentScope targetScope, boolean providerCheck)
	{
		if(!(type instanceof PsiClassType))
		{
			return null;
		}

		PsiClassType.ClassResolveResult classResolveResult = ((PsiClassType) type).resolveGenerics();

		PsiClass element = classResolveResult.getElement();
		if(element == null)
		{
			return null;
		}

		// some implicit bindings, without annotation
		Set<String> implicitBindings = getImplicitBindings(targetScope);
		if(implicitBindings.contains(element.getQualifiedName()))
		{
			return targetScope;
		}

		// it's service api
		PsiAnnotation serviceApiAnno = AnnotationUtil.findAnnotationInHierarchy(element, Set.of(ValhallaAnnotations.ServiceAPI));
		if(serviceApiAnno != null)
		{
			PsiAnnotationMemberValue value = serviceApiAnno.findDeclaredAttributeValue(PsiAnnotation.DEFAULT_REFERENCED_METHOD_NAME);
			if(value != null)
			{
				return resolveScope(value);
			}
		}

		if(providerCheck && PROVIDERS.contains(element.getQualifiedName()))
		{
			PsiSubstitutor substitutor = classResolveResult.getSubstitutor();
			PsiType genericIn = ContainerUtil.getFirstItem(substitutor.getSubstitutionMap().values());
			return resolveServiceScope(genericIn, targetScope, false);
		}

		return null;
	}

	@Nonnull
	private static Set<String> getImplicitBindings(DevKitComponentScope scope)
	{
		switch(scope)
		{
			case APPLICATION:
				return APPLICATION_IMPLICIT;
			case PROJECT:
				return PROJECT_IMPLICIT;
			case MODULE:
				return MODULE_IMPLICIT;
			default:
				throw new UnsupportedOperationException();
		}
	}

	@Nullable
	private static DevKitComponentScope resolveComponentScope(PsiMethod method)
	{
		return CachedValuesManager.getCachedValue(method, () -> CachedValueProvider.Result.create(resolveComponentScopeImpl(method), PsiModificationTracker.MODIFICATION_COUNT));
	}

	@RequiredReadAction
	@Nullable
	private static DevKitComponentScope resolveComponentScopeImpl(PsiMethod method)
	{
		if(!method.isConstructor())
		{
			return null;
		}

		PsiClass containingClass = method.getContainingClass();
		if(containingClass == null)
		{
			return null;
		}

		// owner contains inject annotation
		if(AnnotationUtil.isAnnotated(method, NoInjectAnnotationInspection.INJECT_ANNOTATIONS, 0))
		{
			for(Pair<String, String> apiAndImpl : ValhallaAnnotations.ApiToImpl)
			{
				PsiAnnotation implAnno = AnnotationUtil.findAnnotation(containingClass, apiAndImpl.getSecond());
				if(implAnno != null)
				{
					PsiAnnotationMemberValue value = implAnno.findDeclaredAttributeValue(PsiAnnotation.DEFAULT_REFERENCED_METHOD_NAME);
					if(value != null)
					{
						return resolveScope(value);
					}

					PsiAnnotation apiAnno = AnnotationUtil.findAnnotationInHierarchy(containingClass, Set.of(apiAndImpl.getFirst()));
					if(apiAnno != null)
					{
						value = apiAnno.findDeclaredAttributeValue(PsiAnnotation.DEFAULT_REFERENCED_METHOD_NAME);
						if(value != null)
						{
							return resolveScope(value);
						}
					}
				}
			}
		}
		return null;
	}

	@Nullable
	@RequiredReadAction
	private static DevKitComponentScope resolveScope(PsiAnnotationMemberValue value)
	{
		if(!(value instanceof PsiReferenceExpression))
		{
			return null;
		}

		PsiElement resolveTarget = ((PsiReferenceExpression) value).resolve();
		if(resolveTarget instanceof PsiEnumConstant)
		{
			String name = ((PsiEnumConstant) resolveTarget).getName();

			PsiClass containingClass = ((PsiEnumConstant) resolveTarget).getContainingClass();

			if(containingClass != null && "consulo.annotation.component.ComponentScope".equals(containingClass.getQualifiedName()))
			{
				try
				{
					return DevKitComponentScope.valueOf(name);
				}
				catch(IllegalArgumentException ignored)
				{
				}
			}
		}

		return null;
	}
}
