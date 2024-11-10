package consulo.devkit.inspections.inject;

import com.intellij.java.analysis.impl.codeInsight.intention.AddAnnotationFix;
import com.intellij.java.language.codeInsight.AnnotationUtil;
import com.intellij.java.language.psi.JavaElementVisitor;
import com.intellij.java.language.psi.PsiClass;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.devkit.inspections.util.service.ServiceInfo;
import consulo.devkit.inspections.util.service.ServiceLocator;
import consulo.devkit.localize.DevKitLocalize;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.psi.PsiElementVisitor;
import jakarta.annotation.Nonnull;
import org.jetbrains.idea.devkit.inspections.internal.InternalInspection;

import java.util.List;

/**
 * @author VISTALL
 * @since 2018-08-16
 */
@ExtensionImpl
public class NoSingletonAnnotationInspection extends InternalInspection {
    private static final List<String> SINGLETON_ANNOTATIONS = List.of("jakarta.inject.Singleton");

    private static class Visitor extends JavaElementVisitor {
        private final ProblemsHolder myHolder;

        public Visitor(ProblemsHolder holder) {
            myHolder = holder;
        }

        @Override
        @RequiredReadAction
        public void visitClass(@Nonnull PsiClass aClass) {
            if (isSingleton(aClass) && !AnnotationUtil.isAnnotated(aClass, SINGLETON_ANNOTATIONS, 0)) {
                myHolder.newProblem(DevKitLocalize.noSingletonAnnotationInspectionMessage())
                    .range(aClass.getNameIdentifier())
                    .withFix(new AddAnnotationFix(SINGLETON_ANNOTATIONS.get(0), aClass))
                    .create();
            }
        }
    }

    @Nonnull
    @Override
    public String getDisplayName() {
        return DevKitLocalize.noSingletonAnnotationInspectionDisplayName().get();
    }

    @Nonnull
    @Override
    public HighlightDisplayLevel getDefaultLevel() {
        return HighlightDisplayLevel.ERROR;
    }

    @Override
    public PsiElementVisitor buildInternalVisitor(@Nonnull ProblemsHolder holder, boolean isOnTheFly) {
        return new Visitor(holder);
    }

    @RequiredReadAction
    private static boolean isSingleton(PsiClass psiClass) {
        ServiceInfo serviceInfo = ServiceLocator.findImplementationService(psiClass);
        return serviceInfo != null;
    }
}
