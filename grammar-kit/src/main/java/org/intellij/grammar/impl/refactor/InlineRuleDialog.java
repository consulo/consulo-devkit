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

import consulo.language.editor.refactoring.inline.InlineOptionsDialog;
import consulo.language.psi.ElementDescriptionUtil;
import consulo.language.psi.PsiReference;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.usage.UsageViewNodeTextLocation;
import org.intellij.grammar.psi.BnfRule;

/**
 * Created by IntelliJ IDEA.
 * Date: 8/11/11
 * Time: 4:04 PM
 *
 * @author Vadim Romansky
 */
public class InlineRuleDialog extends InlineOptionsDialog {
    private final PsiReference myReference;

    private final BnfRule myRule;

    public InlineRuleDialog(Project project, BnfRule rule, PsiReference ref) {
        super(project, true, rule);
        myRule = rule;
        myReference = ref;
        myInvokedOnReference = myReference != null;

        setTitle("Inline Rule");

        init();
    }

    @Override
    protected LocalizeValue getNameLabelText() {
        return LocalizeValue.localizeTODO(ElementDescriptionUtil.getElementDescription(myElement, UsageViewNodeTextLocation.INSTANCE));
    }

    @Override
    protected LocalizeValue getBorderTitle() {
        return LocalizeValue.localizeTODO("Inline");
    }

    @Override
    protected LocalizeValue getInlineThisText() {
        return LocalizeValue.localizeTODO("&This reference only and keep the rule");
    }

    @Override
    protected LocalizeValue getInlineAllText() {
        return LocalizeValue.localizeTODO("&All references and remove the rule");
    }

    @Override
    protected boolean isInlineThis() {
        return false;
    }

    @Override
    protected void doAction() {
        invokeRefactoring(new BnfInlineRuleProcessor(myRule, getProject(), myReference, isInlineThisOnly()));
    }
}
