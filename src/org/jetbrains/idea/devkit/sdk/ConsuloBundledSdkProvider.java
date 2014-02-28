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

package org.jetbrains.idea.devkit.sdk;

import org.jetbrains.annotations.NotNull;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.projectRoots.BundledSdkProvider;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.impl.SdkImpl;

/**
 * @author VISTALL
 * @since 28.02.14
 */
public class ConsuloBundledSdkProvider implements BundledSdkProvider
{
	@NotNull
	@Override
	public Sdk[] createBundledSdks()
	{
		ConsuloSdkType sdkType = ConsuloSdkType.getInstance();
		String homePath = PathManager.getHomePath();

		SdkImpl sdk = new SdkImpl("Consulo SDK (bundled)", sdkType);
		sdk.setHomePath(homePath);
		sdk.setVersionString(sdkType.getVersionString(sdk));

		sdkType.setupSdkPaths(sdk);

		return new Sdk[]{sdk};
	}
}
