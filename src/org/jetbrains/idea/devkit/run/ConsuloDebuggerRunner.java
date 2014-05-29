/*
 * Copyright 2013-2014 must-be.org
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

package org.jetbrains.idea.devkit.run;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.debugger.engine.DebuggerUtils;
import com.intellij.debugger.impl.GenericDebuggerRunner;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.RemoteConnection;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.project.Project;

/**
 * @author VISTALL
 * @since 29.05.14
 */
public class ConsuloDebuggerRunner extends GenericDebuggerRunner
{
	@Override
	public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile)
	{
		return super.canRun(executorId, profile) && profile instanceof PluginRunConfiguration;
	}

	@Nullable
	@Override
	protected RunContentDescriptor createContentDescriptor(
			Project project, RunProfileState state, RunContentDescriptor contentToReuse, ExecutionEnvironment env) throws ExecutionException
	{
		String address = DebuggerUtils.getInstance().findAvailableDebugAddress(true);
		RemoteConnection connection = new RemoteConnection(true, "127.0.0.1", address, false);

		ConsuloSandboxRunState consuloSandboxRunState = (ConsuloSandboxRunState) state;

		consuloSandboxRunState.getJavaParameters().getVMParametersList().addParametersString(connection.getLaunchCommandLine());
		return attachVirtualMachine(project, state, contentToReuse, env, connection, true);
	}
}
