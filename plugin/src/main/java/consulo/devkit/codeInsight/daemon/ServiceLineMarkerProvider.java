package consulo.devkit.codeInsight.daemon;

import com.intellij.java.language.JavaLanguage;
import com.intellij.java.language.psi.JavaTokenType;
import com.intellij.java.language.psi.PsiClass;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.AllIcons;
import consulo.codeEditor.markup.GutterIconRenderer;
import consulo.devkit.inspections.util.service.ServiceInfo;
import consulo.devkit.inspections.util.service.ServiceLocator;
import consulo.language.Language;
import consulo.language.editor.Pass;
import consulo.language.editor.gutter.LineMarkerInfo;
import consulo.language.editor.gutter.LineMarkerProvider;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiUtilCore;
import consulo.language.psi.util.PsiNavigateUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2018-08-16
 */
@ExtensionImpl
public class ServiceLineMarkerProvider implements LineMarkerProvider {
  @RequiredReadAction
  @Nullable
  @Override
  public LineMarkerInfo getLineMarkerInfo(@Nonnull PsiElement psiElement) {
    if (PsiUtilCore.getElementType(psiElement) == JavaTokenType.IDENTIFIER && psiElement.getParent() instanceof PsiClass) {
      PsiClass psiClass = (PsiClass)psiElement.getParent();

      ServiceInfo info = ServiceLocator.findAnyService(psiClass);
      if (info != null) {
        return new LineMarkerInfo<>(psiElement,
                                    psiElement.getTextRange(),
                                    AllIcons.Nodes.Plugin,
                                    Pass.LINE_MARKERS,
                                    element -> "Service",
                                    (mouseEvent, element) ->
                                    {
                                      ServiceInfo info2 = ServiceLocator.findAnyService((PsiClass)element.getParent());
                                      if (info2 != null) {
                                        PsiNavigateUtil.navigate(info2.getNavigatableElement());
                                      }
                                    },
                                    GutterIconRenderer.Alignment.RIGHT);
      }
    }
    return null;
  }

  @Nonnull
  @Override
  public Language getLanguage() {
    return JavaLanguage.INSTANCE;
  }
}
