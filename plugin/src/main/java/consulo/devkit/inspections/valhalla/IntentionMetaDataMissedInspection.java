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
    @Nonnull
    @Override
    public String getDisplayName() {
        return DevKitLocalize.intentionMetaDataMissedInspectionDisplayName().get();
    }

    @Nonnull
    @Override
    public HighlightDisplayLevel getDefaultLevel() {
        return HighlightDisplayLevel.ERROR;
    }

    @Override
    public PsiElementVisitor buildInternalVisitor(@Nonnull ProblemsHolder holder, boolean isOnTheFly) {
        return new JavaElementVisitor() {
            @Override
            @RequiredReadAction
            public void visitClass(@Nonnull PsiClass aClass) {
                PsiIdentifier nameIdentifier = aClass.getNameIdentifier();

                if (nameIdentifier == null || !ExtensionImplUtil.isTargetClass(aClass)
                    || !AnnotationUtil.isAnnotated(aClass, ValhallaClasses.EXTENSION_IMPL, 0)
                    || AnnotationUtil.isAnnotated(aClass, ValhallaClasses.INTENTION_META_DATA, 0)) {
                    return;
                }

                JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(aClass.getProject());
                PsiClass intentionAction = javaPsiFacade.findClass(ValhallaClasses.INTENTION_ACTION, aClass.getResolveScope());
                if (intentionAction == null || !aClass.isInheritor(intentionAction, true)) {
                    return;
                }

                PsiClass syntheticAction = javaPsiFacade.findClass(ValhallaClasses.SYNTHETIC_INTENTION_ACTION, aClass.getResolveScope());
                if (syntheticAction == null || aClass.isInheritor(syntheticAction, true)) {
                    return;
                }

                holder.newProblem(DevKitLocalize.intentionMetaDataMissedInspectionMessage())
                    .range(nameIdentifier)
                    .withFix(new AddAnnotationFix(ValhallaClasses.INTENTION_META_DATA, aClass))
                    .create();
            }
        };
    }
}
