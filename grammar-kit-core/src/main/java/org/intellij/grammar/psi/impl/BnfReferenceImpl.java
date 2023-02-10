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
package org.intellij.grammar.psi.impl;

import consulo.document.util.TextRange;
import consulo.language.psi.NavigatablePsiElement;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiReferenceBase;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.util.collection.ContainerUtil;
import org.intellij.grammar.KnownAttribute;
import org.intellij.grammar.generator.ParserGeneratorUtil;
import org.intellij.grammar.java.JavaHelper;
import org.intellij.grammar.psi.*;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * @author gregsh
 */
public class BnfReferenceImpl<T extends BnfComposite> extends PsiReferenceBase<T> {

  public BnfReferenceImpl(@Nonnull T element, TextRange range) {
    super(element, range);
  }

  @Override
  public PsiElement resolve() {
    PsiFile containingFile = myElement.getContainingFile();
    String referenceName = getRangeInElement().substring(myElement.getText());
    PsiElement result = containingFile instanceof BnfFile ? ((BnfFile)containingFile).getRule(referenceName) : null;
    String version = containingFile instanceof BnfFile ? ((BnfFile)containingFile).getVersion() : null;
    if (result == null && GrammarUtil.isExternalReference(myElement)) {
      PsiElement parent = myElement.getParent();
      int paramCount = parent instanceof BnfSequence ? ((BnfSequence)parent).getExpressionList().size() - 1 :
        parent instanceof BnfExternalExpression ? ((BnfExternalExpression)parent).getExpressionList().size() - 1 : 0;
      BnfRule rule = PsiTreeUtil.getParentOfType(myElement, BnfRule.class);
      String parserClass = ParserGeneratorUtil.getAttribute(version, rule, KnownAttribute.PARSER_UTIL_CLASS);
      // paramCount + 2 (builder and level)
      JavaHelper helper = JavaHelper.getJavaHelper(myElement);
      for (String className = parserClass; className != null; className = helper.getSuperClassName(className)) {
        List<NavigatablePsiElement> methods =
          helper.findClassMethods(version, className, JavaHelper.MethodType.STATIC, referenceName, paramCount + 2);
        result = ContainerUtil.getFirstItem(methods);
        if (result != null) break;
      }
    }
    return result;
  }

  // TODO [VISTALL] remove that

//  @Nonnull
//  @Override
//  public Object[] getVariants() {
//    List<LookupElement> list = ContainerUtil.newArrayList();
//    boolean isExternal = GrammarUtil.isExternalReference(myElement);
//
//    if (!isExternal || PsiTreeUtil.getParentOfType(myElement, BnfExternalExpression.class) != null) {
//      PsiFile containingFile = myElement.getContainingFile();
//      List<BnfRule> rules = containingFile instanceof BnfFile ? ((BnfFile)containingFile).getRules() : Collections.<BnfRule>emptyList();
//      for (BnfRule rule : rules) {
//        boolean fakeRule = ParserGeneratorUtil.Rule.isFake(rule);
//        boolean privateRule = ParserGeneratorUtil.Rule.isPrivate(rule);
//        if (isExternal && !ParserGeneratorUtil.Rule.isMeta(rule)) continue;
//        String idText = rule.getId().getText();
//        LookupElementBuilder e = LookupElementBuilder.create(rule, idText)
//          .withIcon(IconDescriptorUpdaters.getIcon(rule, 0))
//          .withBoldness(!privateRule)
//          .withStrikeoutness(fakeRule);
//        if (!Comparing.equal(idText, rule.getName())) {
//          e = e.withLookupString(rule.getName());
//        }
//        list.add(e);
//      }
//    }
//    if (isExternal) {
//      BnfRule rule = PsiTreeUtil.getParentOfType(myElement, BnfRule.class);
//      String parserClass = ParserGeneratorUtil.getAttribute(rule, KnownAttribute.PARSER_UTIL_CLASS);
//      if (StringUtil.isNotEmpty(parserClass)) {
//        JavaHelper helper = JavaHelper.getJavaHelper(myElement);
//        for (String className = parserClass; className != null; className = helper.getSuperClassName(className)) {
//          for (NavigatablePsiElement element : helper.findClassMethods(className, JavaHelper.MethodType.STATIC, "*", -1, BnfConstants.PSI_BUILDER_CLASS, "int")) {
//            List<String> methodTypes = helper.getMethodTypes(element);
//            if ("boolean".equals(ContainerUtil.getFirstItem(methodTypes))) {
//              list.add(LookupElementBuilder.createWithIcon((PsiNamedElement)element));
//            }
//          }
//        }
//      }
//    }
//    return ArrayUtil.toObjectArray(list);
//  }

}
