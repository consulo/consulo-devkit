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
import consulo.application.Application;
import consulo.application.Result;
import consulo.codeEditor.Editor;
import consulo.codeEditor.ScrollType;
import consulo.component.util.text.UniqueNameGenerator;
import consulo.dataContext.DataContext;
import consulo.document.RangeMarker;
import consulo.document.util.TextRange;
import consulo.language.editor.WriteCommandAction;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.refactoring.action.RefactoringActionHandler;
import consulo.language.editor.refactoring.introduce.inplace.OccurrencesChooser;
import consulo.language.editor.template.*;
import consulo.language.editor.template.event.TemplateEditingAdapter;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.project.Project;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.StringUtil;
import org.intellij.grammar.KnownAttribute;
import org.intellij.grammar.generator.ParserGeneratorUtil;
import org.intellij.grammar.generator.RuleGraphHelper;
import org.intellij.grammar.psi.*;
import org.intellij.grammar.psi.impl.BnfElementFactory;
import org.intellij.grammar.psi.impl.GrammarUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;

/**
 * @author greg
 */
public class BnfIntroduceTokenHandler implements RefactoringActionHandler {
    public static final String REFACTORING_NAME = "Introduce Token";

    @Override
    public void invoke(final @Nonnull Project project, @Nonnull PsiElement[] elements, DataContext dataContext) {
        // do not support this case
    }

    @Override
    @RequiredReadAction
    public void invoke(
        @Nonnull final Project project,
        final Editor editor,
        final PsiFile file,
        @Nullable DataContext dataContext
    ) {
        if (!(file instanceof BnfFile)) {
            return;
        }
        final BnfFile bnfFile = (BnfFile)file;

        final Map<String, String> tokenNameMap = RuleGraphHelper.getTokenNameToTextMap(bnfFile);
        final Map<String, String> tokenTextMap = RuleGraphHelper.getTokenTextToNameMap(bnfFile);

        final String tokenText;
        final String tokenName;
        BnfExpression target = PsiTreeUtil.getParentOfType(
            file.findElementAt(editor.getCaretModel().getOffset()),
            BnfReferenceOrToken.class,
            BnfStringLiteralExpression.class
        );
        if (target instanceof BnfReferenceOrToken) {
            if (bnfFile.getRule(target.getText()) != null) {
                return;
            }
            if (GrammarUtil.isExternalReference(target)) {
                return;
            }
            tokenName = target.getText();
            tokenText = tokenNameMap.get(tokenName);
        }
        else if (target instanceof BnfStringLiteralExpression) {
            if (PsiTreeUtil.getParentOfType(target, BnfAttrs.class) != null) {
                return;
            }
            tokenText = target.getText();
            tokenName = tokenTextMap.get(StringUtil.unquoteString(tokenText));
        }
        else {
            return;
        }

        final List<BnfExpression> allOccurrences = ContainerUtil.newArrayList();
        final Map<OccurrencesChooser.ReplaceChoice, List<BnfExpression>> occurrencesMap = new LinkedHashMap<>();
        occurrencesMap.put(OccurrencesChooser.ReplaceChoice.NO, Collections.singletonList(target));
        occurrencesMap.put(OccurrencesChooser.ReplaceChoice.ALL, allOccurrences);

        BnfVisitor visitor = new BnfVisitor<Void>() {
            @Override
            public Void visitStringLiteralExpression(@Nonnull BnfStringLiteralExpression o) {
                if (tokenText != null && tokenText.equals(o.getText())) {
                    allOccurrences.add(o);
                }
                return null;
            }

            @Override
            public Void visitReferenceOrToken(@Nonnull BnfReferenceOrToken o) {
                if (GrammarUtil.isExternalReference(o)) {
                    return null;
                }
                if (tokenName != null && tokenName.equals(o.getText())) {
                    allOccurrences.add(o);
                }
                return null;
            }
        };
        for (PsiElement o : GrammarUtil.bnfTraverserNoAttrs(file)) {
            o.accept(visitor);
        }

        if (occurrencesMap.get(OccurrencesChooser.ReplaceChoice.ALL).size() <= 1 && !Application.get().isUnitTestMode()) {
            occurrencesMap.remove(OccurrencesChooser.ReplaceChoice.ALL);
        }

        final Consumer<OccurrencesChooser.ReplaceChoice> callback = choice -> new WriteCommandAction(project, REFACTORING_NAME, file) {
            @Override
            protected void run(@Nonnull Result result) throws Throwable {
                buildTemplateAndRun(project, editor, bnfFile, occurrencesMap.get(choice), tokenName, tokenText, tokenNameMap.keySet());
            }
        }.execute();
        if (Application.get().isUnitTestMode()) {
            callback.accept(OccurrencesChooser.ReplaceChoice.ALL);
        }
        else {
            new OccurrencesChooser<BnfExpression>(editor) {
                @Override
                protected TextRange getOccurrenceRange(BnfExpression occurrence) {
                    return occurrence.getTextRange();
                }
            }.showChooser(callback, occurrencesMap);
        }
    }

    @RequiredReadAction
    private void buildTemplateAndRun(
        final Project project,
        final Editor editor,
        BnfFile bnfFile, List<BnfExpression> occurrences,
        String tokenName,
        String tokenText,
        Set<String> tokenNames
    ) {
        BnfListEntry entry = addTokenDefinition(project, bnfFile, tokenName, tokenText, tokenNames);
        PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.getDocument());

