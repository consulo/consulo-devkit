/*
 * Copyright 2013-2016 consulo.io
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

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.jetbrains.annotations.NotNull;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.RepositoryHelper;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkTable;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.impl.libraries.ProjectLibraryTable;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.io.DownloadUtil;
import com.intellij.util.io.ZipUtil;
import consulo.annotations.RequiredDispatchThread;
import consulo.devkit.module.library.ConsuloPluginLibraryType;
import consulo.ide.updateSettings.UpdateChannel;
import consulo.ide.webService.WebServiceApi;
import consulo.lombok.annotations.Logger;
import consulo.roots.types.BinariesOrderRootType;
import consulo.vfs.util.ArchiveVfsUtil;

/**
 * @author VISTALL
 * @since 16.02.14
 */
@Logger
public class DownloadDependenciesAction extends AnAction
{
	private static NotificationGroup ourNotificationGroup = new NotificationGroup("consulo-dev-plugin", NotificationDisplayType.BALLOON, true);

	@RequiredDispatchThread
	@Override
	public void update(@NotNull AnActionEvent e)
	{
		super.update(e);
		if(e.getPresentation().isVisible())
		{
			Project project = e.getData(PlatformDataKeys.PROJECT);
			VirtualFile virtualFile = e.getData(PlatformDataKeys.VIRTUAL_FILE);
			e.getPresentation().setEnabledAndVisible(virtualFile != null && virtualFile.equals(project.getBaseDir()));
		}
	}

	@RequiredDispatchThread
	@Override
	public void actionPerformed(@NotNull AnActionEvent e)
	{
		Project project = e.getData(PlatformDataKeys.PROJECT);
		if(project == null)
		{
			return;
		}

		Library[] libraries = ProjectLibraryTable.getInstance(project).getLibraries();
		Map<PluginId, Library> librariesByPluginId = new HashMap<>();
		for(Library library : libraries)
		{
			String name = library.getName();
			if(name != null && name.startsWith(ConsuloPluginLibraryType.LIBRARY_PREFIX))
			{
				String pluginId = name.substring(ConsuloPluginLibraryType.LIBRARY_PREFIX.length(), name.length());

				librariesByPluginId.put(PluginId.getId(pluginId), library);
			}
		}

		final Sdk sdk = SdkTable.getInstance().findSdk("Consulo SNAPSHOT");

		final String consuloVersion = sdk != null ? StringUtil.notNullize(sdk.getVersionString(), "SNAPSHOT") : "SNAPSHOT";

		final File depDir = new File(project.getBasePath(), "dep");
		depDir.mkdirs();

		new Task.Backgroundable(project, "Downloading build project info...")
		{
			@Override
			public void run(@NotNull ProgressIndicator progressIndicator)
			{
				FileUtil.delete(depDir);
				progressIndicator.setText("Downloading plugin list...");
				List<IdeaPluginDescriptor> plugins;
				try
				{
					plugins = RepositoryHelper.loadPluginsFromRepository(progressIndicator, UpdateChannel.nightly);
				}
				catch(Exception e)
				{
					ourNotificationGroup.createNotification("Plugin repository is down", NotificationType.ERROR).notify(project);
					LOGGER.warn(e);
					return;
				}

				progressIndicator.setText("Downloaded plugin list [" + plugins.size() + "]");

				Set<PluginId> deepDependencies = new TreeSet<>();

				progressIndicator.setText("Collected original dependencies: " + librariesByPluginId.keySet());

				for(PluginId pluginId : librariesByPluginId.keySet())
				{
					collectDependencies(pluginId, deepDependencies, plugins);
				}

				progressIndicator.setText("Collected deep dependencies: " + deepDependencies);

				// use cold id for don't store statistics
				String uuid = "cold";

				for(PluginId deepDependency : deepDependencies)
				{
					try
					{
						String downloadUrl = WebServiceApi.PLUGINS_API.buildUrl("download") + "?channel=nightly&platformVersion=" + consuloVersion + "&pluginId=" + URLEncoder.encode(deepDependency
								.getIdString(), "UTF-8") + "&id=" + uuid;

						File targetFileToDownload = FileUtil.createTempFile("download_target", ".zip");
						File tempTargetFileToDownload = FileUtil.createTempFile("temp_download_target", ".zip");

						progressIndicator.setText("Downloading plugin: " + deepDependency);
						DownloadUtil.downloadAtomically(progressIndicator, downloadUrl, targetFileToDownload, tempTargetFileToDownload);

						progressIndicator.setText("Extracting plugin: " + deepDependency);
						ZipUtil.extract(targetFileToDownload, depDir, null);
					}
					catch(IOException e)
					{
						ourNotificationGroup.createNotification("Failed to download plugin: " + deepDependency, NotificationType.WARNING).notify(project);

						LOGGER.warn(e);
					}
				}

				progressIndicator.setText("Updating libraries");
				VirtualFile depVirtualDir = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(depDir);
				if(depVirtualDir == null)
				{
					return;
				}
				depVirtualDir.refresh(false, true);

				for(Map.Entry<PluginId, Library> entry : librariesByPluginId.entrySet())
				{
					VirtualFile libDir = depVirtualDir.findFileByRelativePath(entry.getKey() + "/lib");
					if(libDir == null)
					{
						continue;
					}

					Library library = entry.getValue();

					final Library.ModifiableModel modifiableModel = library.getModifiableModel();
					for(OrderRootType orderRootType : OrderRootType.getAllTypes())
					{
						String[] urls = modifiableModel.getUrls(orderRootType);
						for(String url : urls)
						{
							modifiableModel.removeRoot(url, orderRootType);
						}
					}

					for(VirtualFile file : libDir.getChildren())
					{
						if(file.isDirectory())
						{
							continue;
						}
						VirtualFile localVirtualFileByPath = ArchiveVfsUtil.getArchiveRootForLocalFile(file);
						if(localVirtualFileByPath == null)
						{
							continue;
						}
						modifiableModel.addRoot(localVirtualFileByPath, BinariesOrderRootType.getInstance());
					}

					WriteCommandAction.runWriteCommandAction(project, modifiableModel::commit);
				}
			}
		}.queue();
	}

	private void collectDependencies(PluginId pluginId, Set<PluginId> deps, List<IdeaPluginDescriptor> plugins)
	{
		if(!deps.add(pluginId))
		{
			return;
		}

		for(IdeaPluginDescriptor plugin : plugins)
		{
			if(pluginId.equals(plugin.getPluginId()))
			{
				for(PluginId dependencyId : plugin.getDependentPluginIds())
				{
					collectDependencies(dependencyId, deps, plugins);
				}
			}
		}
	}
}
