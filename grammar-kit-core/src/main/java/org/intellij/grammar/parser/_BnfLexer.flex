package org.intellij.grammar.parser;
import com.intellij.lexer.*;
import com.intellij.psi.tree.IElementType;
import org.intellij.grammar.psi.BnfTypes;
import static org.intellij.grammar.BnfParserDefinition.BNF_LINE_COMMENT;
import static org.intellij.grammar.BnfParserDefinition.BNF_BLOCK_COMMENT;

%%

%public
%class BnfLexer
%extends LexerBase
%function advanceImpl
%type IElementType
%unicode
%eof{
  return;
%eof}

EOL="\r"|"\n"|"\r\n"
LINE_WS=[\ \t\f]
WHITE_SPACE=({LINE_WS}|{EOL})+
LINE_COMMENT="//" .*
BLOCK_COMMENT="/*" !([^]* "*/" [^]*) ("*/")?

ALPHA=[:letter:]
DIGIT=[:digit:]

ID_BODY={ALPHA} | {DIGIT} | "_" | "-"
ID={ALPHA} ({ID_BODY}) * | "<" ([^<>])+ ">"
HEX={DIGIT} | [aAbBcCdDeEfF]
NUMBER={DIGIT}+ | "0x" {HEX}+

ESC="\\" ( [^] | "u" {HEX}{HEX}{HEX}{HEX} )
CHAR=[^\r\n\'\"\\]
STRING_BAD1=\" ({CHAR} | {ESC} | \') *
STRING_BAD2=\' ({CHAR} | \" | \\) *
STRING={STRING_BAD1} \" | {STRING_BAD2} \'

BAD_TOKENS={STRING_BAD1} | {STRING_BAD2}



%%
<YYINITIAL> {
  {WHITE_SPACE} {yybegin(YYINITIAL); return com.intellij.psi.TokenType.WHITE_SPACE; }
  {LINE_COMMENT} {yybegin(YYINITIAL); return BNF_LINE_COMMENT; }
  {BLOCK_COMMENT} {yybegin(YYINITIAL); return BNF_BLOCK_COMMENT; }

  {STRING} {yybegin(YYINITIAL); return BnfTypes.BNF_STRING; }
  {NUMBER} {yybegin(YYINITIAL); return BnfTypes.BNF_NUMBER; }

  {ID} {yybegin(YYINITIAL); return BnfTypes.BNF_ID; }

  ";" {yybegin(YYINITIAL); return BnfTypes.BNF_SEMICOLON; }

  "(" {yybegin(YYINITIAL); return BnfTypes.BNF_LEFT_PAREN; }
  ")" {yybegin(YYINITIAL); return BnfTypes.BNF_RIGHT_PAREN; }
  "{" {yybegin(YYINITIAL); return BnfTypes.BNF_LEFT_BRACE; }
  "}" {yybegin(YYINITIAL); return BnfTypes.BNF_RIGHT_BRACE; }
  "[" {yybegin(YYINITIAL); return BnfTypes.BNF_LEFT_BRACKET; }
  "]" {yybegin(YYINITIAL); return BnfTypes.BNF_RIGHT_BRACKET; }
  "<<" {yybegin(YYINITIAL); return BnfTypes.BNF_EXTERNAL_START; }
  ">>" {yybegin(YYINITIAL); return BnfTypes.BNF_EXTERNAL_END; }

  "::=" {yybegin(YYINITIAL); return BnfTypes.BNF_OP_IS; }
  "=" {yybegin(YYINITIAL); return BnfTypes.BNF_OP_EQ; }
  "+" {yybegin(YYINITIAL); return BnfTypes.BNF_OP_ONEMORE; }
  "*" {yybegin(YYINITIAL); return BnfTypes.BNF_OP_ZEROMORE; }
  "?" {yybegin(YYINITIAL); return BnfTypes.BNF_OP_OPT; }
  "&" {yybegin(YYINITIAL); return BnfTypes.BNF_OP_AND; }
  "!" {yybegin(YYINITIAL); return BnfTypes.BNF_OP_NOT; }
  "|" {yybegin(YYINITIAL); return BnfTypes.BNF_OP_OR; }
}

{BAD_TOKENS} {yybegin(YYINITIAL); return com.intellij.psi.TokenType.BAD_CHARACTER; }

[^] {yybegin(YYINITIAL); return com.intellij.psi.TokenType.BAD_CHARACTER; }
