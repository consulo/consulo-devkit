package consulo.devkit.inspections;

import com.intellij.java.language.impl.psi.impl.source.PsiClassReferenceType;
import com.intellij.java.language.psi.*;
import com.intellij.java.language.psi.util.InheritanceUtil;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.devkit.DevKitComponentScope;
import consulo.devkit.inspections.valhalla.ExtensionImplUtil;
import consulo.devkit.inspections.valhalla.ValhallaClasses;
import consulo.devkit.localize.DevKitLocalize;
import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.localize.LocalizeValue;
import consulo.module.Module;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import org.jetbrains.idea.devkit.inspections.internal.InternalInspection;

import java.util.Set;

/**
 * @author UNV
 * @since 2025-04-22
 */
@ExtensionImpl
public class GetExtensionPointInspection extends InternalInspection {
    private static final Set<String> EXTENSION_POINT_METHOD_NAMES = Set.of("getExtensionPoint", "getExtensionList");

    @Nonnull
    @Override
    public LocalizeValue getDisplayName() {
        return DevKitLocalize.inspectionsGetExtensionPointValidationTitle();
    }

    @Override
    public PsiElementVisitor buildInternalVisitor(@Nonnull ProblemsHolder holder, boolean isOnTheFly) {
        return new GetExtensionPointVisitor(holder);
    }

    private static class GetExtensionPointVisitor extends JavaElementVisitor {
        @Nonnull
        private final ProblemsHolder myHolder;

        private GetExtensionPointVisitor(@Nonnull ProblemsHolder holder) {
            myHolder = holder;
        }

        @Override
        @RequiredReadAction
        public void visitMethodCallExpression(@Nonnull PsiMethodCallExpression expression) {
            new GetExtensionPointInspector(expression).registerProblem();
        }

        private class GetExtensionPointInspector {
            @Nonnull
            protected final PsiMethodCallExpression myExpression;
            @Nonnull
            protected final PsiReferenceExpression myMethodExpression;
            @Nonnull
            protected PsiClass myExtClass;
            @Nonnull
            protected PsiExpression[] myArgExpressions;

            protected GetExtensionPointInspector(@Nonnull PsiMethodCallExpression expression) {
                myExpression = expression;
                myMethodExpression = expression.getMethodExpression();
            }

            @RequiredReadAction
            public void registerProblem() {
                if (!isApplicable()) {
                    return;
                }

                PsiAnnotation annotation = myExtClass.getAnnotation("consulo.annotation.component.ExtensionAPI");
                String className = myExtClass.getQualifiedName();

                if (annotation == null) {
                    assert className != null;
                    myHolder.newProblem(DevKitLocalize.inspectionsGetExtensionPointValidationNoAnnotation(className))
                        .range(myArgExpressions[0])
                        .highlightType(ProblemHighlightType.GENERIC_ERROR)
                        .create();
                    return;
                }

                if (!(myMethodExpression.getQualifierExpression() instanceof PsiExpression qualifier
                    && qualifier.getType() instanceof PsiType callerType)) {
                    return;
                }

                DevKitComponentScope enumValue = ExtensionImplUtil.resolveScope(annotation.findAttributeValue("value"));

                boolean validScope = false;
                if (enumValue != null) {
                    validScope = switch (enumValue) {
                        case DevKitComponentScope.APPLICATION -> InheritanceUtil.isInheritor(callerType, Application.class.getName());
                        case DevKitComponentScope.PROJECT -> InheritanceUtil.isInheritor(callerType, Project.class.getName());
                        case DevKitComponentScope.MODULE -> InheritanceUtil.isInheritor(callerType, Module.class.getName());
                        default -> false;
                    };
                }

                if (!validScope) {
                    myHolder.newProblem(DevKitLocalize.inspectionsGetExtensionPointValidationIncorrectScope())
                        .range(myExpression)
                        .highlightType(ProblemHighlightType.GENERIC_ERROR)
                        .create();
                }
            }

            @RequiredReadAction
            boolean isApplicable() {
                if (!EXTENSION_POINT_METHOD_NAMES.contains(myMethodExpression.getReferenceName())) {
                    return false;
                }

                PsiElement method = myMethodExpression.resolve();
                PsiElement parent = (method == null) ? null : method.getParent();
                PsiClass declaringClass = (parent instanceof PsiClass psiClass) ? psiClass : null;
                if (declaringClass == null || !InheritanceUtil.isInheritor(declaringClass, ValhallaClasses.COMPONENT_MANAGER)) {
                    return false;
                }

                myArgExpressions = myExpression.getArgumentList().getExpressions();
                if (myArgExpressions.length != 1) {
                    return false;
                }

                if (!(myArgExpressions[0] instanceof PsiClassObjectAccessExpression classAccess)) {
                    return false;
                }

                if (!(classAccess.getOperand().getType() instanceof PsiClassReferenceType extClassRef)
                    || !(extClassRef.resolve() instanceof PsiClass extClass)) {
                    return false;
                }

                myExtClass = extClass;

                return true;
            }
        }
    }
}
