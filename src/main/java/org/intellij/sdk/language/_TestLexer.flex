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

  public void reset(CharSequence buffer, int start, int end, int initialState) {
    char[] buf = new char[end];
    for (int i = 0; i < end; i++) {
      buf[i] = buffer.charAt(i);
    }
    this.zzBuffer = buf;
    this.zzCurrentPos = this.zzMarkedPos = this.zzStartRead = start;
    this.zzAtEOF = false;
    this.zzAtBOL = true;
    this.zzEndRead = end;
    this.zzReader = java.io.Reader.nullReader();
    yybegin(initialState);
  }

  /** Pre-loaded buffer — no refilling needed. Signal EOF when buffer is consumed. */
  private boolean zzRefill() {
    return true;
  }

  public int getTokenStart() {
    return zzStartRead;
  }

  public int getTokenEnd() {
    return zzMarkedPos;
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
SQL=(ADD|ALTER|AND|ANY|AS|ASC|BACKUP|BETWEEN|CASE|CHECK|CREATE|REPLACE|DELETE|DESC|COLUMN|CONSTRAINT|DROP|DATABASE|DEFAULT|EXEC|EXISTS|FOREIGN|FROM|FULL|GROUP|HAVING|IN|INDEX|INNER|INSERT|IS|LEFT|LIKE|LIMIT|NOT|OR|ORDER|BY|OUTER|PRIMARY|KEY|PROCEDURE|RIGHT|JOIN|ROWNUM|SELECT|DISTINCT|INTO|SET|TOP|TRUNCATE|TABLE|UNION|ALL|UNIQUE|UPDATE|VALUES|VIEW|WHERE|WITH|RECURSIVE|COPY|EXPLAIN|CALL|ATTACH|DETACH|EXPORT|IMPORT|PIVOT|UNPIVOT|DESCRIBE|SHOW|SUMMARIZE|RETURNING|QUALIFY|WINDOW|OVER|PARTITION|ROWS|RANGE|UNBOUNDED|PRECEDING|FOLLOWING|CURRENT|ROW|LATERAL|CROSS|NATURAL|USING|ON|WHEN|THEN|ELSE|END|CAST|TRY_CAST|NULL|TRUE|FALSE|IF|EXCEPT|INTERSECT|TYPE|ENUM|STRUCT|MAP|LIST|ARRAY|FUNCTION|MACRO|TEMPORARY|TEMP|SEQUENCE|SCHEMA|VACUUM|ANALYZE|RESET|GLOB|ILIKE|SIMILAR|TO|COLLATE|REFERENCES|GENERATED|ALWAYS|STORED|VIRTUAL|OFFSET|FETCH|FILTER|FIRST|LAST|ESCAPE|DO|NOTHING|CONFLICT|IGNORE|ABORT|EXCLUDE|RESPECT|NULLS|NEXT|CASCADE|RESTRICT|GRANT|REVOKE|FORCE|AUTOINCREMENT)+[ \t\n\x0B\f\r]

RESULT_TEXT=[^\r\n]+

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
  "foreach"                { return FOREACH; }
  "endforeach"             { return ENDFOREACH; }
  "concurrentloop"         { return CONCURRENTLOOP; }
  "concurrentforeach"      { return CONCURRENTFOREACH; }
  "statement"              { return STATEMENT; }
  "query"                  { return QUERY; }
  "load"                   { return LOAD; }
  "require-env"            { return REQUIRE_ENV; }
  "require_reinit"         { return REQUIRE_REINIT; }
  "require"                { return REQUIRE; }
  "mode"                   { return MODE; }
  "halt"                   { return HALT; }
  "skipif"                 { return SKIPIF; }
  "onlyif"                 { return ONLYIF; }
  "PRAGMA"                 { return PRAGMA; }
  "restart"                { return RESTART; }
  "reconnect"              { return RECONNECT; }
  "begin"                  { return BEGIN; }
  "transaction"            { return TRANSACTION; }
  "rollback"               { return ROLLBACK; }
  "----"                   { yybegin(RESULT_SECTION); return Q_RESULT; }
  "physical_plan"          { return PHYSICAL_PLAN; }
  "sleep"                  { return SLEEP; }
  "hash-threshold"         { return HASH_THRESHOLD; }
  "unzip"                  { return UNZIP; }
  "test-env"               { return TEST_ENV; }
  "tags"                   { return TAGS; }
  "continue"               { return CONTINUE; }
  "template"               { return TEMPLATE; }
  "cache_file"             { return CACHE_FILE; }
  "cache"                  { return CACHE; }
  "cleanup"                { return CLEANUP; }
  "init"                   { return INIT; }
  "reload"                 { return RELOAD; }
  "resultmode"             { return RESULTMODE; }
  "result_query"           { return RESULT_QUERY; }
  "assert"                 { return ASSERT; }
  "run"                    { return RUN; }
  "include"                { return INCLUDE; }
  "argument"               { return ARGUMENT; }
  "subgroup"               { return SUBGROUP; }
  "storage"                { return STORAGE; }
  "retry"                  { return RETRY; }

  {COMMENT}                { return COMMENT; }
  {NUMBER}                 { return NUMBER; }
  {ID}                     { return ID; }
  {STRING}                 { return STRING; }
  {QUERY_RETURN_TYPE}      { return QUERY_RETURN_TYPE; }
  {QUERY_LABEL}            { return QUERY_LABEL; }
  {SQL}                    { return SQL; }
}

<RESULT_SECTION> {
  {EOL} / [ \t]*(("statement"|"query"|"load"|"loop"|"endloop"|"foreach"|"endforeach"|"concurrentloop"|"concurrentforeach"|"restart"|"reconnect"|"begin"|"halt"|"require"|"require-env"|"require_reinit"|"mode"|"skipif"|"onlyif"|"sleep"|"hash-threshold"|"unzip"|"test-env"|"tags"|"continue"|"template"|"cache_file"|"cache"|"cleanup"|"init"|"reload"|"resultmode"|"result_query"|"assert"|"run"|"include"|"argument"|"subgroup"|"storage"|"retry"|"physical_plan"|"#")[ \t\n\x0B\f\r])  {
    yybegin(YYINITIAL);
    return WHITE_SPACE;
  }
  {EOL} / [ \t]*{EOL}  {
    yybegin(YYINITIAL);
    return WHITE_SPACE;
  }
  {RESULT_TEXT}            { return RESULT_LINE; }
  {EOL}                    { return WHITE_SPACE; }
}

[^] { return BAD_CHARACTER; }
