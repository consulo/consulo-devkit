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

package consulo.devkit.run;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.intellij.debugger.DebugEnvironment;
import com.intellij.debugger.DebuggerManagerEx;
import com.intellij.debugger.engine.DebugProcessImpl;
import com.intellij.debugger.engine.DebuggerUtils;
import com.intellij.debugger.engine.JavaDebugProcess;
import com.intellij.debugger.impl.DebuggerSession;
import com.intellij.debugger.impl.GenericDebuggerRunner;
import com.intellij.debugger.settings.DebuggerSettings;
import com.intellij.debugger.ui.tree.render.BatchEvaluator;
import com.intellij.execution.DefaultExecutionResult;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.configurations.RemoteConnection;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.xdebugger.XDebuggerManager;
import com.intellij.xdebugger.impl.XDebugSessionImpl;

/**
 * @author VISTALL
 * @since 29.05.14
 */
public class ConsuloDebuggerRunner extends GenericDebuggerRunner
{
	@Nonnull
	@Override
	public String getRunnerId()
	{
		return "ConsuloDebuggerRunner";
	}

	@Override
	public boolean canRun(@Nonnull String executorId, @Nonnull RunProfile profile)
	{
		return super.canRun(executorId, profile) && profile instanceof ConsuloRunConfigurationBase;
	}

	@Nullable
	@Override
	protected RunContentDescriptor createContentDescriptor(@Nonnull RunProfileState state, @Nonnull ExecutionEnvironment env) throws ExecutionException
	{
		String address = DebuggerUtils.getInstance().findAvailableDebugAddress(DebuggerSettings.SOCKET_TRANSPORT).address();
		RemoteConnection connection = new RemoteConnection(true, "127.0.0.1", address, false);

		ConsuloSandboxRunState consuloSandboxRunState = (ConsuloSandboxRunState) state;

		consuloSandboxRunState.getJavaParameters().getVMParametersList().addParametersString(connection.getLaunchCommandLine());
		return attachVirtualMachine(consuloSandboxRunState, env, connection, true);
	}

	@Override
	@Nullable
	protected RunContentDescriptor attachVirtualMachine(RunProfileState state, @Nonnull ExecutionEnvironment env, RemoteConnection connection, boolean pollConnection) throws ExecutionException
	{
		DebugEnvironment environment = new ConsuloDebugEnvironment(env, (ConsuloSandboxRunState) state, connection, pollConnection);
		final DebuggerSession debuggerSession = DebuggerManagerEx.getInstanceEx(env.getProject()).attachVirtualMachine(environment);
		if(debuggerSession == null)
		{
			return null;
		}
		else
		{
			final DebugProcessImpl debugProcess = debuggerSession.getProcess();
			if(!debugProcess.isDetached() && !debugProcess.isDetaching())
			{
				if(environment.isRemote())
				{
					debugProcess.putUserData(BatchEvaluator.REMOTE_SESSION_KEY, Boolean.TRUE);
				}

				return XDebuggerManager.getInstance(env.getProject()).startSession(env, session ->
				{
					XDebugSessionImpl sessionImpl = (XDebugSessionImpl) session;
					ExecutionResult executionResult = debugProcess.getExecutionResult();
					sessionImpl.addExtraActions(executionResult.getActions());
					if(executionResult instanceof DefaultExecutionResult)
					{
						sessionImpl.addRestartActions(((DefaultExecutionResult) executionResult).getRestartActions());
					}

					return JavaDebugProcess.create(session, debuggerSession);
				}).getRunContentDescriptor();
			}
			else
			{
				debuggerSession.dispose();
				return null;
			}
		}
	}
}
