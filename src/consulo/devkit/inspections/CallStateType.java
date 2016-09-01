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

import javax.swing.SwingUtilities;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.BaseActionRunnable;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.application.WriteAction;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.util.ArrayUtil;
import com.intellij.util.ui.UIUtil;
import consulo.annotations.RequiredDispatchThread;
import consulo.annotations.RequiredReadAction;
import consulo.annotations.RequiredWriteAction;
import consulo.ui.RequiredUIAccess;
import consulo.ui.UIAccess;

/**
 * @author VISTALL
 * @since 22-Jun-16
 */
public enum CallStateType
{
	NONE(null, null),
	READ(RequiredReadAction.class.getName(), ReadAction.class, new AcceptableMethodCallCheck(Application.class, "runReadAction"))
			{
				@Override
				public boolean isAcceptableActionType(@NotNull CallStateType type)
				{
					return type == READ || type == DISPATCH_THREAD || type == UI_ACCESS || type == WRITE;
				}
			},
	WRITE(RequiredWriteAction.class.getName(), WriteAction.class, new AcceptableMethodCallCheck(Application.class, "runWriteAction")),
	// replacement for DISPATCH_THREAD
	UI_ACCESS(RequiredUIAccess.class.getName(), null, new AcceptableMethodCallCheck(UIAccess.class.getName(), "give"))
			{
				@Override
				public boolean isAcceptableActionType(@NotNull CallStateType type)
				{
					// write actions required call from ui thread, and it inherit ui state
					return type == DISPATCH_THREAD || type == WRITE || type == UI_ACCESS;
				}
			},
	DISPATCH_THREAD(RequiredDispatchThread.class.getName(), null, new AcceptableMethodCallCheck[]{
			new AcceptableMethodCallCheck(Application.class, "invokeLater"),
			new AcceptableMethodCallCheck(Application.class, "invokeAndWait"),
			new AcceptableMethodCallCheck(UIUtil.class, "invokeAndWaitIfNeeded"),
			new AcceptableMethodCallCheck(UIUtil.class, "invokeLaterIfNeeded"),
			new AcceptableMethodCallCheck(SwingUtilities.class, "invokeAndWait"),
			new AcceptableMethodCallCheck(SwingUtilities.class, "invokeLater")
	})
			{
				@Override
				public boolean isAcceptableActionType(@NotNull CallStateType type)
				{
					// write actions required call from dispatch thread, and it inherit dispatch state
					return type == DISPATCH_THREAD || type == WRITE || type == UI_ACCESS;
				}
			};

	@Nullable
	private final String myActionClass;
	@Nullable
	private final Class<? extends BaseActionRunnable> myActionRunnable;
	@NotNull
	private final AcceptableMethodCallCheck[] myAcceptableMethodCallChecks;

	CallStateType(@Nullable String actionClass, @Nullable Class<? extends BaseActionRunnable> actionRunnable, @NotNull AcceptableMethodCallCheck... methodCallChecks)
	{
		myActionClass = actionClass;
		myAcceptableMethodCallChecks = methodCallChecks;
		myActionRunnable = actionRunnable;
	}

	@NotNull
	public static CallStateType findSelfActionType(@NotNull PsiMethod method)
	{
		for(CallStateType actionType : values())
		{
			String actionClass = actionType.myActionClass;
			if(actionClass == null)
			{
				continue;
			}

			if(AnnotationUtil.isAnnotated(method, actionClass, false))
			{
				return actionType;
			}
		}
		return NONE;
	}

	@NotNull
	public AcceptableMethodCallCheck[] getAcceptableMethodCallChecks()
	{
		return myAcceptableMethodCallChecks;
	}

	@NotNull
	public String getActionClass()
	{
		assert myActionClass != null;
		return myActionClass;
	}

	@NotNull
	public static CallStateType findActionType(@NotNull PsiMethod method)
	{
		PsiClass baseActionRunnable = JavaPsiFacade.getInstance(method.getProject()).findClass(BaseActionRunnable.class.getName(), method.getResolveScope());

		PsiMethod baseRunMethod = null;
		if(baseActionRunnable != null)
		{
			PsiMethod[] runMethods = baseActionRunnable.findMethodsByName("run", false);
			baseRunMethod = ArrayUtil.getFirstElement(runMethods);
		}

		for(CallStateType actionType : values())
		{
			String actionClass = actionType.myActionClass;
			if(actionClass == null)
			{
				continue;
			}

			if(AnnotationUtil.isAnnotated(method, actionClass, false))
			{
				return actionType;
			}

			if(baseRunMethod != null && actionType.myActionRunnable != null)
			{
				PsiMethod[] superMethods = method.findSuperMethods(baseActionRunnable);
				if(ArrayUtil.contains(baseRunMethod, superMethods))
				{
					PsiClass containingClass = method.getContainingClass();
					if(containingClass != null)
					{
						if(InheritanceUtil.isInheritor(containingClass, actionType.myActionRunnable.getName()))
						{
							return actionType;
						}
					}
				}
			}
		}
		return NONE;
	}

	public boolean isAcceptableActionType(@NotNull CallStateType type)
	{
		return type == this;
	}
}
