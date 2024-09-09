package consulo.devkit.localize;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.access.RequiredWriteAction;
import consulo.language.impl.psi.LightElement;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiNameIdentifierOwner;
import consulo.language.psi.PsiNamedElement;
import consulo.language.util.IncorrectOperationException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.yaml.psi.YAMLKeyValue;

/**
 * @author VISTALL
 * @since 2024-09-09
 */
public class LocalizeKeyElement extends LightElement implements PsiNameIdentifierOwner, PsiNamedElement {
    @Nonnull
    private final YAMLKeyValue myKeyValue;

    @RequiredReadAction
    protected LocalizeKeyElement(YAMLKeyValue keyValue) {
        super(keyValue.getManager(), keyValue.getLanguage());
        myKeyValue = keyValue;
    }

    @Override
    public PsiElement getOriginalElement() {
        return myKeyValue;
    }

    @RequiredReadAction
    @Override
    public String getName() {
        return myKeyValue.getKeyText();
    }

    @Override
    public String toString() {
        return myKeyValue.toString();
    }

    @RequiredReadAction
    @Nullable
    @Override
    public PsiElement getNameIdentifier() {
        return myKeyValue.getKey();
    }

    @RequiredWriteAction
    @Override
    public PsiElement setName(@Nonnull String name) throws IncorrectOperationException {
        return null;
    }
}
