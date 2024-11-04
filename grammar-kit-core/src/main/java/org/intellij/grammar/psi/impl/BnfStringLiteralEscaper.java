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

import consulo.document.util.ProperTextRange;
import consulo.document.util.TextRange;
import consulo.language.psi.LiteralTextEscaper;

import jakarta.annotation.Nonnull;

/**
 * @author gregsh
 */
public class BnfStringLiteralEscaper extends LiteralTextEscaper<BnfStringImpl> {
    public BnfStringLiteralEscaper(BnfStringImpl element) {
        super(element);
    }

    @Override
    public boolean decode(@Nonnull final TextRange rangeInsideHost, @Nonnull final StringBuilder outChars) {
        // todo implement proper java-like string escapes support
        ProperTextRange.assertProperRange(rangeInsideHost);
        outChars.append(myHost.getText(), rangeInsideHost.getStartOffset(), rangeInsideHost.getEndOffset());
        return true;
    }

    @Override
    public int getOffsetInHost(final int offsetInDecoded, @Nonnull final TextRange rangeInsideHost) {
        ProperTextRange.assertProperRange(rangeInsideHost);
        int offset = offsetInDecoded;
        // todo implement proper java-like string escapes support
        offset += rangeInsideHost.getStartOffset();
        if (offset < rangeInsideHost.getStartOffset()) {
            offset = rangeInsideHost.getStartOffset();
        }
        if (offset > rangeInsideHost.getEndOffset()) {
            offset = rangeInsideHost.getEndOffset();
        }
        return offset;
    }

    @Override
    public boolean isOneLine() {
        return true;
    }
}

