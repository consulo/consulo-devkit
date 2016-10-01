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

package consulo.devkit.inspections.requiredXAction.stateResolver;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.psi.PsiCall;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpressionList;
import com.intellij.util.ThreeState;
import com.intellij.util.ThrowableRunnable;
import consulo.devkit.inspections.requiredXAction.AcceptableMethodCallCheck;
import consulo.devkit.inspections.requiredXAction.CallStateType;

/**
 * @author VISTALL
 * @since 01-Oct-16
 */
public abstract class StateResolver
{
	@NotNull
	public abstract Pair<ThreeState, CallStateType> resolveState(CallStateType actionType, PsiCall expression);

	protected static Map<String, Class[]> ourInterfaces = new HashMap<String, Class[]>()
	{
		{
			put("compute", new Class[]{
					Computable.class,
					ThrowableComputable.class
			});
			put("run", new Class[]{
					Runnable.class,
					ThrowableRunnable.class
			});
		}
	};

	protected static boolean acceptActionTypeFromCall(@NotNull PsiExpressionList expressionList, @NotNull CallStateType actionType)
	{
		for(CallStateType type : CallStateType.values())
		{
			if(actionType.isAcceptableActionType(type))
			{
				PsiElement parent = expressionList.getParent();

				for(AcceptableMethodCallCheck acceptableMethodCallCheck : type.getAcceptableMethodCallChecks())
				{
					if(acceptableMethodCallCheck.accept(parent))
					{
						return true;
					}
				}
			}
		}
		return false;
	}
}
