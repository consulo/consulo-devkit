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

import com.intellij.ide.presentation.Presentation;
import com.intellij.psi.PsiClass;
import com.intellij.util.xml.*;
import consulo.annotation.DeprecationInfo;
import org.jetbrains.idea.devkit.dom.impl.PluginPsiClassConverter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * @author mike
 */
@Presentation(typeName = "Extension Point")
public interface ExtensionPoint extends DomElement
{
	enum Area
	{
		@Deprecated
		@DeprecationInfo("Use without 'CONSULO_' prefix")
		CONSULO_PROJECT,
		@Deprecated
		@DeprecationInfo("Use without 'CONSULO_' prefix")
		CONSULO_MODULE,

		PROJECT,
		MODULE
	}

	@Nonnull
	@NameValue
	GenericAttributeValue<String> getName();

	@Nonnull
	@Convert(PluginPsiClassConverter.class)
	GenericAttributeValue<PsiClass> getInterface();

	@Nonnull
	@Attribute("beanClass")
	@Convert(PluginPsiClassConverter.class)
	GenericAttributeValue<PsiClass> getBeanClass();

	@Nonnull
	GenericAttributeValue<Area> getArea();

	@Nonnull
	@SubTagList("with")
	List<With> getWithElements();

	With addWith();

	/**
	 * Returns the fully qualified EP name
	 *
	 * @return {@code PluginID.name} or {@code qualifiedName}.
	 * @since 14
	 */
	@Nonnull
	String getEffectiveQualifiedName();

	/**
	 * Returns EP name prefix (Plugin ID).
	 *
	 * @return {@code null} if {@code qualifiedName} is set.
	 */
	@Nullable
	String getNamePrefix();
}
