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
package org.jetbrains.idea.devkit.inspections;

import consulo.annotation.component.ExtensionImpl;
import consulo.xml.util.xml.highlighting.BasicDomElementsInspection;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.idea.devkit.DevKitBundle;
import org.jetbrains.idea.devkit.dom.IdeaPlugin;

import javax.annotation.Nonnull;

/**
 * @author mike
 */
@ExtensionImpl
public class PluginXmlDomInspection extends BasicDomElementsInspection<IdeaPlugin> {
  public PluginXmlDomInspection() {
    super(IdeaPlugin.class);
  }

  @Nls
  @Nonnull
  public String getGroupDisplayName() {
    return DevKitBundle.message("inspections.group.name");
  }

  @Nls
  @Nonnull
  public String getDisplayName() {
    return "Plugin.xml Validity";
  }

  @NonNls
  @Nonnull
  public String getShortName() {
    return "PluginXmlValidity";
  }
}
