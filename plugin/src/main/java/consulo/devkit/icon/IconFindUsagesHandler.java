package consulo.devkit.icon;

import com.intellij.java.language.psi.PsiMethod;
import consulo.annotation.access.RequiredReadAction;
import consulo.find.FindUsagesHandler;
import consulo.language.psi.PsiElement;
import consulo.module.content.DirectoryIndex;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2024-09-08
 */
public class IconFindUsagesHandler extends FindUsagesHandler {
    private final DirectoryIndex myDirectoryIndex;

    public IconFindUsagesHandler(DirectoryIndex directoryIndex, @Nonnull PsiElement psiElement) {
        super(psiElement);
        myDirectoryIndex = directoryIndex;
    }

    @Nonnull
    @Override
    @RequiredReadAction
    public PsiElement[] getPrimaryElements() {
        Object iconReference = IconFindUsagesHanderFactory.findIconReference(myDirectoryIndex, getPsiElement(), false);
        if (iconReference instanceof PsiMethod method) {
            return new PsiElement[]{getPsiElement(), method};
        }
        return super.getPrimaryElements();
    }
}
