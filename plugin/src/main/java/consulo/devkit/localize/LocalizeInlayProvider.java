package consulo.devkit.localize;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.ReadAction;
import consulo.codeEditor.Editor;
import consulo.devkit.localize.folding.LocalizeFoldingBuilder;
import consulo.document.Document;
import consulo.language.Language;
import consulo.language.editor.inlay.*;
import consulo.language.pattern.PsiElementPattern;
import consulo.language.pattern.StandardPatterns;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.util.lang.Pair;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.yaml.YAMLLanguage;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jetbrains.yaml.psi.YAMLKeyValue;

import java.util.Locale;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2025-06-20
 */
@ExtensionImpl
public class LocalizeInlayProvider implements DeclarativeInlayHintsProvider {
    private static final PsiElementPattern.Capture<YAMLKeyValue> KEY_PATTERN =
        StandardPatterns.psiElement(YAMLKeyValue.class).withSuperParent(3, YAMLFile.class);
    private static final HintFormat FORMAT =
        new HintFormat(HintColorKind.Default, HintFontSize.ABitSmallerThanInEditor, HintMarginPadding.MarginAndSmallerPadding);

    @Nullable
    @Override
    public DeclarativeInlayHintsCollector createCollector(PsiFile psiFile, Editor editor) {
        Locale locale = LocalizeUtil.extractLocaleFromFile(psiFile.getVirtualFile());
        if (LocalizeUtil.DEFAULT_LOCALE.equals(locale)) {
            return null;
        }

        Project project = psiFile.getProject();
        Pair<VirtualFile, String> pair =
            LocalizeUtil.findOtherLocaleFile(project, psiFile.getVirtualFile(), FileBasedIndex.getInstance());

        if (pair == null) {
            return null;
        }

        PsiFile file = ReadAction.compute(() -> PsiManager.getInstance(project).findFile(pair.getKey()));
        if (!(file instanceof YAMLFile otherYamlState)) {
            return null;
        }

        Map<String, String> data = ReadAction.compute(() -> LocalizeFoldingBuilder.buildLocalizeCache(otherYamlState));

        Document document = editor.getDocument();

        return new DeclarativeInlayHintsCollector.SharedBypassCollector() {
            @Override
            public void collectFromElement(PsiElement element, DeclarativeInlayTreeSink sink) {
                if (KEY_PATTERN.accepts(element)) {
                    YAMLKeyValue yamlKeyValue = (YAMLKeyValue) element;

                    String otherTextVariant = data.get(yamlKeyValue.getKeyText());
                    if (otherTextVariant != null) {
                        int lineNumber = document.getLineNumber(element.getTextOffset());

                        sink.addPresentation(
                            new DeclarativeInlayPosition.EndOfLinePosition(lineNumber),
                            FORMAT,
                            builder -> {
                                builder.text(otherTextVariant);
                            }
                        );
                    }
                }
            }
        };
    }

    @Nonnull
    @Override
    public Language getLanguage() {
        return YAMLLanguage.INSTANCE;
    }

    @Nonnull
    @Override
    public String getId() {
        return "devkit-yaml-provider";
    }

    @Nonnull
    @Override
    public LocalizeValue getName() {
        return LocalizeValue.localizeTODO("English Localization");
    }

    @Nonnull
    @Override
    public LocalizeValue getDescription() {
        return LocalizeValue.localizeTODO("Showing English Localization in non-english localization");
    }

    @Nonnull
    @Override
    public LocalizeValue getPreviewFileText() {
        return LocalizeValue.localizeTODO("whatsnew.platform.text:\n" +
            "    text: Платформа");
    }

    @Nonnull
    @Override
    public InlayGroup getGroup() {
        return InlayGroup.OTHER_GROUP;
    }
}
