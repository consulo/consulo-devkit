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

import java.util.List;

import org.jetbrains.annotations.NotNull;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import consulo.annotations.RequiredDispatchThread;
import consulo.devkit.util.PluginModuleUtil;

/**
 * @author VISTALL
 * @since 28-Jan-17
 */
public class InternalGroup extends DefaultActionGroup
{
	public InternalGroup()
	{
	}

	public InternalGroup(@NotNull AnAction... actions)
	{
		super(actions);
	}

	public InternalGroup(@NotNull List<? extends AnAction> actions)
	{
		super(actions);
	}

	public InternalGroup(@NotNull String name, @NotNull List<? extends AnAction> actions)
	{
		super(name, actions);
	}

	public InternalGroup(String shortName, boolean popup)
	{
		super(shortName, popup);
	}

	@RequiredDispatchThread
	@Override
	public void update(@NotNull AnActionEvent e)
	{
		super.update(e);

		e.getPresentation().setVisible(PluginModuleUtil.isConsuloOrPluginProject(e.getRequiredData(CommonDataKeys.PROJECT), null));
	}
}
