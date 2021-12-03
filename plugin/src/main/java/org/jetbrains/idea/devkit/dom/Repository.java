package org.jetbrains.idea.devkit.dom;

import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.GenericAttributeValue;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 04/12/2021
 */
public interface Repository extends DomElement
{
	/**
	 * Returns the value of the url child.
	 * Attribute url
	 *
	 * @return the value of the url child.
	 */
	@Nonnull
	GenericAttributeValue<String> getUrl();
}
