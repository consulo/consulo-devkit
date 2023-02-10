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

import com.intellij.java.coverage.JavaCoverageEngineExtension;
import consulo.annotation.component.ExtensionImpl;
import consulo.execution.configuration.RunConfigurationBase;

import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 12.04.2015
 */
@ExtensionImpl
public class ConsuloRunCoverageEngineExtension extends JavaCoverageEngineExtension {
  @Override
  public boolean isApplicableTo(@Nullable RunConfigurationBase runConfigurationBase) {
    return runConfigurationBase instanceof ConsuloRunConfigurationBase;
  }
}
