package consulo.devkit.inspections.internal;

import javax.annotation.Nonnull;

import org.jetbrains.idea.devkit.inspections.internal.InternalInspection;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.openapi.wm.ex.IdeFrameEx;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiInstanceOfExpression;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypeCastExpression;
import com.intellij.psi.PsiTypeElement;
import com.intellij.psi.util.TypeConversionUtil;

/**
 * @author VISTALL
 * @since 2019-02-17
 */
public class WrongCastRequireExplicitConversionInspection extends InternalInspection
{
	private enum PossibleWrondCast
	{
		AWT_COMPONENT_TO_UI_COMPONENT("java.awt.Component", "consulo.ui.Component"),
		UI_COMPONENT_TO_AWT_COMPONENT("consulo.ui.Component", "java.awt.Component"),

		VAADIN_COMPONENT_TO_UI_COMPONENT("com.vaadin.ui.Component", "consulo.ui.Component"),
		UI_COMPONENT_TO_VAADIN_COMPONENT("consulo.ui.Component", "com.vaadin.ui.Component"),

		IDE_FRAME_TO_AWT_WINDOW(IdeFrame.class.getName(), "java.awt.Component"),
		AWT_WINDOW_TO_IDE_FRAME("java.awt.Component", IdeFrame.class.getName()),

		IDE_FRAME_EX_TO_AWT_WINDOW(IdeFrameEx.class.getName(), "java.awt.Component"),
		AWT_WINDOW_TO_IDE_FRAME_EX("java.awt.Component", IdeFrameEx.class.getName());

		private final String myType1;
		private final String myType2;

		PossibleWrondCast(String type1, String type2)
		{
			myType1 = type1;
			myType2 = type2;
		}
	}

	@Override
	public PsiElementVisitor buildInternalVisitor(@Nonnull ProblemsHolder holder, boolean isOnTheFly)
	{
		return new JavaElementVisitor()
		{
			@Override
			public void visitTypeCastExpression(PsiTypeCastExpression expression)
			{
				super.visitTypeCastExpression(expression);

				PsiTypeElement castTypeElement = expression.getCastType();

				PsiExpression operand = expression.getOperand();
				if(operand == null || castTypeElement == null)
				{
					return;
				}

				PsiType castType = castTypeElement.getType();
				PsiType expressionType = operand.getType();
				checkType(expression, castTypeElement, castType, expressionType);
			}

			@Override
			public void visitInstanceOfExpression(PsiInstanceOfExpression expression)
			{
				super.visitInstanceOfExpression(expression);

				PsiTypeElement checkTypeElement = expression.getCheckType();
				PsiExpression operand = expression.getOperand();
				if(checkTypeElement == null)
				{
					return;
				}

				PsiType castType = checkTypeElement.getType();
				PsiType expressionType = operand.getType();
				checkType(expression, checkTypeElement, castType, expressionType);
			}

			private void checkType(PsiExpression expression, PsiTypeElement castTypeElement, PsiType castType, PsiType expressionType)
			{
				if(expressionType == null || PsiType.NULL.equals(castType) || PsiType.NULL.equals(expressionType))
				{
					return;
				}

				for(PossibleWrondCast possibleWrondCast : PossibleWrondCast.values())
				{
					PsiClassType type1Ref = JavaPsiFacade.getElementFactory(expression.getProject()).createTypeByFQClassName(possibleWrondCast.myType1);
					PsiClassType type2Ref = JavaPsiFacade.getElementFactory(expression.getProject()).createTypeByFQClassName(possibleWrondCast.myType2);

					if(!TypeConversionUtil.isAssignable(type1Ref, castType))
					{
						continue;
					}

					if(!TypeConversionUtil.isAssignable(type2Ref, expressionType))
					{
						continue;
					}

					holder.registerProblem(castTypeElement, "Wrong cast - require explicit conversion", ProblemHighlightType.GENERIC_ERROR);
				}
			}
		};
	}
}
