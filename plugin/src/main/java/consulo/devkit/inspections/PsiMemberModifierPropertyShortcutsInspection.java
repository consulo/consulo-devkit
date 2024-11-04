package consulo.devkit.inspections;

import com.intellij.java.language.psi.*;
import com.intellij.java.language.psi.util.InheritanceUtil;
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

import java.util.Map;

/**
 * @author UNV
 * @since 2024-11-03
 */
@ExtensionImpl
public class PsiMemberModifierPropertyShortcutsInspection extends InternalInspection {
    private static final String PSI_MEMBER_CLASS_NAME = PsiMember.class.getName();
    private static final Map<String, String> MODIFIER_TO_SHORTCUT_METHOD_NAME = Map.of(
        "FINAL", "isFinal",
        "PRIVATE", "isPrivate",
        "PROTECTED", "isProtected",
        "PUBLIC", "isPublic",
        "STATIC", "isStatic"
    );

    @Nonnull
    @Override
    public String getDisplayName() {
        return DevKitLocalize.psiMemberModifierPropertyStortcutsInspectionDisplayName().get();
    }

    @Override
    public PsiElementVisitor buildInternalVisitor(@Nonnull ProblemsHolder holder, boolean isOnTheFly) {
        return new JavaElementVisitor() {
            @Override
            @RequiredReadAction
            public void visitMethodCallExpression(@Nonnull PsiMethodCallExpression expression) {
                PsiReferenceExpression methodExpression = expression.getMethodExpression();
                if (!"hasModifierProperty".equals(methodExpression.getReferenceName())) {
                    return;
                }

                PsiExpressionList argumentList = expression.getArgumentList();
                if (argumentList.getExpressionCount() != 1) {
                    return;
                }

                PsiElement method = methodExpression.resolve();
                PsiElement parent = (method == null) ? null : method.getParent();
                PsiClass ownerClass = parent instanceof PsiClass psiClass ? psiClass : null;
                if (ownerClass == null || !InheritanceUtil.isInheritor(ownerClass, PSI_MEMBER_CLASS_NAME)) {
                    return;
                }

                if (argumentList.getExpressions()[0] instanceof PsiReferenceExpression nameRefExpr
                    && nameRefExpr.resolve() instanceof PsiField field
                    && MODIFIER_TO_SHORTCUT_METHOD_NAME.keySet().contains(field.getName())
                ) {
                    String replacementMethodName = MODIFIER_TO_SHORTCUT_METHOD_NAME.get(field.getName());
                    PsiExpression qualifier = methodExpression.getQualifierExpression();
                    String replacementCodeBlock = (qualifier != null ? qualifier.getText() : "") + "." + replacementMethodName + "()";
                    holder.registerProblem(
                        expression,
                        getDisplayName(),
                        new MyQuickFix(expression, replacementMethodName, replacementCodeBlock)
                    );
                }
            }
        };
    }

    private static class MyQuickFix extends LocalQuickFixOnPsiElement {
        private final String myReplacementMethodName, myReplacementCodeBlock;
        private MyQuickFix(@Nonnull PsiElement element, String replacementMethodName, String replacementCodeBlock) {
            super(element);
            myReplacementMethodName = replacementMethodName;
            myReplacementCodeBlock = replacementCodeBlock;
        }

        @Nonnull
        @Override
        public String getFamilyName() {
            return DevKitLocalize.inspectionsGroupName().get();
        }

        @Nonnull
        @Override
        public String getText() {
            return DevKitLocalize.psiMemberReplaceWithShortcutFix(myReplacementMethodName).get();
        }

        @Override
        public void invoke(
            @Nonnull Project project,
            @Nonnull PsiFile file,
            @Nonnull PsiElement startElement,
            @Nonnull PsiElement endElement
        ) {
            PsiExpression newExpression = JavaPsiFacade.getElementFactory(project)
                .createExpressionFromText(myReplacementCodeBlock, startElement);

            WriteAction.run(() -> startElement.replace(newExpression));
        }
    }
}
