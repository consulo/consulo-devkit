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
package org.jetbrains.idea.devkit.dom.impl;

import com.intellij.java.impl.util.xml.DomJavaUtil;
import com.intellij.java.indexing.search.searches.ClassInheritorsSearch;
import com.intellij.java.language.psi.*;
import com.intellij.java.language.psi.util.PsiUtil;
import consulo.application.AllIcons;
import consulo.language.DependentLanguage;
import consulo.language.Language;
import consulo.language.file.LanguageFileType;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.scope.GlobalSearchScopesCore;
import consulo.ui.image.Image;
import consulo.util.collection.ContainerUtil;
import consulo.xml.util.xml.ConvertContext;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

class LanguageResolvingUtil {
  private static String ANY_LANGUAGE_DEFAULT_ID = Language.ANY.getID();

  private static String[] ourLanguageClasses = {
    // v2 - will removed in future
    "com.intellij.lang.Language",
    // v3
    "consulo.language.Language"
  };

  static Collection<LanguageDefinition> getAllLanguageDefinitions(ConvertContext context) {
    List<LanguageDefinition> languageDefinitions = collectLanguageDefinitions(context);
    ContainerUtil.addIfNotNull(languageDefinitions, createAnyLanguageDefinition(context));
    return languageDefinitions;
  }

  private static List<LanguageDefinition> collectLanguageDefinitions(final ConvertContext context) {
    final List<LanguageDefinition> libraryDefinitions = collectLibraryLanguages(context);
    final List<LanguageDefinition> projectDefinitions = collectProjectLanguages(context, libraryDefinitions);

    final List<LanguageDefinition> all = new ArrayList<>(libraryDefinitions);
    all.addAll(projectDefinitions);
    return all;
  }

  private static List<LanguageDefinition> collectLibraryLanguages(final ConvertContext context) {
    return ContainerUtil.mapNotNull(Language.getRegisteredLanguages(), language ->
    {
      if (language.getID().isEmpty() || language instanceof DependentLanguage) {
        return null;
      }
      final PsiClass psiClass = DomJavaUtil.findClass(language.getClass().getName(), context.getInvocationElement());
      if (psiClass == null) {
        return null;
      }

      final LanguageFileType type = language.getAssociatedFileType();
      final Image icon = type != null ? type.getIcon() : null;
      return new LanguageDefinition(language.getID(), psiClass, icon, language.getDisplayName());
    });
  }

  private static List<LanguageDefinition> collectProjectLanguages(ConvertContext context, final List<LanguageDefinition> libraryLanguages) {
    List<PsiClass> languagesPsiClasses = new ArrayList<>();

    GlobalSearchScope scope = GlobalSearchScopesCore.projectProductionScope(context.getProject());

    for (String languageClassName : ourLanguageClasses) {
      final PsiClass languageClass = DomJavaUtil.findClass(languageClassName, context.getInvocationElement());
      if (languageClass == null) {
        continue;
      }

      ClassInheritorsSearch.search(languageClass, scope, true).forEach(it -> {
        languagesPsiClasses.add(it);
        return true;
      });
    }

    return ContainerUtil.mapNotNull(languagesPsiClasses, new Function<PsiClass, LanguageDefinition>() {
      @Nullable
      @Override
      public LanguageDefinition apply(final PsiClass language) {
        if (language.hasModifierProperty(PsiModifier.ABSTRACT)) {
          return null;
        }

        if (ContainerUtil.exists(libraryLanguages, definition -> definition.clazz.equals(language))) {
          return null;
        }

        String id = computeConstantSuperCtorCallParameter(language, 0);
        if (id == null) {
          id = computeConstantSuperCtorCallParameter(language, 1);
        }
        if (id == null) {
          id = computeConstantReturnValue(language, "getID");
        }
        if (id == null) {
          return null;
        }

        return new LanguageDefinition(id, language, null, computeConstantReturnValue(language, "getDisplayName"));
      }
    });
  }

  @Nullable
  private static String computeConstantReturnValue(PsiClass languagePsiClass, String methodName) {
    final PsiMethod[] methods = languagePsiClass.findMethodsByName(methodName, false);
    if (methods.length != 1) {
      return null;
    }

    final PsiMethod method = methods[0];
    final PsiCodeBlock body = method.getBody();

    if (body == null) {
      return null;
    }
    final PsiStatement[] statements = body.getStatements();
    if (statements.length != 1) {
      return null;
    }


    final PsiStatement statement = statements[0];
    if (!(statement instanceof PsiReturnStatement)) {
      return null;
    }
    final PsiReturnStatement returnStatement = (PsiReturnStatement)statement;
    final PsiExpression returnValue = returnStatement.getReturnValue();
    if (returnValue == null || !PsiUtil.isConstantExpression(returnValue)) {
      return null;
    }

    return computeConstant(languagePsiClass, returnValue);
  }

  private static String computeConstantSuperCtorCallParameter(PsiClass languagePsiClass, int index) {
    PsiMethod defaultConstructor = null;
    for (PsiMethod constructor : languagePsiClass.getConstructors()) {
      if (constructor.getParameterList().getParametersCount() == 0) {
        defaultConstructor = constructor;
        break;
      }
    }
    if (defaultConstructor == null) {
      return null;
    }

    final PsiCodeBlock body = defaultConstructor.getBody();
    if (body == null) {
      return null;
    }
    final PsiStatement[] statements = body.getStatements();
    if (statements.length < 1) {
      return null;
    }

    // super() must be first
    PsiStatement statement = statements[0];
    if (!(statement instanceof PsiExpressionStatement)) {
      return null;
    }
    PsiExpression expression = ((PsiExpressionStatement)statement).getExpression();
    if (!(expression instanceof PsiMethodCallExpression)) {
      return null;
    }
    PsiMethodCallExpression methodCallExpression = (PsiMethodCallExpression)expression;
    final PsiExpression[] argumentExpressions = methodCallExpression.getArgumentList().getExpressions();
    if (argumentExpressions.length < index + 1) {
      return null;
    }
    return computeConstant(languagePsiClass, argumentExpressions[index]);
  }

  private static String computeConstant(PsiClass languagePsiClass, PsiExpression returnValue) {
    final PsiConstantEvaluationHelper constantEvaluationHelper = JavaPsiFacade.getInstance(languagePsiClass.getProject())
                                                                              .getConstantEvaluationHelper();

    final Object constant = constantEvaluationHelper.computeConstantExpression(returnValue);
    return constant instanceof String ? ((String)constant) : null;
  }

  @Nullable
  private static LanguageDefinition createAnyLanguageDefinition(ConvertContext context) {
    PsiClass languageClass = null;
    for (String languageClassName : ourLanguageClasses) {
      final PsiClass temp = DomJavaUtil.findClass(languageClassName, context.getInvocationElement());
      if (temp != null) {
        languageClass = temp;
        break;
      }
    }

    if (languageClass == null) {
      return null;
    }

    String anyLanguageId = calculateAnyLanguageId(context);
    return new LanguageDefinition(anyLanguageId, languageClass, AllIcons.FileTypes.Any_type, "<any language>");
  }

  private static String calculateAnyLanguageId(ConvertContext context) {
    return ANY_LANGUAGE_DEFAULT_ID;
  }

  static class LanguageDefinition {

    final String id;
    final PsiClass clazz;
    final Image icon;
    final String displayName;

    LanguageDefinition(String id, PsiClass clazz, Image icon, String displayName) {
      this.id = id;
      this.clazz = clazz;
      this.icon = icon;
      this.displayName = displayName;
    }
  }
}
