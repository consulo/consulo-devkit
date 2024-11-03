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

package consulo.devkit.module.extension;

import consulo.disposer.Disposable;
import consulo.module.content.layer.ModuleRootLayer;
import consulo.module.extension.MutableModuleExtension;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2013-05-23
 */
public class PluginMutableModuleExtension extends PluginModuleExtension implements MutableModuleExtension<PluginModuleExtension> {
    public PluginMutableModuleExtension(@Nonnull String id, @Nonnull ModuleRootLayer module) {
        super(id, module);
    }

    @RequiredUIAccess
    @Nullable
    @Override
    public Component createConfigurationComponent(@Nonnull Disposable disposable, @Nonnull Runnable runnable) {
        return null;
    }

    @Override
    public void setEnabled(boolean val) {
        myIsEnabled = val;
    }

    @Override
    public boolean isModified(@Nonnull PluginModuleExtension pluginModuleExtension) {
        return isEnabled() != pluginModuleExtension.isEnabled();
    }
}
