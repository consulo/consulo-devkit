package consulo.devkit.inspections;

import com.intellij.java.language.codeInsight.AnnotationUtil;
import com.intellij.java.language.jvm.JvmParameter;
import com.intellij.java.language.psi.*;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.WriteAction;
import consulo.language.editor.inspection.LocalQuickFixOnPsiElement;
import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import org.jetbrains.annotations.Nls;
import org.jetbrains.idea.devkit.inspections.internal.InternalInspection;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2020-10-22
 */
@ExtensionImpl
public class MigratedExtensionsToInspection extends InternalInspection {
  private static final String annotationFq = "consulo.annotation.internal.MigratedExtensionsTo";

  private static class DelegateMethodsFix extends LocalQuickFixOnPsiElement {

    protected DelegateMethodsFix(@Nonnull PsiMethod element) {
      super(element);
    }

    @Nonnull
    @Override
    public String getText() {
      return "Delegate to new class";
    }

    @Override
    public void invoke(@Nonnull Project project,
                       @Nonnull PsiFile psiFile,
                       @Nonnull PsiElement psiElement,
                       @Nonnull PsiElement psiElement1) {
      PsiMethod method = (PsiMethod)psiElement;

      PsiClass containingClass = method.getContainingClass();
      if (containingClass == null || !method.hasModifierProperty(PsiModifier.STATIC) || !method.hasModifierProperty(PsiModifier.PUBLIC)) {
        return;
      }

      PsiAnnotation annotation = AnnotationUtil.findAnnotation(containingClass, annotationFq);
      if (annotation == null) {
        return;
      }

      PsiNameValuePair[] attributes = annotation.getParameterList().getAttributes();
      if (attributes.length != 1) {
        return;
      }

      PsiAnnotationMemberValue value = attributes[0].getValue();

      PsiTypeElement operand = null;
      if (value instanceof PsiClassObjectAccessExpression) {
        operand = ((PsiClassObjectAccessExpression)value).getOperand();
      }

      if (operand == null) {
        return;
      }

      JvmParameter[] parameters = method.getParameters();

      StringBuilder codeBlock = new StringBuilder();
      codeBlock.append("{");

      if (!PsiType.VOID.equals(method.getReturnType())) {
        codeBlock.append("return ");
      }

      codeBlock.append(operand.getType().getCanonicalText());
      codeBlock.append(".");
      codeBlock.append(method.getName());
      codeBlock.append("(");
      for (int i = 0; i < parameters.length; i++) {
        if (i != 0) {
          codeBlock.append(", ");
        }

        codeBlock.append(parameters[i].getName());
      }
      codeBlock.append(");\n");

      codeBlock.append("}");

      PsiCodeBlock newBlock = PsiElementFactory.getInstance(method.getProject()).createCodeBlockFromText(codeBlock.toString(), method);

      WriteAction.run(() ->
                      {
                        PsiCodeBlock body = method.getBody();

                        body.replace(newBlock);
                      });
    }

    @Nls
    @Nonnull
    @Override
    public String getFamilyName() {
      return "DevKit";
    }
  }

  @Nonnull
  @Override
  public String getDisplayName() {
    return "@MigratedExtensionsTo helper";
  }

  @Override
  public PsiElementVisitor buildInternalVisitor(@Nonnull ProblemsHolder holder, boolean isOnTheFly) {
    return new JavaElementVisitor() {
      @Override
      @RequiredReadAction
      public void visitMethod(PsiMethod method) {
        PsiClass containingClass = method.getContainingClass();

        if (containingClass != null && AnnotationUtil.isAnnotated(containingClass, annotationFq, 0)) {
          PsiIdentifier nameIdentifier = method.getNameIdentifier();
          if (nameIdentifier == null) {
            return;
          }

          holder.registerProblem(nameIdentifier,
                                 "Delegate method calls",
                                 ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                                 new DelegateMethodsFix(method));
        }
      }
    };
  }
}
