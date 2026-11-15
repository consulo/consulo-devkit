/*
 * Copyright 2013-2017 consulo.io
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

package consulo.devkit.action;

import consulo.devkit.module.extension.PluginModuleExtension;
import consulo.devkit.util.PluginModuleUtil;
import consulo.module.extension.ModuleExtensionHelper;
import consulo.project.Project;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithAsyncUpdate;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.Presentation;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.step.CodeExecution;

/**
 * @author VISTALL
 * @since 2017-01-28
 */
public class InternalGroup extends DefaultActionGroup implements AnActionWithAsyncUpdate {
    @Override
    public Coroutine<?, ?> updateAsync(AnActionEvent e) {
        // PluginModuleUtil.isConsuloOrPluginProject(project, null) without a module is the project name and the module
        // extensions only: nothing needs the read lock, so the group is never disabled by a running write action
        return Coroutine.first(CodeExecution.<Object, Object>apply(input -> {
            Presentation presentation = e.getPresentation();
            Project project = e.getData(Project.KEY);
            presentation.setVisible(project != null && (PluginModuleUtil.isConsuloProject(project)
                || ModuleExtensionHelper.getInstance(project).hasModuleExtension(PluginModuleExtension.class)));
            return input;
        }));
    }
}
