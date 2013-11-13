/*
 * Copyright 2013 must-be.org
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
package org.jetbrains.idea.devkit.module.extension;

import java.awt.BorderLayout;

import javax.swing.JComponent;
import javax.swing.JPanel;

import org.consulo.module.extension.MutableModuleExtensionWithSdk;
import org.consulo.module.extension.MutableModuleInheritableNamedPointer;
import org.consulo.module.extension.ui.ModuleExtensionWithSdkPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.util.Comparing;
import lombok.NonNull;

/**
 * @author VISTALL
 * @since 1:58/23.05.13
 */
public class PluginMutableModuleExtension extends PluginModuleExtension implements MutableModuleExtensionWithSdk<PluginModuleExtension>
{
	private PluginModuleExtension myPluginModuleExtension;

	public PluginMutableModuleExtension(@NotNull String id, @NotNull Module module, PluginModuleExtension pluginModuleExtension)
	{
		super(id, module);
		myPluginModuleExtension = pluginModuleExtension;

		commit(myPluginModuleExtension);
	}

	@Nullable
	@Override
	public JComponent createConfigurablePanel(@NonNull ModifiableRootModel rootModel, @Nullable Runnable updateOnCheck)
	{
		JPanel panel = new JPanel(new BorderLayout());
		panel.add(new ModuleExtensionWithSdkPanel(this, updateOnCheck), BorderLayout.NORTH);
		return new PluginConfigPanel(this, rootModel, updateOnCheck);
	}

	@Override
	public void setEnabled(boolean val)
	{
		myIsEnabled = val;
	}

	@Override
	public boolean isModified()
	{
		return isModifiedImpl(myPluginModuleExtension) || !Comparing.equal(myCustomPluginDirPointer, myPluginModuleExtension.myCustomPluginDirPointer);
	}

	@Override
	public void commit()
	{
		myPluginModuleExtension.commit(this);
	}

	@NotNull
	@Override
	public MutableModuleInheritableNamedPointer<Sdk> getInheritableSdk()
	{
		return (MutableModuleInheritableNamedPointer<Sdk>) super.getInheritableSdk();
	}
}
