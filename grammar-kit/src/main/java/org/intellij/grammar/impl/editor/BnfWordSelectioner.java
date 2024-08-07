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

package org.intellij.grammar.impl.editor;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiElement;
import consulo.language.impl.psi.LeafPsiElement;
import consulo.codeEditor.Editor;
import consulo.language.editor.action.BraceMatcherBasedSelectioner;
import org.intellij.grammar.psi.BnfComposite;
import org.intellij.grammar.psi.BnfTypes;

import java.util.List;

/**
 * @author gregsh
 */
@ExtensionImpl
public class BnfWordSelectioner extends BraceMatcherBasedSelectioner {
    @Override
    public boolean canSelect(PsiElement e) {
        return e instanceof BnfComposite
            || e instanceof LeafPsiElement leafPsiElement && leafPsiElement.getElementType() == BnfTypes.BNF_STRING;
    }

    @Override
    @RequiredReadAction
    public List<TextRange> select(PsiElement e, CharSequence editorText, int cursorOffset, Editor editor) {
        List<TextRange> list = super.select(e, editorText, cursorOffset, editor);
        if (e instanceof LeafPsiElement leafPsiElement && leafPsiElement.getElementType() == BnfTypes.BNF_STRING) {
            TextRange range = e.getTextRange();
            list.add(TextRange.from(range.getStartOffset() + 1, range.getLength() - 2));
        }
        return list;
    }
}
