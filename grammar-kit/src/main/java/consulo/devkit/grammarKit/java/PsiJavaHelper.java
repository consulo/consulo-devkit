package consulo.devkit.grammarKit.java;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import org.intellij.grammar.java.JavaHelper;
import org.jetbrains.annotations.NotNull;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.JavaClassReferenceProvider;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.ContainerUtilRt;

/**
 * @author VISTALL
 * @since 2018-06-17
 */
public class PsiJavaHelper extends JavaHelper
{
	private final JavaPsiFacade myFacade;
	private final PsiElementFactory myElementFactory;

	private PsiJavaHelper(JavaPsiFacade facade, PsiElementFactory elementFactory)
	{
		myFacade = facade;
		myElementFactory = elementFactory;
	}

	@Override
	public PsiReferenceProvider getClassReferenceProvider()
	{
		JavaClassReferenceProvider provider = new JavaClassReferenceProvider();
		provider.setSoft(false);
		return provider;
	}

	@Override
	public NavigatablePsiElement findClass(String className)
	{
		PsiClass aClass = findClassSafe(className);
		return aClass != null ? aClass : super.findClass(className);
	}

	private PsiClass findClassSafe(String className)
	{
		if(className == null)
		{
			return null;
		}
		try
		{
			return myFacade.findClass(className, GlobalSearchScope.allScope(myFacade.getProject()));
		}
		catch(IndexNotReadyException e)
		{
			return null;
		}
	}

	@Override
	public NavigationItem findPackage(String packageName)
	{
		return myFacade.findPackage(packageName);
	}

	@NotNull
	@Override
	public List<NavigatablePsiElement> findClassMethods(@Nullable String className, @NotNull MethodType methodType, @Nullable String methodName, int paramCount, String... paramTypes)
	{
		if(methodName == null)
		{
			return Collections.emptyList();
		}
		PsiClass aClass = findClassSafe(className);
		if(aClass == null)
		{
			return super.findClassMethods(className, methodType, methodName, paramCount, paramTypes);
		}
		List<NavigatablePsiElement> result = ContainerUtil.newArrayList();
		PsiMethod[] methods = methodType == MethodType.CONSTRUCTOR ? aClass.getConstructors() : aClass.getMethods();
		for(PsiMethod method : methods)
		{
			if(!acceptsName(methodName, method.getName()))
			{
				continue;
			}
			if(!acceptsMethod(method, methodType == MethodType.STATIC))
			{
				continue;
			}
			if(!acceptsMethod(myElementFactory, method, paramCount, paramTypes))
			{
				continue;
			}
			result.add(method);
		}
		return result;
	}

	@Nullable
	@Override
	public String getSuperClassName(@Nullable String className)
	{
		PsiClass aClass = findClassSafe(className);
		PsiClass superClass = aClass != null ? aClass.getSuperClass() : null;
		return superClass != null ? superClass.getQualifiedName() : super.getSuperClassName(className);
	}

	private static boolean acceptsMethod(PsiElementFactory elementFactory, PsiMethod method, int paramCount, String... paramTypes)
	{
		PsiParameterList parameterList = method.getParameterList();
		if(paramCount >= 0 && paramCount != parameterList.getParametersCount())
		{
			return false;
		}
		if(paramTypes.length == 0)
		{
			return true;
		}
		if(parameterList.getParametersCount() < paramTypes.length)
		{
			return false;
		}
		PsiParameter[] psiParameters = parameterList.getParameters();
		for(int i = 0; i < paramTypes.length; i++)
		{
			String paramType = paramTypes[i];
			PsiParameter parameter = psiParameters[i];
			PsiType psiType = parameter.getType();
			if(acceptsName(paramType, psiType.getCanonicalText()))
			{
				continue;
			}
			try
			{
				if(psiType.isAssignableFrom(elementFactory.createTypeFromText(paramType, parameter)))
				{
					continue;
				}
			}
			catch(IncorrectOperationException ignored)
			{
			}
			return false;
		}
		return true;
	}

	private static boolean acceptsMethod(PsiMethod method, boolean staticMethods)
	{
		PsiModifierList modifierList = method.getModifierList();
		return staticMethods == modifierList.hasModifierProperty(PsiModifier.STATIC) && !modifierList.hasModifierProperty(PsiModifier.ABSTRACT) && (modifierList.hasModifierProperty(PsiModifier.PUBLIC) || !(modifierList.hasModifierProperty(PsiModifier.PROTECTED) || modifierList.hasModifierProperty(PsiModifier.PRIVATE)));
	}

	@NotNull
	@Override
	public List<String> getMethodTypes(NavigatablePsiElement method)
	{
		if(!(method instanceof PsiMethod))
		{
			return super.getMethodTypes(method);
		}
		PsiMethod psiMethod = (PsiMethod) method;
		PsiType returnType = psiMethod.getReturnType();
		List<String> strings = new ArrayList<String>();
		strings.add(returnType == null ? "" : returnType.getCanonicalText());
		for(PsiParameter parameter : psiMethod.getParameterList().getParameters())
		{
			PsiType type = parameter.getType();
			boolean generic = type instanceof PsiClassType && ((PsiClassType) type).resolve() instanceof PsiTypeParameter;
			strings.add((generic ? "<" : "") + type.getCanonicalText(false) + (generic ? ">" : ""));
			strings.add(parameter.getName());
		}
		return strings;
	}

	@NotNull
	@Override
	public String getDeclaringClass(@Nullable NavigatablePsiElement method)
	{
		if(!(method instanceof PsiMethod))
		{
			return super.getDeclaringClass(method);
		}
		PsiMethod psiMethod = (PsiMethod) method;
		PsiClass aClass = psiMethod.getContainingClass();
		return aClass == null ? "" : StringUtil.notNullize(aClass.getQualifiedName());
	}

	@NotNull
	@Override
	public List<String> getAnnotations(NavigatablePsiElement element)
	{
		if(!(element instanceof PsiModifierListOwner))
		{
			return super.getAnnotations(element);
		}
		PsiModifierList modifierList = ((PsiModifierListOwner) element).getModifierList();
		if(modifierList == null)
		{
			return ContainerUtilRt.emptyList();
		}
		List<String> strings = new ArrayList<String>();
		for(PsiAnnotation annotation : modifierList.getAnnotations())
		{
			if(annotation.getParameterList().getAttributes().length > 0)
			{
				continue;
			}
			strings.add(annotation.getQualifiedName());
		}
		return strings;
	}
}