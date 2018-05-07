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
package org.jetbrains.idea.devkit.sdk;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.swing.Icon;

import org.jdom.Document;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.jetbrains.idea.devkit.DevKitBundle;
import com.intellij.icons.AllIcons;
import com.intellij.ide.highlighter.JarArchiveFileType;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkModificator;
import com.intellij.openapi.projectRoots.SdkType;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ArrayUtil;
import com.intellij.util.containers.ContainerUtil;
import consulo.roots.types.BinariesOrderRootType;
import consulo.vfs.ArchiveFileSystem;
import consulo.vfs.util.ArchiveVfsUtil;

/**
 * User: anna
 * Date: Nov 22, 2004
 */
public class ConsuloSdkType extends SdkType
{
	private static final Logger LOGGER = Logger.getInstance(ConsuloSdkType.class);

	@NonNls
	private static final String LIB_DIR_NAME = "lib";
	@NonNls
	private static final String PLUGINS_DIR = "plugins";

	public ConsuloSdkType()
	{
		super(DevKitBundle.message("sdk.title"));
	}

	@Nonnull
	private static VirtualFile[] getLibraries(VirtualFile home)
	{
		String selectSdkHome = selectBuild(home.getPath());

		String plugins = selectSdkHome + File.separator + PLUGINS_DIR + File.separator;
		List<VirtualFile> result = new ArrayList<>();
		appendLibraries(selectSdkHome, result);
		appendLibraries(plugins + "core", result);
		appendLibraries(plugins + "platform-independent", result);
		return VfsUtilCore.toVirtualFileArray(result);
	}

	private static void appendLibraries(final String libDirPath, final List<VirtualFile> result)
	{
		final String path = libDirPath + File.separator + LIB_DIR_NAME;
		ArchiveFileSystem fileSystem = JarArchiveFileType.INSTANCE.getFileSystem();
		final File lib = new File(path);
		if(lib.isDirectory())
		{
			File[] jars = lib.listFiles();
			if(jars != null)
			{
				for(File jar : jars)
				{
					String name = jar.getName();
					if(name.endsWith(".jar") || name.endsWith(".zip"))
					{
						VirtualFile virtualFile = fileSystem.findLocalVirtualFileByPath(jar.getPath());
						if(virtualFile == null)
						{
							continue;
						}
						result.add(virtualFile);
					}
				}
			}
		}
	}

	@Nonnull
	public static ConsuloSdkType getInstance()
	{
		return EP_NAME.findExtension(ConsuloSdkType.class);
	}

	@Override
	public Icon getIcon()
	{
		return AllIcons.Icon16;
	}

	@Nullable
	@Override
	public Icon getGroupIcon()
	{
		return AllIcons.Icon16;
	}

	@Nonnull
	@Override
	public String getHelpTopic()
	{
		return "reference.project.structure.sdk.idea";
	}

	@Nonnull
	@Override
	public Collection<String> suggestHomePaths()
	{
		return Collections.singletonList(PathManager.getAppHomeDirectory().getPath());
	}

	@Override
	public boolean canCreatePredefinedSdks()
	{
		return true;
	}

	@Override
	public boolean isValidSdkHome(String path)
	{
		return getBuildNumber(path) != null;
	}

	@Nullable
	@Override
	public String getVersionString(String sdkHome)
	{
		return getBuildNumber(sdkHome);
	}

	@Nullable
	public static String selectBuild(@Nonnull String sdkHome)
	{
		File platformDirectory = new File(sdkHome, "platform");
		if(!platformDirectory.exists())
		{
			String oldSdkNumber = getBuildNumberImpl(new File(sdkHome));
			if(oldSdkNumber != null)
			{
				return sdkHome;
			}

			return null;
		}

		String[] child = platformDirectory.list();
		if(child.length == 0)
		{
			return null;
		}

		ContainerUtil.sort(child);

		return new File(platformDirectory, ArrayUtil.getLastElement(child)).getPath();
	}

	@Nullable
	private static String getBuildNumber(String sdkHome)
	{
		String home = selectBuild(sdkHome);
		if(home == null)
		{
			return null;
		}
		return getBuildNumberImpl(new File(home));
	}

	@Nullable
	private static String getBuildNumberImpl(File sdkHome)
	{
		// 1.0 resource path
		// pre-maven resource path
		// maven resource path
		String[] resourceFiles = new String[] {"/lib/resources.jar", "/lib/consulo-resources.jar", "/lib/consulo-platform-resources.jar"};
		File targetJar = null;

		for(String file : resourceFiles)
		{
			File temp = new File(sdkHome, file);
			if(temp.exists())
			{
				targetJar = temp;
			}
		}

		if(targetJar == null)
		{
			return null;
		}

		VirtualFile ideaJarFile = LocalFileSystem.getInstance().findFileByIoFile(targetJar);
		if(ideaJarFile == null)
		{
			return null;
		}

		VirtualFile ideaJarRoot = ArchiveVfsUtil.getArchiveRootForLocalFile(ideaJarFile);
		if(ideaJarRoot == null)
		{
			return null;
		}

		VirtualFile appInfo = ideaJarRoot.findFileByRelativePath(ApplicationInfo.APPLICATION_INFO_XML);
		if(appInfo == null)
		{
			return null;
		}

		try
		{
			Document document = JDOMUtil.loadDocument(appInfo.getInputStream());
			Element build = document.getRootElement().getChild("build");
			return build.getAttributeValue("number");
		}
		catch(Exception e)
		{
			LOGGER.error(e);
		}
		return null;
	}

	@Override
	public String suggestSdkName(String currentSdkName, String sdkHome)
	{
		String buildNumber = getBuildNumber(sdkHome);
		return "Consulo " + (buildNumber != null ? buildNumber : "");
	}

	@Override
	public void setupSdkPaths(Sdk sdk)
	{
		VirtualFile homeDirectory = sdk.getHomeDirectory();
		if(homeDirectory == null)
		{
			return;
		}

		final SdkModificator sdkModificator = sdk.getSdkModificator();

		for(VirtualFile virtualFile : getLibraries(homeDirectory))
		{
			sdkModificator.addRoot(virtualFile, BinariesOrderRootType.getInstance());
		}

		sdkModificator.commitChanges();
	}

	@Override
	public boolean isRootTypeApplicable(OrderRootType type)
	{
		return JavaSdk.getInstance().isRootTypeApplicable(type);
	}

	@Nonnull
	@Override
	public String getPresentableName()
	{
		return DevKitBundle.message("sdk.title");
	}
}
