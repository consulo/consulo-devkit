package org.jetbrains.idea.devkit.dom;

import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.Required;

/**
 * @author VISTALL
 * @since 2019-03-22
 */
public interface ImportFile extends DomElement
{
	@Required
	GenericAttributeValue<String> getPath();
}
