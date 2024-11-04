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

package org.intellij.grammar.impl.refactor;

import consulo.annotation.access.RequiredReadAction;
import consulo.language.editor.refactoring.BaseRefactoringProcessor;
import consulo.language.editor.refactoring.util.CommonRefactoringUtil;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiRecursiveElementWalkingVisitor;
import consulo.language.psi.PsiReference;
import consulo.language.psi.search.ReferencesSearch;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.util.IncorrectOperationException;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.usage.UsageInfo;
import consulo.usage.UsageViewDescriptor;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.primitive.objects.ObjectIntMap;
import consulo.util.collection.primitive.objects.ObjectMaps;
import consulo.util.lang.Couple;
import org.intellij.grammar.generator.ParserGeneratorUtil;
import org.intellij.grammar.psi.*;
import org.intellij.grammar.psi.impl.BnfElementFactory;
import org.intellij.grammar.psi.impl.GrammarUtil;

import jakarta.annotation.Nonnull;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * Date: 8/11/11
 * Time: 4:19 PM
 *
 * @author Vadim Romansky
 */
public class BnfInlineRuleProcessor extends BaseRefactoringProcessor {
    private static final Logger LOG = Logger.getInstance(BnfInlineRuleProcessor.class);
    private BnfRule myRule;
    private final PsiReference myReference;
    private final boolean myInlineThisOnly;

    public BnfInlineRuleProcessor(BnfRule rule, Project project, PsiReference ref, boolean isInlineThisOnly) {
        super(project);
        myRule = rule;
        myReference = ref;
        myInlineThisOnly = isInlineThisOnly;
    }

    @Nonnull
    protected UsageViewDescriptor createUsageViewDescriptor(@Nonnull UsageInfo[] usages) {
        return new BnfInlineViewDescriptor(myRule);
    }

    @Nonnull
    protected String getCommandName() {
        return "Inline rule '" + myRule.getName() + "'";
    }

    @Nonnull
    @RequiredReadAction
    protected UsageInfo[] findUsages() {
        if (myInlineThisOnly) {
            return new UsageInfo[]{new UsageInfo(myReference.getElement())};
        }

        List<UsageInfo> result = ContainerUtil.newArrayList();
        for (PsiReference reference : ReferencesSearch.search(myRule, myRule.getUseScope(), false)) {
            PsiElement element = reference.getElement();
            if (GrammarUtil.isInAttributesReference(element)) {
                continue;
            }
            result.add(new UsageInfo(element));
        }
        return result.toArray(new UsageInfo[result.size()]);
    }

    protected void refreshElements(PsiElement[] elements) {
        LOG.assertTrue(elements.length == 1 && elements[0] instanceof BnfRule);
        myRule = (BnfRule)elements[0];
    }

    @RequiredReadAction
    protected void performRefactoring(@Nonnull UsageInfo[] usages) {
        BnfExpression expression = myRule.getExpression();
        boolean meta = ParserGeneratorUtil.Rule.isMeta(myRule);

        CommonRefactoringUtil.sortDepthFirstRightLeftOrder(usages);
        for (UsageInfo info : usages) {
            try {
                final BnfExpression element = (BnfExpression)info.getElement();
                boolean metaRuleRef = GrammarUtil.isExternalReference(element);
                if (meta && metaRuleRef) {
                    inlineMetaRuleUsage(element, expression);
                }
                else if (!meta && !metaRuleRef) {
                    inlineExpressionUsage(element, expression);
                }
            }
            catch (IncorrectOperationException e) {
                LOG.error(e);
            }
        }

        if (!myInlineThisOnly) {
            try {
                myRule.delete();
            }
            catch (IncorrectOperationException e) {
                LOG.error(e);
            }
        }
    }

    @RequiredReadAction
    private static void inlineExpressionUsage(BnfExpression place, BnfExpression ruleExpr) throws IncorrectOperationException {
        BnfExpression replacement = BnfElementFactory.createExpressionFromText(ruleExpr.getProject(), '(' + ruleExpr.getText() + ')');
        BnfExpressionOptimizer.optimize(place.replace(replacement));
    }

    @RequiredReadAction
    private static void inlineMetaRuleUsage(BnfExpression place, BnfExpression expression) {
        BnfRule rule = PsiTreeUtil.getParentOfType(place, BnfRule.class);
        PsiElement parent = place.getParent();
        final List<BnfExpression> expressionList;
        if (parent instanceof BnfExternalExpression) {
            expressionList = ((BnfExternalExpression)parent).getExpressionList();
        }
        else if (parent instanceof BnfSequence) {
            expressionList = ((BnfSequence)parent).getExpressionList();
        }
        else if (parent instanceof BnfRule) {
            expressionList = Collections.emptyList();
        }
        else {
            LOG.error(parent);
            return;
        }
        final ObjectIntMap<String> visited = ObjectMaps.newObjectIntHashMap();
        final LinkedList<Couple<PsiElement>> work = new LinkedList<>();
        (expression = (BnfExpression)expression.copy()).acceptChildren(new PsiRecursiveElementWalkingVisitor() {
            @Override
            public void visitElement(PsiElement element) {
                if (element instanceof BnfExternalExpression) {
                    List<BnfExpression> list = ((BnfExternalExpression)element).getExpressionList();
                    if (list.size() == 1) {
                        String text = list.get(0).getText();
                        int idx = visited.getInt(text);
                        if (idx == 0) {
                            visited.putInt(text, idx = visited.size() + 1);
                        }
                        if (idx < expressionList.size()) {
                            work.addFirst(Couple.of(element, expressionList.get(idx)));
                        }
                    }
                }
                else {
                    super.visitElement(element);
                }
            }
        });
        for (Couple<PsiElement> pair : work) {
            BnfExpressionOptimizer.optimize(pair.first.replace(pair.second));
        }
        inlineExpressionUsage((BnfExpression)parent, expression);
        if (!(parent instanceof BnfExternalExpression)) {
            for (BnfModifier modifier : rule.getModifierList()) {
                if (modifier.getText().equals("external")) {
                    modifier.getNextSibling().delete(); // whitespace
                    modifier.delete();
                    break;
                }
            }
        }
    }
}
