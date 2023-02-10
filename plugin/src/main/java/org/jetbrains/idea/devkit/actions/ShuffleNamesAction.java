///*
// * Copyright 2000-2012 JetBrains s.r.o.
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// * http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//package org.jetbrains.idea.devkit.actions;
//
//import consulo.application.AccessToken;
//import consulo.application.ApplicationManager;
//import consulo.codeEditor.Editor;
//import consulo.devkit.action.InternalAction;
//import consulo.language.editor.LangDataKeys;
//import consulo.language.editor.PlatformDataKeys;
//import consulo.language.impl.psi.LeafPsiElement;
//import consulo.language.psi.PsiElement;
//import consulo.language.psi.PsiFile;
//import consulo.language.psi.PsiRecursiveElementWalkingVisitor;
//import consulo.project.Project;
//import consulo.ui.annotation.RequiredUIAccess;
//import consulo.ui.ex.action.AnActionEvent;
//import consulo.undoRedo.CommandProcessor;
//import consulo.undoRedo.UndoConfirmationPolicy;
//import consulo.util.lang.StringUtil;
//
//import java.util.*;
//
///**
// * @author gregsh
// */
//public class ShuffleNamesAction extends InternalAction {
//  @RequiredUIAccess
//  @Override
//  public void update(AnActionEvent e) {
//    Editor editor = e.getData(PlatformDataKeys.EDITOR);
//    PsiFile file = e.getData(LangDataKeys.PSI_FILE);
//    e.getPresentation().setEnabled(editor != null && file != null);
//  }
//
//  @Override
//  public void actionPerformed(AnActionEvent e) {
//    final Editor editor = e.getData(PlatformDataKeys.EDITOR);
//    PsiFile file = e.getData(LangDataKeys.PSI_FILE);
//    if (editor == null || file == null) {
//      return;
//    }
//    final Project project = file.getProject();
//    CommandProcessor commandProcessor = CommandProcessor.getInstance();
//    CommandToken commandToken =
//      commandProcessor.startCommand(project, e.getPresentation().getText(), e.getPresentation().getText(), UndoConfirmationPolicy.DEFAULT);
//    AccessToken token = ApplicationManager.getApplication().acquireWriteActionLock(getClass());
//    try {
//      shuffleIds(file, editor);
//    }
//    finally {
//      token.finish();
//      commandProcessor.finishCommand(commandToken, null);
//    }
//  }
//
//  private static boolean shuffleIds(PsiFile file, Editor editor) {
//    final Map<String, String> map = new HashMap<String, String>();
//    final StringBuilder sb = new StringBuilder();
//    final StringBuilder quote = new StringBuilder();
//    final ArrayList<String> split = new ArrayList<String>(100);
//    file.acceptChildren(new PsiRecursiveElementWalkingVisitor() {
//      @Override
//      public void visitElement(PsiElement element) {
//        if (element instanceof LeafPsiElement) {
//          String type = ((LeafPsiElement)element).getElementType().toString();
//          String text = element.getText();
//          if (text.isEmpty()) {
//            return;
//          }
//
//          for (int i = 0, len = text.length(); i < len / 2; i++) {
//            char c = text.charAt(i);
//            if (c == text.charAt(len - i - 1) && !Character.isLetter(c)) {
//              quote.append(c);
//            }
//            else {
//              break;
//            }
//          }
//
//          boolean isQuoted = quote.length() > 0;
//          boolean isNumber = false;
//          if (isQuoted || type.equals("ID") || type.contains("IDENT") && !"ts".equals(text) || (isNumber = text.matches("[0-9]+"))) {
//            String replacement = map.get(text);
//            if (replacement == null) {
//              split.addAll(Arrays.asList((isQuoted ? text.substring(quote.length(), text.length() - quote.length())
//                                                         .replace("''", "") : text).split("")));
//              if (!isNumber) {
//                for (ListIterator<String> it = split.listIterator(); it.hasNext(); ) {
//                  String s = it.next();
//                  if (s.isEmpty()) {
//                    it.remove();
//                    continue;
//                  }
//                  int c = s.charAt(0);
//                  int cap = c & 32;
//                  c &= ~cap;
//                  c = (char)((c >= 'A') && (c <= 'Z') ? ((c - 'A' + 7) % 26 + 'A') : c) | cap;
//                  it.set(String.valueOf((char)c));
//                }
//              }
//              Collections.shuffle(split);
//              if (isNumber && "0".equals(split.get(0))) {
//                split.set(0, "1");
//              }
//              replacement = StringUtil.join(split, "");
//              if (isQuoted) {
//                replacement = quote + replacement + quote.reverse();
//              }
//              map.put(text, replacement);
//            }
//            text = replacement;
//          }
//          sb.append(text);
//          quote.setLength(0);
//          split.clear();
//        }
//        super.visitElement(element);
//      }
//    });
//    editor.getDocument().setText(sb.toString());
//    return true;
//  }
//}
