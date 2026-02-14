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
import consulo.application.Application;
import consulo.application.progress.PerformInBackgroundOption;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.progress.Task;
import consulo.application.util.function.ThrowableComputable;
import consulo.component.ProcessCanceledException;
import consulo.devkit.grammarKit.generator.GenerateTarget;
import consulo.devkit.grammarKit.impl.BnfNotificationGroup;
import consulo.document.FileDocumentManager;
import consulo.language.editor.LangDataKeys;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ui.notification.Notification;
import consulo.project.ui.notification.NotificationGroup;
import consulo.project.ui.notification.NotificationType;
import consulo.project.ui.notification.Notifications;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.util.collection.ContainerUtil;
import consulo.util.io.FileUtil;
import consulo.util.io.PathUtil;
import consulo.util.lang.ExceptionUtil;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
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
 * Date: 15.07.11 17:12
 */
public class GenerateAction extends AnAction {
    public static final NotificationGroup LOG_GROUP = NotificationGroup.logOnlyGroup("Parser Generator Log");
    private static final Logger LOG = Logger.getInstance(GenerateAction.class);

    private final Set<GenerateTarget> myGenerateTargets;

    public GenerateAction(@Nullable String text, Set<GenerateTarget> generateTargets) {
        super(text);
        myGenerateTargets = generateTargets;
    }

    @Override
    @RequiredReadAction
    public void update(@Nonnull AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        List<BnfFile> files = getFiles(e);
        e.getPresentation().setEnabledAndVisible(project != null && !files.isEmpty());
    }

    @RequiredReadAction
    private static List<BnfFile> getFiles(AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        VirtualFile[] files = e.getData(LangDataKeys.VIRTUAL_FILE_ARRAY);
        if (project == null || files == null) {
            return Collections.emptyList();
        }
        final PsiManager manager = PsiManager.getInstance(project);
        return ContainerUtil.mapNotNull(files, file -> {
            @SuppressWarnings("RequiredXAction")
            PsiFile psiFile = manager.findFile(file);
            return psiFile instanceof BnfFile bnfFile ? bnfFile : null;
        });
    }

    @Override
    @RequiredReadAction
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

    public static void doGenerate(@Nonnull final Project project, final List<BnfFile> bnfFiles, Set<GenerateTarget> generateTargets) {
        final Map<BnfFile, VirtualFile> rootMap = new LinkedHashMap<>();
        Application.get().runWriteAction(() -> {
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
            new Task.Backgroundable(project, "Parser Generation", true, PerformInBackgroundOption.ALWAYS_BACKGROUND) {
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
                        String report = String.format(
                            "%d grammars: %d files generated (%s) in %s",
                            bnfFiles.size(),
                            files.size(),
                            StringUtil.formatFileSize(totalWritten),
                            StringUtil.formatDuration(System.currentTimeMillis() - startTime)
                        );
                        if (bnfFiles.size() > 3) {
                            Notifications.Bus.notify(
                                new Notification(BnfNotificationGroup.GRAMMAR_KIT, "", report, NotificationType.INFORMATION),
                                project
                            );
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
                            Application.get().runReadAction(new ThrowableComputable<Boolean, Exception>() {
                                @Override
                                public Boolean compute() throws Exception {
                                    new ParserGenerator(file, sourcePath, genDir.getPath(), "") {
                                        @Override
                                        protected PrintWriter openOutputInner(File file) throws IOException {
                                            files.add(file);
                                            return super.openOutputInner(file);
                                        }
                                    }.generate(generateTargets);
                                    return true;
                                }
                            });
                            long millis = System.currentTimeMillis() - time;
                            String duration = millis < 1000 ? null : StringUtil.formatDuration(millis);
                            long written = 0;
                            for (File f : files.subList(filesCount, files.size())) {
                                written += f.length();
                            }
                            totalWritten += written;
                            Notifications.Bus.notify(
                                new Notification(
                                    BnfNotificationGroup.GRAMMAR_KIT,
                                    String.format("%s generated (%s)", file.getName(), StringUtil.formatFileSize(written)),
                                    "to " + genDir + (duration == null ? "" : " in " + duration),
                                    NotificationType.INFORMATION
                                ),
                                project
                            );
                        }
                        catch (ProcessCanceledException ignored) {
                        }
                        catch (Exception ex) {
                            Notifications.Bus.notify(
                                new Notification(
                                    BnfNotificationGroup.GRAMMAR_KIT,
                                    file.getName() + " generation failed",
                                    ExceptionUtil.getMessage(ex),
                                    NotificationType.ERROR
                                ),
                                project
                            );
                            LOG.warn(ex);
                        }
                    }
                }
            }
        );
    }
}
