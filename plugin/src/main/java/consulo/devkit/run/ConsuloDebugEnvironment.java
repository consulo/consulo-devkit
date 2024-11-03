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

package consulo.devkit.run;

import com.intellij.java.debugger.impl.DefaultDebugEnvironment;
import com.intellij.java.execution.configurations.RemoteConnection;
import consulo.content.bundle.Sdk;
import consulo.execution.runner.ExecutionEnvironment;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2017-05-23
 */
class ConsuloDebugEnvironment extends DefaultDebugEnvironment {
    private final ConsuloSandboxRunState myState;

    public ConsuloDebugEnvironment(
        @Nonnull ExecutionEnvironment environment,
        @Nonnull ConsuloSandboxRunState state,
        RemoteConnection remoteConnection,
        boolean pollConnection
    ) {
        super(environment, state, remoteConnection, pollConnection);
        myState = state;
    }

    @Nullable
    @Override
    public Sdk getRunJre() {
        return myState.getJavaSdk();
    }
}
