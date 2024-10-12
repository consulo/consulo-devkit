package consulo.devkit.localize;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.WriteAction;
import consulo.application.progress.Task;
import consulo.document.Document;
import consulo.fileEditor.EditorNotificationBuilder;
import consulo.fileEditor.EditorNotificationProvider;
import consulo.fileEditor.FileEditor;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.Alerts;
import consulo.ui.UIAccess;
import consulo.undoRedo.CommandProcessor;
import consulo.util.io.CharSequenceReader;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import org.jetbrains.yaml.psi.YAMLFile;
import org.yaml.snakeyaml.Yaml;

import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2024-10-12
 */
@ExtensionImpl
public class LocalizeEditorNotificationProvider implements EditorNotificationProvider {
    private final Project myProject;
    private final PsiDocumentManager myDocumentManager;
    private final PsiManager myPsiManager;
    private final CommandProcessor myCommandProcessor;

    @Inject
    public LocalizeEditorNotificationProvider(Project project,
                                              PsiDocumentManager documentManager,
                                              PsiManager psiManager,
                                              CommandProcessor commandProcessor) {
        myProject = project;
        myDocumentManager = documentManager;
        myPsiManager = psiManager;
        myCommandProcessor = commandProcessor;
    }

    @Nonnull
    @Override
    public String getId() {
        return "localize-editor-notification";
    }

    @RequiredReadAction
    @Nullable
    @Override
    public EditorNotificationBuilder buildNotification(@Nonnull VirtualFile virtualFile,
                                                       @Nonnull FileEditor fileEditor,
                                                       @Nonnull Supplier<EditorNotificationBuilder> supplier) {
        Locale locale = LocalizeUtil.extractLocaleFromFile(virtualFile);
        if (locale == null) {
            return null;
        }

        EditorNotificationBuilder builder = supplier.get();
        builder.withText(LocalizeValue.localizeTODO("Locale: " + locale.getDisplayName()));

        builder.withAction(LocalizeValue.localizeTODO("Sort"), e -> {
            PsiFile file = myPsiManager.findFile(virtualFile);
            if (!(file instanceof YAMLFile yamlFile)) {
                return;
            }

            Document document = myDocumentManager.getDocument(yamlFile);
            if (document == null) {
                return;
            }

            // since we use document text - there no sense, but all data must be in file before rewrite
            myDocumentManager.commitDocument(document);

            UIAccess uiAccess = UIAccess.current();

            Task.Backgroundable.queue(myProject, "Reordering...", indicator -> reorder(document, uiAccess));
        });

        return builder;
    }

    @SuppressWarnings("unchecked")
    private void reorder(Document document, UIAccess uiAccess) {
        String newText;
        try {
            Yaml yaml = LocalizeYamlUtil.create();

            Map<String, Object> data;
            try (CharSequenceReader reader = new CharSequenceReader(document.getImmutableCharSequence())) {
                data = yaml.loadAs(reader, Map.class);
            }

            TreeMap<String, Object> sortedData = new TreeMap<>(data);

            newText = yaml.dump(sortedData);
        }
        catch (Exception error) {
            uiAccess.give(() -> Alerts.okError(LocalizeValue.of(error.getLocalizedMessage())).showAsync(myProject));
            return;
        }

        WriteAction.runAndWait(() -> {
            myCommandProcessor.executeCommand(myProject, () -> {
                document.setText(newText);
            }, "Sorting Keys", null);
        });
    }
}
