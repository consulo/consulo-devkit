package consulo.devkit.localize;

import com.intellij.java.language.psi.PsiMethod;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.devkit.localize.folding.LocalizeFoldingBuilder;
import consulo.devkit.localize.folding.LocalizeResolveInfo;
import consulo.language.editor.TargetElementUtilExtender;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nullable;
import org.jetbrains.yaml.psi.*;

import java.util.List;
import java.util.Objects;

/**
 * @author VISTALL
 * @since 2024-09-08
 */
@ExtensionImpl
public class LocalizeTargetElementUtilExtender implements TargetElementUtilExtender {
    @Nullable
    @Override
    @RequiredReadAction
    public PsiElement getGotoDeclarationTarget(PsiElement element, PsiElement navElement) {
        if (navElement instanceof PsiMethod method) {
            LocalizeResolveInfo resolveInfo = LocalizeFoldingBuilder.findLocalizeInfo(navElement, method.getName(), method.getContainingClass());
            if (resolveInfo != null) {
                YAMLFile file = resolveInfo.file();

                List<YAMLDocument> documents = file.getDocuments();
                for (YAMLDocument document : documents) {
                    YAMLValue topLevelValue = document.getTopLevelValue();
                    if (topLevelValue instanceof YAMLMapping topLevelMapping) {
                        for (YAMLKeyValue value : topLevelMapping.getKeyValues()) {
                            String key = value.getKeyText();

                            if (Objects.equals(key, resolveInfo.key())) {
                                return value;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }
}
