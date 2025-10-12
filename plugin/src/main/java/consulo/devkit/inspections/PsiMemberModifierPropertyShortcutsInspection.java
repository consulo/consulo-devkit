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
import consulo.localize.LocalizeValue;
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
    private static final String PSI_PSI_MODIFIER_LIST_OWNER_CLASS_NAME = PsiModifierListOwner.class.getName();
    private static final String PSI_MEMBER_CLASS_NAME = PsiMember.class.getName();
    private static final Map<String, String> MODIFIER_TO_SHORTCUT_METHOD_NAME = Map.of(
        "ABSTRACT", "isAbstract",
        "FINAL", "isFinal",
        "PRIVATE", "isPrivate",
        "PROTECTED", "isProtected",
        "PUBLIC", "isPublic",
        "STATIC", "isStatic"
    );

    @Nonnull
    @Override
    public LocalizeValue getDisplayName() {
        return DevKitLocalize.psiMemberModifierPropertyStortcutsInspectionDisplayName();
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

                PsiClass declaringClass = methodExpression.resolve() instanceof PsiMethod method ? method.getContainingClass() : null;
                if (declaringClass == null || !PSI_PSI_MODIFIER_LIST_OWNER_CLASS_NAME.equals(declaringClass.getQualifiedName())) {
                    return;
                }

                if (methodExpression.getQualifierExpression() instanceof PsiExpression qualifier
                    && qualifier.getType() instanceof PsiClassType classType
                    && classType.resolve() instanceof PsiClass ownerClass
                    && InheritanceUtil.isInheritor(ownerClass, PSI_MEMBER_CLASS_NAME)
                    && argumentList.getExpressions()[0] instanceof PsiReferenceExpression nameRefExpr
                    && nameRefExpr.resolve() instanceof PsiField field
                    && MODIFIER_TO_SHORTCUT_METHOD_NAME.keySet().contains(field.getName())
                ) {
                    String replacementMethodName = MODIFIER_TO_SHORTCUT_METHOD_NAME.get(field.getName());
                    String replacementCodeBlock = qualifier.getText() + "." + replacementMethodName + "()";
                    holder.newProblem(getDisplayName())
                        .range(expression)
                        .withFix(new MyQuickFix(expression, replacementMethodName, replacementCodeBlock))
                        .create();
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
        public LocalizeValue getText() {
            return DevKitLocalize.psiMemberReplaceWithShortcutFix(myReplacementMethodName);
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
