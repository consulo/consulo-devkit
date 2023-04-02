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

import com.intellij.java.debugger.engine.DebuggerUtils;
import com.intellij.java.debugger.impl.DebugEnvironment;
import com.intellij.java.debugger.impl.DebuggerManagerEx;
import com.intellij.java.debugger.impl.DebuggerSession;
import com.intellij.java.debugger.impl.GenericDebuggerRunner;
import com.intellij.java.debugger.impl.engine.DebugProcessImpl;
import com.intellij.java.debugger.impl.engine.JavaDebugProcess;
import com.intellij.java.debugger.impl.settings.DebuggerSettings;
import com.intellij.java.debugger.impl.ui.tree.render.BatchEvaluator;
import com.intellij.java.execution.configurations.RemoteConnection;
import consulo.annotation.component.ExtensionImpl;
import consulo.execution.DefaultExecutionResult;
import consulo.execution.ExecutionResult;
import consulo.execution.configuration.RunProfile;
import consulo.execution.configuration.RunProfileState;
import consulo.execution.debug.XDebuggerManager;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.execution.ui.RunContentDescriptor;
import consulo.process.ExecutionException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 29.05.14
 */
@ExtensionImpl(order = "before defaultJavaDebugRunner")
public class ConsuloDebuggerRunner extends GenericDebuggerRunner {
  @Nonnull
  @Override
  public String getRunnerId() {
    return "ConsuloDebuggerRunner";
  }

  @Override
  public boolean canRun(@Nonnull String executorId, @Nonnull RunProfile profile) {
    return super.canRun(executorId, profile) && profile instanceof ConsuloRunConfigurationBase;
  }

  @Nullable
  @Override
  protected RunContentDescriptor createContentDescriptor(@Nonnull RunProfileState state,
                                                         @Nonnull ExecutionEnvironment env) throws ExecutionException {
    String address = DebuggerUtils.getInstance().findAvailableDebugAddress(DebuggerSettings.SOCKET_TRANSPORT).address();
    RemoteConnection connection = new RemoteConnection(true, "127.0.0.1", address, false);

    ConsuloSandboxRunState consuloSandboxRunState = (ConsuloSandboxRunState)state;

    consuloSandboxRunState.addAdditionalVMParameter(connection.getLaunchCommandLine());
    return attachVirtualMachine(consuloSandboxRunState, env, connection, true);
  }

  @Override
  @Nullable
  protected RunContentDescriptor attachVirtualMachine(RunProfileState state,
                                                      @Nonnull ExecutionEnvironment env,
                                                      RemoteConnection connection,
                                                      boolean pollConnection) throws ExecutionException {
    DebugEnvironment environment = new ConsuloDebugEnvironment(env, (ConsuloSandboxRunState)state, connection, pollConnection);
    final DebuggerSession debuggerSession = DebuggerManagerEx.getInstanceEx(env.getProject()).attachVirtualMachine(environment);
    if (debuggerSession == null) {
      return null;
    }
    else {
      final DebugProcessImpl debugProcess = debuggerSession.getProcess();
      if (!debugProcess.isDetached() && !debugProcess.isDetaching()) {
        if (environment.isRemote()) {
          debugProcess.putUserData(BatchEvaluator.REMOTE_SESSION_KEY, Boolean.TRUE);
        }

        return XDebuggerManager.getInstance(env.getProject()).startSession(env, session ->
        {
          ExecutionResult executionResult = debugProcess.getExecutionResult();
          session.addExtraActions(executionResult.getActions());
          if (executionResult instanceof DefaultExecutionResult) {
            session.addRestartActions(((DefaultExecutionResult)executionResult).getRestartActions());
          }

          return JavaDebugProcess.create(session, debuggerSession);
        }).getRunContentDescriptor();
      }
      else {
        debuggerSession.dispose();
        return null;
      }
    }
  }
}
