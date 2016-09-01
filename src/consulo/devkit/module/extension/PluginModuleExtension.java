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

import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.devkit.sdk.ConsuloSdkType;
import com.intellij.openapi.projectRoots.SdkType;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.pointers.VirtualFilePointer;
import com.intellij.openapi.vfs.pointers.VirtualFilePointerManager;
import consulo.annotations.RequiredReadAction;
import consulo.extension.impl.ModuleExtensionWithSdkImpl;
import consulo.roots.ModuleRootLayer;

/**
 * @author VISTALL
 * @since 1:58/23.05.13
 */
public class PluginModuleExtension extends ModuleExtensionWithSdkImpl<PluginModuleExtension>
{
	private static final String CUSTOM_PLUGIN_DIR_URL = "custom-plugin-dir-url";

	protected VirtualFilePointer myCustomPluginDirPointer;

	public PluginModuleExtension(@NotNull String id, @NotNull ModuleRootLayer module)
	{
		super(id, module);
	}

	@NotNull
	@Override
	public Class<? extends SdkType> getSdkTypeClass()
	{
		return ConsuloSdkType.class;
	}

	@Nullable
	public String getCustomPluginDirPresentableUrl()
	{
		return myCustomPluginDirPointer == null ? null : myCustomPluginDirPointer.getPresentableUrl();
	}

	@Nullable
	public VirtualFile getCustomPluginDir()
	{
		return myCustomPluginDirPointer == null ? null : myCustomPluginDirPointer.getFile();
	}

	@Override
	public void commit(@NotNull PluginModuleExtension mutableModuleExtension)
	{
		super.commit(mutableModuleExtension);

		myCustomPluginDirPointer = mutableModuleExtension.myCustomPluginDirPointer;
	}

	@Override
	@RequiredReadAction
	protected void loadStateImpl(@NotNull Element element)
	{
		super.loadStateImpl(element);

		setCustomPluginDirUrl(element.getAttributeValue(CUSTOM_PLUGIN_DIR_URL));
	}

	public void setCustomPluginDirUrl(@Nullable String url)
	{
		myCustomPluginDirPointer = url == null ? null : VirtualFilePointerManager.getInstance().create(url, getModule(), null);
	}

	@Override
	protected void getStateImpl(@NotNull Element element)
	{
		super.getStateImpl(element);

		if(myCustomPluginDirPointer != null)
		{
			element.setAttribute(CUSTOM_PLUGIN_DIR_URL, myCustomPluginDirPointer.getUrl());
		}
	}
}
