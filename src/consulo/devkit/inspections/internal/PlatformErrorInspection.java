/*
 * Copyright 2013-2017 consulo.io
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

package consulo.devkit.inspections.internal;

import java.util.Collection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.devkit.inspections.internal.InternalInspection;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.util.containers.MultiMap;
import consulo.annotations.RequiredReadAction;

/**
 * @author VISTALL
 * @since 23-Aug-17
 */
public class PlatformErrorInspection extends InternalInspection
{
	private final MultiMap<String, String> myRestrictedMethodList = MultiMap.create();

	public PlatformErrorInspection()
	{
		myRestrictedMethodList.putValue(System.class.getName(), "getProperties");
		myRestrictedMethodList.putValue(System.class.getName(), "getProperty");
		myRestrictedMethodList.putValue(System.class.getName(), "getenv");
		myRestrictedMethodList.putValue(System.class.getName(), "lineSeparator");
		myRestrictedMethodList.putValue(System.class.getName(), "setProperties");
		myRestrictedMethodList.putValue(System.class.getName(), "setProperty");
		myRestrictedMethodList.putValue(System.class.getName(), "clearProperty");

		myRestrictedMethodList.putValue(Boolean.class.getName(), "getBoolean");
		myRestrictedMethodList.putValue(Integer.class.getName(), "getInteger");
		myRestrictedMethodList.putValue(Long.class.getName(), "getLong");
	}

	@Override
	public PsiElementVisitor buildInternalVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly)
	{
		return new JavaElementVisitor()
		{
			@Override
			@RequiredReadAction
			public void visitMethodCallExpression(PsiMethodCallExpression expression)
			{
				PsiMethod method = expression.resolveMethod();
				if(method != null && myRestrictedMethodList.containsScalarValue(method.getName()))
				{
					PsiClass containingClass = method.getContainingClass();
					if(containingClass != null)
					{
						Collection<String> strings = myRestrictedMethodList.get(containingClass.getQualifiedName());
						if(strings.contains(method.getName()))
						{
							TextRange range = expression.getMethodExpression().getRangeInElement();
							holder.registerProblem(expression, "Platform call restricted. Use 'consulo.platform.Platform.current()'", ProblemHighlightType.GENERIC_ERROR, range);
						}
					}
				}
			}
		};
	}
}
