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
package org.intellij.grammar.actions;

import static org.intellij.grammar.actions.FileGeneratorUtil.getTargetDirectoryFor;
import static org.intellij.grammar.generator.ParserGeneratorUtil.getRootAttribute;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.intellij.grammar.KnownAttribute;
import org.intellij.grammar.generator.BnfConstants;
import org.intellij.grammar.generator.ParserGenerator;
import org.intellij.grammar.psi.BnfFile;
import org.jetbrains.annotations.NotNull;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vcs.changes.BackgroundFromStartOption;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.ExceptionUtil;
import com.intellij.util.Function;
import com.intellij.util.PathUtil;
import com.intellij.util.containers.ContainerUtil;

/**
 * @author gregory
 *         Date: 15.07.11 17:12
 */
public class GenerateAction extends AnAction {

  public static final NotificationGroup LOG_GROUP = NotificationGroup.logOnlyGroup("Parser Generator Log");
  
  private static final Logger LOG = Logger.getInstance("org.intellij.grammar.actions.GenerateAction");

  @Override
  public void update(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    List<BnfFile> files = getFiles(e);
    e.getPresentation().setEnabledAndVisible(project != null && !files.isEmpty());
  }

  private static List<BnfFile> getFiles(AnActionEvent e) {
    Project project = e.getProject();
    VirtualFile[] files = e.getData(LangDataKeys.VIRTUAL_FILE_ARRAY);
    if (project == null || files == null) return Collections.emptyList();
    final PsiManager manager = PsiManager.getInstance(project);
    return ContainerUtil.mapNotNull(files, new Function<VirtualFile, BnfFile>() {
      @Override
      public BnfFile fun(VirtualFile file) {
        PsiFile psiFile = manager.findFile(file);
        return psiFile instanceof BnfFile ? (BnfFile)psiFile : null;
      }
    });
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = getEventProject(e);
    List<BnfFile> files = getFiles(e);
    if (project == null || files.isEmpty()) return;
    PsiDocumentManager.getInstance(project).commitAllDocuments();
    FileDocumentManager.getInstance().saveAllDocuments();

    doGenerate(project, files);
  }

  public static void doGenerate(@NotNull final Project project, final List<BnfFile> bnfFiles) {
    final Map<BnfFile, VirtualFile> rootMap = ContainerUtil.newLinkedHashMap();
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {
        for (BnfFile file : bnfFiles) {
          String parserClass = getRootAttribute(file, KnownAttribute.PARSER_CLASS);
          VirtualFile target =
            getTargetDirectoryFor(project, file.getVirtualFile(),
                                  StringUtil.getShortName(parserClass) + ".java",
                                  StringUtil.getPackageName(parserClass), true);
          rootMap.put(file, target);
        }
      }
    });

    ProgressManager.getInstance().run(new Task.Backgroundable(project, "Parser Generation", true, new BackgroundFromStartOption()) {

      List<File> files = ContainerUtil.newArrayList();
      Set<VirtualFile> targets = ContainerUtil.newLinkedHashSet();
      long totalWritten = 0;

      @Override
      public void run(@NotNull ProgressIndicator indicator) {
        long startTime = System.currentTimeMillis();
        indicator.setIndeterminate(true);
        try {
          runInner();
        }
        finally {
          String report = String.format("%d grammars: %d files generated (%s) in %s",
                                        bnfFiles.size(),
                                        files.size(),
                                        StringUtil.formatFileSize(totalWritten),
                                        StringUtil.formatDuration(System.currentTimeMillis() - startTime));
          if (bnfFiles.size() > 3) {
            Notifications.Bus.notify(new Notification(
              BnfConstants.GENERATION_GROUP,
              "", report, NotificationType.INFORMATION), project);
          }
          VfsUtil.markDirtyAndRefresh(true, true, true, targets.toArray(new VirtualFile[targets.size()]));
        }
      }

      private void runInner() {
        for (final BnfFile file : bnfFiles) {
          final String sourcePath = FileUtil.toSystemDependentName(PathUtil.getCanonicalPath(
            file.getVirtualFile().getParent().getPath()));
          VirtualFile target = rootMap.get(file);
          if (target == null) return;
          targets.add(target);
          final File genDir = new File(VfsUtil.virtualToIoFile(target).getAbsolutePath());
          try {
            long time = System.currentTimeMillis();
            int filesCount = files.size();
            ApplicationManager.getApplication().runReadAction(new ThrowableComputable<Boolean, Exception>() {
              @Override
              public Boolean compute() throws Exception {
                new ParserGenerator(file, sourcePath, genDir.getPath()) {
                  @Override
                  protected PrintWriter openOutputInner(File file) throws IOException {
                    files.add(file);
                    return super.openOutputInner(file);
                  }
                }.generate();
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
            Notifications.Bus.notify(new Notification(
              BnfConstants.GENERATION_GROUP,
              String.format("%s generated (%s)", file.getName(), StringUtil.formatFileSize(written)),
              "to " + genDir + (duration == null ? "" : " in " + duration), NotificationType.INFORMATION), project);
          }
          catch (ProcessCanceledException ignored) {
          }
          catch (Exception ex) {
            Notifications.Bus.notify(new Notification(
              BnfConstants.GENERATION_GROUP,
              file.getName() + " generation failed",
              ExceptionUtil.getUserStackTrace(ex, ParserGenerator.LOG), NotificationType.ERROR), project);
            LOG.warn(ex);
          }
        }

      }
    });
  }
}
