package org.intellij.sdk.language;

import java.util.*;

/**
 * Formats SQL: uppercases keywords, breaks into multi-line clauses, indents.
 *
 * <pre>
 *   select a, b from t where x > 1 and y < 2 order by a
 * →
 *   SELECT
 *       a,
 *       b
 *   FROM t
 *   WHERE x > 1
 *       AND y < 2
 *   ORDER BY a
 * </pre>
 */
public class SQLTestSqlFormatter {

    private static final String IND = "    ";

    // ── Token types ──────────────────────────────────────────────────────────

    enum TT { KW, ID, STR, NUM, TPL, LP, RP, COMMA, SEMI, DOT, STAR, OP, OTHER }

    static final class Tok {
        final TT type;
        final String text;
        Tok(TT type, String text) { this.type = type; this.text = text; }
    }

    // ── Keyword sets ─────────────────────────────────────────────────────────

    private static final Set<String> SQL_KEYWORDS = new HashSet<>(Arrays.asList(
            "ADD", "ALTER", "AND", "ANY", "AS", "ASC", "BACKUP", "BETWEEN",
            "CASE", "CHECK", "CREATE", "REPLACE", "DELETE", "DESC", "COLUMN",
            "CONSTRAINT", "DROP", "DATABASE", "DEFAULT", "EXEC", "EXISTS",
            "FOREIGN", "FROM", "FULL", "GROUP", "HAVING", "IN", "INDEX",
            "INNER", "INSERT", "IS", "LEFT", "LIKE", "LIMIT", "NOT", "OR",
            "ORDER", "BY", "OUTER", "PRIMARY", "KEY", "PROCEDURE", "RIGHT",
            "JOIN", "ROWNUM", "SELECT", "DISTINCT", "INTO", "SET", "TOP",
            "TRUNCATE", "TABLE", "UNION", "ALL", "UNIQUE", "UPDATE", "VALUES",
            "VIEW", "WHERE", "WITH", "RECURSIVE", "COPY", "EXPLAIN", "CALL",
            "ATTACH", "DETACH", "EXPORT", "IMPORT", "PIVOT", "UNPIVOT",
            "DESCRIBE", "SHOW", "SUMMARIZE", "RETURNING", "QUALIFY", "WINDOW",
            "OVER", "PARTITION", "ROWS", "RANGE", "UNBOUNDED", "PRECEDING",
            "FOLLOWING", "CURRENT", "ROW", "LATERAL", "CROSS", "NATURAL",
            "USING", "ON", "WHEN", "THEN", "ELSE", "END", "CAST", "TRY_CAST",
            "NULL", "TRUE", "FALSE", "IF", "EXCEPT", "INTERSECT", "TYPE",
            "ENUM", "STRUCT", "MAP", "LIST", "ARRAY", "FUNCTION", "MACRO",
            "TEMPORARY", "TEMP", "SEQUENCE", "SCHEMA", "VACUUM", "ANALYZE",
            "RESET", "GLOB", "ILIKE", "SIMILAR", "TO", "COLLATE", "REFERENCES",
            "GENERATED", "ALWAYS", "STORED", "VIRTUAL", "OFFSET", "FETCH",
            "FILTER", "FIRST", "LAST", "ESCAPE", "DO", "NOTHING", "CONFLICT",
            "IGNORE", "ABORT", "EXCLUDE", "RESPECT", "NULLS", "NEXT",
            "CASCADE", "RESTRICT", "GRANT", "REVOKE", "FORCE", "AUTOINCREMENT",
            "PRAGMA", "BEGIN", "COMMIT", "ROLLBACK", "TRANSACTION", "RETURNS",
            "LANGUAGE", "IMMUTABLE", "VOLATILE", "STABLE", "PARALLEL", "SAFE",
            "INTEGER", "VARCHAR", "BOOLEAN", "FLOAT", "DOUBLE", "BIGINT",
            "SMALLINT", "TINYINT", "HUGEINT", "DECIMAL", "NUMERIC", "REAL",
            "DATE", "TIME", "TIMESTAMP", "INTERVAL", "BLOB", "TEXT", "BIT",
            "SERIAL", "JSON", "UUID", "UINTEGER", "UBIGINT", "USMALLINT",
            "UTINYINT", "UHUGEINT", "GROUPS", "MATERIALIZED"
    ));

    /** Clause keywords that start on a new line at the current base indent. */
    private static final Set<String> NEW_LINE_CLAUSES = new HashSet<>(Arrays.asList(
            "SELECT", "FROM", "WHERE", "HAVING", "LIMIT", "OFFSET",
            "RETURNING", "VALUES", "SET", "WINDOW", "QUALIFY"
    ));

