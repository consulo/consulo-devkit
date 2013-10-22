/*
 * Copyright 2013 Consulo.org
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
package org.consulo.lombok.devkit.processor.impl;

import org.jetbrains.annotations.NotNull;
import com.intellij.openapi.module.Module;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiType;

/**
 * @author VISTALL
 * @since 20:38/03.06.13
 */
public class ModuleServiceAnnotationProcessor extends QServiceAnnotationProcessor {
  public ModuleServiceAnnotationProcessor(String annotationClass) {
    super(annotationClass);
  }

  @NotNull
  @Override
  public PsiType[] getParameters(JavaPsiFacade javaPsiFacade) {
    return new PsiType[] {javaPsiFacade.getElementFactory().createTypeByFQClassName(Module.class.getName())};
  }
}
