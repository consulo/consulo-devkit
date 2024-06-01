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

import consulo.logging.Logger;
import consulo.util.collection.ArrayUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.Arrays;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

/**
 * User: anna
 * Date: Nov 22, 2004
 */
public class ConsuloSdkType  {
  private static final Logger LOG = Logger.getInstance(ConsuloSdkType.class);

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
}
