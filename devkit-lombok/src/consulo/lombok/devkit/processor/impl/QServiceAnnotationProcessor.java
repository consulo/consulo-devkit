/*
 * Copyright 2013 must-be.org
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
package consulo.lombok.devkit.processor.impl;

import java.util.List;

import org.consulo.lombok.processors.LombokSelfClassProcessor;
import org.jetbrains.annotations.NotNull;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiType;
import com.intellij.psi.impl.light.LightMethodBuilder;
import com.intellij.psi.util.PsiTypesUtil;

/**
 * @author VISTALL
 * @since 20:20/03.06.13
 */
public class QServiceAnnotationProcessor extends LombokSelfClassProcessor {
  public QServiceAnnotationProcessor(String annotationClass) {
    super(annotationClass);
  }

  @Override
  public void processElement(@NotNull PsiClass parent, @NotNull PsiClass psiClass, @NotNull List<PsiElement> result) {
    JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(parent.getProject());

    LightMethodBuilder builder = new LightMethodBuilder(parent.getManager(), parent.getLanguage(), "getInstance");
    builder.setMethodReturnType(javaPsiFacade.getElementFactory().createType(psiClass));
    builder.setContainingClass(parent);
    builder.setModifiers(PsiModifier.STATIC, PsiModifier.PUBLIC);

    int i = 0;
    for(PsiType psiType : getParameters(javaPsiFacade)) {
      final PsiClass parameterClass = PsiTypesUtil.getPsiClass(psiType);

      builder.addParameter(parameterClass == null ? "p" + i : parameterClass.getName().toLowerCase(), psiType);
      i++;
    }
    builder.setNavigationElement(getAffectedAnnotation(parent));
    result.add(builder);
  }

  @NotNull
  public PsiType[] getParameters(JavaPsiFacade javaPsiFacade) {
    return PsiType.EMPTY_ARRAY;
  }

  @NotNull
  @Override
  public Class<? extends PsiElement> getCollectorPsiElementClass() {
    return PsiMethod.class;
  }
}
