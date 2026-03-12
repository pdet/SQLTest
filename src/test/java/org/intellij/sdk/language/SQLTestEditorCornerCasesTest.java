package org.intellij.sdk.language;

import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

/**
 * Corner case tests for the SQLTest plugin.
 * Each test opens a file in a simulated CLion editor and runs the full
 * pipeline: lexer -> parser -> syntax highlighting -> annotator.
 *
 * Categories:
 * 1. Lexer edge cases (state transitions, unterminated sections, special chars)
 * 2. File boundary conditions (empty, single char, no trailing newline)
 * 3. Result section edge cases (the RESULT_SECTION lexer state)
 * 4. Directive parsing edge cases
 * 5. Real-world DuckDB test file patterns
 * 6. File extension variants
 * 7. Stress / boundary tests
 */
public class SQLTestEditorCornerCasesTest extends BasePlatformTestCase {

    // =========================================================================
    // 1. Lexer edge cases
    // =========================================================================

    /** File with no trailing newline — lexer must handle EOF at any position. */
    public void testNoTrailingNewline() {
        PsiFile file = myFixture.configureByText("no_newline.test",
                "statement ok\nSELECT 1");
        assertNotNull(file);
        myFixture.doHighlighting();
    }

    /** Single newline only. */
    public void testSingleNewline() {
        myFixture.configureByText("newline.test", "\n");
        myFixture.doHighlighting();
    }

    /** Single character file. */
    public void testSingleCharacter() {
        myFixture.configureByText("single.test", "x");
        myFixture.doHighlighting();
    }

    /** Only whitespace. */
    public void testWhitespaceOnly() {
        myFixture.configureByText("ws.test", "   \n\t\n   \n");
        myFixture.doHighlighting();
    }

    /** Unicode content in SQL and results. */
    public void testUnicodeContent() {
        myFixture.configureByText("unicode.test",
                "statement ok\n" +
                "INSERT INTO t VALUES ('\u65e5\u672c\u8a9e\u30c6\u30b9\u30c8')\n" +
                "\n" +
                "query I\n" +
                "SELECT * FROM t\n" +
                "----\n" +
                "\u65e5\u672c\u8a9e\u30c6\u30b9\u30c8\n");
        myFixture.doHighlighting();
    }

    /** Emoji in strings and results. */
    public void testEmojiContent() {
        myFixture.configureByText("emoji.test",
                "query I\n" +
                "SELECT '\uD83E\uDD86'\n" +
                "----\n" +
                "\uD83E\uDD86\n");
        myFixture.doHighlighting();
    }

    /** Very long single line — tests lexer buffer handling. */
    public void testVeryLongLine() {
        StringBuilder sb = new StringBuilder("statement ok\nSELECT '");
        for (int i = 0; i < 10000; i++) sb.append('x');
        sb.append("'\n");
        myFixture.configureByText("longline.test", sb.toString());
        myFixture.doHighlighting();
    }

