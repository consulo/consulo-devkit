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
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.psi.*;
import com.intellij.psi.impl.light.LightMethodBuilder;
import com.intellij.psi.impl.light.LightParameter;

/**
 * @author VISTALL
 * @since 1:49/22.10.13
 */
public class BundleAnnotationProcessor extends LombokSelfClassProcessor
{
  public BundleAnnotationProcessor(String annotationClass) {
    super(annotationClass);
  }

  @Override
  public void processElement(@NotNull PsiClass parent, @NotNull PsiClass psiClass, @NotNull List<PsiElement> result) {
    JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(parent.getProject());
    PsiElementFactory elementFactory = javaPsiFacade.getElementFactory();

    PsiJavaParserFacade parserFacade = javaPsiFacade.getParserFacade();

    PsiAnnotation affectedAnnotation = getAffectedAnnotation(psiClass);
    String value = calcAnnotationValue(affectedAnnotation, PsiAnnotation.DEFAULT_REFERENCED_METHOD_NAME, javaPsiFacade);
    if (value == null) {
      return;
    }

    createMessage0(parent, psiClass, result, parserFacade, affectedAnnotation, value);
    createMessage1(parent, psiClass, result, parserFacade, affectedAnnotation, value);
  }

  private static void createMessage0(PsiClass parent,
                                     PsiClass psiClass,
                                     List<PsiElement> result,
                                     PsiJavaParserFacade parserFacade,
                                     PsiAnnotation affectedAnnotation,
                                     String value) {
    LightMethodBuilder builder = new LightMethodBuilder(parent.getManager(), "message");
    builder.setContainingClass(psiClass);
    builder.setModifiers(PsiModifier.STATIC, PsiModifier.FINAL, PsiModifier.PUBLIC);
    builder.setNavigationElement(affectedAnnotation);
    builder.setMethodReturnType(CommonClassNames.JAVA_LANG_STRING);


    final PsiParameter parameterFromText = parserFacade.createParameterFromText("@org.jetbrains.annotations.PropertyKey(" +
                                                                                value +
                                                                                ") java.lang.String key", psiClass);

    builder.addParameter(new LightParameter("key", JavaPsiFacade.getElementFactory(parent.getProject())
      .createTypeByFQClassName(CommonClassNames.JAVA_LANG_STRING), builder, JavaLanguage.INSTANCE) {
      @NotNull
      @Override
      public PsiModifierList getModifierList() {
        return parameterFromText.getModifierList();
      }
    });

    result.add(builder);
  }

  private static void createMessage1(PsiClass parent,
                                     PsiClass psiClass,
                                     List<PsiElement> result,
                                     PsiJavaParserFacade parserFacade,
                                     PsiAnnotation affectedAnnotation,
                                     String value) {
    LightMethodBuilder builder = new LightMethodBuilder(parent.getManager(), "message");
    builder.setContainingClass(psiClass);
    builder.setModifiers(PsiModifier.STATIC, PsiModifier.FINAL, PsiModifier.PUBLIC);
    builder.setNavigationElement(affectedAnnotation);
    builder.setMethodReturnType(CommonClassNames.JAVA_LANG_STRING);


    final PsiParameter parameterFromText = parserFacade.createParameterFromText("@org.jetbrains.annotations.PropertyKey(" +
                                                                          value +
                                                                          ") java.lang.String key", psiClass);

    builder.addParameter(new LightParameter("key", JavaPsiFacade.getElementFactory(parent.getProject())
      .createTypeByFQClassName(CommonClassNames.JAVA_LANG_STRING), builder, JavaLanguage.INSTANCE) {
      @NotNull
      @Override
      public PsiModifierList getModifierList() {
        return parameterFromText.getModifierList();
      }
    });

    PsiClassType javaLangObject =
      JavaPsiFacade.getElementFactory(parent.getProject()).createTypeByFQClassName(CommonClassNames.JAVA_LANG_OBJECT);

    builder.addParameter("args", new PsiEllipsisType(javaLangObject), true);

    result.add(builder);
  }

  @Nullable
  public static String calcAnnotationValue(@NotNull PsiAnnotation annotation, @NonNls String attr, @NotNull JavaPsiFacade javaPsiFacade) {
    PsiElement value = annotation.findAttributeValue(attr);
    if (value instanceof PsiExpression) {
      Object o = javaPsiFacade.getConstantEvaluationHelper().computeConstantExpression(value);
      if (o instanceof String) {
        return (String)o;
      }
    }
    return null;
  }

  @NotNull
  @Override
  public Class<? extends PsiElement> getCollectorPsiElementClass() {
    return PsiMethod.class;
  }
}
