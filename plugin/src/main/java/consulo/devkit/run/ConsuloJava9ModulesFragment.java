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

import consulo.devkit.localize.DevKitLocalize;
import consulo.execution.configuration.fragment.TagRunConfigurationFragment;
import consulo.localize.LocalizeValue;

public class ConsuloJava9ModulesFragment<T extends ConsuloRunConfigurationBase> implements TagRunConfigurationFragment<T> {
    @Override
    public String getId() {
        return "consuloJava9Modules";
    }

    @Override
    public LocalizeValue getName() {
        return DevKitLocalize.runConfigurationFragmentJava9Modules();
    }

    @Override
    public boolean isSet(T target) {
        return target.isEnabledJava9Modules();
    }

    @Override
    public void set(T target, boolean value) {
        target.setEnabledJava9Modules(value);
    }
}
