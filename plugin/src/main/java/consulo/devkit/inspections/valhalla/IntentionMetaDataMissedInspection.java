package consulo.devkit.inspections.valhalla;

import com.intellij.java.analysis.impl.codeInsight.intention.AddAnnotationFix;
import com.intellij.java.language.codeInsight.AnnotationUtil;
import com.intellij.java.language.psi.JavaElementVisitor;
import com.intellij.java.language.psi.JavaPsiFacade;
import com.intellij.java.language.psi.PsiClass;
import com.intellij.java.language.psi.PsiIdentifier;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.psi.PsiElementVisitor;
import org.jetbrains.idea.devkit.inspections.internal.InternalInspection;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2022-12-18
 */
@ExtensionImpl
public class IntentionMetaDataMissedInspection extends InternalInspection {
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

                if (!AnnotationUtil.isAnnotated(aClass, ValhallaClasses.ExtensionImpl, 0)) {
                    return;
                }

                // already annotated
                if (AnnotationUtil.isAnnotated(aClass, ValhallaClasses.IntentionMetaData, 0)) {
                    return;
                }

                JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(aClass.getProject());
                PsiClass intentionAction = javaPsiFacade.findClass(ValhallaClasses.IntentionAction, aClass.getResolveScope());
                if (intentionAction == null) {
                    return;
                }

                if (!aClass.isInheritor(intentionAction, true)) {
                    return;
                }

                PsiClass syntheticAction = javaPsiFacade.findClass(ValhallaClasses.SyntheticIntentionAction, aClass.getResolveScope());
                if (syntheticAction == null) {
                    return;
                }


                if (aClass.isInheritor(syntheticAction, true)) {
                    return;
                }

                holder.registerProblem(
                    nameIdentifier,
                    "Missed @IntentionMetaData annotation",
                    new AddAnnotationFix(ValhallaClasses.IntentionMetaData, aClass)
                );
            }
        };
    }

    @Nonnull
    @Override
    public String getDisplayName() {
        return "Missing @IntentionMetaData annotation";
    }

    @Nonnull
    @Override
    public HighlightDisplayLevel getDefaultLevel() {
        return HighlightDisplayLevel.ERROR;
    }
}
