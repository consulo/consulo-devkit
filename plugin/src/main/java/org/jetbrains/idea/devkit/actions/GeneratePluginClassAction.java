/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package org.jetbrains.idea.devkit.actions;

import com.intellij.java.language.psi.JavaDirectoryService;
import com.intellij.java.language.psi.PsiClass;
import consulo.dataContext.DataContext;
import consulo.devkit.localize.DevKitLocalize;
import consulo.devkit.module.extension.PluginModuleExtension;
import consulo.devkit.util.PluginModuleUtil;
import consulo.ide.IdeView;
import consulo.ide.action.CreateElementActionBase;
import consulo.language.editor.PlatformDataKeys;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiElement;
import consulo.language.util.IncorrectOperationException;
import consulo.language.util.ModuleUtilCore;
import consulo.localize.LocalizeValue;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.ProjectRootManager;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.ui.image.Image;
import consulo.virtualFileSystem.VirtualFile;
import consulo.xml.psi.xml.XmlFile;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.idea.devkit.util.ChooseModulesDialog;
import org.jetbrains.idea.devkit.util.DescriptorUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;

/**
 * @author yole
 */
public abstract class GeneratePluginClassAction extends CreateElementActionBase implements DescriptorUtil.Patcher {
    protected final Set<XmlFile> myFilesToPatch = new HashSet<>();

    // length == 1 is important to make MyInputValidator close the dialog when
    // module selection is canceled. That's some weird interface actually...
    private static final PsiElement[] CANCELED = new PsiElement[1];

    public GeneratePluginClassAction(@Nonnull LocalizeValue text, @Nonnull LocalizeValue description, @Nullable Image icon) {
        super(text, description, icon);
    }

    protected final void invokeDialog(Project project, PsiDirectory directory, @Nonnull Consumer<PsiElement[]> consumer) {
        try {
            invokeDialogImpl(project, directory, consumer);
        }
        finally {
            myFilesToPatch.clear();
        }
    }

    protected abstract void invokeDialogImpl(Project project, PsiDirectory directory, @Nonnull Consumer<PsiElement[]> consumer);

    private void addPluginModule(Module module) {
        final XmlFile pluginXml = PluginModuleUtil.getPluginXml(module);
        if (pluginXml != null) {
            myFilesToPatch.add(pluginXml);
        }
    }

    @RequiredUIAccess
    @Override
    public void update(final AnActionEvent e) {
        super.update(e);
        final Presentation presentation = e.getPresentation();
        if (presentation.isEnabled()) {
            final DataContext context = e.getDataContext();
            Module module = e.getData(Module.KEY);
            if (!PluginModuleUtil.isPluginModuleOrDependency(module)) {
                presentation.setEnabled(false);
                presentation.setVisible(false);
                return;
            }
            final IdeView view = e.getData(IdeView.KEY);
            final Project project = e.getData(PlatformDataKeys.PROJECT);
            if (view != null && project != null) {
                // from com.intellij.ide.actions.CreateClassAction.update()
                ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(project).getFileIndex();
                PsiDirectory[] dirs = view.getDirectories();
                for (PsiDirectory dir : dirs) {
                    if (projectFileIndex.isInSourceContent(dir.getVirtualFile())
                        && JavaDirectoryService.getInstance().getPackage(dir) != null) {
                        return;
                    }
                }

                presentation.setEnabled(false);
                presentation.setVisible(false);
            }
        }
    }

    @Nullable
    protected static Module getModule(PsiDirectory dir) {
        Project project = dir.getProject();
        final ProjectFileIndex fileIndex = ProjectRootManager.getInstance(project).getFileIndex();

        final VirtualFile vFile = dir.getVirtualFile();
        if (fileIndex.isInLibrarySource(vFile) || fileIndex.isInLibraryClasses(vFile)) {
            final List<OrderEntry> orderEntries = fileIndex.getOrderEntriesForFile(vFile);
            if (orderEntries.isEmpty()) {
                return null;
            }
            Set<Module> modules = new HashSet<>();
            for (OrderEntry orderEntry : orderEntries) {
                modules.add(orderEntry.getOwnerModule());
            }
            final Module[] candidates = modules.toArray(new Module[modules.size()]);
            Arrays.sort(candidates, ModuleManager.getInstance(project).moduleDependencyComparator());
            return candidates[0];
        }
        return fileIndex.getModuleForFile(vFile);
    }

    @Nonnull
    protected PsiElement[] create(String newName, PsiDirectory directory) throws Exception {
        final Project project = directory.getProject();
        final Module module = getModule(directory);

        if (module != null) {
            if (ModuleUtilCore.getExtension(module, PluginModuleExtension.class) != null) {
                addPluginModule(module);
            }
            else {
                final List<Module> candidateModules = PluginModuleUtil.getCandidateModules(module);
                final Iterator<Module> it = candidateModules.iterator();
                while (it.hasNext()) {
                    Module m = it.next();
                    if (PluginModuleUtil.getPluginXml(m) == null) {
                        it.remove();
                    }
                }

                if (candidateModules.size() == 1) {
                    addPluginModule(candidateModules.get(0));
                }
                else {
                    final ChooseModulesDialog dialog =
                        new ChooseModulesDialog(project, candidateModules, getTemplatePresentation().getDescription());
                    dialog.show();
                    if (!dialog.isOK()) {
                        // create() should return CANCELED now
                        return CANCELED;
                    }
                    else {
                        final List<Module> modules = dialog.getSelectedModules();
                        for (Module m : modules) {
                            addPluginModule(m);
                        }
                    }
                }
            }
        }

        if (myFilesToPatch.size() == 0) {
            throw new IncorrectOperationException(DevKitLocalize.errorNoPluginXml().get());
        }
        if (myFilesToPatch.size() == 0) {
            // user canceled module selection
            return CANCELED;
        }

        final PsiClass klass = JavaDirectoryService.getInstance().createClass(directory, newName, getClassTemplateName());

        DescriptorUtil.patchPluginXml(this, klass, myFilesToPatch.toArray(new XmlFile[myFilesToPatch.size()]));

        return new PsiElement[]{klass};
    }

    @NonNls
    protected abstract String getClassTemplateName();
}
