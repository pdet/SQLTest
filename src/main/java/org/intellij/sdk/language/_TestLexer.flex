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

%state RESULT_SECTION

EOL=\R
WHITE_SPACE=\s+

COMMENT=#.*
NUMBER=[0-9]+(\.[0-9]*)?
ID=:?[:letter:][a-zA-Z_0-9]*:?
STRING=('([^'\\]|\\.)*'|\"([^\"\\]|\\.)*\")
QUERY_RETURN_TYPE=[TIR]+[ \t\n\x0B\f\r]
QUERY_LABEL=label.+[ \t\n\x0B\f\r]
SQL=(ADD|ALTER|AND|ANY|AS|ASC|BACKUP|BETWEEN|CASE|CHECK|CREATE|REPLACE|DELETE|DESC|COLUMN|CONSTRAINT|DROP|DATABASE|DEFAULT|EXEC|EXISTS|FOREIGN|FROM|FULL|GROUP|HAVING|IN|INDEX|INNER|INSERT|IS|LEFT|LIKE|LIMIT|NOT|OR|ORDER|BY|OUTER|PRIMARY|KEY|PROCEDURE|RIGHT|JOIN|ROWNUM|SELECT|DISTINCT|INTO|SET|TOP|TRUNCATE|TABLE|UNION|ALL|UNIQUE|UPDATE|VALUES|VIEW|WHERE|WITH|RECURSIVE|COPY|EXPLAIN|CALL|ATTACH|DETACH|EXPORT|IMPORT|PIVOT|UNPIVOT|DESCRIBE|SHOW|SUMMARIZE|RETURNING|QUALIFY|WINDOW|OVER|PARTITION|ROWS|RANGE|UNBOUNDED|PRECEDING|FOLLOWING|CURRENT|ROW|LATERAL|CROSS|NATURAL|USING|ON|WHEN|THEN|ELSE|END|CAST|TRY_CAST|NULL|TRUE|FALSE|IF|EXCEPT|INTERSECT|TYPE|ENUM|STRUCT|MAP|LIST|ARRAY|FUNCTION|MACRO|TEMPORARY|TEMP|SEQUENCE|SCHEMA|VACUUM|ANALYZE|RESET|GLOB|ILIKE|SIMILAR|TO|COLLATE|REFERENCES|GENERATED|ALWAYS|STORED|VIRTUAL|OFFSET|FETCH|ONLY|FILTER|FIRST|LAST|ESCAPE|DO|NOTHING|CONFLICT|IGNORE|ABORT|EXCLUDE|RESPECT|NULLS|NEXT|CASCADE|RESTRICT|GRANT|REVOKE|FORCE|AUTOINCREMENT)+[ \t\n\x0B\f\r]

// In RESULT_SECTION, a blank line followed by a directive keyword returns to YYINITIAL
RESULT_LINE=[^\r\n]+

%%

<YYINITIAL> {
  {WHITE_SPACE}            { return WHITE_SPACE; }

  ";"                      { return SEMI; }
  ":"                      { return DDOT; }
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
  "----"                   { yybegin(RESULT_SECTION); return Q_RESULT; }
  "physical_plan"          { return PHYSICAL_PLAN; }

  {COMMENT}                { return COMMENT; }
  {NUMBER}                 { return NUMBER; }
  {ID}                     { return ID; }
  {STRING}                 { return STRING; }
  {QUERY_RETURN_TYPE}      { return QUERY_RETURN_TYPE; }
  {QUERY_LABEL}            { return QUERY_LABEL; }
  {SQL}                    { return SQL; }
}

<RESULT_SECTION> {
  // Blank line signals potential end of results — peek for directive keywords
  {EOL} / [ \t]*(("statement"|"query"|"load"|"loop"|"endloop"|"restart"|"begin"|"halt"|"require"|"mode"|"foreach"|"endforeach"|"concurrentloop"|"concurrentforeach"|"skipif"|"onlyif"|"#")[ \t\n\x0B\f\r])  {
    yybegin(YYINITIAL);
    return WHITE_SPACE;
  }
  {EOL} / [ \t]*{EOL}  {
    // Double blank line also exits result section
    yybegin(YYINITIAL);
    return WHITE_SPACE;
  }
  {RESULT_LINE}            { return ID; }
  {EOL}                    { return WHITE_SPACE; }
}

[^] { return BAD_CHARACTER; }
