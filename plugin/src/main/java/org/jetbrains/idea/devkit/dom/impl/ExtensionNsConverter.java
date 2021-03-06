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
package org.jetbrains.idea.devkit.dom.impl;

import com.intellij.openapi.util.Condition;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xml.ConvertContext;
import com.intellij.util.xml.ResolvingConverter;
import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.jetbrains.idea.devkit.dom.IdeaPlugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/*
* Created by IntelliJ IDEA.
* User: sweinreuter
* Date: 06.12.2007
*/
public class ExtensionNsConverter extends ResolvingConverter<IdeaPlugin> {
  @Nonnull
  public Collection<? extends IdeaPlugin> getVariants(ConvertContext context) {
    final IdeaPlugin ideaPlugin = context.getInvocationElement().getParentOfType(IdeaPlugin.class, true);
    if (ideaPlugin == null) return Collections.emptyList();

    final Collection<String> dependencies = ExtensionDomExtender.getDependencies(ideaPlugin);
    final List<IdeaPlugin> depPlugins = new ArrayList<IdeaPlugin>();
    for (IdeaPlugin plugin : IdeaPluginConverter.getAllPlugins(context.getProject())) {
      final String value = plugin.getPluginId();
      if (value != null && dependencies.contains(value)) {
        depPlugins.add(plugin);
      }
    }
    return depPlugins;
  }

  public IdeaPlugin fromString(@Nullable @NonNls final String s, ConvertContext context) {
    final IdeaPlugin ideaPlugin = context.getInvocationElement().getParentOfType(IdeaPlugin.class, true);
    if (ideaPlugin == null) return null;
    if (s != null && s.equals(ideaPlugin.getPluginId())) {
      // a plugin can extend itself
      return ideaPlugin;
    }
    return ContainerUtil.find(getVariants(context), new Condition<IdeaPlugin>() {
      public boolean value(IdeaPlugin o) {
        final String id = o.getPluginId();
        return id != null && id.equals(s);
      }
    });
  }

  public String toString(@Nullable IdeaPlugin ideaPlugin, ConvertContext context) {
    return ideaPlugin != null ? ideaPlugin.getPluginId() : null;
  }
}
