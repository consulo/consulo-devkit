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
package org.jetbrains.idea.devkit.build;

import consulo.application.util.function.Computable;
import consulo.content.library.Library;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.layer.OrderEnumerator;
import consulo.util.collection.ArrayUtil;

import java.util.ArrayList;
import java.util.Set;

/**
 * User: anna
 * Date: Nov 24, 2004
 */
public class PluginBuildUtil {
    private PluginBuildUtil() {
    }

    public static void getDependencies(Module module, final Set<Module> modules) {
        productionRuntimeDependencies(module).forEachModule(dep -> {
            if (!modules.contains(dep)) {
                modules.add(dep);
                getDependencies(dep, modules);
            }
            return true;
        });
    }

    public static Module[] getWrongSetDependencies(final Module module) {
        return module.getProject().getApplication().runReadAction((Computable<Module[]>)() -> {
            ArrayList<Module> result = new ArrayList<>();
            final Module[] projectModules = ModuleManager.getInstance(module.getProject()).getModules();
            for (Module projectModule : projectModules) {
                if (ArrayUtil.find(ModuleRootManager.getInstance(projectModule).getDependencies(), module) > -1) {
                    result.add(projectModule);
                }
            }
            return result.toArray(new Module[result.size()]);
        });
    }

    public static void getLibraries(Module module, final Set<Library> libs) {
        productionRuntimeDependencies(module).forEachLibrary(library -> {
            libs.add(library);
            return true;
        });
    }

    private static OrderEnumerator productionRuntimeDependencies(Module module) {
        return OrderEnumerator.orderEntries(module).productionOnly().runtimeOnly();
    }
}
