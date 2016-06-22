/*
 * Copyright 2013-2016 must-be.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package consulo.devkit.inspections;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;

/**
 * @author VISTALL
 * @since 22-Jun-16
 */
public class AcceptableMethodCallCheck
{
	private final String myParentClass;
	private final String myMethodName;

	public AcceptableMethodCallCheck(String parentClass, String methodName)
	{
		myParentClass = parentClass;
		myMethodName = methodName;
	}

	public AcceptableMethodCallCheck(Class<?> parentClass, String methodName)
	{
		this(parentClass.getName(), methodName);
	}

	public boolean accept(PsiElement parent)
	{
		if(parent instanceof PsiMethodCallExpression)
		{
			PsiMethod psiMethod = ((PsiMethodCallExpression) parent).resolveMethod();
			if(psiMethod == null)
			{
				return false;
			}

			if(myMethodName.equals(psiMethod.getName()))
			{
				PsiClass containingClass = psiMethod.getContainingClass();
				if(containingClass == null)
				{
					return false;
				}

				if(myParentClass.equals(containingClass.getQualifiedName()))
				{
					return true;
				}
			}
		}
		return false;
	}
}