    /** Many short lines — tests repeated lexer state transitions. */
    public void testManyShortLines() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 500; i++) {
            sb.append("statement ok\nSELECT ").append(i).append("\n\n");
        }
        myFixture.configureByText("many_lines.test", sb.toString());
        myFixture.doHighlighting();
    }

    /** Windows line endings (CRLF). */
    public void testWindowsLineEndings() {
        myFixture.configureByText("crlf.test",
                "statement ok\r\nCREATE TABLE t (id INT)\r\n\r\n" +
                "query I\r\nSELECT 1\r\n----\r\n1\r\n");
        myFixture.doHighlighting();
    }

    /** Mixed line endings. */
    public void testMixedLineEndings() {
        myFixture.configureByText("mixed_eol.test",
                "statement ok\nSELECT 1\r\n\rquery I\nSELECT 2\r\n----\n2\r\n");
        myFixture.doHighlighting();
    }

    /** Tab characters in various positions. */
    public void testTabCharacters() {
        myFixture.configureByText("tabs.test",
                "statement\tok\n\tSELECT 1\n\nquery\tI\nSELECT\t42\n----\n42\n");
        myFixture.doHighlighting();
    }

    // =========================================================================
    // 2. Result section edge cases (RESULT_SECTION lexer state)
    // =========================================================================

    /** Result section at EOF with no trailing newline.
     *  Lexer is in RESULT_SECTION state when EOF is reached. */
    public void testResultSectionAtEOF() {
        myFixture.configureByText("result_eof.test",
                "query I\nSELECT 1\n----\n1");
        myFixture.doHighlighting();
    }

    /** Empty result section (---- followed by blank line then directive). */
    public void testEmptyResultSection() {
        myFixture.configureByText("empty_result.test",
                "query I\nSELECT 1\n----\n\nstatement ok\nSELECT 2\n");
        myFixture.doHighlighting();
    }

    /** Only ---- at EOF. */
    public void testResultSeparatorAtEOF() {
        myFixture.configureByText("sep_eof.test",
                "query I\nSELECT 1\n----");
        myFixture.doHighlighting();
    }

    /** ---- followed by single newline at EOF. */
    public void testResultSeparatorNewlineEOF() {
        myFixture.configureByText("sep_nl.test",
                "query I\nSELECT 1\n----\n");
        myFixture.doHighlighting();
    }

    /** Multi-line result with NULLs and empty fields. */
    public void testMultiLineResultWithNulls() {
        myFixture.configureByText("nulls.test",
                "query III\nSELECT * FROM t\n----\n" +
                "1\thello\t3.14\n" +
                "2\tworld\t2.71\n" +
                "NULL\t\t0.0\n" +
                "\nstatement ok\nDROP TABLE t\n");
        myFixture.doHighlighting();
    }

    /** Result containing text that looks like directives. */
    public void testResultLooksLikeDirective() {
        myFixture.configureByText("tricky.test",
                "query I\nSELECT 'statement ok'\n----\n" +
                "statement ok\n\nstatement ok\nSELECT 1\n");
        myFixture.doHighlighting();
    }

    /** Result containing ---- (should not be treated as separator). */
    public void testResultContainsDashes() {
        myFixture.configureByText("dashes.test",
                "query I\nSELECT '----'\n----\n----\n\nstatement ok\nSELECT 1\n");
        myFixture.doHighlighting();
    }

    /** Multiple consecutive queries with result sections. */
    public void testConsecutiveQueries() {
        myFixture.configureByText("consecutive.test",
                "query I\nSELECT 1\n----\n1\n\n" +
                "query I\nSELECT 2\n----\n2\n\n" +
                "query I\nSELECT 3\n----\n3\n");
        myFixture.doHighlighting();
    }

    /** Rapidly alternating between YYINITIAL and RESULT_SECTION. */
    public void testRapidStateTransitions() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 200; i++) {
            sb.append("query I\nSELECT ").append(i).append("\n----\n").append(i).append("\n\n");
        }
        myFixture.configureByText("rapid.test", sb.toString());
        myFixture.doHighlighting();
    }

    // =========================================================================
    // 3. Directive parsing edge cases
    // =========================================================================

    /** All directive types in one file. */
    public void testAllDirectives() {
        myFixture.configureByText("all.test",
                "# comment\n" +
                "require httpfs\n" +
                "require-env S3_BUCKET\n" +
                "mode output_result\n" +
                "skipif duckdb\n" +
                "onlyif duckdb\n" +
                "halt\n\n" +
                "statement ok\nCREATE TABLE t (id INT)\n\n" +
                "statement error\nINVALID SQL\n\n" +
                "statement maybe\nSOMETHING\n\n" +
                "query I\nSELECT 1\n----\n1\n\n" +
                "query II rowsort\nSELECT 1, 2\n----\n1\t2\n\n" +
                "query I nosort\nSELECT 1\n----\n1\n\n" +
                "load __TEST_DIR__/test.db\n\n" +
                "restart\n\n" +
                "PRAGMA enable_verification\n\n" +
                "loop 3\nstatement ok\nSELECT 1\nendloop\n\n" +
                "foreach val a b c\nstatement ok\nSELECT '${val}'\nendforeach\n\n" +
                "concurrentloop 2\nstatement ok\nSELECT 1\nendloop\n\n" +
                "concurrentforeach val x y\nstatement ok\nSELECT '${val}'\nendforeach\n");
        myFixture.doHighlighting();
    }

    /** Nested loops. */
    public void testNestedLoops() {
        myFixture.configureByText("nested.test",
                "loop 3\nloop 2\nstatement ok\nSELECT 1\nendloop\nendloop\n");
        myFixture.doHighlighting();
    }

    /** Mismatched endforeach (annotator flags it, must not crash). */
    public void testMismatchedEndforeach() {
        myFixture.configureByText("bad_foreach.test",
                "foreach val a b c\nstatement ok\nSELECT '${val}'\n");
        myFixture.doHighlighting();
    }

    /** Orphan endloop/endforeach without matching open. */
    public void testOrphanEndloop() {
        myFixture.configureByText("orphan.test", "endloop\nendforeach\n");
        myFixture.doHighlighting();
    }

    // =========================================================================
    // 4. SQL content edge cases
    // =========================================================================

    /** Strings with escaped quotes (the apostrophe bug from issue #10). */
    public void testEscapedQuotes() {
        myFixture.configureByText("escaped.test",
                "statement ok\nSELECT 'it''s a test'\n\n" +
                "query I\nSELECT 'hello''world'\n----\nhello'world\n");
        myFixture.doHighlighting();
    }

    /** Double-quoted identifiers. */
    public void testDoubleQuotedIdentifiers() {
        myFixture.configureByText("dblquote.test",
                "statement ok\nCREATE TABLE \"my table\" (\"my column\" INT)\n");
        myFixture.doHighlighting();
    }

    /** SQL with semicolons. */
    public void testSQLWithSemicolons() {
        myFixture.configureByText("semis.test",
                "statement ok\nCREATE TABLE t (id INT);\n\nstatement ok\nINSERT INTO t VALUES (1);\n");
        myFixture.doHighlighting();
    }

    /** Multiline SQL with all operators. */
    public void testComplexSQL() {
        myFixture.configureByText("complex_sql.test",
                "query II\nSELECT\n  (1 + 2) * 3 - 4 / 2,\n  id\nFROM t\n" +
                "WHERE id > 0 AND id < 100\nORDER BY id\nLIMIT 10\n----\n7\t1\n");
        myFixture.doHighlighting();
    }

    /** Empty SQL body (statement with nothing after). */
    public void testEmptySQLBody() {
        myFixture.configureByText("empty_body.test",
                "statement ok\n\nquery I\n----\n\n");
        myFixture.doHighlighting();
    }

    // =========================================================================
    // 5. Real-world DuckDB patterns
    // =========================================================================

    /** Template variable substitution. */
    public void testTemplateVariables() {
        myFixture.configureByText("template.test",
                "foreach type INTEGER BIGINT DOUBLE VARCHAR\n\n" +
                "statement ok\nCREATE TABLE t_${type} (col ${type})\n\n" +
                "statement ok\nDROP TABLE t_${type}\n\n" +
                "endforeach\n\n" +
                "loop i 0 10\nquery I\nSELECT ${i}\n----\n${i}\n\nendloop\n");
        myFixture.doHighlighting();
    }

    /** __TEST_DIR__ pattern. */
    public void testTestDirPattern() {
        myFixture.configureByText("testdir.test",
                "load __TEST_DIR__/my_database.db\n\n" +
                "statement ok\nCOPY t TO '__TEST_DIR__/output.csv' (HEADER)\n\n" +
                "restart\n\nload __TEST_DIR__/my_database.db\n\n" +
                "query I\nSELECT count(*) FROM t\n----\n42\n");
        myFixture.doHighlighting();
    }

    /** Physical plan output. */
    public void testPhysicalPlanPattern() {
        myFixture.configureByText("plan.test",
                "statement ok\nPRAGMA explain_output='physical_only'\n\n" +
                "query II\nEXPLAIN SELECT * FROM t WHERE id = 1\n----\n" +
                "physical_plan\tSEQ_SCAN(t)\n");
        myFixture.doHighlighting();
    }

    // =========================================================================
    // 6. File extension variants
    // =========================================================================

    /** .benchmark extension. */
    public void testBenchmarkExtension() {
        PsiFile file = myFixture.configureByText("tpch.benchmark",
                "statement ok\nPRAGMA tpch(1)\n");
        assertEquals("Test", file.getLanguage().getID());
        myFixture.doHighlighting();
    }

    /** .testslow extension. */
    public void testTestSlowNoUnderscore() {
        PsiFile file = myFixture.configureByText("slow.testslow",
                "statement ok\nSELECT 1\n");
        assertEquals("Test", file.getLanguage().getID());
        myFixture.doHighlighting();
    }

    // =========================================================================
    // 7. Stress / boundary tests
    // =========================================================================

    /** Result section with very long lines. */
    public void testLongResultLines() {
        StringBuilder sb = new StringBuilder("query I\nSELECT 1\n----\n");
        for (int i = 0; i < 50; i++) {
            for (int j = 0; j < 200; j++) sb.append("data_").append(j).append('\t');
            sb.append('\n');
        }
        sb.append("\nstatement ok\nSELECT 1\n");
        myFixture.configureByText("long_results.test", sb.toString());
        myFixture.doHighlighting();
    }

    /** File that starts with ----. */
    public void testFileStartsWithSeparator() {
        myFixture.configureByText("starts_sep.test", "----\nsome data\nmore data\n");
        myFixture.doHighlighting();
    }

    /** Multiple consecutive blank lines. */
    public void testManyBlankLines() {
        StringBuilder sb = new StringBuilder("statement ok\nSELECT 1\n");
        for (int i = 0; i < 100; i++) sb.append('\n');
        sb.append("statement ok\nSELECT 2\n");
        myFixture.configureByText("blanks.test", sb.toString());
        myFixture.doHighlighting();
    }

    /** 50 comment lines and nothing else. */
    public void testOnlyComments() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 50; i++) sb.append("# Comment ").append(i).append('\n');
        myFixture.configureByText("comments.test", sb.toString());
        myFixture.doHighlighting();
    }

    /** Null byte in content (malformed file). */
    public void testNullBytes() {
        myFixture.configureByText("nullbytes.test", "statement ok\nSELECT 1\0 + 2\n");
        myFixture.doHighlighting();
    }

    /** Content after halt. */
    public void testContentAfterHalt() {
        myFixture.configureByText("after_halt.test",
                "halt\n\nstatement ok\nTHIS SHOULD NOT RUN\n\n" +
                "query I\nSELECT 1\n----\n1\n");
        myFixture.doHighlighting();
    }

    /** Only ----. */
    public void testOnlySeparator() {
        myFixture.configureByText("only_sep.test", "----");
        myFixture.doHighlighting();
    }

    /** ---- followed by ---- immediately. */
    public void testDoubleSeparator() {
        myFixture.configureByText("double_sep.test", "query I\nSELECT 1\n----\n----\n");
        myFixture.doHighlighting();
    }

    /** Large file (1000+ statements). */
    public void testLargeFile() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append("statement ok\nINSERT INTO t VALUES (").append(i).append(")\n\n");
        }
        sb.append("query I\nSELECT count(*) FROM t\n----\n1000\n");
        myFixture.configureByText("large.test", sb.toString());
        myFixture.doHighlighting();
    }

    /** File with only a single directive keyword. */
    public void testSingleDirectiveKeyword() {
        myFixture.configureByText("single_kw.test", "statement");
        myFixture.doHighlighting();
    }

    /** Directive followed immediately by EOF (no space, no arguments). */
    public void testDirectiveAtEOFNoArgs() {
        myFixture.configureByText("dir_eof.test", "query");
        myFixture.doHighlighting();
    }

    /** begin/transaction/rollback sequence. */
    public void testTransactionBlock() {
        myFixture.configureByText("txn.test",
                "statement ok\nbegin transaction\n\n" +
                "statement ok\nINSERT INTO t VALUES (1)\n\n" +
                "statement ok\nrollback\n");
        myFixture.doHighlighting();
    }
}
