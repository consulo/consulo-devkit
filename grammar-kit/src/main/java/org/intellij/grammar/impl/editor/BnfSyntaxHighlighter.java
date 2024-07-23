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

import consulo.codeEditor.DefaultLanguageHighlighterColors;
import consulo.colorScheme.TextAttributesKey;
import consulo.colorScheme.EffectType;
import consulo.colorScheme.TextAttributes;
import consulo.language.ast.IElementType;
import consulo.language.editor.highlight.SyntaxHighlighterBase;
import consulo.language.ast.TokenType;
import consulo.language.lexer.Lexer;
import org.intellij.grammar.BnfParserDefinition;
import org.intellij.grammar.parser.BnfLexer;

import javax.annotation.Nonnull;

import static consulo.colorScheme.TextAttributesKey.createTextAttributesKey;
import static org.intellij.grammar.psi.BnfTypes.*;

/**
 * @author gregsh
 */
class BnfSyntaxHighlighter extends SyntaxHighlighterBase {
    public static final TextAttributesKey
        ILLEGAL = createTextAttributesKey("BNF_ILLEGAL", DefaultLanguageHighlighterColors.INVALID_STRING_ESCAPE),
        COMMENT = createTextAttributesKey("BNF_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT),
        STRING = createTextAttributesKey("BNF_STRING", DefaultLanguageHighlighterColors.STRING),
        PATTERN = createTextAttributesKey("BNF_PATTERN", DefaultLanguageHighlighterColors.INSTANCE_FIELD),
        NUMBER = createTextAttributesKey("BNF_NUMBER", DefaultLanguageHighlighterColors.NUMBER),
        KEYWORD = createTextAttributesKey("BNF_KEYWORD", DefaultLanguageHighlighterColors.MARKUP_ATTRIBUTE),
        TOKEN = createTextAttributesKey("BNF_TOKEN", DefaultLanguageHighlighterColors.STRING),
        RULE = createTextAttributesKey("BNF_RULE", DefaultLanguageHighlighterColors.KEYWORD),
        META_RULE = createTextAttributesKey("BNF_META_RULE", DefaultLanguageHighlighterColors.KEYWORD),
        META_PARAM = createTextAttributesKey("BNF_META_RULE_PARAM"),
        ATTRIBUTE = createTextAttributesKey("BNF_ATTRIBUTE", DefaultLanguageHighlighterColors.INTERFACE_NAME),
        EXTERNAL = createTextAttributesKey("BNF_EXTERNAL", DefaultLanguageHighlighterColors.STATIC_METHOD),
        PARENTHS = createTextAttributesKey("BNF_PARENTHS", DefaultLanguageHighlighterColors.PARENTHESES),
        BRACES = createTextAttributesKey("BNF_BRACES", DefaultLanguageHighlighterColors.BRACES),
        BRACKETS = createTextAttributesKey("BNF_BRACKETS", DefaultLanguageHighlighterColors.BRACKETS),
        ANGLES = createTextAttributesKey("BNF_ANGLES", DefaultLanguageHighlighterColors.PARENTHESES),
        OP_SIGN = createTextAttributesKey("BNF_OP_SIGN", DefaultLanguageHighlighterColors.OPERATION_SIGN),
        RECOVER_MARKER = createTextAttributesKey("BNF_RECOVER_MARKER"),
        PIN_MARKER = createTextAttributesKey(
            "BNF_PIN",
            new TextAttributes(null,
                null,
                DefaultLanguageHighlighterColors.LINE_COMMENT.getDefaultAttributes().getForegroundColor(),
                EffectType.BOLD_DOTTED_LINE,
                0
            )
        );

    @Nonnull
    @Override
    public Lexer getHighlightingLexer() {
        return new BnfLexer();
    }

    @Nonnull
    @Override
    public TextAttributesKey[] getTokenHighlights(IElementType type) {
        if (type == TokenType.BAD_CHARACTER) {
            return pack(ILLEGAL);
        }
        if (type == BnfParserDefinition.BNF_LINE_COMMENT || type == BnfParserDefinition.BNF_BLOCK_COMMENT) {
            return pack(COMMENT);
        }
        if (type == BNF_STRING) {
            return pack(STRING);
        }
        if (type == BNF_NUMBER) {
            return pack(NUMBER);
        }
        if (type == BNF_OP_ONEMORE || type == BNF_OP_AND || type == BNF_OP_EQ || type == BNF_OP_IS ||
            type == BNF_OP_NOT || type == BNF_OP_OPT || type == BNF_OP_OR || type == BNF_OP_ZEROMORE) {
            return pack(OP_SIGN);
        }
        if (type == BNF_LEFT_PAREN || type == BNF_RIGHT_PAREN) {
            return pack(PARENTHS);
        }
        if (type == BNF_LEFT_BRACE || type == BNF_RIGHT_BRACE) {
            return pack(BRACES);
        }
        if (type == BNF_LEFT_BRACKET || type == BNF_RIGHT_BRACKET) {
            return pack(BRACKETS);
        }
        if (type == BNF_EXTERNAL_START || type == BNF_EXTERNAL_END) {
            return pack(ANGLES);
        }
        return EMPTY;
    }
}
