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
package org.jetbrains.idea.devkit.dom.impl;

import consulo.application.util.CachedValueProvider;
import consulo.application.util.CachedValuesManager;
import consulo.language.psi.PsiModificationTracker;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.scope.GlobalSearchScopesCore;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.project.content.scope.ProjectScopes;
import consulo.util.collection.ContainerUtil;
import consulo.xml.util.xml.DomFileElement;
import consulo.xml.util.xml.DomService;
import org.jetbrains.idea.devkit.dom.IdeaPlugin;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author mike
 */
public class IdeaPluginConverter  {
  public static Collection<IdeaPlugin> getAllPlugins(final Project project) {
    if (DumbService.isDumb(project)) {
      return Collections.emptyList();
    }

    return CachedValuesManager.getManager(project).getCachedValue(project, new CachedValueProvider<Collection<IdeaPlugin>>() {
      @Nullable
      @Override
      public Result<Collection<IdeaPlugin>> compute() {
        GlobalSearchScope scope = GlobalSearchScopesCore.projectProductionScope(project).union(ProjectScopes.getLibrariesScope(project));
        return Result.create(getPlugins(project, scope), PsiModificationTracker.OUT_OF_CODE_BLOCK_MODIFICATION_COUNT);
      }
    });
  }

  @Nonnull
  public static Collection<IdeaPlugin> getPlugins(Project project, GlobalSearchScope scope) {
    if (DumbService.isDumb(project)) {
      return Collections.emptyList();
    }

    List<DomFileElement<IdeaPlugin>> files = DomService.getInstance().getFileElements(IdeaPlugin.class, project, scope);
    return ContainerUtil.map(files, DomFileElement::getRootElement);
  }
}
