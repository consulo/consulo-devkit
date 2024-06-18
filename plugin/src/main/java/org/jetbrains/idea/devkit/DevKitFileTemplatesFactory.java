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
package org.jetbrains.idea.devkit;

import com.intellij.java.language.impl.JavaFileType;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.AllIcons;
import consulo.devkit.localize.DevKitLocalize;
import consulo.fileTemplate.FileTemplateDescriptor;
import consulo.fileTemplate.FileTemplateGroupDescriptor;
import consulo.fileTemplate.FileTemplateGroupDescriptorFactory;
import consulo.xml.ide.highlighter.HtmlFileType;

@ExtensionImpl
public class DevKitFileTemplatesFactory implements FileTemplateGroupDescriptorFactory {
  @Override
  public FileTemplateGroupDescriptor getFileTemplatesDescriptor() {
    FileTemplateGroupDescriptor descriptor =
      new FileTemplateGroupDescriptor(DevKitLocalize.pluginDescriptor().get(), AllIcons.Nodes.Plugin);

    descriptor.addTemplate(new FileTemplateDescriptor("Action.java", JavaFileType.INSTANCE.getIcon()));
    descriptor.addTemplate(new FileTemplateDescriptor("InspectionDescription.html", HtmlFileType.INSTANCE.getIcon()));
    return descriptor;
  }

}
