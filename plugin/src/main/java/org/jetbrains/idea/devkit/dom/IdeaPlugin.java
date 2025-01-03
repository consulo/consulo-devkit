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

// Generated on Wed Nov 07 17:26:02 MSK 2007
// DTD/Schema  :    plugin.dtd

package org.jetbrains.idea.devkit.dom;

import consulo.devkit.dom.Permissions;
import consulo.devkit.dom.Tags;
import consulo.util.lang.StringUtil;
import consulo.xml.util.xml.*;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.List;

@DefinesXml
public interface IdeaPlugin extends DomElement {
  @Required
  @Nonnull
  default String getPluginId() {
    return StringUtil.notNullize(getId().getValue());
  }

  @Nonnull
  @NameValue
  @Required
  GenericDomValue<String> getId();

  @Nonnull
  GenericDomValue<String> getVersion();

  @Nonnull
  @SubTag("platformVersion")
  GenericDomValue<String> getPlatformVersion();

  @Nonnull
  GenericAttributeValue<String> getUrl();

  @Nonnull
  GenericDomValue<String> getCategory();

  @Nonnull
  GenericDomValue<String> getName();

  @Nonnull
  List<GenericDomValue<String>> getDescriptions();

  GenericDomValue<String> addDescription();

  @Nonnull
  List<Vendor> getVendors();

  Vendor addVendor();

  @Nonnull
  @SubTagList("resource-bundle")
  @Deprecated
  List<GenericDomValue<String>> getResourceBundles();

  @Nonnull
  GenericDomValue<String> getLocalize();

  GenericDomValue<String> addResourceBundle();

  @Nonnull
  @SubTagList("depends")
  List<Dependency> getDependencies();

  @SubTagList("depends")
  Dependency addDependency();

  @Nonnull
  @SubTagList("actions")
  List<Actions> getActions();

  Actions addActions();

  @Nonnull
  GenericDomValue<Boolean> getExperimental();

  @Nullable
  Permissions getPermissions();

  @Nullable
  @Required
  Tags getTags();
}
