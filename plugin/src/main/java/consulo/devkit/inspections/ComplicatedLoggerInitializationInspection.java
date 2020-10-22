package consulo.devkit.inspections;

import com.intellij.codeInspection.LocalQuickFixOnPsiElement;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.containers.ContainerUtil;
import consulo.annotation.access.RequiredReadAction;
import consulo.logging.Logger;
import org.jetbrains.annotations.Nls;
import org.jetbrains.idea.devkit.inspections.internal.InternalInspection;

import javax.annotation.Nonnull;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2020-06-08
 */
public class ComplicatedLoggerInitializationInspection extends InternalInspection
{
	private static class Fix extends LocalQuickFixOnPsiElement
	{
		protected Fix(@Nonnull PsiElement element)
		{
			super(element);
		}

		@Nonnull
		@Override
		public String getText()
		{
			return "Replace by class constant";
		}

		@Override
		public void invoke(@Nonnull Project project, @Nonnull PsiFile psiFile, @Nonnull PsiElement psiElement, @Nonnull PsiElement psiElement1)
		{
			PsiClass psiClass = PsiTreeUtil.getParentOfType(psiElement, PsiClass.class);
			if(psiClass == null)
			{
				return;
			}

			PsiExpression expression = JavaPsiFacade.getElementFactory(project).createExpressionFromText(psiClass.getQualifiedName() + ".class", psiElement);

			WriteAction.run(() -> psiElement.replace(expression));
		}

		@Nls
		@Nonnull
		@Override
		public String getFamilyName()
		{
			return "Consulo Devkit";
		}
	}

	private static final Set<String> ourLoggerClasses = ContainerUtil.newHashSet(Logger.class.getName(), com.intellij.openapi.diagnostic.Logger.class.getName());

	@Override
	public PsiElementVisitor buildInternalVisitor(@Nonnull ProblemsHolder holder, boolean isOnTheFly)
	{
		return new JavaElementVisitor()
		{
			@Override
			@RequiredReadAction
			public void visitMethodCallExpression(PsiMethodCallExpression expression)
			{
				PsiReferenceExpression methodExpression = expression.getMethodExpression();

				String referenceName = methodExpression.getReferenceName();

				PsiExpressionList argumentList = expression.getArgumentList();

				if("getInstance".equals(referenceName) && argumentList.getExpressionCount() == 1)
				{
					PsiElement element = methodExpression.resolve();

					if(element instanceof PsiMethod)
					{
						PsiClass containingClass = ((PsiMethod) element).getContainingClass();
						if(containingClass != null && ourLoggerClasses.contains(containingClass.getQualifiedName()))
						{
							PsiExpression argument = argumentList.getExpressions()[0];
							if(isComplicatedArgument(argument))
							{
								holder.registerProblem(argument, "Compilated logger initialization", ProblemHighlightType.GENERIC_ERROR_OR_WARNING, new TextRange(0, argument.getTextLength()), new
										Fix(argument));
							}
						}
					}
				}
			}
		};
	}

	private boolean isComplicatedArgument(PsiExpression expression)
	{
		PsiClass psiClass = PsiTreeUtil.getParentOfType(expression, PsiClass.class);
		if(psiClass == null)
		{
			return false;
		}

		String qualifiedName = psiClass.getQualifiedName();
		if(qualifiedName == null)
		{
			return false;
		}

		if(expression instanceof PsiLiteralExpression)
		{
			Object value = ((PsiLiteralExpression) expression).getValue();
			if(value instanceof String)
			{
				return qualifiedName.equals(value) || ("#" + qualifiedName).equals(value);
			}
		}
		else if(expression instanceof PsiBinaryExpression)
		{
			PsiExpression lOperand = ((PsiBinaryExpression) expression).getLOperand();
			PsiExpression rOperand = ((PsiBinaryExpression) expression).getROperand();
			if(lOperand instanceof PsiLiteralExpression)
			{
				if("#".equals(((PsiLiteralExpression) lOperand).getValue()))
				{
					if(rOperand instanceof PsiClassObjectAccessExpression)
					{
						PsiTypeElement operand = ((PsiClassObjectAccessExpression) rOperand).getOperand();
						PsiType type = operand.getType();
						if(type instanceof PsiClassType)
						{
							PsiClass className = ((PsiClassType) type).resolve();

							if(className != null && qualifiedName.equals(className.getQualifiedName()))
							{
								return true;
							}
						}
					}
					else if(rOperand instanceof PsiMethodCallExpression)
					{
						PsiReferenceExpression methodExpression = ((PsiMethodCallExpression) rOperand).getMethodExpression();

						if("getName".equals(methodExpression.getReferenceName()))
						{
							PsiExpression qualifierExpression = methodExpression.getQualifierExpression();
							if(qualifierExpression instanceof PsiClassObjectAccessExpression)
							{
								PsiType type = ((PsiClassObjectAccessExpression) qualifierExpression).getOperand().getType();
								if(type instanceof PsiClassType)
								{
									PsiClass className = ((PsiClassType) type).resolve();

									if(className != null && qualifiedName.equals(className.getQualifiedName()))
									{
										return true;
									}
								}
							}
						}
					}
				}
			}
		}
		else if(expression instanceof PsiMethodCallExpression)
		{
			PsiReferenceExpression methodExpression = ((PsiMethodCallExpression) expression).getMethodExpression();

			if("getName".equals(methodExpression.getReferenceName()))
			{
				PsiExpression qualifierExpression = methodExpression.getQualifierExpression();
				if(qualifierExpression instanceof PsiClassObjectAccessExpression)
				{
					PsiType type = ((PsiClassObjectAccessExpression) qualifierExpression).getOperand().getType();
					if(type instanceof PsiClassType)
					{
						PsiClass className = ((PsiClassType) type).resolve();

						if(className != null && qualifiedName.equals(className.getQualifiedName()))
						{
							return true;
						}
					}
				}
			}
		}

		return false;
	}
}
