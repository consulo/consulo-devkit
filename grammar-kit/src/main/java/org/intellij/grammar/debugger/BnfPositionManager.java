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

package org.intellij.grammar.debugger;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.intellij.grammar.KnownAttribute;
import org.intellij.grammar.generator.ParserGeneratorUtil;
import org.intellij.grammar.parser.GeneratedParserUtilBase;
import org.intellij.grammar.psi.BnfAttr;
import org.intellij.grammar.psi.BnfExpression;
import org.intellij.grammar.psi.BnfRule;
import org.intellij.grammar.psi.impl.BnfFileImpl;
import org.intellij.grammar.psi.impl.GrammarUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.debugger.NoDataException;
import com.intellij.debugger.PositionManager;
import com.intellij.debugger.SourcePosition;
import com.intellij.debugger.engine.DebugProcess;
import com.intellij.debugger.requests.ClassPrepareRequestor;
import com.intellij.openapi.application.AccessToken;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiCodeBlock;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiRecursiveElementWalkingVisitor;
import com.intellij.psi.PsiStatement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiNonJavaFileReferenceProcessor;
import com.intellij.psi.search.PsiSearchHelper;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.PairProcessor;
import com.intellij.util.containers.FactoryMap;
import consulo.internal.com.sun.jdi.AbsentInformationException;
import consulo.internal.com.sun.jdi.Location;
import consulo.internal.com.sun.jdi.ReferenceType;
import consulo.internal.com.sun.jdi.request.ClassPrepareRequest;

/**
 * @author gregsh
 */
public class BnfPositionManager implements PositionManager {
  private final DebugProcess myProcess;
  private final Map<String, Collection<PsiFile>> myGrammars = new FactoryMap<String, Collection<PsiFile>>() {
    @Override
    protected Collection<PsiFile> create(String key) {
      final Project project = myProcess.getProject();
      final Ref<Collection<PsiFile>> result = Ref.create(null);
      PsiSearchHelper.SERVICE.getInstance(project).processUsagesInNonJavaFiles(key, new PsiNonJavaFileReferenceProcessor() {
        @Override
        public boolean process(PsiFile file, int startOffset, int endOffset) {
          if (!(file instanceof BnfFileImpl)) return true;
          BnfAttr attr = PsiTreeUtil.getParentOfType(file.findElementAt(startOffset), BnfAttr.class);
          if (attr == null || !"parserClass".equals(attr.getName())) return true;
          if (result.isNull()) result.set(new LinkedHashSet<PsiFile>(1));
          result.get().add(file);
          return true;
        }
      }, GlobalSearchScope.allScope(project));
      return result.isNull()? Collections.<PsiFile>emptyList() : result.get();
    }
  };

  public BnfPositionManager(DebugProcess process) {
    myProcess = process;
  }

  @Override
  public SourcePosition getSourcePosition(@Nullable Location location) throws NoDataException {
    if (true) throw new NoDataException();
    if (location == null) throw new NoDataException();

    final ReferenceType refType = location.declaringType();
    if (refType == null) throw new NoDataException();

    int dollar = refType.name().indexOf("$");
    String qname = dollar == -1? refType.name() : refType.name().substring(0, dollar);

    final String name = location.method().name();
    int lineNumber = location.lineNumber() - 1;

    for (PsiFile file : myGrammars.get(qname)) {
      BnfExpression expression = findExpression(file, name);
      BnfRule rule = PsiTreeUtil.getParentOfType(expression, BnfRule.class);
      if (expression != null && qname.equals(ParserGeneratorUtil.getAttribute(rule, KnownAttribute.PARSER_CLASS))) {
        for (BnfExpression expr : ParserGeneratorUtil.getChildExpressions(expression)) {
          int line = getLineNumber(expr, qname, lineNumber);
          if (line == lineNumber) {
            return SourcePosition.createFromElement(expr);
          }
        }
        if (lineNumber == getLineNumber(expression, qname, lineNumber)) {
          return SourcePosition.createFromElement(expression);
        }
        return SourcePosition.createFromElement(rule);
      }
    }
    throw new NoDataException();
  }

