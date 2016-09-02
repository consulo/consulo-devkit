/*
 * Copyright 2013-2016 must-be.org
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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.jetbrains.annotations.NotNull;
import com.google.gson.Gson;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkTable;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.impl.libraries.ProjectLibraryTable;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.updateSettings.impl.UpdateChecker;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ArrayUtil;
import com.intellij.util.io.DownloadUtil;
import com.intellij.util.io.ZipUtil;
import consulo.annotations.RequiredDispatchThread;
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
	public static class PluginJson
	{
		public String id;
		public String[] dependencies = ArrayUtil.EMPTY_STRING_ARRAY;
	}

	private static final String ourLibraryPrefix = "consulo-plugin: ";
	private static final String ourDefaultPluginHost = "http://must-be.org/api/v2/consulo/plugins/";

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
		Map<String, Library> librariesByPluginId = new HashMap<>();
		for(Library library : libraries)
		{
			String name = library.getName();
			if(name != null && name.startsWith(ourLibraryPrefix))
			{
				String pluginId = name.substring(ourLibraryPrefix.length(), name.length());

				librariesByPluginId.put(pluginId, library);
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
				try
				{
					progressIndicator.setText("Downloading plugin list...");
					PluginJson[] plugins;
					try
					{
						InputStream inputStream = new URL(ourDefaultPluginHost + "list?channel=nightly&platformVersion=" + consuloVersion).openStream();

						plugins = new Gson().fromJson(new InputStreamReader(inputStream, StandardCharsets.UTF_8), PluginJson[].class);
					}
					catch(IOException e)
					{
						LOGGER.warn(e);
						return;
					}

					progressIndicator.setText("Downloaded plugin list [" + plugins.length + "]");

					Set<String> deepDependencies = new TreeSet<>();

					progressIndicator.setText("Collected original dependencies: " + librariesByPluginId.keySet());

					for(String pluginId : librariesByPluginId.keySet())
					{
						collectDependencies(pluginId, deepDependencies, plugins);
					}

					progressIndicator.setText("Collected deep dependencies: " + deepDependencies);

					String uuid = UpdateChecker.getInstallationUID(PropertiesComponent.getInstance());

					for(String deepDependency : deepDependencies)
					{
						String downloadUrl = ourDefaultPluginHost + "download?channel=nightly&platformVersion=" + consuloVersion + "&pluginId=" + URLEncoder.encode(deepDependency,
								"UTF-8") + "&id=" + URLEncoder.encode(uuid, "UTF-8");

						File targetFileToDownload = FileUtil.createTempFile("download_target", ".zip");
						File tempTargetFileToDownload = FileUtil.createTempFile("temp_download_target", ".zip");

						progressIndicator.setText("Downloading plugin: " + deepDependency);
						DownloadUtil.downloadAtomically(progressIndicator, downloadUrl, targetFileToDownload, tempTargetFileToDownload);

						progressIndicator.setText("Extracting plugin: " + deepDependency);
						ZipUtil.extract(targetFileToDownload, depDir, null);
					}
				}
				catch(IOException e)
				{
					LOGGER.warn(e);
					return;
				}

				progressIndicator.setText("Updating libraries");
				VirtualFile depVirtualDir = LocalFileSystem.getInstance().refreshAndFindFileByPath(depDir.getPath());
				if(depVirtualDir == null)
				{
					return;
				}

				for(Map.Entry<String, Library> entry : librariesByPluginId.entrySet())
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

	private void collectDependencies(String pluginId, Set<String> deps, PluginJson[] plugins)
	{
		if(!deps.add(pluginId))
		{
			return;
		}

		for(PluginJson plugin : plugins)
		{
			if(pluginId.equals(plugin.id))
			{
				for(String dependencyId : plugin.dependencies)
				{
					collectDependencies(dependencyId, deps, plugins);
				}
			}
		}
	}
}
