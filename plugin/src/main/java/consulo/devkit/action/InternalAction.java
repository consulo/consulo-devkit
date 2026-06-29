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

import consulo.annotation.access.RequiredReadAction;
import consulo.devkit.util.PluginModuleUtil;
import consulo.module.Module;
import consulo.project.Project;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.coroutine.ActionSafeReadLock;
import consulo.ui.image.Image;
import consulo.util.concurrent.coroutine.Coroutine;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2017-01-28
 */
public abstract class InternalAction extends AnAction {
    protected InternalAction() {
    }

    protected InternalAction(Image icon) {
        super(icon);
    }

    protected InternalAction(@Nullable String text) {
        super(text);
    }

    @Override
    public final Coroutine<?, ?> updateAsync(AnActionEvent e) {
        return ActionSafeReadLock.run(e, presentation -> presentation.setEnabledAndVisible(checkUpdate(e))).toCoroutine();
    }

    @Override
    public final void update(AnActionEvent e) {
        throw new AbstractMethodError();
    }

    @RequiredReadAction
    protected boolean checkUpdate(@Nonnull AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        Module module = e.getData(Module.KEY);

        return project != null && PluginModuleUtil.isConsuloOrPluginProject(project, module);
    }
}
