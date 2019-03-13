package consulo.devkit.inspections.util.service;

import com.intellij.openapi.extensions.impl.ExtensionAreaId;
import com.intellij.psi.PsiElement;

/**
 * @author VISTALL
 * @since 2018-08-16
 */
public class ServiceInfo
{
	private ExtensionAreaId myArea;
	private String myInterface;
	private String myImplementation;
	private PsiElement myNavigatableElement;

	public ServiceInfo(ExtensionAreaId area, String anInterface, String implementation, PsiElement navigatableElement)
	{
		myArea = area;
		myInterface = anInterface;
		myImplementation = implementation;
		myNavigatableElement = navigatableElement;
	}

	public PsiElement getNavigatableElement()
	{
		return myNavigatableElement;
	}

	public ExtensionAreaId getArea()
	{
		return myArea;
	}

	public String getImplementation()
	{
		return myImplementation;
	}

	public String getInterface()
	{
		return myInterface;
	}
}
