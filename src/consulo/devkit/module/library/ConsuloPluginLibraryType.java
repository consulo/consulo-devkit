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

package consulo.devkit.module.library;

import consulo.annotations.DeprecationInfo;

/**
 * @author VISTALL
 * @since 29-Sep-16
 */
@Deprecated
@DeprecationInfo("After full migration to maven, old plugin dependencies load will be dropped")
public class ConsuloPluginLibraryType
{
	public static final String LIBRARY_PREFIX = "consulo-plugin: ";
	public static final String DEP_LIBRARY = "dep";
}
