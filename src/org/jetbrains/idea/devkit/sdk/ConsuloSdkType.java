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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.swing.Icon;

import org.jdom.Document;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.devkit.DevKitBundle;
import com.intellij.icons.AllIcons;
import com.intellij.ide.highlighter.JarArchiveFileType;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.PathManager;
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
import consulo.lombok.annotations.Logger;
import consulo.roots.types.BinariesOrderRootType;
import consulo.vfs.ArchiveFileSystem;
import consulo.vfs.util.ArchiveVfsUtil;

/**
 * User: anna
 * Date: Nov 22, 2004
 */
@Logger
public class ConsuloSdkType extends SdkType
{
	@NonNls
	private static final String LIB_DIR_NAME = "lib";
	@NonNls
	private static final String PLUGINS_DIR = "plugins";

	public ConsuloSdkType()
	{
		super(DevKitBundle.message("sdk.title"));
	}

	@NotNull
	private static VirtualFile[] getLibraries(VirtualFile home)
	{
		String selectSdkHome = selectBuild(home.getPath());

		String plugins = selectSdkHome + File.separator + PLUGINS_DIR + File.separator;
		List<VirtualFile> result = new ArrayList<>();
		appendLibraries(selectSdkHome, result, "junit.jar");
		appendLibraries(plugins + "core", result);
		return VfsUtilCore.toVirtualFileArray(result);
	}

	private static void appendLibraries(final String libDirPath, final List<VirtualFile> result, @NonNls final String... forbidden)
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
					@NonNls String name = jar.getName();
					if(jar.isFile() && Arrays.binarySearch(forbidden, name) < 0 && (name.endsWith(".jar") || name.endsWith(".zip")))
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

	@NotNull
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

	@NotNull
	@Override
	public String getHelpTopic()
	{
		return "reference.project.structure.sdk.idea";
	}

	@NotNull
	@Override
	public Collection<String> suggestHomePaths()
	{
		return Collections.singletonList(PathManager.getDistributionDirectory().getPath());
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
	public static String selectBuild(@NotNull String sdkHome)
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
		File targetJar = new File(sdkHome, "/lib/resources.jar");

		if(!targetJar.exists())
		{
			targetJar = new File(sdkHome, "/lib/consulo-resources.jar");
		}

		if(!targetJar.exists())
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

	@NotNull
	@Override
	public String getPresentableName()
	{
		return DevKitBundle.message("sdk.title");
	}
}
