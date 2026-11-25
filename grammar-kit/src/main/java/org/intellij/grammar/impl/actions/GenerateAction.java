/*
 * Copyright 2011-present Greg Shrago
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.intellij.grammar.impl.actions;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.ReadAction;
import consulo.application.WriteAction;
import consulo.application.progress.PerformInBackgroundOption;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.progress.Task;
import consulo.component.ProcessCanceledException;
import consulo.devkit.grammarKit.generator.GenerateTarget;
import consulo.devkit.grammarKit.impl.BnfNotificationGroup;
import consulo.devkit.grammarKit.localize.BnfLocalize;
import consulo.document.FileDocumentManager;
import consulo.language.editor.LangDataKeys;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ui.notification.NotificationGroup;
import consulo.project.ui.notification.NotificationService;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithAsyncUpdate;
import consulo.ui.ex.action.coroutine.ActionSafeReadLock;
import consulo.util.collection.ContainerUtil;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.io.FileUtil;
import consulo.util.io.PathUtil;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.annotation.Nonnull;
import org.intellij.grammar.KnownAttribute;
import org.intellij.grammar.generator.ParserGenerator;
import org.intellij.grammar.psi.BnfFile;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import static org.intellij.grammar.generator.ParserGeneratorUtil.getRootAttribute;
import static org.intellij.grammar.impl.actions.FileGeneratorUtil.getTargetDirectoryFor;

/**
 * @author gregory
 * @since 2011-07-15
 */
public class GenerateAction extends AnAction implements AnActionWithAsyncUpdate {
    public static final NotificationGroup LOG_GROUP =
        NotificationGroup.logOnlyGroup("parserGeneratorLog", BnfLocalize.notificationGroupParserGeneratorLog());
    private static final Logger LOG = Logger.getInstance(GenerateAction.class);

    private final Set<GenerateTarget> myGenerateTargets;

    public GenerateAction(LocalizeValue text, Set<GenerateTarget> generateTargets) {
        super(text);
        myGenerateTargets = generateTargets;
    }

    @Override
    public Coroutine<?, ?> updateAsync(AnActionEvent e) {
        return ActionSafeReadLock.run(
            e,
            presentation -> presentation.setEnabledAndVisible(e.hasData(Project.KEY)),
            presentation -> presentation.setEnabledAndVisible(!getFiles(e).isEmpty())
        ).toCoroutine();
    }

    @RequiredReadAction
    private static List<BnfFile> getFiles(AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        VirtualFile[] files = e.getData(LangDataKeys.VIRTUAL_FILE_ARRAY);
        if (project == null || files == null) {
            return Collections.emptyList();
        }
        PsiManager manager = PsiManager.getInstance(project);
        return ContainerUtil.mapNotNull(files, file -> {
            @SuppressWarnings("RequiredXAction")
            PsiFile psiFile = manager.findFile(file);
            return psiFile instanceof BnfFile bnfFile ? bnfFile : null;
        });
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        List<BnfFile> files = getFiles(e);
        if (project == null || files.isEmpty()) {
            return;
        }
        PsiDocumentManager.getInstance(project).commitAllDocuments();
        FileDocumentManager.getInstance().saveAllDocuments();

        doGenerate(project, files, myGenerateTargets);
    }

    @RequiredUIAccess
    public static void doGenerate(@Nonnull final Project project, final List<BnfFile> bnfFiles, Set<GenerateTarget> generateTargets) {
        final Map<BnfFile, VirtualFile> rootMap = new LinkedHashMap<>();
        WriteAction.run(() -> {
            for (BnfFile file : bnfFiles) {
                String parserClass = getRootAttribute(file.getVersion(), file, KnownAttribute.PARSER_CLASS);
                VirtualFile target = getTargetDirectoryFor(
                    project,
                    file.getVirtualFile(),
                    StringUtil.getShortName(parserClass) + ".java",
                    StringUtil.getPackageName(parserClass),
                    true
                );
                rootMap.put(file, target);
            }
        });

        ProgressManager.getInstance().run(
            new Task.Backgroundable(project, BnfLocalize.taskTitleParserGeneration(), true, PerformInBackgroundOption.ALWAYS_BACKGROUND) {
                List<File> files = new ArrayList<>();
                Set<VirtualFile> targets = new LinkedHashSet<>();
                long totalWritten = 0;

                @Override
                @RequiredReadAction
                public void run(@Nonnull ProgressIndicator indicator) {
                    long startTime = System.currentTimeMillis();
                    indicator.setIndeterminate(true);
                    try {
                        runInner();
                    }
                    finally {
                        if (bnfFiles.size() > 3) {
                            NotificationService.getInstance().newInfo(BnfNotificationGroup.GRAMMAR_KIT)
                                .content(BnfLocalize.notificationContentNGrammarsNFilesGeneratedWithSizeInTime(
                                    bnfFiles.size(),
                                    files.size(),
                                    StringUtil.formatFileSize(totalWritten),
                                    StringUtil.formatDuration(System.currentTimeMillis() - startTime)
                                ))
                                .notify(project);
                        }
                        VirtualFileUtil.markDirtyAndRefresh(true, true, true, targets.toArray(new VirtualFile[targets.size()]));
                    }
                }

                @RequiredReadAction
                private void runInner() {
                    for (final BnfFile file : bnfFiles) {
                        final String sourcePath =
                            FileUtil.toSystemDependentName(PathUtil.getCanonicalPath(file.getVirtualFile().getParent().getPath()));
                        VirtualFile target = rootMap.get(file);
                        if (target == null) {
                            return;
                        }
                        targets.add(target);
                        final File genDir = new File(VirtualFileUtil.virtualToIoFile(target).getAbsolutePath());
                        try {
                            long time = System.currentTimeMillis();
                            int filesCount = files.size();
                            ReadAction.run(
                                () -> new ParserGenerator(file, sourcePath, genDir.getPath(), "") {
                                    @Override
                                    protected PrintWriter openOutputInner(File file) throws IOException {
                                        files.add(file);
                                        return super.openOutputInner(file);
                                    }
                                }.generate(generateTargets)
                            );
                            long millis = System.currentTimeMillis() - time;
                            long written = 0;
                            for (File f : files.subList(filesCount, files.size())) {
                                written += f.length();
                            }
                            totalWritten += written;
                            NotificationService.getInstance().newInfo(BnfNotificationGroup.GRAMMAR_KIT)
                                .title(BnfLocalize.notificationTitleFileGeneratedWithSize(
                                    file.getName(),
                                    StringUtil.formatFileSize(written)
                                ))
                                .content(BnfLocalize.notificationContentToDirInTime(genDir.getPath(), StringUtil.formatDuration(millis)))
                                .notify(project);
                        }
                        catch (ProcessCanceledException ignored) {
                        }
                        catch (Exception ex) {
                            NotificationService.getInstance().newError(BnfNotificationGroup.GRAMMAR_KIT)
                                .title(BnfLocalize.notificationTitleFileGenerationFailed(file.getName()))
                                .content(LocalizeValue.of(ex))
                                .notify(project);
                            LOG.warn(ex);
                        }
                    }
                }
            }
        );
    }
}
