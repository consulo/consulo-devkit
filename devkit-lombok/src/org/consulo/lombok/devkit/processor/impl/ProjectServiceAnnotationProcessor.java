package org.consulo.lombok.devkit.processor.impl;

import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiType;
import org.jetbrains.annotations.NotNull;

/**
 * @author VISTALL
 * @since 20:38/03.06.13
 */
public class ProjectServiceAnnotationProcessor extends QServiceAnnotationProcessor {
  public ProjectServiceAnnotationProcessor(String annotationClass) {
    super(annotationClass);
  }

  @NotNull
  @Override
  public PsiType[] getParameters(JavaPsiFacade javaPsiFacade) {
    return new PsiType[] {javaPsiFacade.getElementFactory().createTypeByFQClassName(Project.class.getName())};
  }
}
