/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.paths.PathReference;
import com.intellij.openapi.paths.PathReferenceManager;
import com.intellij.openapi.paths.PathReferenceProvider;
import com.intellij.openapi.paths.StaticPathReferenceProvider;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceSet;
import com.intellij.psi.xml.XmlElement;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.NullableFunction;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xml.ConvertContext;
import com.intellij.util.xml.DomFileElement;
import com.intellij.util.xml.DomUtil;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.converters.PathReferenceConverter;
import consulo.annotation.access.RequiredReadAction;
import consulo.java.roots.SpecialDirUtil;
import org.jetbrains.idea.devkit.util.DescriptorUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class DependencyConfigFileConverter extends PathReferenceConverter
{

	private static final PathReferenceProvider ourProvider = new StaticPathReferenceProvider(null)
	{

		@Override
		public boolean createReferences(@Nonnull final PsiElement psiElement, int offset, String text, @Nonnull List<PsiReference> references, boolean soft)
		{
			FileReferenceSet set = new FileReferenceSet(text, psiElement, offset, null, true, true, new FileType[]{XmlFileType.INSTANCE})
			{

				private final Condition<PsiFileSystemItem> PLUGIN_XML_CONDITION = item -> !item.isDirectory() && !item.equals(getContainingFile()) && (item instanceof XmlFile && DescriptorUtil
						.isPluginXml((PsiFile) item)) && !isAlreadyUsed((XmlFile) item);

				private boolean isAlreadyUsed(final XmlFile xmlFile)
				{
					final PsiFile file = getContainingFile();
					if(!(file instanceof XmlFile))
					{
						return false;
					}
					final DomFileElement<IdeaPlugin> ideaPlugin = DescriptorUtil.getConsuloPlugin((XmlFile) file);
					if(ideaPlugin == null)
					{
						return false;
					}
					return !ContainerUtil.process(ideaPlugin.getRootElement().getDependencies(), dependency ->
					{
						final GenericAttributeValue<PathReference> configFileAttribute = dependency.getConfigFile();
						if(!DomUtil.hasXml(configFileAttribute))
						{
							return true;
						}
						final PathReference pathReference = configFileAttribute.getValue();
						if(pathReference == null)
						{
							return true;
						}
						return !xmlFile.equals(pathReference.resolve());
					});
				}

				@Nonnull
				@Override
				@RequiredReadAction
				public Collection<PsiFileSystemItem> computeDefaultContexts()
				{
					final PsiFile containingFile = getContainingFile();
					if(containingFile == null)
					{
						return Collections.emptyList();
					}

					final Module module = ModuleUtilCore.findModuleForPsiElement(getElement());
					if(module == null)
					{
						return Collections.emptyList();
					}

					final Set<VirtualFile> roots = new HashSet<>();
					final VirtualFile parent = containingFile.getVirtualFile().getParent();
					roots.add(parent);

					roots.addAll(SpecialDirUtil.collectSpecialDirs(module, SpecialDirUtil.META_INF));
					return toFileSystemItems(roots);
				}

				@Override
				@Nonnull
				protected Collection<PsiFileSystemItem> toFileSystemItems(@Nonnull Collection<VirtualFile> files)
				{
					final PsiManager manager = getElement().getManager();
					return ContainerUtil.mapNotNull(files, (NullableFunction<VirtualFile, PsiFileSystemItem>) file -> file != null ? manager.findDirectory(file) : null);
				}

				@Override
				protected boolean isSoft()
				{
					return true;
				}

				@Override
				protected Condition<PsiFileSystemItem> getReferenceCompletionFilter()
				{
					return PLUGIN_XML_CONDITION;
				}
			};
			Collections.addAll(references, set.getAllReferences());
			return true;
		}
	};

	@Override
	public PathReference fromString(@Nullable String s, ConvertContext context)
	{
		final XmlElement element = context.getXmlElement();
		final Module module = context.getModule();
		if(s == null || element == null || module == null)
		{
			return null;
		}
		return PathReferenceManager.getInstance().getCustomPathReference(s, module, element, ourProvider);
	}

	@Nonnull
	@Override
	public PsiReference[] createReferences(@Nonnull PsiElement psiElement, boolean soft)
	{
		return PathReferenceManager.getInstance().createCustomReferences(psiElement, soft, ourProvider);
	}
}
