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
package org.intellij.grammar;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.ast.*;
import consulo.language.file.FileViewProvider;
import consulo.language.lexer.Lexer;
import consulo.language.parser.ParserDefinition;
import consulo.language.parser.PsiParser;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.version.LanguageVersion;
import jakarta.annotation.Nonnull;
import org.intellij.grammar.parser.BnfLexer;
import org.intellij.grammar.parser.GrammarParser;
import org.intellij.grammar.psi.BnfTypes;
import org.intellij.grammar.psi.impl.BnfFileImpl;
import org.intellij.grammar.psi.impl.BnfTypesFactory;

/**
 * User: gregory
 * Date: 13.07.11
 * Time: 22:43
 */
@ExtensionImpl
public class BnfParserDefinition implements ParserDefinition {
    public static final IFileElementType BNF_FILE_ELEMENT_TYPE = new IFileElementType("BNF_FILE", BnfLanguage.INSTANCE);
    public static final TokenSet WS = TokenSet.create(TokenType.WHITE_SPACE);
    public static final IElementType BNF_LINE_COMMENT = BnfTypes.BNF_LINE_COMMENT;
    public static final IElementType BNF_BLOCK_COMMENT = BnfTypes.BNF_BLOCK_COMMENT;
    public static final TokenSet COMMENTS = TokenSet.create(BNF_LINE_COMMENT, BNF_BLOCK_COMMENT);
    public static final TokenSet LITERALS = TokenSet.create(BnfTypes.BNF_STRING);

    @Nonnull
    @Override
    public Language getLanguage() {
        return BnfLanguage.INSTANCE;
    }

    @Nonnull
    @Override
    public Lexer createLexer(LanguageVersion languageVersion) {
        return new BnfLexer();
    }

    @Override
    public PsiParser createParser(LanguageVersion languageVersion) {
        return new GrammarParser();
    }

    @Override
    public IFileElementType getFileNodeType() {
        return BNF_FILE_ELEMENT_TYPE;
    }

    @Nonnull
    @Override
    public TokenSet getWhitespaceTokens(LanguageVersion languageVersion) {
        return WS;
    }

    @Nonnull
    @Override
    public TokenSet getCommentTokens(LanguageVersion languageVersion) {
        return COMMENTS;
    }

    @Nonnull
    @Override
    public TokenSet getStringLiteralElements(LanguageVersion languageVersion) {
        return LITERALS;
    }

    @Nonnull
    @Override
    public PsiElement createElement(ASTNode astNode) {
        return BnfTypesFactory.createElement(astNode);
    }

    @Override
    public PsiFile createFile(FileViewProvider fileViewProvider) {
        return new BnfFileImpl(fileViewProvider);
    }

    @Override
    public SpaceRequirements spaceExistanceTypeBetweenTokens(ASTNode astNode, ASTNode astNode1) {
        return SpaceRequirements.MAY;
    }
}
