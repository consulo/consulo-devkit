package consulo.devkit.localize;

import com.intellij.java.language.psi.PsiMethod;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.markup.GutterIconRenderer;
import consulo.devkit.util.PluginModuleUtil;
import consulo.language.Language;
import consulo.language.editor.Pass;
import consulo.language.editor.gutter.LineMarkerInfo;
import consulo.language.editor.gutter.LineMarkerProvider;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiUtilCore;
import consulo.platform.base.icon.PlatformIconGroup;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.yaml.YAMLLanguage;
import org.jetbrains.yaml.YAMLTokenTypes;
import org.jetbrains.yaml.psi.YAMLKeyValue;

/**
 * @author VISTALL
 * @since 2024-09-08
 */
@ExtensionImpl
public class LocalizeYAMLLineMarkerProvider implements LineMarkerProvider {
    @RequiredReadAction
    @Nullable
    @Override
    public LineMarkerInfo getLineMarkerInfo(@Nonnull PsiElement element) {
        if (PsiUtilCore.getElementType(element) == YAMLTokenTypes.SCALAR_KEY && element.getParent() instanceof YAMLKeyValue) {
            YAMLKeyValue yamlKeyValue = (YAMLKeyValue) element.getParent();

            PsiMethod method = LocalizeUtil.findMethodByYAMLKey(yamlKeyValue);
            if (method != null) {
                return new LineMarkerInfo<>(element,
                    element.getTextRange(),
                    PlatformIconGroup.gutterImplementedmethod(),
                    Pass.LINE_MARKERS,
                    null,
                    (mouseEvent, psiElement) -> {
                        PsiElement nextParent = psiElement.getParent();
                        if (nextParent instanceof YAMLKeyValue nextKey) {
                            PsiMethod nextMethod = LocalizeUtil.findMethodByYAMLKey(nextKey);
                            if (nextMethod != null) {
                                nextMethod.navigate(true);
                            }
                        }
                    },
                    GutterIconRenderer.Alignment.RIGHT);
            }
        }
        return null;
    }

    @Override
    @RequiredReadAction
    public boolean isAvailable(@Nonnull PsiFile file) {
        return PluginModuleUtil.isConsuloOrPluginProject(file);
    }

    @Nonnull
    @Override
    public Language getLanguage() {
        return YAMLLanguage.INSTANCE;
    }
}
