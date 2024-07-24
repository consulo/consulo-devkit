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

import consulo.application.util.CachedValueProvider;
import consulo.language.ast.IElementType;
import consulo.language.ast.TokenType;
import consulo.language.lexer.LexerBase;
import consulo.language.psi.util.LanguageCachedValueUtil;
import consulo.project.Project;
import consulo.util.lang.StringUtil;
import org.intellij.grammar.KnownAttribute;
import org.intellij.grammar.generator.Case;
import org.intellij.grammar.generator.ParserGeneratorUtil;
import org.intellij.grammar.psi.BnfFile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.intellij.grammar.generator.ParserGeneratorUtil.getRootAttribute;
import static org.intellij.grammar.impl.livePreview.LivePreviewParserDefinition.*;

/**
 * @author gregsh
 */
public class LivePreviewLexer extends LexerBase {
    private CharSequence myBuffer;
    private int myEndOffset;
    private int myPosition;
    private int myTokenEnd;
    private IElementType myTokenType;

    private final Token[] myTokens;
    private Matcher[] myMatchers;

    public LivePreviewLexer(Project project, final LivePreviewLanguage language) {
        final BnfFile bnfFile = language.getGrammar(project);

        myTokens = bnfFile == null ? new Token[0] : LanguageCachedValueUtil.getCachedValue(bnfFile, new CachedValueProvider<Token[]>() {
            @Nullable
            @Override
            public Result<Token[]> compute() {
                Set<String> usedInGrammar = new LinkedHashSet<>();
                Map<String, String> map = collectTokenPattern2Name(bnfFile, usedInGrammar);

                Token[] tokens = new Token[map.size()];
                int i = 0;
                String tokenConstantPrefix = getRootAttribute(bnfFile.getVersion(), bnfFile, KnownAttribute.ELEMENT_TYPE_PREFIX);
                for (String pattern : map.keySet()) {
                    String tokenName = map.get(pattern);

                    tokens[i++] = new Token(pattern, tokenName, usedInGrammar.contains(tokenName), tokenConstantPrefix, language);
                }
                return Result.create(tokens, bnfFile);
            }
        });
    }

    @Override
    public void start(@Nonnull CharSequence buffer, int startOffset, int endOffset, int initialState) {
        myBuffer = buffer;
        myEndOffset = endOffset;
        myPosition = startOffset;
        myTokenEnd = myPosition;
        myTokenType = null;
        myMatchers = new Matcher[myTokens.length];
        for (int i = 0; i < myMatchers.length; i++) {
            Pattern pattern = myTokens[i].pattern;
            if (pattern == null) {
                continue;
            }
            myMatchers[i] = pattern.matcher(buffer);
        }
        nextToken();
    }

    private void nextToken() {
        myTokenEnd = myPosition;
        if (myPosition >= myEndOffset) {
            myTokenType = null;
            return;
        }
        if (!findAtOffset(myPosition)) {
            int nextOffset = myPosition;
            while (++nextOffset < myEndOffset) {
                if (findAtOffset(nextOffset)) {
                    break;
                }
            }
            myTokenEnd = nextOffset;
            myTokenType = TokenType.BAD_CHARACTER;
        }
    }

    private boolean findAtOffset(int position) {
        myTokenEnd = position;
        myTokenType = null;
        for (int i = 0; i < myMatchers.length; i++) {
            if (myMatchers[i] == null) {
                continue;
            }
            Matcher matcher = myMatchers[i].region(position, myEndOffset);
            if (matcher.lookingAt()) {
                int end = matcher.end();
                if (end > myTokenEnd) {
                    myTokenEnd = end;
                    myTokenType = myTokens[i].tokenType;
                }
            }
        }
        return myTokenType != null;
    }

    @Override
    public int getState() {
        return 0;
    }

    @Nullable
    @Override
    public IElementType getTokenType() {
        if (myTokenType == null && myPosition != myEndOffset) {
            nextToken();
            assert false : "not lexed: '" + myBuffer.subSequence(myPosition, myEndOffset) + "'";
        }
        return myTokenType;
    }

    @Override
    public int getTokenStart() {
        return myPosition;
    }

    @Override
    public int getTokenEnd() {
        return myTokenEnd;
    }

    @Override
    public void advance() {
        if (myTokenType != null) {
            myPosition = myTokenEnd;
            nextToken();
        }
    }

    @Nonnull
    @Override
    public CharSequence getBufferSequence() {
        return myBuffer;
    }

    @Override
    public int getBufferEnd() {
        return myEndOffset;
    }

    public Collection<Token> getTokens() {
        return Arrays.asList(myTokens);
    }

    static class Token {
        final String constantName;
        final Pattern pattern;
        final IElementType tokenType;

        Token(String pattern, String mappedName, boolean usedInGrammar, String constantPrefix, LivePreviewLanguage language) {
            constantName = constantPrefix + Case.UPPER.apply(mappedName);
            String tokenName;
            boolean keyword;
            if (ParserGeneratorUtil.isRegexpToken(pattern)) {
                String patternText = ParserGeneratorUtil.getRegexpTokenRegexp(pattern);
                this.pattern = ParserGeneratorUtil.compilePattern(patternText);
                tokenName = mappedName;
                keyword = false;
            }
            else {
                this.pattern = ParserGeneratorUtil.compilePattern(StringUtil.escapeToRegexp(pattern));
                tokenName = pattern;
                keyword = StringUtil.isJavaIdentifier(pattern);
            }

            IElementType delegate = keyword ? null : guessDelegateType(tokenName, this.pattern, usedInGrammar);
            if (keyword) {
                tokenType = new LivePreviewElementType.KeywordType(tokenName, language);
            }
            else if (delegate == TokenType.WHITE_SPACE || delegate == COMMENT) {
                tokenType = delegate; // PreviewTokenType(tokenName, language, delegate);
            }
            else {
                tokenType = new LivePreviewElementType.TokenType(delegate, tokenName, language);
            }
        }

        @Override
        public String toString() {
            return "Token{" +
                constantName +
                ", pattern=" + pattern +
                ", tokenType=" + tokenType +
                '}';
        }
    }

    @Nullable
    private static IElementType guessDelegateType(@Nonnull String tokenName, @Nullable Pattern pattern, boolean usedInGrammar) {
        if (pattern != null) {
            if (!usedInGrammar && (pattern.matcher(" ").matches() || pattern.matcher("\n").matches())) {
                return TokenType.WHITE_SPACE;
            }
            else if (pattern.matcher("1234").matches()) {
                return NUMBER;
            }
            else if (pattern.matcher("\"sdf\"").matches() || pattern.matcher("\'sdf\'").matches()) {
                return STRING;
            }
        }
        if (!usedInGrammar && StringUtil.endsWithIgnoreCase(tokenName, "comment")) {
            return COMMENT;
        }
        return null;
    }

    @Nonnull
    public static Map<String, String> collectTokenPattern2Name(@Nonnull BnfFile file, @Nullable Set<String> usedInGrammar) {
        return ParserGeneratorUtil.collectTokenPattern2Name(file, true, new LinkedHashMap<>(), usedInGrammar);
    }
}
