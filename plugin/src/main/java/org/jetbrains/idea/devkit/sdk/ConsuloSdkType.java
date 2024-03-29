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

import com.intellij.java.language.impl.JarArchiveFileType;
import com.intellij.java.language.projectRoots.JavaSdk;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.AllIcons;
import consulo.container.boot.ContainerPathManager;
import consulo.content.OrderRootType;
import consulo.content.base.BinariesOrderRootType;
import consulo.content.bundle.Sdk;
import consulo.content.bundle.SdkModificator;
import consulo.content.bundle.SdkType;
import consulo.logging.Logger;
import consulo.ui.image.Image;
import consulo.util.collection.ArrayUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.archive.ArchiveFileSystem;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.idea.devkit.DevKitBundle;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

/**
 * User: anna
 * Date: Nov 22, 2004
 */
@ExtensionImpl
public class ConsuloSdkType extends SdkType {
  private static final Logger LOG = Logger.getInstance(ConsuloSdkType.class);

  @NonNls
  private static final String LIB_DIR_NAME = "lib";

  public ConsuloSdkType() {
    super(DevKitBundle.message("sdk.title"));
  }

  @Nonnull
  private static VirtualFile[] getLibraries(VirtualFile home) {
    String selectSdkHome = selectBuild(home.getPath());

    String plugins = selectSdkHome + File.separator + "modules" + File.separator;
    List<VirtualFile> result = new ArrayList<>();
    appendLibraries(selectSdkHome, result);
    appendLibraries(plugins + "consulo.platform.base", result);
    appendLibraries(plugins + "consulo.platform.desktop", result);
    return VirtualFileUtil.toVirtualFileArray(result);
  }

  private static void appendLibraries(final String libDirPath, final List<VirtualFile> result) {
    final String path = libDirPath + File.separator + LIB_DIR_NAME;
    ArchiveFileSystem fileSystem = JarArchiveFileType.INSTANCE.getFileSystem();
    final File lib = new File(path);
    if (lib.isDirectory()) {
      File[] jars = lib.listFiles();
      if (jars != null) {
        for (File jar : jars) {
          String name = jar.getName();
          if (name.endsWith(".jar") || name.endsWith(".zip")) {
            VirtualFile virtualFile = fileSystem.findLocalVirtualFileByPath(jar.getPath());
            if (virtualFile == null) {
              continue;
            }
            result.add(virtualFile);
          }
        }
      }
    }
  }

  @Nonnull
  public static ConsuloSdkType getInstance() {
    return EP_NAME.findExtensionOrFail(ConsuloSdkType.class);
  }

  @Override
  public Image getIcon() {
    return AllIcons.Icon16;
  }

  @Nonnull
  @Override
  public String getHelpTopic() {
    return "reference.project.structure.sdk.idea";
  }

  @Nonnull
  @Override
  public Collection<String> suggestHomePaths() {
    return Collections.singletonList(ContainerPathManager.get().getAppHomeDirectory().getPath());
  }

  @Override
  public boolean canCreatePredefinedSdks() {
    return true;
  }

  @Override
  public boolean isValidSdkHome(String path) {
    return getBuildNumber(path) != null;
  }

  @Nullable
  @Override
  public String getVersionString(String sdkHome) {
    return getBuildNumber(sdkHome);
  }

  @Nullable
  public static String selectBuild(@Nonnull String sdkHome) {
    File platformDirectory = new File(sdkHome, "platform");
    if (!platformDirectory.exists()) {
      String oldSdkNumber = getBuildNumberImpl(new File(sdkHome));
      if (oldSdkNumber != null) {
        return sdkHome;
      }

      return null;
    }

    String[] child = platformDirectory.list();
    if (child.length == 0) {
      return null;
    }

    Arrays.sort(child);

    return new File(platformDirectory, ArrayUtil.getLastElement(child)).getPath();
  }

  @Nullable
  private static String getBuildNumber(String sdkHome) {
    String home = selectBuild(sdkHome);
    if (home == null) {
      return null;
    }
    return getBuildNumberImpl(new File(home));
  }

  @Nullable
  private static String getBuildNumberImpl(File sdkHome) {
    if (!sdkHome.exists()) {
      return null;
    }

    File bootstrapJar = new File(sdkHome, "boot/consulo-bootstrap.jar");
    try {
      JarFile jarFile = new JarFile(bootstrapJar);
      Attributes mainAttributes = jarFile.getManifest().getMainAttributes();

      String number = mainAttributes.getValue("Consulo-Build-Number");
      if (number != null) {
        return number;
      }
    }
    catch (Exception e) {
      LOG.error(e);
    }
    return null;
  }

  @Override
  public String suggestSdkName(String currentSdkName, String sdkHome) {
    String buildNumber = getBuildNumber(sdkHome);
    return "Consulo " + (buildNumber != null ? buildNumber : "");
  }

  @Override
  public void setupSdkPaths(Sdk sdk) {
    VirtualFile homeDirectory = sdk.getHomeDirectory();
    if (homeDirectory == null) {
      return;
    }

    final SdkModificator sdkModificator = sdk.getSdkModificator();

    for (VirtualFile virtualFile : getLibraries(homeDirectory)) {
      sdkModificator.addRoot(virtualFile, BinariesOrderRootType.getInstance());
    }

    sdkModificator.commitChanges();
  }

  @Override
  public boolean isRootTypeApplicable(OrderRootType type) {
    return JavaSdk.getInstance().isRootTypeApplicable(type);
  }

  @Nonnull
  @Override
  public String getPresentableName() {
    return DevKitBundle.message("sdk.title");
  }
}
