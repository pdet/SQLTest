package org.intellij.sdk.language;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;

import static com.intellij.psi.TokenType.BAD_CHARACTER;
import static com.intellij.psi.TokenType.WHITE_SPACE;
import static org.intellij.sdk.language.psi.TestTypes.*;

%%

%{
  public _TestLexer() {
    this((java.io.Reader)null);
  }
%}

%public
%class _TestLexer
%implements FlexLexer
%function advance
%type IElementType
%unicode
%caseless

EOL=\R
WHITE_SPACE=\s+

COMMENT=#.*
NUMBER=[0-9]+(\.[0-9]*)?
ID=:?[:letter:][a-zA-Z_0-9]*:?
STRING=('([^'\\]|\\.)*'|\"([^\"\\]|\\.)*\")
QUERY_RETURN_TYPE=[TIR]+[ \t\n\x0B\f\r]
QUERY_LABEL=label.+[ \t\n\x0B\f\r]
SQL=(ADD|ALTER|AND|ANY|AS|ASC|BACKUP|BETWEEN|CASE|CHECK|CREATE|REPLACE|DELETE|DESC|COLUMN|CONSTRAINT|DROP|DATABASE|DEFAULT|EXEC|EXISTS|FOREIGN|FROM|FULL|GROUP|HAVING|IN|INDEX|INNER|INSERT|IS|LEFT|LIKE|LIMIT|NOT|OR|ORDER|BY|OUTER|PRIMARY|KEY|PROCEDURE|RIGHT|JOIN|ROWNUM|SELECT|DISTINCT|INTO|SET|TOP|TRUNCATE|TABLE|UNION|ALL|UNIQUE|UPDATE|VALUES|VIEW|WHERE)+[ \t\n\x0B\f\r]

%%
<YYINITIAL> {
  {WHITE_SPACE}            { return WHITE_SPACE; }

  ";"                      { return SEMI; }
  "="                      { return EQ; }
  "("                      { return LP; }
  ")"                      { return RP; }
  " "                      { return SPACE; }
  "."                      { return DOT; }
  "-"                      { return MINUS; }
  "+"                      { return PLUS; }
  ","                      { return COMMA; }
  "*"                      { return STAR; }
  "<"                      { return LESS; }
  ">"                      { return HIGHER; }
  "loop"                   { return LOOP; }
  "endloop"                { return ENDLOOP; }
  "statement"              { return STATEMENT; }
  "query"                  { return QUERY; }
  "load"                   { return LOAD; }
  "PRAGMA"                 { return PRAGMA; }
  "restart"                { return RESTART; }
  "begin"                  { return BEGIN; }
  "transaction"            { return TRANSACTION; }
  "rollback"               { return ROLLBACK; }
  "----"                   { return Q_RESULT; }

  {COMMENT}                { return COMMENT; }
  {NUMBER}                 { return NUMBER; }
  {ID}                     { return ID; }
  {STRING}                 { return STRING; }
  {QUERY_RETURN_TYPE}      { return QUERY_RETURN_TYPE; }
  {QUERY_LABEL}            { return QUERY_LABEL; }
  {SQL}                    { return SQL; }

}

[^] { return BAD_CHARACTER; }
