/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

import java.util.List;

import org.consulo.lombok.processors.LombokSelfClassProcessor;
import org.jetbrains.annotations.NotNull;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiArrayType;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.impl.light.LightFieldBuilder;
import com.intellij.util.ArrayFactory;

/**
 * @author VISTALL
 * @since 1:32/22.10.13
 */
public class ArrayFactoryFieldsAnnotationProcessor extends LombokSelfClassProcessor
{
  public ArrayFactoryFieldsAnnotationProcessor(String annotationClass) {
    super(annotationClass);
  }

  @Override
  public void processElement(@NotNull PsiClass parent, @NotNull PsiClass psiClass, @NotNull List<PsiElement> result) {
    PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(parent.getProject());

    PsiAnnotation affectedAnnotation = getAffectedAnnotation(psiClass);

    createEmptyArray(psiClass, result, elementFactory,affectedAnnotation);

    createArrayFactory(psiClass, result, elementFactory, affectedAnnotation);
  }

  private static void createArrayFactory(PsiClass psiClass,
                                  List<PsiElement> result,
                                  PsiElementFactory elementFactory,
                                  PsiAnnotation affectedAnnotation) {

    PsiClass arrayFactory = JavaPsiFacade.getInstance(psiClass.getProject()).findClass(ArrayFactory.class.getName(), psiClass.getResolveScope());

    if(arrayFactory == null) {
      return;
    }

    PsiClassType type = elementFactory.createType(arrayFactory, elementFactory.createType(psiClass));

    LightFieldBuilder builder = new LightFieldBuilder("ARRAY_FACTORY", type, affectedAnnotation);
    builder.setContainingClass(psiClass);
    builder.setModifiers(PsiModifier.STATIC, PsiModifier.FINAL, PsiModifier.PUBLIC);

    result.add(builder);
  }

  private static void createEmptyArray(PsiClass psiClass,
                                List<PsiElement> result,
                                PsiElementFactory elementFactory,
                                PsiAnnotation affectedAnnotation) {
    PsiClassType classType = elementFactory.createType(psiClass);
    PsiArrayType arrayType = new PsiArrayType(classType);

    LightFieldBuilder builder = new LightFieldBuilder("EMPTY_ARRAY", arrayType, affectedAnnotation);
    builder.setContainingClass(psiClass);
    builder.setModifiers(PsiModifier.STATIC, PsiModifier.FINAL, PsiModifier.PUBLIC);

    result.add(builder);
  }

  @NotNull
  @Override
  public Class<? extends PsiElement> getCollectorPsiElementClass() {
    return PsiField.class;
  }
}
