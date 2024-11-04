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
import consulo.language.psi.*;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.util.IncorrectOperationException;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.Comparing;
import consulo.util.lang.ObjectUtil;
import jakarta.annotation.Nonnull;
import org.intellij.grammar.KnownAttribute;
import org.intellij.grammar.java.JavaHelper;
import org.intellij.grammar.psi.*;

import java.util.List;

import static org.intellij.grammar.generator.ParserGeneratorUtil.*;

/**
 * @author gregsh
 */
public class GrammarPsiImplUtil {
    @Nonnull
    public static PsiReference[] getReferences(BnfListEntry o) {
        BnfAttr attr = PsiTreeUtil.getParentOfType(o, BnfAttr.class);
        if (attr == null || !Comparing.equal(KnownAttribute.METHODS.getName(), attr.getName())) {
            return PsiReference.EMPTY_ARRAY;
        }
        PsiElement id = o.getId();
        BnfLiteralExpression value = o.getLiteralExpression();
        if (id == null || value != null) {
            return PsiReference.EMPTY_ARRAY;
        }
        PsiFile containingFile = o.getContainingFile();
        String version = containingFile instanceof BnfFile bnfFile ? bnfFile.getVersion() : null;

        final String psiImplUtilClass = getRootAttribute(version, attr, KnownAttribute.PSI_IMPL_UTIL_CLASS);
        final JavaHelper javaHelper = JavaHelper.getJavaHelper(o);

        return new PsiReference[]{
            new PsiPolyVariantReferenceBase<>(o, TextRange.from(id.getStartOffsetInParent(), id.getTextLength())) {
                private List<NavigatablePsiElement> getTargetMethods(String methodName) {
                    BnfRule rule = PsiTreeUtil.getParentOfType(getElement(), BnfRule.class);
                    String mixinClass = rule == null ? null : getAttribute(version, rule, KnownAttribute.MIXIN);
                    List<NavigatablePsiElement> implMethods = findRuleImplMethods(version, javaHelper, psiImplUtilClass, methodName, rule);
                    if (!implMethods.isEmpty()) {
                        return implMethods;
                    }
                    List<NavigatablePsiElement> mixinMethods =
                        javaHelper.findClassMethods(version, mixinClass, JavaHelper.MethodType.INSTANCE, methodName, -1);
                    return ContainerUtil.concat(implMethods, mixinMethods);
                }

                @Nonnull
                @Override
                public ResolveResult[] multiResolve(boolean b) {
                    return PsiElementResolveResult.createResults(getTargetMethods(getElement().getText()));
                }

                // TODO [VISTALL] remove that
                //        @Nonnull
                //        @Override
                //        public Object[] getVariants() {
                //          List<LookupElement> list = ContainerUtil.newArrayList();
                //          for (NavigatablePsiElement element : getTargetMethods("*")) {
                //            list.add(LookupElementBuilder.createWithIcon((PsiNamedElement)element));
                //          }
                //          return ArrayUtil.toObjectArray(list);
                //        }

                @Override
                public PsiElement handleElementRename(String newElementName) throws IncorrectOperationException {
                    BnfListEntry element = getElement();
                    PsiElement id = ObjectUtil.assertNotNull(element.getId());
                    id.replace(BnfElementFactory.createLeafFromText(element.getProject(), newElementName));
                    return element;
                }
            }
        };
    }

    @Nonnull
    public static List<BnfExpression> getArguments(@Nonnull BnfExternalExpression expr) {
        List<BnfExpression> expressions = expr.getExpressionList();
        return expressions.subList(1, expressions.size());
    }
}
