/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

// Generated on Wed Nov 07 17:26:02 MSK 2007
// DTD/Schema  :    plugin.dtd

package org.jetbrains.idea.devkit.dom;

import consulo.xml.util.xml.Attribute;
import consulo.xml.util.xml.GenericAttributeValue;
import consulo.xml.util.xml.GenericDomValue;
import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * plugin.dtd:group interface.
 */
public interface Group extends Actions, ActionOrGroup {
    /**
     * Returns the value of the popup child.
     * Attribute popup
     *
     * @return the value of the popup child.
     */
    @Nonnull
    GenericAttributeValue<Boolean> getPopup();

    /**
     * Returns the value of the compact child.
     * Attribute popup
     *
     * @return the value of the compact child.
     */
    @Nonnull
    GenericAttributeValue<Boolean> getCompact();

    /**
     * Returns the value of the icon child.
     * Attribute icon
     *
     * @return the value of the icon child.
     */
    @Nonnull
    GenericAttributeValue<String> getIcon();

    /**
     * Returns the value of the description child.
     * Attribute description
     *
     * @return the value of the description child.
     */
    @Nonnull
    GenericAttributeValue<String> getDescription();

    /**
     * Returns the value of the class child.
     * Attribute class
     *
     * @return the value of the class child.
     */
    @Nonnull
    @Attribute("class")
    GenericAttributeValue<String> getClazz();

    /**
     * Returns the value of the text child.
     * Attribute text
     *
     * @return the value of the text child.
     */
    @Nonnull
    GenericAttributeValue<String> getText();

    /**
     * Returns the value of the separator child.
     *
     * @return the value of the separator child.
     */
    @Nonnull
    List<GenericDomValue<String>> getSeparators();

    /**
     * Returns the list of add-to-group children.
     *
     * @return the list of add-to-group children.
     */
    @Nonnull
    List<AddToGroup> getAddToGroups();

    /**
     * Adds new child to the list of add-to-group children.
     *
     * @return created child
     */
    AddToGroup addAddToGroup();

    @Nonnull
    GenericAttributeValue<Boolean> isInternal();

    @Nonnull
    GenericAttributeValue<Boolean> isCanUseProjectAsDefault();

    @Nonnull
    GenericAttributeValue<String> getRequireModuleExtensions();
}