        TemplateBuilder builder = TemplateBuilderFactory.getInstance().createTemplateBuilder(bnfFile);
        PsiElement tokenId = ObjectUtil.assertNotNull(entry.getId());
        PsiElement tokenValue = ObjectUtil.assertNotNull(entry.getLiteralExpression());
        if (tokenName == null) {
            builder.replaceElement(tokenId, "TokenName", new TextExpression(tokenId.getText()), true);
        }
        builder.replaceElement(tokenValue, "TokenText", new TextExpression(tokenValue.getText()), true);

        for (BnfExpression occurrence : occurrences) {
            builder.replaceElement(
                occurrence,
                "Other",
                new Expression() {
                    @Nullable
                    @Override
                    public consulo.language.editor.template.Result calculateResult(ExpressionContext context) {
                        TemplateState state = TemplateManager.getInstance(project).getTemplateState(context.getEditor());
                        assert state != null;
                        TextResult text = ObjectUtil.assertNotNull(state.getVariableValue("TokenText"));
                        String curText = StringUtil.unquoteString(text.getText());
                        return ParserGeneratorUtil.isRegexpToken(curText)
                            ? state.getVariableValue("TokenName")
                            : new TextResult("'" + curText + "'");
                    }

                    @Nullable
                    @Override
                    public consulo.language.editor.template.Result calculateQuickResult(ExpressionContext context) {
                        return calculateResult(context);
                    }

                    @Nullable
                    @Override
                    public LookupElement[] calculateLookupItems(ExpressionContext context) {
                        return LookupElement.EMPTY_ARRAY;
                    }
                },
                false
            );
        }
        final RangeMarker caretMarker = editor.getDocument().createRangeMarker(0, editor.getCaretModel().getOffset());
        caretMarker.setGreedyToRight(true);
        editor.getCaretModel().moveToOffset(0);
        Template template = builder.buildInlineTemplate();
        template.setToReformat(false);
        TemplateManager.getInstance(project).startTemplate(
            editor,
            template,
            new TemplateEditingAdapter() {
                @Override
                public void templateFinished(Template template, boolean brokenOff) {
                    handleTemplateFinished(project, editor, caretMarker);
                }

                @Override
                public void templateCancelled(Template template) {
                    handleTemplateFinished(project, editor, caretMarker);
                }
            }
        );
    }

    private void handleTemplateFinished(Project project, Editor editor, RangeMarker caretMarker) {
        editor.getCaretModel().moveToOffset(caretMarker.getEndOffset());
        editor.getScrollingModel().scrollToCaret(ScrollType.MAKE_VISIBLE);
    }

    @RequiredReadAction
    private static BnfListEntry addTokenDefinition(
        Project project,
        BnfFile bnfFile,
        String tokenName,
        String tokenText,
        Set<String> tokenNames
    ) {
        String fixedTokenName =
            new UniqueNameGenerator(tokenNames, null).generateUniqueName(StringUtil.notNullize(tokenName, "token"));
        String newAttrText = "tokens = [\n    " + fixedTokenName + "=" + StringUtil.notNullize(tokenText, "\"\"") + "\n  ]";
        BnfAttr newAttr = BnfElementFactory.createAttributeFromText(project, newAttrText);
        BnfAttrs attrs = ContainerUtil.getFirstItem(bnfFile.getAttributes());
        BnfAttr tokensAttr = null;
        if (attrs == null) {
            attrs = (BnfAttrs)bnfFile.addAfter(newAttr.getParent(), null);
            tokensAttr = attrs.getAttrList().get(0);
            return ((BnfValueList)tokensAttr.getExpression()).getListEntryList().get(0);
        }
        else {
            for (BnfAttr attr : attrs.getAttrList()) {
                if (KnownAttribute.TOKENS.getName().equals(attr.getName())) {
                    tokensAttr = attr;
                }
            }
            if (tokensAttr == null) {
                List<BnfAttr> attrList = attrs.getAttrList();
                PsiElement anchor = attrList.isEmpty() ? attrs.getFirstChild() : attrList.get(attrList.size() - 1);
                newAttr = (BnfAttr)attrs.addAfter(newAttr, anchor);
                attrs.addAfter(BnfElementFactory.createLeafFromText(project, "\n  "), anchor);
                return ((BnfValueList)newAttr.getExpression()).getListEntryList().get(0);
            }
            else {
                BnfExpression expression = tokensAttr.getExpression();
                List<BnfListEntry> entryList = expression instanceof BnfValueList bnfValueList ? bnfValueList.getListEntryList() : null;
                if (entryList == null || entryList.isEmpty()) {
                    expression.replace(newAttr.getParent());
                    return ((BnfValueList)tokensAttr.getExpression()).getListEntryList().get(0);
                }
                else {
                    for (BnfListEntry entry : entryList) {
                        PsiElement id = entry.getId();
                        if (id != null && id.getText().equals(tokenName)) {
                            return entry;
                        }
                    }
                    BnfListEntry newValue = ((BnfValueList)newAttr.getExpression()).getListEntryList().get(0);
                    PsiElement anchor = entryList.get(entryList.size() - 1);
                    newValue = (BnfListEntry)expression.addAfter(newValue, anchor);
                    expression.addAfter(BnfElementFactory.createLeafFromText(project, "\n    "), anchor);
                    return newValue;
                }
            }
        }
    }
}

