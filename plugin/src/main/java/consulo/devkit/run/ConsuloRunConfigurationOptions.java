/*
 * Copyright 2013-2026 consulo.io
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

import com.dslplatform.json.CompiledJson;
import consulo.execution.configuration.RunConfigurationOptions;
import consulo.util.lang.StringUtil;
import consulo.util.xml.serializer.annotation.Attribute;
import org.jspecify.annotations.Nullable;

@CompiledJson
public class ConsuloRunConfigurationOptions extends RunConfigurationOptions {
    private @Nullable String myJavaSdk;
    private @Nullable String myConsuloSdkPath;
    private @Nullable String myPluginsHomePath;
    private @Nullable String myVmParameters;
    private boolean myJava9Modules;

    @Attribute("javaSdk")
    public @Nullable String getJavaSdk() {
        return myJavaSdk;
    }

    public void setJavaSdk(@Nullable String javaSdk) {
        myJavaSdk = StringUtil.nullize(javaSdk, true);
    }

    @Attribute("consuloSdkPath")
    public @Nullable String getConsuloSdkPath() {
        return myConsuloSdkPath;
    }

    public void setConsuloSdkPath(@Nullable String consuloSdkPath) {
        myConsuloSdkPath = StringUtil.nullize(consuloSdkPath, true);
    }

    @Attribute("pluginsHomePath")
    public @Nullable String getPluginsHomePath() {
        return myPluginsHomePath;
    }

    public void setPluginsHomePath(@Nullable String pluginsHomePath) {
        myPluginsHomePath = StringUtil.nullize(pluginsHomePath, true);
    }

    @Attribute("vmParameters")
    public @Nullable String getVmParameters() {
        return myVmParameters;
    }

    public void setVmParameters(@Nullable String vmParameters) {
        myVmParameters = StringUtil.nullize(vmParameters, true);
    }

    @Attribute("java9Modules")
    public boolean isJava9Modules() {
        return myJava9Modules;
    }

    public void setJava9Modules(boolean java9Modules) {
        myJava9Modules = java9Modules;
    }
}
