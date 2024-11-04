/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package org.jetbrains.idea.devkit.dom;

import consulo.xml.util.xml.DomElement;
import consulo.xml.util.xml.GenericAttributeValue;
import consulo.xml.util.xml.NameValue;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author mike
 */
public interface Extension extends DomElement {
    String IMPLEMENTATION_ATTRIBUTE = "implementation";

    @NameValue
    GenericAttributeValue<String> getId();

    GenericAttributeValue<String> getOrder();

    @Nullable
    ExtensionPoint getExtensionPoint();

    static boolean isClassField(@Nonnull String fieldName) {
        return IMPLEMENTATION_ATTRIBUTE.equals(fieldName)
            || "className".equals(fieldName)
            || "serviceInterface".equals(fieldName)
            || "serviceImplementation".equals(fieldName)
            || "class".equals(fieldName)
            || fieldName.endsWith("ClassName")
            || (fieldName.endsWith("Class") && !fieldName.equals("forClass"));
    }
}
