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
package org.jetbrains.idea.devkit.util;

import com.intellij.java.language.psi.PsiClass;
import consulo.devkit.localize.DevKitLocalize;
import consulo.devkit.module.extension.PluginModuleExtension;
import consulo.devkit.util.PluginModuleUtil;
import consulo.language.psi.PsiFile;
import consulo.language.util.IncorrectOperationException;
import consulo.language.util.ModuleUtilCore;
import consulo.module.Module;
import consulo.virtualFileSystem.ReadonlyStatusHandler;
import consulo.virtualFileSystem.VirtualFile;
import consulo.xml.psi.xml.XmlFile;
import consulo.xml.psi.xml.XmlTag;
import consulo.xml.util.xml.DomFileElement;
import consulo.xml.util.xml.DomManager;
import org.jetbrains.idea.devkit.dom.IdeaPlugin;

import javax.annotation.Nullable;

/**
 * @author swr
 */
public class DescriptorUtil {
  public static void processActions(XmlTag root, ActionType.Processor processor) {
    final ActionType[] types = ActionType.values();
    for (ActionType type : types) {
      type.process(root, processor);
    }
  }

  public interface Patcher {
    void patchPluginXml(XmlFile pluginXml, PsiClass klass) throws IncorrectOperationException;
  }

  public static void patchPluginXml(Patcher patcher,
                                    PsiClass klass,
                                    XmlFile... pluginXmls) throws IncorrectOperationException {
    final VirtualFile[] files = new VirtualFile[pluginXmls.length];
    int i = 0;
    for (XmlFile pluginXml : pluginXmls) {
      files[i++] = pluginXml.getVirtualFile();
    }

    final ReadonlyStatusHandler readonlyStatusHandler = ReadonlyStatusHandler.getInstance(klass.getProject());
    final ReadonlyStatusHandler.OperationStatus status = readonlyStatusHandler.ensureFilesWritable(files);
    if (status.hasReadonlyFiles()) {
      throw new IncorrectOperationException(DevKitLocalize.errorPluginXmlReadonly().get());
    }

    for (XmlFile pluginXml : pluginXmls) {
      patcher.patchPluginXml(pluginXml, klass);
    }
  }

  @Nullable
  public static DomFileElement<IdeaPlugin> getConsuloPlugin(XmlFile file) {
    return DomManager.getDomManager(file.getProject()).getFileElement(file, IdeaPlugin.class);
  }

  public static boolean isPluginXml(PsiFile file) {
    return file instanceof XmlFile xmlFile && getConsuloPlugin(xmlFile) != null;
  }

  @Nullable
  public static String getPluginId(Module plugin) {
    assert ModuleUtilCore.getExtension(plugin, PluginModuleExtension.class) != null;

    final XmlFile pluginXml = PluginModuleUtil.getPluginXml(plugin);
    if (pluginXml != null) {
      final XmlTag rootTag = pluginXml.getDocument().getRootTag();
      if (rootTag != null) {
        final XmlTag idTag = rootTag.findFirstSubTag("id");
        if (idTag != null) {
          return idTag.getValue().getTrimmedText();
        }

        final XmlTag nameTag = rootTag.findFirstSubTag("name");
        if (nameTag != null) {
          return nameTag.getValue().getTrimmedText();
        }
      }
    }
    return null;
  }
}
