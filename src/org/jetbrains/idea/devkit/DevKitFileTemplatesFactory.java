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

import com.intellij.icons.AllIcons;
import com.intellij.ide.fileTemplates.FileTemplateDescriptor;
import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptor;
import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptorFactory;
import com.intellij.ide.highlighter.HtmlFileType;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.ide.highlighter.XmlFileType;

public class DevKitFileTemplatesFactory implements FileTemplateGroupDescriptorFactory
{
	@Override
	public FileTemplateGroupDescriptor getFileTemplatesDescriptor()
	{
		FileTemplateGroupDescriptor descriptor = new FileTemplateGroupDescriptor(DevKitBundle.message("plugin.descriptor"), AllIcons.Nodes.Plugin);
		descriptor.addTemplate(new FileTemplateDescriptor("plugin.xml", XmlFileType.INSTANCE.getIcon()));
		descriptor.addTemplate(new FileTemplateDescriptor("ProjectComponent.java", JavaFileType.INSTANCE.getIcon()));
		descriptor.addTemplate(new FileTemplateDescriptor("ApplicationComponent.java", JavaFileType.INSTANCE.getIcon()));
		descriptor.addTemplate(new FileTemplateDescriptor("ModuleComponent.java", JavaFileType.INSTANCE.getIcon()));
		descriptor.addTemplate(new FileTemplateDescriptor("Action.java", JavaFileType.INSTANCE.getIcon()));
		descriptor.addTemplate(new FileTemplateDescriptor("InspectionDescription.html", HtmlFileType.INSTANCE.getIcon()));
		return descriptor;
	}

}
