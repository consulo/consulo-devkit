/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package org.jetbrains.idea.devkit.dom.impl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.jetbrains.idea.devkit.dom.ExtensionPoint;
import org.jetbrains.idea.devkit.dom.IdeaPlugin;
import com.intellij.openapi.util.text.StringUtil;

public abstract class ExtensionPointImpl implements ExtensionPoint
{
	@Nullable
	@Override
	public String getNamePrefix()
	{
		final IdeaPlugin plugin = getParentOfType(IdeaPlugin.class, false);
		if(plugin == null)
		{
			return null;
		}

		return StringUtil.notNullize(plugin.getPluginId(), "com.intellij");
	}

	@Nonnull
	@Override
	public String getEffectiveQualifiedName()
	{
		return getNamePrefix() + "." + getName().getRawText();
	}
}
