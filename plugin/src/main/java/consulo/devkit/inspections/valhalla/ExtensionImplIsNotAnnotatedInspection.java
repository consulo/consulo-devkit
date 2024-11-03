package consulo.devkit.inspections.valhalla;

import com.intellij.java.analysis.impl.codeInsight.intention.AddAnnotationFix;
import com.intellij.java.language.codeInsight.AnnotationUtil;
import com.intellij.java.language.psi.JavaElementVisitor;
import com.intellij.java.language.psi.JavaPsiFacade;
import com.intellij.java.language.psi.PsiClass;
import com.intellij.java.language.psi.PsiIdentifier;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.psi.PsiElementVisitor;
import org.jetbrains.idea.devkit.inspections.internal.InternalInspection;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2022-08-08
 */
@ExtensionImpl
public class ExtensionImplIsNotAnnotatedInspection extends InternalInspection {
    @Nonnull
    @Override
    public String getDisplayName() {
        return "Extension implementation is not annotated by @ExtensionImpl";
    }

    @Override
    public PsiElementVisitor buildInternalVisitor(@Nonnull ProblemsHolder holder, boolean isOnTheFly) {
        return new JavaElementVisitor() {
            @Override
            @RequiredReadAction
            public void visitClass(PsiClass aClass) {
                PsiIdentifier nameIdentifier = aClass.getNameIdentifier();
                if (nameIdentifier == null) {
                    return;
                }

                if (!ExtensionImplUtil.isTargetClass(aClass)) {
                    return;
                }

                // not annotated by @ExtensionAPI, and not annotated by @ExtensionImpl, but has @ExtensionAPI in class hierarchy
                if (!AnnotationUtil.isAnnotated(aClass, ValhallaClasses.ExtensionAPI, 0) &&
                    !AnnotationUtil.isAnnotated(aClass, ValhallaClasses.ExtensionImpl, 0) &&
                    AnnotationUtil.isAnnotated(aClass, ValhallaClasses.ExtensionAPI, AnnotationUtil.CHECK_HIERARCHY)) {
                    PsiClass syntheticAction = JavaPsiFacade.getInstance(aClass.getProject())
                        .findClass(ValhallaClasses.SyntheticIntentionAction, aClass.getResolveScope());
                    if (syntheticAction != null && aClass.isInheritor(syntheticAction, true)) {
                        return;
                    }

                    AddAnnotationFix addAnnotationFix = new AddAnnotationFix(ValhallaClasses.ExtensionImpl, aClass);
                    holder.registerProblem(
                        nameIdentifier,
                        "Extension implementation not annotated by @ExtensionImpl",
                        ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                        addAnnotationFix
                    );
                }
            }
        };
    }
}
