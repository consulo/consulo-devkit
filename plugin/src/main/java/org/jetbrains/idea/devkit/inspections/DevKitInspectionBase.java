/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

import com.intellij.java.analysis.impl.codeInspection.BaseJavaLocalInspectionTool;
import com.intellij.java.language.psi.JavaPsiFacade;
import com.intellij.java.language.psi.PsiClass;
import com.intellij.java.language.psi.PsiModifier;
import com.intellij.java.language.psi.PsiModifierListOwner;
import consulo.devkit.util.PluginModuleUtil;
import consulo.java.language.module.extension.JavaModuleExtension;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiErrorElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.util.ModuleUtilCore;
import consulo.module.Module;
import consulo.project.Project;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.AnAction;
import consulo.util.collection.ContainerUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.xml.psi.xml.*;
import org.jetbrains.idea.devkit.DevKitBundle;
import org.jetbrains.idea.devkit.util.ActionType;
import org.jetbrains.idea.devkit.util.DescriptorUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

/**
 * @author swr
 */
public abstract class DevKitInspectionBase extends BaseJavaLocalInspectionTool {

  @Override
  @Nonnull
  public String getGroupDisplayName() {
    return DevKitBundle.message("inspections.group.name");
  }

  @Override
  public boolean isEnabledByDefault() {
    return true;
  }

  @Nullable
  protected static Set<PsiClass> getRegistrationTypes(PsiClass psiClass, boolean includeActions) {
    final Project project = psiClass.getProject();
    final PsiFile psiFile = psiClass.getContainingFile();

    assert psiFile != null;

    final VirtualFile virtualFile = psiFile.getVirtualFile();
    if (virtualFile == null) return null;
    final Module module = ModuleUtilCore.findModuleForFile(virtualFile, project);

    if (module == null) return null;

    if (ModuleUtilCore.getExtension(module, JavaModuleExtension.class) != null) {
      return checkModule(module, psiClass, null, includeActions);
    }
    else {
      Set<PsiClass> types = null;
      final List<Module> modules = PluginModuleUtil.getCandidateModules(module);
      for (Module m : modules) {
        types = checkModule(m, psiClass, types, includeActions);
      }
      return types;
    }
  }

  @Nullable
  private static Set<PsiClass> checkModule(Module module, PsiClass psiClass, @Nullable Set<PsiClass> types, boolean includeActions) {
    final XmlFile pluginXml = PluginModuleUtil.getPluginXml(module);
    if (!isPluginXml(pluginXml)) return types;
    assert pluginXml != null;

    final XmlDocument document = pluginXml.getDocument();
    assert document != null;

    final XmlTag rootTag = document.getRootTag();
    assert rootTag != null;

    final String qualifiedName = psiClass.getQualifiedName();
    if (qualifiedName != null) {
      final RegistrationTypeFinder finder = new RegistrationTypeFinder(psiClass, types);


      if (includeActions) {
        DescriptorUtil.processActions(rootTag, finder);
      }

      types = finder.getTypes();
    }

    return types;
  }

  public static boolean isPluginXml(PsiFile file) {
    if (!(file instanceof XmlFile)) return false;
    final XmlFile pluginXml = (XmlFile)file;

    final XmlDocument document = pluginXml.getDocument();
    if (document == null) return false;
    final XmlTag rootTag = document.getRootTag();
    return rootTag != null && "idea-plugin".equals(rootTag.getLocalName());

  }

  @Nullable
  protected static PsiElement getAttValueToken(@Nonnull XmlAttribute attribute) {
    final XmlAttributeValue valueElement = attribute.getValueElement();
    if (valueElement == null) return null;

    final PsiElement[] children = valueElement.getChildren();
    if (children.length == 3 && children[1] instanceof XmlToken) {
      return children[1];
    }
    if (children.length == 1 && children[0] instanceof PsiErrorElement) return null;
    return valueElement;
  }

  protected static boolean isAbstract(PsiModifierListOwner checkedClass) {
    return checkedClass.hasModifierProperty(PsiModifier.ABSTRACT);
  }

  protected static boolean isPublic(PsiModifierListOwner checkedClass) {
    return checkedClass.hasModifierProperty(PsiModifier.PUBLIC);
  }

  protected static boolean isActionRegistered(PsiClass psiClass) {
    final Set<PsiClass> registrationTypes = getRegistrationTypes(psiClass, true);
    if (registrationTypes != null) {
      for (PsiClass type : registrationTypes) {
        if (AnAction.class.getName().equals(type.getQualifiedName())) return true;
        if (ActionGroup.class.getName().equals(type.getQualifiedName())) return true;
      }
    }
    return false;
  }

  static class RegistrationTypeFinder implements ActionType.Processor {
    private Set<PsiClass> myTypes;
    private final String myQualifiedName;
    private final PsiManager myManager;
    private final GlobalSearchScope myScope;

    public RegistrationTypeFinder(PsiClass psiClass, Set<PsiClass> types) {
      myTypes = types;
      myQualifiedName = psiClass.getQualifiedName();
      myManager = psiClass.getManager();
      myScope = psiClass.getResolveScope();
    }

    public boolean process(ActionType type, XmlTag action) {
      final String actionClass = action.getAttributeValue("class");
      if (actionClass != null) {
        if (actionClass.trim().equals(myQualifiedName)) {
          final PsiClass clazz = JavaPsiFacade.getInstance(myManager.getProject()).findClass(type.myClassName, myScope);
          if (clazz != null) {
            addType(clazz);
            return false;
          }
        }
      }
      return true;
    }

    private void addType(PsiClass clazz) {
      if (myTypes == null) {
        //noinspection unchecked
        myTypes = ContainerUtil.<PsiClass>newIdentityTroveSet(2);
      }
      myTypes.add(clazz);
    }

    public Set<PsiClass> getTypes() {
      return myTypes;
    }
  }
}
