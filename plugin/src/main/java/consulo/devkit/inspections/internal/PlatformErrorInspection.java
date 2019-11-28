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

import com.intellij.codeInspection.BaseJavaLocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.util.containers.MultiMap;
import consulo.annotation.access.RequiredReadAction;
import consulo.devkit.util.PluginModuleUtil;
import consulo.platform.Platform;

import javax.annotation.Nonnull;
import java.util.Collection;

/**
 * @author VISTALL
 * @since 23-Aug-17
 */
public class PlatformErrorInspection extends BaseJavaLocalInspectionTool
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

	@Nonnull
	@Override
	@RequiredReadAction
	public PsiElementVisitor buildVisitor(@Nonnull ProblemsHolder holder, boolean isOnTheFly)
	{
		if(PluginModuleUtil.searchClassInFileUseScope(holder.getFile(), Platform.class.getName()) == null)
		{
			return PsiElementVisitor.EMPTY_VISITOR;
		}

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
