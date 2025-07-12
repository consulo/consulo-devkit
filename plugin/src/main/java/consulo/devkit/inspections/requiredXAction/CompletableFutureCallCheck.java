package consulo.devkit.inspections.requiredXAction;

import com.intellij.java.language.psi.*;
import consulo.annotation.access.RequiredReadAction;
import jakarta.annotation.Nonnull;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 2025-07-12
 */
public class CompletableFutureCallCheck extends AcceptableMethodCallCheck {
    @Nonnull
    private final String myProviderAccessArgumentClassName;

    public CompletableFutureCallCheck(@Nonnull String providerAccessArgumentClassName) {
        super(CompletableFuture.class, Set.of("whenCompleteAsync"));
        myProviderAccessArgumentClassName = providerAccessArgumentClassName;
    }

    @RequiredReadAction
    @Override
    public boolean accept(PsiMethod method, PsiExpressionList expressionList) {
        if (!super.accept(method, expressionList)) {
            return false;
        }

        int expressionCount = expressionList.getExpressionCount();
        if (expressionCount != 2) {
            return false;
        }

        PsiExpression expression = expressionList.getExpressions()[1];

        PsiType type = expression.getType();
        if (type instanceof PsiClassType classType
            && classType.resolve() instanceof PsiClass psiClass
            && myProviderAccessArgumentClassName.equals(psiClass.getQualifiedName())) {
            return true;
        }

        // some additional logic?
        
        return false;
    }
}
