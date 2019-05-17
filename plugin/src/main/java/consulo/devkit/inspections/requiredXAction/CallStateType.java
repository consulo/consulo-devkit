/*
 * Copyright 2013-2016 consulo.io
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

package consulo.devkit.inspections.requiredXAction;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.psi.PsiMethod;
import com.intellij.util.ui.UIUtil;
import consulo.annotations.RequiredReadAction;
import consulo.annotations.RequiredWriteAction;
import consulo.ui.RequiredUIAccess;
import consulo.ui.UIAccess;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;

/**
 * @author VISTALL
 * @since 22-Jun-16
 */
public enum CallStateType
{
	NONE(null, null),
	READ(RequiredReadAction.class.getName(), new AcceptableMethodCallCheck[]{
			new AcceptableMethodCallCheck(Application.class, "runReadAction"),
			new AcceptableMethodCallCheck(ReadAction.class, "run"),
			new AcceptableMethodCallCheck(ReadAction.class, "compute")
	})
			{
				@Override
				public boolean isAcceptableActionType(@Nonnull CallStateType type)
				{
					return type == READ || type == DISPATCH_THREAD || type == UI_ACCESS || type == WRITE;
				}
			},
	WRITE(RequiredWriteAction.class.getName(), new AcceptableMethodCallCheck[]{
			new AcceptableMethodCallCheck(Application.class, "runWriteAction"),
			new AcceptableMethodCallCheck(WriteAction.class, "run"),
			new AcceptableMethodCallCheck(WriteAction.class, "compute"),
			new AcceptableMethodCallCheck(WriteCommandAction.class, "runWriteCommandAction")
	}),
	// replacement for DISPATCH_THREAD
	UI_ACCESS(RequiredUIAccess.class.getName(), new AcceptableMethodCallCheck(UIAccess.class.getName(), "give"))
			{
				@Override
				public boolean isAcceptableActionType(@Nonnull CallStateType type)
				{
					// write actions required call from ui thread, and it inherit ui state
					return type == DISPATCH_THREAD || type == WRITE || type == UI_ACCESS;
				}
			},
	DISPATCH_THREAD(RequiredUIAccess.class.getName(), new AcceptableMethodCallCheck[]{
			new AcceptableMethodCallCheck(Application.class, "invokeLater"),
			new AcceptableMethodCallCheck(Application.class, "invokeAndWait"),
			new AcceptableMethodCallCheck(UIUtil.class, "invokeAndWaitIfNeeded"),
			new AcceptableMethodCallCheck(UIUtil.class, "invokeLaterIfNeeded"),
			new AcceptableMethodCallCheck(SwingUtilities.class, "invokeAndWait"),
			new AcceptableMethodCallCheck(SwingUtilities.class, "invokeLater")
	})
			{
				@Override
				public boolean isAcceptableActionType(@Nonnull CallStateType type)
				{
					// write actions required call from dispatch thread, and it inherit dispatch state
					return type == DISPATCH_THREAD || type == WRITE || type == UI_ACCESS;
				}
			};

	@Nullable
	private final String myActionClass;
	@Nonnull
	private final AcceptableMethodCallCheck[] myAcceptableMethodCallChecks;

	CallStateType(@Nullable String actionClass, AcceptableMethodCallCheck... methodCallChecks)
	{
		myActionClass = actionClass;
		myAcceptableMethodCallChecks = methodCallChecks;
	}

	@Nonnull
	public static CallStateType findSelfActionType(@Nonnull PsiMethod method)
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

	@Nonnull
	public AcceptableMethodCallCheck[] getAcceptableMethodCallChecks()
	{
		return myAcceptableMethodCallChecks;
	}

	@Nonnull
	public String getActionClass()
	{
		assert myActionClass != null;
		return myActionClass;
	}

	@Nonnull
	public static CallStateType findActionType(@Nonnull PsiMethod method)
	{
		for(CallStateType actionType : values())
		{
			String actionClass = actionType.myActionClass;
			if(actionClass == null)
			{
				continue;
			}

			if(AnnotationUtil.isAnnotated(method, actionClass, 0))
			{
				return actionType;
			}
		}
		return NONE;
	}

	public boolean isAcceptableActionType(@Nonnull CallStateType type)
	{
		return type == this;
	}
}
