package consulo.devkit.localize.inspection;

import com.intellij.java.language.psi.*;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.devkit.localize.DevKitLocalize;
import consulo.document.util.TextRange;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.localize.LocalizeValue;
import consulo.util.lang.StringUtil;
import org.jetbrains.idea.devkit.inspections.internal.InternalInspection;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 08/10/2021
 */
@ExtensionImpl
public class LocalizeTODOInspection extends InternalInspection {
    private static final String LOCALIZE_TODO = "localizeTODO";

    @Nonnull
    @Override
    public String getDisplayName() {
        return DevKitLocalize.localizeTodoInspectionDisplayName().get();
    }

    @Nonnull
    @Override
    public PsiElementVisitor buildInternalVisitor(@Nonnull ProblemsHolder holder, boolean isOnTheFly) {
        return new JavaElementVisitor() {
            @Override
            @RequiredReadAction
            public void visitMethodCallExpression(@Nonnull PsiMethodCallExpression expression) {
                PsiReferenceExpression methodExpression = expression.getMethodExpression();

                String methodName = methodExpression.getReferenceName();
                if (!LOCALIZE_TODO.equals(methodName)) {
                    return;
                }

                PsiElement localizeTODOMethod = methodExpression.resolve();
                if (localizeTODOMethod == null) {
                    return;
                }

                PsiElement parent = localizeTODOMethod.getParent();
                if (!(parent instanceof PsiClass psiClass)
                    || !LocalizeValue.class.getName().equals(psiClass.getQualifiedName())) {
                    return;
                }

                PsiExpression[] expressions = expression.getArgumentList().getExpressions();
                if (expressions.length != 1) {
                    return;
                }

                PsiExpression stringLiteral = expressions[0];

                // unquote string. if expression is reference - it's invalid, and must be fixed anyway
                String text = StringUtil.unquoteString(stringLiteral.getText());
                holder.registerProblem(
                    stringLiteral,
                    new TextRange(0, stringLiteral.getTextLength()),
                    LocalizeValue.localizeTODO("'" + text + "' not localized").get()
                );
            }
        };
    }
}
