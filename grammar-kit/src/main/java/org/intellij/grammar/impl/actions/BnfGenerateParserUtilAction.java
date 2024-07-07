/*
 * Copyright 2011-present Greg Shrago
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

package org.intellij.grammar.impl.actions;

import com.intellij.java.impl.codeInsight.daemon.impl.quickfix.CreateClassKind;
import com.intellij.java.impl.codeInsight.intention.impl.CreateClassDialog;
import com.intellij.java.language.psi.*;
import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.application.Result;
import consulo.devkit.grammarKit.generator.PlatformClass;
import consulo.document.Document;
import consulo.fileEditor.history.IdeDocumentHistory;
import consulo.language.editor.LangDataKeys;
import consulo.language.editor.WriteCommandAction;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.util.IncorrectOperationException;
import consulo.module.Module;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.awt.Messages;
import consulo.util.collection.ContainerUtil;
import consulo.util.io.FileUtil;
import consulo.util.lang.StringUtil;
import consulo.util.lang.ref.Ref;
import org.intellij.grammar.KnownAttribute;
import org.intellij.grammar.psi.BnfAttr;
import org.intellij.grammar.psi.BnfAttrs;
import org.intellij.grammar.psi.BnfFile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Consumer;

/**
 * @author greg
 */
public class BnfGenerateParserUtilAction extends AnAction {
  @Override
  public void update(AnActionEvent e) {
    PsiFile file = e.getData(LangDataKeys.PSI_FILE);
    if (file instanceof BnfFile bnfFile) {
      boolean enabled = bnfFile.findAttribute(bnfFile.getVersion(), null, KnownAttribute.PARSER_UTIL_CLASS, null) == null;
      e.getPresentation().setEnabledAndVisible(enabled);
    }
    else {
      e.getPresentation().setEnabledAndVisible(false);
    }
  }

  @RequiredUIAccess
  @Override
  public void actionPerformed(AnActionEvent e) {
    PsiFile file = e.getData(LangDataKeys.PSI_FILE);
    if (!(file instanceof BnfFile)) {
      return;
    }

    Project project = file.getProject();
    BnfFile bnfFile = (BnfFile)file;
    final String qualifiedName = createClass(
      bnfFile, "Create Parser Util Class", PlatformClass.GENERATED_PARSER_UTIL_BASE.select(bnfFile.getVersion()),
      getGrammarName(bnfFile) + "ParserUtil",
      getGrammarPackage(bnfFile));
    if (qualifiedName == null) {
      return;
    }

    final int anchorOffset;
    final String text;
    String definition = "\n  " + KnownAttribute.PARSER_UTIL_CLASS.getName() + "=\"" + qualifiedName + "\"";
    BnfAttr attrParser = bnfFile.findAttribute(bnfFile.getVersion(), null, KnownAttribute.PARSER_CLASS, null);
    if (attrParser == null) {
      BnfAttrs rootAttrs = ContainerUtil.getFirstItem(bnfFile.getAttributes());
      if (rootAttrs == null) {
        anchorOffset = 0;
        text = "{" + definition + "\n}";
      }
      else {
        anchorOffset = rootAttrs.getFirstChild().getTextOffset();
        text = definition;
      }
    }
    else {
      anchorOffset = attrParser.getTextRange().getEndOffset();
      text = definition;
    }
    final Document document = PsiDocumentManager.getInstance(project).getDocument(bnfFile);
    if (document == null) {
      return;
    }
    new WriteCommandAction.Simple(project, file) {
      @Override
      protected void run() throws Throwable {
        int position = document.getLineEndOffset(document.getLineNumber(anchorOffset));
        document.insertString(position, text);
      }
    }.execute();

  }

  static String getGrammarPackage(BnfFile bnfFile) {
    String version = bnfFile.findAttributeValue(null, null, KnownAttribute.VERSION, null);

    return StringUtil.getPackageName(bnfFile.findAttributeValue(version, null, KnownAttribute.PARSER_CLASS, null));
  }

  static String getGrammarName(BnfFile bnfFile) {
    String version = bnfFile.findAttributeValue(null, null, KnownAttribute.VERSION, null);

    String parser = bnfFile.findAttributeValue(version, null, KnownAttribute.PARSER_CLASS, null);
    if (!KnownAttribute.PARSER_CLASS.getDefaultValue(version).equals(parser)) {
      String shortName = StringUtil.getShortName(parser);
      int len = "Parser".length();
      String result = shortName.endsWith("Parser") ? shortName.substring(0, shortName.length() - len) : shortName;
      if (StringUtil.isNotEmpty(result)) {
        return result;
      }
    }
    return StringUtil.capitalize(FileUtil.getNameWithoutExtension(bnfFile.getName()));
  }

  @RequiredReadAction
  public static String createClass(
    @Nonnull PsiFile origin,
    @Nonnull final String title,
    @Nullable final String baseClass,
    @Nonnull String suggestedName,
    @Nonnull String suggestedPackage
  ) {
    Project project = origin.getProject();
    Module module = origin.getModule();
    CreateClassDialog dialog = new CreateClassDialog(
      project,
      title,
      suggestedName,
      suggestedPackage,
      CreateClassKind.CLASS,
      true,
      module
    );
    if (!dialog.showAndGet()) {
      return null;
    }

    final String className = dialog.getClassName();
    final PsiDirectory targetDirectory = dialog.getTargetDirectory();
    return createClass(className, targetDirectory, baseClass, title, null);
  }

  static String createClass(
    final String className,
    final PsiDirectory targetDirectory,
    final String baseClass,
    final String title,
    final Consumer<PsiClass> consumer
  ) {
    final Project project = targetDirectory.getProject();
    final Ref<PsiClass> resultRef = Ref.create();

    new WriteCommandAction(project, title) {
      @Override
      protected void run(Result result) throws Throwable {
        IdeDocumentHistory.getInstance(project).includeCurrentPlaceAsChangePlace();

        PsiElementFactory elementFactory = JavaPsiFacade.getInstance(project).getElementFactory();
        PsiJavaCodeReferenceElement ref = baseClass == null ? null
          : elementFactory.createReferenceElementByFQClassName(baseClass, GlobalSearchScope.allScope(project));

        try {
          PsiClass resultClass = JavaDirectoryService.getInstance().createClass(targetDirectory, className);
          resultRef.set(resultClass);
          if (ref != null) {
            PsiElement baseClass = ref.resolve();
            boolean isInterface = baseClass instanceof PsiClass psiClass && psiClass.isInterface();
            PsiReferenceList targetReferenceList = isInterface ? resultClass.getImplementsList() : resultClass.getExtendsList();
            assert targetReferenceList != null;
            targetReferenceList.add(ref);
          }
          if (consumer != null) {
            consumer.accept(resultClass);
          }
        }
        catch (final IncorrectOperationException e) {
          Application.get().invokeLater(() -> Messages.showErrorDialog(
            project,
            "Unable to create class " + className + "\n" + e.getLocalizedMessage(),
            title
          ));
        }
      }
    }.execute();
    return resultRef.isNull() ? null : resultRef.get().getQualifiedName();
  }

}
