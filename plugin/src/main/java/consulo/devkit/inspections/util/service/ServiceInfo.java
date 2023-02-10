package consulo.devkit.inspections.util.service;

import consulo.language.psi.PsiElement;

/**
 * @author VISTALL
 * @since 2018-08-16
 */
public class ServiceInfo
{
	private String myInterface;
	private String myImplementation;
	private PsiElement myNavigatableElement;

	public ServiceInfo(String anInterface, String implementation, PsiElement navigatableElement)
	{
		myInterface = anInterface;
		myImplementation = implementation;
		myNavigatableElement = navigatableElement;
	}

	public PsiElement getNavigatableElement()
	{
		return myNavigatableElement;
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
