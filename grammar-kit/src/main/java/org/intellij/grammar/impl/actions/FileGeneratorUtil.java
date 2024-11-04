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

import consulo.application.WriteAction;
import consulo.component.ProcessCanceledException;
import consulo.devkit.grammarKit.impl.BnfNotificationGroup;
import consulo.language.psi.search.FilenameIndex;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.ProjectRootManager;
import consulo.project.Project;
import consulo.project.content.scope.ProjectScopes;
import consulo.project.ui.notification.Notification;
import consulo.project.ui.notification.NotificationType;
import consulo.project.ui.notification.Notifications;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.annotation.Nullable;
import org.intellij.grammar.BnfFileType;
import org.intellij.grammar.config.Options;

import jakarta.annotation.Nonnull;

import java.util.Collection;
import java.util.Collections;

import static consulo.util.collection.ArrayUtil.getFirstElement;

/**
 * @author gregsh
 */
public class FileGeneratorUtil {
    @Nonnull
    public static VirtualFile getTargetDirectoryFor(
        @Nonnull Project project,
        @Nonnull VirtualFile sourceFile,
        @Nullable String targetFile,
        @Nullable String targetPackage,
        boolean returnRoot
    ) {
        boolean hasPackage = StringUtil.isNotEmpty(targetPackage);
        ProjectRootManager rootManager = ProjectRootManager.getInstance(project);
        ProjectFileIndex fileIndex = ProjectFileIndex.getInstance(project);
        Collection<VirtualFile> files = targetFile == null ? Collections.<VirtualFile>emptyList() :
            FilenameIndex.getVirtualFilesByName(project, targetFile,
                ProjectScopes.getAllScope(project)
            );

        VirtualFile existingFile = null;
        for (VirtualFile file : files) {
            String existingFilePackage = fileIndex.getPackageNameByDirectory(file.getParent());
            if (!hasPackage || existingFilePackage == null || targetPackage.equals(existingFilePackage)) {
                existingFile = file;
                break;
            }
        }

        VirtualFile existingFileRoot =
            existingFile == null ? null
                : fileIndex.isInSourceContent(existingFile) ? fileIndex.getSourceRootForFile(existingFile)
                : fileIndex.isInContent(existingFile) ? fileIndex.getContentRootForFile(existingFile) : null;

        boolean preferGenRoot = sourceFile.getFileType() == BnfFileType.INSTANCE;
        boolean preferSourceRoot = hasPackage && !preferGenRoot;
        VirtualFile[] sourceRoots = rootManager.getContentSourceRoots();
        VirtualFile[] contentRoots = rootManager.getContentRoots();
        final VirtualFile virtualRoot = existingFileRoot != null ? existingFileRoot
            : preferSourceRoot && fileIndex.isInSource(sourceFile) ? fileIndex.getSourceRootForFile(sourceFile)
            : fileIndex.isInContent(sourceFile) ? fileIndex.getContentRootForFile(sourceFile)
            : getFirstElement(preferSourceRoot && sourceRoots.length > 0 ? sourceRoots : contentRoots);
        if (virtualRoot == null) {
            fail(project, sourceFile, "Unable to guess target source root");
            throw new ProcessCanceledException();
        }
        try {
            String genDirName = Options.GEN_DIR.get();
            boolean newGenRoot = !fileIndex.isInSourceContent(virtualRoot);
            final String relativePath = (
                hasPackage && newGenRoot ? genDirName + "/" + targetPackage
                    : hasPackage ? targetPackage
                    : newGenRoot ? genDirName : ""
            ).replace('.', '/');
            if (relativePath.isEmpty()) {
                return virtualRoot;
            }
            else {
                VirtualFile result = WriteAction.compute(() -> VirtualFileUtil.createDirectoryIfMissing(virtualRoot, relativePath));
                VirtualFileUtil.markDirtyAndRefresh(false, true, true, result);
                return returnRoot && newGenRoot ? ObjectUtil.assertNotNull(virtualRoot.findChild(genDirName))
                    : returnRoot ? virtualRoot : result;
            }
        }
        catch (ProcessCanceledException ex) {
            throw ex;
        }
        catch (Exception ex) {
            fail(project, sourceFile, ex.getMessage());
            throw new ProcessCanceledException();
        }
    }

    static void fail(@Nonnull Project project, @Nonnull VirtualFile sourceFile, @Nonnull String message) {
        fail(project, sourceFile.getName(), message);
    }

    static void fail(@Nonnull Project project, @Nonnull String title, @Nonnull String message) {
        Notifications.Bus.notify(
            new Notification(
                BnfNotificationGroup.GRAMMAR_KIT,
                title, message,
                NotificationType.ERROR
            ),
            project
        );
        throw new ProcessCanceledException();
    }
}
