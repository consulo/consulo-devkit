package consulo.devkit.inspections.valhalla;

import com.intellij.java.analysis.impl.codeInsight.intention.AddAnnotationFix;
import com.intellij.java.language.codeInsight.AnnotationUtil;
import com.intellij.java.language.psi.JavaElementVisitor;
import com.intellij.java.language.psi.JavaPsiFacade;
import com.intellij.java.language.psi.PsiClass;
import com.intellij.java.language.psi.PsiIdentifier;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.devkit.localize.DevKitLocalize;
import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.psi.PsiElementVisitor;
import jakarta.annotation.Nonnull;
import org.jetbrains.idea.devkit.inspections.internal.InternalInspection;

/**
 * @author VISTALL
 * @since 2022-08-08
 */
@ExtensionImpl
public class ExtensionImplIsNotAnnotatedInspection extends InternalInspection {
    @Nonnull
    @Override
    public String getDisplayName() {
        return DevKitLocalize.extensionImplIsNotAnnotatedInspectionDisplayName().get();
    }

    @Override
    public PsiElementVisitor buildInternalVisitor(@Nonnull ProblemsHolder holder, boolean isOnTheFly) {
        return new JavaElementVisitor() {
            @Override
            @RequiredReadAction
            public void visitClass(@Nonnull PsiClass aClass) {
                PsiIdentifier nameIdentifier = aClass.getNameIdentifier();
                if (nameIdentifier == null || !ExtensionImplUtil.isTargetClass(aClass)) {
                    return;
                }

                // not annotated by @ExtensionAPI, and not annotated by @ExtensionImpl, but has @ExtensionAPI in class hierarchy
                if (!AnnotationUtil.isAnnotated(aClass, ValhallaClasses.ExtensionAPI, 0)
                    && !AnnotationUtil.isAnnotated(aClass, ValhallaClasses.ExtensionImpl, 0)
                    && AnnotationUtil.isAnnotated(aClass, ValhallaClasses.ExtensionAPI, AnnotationUtil.CHECK_HIERARCHY)) {
                    PsiClass syntheticAction = JavaPsiFacade.getInstance(aClass.getProject())
                        .findClass(ValhallaClasses.SyntheticIntentionAction, aClass.getResolveScope());
                    if (syntheticAction != null && aClass.isInheritor(syntheticAction, true)) {
                        return;
                    }

                    AddAnnotationFix addAnnotationFix = new AddAnnotationFix(ValhallaClasses.ExtensionImpl, aClass);
                    holder.newProblem(DevKitLocalize.extensionImplIsNotAnnotatedInspectionMessage())
                        .range(nameIdentifier)
                        .withFix(addAnnotationFix)
                        .highlightType(ProblemHighlightType.GENERIC_ERROR_OR_WARNING)
                        .create();
                }
            }
        };
    }
}
