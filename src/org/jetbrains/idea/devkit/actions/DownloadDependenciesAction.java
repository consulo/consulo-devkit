/*
 * Copyright 2013-2014 must-be.org
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.consulo.lombok.annotations.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jetbrains.annotations.NotNull;
import com.intellij.ide.plugins.RepositoryHelper;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.ex.ApplicationInfoEx;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.updateSettings.impl.UpdateChecker;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.platform.templates.github.DownloadUtil;
import com.intellij.util.containers.MultiMap;
import com.intellij.util.io.ZipUtil;
import com.intellij.util.net.HttpConfigurable;

/**
 * @author VISTALL
 * @since 16.02.14
 */
@Logger
public class DownloadDependenciesAction extends AnAction
{
	@Override
	public void update(AnActionEvent e)
	{
		super.update(e);
		if(e.getPresentation().isVisible())
		{
			Project project = e.getData(PlatformDataKeys.PROJECT);
			VirtualFile virtualFile = e.getData(PlatformDataKeys.VIRTUAL_FILE);
			e.getPresentation().setEnabledAndVisible(virtualFile != null && virtualFile.equals(project.getBaseDir()));
		}
	}

	@Override
	public void actionPerformed(AnActionEvent e)
	{
		Project project = e.getData(PlatformDataKeys.PROJECT);
		if(project == null)
		{
			return;
		}

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
					HttpURLConnection connection = HttpConfigurable.getInstance().openHttpConnection("http://must-be.org/vulcan/projects.jsp");
					connection.connect();

					InputStream inputStream = null;
					try
					{
						Document document = JDOMUtil.loadDocument(inputStream = connection.getInputStream());

						MultiMap<String, String> map = new MultiMap<String, String>();
						for(Element element : document.getRootElement().getChildren())
						{
							String projectName = element.getChildText("name");
							Element dependencies = element.getChild("dependencies");
							if(dependencies != null)
							{
								for(Element dependencyElement : dependencies.getChildren())
								{
									String textTrim = dependencyElement.getTextTrim();
									if(Comparing.equal(textTrim, "consulo"))
									{
										continue;
									}
									map.putValue(projectName, textTrim);
								}
							}
						}

						inputStream.close();

						String projectName = getProject().getName();
						if(!map.containsKey(projectName))
						{
							return;
						}
						progressIndicator.setText("Downloading plugin list...");
						connection = HttpConfigurable.getInstance().openHttpConnection(ApplicationInfoEx.getInstanceEx().getPluginsListUrl());
						connection.connect();

						MultiMap<String, String> buildProjectToId = new MultiMap<String, String>();
						document = JDOMUtil.loadDocument(inputStream = connection.getInputStream());

						for(Element categoryElement : document.getRootElement().getChildren())
						{
							for(Element ideaPluginElement : categoryElement.getChildren())
							{
								String idText = ideaPluginElement.getChildText("id");
								String buildProjectIdText = ideaPluginElement.getChildText("build-project");
								if(StringUtil.isEmpty(idText) || StringUtil.isEmpty(buildProjectIdText))
								{
									continue;
								}
								buildProjectToId.putValue(buildProjectIdText, idText);
							}
						}

						Set<String> toDownloadIds = new HashSet<String>();
						collectDependencies(projectName, map, toDownloadIds, buildProjectToId);

						String uuid = UpdateChecker.getInstallationUID(PropertiesComponent.getInstance());

						for(String toDownloadId : toDownloadIds)
						{
							String url = RepositoryHelper.getDownloadUrl() + URLEncoder.encode(toDownloadId, "UTF8") +
									"&build=SNAPSHOT&uuid=" + URLEncoder.encode(uuid, "UTF8");

							File targetFileToDownload = FileUtil.createTempFile("download_target", ".zip");
							File tempTargetFileToDownload = FileUtil.createTempFile("temp_download_target", ".zip");

							progressIndicator.setText("Downloading plugin: " + toDownloadId);
							DownloadUtil.downloadAtomically(progressIndicator, url, targetFileToDownload, tempTargetFileToDownload);

							progressIndicator.setText("Extracting plugin: " + toDownloadId);
							ZipUtil.extract(targetFileToDownload, depDir, null);
						}

						LocalFileSystem.getInstance().refreshIoFiles(Collections.singletonList(depDir), false, true, null);
					}
					catch(JDOMException e)
					{
						LOGGER.warn(e);
					}
					finally
					{
						try
						{
							if(inputStream != null)
							{
								inputStream.close();
							}
						}
						catch(IOException e1)
						{
							//
						}
					}
				}
				catch(IOException e)
				{
					LOGGER.warn(e);
				}
			}
		}.queue();
	}

	private static void collectDependencies(
			String projectName, MultiMap<String, String> dependenciesInBuild, Collection<String> make, MultiMap<String, String> buildProjectToId)
	{
		Collection<String> dependencies = dependenciesInBuild.get(projectName);
		for(String dependency : dependencies)
		{
			for(String id : buildProjectToId.get(dependency))
			{
				make.add(id);
			}

			collectDependencies(dependency, dependenciesInBuild, make, buildProjectToId);
		}
	}
}
