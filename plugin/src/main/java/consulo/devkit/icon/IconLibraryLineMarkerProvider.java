package consulo.devkit.icon;

import com.intellij.java.language.JavaLanguage;
import com.intellij.java.language.psi.PsiIdentifier;
import com.intellij.java.language.psi.PsiMethod;
import com.intellij.java.language.psi.PsiMethodCallExpression;
import com.intellij.java.language.psi.PsiReferenceExpression;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.markup.GutterIconRenderer;
import consulo.language.Language;
import consulo.language.editor.Pass;
import consulo.language.editor.gutter.LineMarkerInfo;
import consulo.language.editor.gutter.LineMarkerProvider;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.navigation.OpenFileDescriptorFactory;
import consulo.project.Project;
import consulo.ui.image.Image;
import consulo.util.lang.Pair;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 23/04/2023
 */
@ExtensionImpl
public class IconLibraryLineMarkerProvider implements LineMarkerProvider {
    public static final String ICON_GROUP_SUFFIX = "IconGroup";

    @Override
    @RequiredReadAction
    public boolean isAvailable(@Nonnull PsiFile file) {
        return IconLibraryChecker.containsImageApi(file);
    }

    @RequiredReadAction
    @Nullable
    @Override
    public LineMarkerInfo getLineMarkerInfo(@Nonnull PsiElement element) {
        // method call like MyIconGroup.testMe()
        if (element instanceof PsiIdentifier && element.getParent() instanceof PsiReferenceExpression
            && element.getParent().getParent() instanceof PsiMethodCallExpression) {
            PsiMethodCallExpression methodCall = (PsiMethodCallExpression)element.getParent().getParent();

            PsiMethod psiMethod = methodCall.resolveMethod();
            if (psiMethod != null) {
                return create(element, psiMethod);
            }
        }
        // method declaration inside MyIconGroup
        else if (element instanceof PsiIdentifier && element.getParent() instanceof PsiMethod method) {
            return create(element, method);
        }
        return null;
    }

    @Nullable
    @RequiredReadAction
    private LineMarkerInfo<PsiElement> create(@Nonnull PsiElement targetElement, @Nonnull PsiMethod psiMethod) {
        Project project = targetElement.getProject();
        Pair<Image, VirtualFile> pair = IconLibraryGroupImageCache.getInstance(project).getImage(psiMethod);
        if (pair != null) {
            return new LineMarkerInfo<>(
                targetElement,
                targetElement.getTextRange(),
                pair.getFirst(),
                Pass.LINE_MARKERS,
                null,
                (mouseEvent, element) -> OpenFileDescriptorFactory.getInstance(project).newBuilder(pair.getSecond()).build().navigate(true),
                GutterIconRenderer.Alignment.RIGHT
            );
        }

        return null;
    }

    @Nonnull
    @Override
    public Language getLanguage() {
        return JavaLanguage.INSTANCE;
    }
}
