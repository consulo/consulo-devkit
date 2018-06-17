/*
 * Copyright 2011-present Greg Shrago
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
package org.intellij.grammar;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.IconLoader;
import consulo.ui.image.Image;

/**
 * @author gregory
 *         Date: 17.07.11 2:55
 */
public interface BnfIcons
{
	Image FILE = IconLoader.getIcon("/resources/file.png");

	Image RULE = AllIcons.Nodes.Method;
	Image EXTERNAL_RULE = AllIcons.Nodes.AbstractMethod;
	Image ATTRIBUTE = AllIcons.Nodes.Attribute;

	Image RELATED_METHOD = IconLoader.getIcon("/gutter/implementedMethod.png");
}
