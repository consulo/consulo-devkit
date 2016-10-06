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

package consulo.devkit.run;

import org.jdom.Element;
import org.jetbrains.annotations.Nullable;
import com.intellij.openapi.util.NotNullFactory;
import consulo.util.pointers.Named;
import consulo.util.pointers.NamedPointer;
import consulo.util.pointers.NamedPointerManager;

/**
 * @author VISTALL
 * @since 29.05.14
 */
public class PluginRunXmlConfigurationUtil
{
	private static final String NAME = "name";

	@Nullable
	public static <T extends Named> NamedPointer<T> readPointer(String childName, Element parent, NotNullFactory<NamedPointerManager<T>> fun)
	{
		final NamedPointerManager<T> namedPointerManager = fun.create();

		Element child = parent.getChild(childName);
		if(child != null)
		{
			final String attributeValue = child.getAttributeValue(NAME);
			if(attributeValue != null)
			{
				return namedPointerManager.create(attributeValue);
			}
		}
		return null;
	}

	public static void writePointer(String childName, Element parent, NamedPointer<?> pointer)
	{
		if(pointer == null)
		{
			return;
		}
		Element element = new Element(childName);
		element.setAttribute(NAME, pointer.getName());

		parent.addContent(element);
	}
}
