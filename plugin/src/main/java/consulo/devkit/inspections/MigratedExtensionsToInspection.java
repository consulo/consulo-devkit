package consulo.devkit.inspections;

import com.intellij.java.language.codeInsight.AnnotationUtil;
import com.intellij.java.language.jvm.JvmParameter;
import com.intellij.java.language.psi.*;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.WriteAction;
import consulo.devkit.localize.DevKitLocalize;
import consulo.language.editor.inspection.LocalQuickFixOnPsiElement;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import org.jetbrains.idea.devkit.inspections.internal.InternalInspection;

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
            return DevKitLocalize.migratedExtensionsToInspectionQuickfixName().get();
        }

        @Override
        public void invoke(
            @Nonnull Project project,
            @Nonnull PsiFile psiFile,
            @Nonnull PsiElement psiElement,
            @Nonnull PsiElement psiElement1
        ) {
            PsiMethod method = (PsiMethod)psiElement;

            PsiClass containingClass = method.getContainingClass();
            if (containingClass == null || !method.isStatic() || !method.isPublic()) {
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
            if (value instanceof PsiClassObjectAccessExpression classObjectAccessExpression) {
                operand = classObjectAccessExpression.getOperand();
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

            codeBlock.append(operand.getType().getCanonicalText())
                .append(".")
                .append(method.getName())
                .append("(");
            for (int i = 0; i < parameters.length; i++) {
                if (i != 0) {
                    codeBlock.append(", ");
                }

                codeBlock.append(parameters[i].getName());
            }
            codeBlock.append(");\n");

            codeBlock.append("}");

            PsiCodeBlock newBlock =
                PsiElementFactory.getInstance(method.getProject()).createCodeBlockFromText(codeBlock.toString(), method);

            WriteAction.run(() -> {
                PsiCodeBlock body = method.getBody();

                assert body != null;
                body.replace(newBlock);
            });
        }

        @Nonnull
        @Override
        public String getFamilyName() {
            return DevKitLocalize.inspectionsGroupName().get();
        }
    }

    @Nonnull
    @Override
    public String getDisplayName() {
        return DevKitLocalize.migratedExtensionsToInspectionDisplayName().get();
    }

    @Override
    public PsiElementVisitor buildInternalVisitor(@Nonnull ProblemsHolder holder, boolean isOnTheFly) {
        return new JavaElementVisitor() {
            @Override
            @RequiredReadAction
            public void visitMethod(@Nonnull PsiMethod method) {
                PsiClass containingClass = method.getContainingClass();

                if (containingClass != null && AnnotationUtil.isAnnotated(containingClass, annotationFq, 0)) {
                    PsiIdentifier nameIdentifier = method.getNameIdentifier();
                    if (nameIdentifier == null) {
                        return;
                    }

                    holder.newProblem(DevKitLocalize.migratedExtensionsToInspectionMessage())
                        .range(nameIdentifier)
                        .withFix(new DelegateMethodsFix(method))
                        .create();
                }
            }
        };
    }
}
