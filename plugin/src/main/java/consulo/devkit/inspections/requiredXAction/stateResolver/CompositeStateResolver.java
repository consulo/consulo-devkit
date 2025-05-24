package consulo.devkit.inspections.requiredXAction.stateResolver;

import com.intellij.java.language.psi.PsiExpression;
import consulo.annotation.access.RequiredReadAction;
import consulo.devkit.inspections.requiredXAction.CallStateType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author UNV
 * @since 2025-05-24
 */
public class CompositeStateResolver extends StateResolver {
    private final StateResolver[] myStateResolvers;

    public CompositeStateResolver(@Nonnull StateResolver... resolvers) {
        myStateResolvers = resolvers.clone();
    }

    @Nullable
    @Override
    @RequiredReadAction
    public Boolean resolveState(CallStateType actionType, PsiExpression expression) {
        for (StateResolver stateResolver : myStateResolvers) {
            Boolean state = stateResolver.resolveState(actionType, expression);
            if (state != null) {
                return state;
            }
        }
        return null;
    }
}
