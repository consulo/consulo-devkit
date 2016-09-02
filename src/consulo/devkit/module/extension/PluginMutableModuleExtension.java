/*
 * Copyright 2013-2016 must-be.org
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

import javax.swing.JComponent;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.openapi.projectRoots.Sdk;
import consulo.annotations.RequiredDispatchThread;
import consulo.module.extension.MutableModuleExtensionWithSdk;
import consulo.module.extension.MutableModuleInheritableNamedPointer;
import consulo.roots.ModuleRootLayer;

/**
 * @author VISTALL
 * @since 1:58/23.05.13
 */
public class PluginMutableModuleExtension extends PluginModuleExtension implements MutableModuleExtensionWithSdk<PluginModuleExtension>
{
	public PluginMutableModuleExtension(@NotNull String id, @NotNull ModuleRootLayer module)
	{
		super(id, module);
	}

	@Nullable
	@Override
	@RequiredDispatchThread
	public JComponent createConfigurablePanel(@Nullable Runnable updateOnCheck)
	{
		return new DevKitModuleExtensionPanel(this, updateOnCheck);
	}

	@Override
	public void setEnabled(boolean val)
	{
		myIsEnabled = val;
	}

	@Override
	public boolean isModified(@NotNull PluginModuleExtension pluginModuleExtension)
	{
		return isModifiedImpl(pluginModuleExtension);
	}

	@NotNull
	@Override
	public MutableModuleInheritableNamedPointer<Sdk> getInheritableSdk()
	{
		return (MutableModuleInheritableNamedPointer<Sdk>) super.getInheritableSdk();
	}
}
