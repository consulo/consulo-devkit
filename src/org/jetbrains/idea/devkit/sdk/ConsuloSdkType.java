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
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.Icon;

import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.devkit.DevKitBundle;
import com.intellij.icons.AllIcons;
import com.intellij.ide.highlighter.JarArchiveFileType;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.projectRoots.AdditionalDataConfigurable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkAdditionalData;
import com.intellij.openapi.projectRoots.SdkModel;
import com.intellij.openapi.projectRoots.SdkModificator;
import com.intellij.openapi.projectRoots.SdkType;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.ArchiveFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;

/**
 * User: anna
 * Date: Nov 22, 2004
 */
public class ConsuloSdkType extends SdkType
{
	@NonNls
	private static final String LIB_DIR_NAME = "lib";
	@NonNls
	private static final String SRC_DIR_NAME = "src";
	@NonNls
	private static final String PLUGINS_DIR = "plugins";

	public ConsuloSdkType()
	{
		super(DevKitBundle.message("sdk.title"));
	}

	@Nullable
	private static File getMainJar(String home)
	{

		final File libDir = new File(home, LIB_DIR_NAME);
		File f = new File(libDir, "idea.jar");
		if(f.exists())
		{
			return f;
		}

		return null;
	}

	@Nullable
	public static String getBuildNumber(String ideaHome)
	{
		try
		{
			@NonNls final String buildTxt = "/build.txt";
			return FileUtil.loadFile(new File(ideaHome + buildTxt)).trim();
		}
		catch(IOException e)
		{
			return null;
		}
	}

	private static VirtualFile[] getIdeaLibrary(String home)
	{
		String plugins = home + File.separator + PLUGINS_DIR + File.separator;
		ArrayList<VirtualFile> result = new ArrayList<VirtualFile>();
		appendIdeaLibrary(home, result, "junit.jar");
		appendIdeaLibrary(plugins + "core", result);
		return VfsUtilCore.toVirtualFileArray(result);
	}

	private static void appendIdeaLibrary(final String libDirPath, final ArrayList<VirtualFile> result, @NonNls final String... forbidden)
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
						result.add(fileSystem.findLocalVirtualFileByPath(jar.getPath()));
					}
				}
			}
		}
	}

	private static void addSources(File file, SdkModificator sdkModificator)
	{
		final File src = new File(new File(file, LIB_DIR_NAME), SRC_DIR_NAME);
		if(!src.exists())
		{
			return;
		}
		File[] srcs = src.listFiles(new FileFilter()
		{
			public boolean accept(File pathname)
			{
				@NonNls final String path = pathname.getPath();
				//noinspection SimplifiableIfStatement
				if(path.contains("generics"))
				{
					return false;
				}
				return path.endsWith(".jar") || path.endsWith(".zip");
			}
		});
		for(int i = 0; srcs != null && i < srcs.length; i++)
		{
			File jarFile = srcs[i];
			if(jarFile.exists())
			{
				ArchiveFileSystem fileSystem = JarArchiveFileType.INSTANCE.getFileSystem();
				String path = jarFile.getAbsolutePath().replace(File.separatorChar, '/') + ArchiveFileSystem.ARCHIVE_SEPARATOR;
				fileSystem.setNoCopyJarForPath(path);
				VirtualFile vFile = fileSystem.findFileByPath(path);
				sdkModificator.addRoot(vFile, OrderRootType.SOURCES);
			}
		}
	}

	@Nullable
	public static Sdk findIdeaJdk(@Nullable Sdk jdk)
	{
		if(jdk == null)
		{
			return null;
		}
		if(jdk.getSdkType() instanceof ConsuloSdkType)
		{
			return jdk;
		}
		return null;
	}

	public static SdkType getInstance()
	{
		return SdkType.findInstance(ConsuloSdkType.class);
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

	public String suggestHomePath()
	{
		return PathManager.getHomePath().replace(File.separatorChar, '/');
	}

	public boolean isValidSdkHome(String path)
	{
		File home = new File(path);
		if(!home.exists())
		{
			return false;
		}
		if(getBuildNumber(path) == null || getMainJar(path) == null)
		{
			return false;
		}
		return true;
	}

	@Nullable
	@Override
	public String getVersionString(String sdkHome)
	{
		return getBuildNumber(sdkHome);
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
		final SdkModificator sdkModificator = sdk.getSdkModificator();
		final String sdkHome = sdk.getHomePath();

		final VirtualFile[] ideaLib = getIdeaLibrary(sdkHome);
		if(ideaLib != null)
		{
			for(VirtualFile aIdeaLib : ideaLib)
			{
				sdkModificator.addRoot(aIdeaLib, OrderRootType.CLASSES);
			}
		}
		addSources(new File(sdkHome), sdkModificator);

		sdkModificator.commitChanges();
	}

	public AdditionalDataConfigurable createAdditionalDataConfigurable(final SdkModel sdkModel, SdkModificator sdkModificator)
	{
		return null;
	}

	public void saveAdditionalData(SdkAdditionalData additionalData, Element additional)
	{
	}

	@Override
	public String getPresentableName()
	{
		return getName();
	}
}
