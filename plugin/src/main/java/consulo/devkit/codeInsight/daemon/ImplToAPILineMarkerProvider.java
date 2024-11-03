package consulo.devkit.codeInsight.daemon;

import com.intellij.java.language.JavaLanguage;
import com.intellij.java.language.codeInsight.AnnotationUtil;
import com.intellij.java.language.psi.JavaTokenType;
import com.intellij.java.language.psi.PsiAnnotation;
import com.intellij.java.language.psi.PsiClass;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.AllIcons;
import consulo.codeEditor.markup.GutterIconRenderer;
import consulo.devkit.inspections.valhalla.ValhallaClasses;
import consulo.devkit.util.PluginModuleUtil;
import consulo.language.Language;
import consulo.language.editor.Pass;
import consulo.language.editor.gutter.LineMarkerInfo;
import consulo.language.editor.gutter.LineMarkerProvider;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiUtilCore;
import consulo.language.psi.util.PsiNavigateUtil;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2022-08-08
 */
@ExtensionImpl
public class ImplToAPILineMarkerProvider implements LineMarkerProvider {
    @Override
    @RequiredReadAction
    public boolean isAvailable(@Nonnull PsiFile file) {
        return PluginModuleUtil.isConsuloOrPluginProject(file);
    }

    @RequiredReadAction
    @Nullable
    @Override
    public LineMarkerInfo getLineMarkerInfo(@Nonnull PsiElement psiElement) {
        if (PsiUtilCore.getElementType(psiElement) == JavaTokenType.IDENTIFIER && psiElement.getParent() instanceof PsiClass psiClass) {
            Pair<PsiElement, String> apiInfo = findAPIElement(psiClass);
            if (apiInfo != null) {
                String navigationText = "Navigate to @" + StringUtil.getShortName(apiInfo.getSecond());
                return new LineMarkerInfo<>(
                    psiElement,
                    psiElement.getTextRange(),
                    AllIcons.Nodes.Plugin,
                    Pass.LINE_MARKERS,
                    element -> navigationText,
                    (mouseEvent, element) -> {
                        Pair<PsiElement, String> target = findAPIElement(psiClass);
                        if (target != null) {
                            PsiNavigateUtil.navigate(target.getFirst());
                        }
                    },
                    GutterIconRenderer.Alignment.RIGHT
                );
            }
        }
        return null;
    }

    @Nullable
    private Pair<PsiElement, String> findAPIElement(PsiClass psiClass) {
        for (Pair<String, String> apiPair : ValhallaClasses.ApiToImpl) {
            if (AnnotationUtil.isAnnotated(psiClass, apiPair.getSecond(), 0)) {
                PsiAnnotation annotationInHierarchy = AnnotationUtil.findAnnotationInHierarchy(psiClass, Set.of(apiPair.getFirst()));
                if (annotationInHierarchy != null) {
                    //					PsiClass apiType = PsiTreeUtil.getParentOfType(annotationInHierarchy, PsiClass.class);
                    //					if(apiType != null)
                    //					{
                    //						return apiType;
                    //					}
                    return Pair.create(annotationInHierarchy, apiPair.getFirst());
                }
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
