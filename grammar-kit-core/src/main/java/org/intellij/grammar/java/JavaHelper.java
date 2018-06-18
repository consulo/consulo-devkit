/*
 * Copyright 2011-present Greg Shrago
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

package org.intellij.grammar.java;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.psi.NavigatablePsiElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.impl.FakePsiElement;

/**
 * @author gregsh
 */
public abstract class JavaHelper
{
	protected static class MyElement<T> extends FakePsiElement implements NavigatablePsiElement
	{
		private final T myDelegate;

		protected MyElement(T delegate)
		{
			myDelegate = delegate;
		}

		@Override
		public PsiElement getParent()
		{
			return null;
		}

		@Override
		public boolean equals(Object o)
		{
			if(this == o)
			{
				return true;
			}
			if(o == null || getClass() != o.getClass())
			{
				return false;
			}

			MyElement element = (MyElement) o;

			if(!myDelegate.equals(element.myDelegate))
			{
				return false;
			}

			return true;
		}

		public T getDelegate()
		{
			return myDelegate;
		}

		@Override
		public int hashCode()
		{
			return myDelegate.hashCode();
		}

		@Override
		public String toString()
		{
			return myDelegate.toString();
		}
	}

	public enum MethodType
	{
		STATIC,
		INSTANCE,
		CONSTRUCTOR
	}

	@Nonnull
	public static JavaHelper getJavaHelper(@Nonnull PsiElement context)
	{
		return ServiceManager.getService(context.getProject(), JavaHelper.class);
	}

	@Nullable
	public NavigatablePsiElement findClass(@Nullable String className)
	{
		return null;
	}

	@Nonnull
	public List<NavigatablePsiElement> findClassMethods(@Nullable String className, @Nonnull MethodType methodType, @Nullable String methodName, int paramCount, String... paramTypes)
	{
		return Collections.emptyList();
	}

	@Nullable
	public String getSuperClassName(@Nullable String className)
	{
		return null;
	}

	@Nonnull
	public List<String> getMethodTypes(@Nullable NavigatablePsiElement method)
	{
		return Collections.emptyList();
	}

	@Nonnull
	public String getDeclaringClass(@Nullable NavigatablePsiElement method)
	{
		return "";
	}

	@Nonnull
	public List<String> getAnnotations(@Nullable NavigatablePsiElement element)
	{
		return Collections.emptyList();
	}

	@Nullable
	public PsiReferenceProvider getClassReferenceProvider()
	{
		return null;
	}

	@Nullable
	public NavigationItem findPackage(@Nullable String packageName)
	{
		return null;
	}

	protected static boolean acceptsName(@Nullable String expected, @Nullable String actual)
	{
		return "*".equals(expected) || expected != null && expected.equals(actual);
	}
}