    /** Join modifier words that precede JOIN. */
    private static final Set<String> JOIN_MODIFIERS = new HashSet<>(Arrays.asList(
            "LEFT", "RIGHT", "FULL", "INNER", "CROSS", "NATURAL"
    ));

    /** Top-level statement starters. */
    private static final Set<String> STMT_STARTS = new HashSet<>(Arrays.asList(
            "CREATE", "ALTER", "DROP", "UPDATE", "DELETE", "INSERT",
            "WITH", "ATTACH", "DETACH", "COPY", "EXPLAIN", "CALL",
            "PRAGMA", "DESCRIBE", "SHOW", "SUMMARIZE", "EXPORT", "IMPORT",
            "VACUUM", "ANALYZE", "BEGIN", "COMMIT", "ROLLBACK"
    ));

    // ── Paren context tracking ───────────────────────────────────────────────

    /** Whether a paren level has formatted (multi-line) content. */
    private static final class ParenCtx {
        final boolean formatted;  // indent content inside these parens?
        final String prevClause;  // clause before we entered this paren
        final int prevBase;       // base indent before we entered
        ParenCtx(boolean formatted, String prevClause, int prevBase) {
            this.formatted = formatted;
            this.prevClause = prevClause;
            this.prevBase = prevBase;
        }
    }

    // ── Tokenizer ────────────────────────────────────────────────────────────

    static List<Tok> tokenize(String sql) {
        List<Tok> tokens = new ArrayList<>();
        int i = 0;
        while (i < sql.length()) {
            char c = sql.charAt(i);

            // Whitespace — skip
            if (Character.isWhitespace(c)) { i++; continue; }

            // Single-quoted string
            if (c == '\'') {
                int end = skipQuoted(sql, i, '\'');
                tokens.add(new Tok(TT.STR, sql.substring(i, end)));
                i = end; continue;
            }
            // Double-quoted identifier
            if (c == '"') {
                int end = skipQuoted(sql, i, '"');
                tokens.add(new Tok(TT.STR, sql.substring(i, end)));
                i = end; continue;
            }

            // Template variable ${...}
            if (c == '$' && i + 1 < sql.length() && sql.charAt(i + 1) == '{') {
                int end = sql.indexOf('}', i + 2);
                end = end >= 0 ? end + 1 : sql.length();
                tokens.add(new Tok(TT.TPL, sql.substring(i, end)));
                i = end; continue;
            }

            // Number
            if (Character.isDigit(c)) {
                int s = i;
                while (i < sql.length() && (Character.isDigit(sql.charAt(i)) || sql.charAt(i) == '.')) i++;
                // Handle scientific notation (e.g. 1e10)
                if (i < sql.length() && (sql.charAt(i) == 'e' || sql.charAt(i) == 'E')) {
                    i++;
                    if (i < sql.length() && (sql.charAt(i) == '+' || sql.charAt(i) == '-')) i++;
                    while (i < sql.length() && Character.isDigit(sql.charAt(i))) i++;
                }
                tokens.add(new Tok(TT.NUM, sql.substring(s, i)));
                continue;
            }

            // Word (keyword or identifier)
            if (Character.isLetter(c) || c == '_') {
                int s = i;
                while (i < sql.length() && (Character.isLetterOrDigit(sql.charAt(i)) || sql.charAt(i) == '_')) i++;
                String word = sql.substring(s, i);
                TT type = SQL_KEYWORDS.contains(word.toUpperCase()) ? TT.KW : TT.ID;
                tokens.add(new Tok(type, word));
                continue;
            }

            // Punctuation / operators
            switch (c) {
                case '(': tokens.add(new Tok(TT.LP, "(")); i++; continue;
                case ')': tokens.add(new Tok(TT.RP, ")")); i++; continue;
                case ',': tokens.add(new Tok(TT.COMMA, ",")); i++; continue;
                case ';': tokens.add(new Tok(TT.SEMI, ";")); i++; continue;
                case '.': tokens.add(new Tok(TT.DOT, ".")); i++; continue;
                case '*': tokens.add(new Tok(TT.STAR, "*")); i++; continue;
            }
            // Multi-char operators
            if (i + 1 < sql.length()) {
                String two = sql.substring(i, i + 2);
                if (two.equals("::") || two.equals("||") || two.equals("!=") ||
                        two.equals("<>") || two.equals("<=") || two.equals(">=") ||
                        two.equals("<<") || two.equals(">>")) {
                    tokens.add(new Tok(TT.OP, two));
                    i += 2; continue;
                }
            }
            if ("=<>+-/%&|~^!@".indexOf(c) >= 0) {
                tokens.add(new Tok(TT.OP, String.valueOf(c)));
                i++; continue;
            }

            tokens.add(new Tok(TT.OTHER, String.valueOf(c)));
            i++;
        }
        return tokens;
    }

