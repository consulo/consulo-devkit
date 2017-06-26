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

import javax.swing.Icon;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import consulo.annotations.RequiredDispatchThread;
import consulo.devkit.util.PluginModuleUtil;

/**
 * @author VISTALL
 * @since 28-Jan-17
 */
public abstract class InternalAction extends AnAction
{
	protected InternalAction()
	{
	}

	protected InternalAction(Icon icon)
	{
		super(icon);
	}

	protected InternalAction(@Nullable String text)
	{
		super(text);
	}

	protected InternalAction(@Nullable String text, @Nullable String description, @Nullable Icon icon)
	{
		super(text, description, icon);
	}

	@RequiredDispatchThread
	@Override
	public void update(@NotNull AnActionEvent e)
	{
		Project project = e.getProject();
		e.getPresentation().setEnabledAndVisible(project != null && PluginModuleUtil.isConsuloOrPluginProject(project, null));
	}
}
