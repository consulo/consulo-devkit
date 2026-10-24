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

import org.jspecify.annotations.Nullable;

/**
 * The {@code <option name value>} fields a sandbox configuration wrote before the fragment model.
 */
public class ConsuloRunConfigurationLegacyState {
    public @Nullable String VM_PARAMETERS;
    public boolean ENABLED_JAVA9_MODULES;
    public @Nullable String PROGRAM_PARAMETERS;
    public @Nullable String PLUGINS_HOME_PATH;
    public @Nullable String ALT_CONSULO_SDK_PATH;
}
