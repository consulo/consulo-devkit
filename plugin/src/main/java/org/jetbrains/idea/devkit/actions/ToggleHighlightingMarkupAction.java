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
//import consulo.application.WriteAction;
//import consulo.codeEditor.Editor;
//import consulo.devkit.action.InternalAction;
//import consulo.document.Document;
//import consulo.document.util.TextRange;
//import consulo.ide.impl.idea.openapi.command.CommandProcessorEx;
//import consulo.ide.impl.idea.openapi.command.CommandToken;
//import consulo.ide.impl.idea.openapi.editor.ex.util.EditorUtil;
//import consulo.language.editor.CommonDataKeys;
//import consulo.language.editor.annotation.HighlightSeverity;
//import consulo.language.editor.impl.internal.daemon.DaemonCodeAnalyzerEx;
//import consulo.language.editor.rawHighlight.HighlightInfo;
//import consulo.language.psi.PsiFile;
//import consulo.project.Project;
//import consulo.ui.annotation.RequiredUIAccess;
//import consulo.ui.ex.action.AnActionEvent;
//import consulo.undoRedo.CommandProcessor;
//import consulo.undoRedo.UndoConfirmationPolicy;
//
//import javax.annotation.Nonnull;
//import javax.annotation.Nullable;
//import java.util.ArrayList;
//import java.util.LinkedList;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//
///**
// * @author gregsh
// */
//public class ToggleHighlightingMarkupAction extends InternalAction {
//  @RequiredUIAccess
//  @Override
//  public void update(@Nonnull AnActionEvent e) {
//    Editor editor = e.getData(CommonDataKeys.EDITOR);
//    PsiFile file = e.getData(CommonDataKeys.PSI_FILE);
//    e.getPresentation().setEnabled(editor != null && file != null);
//  }
//
//  @RequiredUIAccess
//  @Override
//  public void actionPerformed(@Nonnull AnActionEvent e) {
//    final Editor editor = e.getData(CommonDataKeys.EDITOR);
//    PsiFile file = e.getData(CommonDataKeys.PSI_FILE);
//    if (editor == null || file == null) {
//      return;
//    }
//    final Project project = file.getProject();
//    consulo.ide.impl.idea.openapi.command.CommandProcessorEx commandProcessor = (CommandProcessorEx)CommandProcessor.getInstance();
//    CommandToken commandToken =
//      commandProcessor.startCommand(project, e.getPresentation().getText(), e.getPresentation().getText(), UndoConfirmationPolicy.DEFAULT);
//    try {
//      WriteAction.run(() ->
//                      {
//                        TextRange range =
//                          editor.getSelectionModel().hasSelection() ? EditorUtil.getSelectionInAnyMode(editor) : TextRange.create(0,
//                                                                                                                                  editor.getDocument()
//                                                                                                                                        .getTextLength());
//                        perform(project, editor.getDocument(), range);
//                      });
//    }
//    finally {
//      commandProcessor.finishCommand(commandToken, null);
//    }
//  }
//
//  private static void perform(Project project, final Document document, TextRange selection) {
//    final CharSequence sequence = document.getCharsSequence();
//    final StringBuilder sb = new StringBuilder();
//    Pattern pattern =
//      Pattern.compile("<(/?(?:error|warning|EOLError|EOLWarning|info|weak_warning))((?:\\s|=|\\w+|\"(?:[^\"]|\\\\\")*?\")*?)>(.*?)");
//    Matcher matcher = pattern.matcher(sequence);
//    if (matcher.find(selection.getStartOffset())) {
//      boolean compactMode = false;
//      int pos = 0;
//      do {
//        if (matcher.start(0) >= selection.getEndOffset()) {
//          break;
//        }
//        String tag = matcher.group(1);
//        if (!tag.startsWith("/")) {
//          boolean hasDescription = matcher.start(2) < matcher.end(2);
//          if (hasDescription) {
//            if (!compactMode) {
//              sb.setLength(pos = 0); // restart in compact mode from the first description
//              compactMode = true;
//            }
//            sb.append(sequence, pos, matcher.start(2));
//            pos = matcher.end(2);
//          }
//          else if (!compactMode) {
//            sb.append(sequence, pos, matcher.start());
//            pos = matcher.end();
//          }
//        }
//        else if (!compactMode) {
//          sb.append(sequence, pos, matcher.start());
//          pos = matcher.end();
//        }
//      }
//      while (matcher.find(matcher.end()));
//      sb.append(sequence, pos, sequence.length());
//    }
//    else {
//      final int[] offset = {0};
//      final ArrayList<HighlightInfo> infos = new ArrayList<>();
//      DaemonCodeAnalyzerEx.processHighlights(document, project, HighlightSeverity.WARNING, 0, sequence.length(), info ->
//      {
//        if (info.getSeverity() != HighlightSeverity.WARNING && info.getSeverity() != HighlightSeverity.ERROR) {
//          return true;
//        }
//        if (info.getStartOffset() >= selection.getEndOffset()) {
//          return false;
//        }
//        if (info.getEndOffset() > selection.getStartOffset()) {
//          offset[0] = appendInfo(info, sb, sequence, offset[0], infos);
//        }
//        return true;
//      });
//      offset[0] = appendInfo(null, sb, sequence, offset[0], infos);
//      sb.append(sequence.subSequence(offset[0], sequence.length()));
//    }
//    document.setText(sb);
//  }
//
//  private static int appendInfo(@Nullable HighlightInfo info,
//                                StringBuilder sb,
//                                CharSequence sequence,
//                                int offset,
//                                ArrayList<HighlightInfo> infos) {
//    if (info == null || !infos.isEmpty() && getMaxEnd(infos) < info.getStartOffset()) {
//      if (infos.size() == 1) {
//        HighlightInfo cur = infos.remove(0);
//        sb.append(sequence.subSequence(offset, cur.getStartOffset()));
//        appendTag(sb, cur, true);
//        sb.append(sequence.subSequence(cur.getStartOffset(), cur.getEndOffset()));
//        appendTag(sb, cur, false);
//        offset = cur.getEndOffset();
//      }
//      else {
//        // process overlapped
//        LinkedList<HighlightInfo> stack = new LinkedList<>();
//        for (HighlightInfo cur : infos) {
//          offset = processStack(stack, sb, sequence, offset, cur.getStartOffset());
//          sb.append(sequence.subSequence(offset, cur.getStartOffset()));
//          offset = cur.getStartOffset();
//          appendTag(sb, cur, true);
//          stack.addLast(cur);
//        }
//        offset = processStack(stack, sb, sequence, offset, sequence.length());
//        infos.clear();
//      }
//    }
//    if (info != null) {
//      boolean found = false;
//      for (HighlightInfo cur : infos) {
//        if (cur.getStartOffset() == info.getStartOffset() && cur.getEndOffset() == info.getEndOffset() && cur.getSeverity() == info.getSeverity()) {
//          found = true;
//          break;
//        }
//      }
//      if (!found) {
//        infos.add(info);
//      }
//    }
//    return offset;
//  }
//
//  private static int getMaxEnd(ArrayList<HighlightInfo> infos) {
//    int max = -1;
//    for (HighlightInfo info : infos) {
//      int endOffset = info.getEndOffset();
//      if (max < endOffset) {
//        max = endOffset;
//      }
//    }
//    return max;
//  }
//
//  private static int processStack(LinkedList<HighlightInfo> stack,
//                                  StringBuilder sb,
//                                  CharSequence sequence,
//                                  int offset,
//                                  final int endOffset) {
//    if (stack.isEmpty()) {
//      return offset;
//    }
//    for (HighlightInfo cur = stack.peekLast(); cur != null && cur.getEndOffset() <= endOffset; cur = stack.peekLast()) {
//      stack.removeLast();
//      if (offset <= cur.getEndOffset()) {
//        sb.append(sequence.subSequence(offset, cur.getEndOffset()));
//      }
//      offset = cur.getEndOffset();
//      appendTag(sb, cur, false);
//    }
//    return offset;
//  }
//
//  private static void appendTag(StringBuilder sb, HighlightInfo cur, boolean opening) {
//    sb.append("<");
//    if (!opening) {
//      sb.append("/");
//    }
//    if (cur.isAfterEndOfLine()) {
//      sb.append(cur.getSeverity() == HighlightSeverity.WARNING ? "EOLWarning" : "EOLError");
//    }
//    else {
//      sb.append(cur.getSeverity() == HighlightSeverity.WARNING ? "warning" : "error");
//    }
//    if (opening) {
//      sb.append(" descr=\"").append(cur.getDescription()).append("\"");
//
//    }
//    sb.append(">");
//  }
//}
