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

import consulo.language.ast.ASTNode;
import consulo.language.parser.ParserDefinition;
import consulo.language.lexer.Lexer;
import consulo.language.file.FileViewProvider;
import consulo.language.ast.TokenType;
import consulo.language.ast.IElementType;
import consulo.language.ast.IFileElementType;
import consulo.language.ast.TokenSet;
import consulo.language.version.LanguageVersion;
import consulo.language.impl.psi.ASTWrapperPsiElement;
import consulo.language.impl.psi.PsiFileBase;
import consulo.language.parser.PsiParser;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.virtualFileSystem.fileType.FileType;
import jakarta.annotation.Nonnull;

/**
 * @author gregsh
 */
public class LivePreviewParserDefinition implements ParserDefinition {
    public static final IElementType COMMENT = new IElementType("COMMENT", LivePreviewLanguage.BASE_INSTANCE);
    public static final IElementType STRING = new IElementType("STRING", LivePreviewLanguage.BASE_INSTANCE);
    public static final IElementType NUMBER = new IElementType("NUMBER", LivePreviewLanguage.BASE_INSTANCE);
    public static final IElementType KEYWORD = new IElementType("KEYWORD", LivePreviewLanguage.BASE_INSTANCE);

    private static final TokenSet ourWhiteSpaceTokens = TokenSet.create(TokenType.WHITE_SPACE);
    private static final TokenSet ourCommentTokens = TokenSet.create(COMMENT);
    private static final TokenSet ourStringLiteralElements = TokenSet.create(STRING);

    private final LivePreviewLanguage myLanguage;
    private final IFileElementType myFileElementType;

    public LivePreviewParserDefinition(LivePreviewLanguage language) {
        myLanguage = language;
        myFileElementType = new IFileElementType(myLanguage); // todo do not register
    }

    public LivePreviewLanguage getLanguage() {
        return myLanguage;
    }

    @Nonnull
    @Override
    public Lexer createLexer(LanguageVersion languageVersion) {
        return new LivePreviewLexer(null, myLanguage);
    }

    @Override
    public PsiParser createParser(LanguageVersion languageVersion) {
        return new LivePreviewParser(null, myLanguage);
    }

    @Override
    public IFileElementType getFileNodeType() {
        return myFileElementType;
    }

    @Nonnull
    @Override
    public TokenSet getWhitespaceTokens(LanguageVersion languageVersion) {
        return ourWhiteSpaceTokens;
    }

    @Nonnull
    @Override
    public TokenSet getCommentTokens(LanguageVersion languageVersion) {
        return ourCommentTokens;
    }

    @Nonnull
    @Override
    public TokenSet getStringLiteralElements(LanguageVersion languageVersion) {
        return ourStringLiteralElements;
    }

    @Nonnull
    @Override
    public PsiElement createElement(ASTNode node) {
        return new ASTWrapperPsiElement(node);
    }

    @Override
    public PsiFile createFile(FileViewProvider viewProvider) {
        return new PsiFileBase(viewProvider, myLanguage) {
            @Nonnull
            @Override
            public FileType getFileType() {
                return LivePreviewFileType.INSTANCE;
            }
        };
    }

    @Override
    public SpaceRequirements spaceExistanceTypeBetweenTokens(ASTNode left, ASTNode right) {
        return SpaceRequirements.MAY;
    }
}