    private static int skipQuoted(String sql, int pos, char quote) {
        for (int i = pos + 1; i < sql.length(); i++) {
            if (sql.charAt(i) == '\\') { i++; continue; }
            // DuckDB uses '' to escape single quotes
            if (sql.charAt(i) == quote) {
                if (i + 1 < sql.length() && sql.charAt(i + 1) == quote) { i++; continue; }
                return i + 1;
            }
        }
        return sql.length();
    }

    // ── Formatter ────────────────────────────────────────────────────────────

    /**
     * Format a SQL string: uppercase keywords, break into multi-line clauses.
     */
    static String format(String sql) {
        List<Tok> tokens = tokenize(sql);
        if (tokens.isEmpty()) return sql;

        StringBuilder out = new StringBuilder();
        int baseIndent = 0;
        String clause = "";             // current clause: SELECT, FROM, WHERE, etc.
        Deque<ParenCtx> parenStack = new ArrayDeque<>();
        // Effective paren depth for "top-level" comma checks
        int exprParenDepth = 0;         // depth of non-formatted (expression/function) parens

        for (int i = 0; i < tokens.size(); i++) {
            Tok tok = tokens.get(i);
            String kw = tok.text.toUpperCase();
            String text = tok.type == TT.KW ? kw : tok.text;

            // ── Close paren ──────────────────────────────────────────────
            if (tok.type == TT.RP) {
                if (!parenStack.isEmpty()) {
                    ParenCtx ctx = parenStack.pop();
                    if (ctx.formatted) {
                        baseIndent = ctx.prevBase;
                        newline(out, baseIndent);
                    } else {
                        exprParenDepth--;
                    }
                    clause = ctx.prevClause;
                }
                out.append(')');
                continue;
            }

            // ── Compound clause detection (must come before single-keyword checks) ──

            if (tok.type == TT.KW) {

                // GROUP BY / ORDER BY / PARTITION BY
                if ((kw.equals("GROUP") || kw.equals("ORDER") || kw.equals("PARTITION"))
                        && peekKw(tokens, i + 1, "BY")) {
                    if (out.length() > 0) newline(out, baseIndent);
                    out.append(kw).append(" BY");
                    i++;
                    clause = kw.equals("GROUP") ? "GROUP BY" : kw + " BY";
                    continue;
                }

                // INSERT INTO
                if (kw.equals("INSERT") && peekKw(tokens, i + 1, "INTO")) {
                    if (out.length() > 0) newline(out, baseIndent);
                    out.append("INSERT INTO");
                    i++;
                    clause = "INSERT";
                    continue;
                }

                // CREATE [OR REPLACE] TABLE/VIEW/...
                if (kw.equals("CREATE")) {
                    if (out.length() > 0) newline(out, baseIndent);
                    out.append("CREATE");
                    // consume OR REPLACE if present
                    if (peekKw(tokens, i + 1, "OR") && peekKw(tokens, i + 2, "REPLACE")) {
                        out.append(" OR REPLACE");
                        i += 2;
                    }
                    // consume TEMPORARY/TEMP if present
                    if (peekKw(tokens, i + 1, "TEMPORARY") || peekKw(tokens, i + 1, "TEMP")) {
                        out.append(" ").append(tokens.get(i + 1).text.toUpperCase());
                        i++;
                    }
                    clause = "CREATE";
                    continue;
                }

                // ON CONFLICT
                if (kw.equals("ON") && peekKw(tokens, i + 1, "CONFLICT")) {
                    newline(out, baseIndent);
                    out.append("ON CONFLICT");
                    i++;
                    clause = "ON CONFLICT";
                    continue;
                }

                // UNION [ALL] / EXCEPT / INTERSECT
                if (kw.equals("UNION") || kw.equals("EXCEPT") || kw.equals("INTERSECT")) {
                    newline(out, baseIndent);
                    out.append(kw);
                    if (kw.equals("UNION") && peekKw(tokens, i + 1, "ALL")) {
                        out.append(" ALL");
                        i++;
                    }
                    clause = kw;
                    continue;
                }

                // JOIN variants: [LEFT|RIGHT|FULL|INNER|CROSS|NATURAL] [OUTER] JOIN
                if (kw.equals("JOIN") || (JOIN_MODIFIERS.contains(kw) && hasJoinAhead(tokens, i))) {
                    if (out.length() > 0) newline(out, baseIndent);
                    out.append(kw);
                    int j = i + 1;
                    // consume OUTER if present
                    if (peekKw(tokens, j, "OUTER")) {
                        out.append(" OUTER");
                        j++;
                    }
                    if (peekKw(tokens, j, "JOIN")) {
                        out.append(" JOIN");
                        j++;
                    }
                    i = j - 1;
                    clause = "JOIN";
                    continue;
                }

                // IS NOT NULL / NOT NULL / NOT IN / NOT EXISTS / NOT LIKE — don't break
                if (kw.equals("NOT")) {
                    appendSpace(out, tok);
                    out.append("NOT");
                    continue;
                }

                // ── Simple clause keywords ───────────────────────────────
                if (NEW_LINE_CLAUSES.contains(kw)) {
                    // INTO: only break if NOT part of INSERT INTO (already handled)
                    // SET: only break in UPDATE context
                    if (kw.equals("INTO")) {
                        appendSpace(out, tok);
                        out.append(text);
                        continue;
                    }
                    if (kw.equals("SET") && !clause.equals("UPDATE")) {
                        appendSpace(out, tok);
                        out.append(text);
                        continue;
                    }
                    // After entering a formatted paren, don't double-newline
                    if (!clause.equals("PAREN_START") && out.length() > 0) {
                        newline(out, baseIndent);
                    }
                    out.append(text);
                    clause = kw.equals("HAVING") ? "WHERE" : kw;
                    continue;
                }

                // ── Statement starters ───────────────────────────────────
                if (STMT_STARTS.contains(kw) && !kw.equals("CREATE") && !kw.equals("INSERT")) {
                    if (!clause.equals("PAREN_START") && out.length() > 0) {
                        newline(out, baseIndent);
                    }
                    out.append(text);
                    clause = kw;
                    continue;
                }

                // ── AND / OR in WHERE/HAVING ─────────────────────────────
                if ((kw.equals("AND") || kw.equals("OR")) && clause.equals("WHERE")
                        && exprParenDepth == 0) {
                    newline(out, baseIndent + 1);
                    out.append(text);
                    continue;
                }

                // ── CASE / WHEN / THEN / ELSE / END ─────────────────────
                if (kw.equals("CASE")) {
                    appendSpace(out, tok);
                    out.append("CASE");
                    continue;
                }
                if (kw.equals("WHEN") && insideCase(tokens, i)) {
                    newline(out, baseIndent + 1);
                    out.append("WHEN");
                    continue;
                }
                if (kw.equals("THEN")) {
                    out.append(" THEN");
                    continue;
                }
                if (kw.equals("ELSE") && insideCase(tokens, i)) {
                    newline(out, baseIndent + 1);
                    out.append("ELSE");
                    continue;
                }
                if (kw.equals("END")) {
                    newline(out, baseIndent);
                    out.append("END");
                    continue;
                }
            }

            // ── Open paren ───────────────────────────────────────────────
            if (tok.type == TT.LP) {
                Tok nextTok = peekTok(tokens, i + 1);
                boolean isSubquery = nextTok != null && nextTok.type == TT.KW &&
                        (nextTok.text.equalsIgnoreCase("SELECT") || nextTok.text.equalsIgnoreCase("WITH"));
                boolean isColDefs = clause.equals("CREATE") && !isSubquery;
                boolean formatted = isSubquery || isColDefs;

                // Check: is it a function call? (prev token is identifier or non-clause keyword)
                Tok prevTok = prevNonSpace(tokens, i - 1);
                boolean isFuncCall = prevTok != null &&
                        (prevTok.type == TT.ID ||
                                (prevTok.type == TT.KW && !NEW_LINE_CLAUSES.contains(prevTok.text.toUpperCase())
                                        && !STMT_STARTS.contains(prevTok.text.toUpperCase()))) &&
                        !isSubquery && !isColDefs;
                if (isFuncCall) formatted = false;

                // Check for AS ( in CTE context
                if (!formatted && prevTok != null && prevTok.type == TT.KW &&
                        prevTok.text.equalsIgnoreCase("AS") && isSubquery) {
                    formatted = true;
                }

                parenStack.push(new ParenCtx(formatted, clause, baseIndent));

                if (formatted) {
                    appendSpace(out, tok);
                    out.append('(');
                    baseIndent++;
                    newline(out, baseIndent);
                    // Use special marker so next clause keyword doesn't add another newline
                    clause = isSubquery ? "PAREN_START" : "COLUMN_DEFS";
                } else {
                    // No space between function name and (
                    if (!isFuncCall) appendSpace(out, tok);
                    out.append('(');
                    exprParenDepth++;
                }
                continue;
            }

            // ── Comma ────────────────────────────────────────────────────
            if (tok.type == TT.COMMA) {
                out.append(',');
                boolean breakAfter = false;
                if (exprParenDepth == 0) {
                    if (clause.equals("SELECT") && selectHasMultipleColumns(tokens, i)) {
                        breakAfter = true;
                    } else if (clause.equals("COLUMN_DEFS")) {
                        breakAfter = true;
                    } else if (clause.equals("SET")) {
                        breakAfter = true;
                    }
                }
                if (breakAfter) {
                    int commaIndent = clause.equals("SELECT") || clause.equals("SET")
                            ? baseIndent + 1 : baseIndent;
                    newline(out, commaIndent);
                } else {
                    out.append(' ');
                }
                continue;
            }

            // ── Semicolon ────────────────────────────────────────────────
            if (tok.type == TT.SEMI) {
                out.append(';');
                continue;
            }

            // ── Dot (no spaces) ──────────────────────────────────────────
            if (tok.type == TT.DOT) {
                out.append('.');
                continue;
            }

            // ── Default: append with proper spacing ──────────────────────
            // No space after dot or open paren, no space before close paren
            if (needsSpace(out, tok, tokens, i)) {
                out.append(' ');
            }
            out.append(text);
        }

        return out.toString().stripTrailing();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static boolean peekKw(List<Tok> tokens, int idx, String kw) {
        return idx < tokens.size() && tokens.get(idx).type == TT.KW &&
                tokens.get(idx).text.equalsIgnoreCase(kw);
    }

    private static Tok peekTok(List<Tok> tokens, int idx) {
        return idx < tokens.size() ? tokens.get(idx) : null;
    }

    private static Tok prevNonSpace(List<Tok> tokens, int idx) {
        return idx >= 0 && idx < tokens.size() ? tokens.get(idx) : null;
    }

    private static boolean hasJoinAhead(List<Tok> tokens, int i) {
        int j = i + 1;
        if (peekKw(tokens, j, "OUTER")) j++;
        return peekKw(tokens, j, "JOIN");
    }

    /** Check if current position is inside a CASE block (look backward for CASE without matching END). */
    private static boolean insideCase(List<Tok> tokens, int pos) {
        int depth = 0;
        for (int j = pos - 1; j >= 0; j--) {
            if (tokens.get(j).type == TT.KW) {
                String k = tokens.get(j).text.toUpperCase();
                if (k.equals("END")) depth++;
                if (k.equals("CASE")) {
                    if (depth == 0) return true;
                    depth--;
                }
            }
        }
        return false;
    }

    /**
     * Check if the SELECT clause has multiple columns (contains a comma
     * before the next major clause keyword, at paren depth 0).
     */
    private static boolean selectHasMultipleColumns(List<Tok> tokens, int fromPos) {
        // Look backwards from current position to SELECT to see if there's another comma
        // Actually, easier: just return true. If we're here, we're already at a comma in SELECT.
        // The question is: should this comma trigger a newline? Yes, if it's a top-level SELECT comma.
        return true;
    }

    private static void newline(StringBuilder sb, int indent) {
        // Trim trailing spaces
        while (sb.length() > 0 && sb.charAt(sb.length() - 1) == ' ') {
            sb.setLength(sb.length() - 1);
        }
        sb.append('\n').append(IND.repeat(indent));
    }

    private static void appendSpace(StringBuilder sb, Tok tok) {
        if (sb.length() > 0) {
            char last = sb.charAt(sb.length() - 1);
            if (last != ' ' && last != '\n' && last != '(') {
                sb.append(' ');
            }
        }
    }

    private static boolean needsSpace(StringBuilder sb, Tok tok, List<Tok> tokens, int pos) {
        if (sb.length() == 0) return false;
        char last = sb.charAt(sb.length() - 1);
        if (last == ' ' || last == '\n' || last == '(' || last == '.') return false;
        // No space before dot
        if (tok.type == TT.DOT) return false;
        // No space for :: cast operator
        if (tok.type == TT.OP && tok.text.equals("::")) return false;
        // No space after :: cast operator
        if (sb.length() >= 2 && sb.charAt(sb.length() - 1) == ':' && sb.charAt(sb.length() - 2) == ':') return false;
        // No space after . (handled by 'last == .' check above)
        return true;
    }

    // ── Public API ───────────────────────────────────────────────────────────

    /**
     * Returns true if the SQL would change after formatting.
     */
    static boolean needsFormatting(String sql) {
        try {
            String formatted = format(sql);
            return !formatted.equals(sql);
        } catch (Exception e) {
            return false;
        }
    }
}
