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

import consulo.annotation.component.ExtensionImpl;
import consulo.language.ast.TokenType;
import consulo.language.editor.action.FileQuoteHandler;
import consulo.language.editor.action.SimpleTokenSetQuoteHandler;
import consulo.virtualFileSystem.fileType.FileType;
import org.intellij.grammar.BnfFileType;
import org.intellij.grammar.psi.BnfTypes;

import javax.annotation.Nonnull;

/**
 * @author gregsh
 */
@ExtensionImpl
public class BnfQuoteHandler extends SimpleTokenSetQuoteHandler implements FileQuoteHandler {
    public BnfQuoteHandler() {
        super(BnfTypes.BNF_STRING, TokenType.BAD_CHARACTER);
    }

    @Nonnull
    @Override
    public FileType getFileType() {
        return BnfFileType.INSTANCE;
    }
}