  @Nullable
  private static BnfExpression findExpression(PsiFile file, final String name) {
    final Ref<BnfExpression> result = Ref.create(null);
    file.acceptChildren(new PsiRecursiveElementWalkingVisitor() {
      @Override
      public void visitElement(PsiElement element) {
        if (element instanceof BnfRule) {
          BnfRule rule = (BnfRule) element;
          String funcName = ParserGeneratorUtil.getFuncName(rule);
          if (name.startsWith(funcName)) {
            if (name.equals(funcName)) {
              result.set(((BnfRule)element).getExpression());
              stopWalking();
            }
            else if (name.substring(funcName.length()).matches("(?:_\\d+)+")) {
              GrammarUtil.processExpressionNames(rule, funcName, ((BnfRule) element).getExpression(), new PairProcessor<String, BnfExpression>() {
                @Override
                public boolean process(String funcName, BnfExpression expression) {
                  if (name.equals(funcName)) {
                    result.set(expression);
                    return false;
                  }
                  return true;
                }
              });
              stopWalking();
            }
          }
        }
        else if (element instanceof GeneratedParserUtilBase.DummyBlock) {
          super.visitElement(element);
        }
      }
    });
    return result.get();
  }

  @NotNull
  @Override
  public List<ReferenceType> getAllClasses(SourcePosition classPosition) throws NoDataException {
    String parserClass = getParserClass(classPosition);

    List<ReferenceType> referenceTypes = myProcess.getVirtualMachineProxy().classesByName(parserClass);
    if (referenceTypes.isEmpty()) {
      throw new NoDataException();
    }
    return referenceTypes;
  }

  @NotNull
  @Override
  public List<Location> locationsOfLine(ReferenceType type, SourcePosition position) throws NoDataException {
    String parserClass = getParserClass(position);
    int line = getLineNumber(position.getElementAt(), parserClass, 0);
    try {
      return type.locationsOfLine(line + 1);
    }
    catch (AbsentInformationException e) {
      // ignore
    }
    throw new NoDataException();
  }

  private int getLineNumber(PsiElement element, String parserClass, int currentLine) {
    int line = 0;
    AccessToken token = ReadAction.start();
    try {
      BnfRule rule = PsiTreeUtil.getParentOfType(element, BnfRule.class);
      PsiClass aClass = JavaPsiFacade.getInstance(myProcess.getProject()).findClass(parserClass, myProcess.getSearchScope());
      Document document = aClass != null? PsiDocumentManager.getInstance(myProcess.getProject()).getDocument(aClass.getContainingFile()) : null;
      if (rule != null && document != null) {
        return getLineNumber(aClass, document, currentLine, rule, element);
      }
    }
    finally {
      token.finish();
    }
    return line;
  }

  private static int getLineNumber(PsiClass aClass, Document document, int currentLine, BnfRule rule, PsiElement element) {
    String methodName = GrammarUtil.getMethodName(rule, element);
    PsiMethod[] methods = aClass.findMethodsByName(methodName, false);
    PsiCodeBlock body = methods.length == 1? methods[0].getBody() : null;
    PsiStatement[] statements = body != null ? body.getStatements() : PsiStatement.EMPTY_ARRAY;

    BnfExpression expr = PsiTreeUtil.getParentOfType(element, BnfExpression.class, false);
    PsiElement parent = expr != null? expr.getParent() : null;
    if (parent instanceof BnfExpression) {
      int index = ParserGeneratorUtil.getChildExpressions((BnfExpression)parent).indexOf(expr);
      for (int i = 0, len = statements.length, j = 0; i < len; i++) {
        PsiStatement cur = statements[i];
        String text = cur.getText();
        boolean misc = text.startsWith("pinned_") || !text.contains("result_");
        if (currentLine > 0 && currentLine == document.getLineNumber(cur.getTextRange().getStartOffset())) {
          if (misc && index == j ) return currentLine;
        }
        if (misc) continue;
        if (j ++ == index) {
          return document.getLineNumber(cur.getTextRange().getStartOffset());
        }
      }
    }
    if (statements.length > 0) {
      return document.getLineNumber(statements[0].getTextRange().getStartOffset());
    }
    return 0;
  }

  @Override
  public ClassPrepareRequest createPrepareRequest(ClassPrepareRequestor requestor, SourcePosition position) throws NoDataException {
    return myProcess.getRequestsManager().createClassPrepareRequest(requestor, getParserClass(position));
  }

  @NotNull
  private String getParserClass(SourcePosition classPosition) throws NoDataException {
    AccessToken token = ReadAction.start();
    try {
      BnfRule rule = getRuleAt(classPosition);
      String parserClass = ParserGeneratorUtil.getAttribute(rule, KnownAttribute.PARSER_CLASS);
      if (StringUtil.isEmpty(parserClass)) throw new NoDataException();
      return parserClass;
    }
    finally {
      token.finish();
    }
  }

  @NotNull
  private BnfRule getRuleAt(SourcePosition position) throws NoDataException {
    PsiFile file = position.getFile();
    if (!(file instanceof BnfFileImpl)) throw new NoDataException();
    BnfRule rule = PsiTreeUtil.getParentOfType(position.getElementAt(), BnfRule.class);
    if (rule == null) throw new NoDataException();
    return rule;
  }
}
