package consulo.devkit.localize.inspection;

import com.intellij.java.language.psi.*;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
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
  private static final String localizeTODO = "localizeTODO";

  @Nonnull
  @Override
  public String getDisplayName() {
    return "Localize TODO inspection";
  }

  @Nonnull
  @Override
  public PsiElementVisitor buildInternalVisitor(@Nonnull ProblemsHolder holder, boolean isOnTheFly) {
    return new JavaElementVisitor() {
      @Override
      @RequiredReadAction
      public void visitMethodCallExpression(PsiMethodCallExpression expression) {
        PsiReferenceExpression methodExpression = expression.getMethodExpression();

        String referenceName = methodExpression.getReferenceName();
        if (localizeTODO.equals(referenceName)) {
          PsiElement localizeTODOMethod = methodExpression.resolve();
          if (localizeTODOMethod == null) {
            return;
          }

          PsiElement parent = localizeTODOMethod.getParent();
          if (parent instanceof PsiClass psiClass && LocalizeValue.class.getName().equals(psiClass.getQualifiedName())) {
            PsiExpression[] expressions = expression.getArgumentList().getExpressions();
            if (expressions.length != 1) {
              return;
            }

            PsiExpression stringLiteral = expressions[0];

            // unquote string. if expression is reference - it's invalid, and must be fixed anyway
            String text = StringUtil.unquoteString(stringLiteral.getText());
            holder.registerProblem(stringLiteral, new TextRange(0, stringLiteral.getTextLength()), "'" + text + "' not localized");
          }
        }
      }
    };
  }
}
