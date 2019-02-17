package consulo.devkit.inspections.internal;

import javax.annotation.Nonnull;

import org.jetbrains.idea.devkit.inspections.internal.InternalInspection;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiExpression;
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

		IDE_FRAME_TO_AWT_WINDOW(IdeFrame.class.getName(), "java.awt.Window"),
		AWT_WINDOW_TO_IDE_FRAME("java.awt.Window", IdeFrame.class.getName());

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
				if(expressionType == null)
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
