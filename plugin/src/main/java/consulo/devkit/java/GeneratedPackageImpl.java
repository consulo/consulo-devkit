package consulo.devkit.java;

import com.intellij.java.language.impl.psi.impl.file.PsiPackageImpl;
import com.intellij.java.language.psi.JavaPsiFacade;
import consulo.java.language.module.extension.JavaModuleExtension;
import consulo.language.psi.PsiManager;
import consulo.language.psi.PsiPackageManager;

/**
 * @author VISTALL
 * @since 2025-10-02
 */
public class GeneratedPackageImpl extends PsiPackageImpl {
    public GeneratedPackageImpl(PsiManager manager,
                                PsiPackageManager packageManager,
                                JavaPsiFacade javaPsiFacade,
                                String qualifiedName) {
        super(manager, packageManager, javaPsiFacade, JavaModuleExtension.class, qualifiedName);
    }

    @Override
    public boolean isValid() {
        return true;
    }
}
