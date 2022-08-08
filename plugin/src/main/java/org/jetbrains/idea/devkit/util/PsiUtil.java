/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package org.jetbrains.idea.devkit.util;

import com.intellij.lang.jvm.JvmModifier;
import com.intellij.psi.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Konstantin Bulenkov
 */
public class PsiUtil
{
	private PsiUtil()
	{
	}

	public static boolean isInstantiable(@Nonnull PsiClass aClass)
	{
		if(aClass.hasModifier(JvmModifier.ABSTRACT) || aClass.isInterface() || aClass.isAnnotationType() || aClass.isEnum() || aClass.isRecord())
		{
			return false;
		}
		return true;
	}
	public static boolean isOneStatementMethod(@Nonnull PsiMethod method)
	{
		final PsiCodeBlock body = method.getBody();
		return body != null && body.getStatements().length == 1 && body.getStatements()[0] instanceof PsiReturnStatement;
	}

	@Nullable
	public static String getReturnedLiteral(PsiMethod method, PsiClass cls)
	{
		if(isOneStatementMethod(method))
		{
			final PsiExpression value = ((PsiReturnStatement) method.getBody().getStatements()[0]).getReturnValue();
			if(value instanceof PsiLiteralExpression)
			{
				final Object str = ((PsiLiteralExpression) value).getValue();
				return str == null ? null : str.toString();
			}
			else if(value instanceof PsiMethodCallExpression)
			{
				if(isSimpleClassNameExpression((PsiMethodCallExpression) value))
				{
					return cls.getName();
				}
			}
		}
		return null;
	}

	private static boolean isSimpleClassNameExpression(PsiMethodCallExpression expr)
	{
		String text = expr.getText();
		if(text == null)
		{
			return false;
		}
		text = text.replaceAll(" ", "").replaceAll("\n", "").replaceAll("\t", "").replaceAll("\r", "");
		return "getClass().getSimpleName()".equals(text) || "this.getClass().getSimpleName()".equals(text);
	}

	@Nullable
	public static PsiMethod findNearestMethod(String name, @Nullable PsiClass cls)
	{
		if(cls == null)
		{
			return null;
		}
		for(PsiMethod method : cls.getMethods())
		{
			if(method.getParameterList().getParametersCount() == 0 && method.getName().equals(name))
			{
				return method.getModifierList().hasModifierProperty(PsiModifier.ABSTRACT) ? null : method;
			}
		}
		return findNearestMethod(name, cls.getSuperClass());
	}

	@Nullable
	public static PsiExpression getReturnedExpression(PsiMethod method)
	{
		if(isOneStatementMethod(method))
		{
			return ((PsiReturnStatement) method.getBody().getStatements()[0]).getReturnValue();
		}
		else
		{
			return null;
		}
	}
}
