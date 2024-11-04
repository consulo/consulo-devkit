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

package org.intellij.grammar.impl.livePreview;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.DefaultLanguageHighlighterColors;
import consulo.colorScheme.TextAttributesKey;
import consulo.language.Language;
import consulo.language.ast.IElementType;
import consulo.language.ast.TokenType;
import consulo.language.editor.highlight.DefaultSyntaxHighlighter;
import consulo.language.editor.highlight.SyntaxHighlighter;
import consulo.language.editor.highlight.SyntaxHighlighterBase;
import consulo.language.editor.highlight.SyntaxHighlighterFactory;
import consulo.language.file.light.LightVirtualFile;
import consulo.language.lexer.Lexer;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author gregsh
 */
@ExtensionImpl
public class LivePreviewSyntaxHighlighterFactory extends SyntaxHighlighterFactory {
    public LivePreviewSyntaxHighlighterFactory() {
    }

    @Nonnull
    @Override
    public SyntaxHighlighter getSyntaxHighlighter(@Nullable final Project project, @Nullable VirtualFile virtualFile) {
        final Language language = virtualFile instanceof LightVirtualFile ? ((LightVirtualFile)virtualFile).getLanguage() : null;
        if (!(language instanceof LivePreviewLanguage)) {
            return new DefaultSyntaxHighlighter();
        }
        return new SyntaxHighlighterBase() {
            @Nonnull
            @Override
            public Lexer getHighlightingLexer() {
                return new LivePreviewLexer(project, (LivePreviewLanguage)language) {
                    @Nullable
                    @Override
                    public IElementType getTokenType() {
                        IElementType tokenType = super.getTokenType();
                        return tokenType instanceof LivePreviewElementType.TokenType livePreviewTokenType
                            ? livePreviewTokenType.delegate : tokenType;
                    }
                };
            }

            @Nonnull
            @Override
            public TextAttributesKey[] getTokenHighlights(IElementType tokenType) {
                if (tokenType == LivePreviewParserDefinition.COMMENT) {
                    return pack(DefaultLanguageHighlighterColors.LINE_COMMENT);
                }
                if (tokenType == LivePreviewParserDefinition.STRING) {
                    return pack(DefaultLanguageHighlighterColors.STRING);
                }
                if (tokenType == LivePreviewParserDefinition.NUMBER) {
                    return pack(DefaultLanguageHighlighterColors.NUMBER);
                }
                if (tokenType == LivePreviewParserDefinition.KEYWORD) {
                    return pack(DefaultLanguageHighlighterColors.KEYWORD);
                }
                if (tokenType == TokenType.BAD_CHARACTER) {
                    return pack(DefaultLanguageHighlighterColors.INVALID_STRING_ESCAPE);
                }
                return EMPTY;
            }
        };
    }

    @Nonnull
    @Override
    public Language getLanguage() {
        return LivePreviewLanguage.BASE_INSTANCE;
    }
}
