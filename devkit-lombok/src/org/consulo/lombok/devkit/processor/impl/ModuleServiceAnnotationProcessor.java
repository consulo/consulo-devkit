package org.consulo.lombok.devkit.processor.impl;

import com.intellij.openapi.module.Module;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiType;
import org.jetbrains.annotations.NotNull;

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
