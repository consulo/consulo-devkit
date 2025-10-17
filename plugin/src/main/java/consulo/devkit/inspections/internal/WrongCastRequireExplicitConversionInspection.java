package consulo.devkit.inspections.internal;

import com.intellij.java.language.psi.*;
import com.intellij.java.language.psi.util.TypeConversionUtil;
import consulo.annotation.component.ExtensionImpl;
import consulo.devkit.localize.DevKitLocalize;
import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.psi.PsiElementVisitor;
import consulo.localize.LocalizeValue;
import consulo.project.ui.wm.IdeFrame;
import jakarta.annotation.Nonnull;
import org.jetbrains.idea.devkit.inspections.internal.InternalInspection;

import java.util.List;

/**
 * @author VISTALL
 * @since 2019-02-17
 */
@ExtensionImpl
public class WrongCastRequireExplicitConversionInspection extends InternalInspection {
    private static class PossibleWrongCast {
        @Nonnull
        private final String myType1, myType2;

        private PossibleWrongCast(@Nonnull String type1, @Nonnull String type2) {
            myType1 = type1;
            myType2 = type2;
        }
    }

    private static final List<PossibleWrongCast> POSSIBLE_WRONG_CASTS = List.of(
        new PossibleWrongCast("java.awt.Component", "consulo.ui.Component"),
        new PossibleWrongCast("consulo.ui.Component", "java.awt.Component"),

        new PossibleWrongCast("com.vaadin.ui.Component", "consulo.ui.Component"),
        new PossibleWrongCast("consulo.ui.Component", "com.vaadin.ui.Component"),

        new PossibleWrongCast(IdeFrame.class.getName(), "java.awt.Component"),
        new PossibleWrongCast("java.awt.Component", IdeFrame.class.getName()),

        new PossibleWrongCast("consulo.project.ui.wm.IdeFrame", "java.awt.Component"),
        new PossibleWrongCast("java.awt.Component", "consulo.project.ui.wm.IdeFrame")
    );

    @Nonnull
    @Override
    public LocalizeValue getDisplayName() {
        return DevKitLocalize.inspectionWrongCastRequireExplicitConversionDisplayName();
    }

    @Nonnull
    @Override
    public HighlightDisplayLevel getDefaultLevel() {
        return HighlightDisplayLevel.ERROR;
    }

    @Override
    public PsiElementVisitor buildInternalVisitor(@Nonnull ProblemsHolder holder, boolean isOnTheFly) {
        return new JavaElementVisitor() {
            @Override
            public void visitTypeCastExpression(@Nonnull PsiTypeCastExpression expression) {
                super.visitTypeCastExpression(expression);

                PsiTypeElement castTypeElement = expression.getCastType();

                PsiExpression operand = expression.getOperand();
                if (operand == null || castTypeElement == null) {
                    return;
                }

                PsiType castType = castTypeElement.getType();
                PsiType expressionType = operand.getType();
                checkType(expression, castTypeElement, castType, expressionType);
            }

            @Override
            public void visitInstanceOfExpression(@Nonnull PsiInstanceOfExpression expression) {
                super.visitInstanceOfExpression(expression);

                PsiTypeElement checkTypeElement = expression.getCheckType();
                PsiExpression operand = expression.getOperand();
                if (checkTypeElement == null) {
                    return;
                }

                PsiType castType = checkTypeElement.getType();
                PsiType expressionType = operand.getType();
                checkType(expression, checkTypeElement, castType, expressionType);
            }

            private void checkType(PsiExpression expression, PsiTypeElement castTypeElement, PsiType castType, PsiType expressionType) {
                if (expressionType == null || PsiType.NULL.equals(castType) || PsiType.NULL.equals(expressionType)) {
                    return;
                }

                PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(expression.getProject());

                for (PossibleWrongCast possibleWrongCast : POSSIBLE_WRONG_CASTS) {
                    PsiClassType type1Ref = elementFactory.createTypeByFQClassName(possibleWrongCast.myType1);
                    PsiClassType type2Ref = elementFactory.createTypeByFQClassName(possibleWrongCast.myType2);

                    if (TypeConversionUtil.isAssignable(type1Ref, castType)
                        && TypeConversionUtil.isAssignable(type2Ref, expressionType)) {

                        holder.newProblem(DevKitLocalize.inspectionWrongCastRequireExplicitConversionMessage())
                            .range(castTypeElement)
                            .highlightType(ProblemHighlightType.GENERIC_ERROR)
                            .create();
                    }
                }
            }
        };
    }
}
