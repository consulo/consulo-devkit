package consulo.devkit.localize.inspection;

import com.ibm.icu.text.MessageFormat;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.devkit.localize.DevKitLocalize;
import consulo.devkit.localize.LocalizeUtil;
import consulo.devkit.util.PluginModuleUtil;
import consulo.language.Language;
import consulo.language.editor.inspection.LocalInspectionTool;
import consulo.language.editor.inspection.LocalInspectionToolSession;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.yaml.YAMLLanguage;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLMapping;

import java.util.Locale;

/**
 * @author VISTALL
 * @since 2024-11-12
 */
@ExtensionImpl
public class InvalidMessageFormatInspection extends LocalInspectionTool {
    @Nonnull
    @Override
    public LocalizeValue getGroupDisplayName() {
        return DevKitLocalize.inspectionsGroupName();
    }

    @Nullable
    @Override
    public Language getLanguage() {
        return YAMLLanguage.INSTANCE;
    }

    @Override
    public boolean isEnabledByDefault() {
        return true;
    }

    @Nonnull
    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.localizeTODO("Invalid icu.MessageFormat text");
    }

    @Nonnull
    @Override
    public HighlightDisplayLevel getDefaultLevel() {
        return HighlightDisplayLevel.ERROR;
    }

    @Nonnull
    @Override
    @RequiredReadAction
    public PsiElementVisitor buildVisitor(@Nonnull ProblemsHolder holder, boolean isOnTheFly, @Nonnull LocalInspectionToolSession session, @Nonnull Object state) {
        PsiFile file = holder.getFile();

        if (!PluginModuleUtil.isConsuloOrPluginProject(file.getProject(), file.getModule())) {
            return PsiElementVisitor.EMPTY_VISITOR;
        }

        Locale locale = LocalizeUtil.extractLocaleFromFile(file.getVirtualFile());
        if (locale == null) {
            return PsiElementVisitor.EMPTY_VISITOR;
        }

        return new PsiElementVisitor() {
            @Override
            @RequiredReadAction
            public void visitElement(PsiElement element) {
                if (element instanceof YAMLKeyValue keyValue && LocalizeUtil.TEXT_KEY.equals(keyValue.getKeyText()) && keyValue.getParent() instanceof YAMLMapping) {
                    try {
                        new MessageFormat(keyValue.getValueText(), locale);
                    } catch (Exception e) {
                        holder.registerProblem(keyValue.getValue(), e.getMessage());
                    }
                }
            }
        };
    }
}