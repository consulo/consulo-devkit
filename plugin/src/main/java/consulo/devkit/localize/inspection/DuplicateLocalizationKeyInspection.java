package consulo.devkit.localize.inspection;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.devkit.localize.DevKitLocalize;
import consulo.devkit.localize.LocalizeUtil;
import consulo.devkit.util.PluginModuleUtil;
import consulo.language.Language;
import consulo.language.editor.inspection.*;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.pattern.PsiElementPattern;
import consulo.language.pattern.StandardPatterns;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.yaml.YAMLLanguage;
import org.jetbrains.yaml.YAMLTokenTypes;
import org.jetbrains.yaml.psi.YAMLDocument;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLMapping;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * @author VISTALL
 * @since 2024-09-09
 */
@ExtensionImpl
public class DuplicateLocalizationKeyInspection extends LocalInspectionTool {
    private static final PsiElementPattern.Capture<PsiElement> OUR_SCALAR_PATTERN = StandardPatterns.psiElement()
        .withElementType(YAMLTokenTypes.SCALAR_KEY)
        .withParent(YAMLKeyValue.class)
        .withSuperParent(2, YAMLMapping.class)
        .withSuperParent(3, YAMLDocument.class)
        .withSuperParent(4, YAMLFile.class);

    private static final Key<Set<String>> ourAlreadyDefinedKeys = Key.create("DuplicateLocalizationKeyInspection.ourAlreadyDefinedKeys");

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
        return DevKitLocalize.inspectionDuplicateLocalizationKeyDisplayName();
    }

    @Nonnull
    @Override
    public HighlightDisplayLevel getDefaultLevel() {
        return HighlightDisplayLevel.ERROR;
    }

    @Nonnull
    @Override
    @RequiredReadAction
    public PsiElementVisitor buildVisitor(
        @Nonnull ProblemsHolder holder,
        boolean isOnTheFly,
        @Nonnull LocalInspectionToolSession session,
        @Nonnull Object state
    ) {
        PsiFile file = holder.getFile();
        if (!PluginModuleUtil.isConsuloOrPluginProject(file.getProject(), file.getModule())) {
            return PsiElementVisitor.EMPTY_VISITOR;
        }

        if (!LocalizeUtil.isDefaultLocalizeFile(file.getVirtualFile())) {
            return PsiElementVisitor.EMPTY_VISITOR;
        }

        return new PsiElementVisitor() {
            @Override
            public void visitElement(PsiElement element) {
                if (!OUR_SCALAR_PATTERN.accepts(element)) {
                    return;
                }

                Set<String> data = session.getUserData(ourAlreadyDefinedKeys);
                if (data == null) {
                    session.putUserDataIfAbsent(ourAlreadyDefinedKeys, data = new CopyOnWriteArraySet<>());
                }

                YAMLKeyValue keyValue = (YAMLKeyValue) element.getParent();

                if (!data.add(keyValue.getKeyText())) {
                    holder.newProblem(DevKitLocalize.inspectionDuplicateLocalizationKeyMessage())
                        .range(element)
                        .highlightType(ProblemHighlightType.GENERIC_ERROR)
                        .create();
                }
            }
        };
    }
}
