/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

import java.io.File;

import org.consulo.java.platform.module.extension.JavaModuleExtension;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.devkit.sdk.ConsuloSdkType;
import org.jetbrains.idea.devkit.util.DescriptorUtil;
import com.intellij.execution.JavaTestPatcher;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.configurations.ParametersList;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.projectRoots.JavaSdkType;
import com.intellij.openapi.projectRoots.Sdk;
import lombok.NonNull;

/**
 * User: anna
 * Date: Mar 4, 2005
 */
public class JUnitDevKitPatcher implements JavaTestPatcher {

  @Override
  public void patchJavaParameters(@Nullable Module module, @NonNull JavaParameters javaParameters) {
    Sdk jdk = javaParameters.getJdk();
    jdk = ConsuloSdkType.findIdeaJdk(jdk);
    if (jdk == null) return;

    @NonNls String libPath = jdk.getHomePath() + File.separator + "lib";

    final ParametersList vm = javaParameters.getVMParametersList();
    vm.add("-Xbootclasspath/a:" + libPath + File.separator + "boot.jar");
    if (!vm.hasProperty("idea.load.plugins.id") && module != null && ModuleUtilCore.getExtension(module, JavaModuleExtension.class) != null) {
      final String id = DescriptorUtil.getPluginId(module);
      if (id != null) {
        vm.defineProperty("idea.load.plugins.id", id);
      }
    }

    final String sandboxHome = getSandboxPath(jdk);
    if (sandboxHome != null) {
      if (!vm.hasProperty("idea.home.path")) {
        vm.defineProperty("idea.home.path", sandboxHome + File.separator + "test");
      }
      if (!vm.hasProperty("idea.plugins.path")) {
        vm.defineProperty("idea.plugins.path", sandboxHome + File.separator + "plugins");
      }
    }

    javaParameters.getClassPath().addFirst(libPath + File.separator + "idea.jar");
    javaParameters.getClassPath().addFirst(libPath + File.separator + "resources.jar");
    javaParameters.getClassPath().addFirst(((JavaSdkType)jdk.getSdkType()).getToolsPath(jdk));
  }

  @Nullable
  private static String getSandboxPath(final Sdk jdk) {
    return null; //TODO [VISTALL]
  }
}
