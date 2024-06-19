/*
 * Copyright 2000-2011 JetBrains s.r.o.
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

import com.intellij.java.language.codeInsight.AnnotationUtil;
import com.intellij.java.language.psi.*;
import com.intellij.java.language.psi.util.InheritanceUtil;
import consulo.annotation.UsedInPlugin;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.ImplicitUsageProvider;
import consulo.language.psi.PsiElement;
import consulo.xml.util.xml.DomElement;

/**
 * User: anna
 */
@ExtensionImpl
public class DevKitEntryPoints implements ImplicitUsageProvider {
  private static final String domClassName = DomElement.class.getName();

  @Override
  public boolean isImplicitUsage(PsiElement element) {
    if (element instanceof PsiClass psiClass) {
      if (InheritanceUtil.isInheritor(psiClass, domClassName)) {
        return true;
      }
    }
    
    if (element instanceof PsiField || element instanceof PsiMethod || element instanceof PsiClass) {
      return AnnotationUtil.isAnnotated((PsiModifierListOwner)element, UsedInPlugin.class.getName(), 0);
    }
    return false;
  }

  @Override
  public boolean isImplicitRead(PsiElement element) {
    return false;
  }

  @Override
  public boolean isImplicitWrite(PsiElement element) {
    return false;
  }
}
