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

package consulo.devkit;

import org.jetbrains.annotations.NotNull;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.util.Comparing;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import consulo.annotations.RequiredReadAction;
import consulo.devkit.module.extension.PluginModuleExtension;
import consulo.ide.IconDescriptor;
import consulo.ide.IconDescriptorUpdater;

/**
 * @author VISTALL
 * @since 06-Oct-16
 */
public class ConsuloDevkitIconDescriptorUpdater implements IconDescriptorUpdater
{
	@RequiredReadAction
	@Override
	public void updateIcon(@NotNull IconDescriptor iconDescriptor, @NotNull PsiElement psiElement, int i)
	{
		if(psiElement instanceof PsiDirectory)
		{
			String name = ((PsiDirectory) psiElement).getName();
			if(name.equals("dep"))
			{
				PsiDirectory parentDirectory = ((PsiDirectory) psiElement).getParentDirectory();
				if(parentDirectory != null && Comparing.equal(parentDirectory.getVirtualFile(), psiElement.getProject().getBaseDir()))
				{
					PluginModuleExtension extension = ModuleUtilCore.getExtension(psiElement, PluginModuleExtension.class);
					if(extension != null)
					{
						iconDescriptor.setMainIcon(ConsuloSandboxIcons.Package);
					}
				}
			}
		}
	}
}
