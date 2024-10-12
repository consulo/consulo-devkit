package consulo.devkit.localize;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.WriteAction;
import consulo.application.progress.Task;
import consulo.devkit.localize.index.LocalizeFileIndexExtension;
import consulo.diff.DiffContentFactory;
import consulo.diff.DiffManager;
import consulo.diff.content.DiffContent;
import consulo.diff.request.SimpleDiffRequest;
import consulo.document.Document;
import consulo.fileEditor.EditorNotificationBuilder;
import consulo.fileEditor.EditorNotificationProvider;
import consulo.fileEditor.FileEditor;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.content.scope.ProjectScopes;
import consulo.ui.Alerts;
import consulo.ui.UIAccess;
import consulo.undoRedo.CommandProcessor;
import consulo.util.io.CharSequenceReader;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import org.jetbrains.yaml.psi.YAMLFile;
import org.yaml.snakeyaml.Yaml;

import java.util.*;
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
    private final FileBasedIndex myFileBasedIndex;
    private final DiffContentFactory myDiffContentFactory;
    private final DiffManager myDiffManager;

    @Inject
    public LocalizeEditorNotificationProvider(Project project,
                                              PsiDocumentManager documentManager,
                                              PsiManager psiManager,
                                              CommandProcessor commandProcessor,
                                              FileBasedIndex fileBasedIndex,
                                              DiffContentFactory diffContentFactory,
                                              DiffManager diffManager) {
        myProject = project;
        myDocumentManager = documentManager;
        myPsiManager = psiManager;
        myCommandProcessor = commandProcessor;
        myFileBasedIndex = fileBasedIndex;
        myDiffContentFactory = diffContentFactory;
        myDiffManager = diffManager;
    }

    @Nonnull
    @Override
    public String getId() {
        return "localize-editor-notification";
    }

    @RequiredReadAction
    @Nullable
    @Override
    public EditorNotificationBuilder buildNotification(@Nonnull VirtualFile file,
                                                       @Nonnull FileEditor fileEditor,
                                                       @Nonnull Supplier<EditorNotificationBuilder> supplier) {
        Locale locale = LocalizeUtil.extractLocaleFromFile(file);
        if (locale == null) {
            return null;
        }

        EditorNotificationBuilder builder = supplier.get();
        builder.withText(LocalizeValue.localizeTODO("Locale: " + locale.getDisplayName()));

        builder.withAction(LocalizeValue.localizeTODO("Compare"), e -> {
            String fileName = file.getNameWithoutExtension();
            String packageName = StringUtil.getPackageName(fileName);
            String id = packageName + ".localize." + StringUtil.getShortName(fileName);

            Collection<VirtualFile> containingFiles = myFileBasedIndex.getContainingFiles(
                LocalizeFileIndexExtension.INDEX,
                id,
                ProjectScopes.getAllScope(myProject)
            );

            VirtualFile otherLocalizeFile = containingFiles.stream().filter(it -> !Objects.equals(it, file)).findAny().orElse(null);

            if (otherLocalizeFile == null) {
                Alerts.okError(LocalizeValue.localizeTODO("There not original localization")).showAsync(myProject);
                return;
            }

            DiffContent currentContent = myDiffContentFactory.create(myProject, file);
            DiffContent originalContent = myDiffContentFactory.create(myProject, otherLocalizeFile);

            myDiffManager.showDiff(myProject, new SimpleDiffRequest(id, currentContent, originalContent, "Current Localize", "Original Localize"));
        });

        builder.withAction(LocalizeValue.localizeTODO("Sort"), e -> {
            PsiFile psiFile = myPsiManager.findFile(file);
            if (!(psiFile instanceof YAMLFile yamlFile)) {
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
