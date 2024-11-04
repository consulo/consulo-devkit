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
package org.jetbrains.idea.devkit.navigation;

import com.intellij.java.language.JavaLanguage;
import com.intellij.java.language.psi.PsiClass;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.AllIcons;
import consulo.codeEditor.markup.GutterIconRenderer;
import consulo.devkit.util.PluginModuleUtil;
import consulo.language.Language;
import consulo.language.editor.gutter.RelatedItemLineMarkerInfo;
import consulo.language.editor.gutter.RelatedItemLineMarkerProvider;
import consulo.language.editor.ui.navigation.NavigationGutterIconBuilder;
import consulo.language.navigation.GotoRelatedItem;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.util.ModuleUtilCore;
import consulo.module.Module;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.Lists;
import consulo.util.lang.StringUtil;
import org.jetbrains.idea.devkit.inspections.DescriptionCheckerUtil;
import org.jetbrains.idea.devkit.inspections.DescriptionType;
import org.jetbrains.idea.devkit.inspections.InspectionDescriptionInfo;
import org.jetbrains.idea.devkit.util.PsiUtil;

import jakarta.annotation.Nonnull;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

@ExtensionImpl
public class DescriptionTypeRelatedItemLineMarkerProvider extends RelatedItemLineMarkerProvider {

  private static final Function<PsiFile, Collection<? extends PsiElement>> CONVERTER = ContainerUtil::createMaybeSingletonList;

  private static final Function<PsiFile, Collection<? extends GotoRelatedItem>> RELATED_ITEM_PROVIDER =
    psiFile -> GotoRelatedItem.createItems(Collections.singleton(psiFile), "DevKit");

  @Override
  @RequiredReadAction
  public boolean isAvailable(@Nonnull PsiFile file) {
    return PluginModuleUtil.isConsuloOrPluginProject(file);
  }

  @Override
  protected void collectNavigationMarkers(@Nonnull PsiElement element,
                                          Collection<? super RelatedItemLineMarkerInfo> result) {
    if (element instanceof PsiClass) {
      process((PsiClass)element, result);
    }
  }

  private static void process(PsiClass psiClass, Collection<? super RelatedItemLineMarkerInfo> result) {
    if (!PsiUtil.isInstantiable(psiClass)) {
      return;
    }

    Module module = ModuleUtilCore.findModuleForPsiElement(psiClass);
    if (module == null) {
      return;
    }

    final GlobalSearchScope scope = GlobalSearchScope.moduleRuntimeScope(module, false);
    final PsiClass actionClass = DescriptionType.INSPECTION.findClass(psiClass.getProject(), scope);
    if (actionClass == null) {
      return;
    }

    PsiElement highlightingElement = psiClass.getNameIdentifier();
    if (highlightingElement == null) {
      return;
    }

    for (DescriptionType type : DescriptionType.values()) {
      if (!type.isInheritor(psiClass)) {
        continue;
      }

      String descriptionDirName = DescriptionCheckerUtil.getDescriptionDirName(psiClass);
      if (StringUtil.isEmptyOrSpaces(descriptionDirName)) {
        return;
      }

      if (type == DescriptionType.INSPECTION) {
        final InspectionDescriptionInfo info = InspectionDescriptionInfo.create(module, psiClass);
        if (info.hasDescriptionFile()) {
          addDescriptionFileGutterIcon(highlightingElement, info.getDescriptionFile(), result);
        }
        return;
      }

      for (PsiDirectory descriptionDir : DescriptionCheckerUtil.getDescriptionsDirs(module, type)) {
        PsiDirectory dir = descriptionDir.findSubdirectory(descriptionDirName);
        if (dir == null) {
          continue;
        }
        final PsiFile descriptionFile = dir.findFile("description.html");
        if (descriptionFile != null) {
          addDescriptionFileGutterIcon(highlightingElement, descriptionFile, result);

          addBeforeAfterTemplateFilesGutterIcon(highlightingElement, dir, result);
          return;
        }
      }
      return;
    }
  }

  private static void addDescriptionFileGutterIcon(PsiElement highlightingElement,
                                                   PsiFile descriptionFile,
                                                   Collection<? super RelatedItemLineMarkerInfo> result) {
    final RelatedItemLineMarkerInfo<PsiElement>
      info = NavigationGutterIconBuilder.create(AllIcons.FileTypes.Html, CONVERTER,
                                                RELATED_ITEM_PROVIDER)
                                        .setTarget(descriptionFile)
                                        .setTooltipText("Description")
                                        .setAlignment(GutterIconRenderer.Alignment.RIGHT)
                                        .createLineMarkerInfo(highlightingElement);
    result.add(info);
  }

  private static void addBeforeAfterTemplateFilesGutterIcon(PsiElement highlightingElement,
                                                            PsiDirectory descriptionDirectory,
                                                            Collection<? super RelatedItemLineMarkerInfo> result) {
    final List<PsiFile> templateFiles =
      Lists.newSortedList((Comparator<PsiFile>)(o1, o2) -> o1.getName().compareTo(o2.getName()));
    for (PsiFile file : descriptionDirectory.getFiles()) {
      final String fileName = file.getName();
      if (fileName.endsWith(".template")) {
        if (fileName.startsWith("after.") || fileName.startsWith("before.")) {
          templateFiles.add(file);
        }
      }
    }
    if (templateFiles.isEmpty()) {
      return;
    }

    final RelatedItemLineMarkerInfo<PsiElement>
      info = NavigationGutterIconBuilder.create(AllIcons.Actions.Diff, CONVERTER,
                                                RELATED_ITEM_PROVIDER)
                                        .setTargets(templateFiles)
                                        .setPopupTitle("Select Template")
                                        .setTooltipText("Before/After Templates")
                                        .setAlignment(GutterIconRenderer.Alignment.RIGHT)
                                        .createLineMarkerInfo(highlightingElement);
    result.add(info);
  }

  @Nonnull
  @Override
  public Language getLanguage() {
    return JavaLanguage.INSTANCE;
  }
}
