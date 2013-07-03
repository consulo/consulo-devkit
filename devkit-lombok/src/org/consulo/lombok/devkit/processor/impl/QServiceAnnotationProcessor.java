package org.consulo.lombok.devkit.processor.impl;

import com.intellij.psi.*;
import com.intellij.psi.impl.light.LightMethodBuilder;
import com.intellij.psi.util.PsiTypesUtil;
import org.consulo.lombok.processors.LombokSelfClassProcessor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

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
