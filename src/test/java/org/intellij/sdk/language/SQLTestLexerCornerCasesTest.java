package org.intellij.sdk.language;

import com.intellij.psi.tree.IElementType;
import org.intellij.sdk.language.psi.TestTypes;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.io.IOException;
import java.util.List;

/**
 * Corner case lexer tests: verifies token streams for tricky inputs.
 *
 * Categories:
 * 1. RESULT_SECTION state management
 * 2. reset() with non-zero start offset (incremental re-lexing)
 * 3. Case insensitivity (%caseless)
 * 4. Buffer boundary conditions
 * 5. Special character handling
 * 6. zzRefill override validation
 */
public class SQLTestLexerCornerCasesTest {

    private List<TokenInfo> tokenize(String input) throws IOException {
        return tokenize(input, 0, input.length(), _TestLexer.YYINITIAL);
    }

    private List<TokenInfo> tokenize(String input, int start, int end, int state) throws IOException {
        _TestLexer lexer = new _TestLexer(null);
        lexer.reset(input, start, end, state);
        List<TokenInfo> tokens = new ArrayList<>();
        IElementType type;
        while ((type = lexer.advance()) != null) {
            String text = input.substring(lexer.getTokenStart(), lexer.getTokenEnd());
            tokens.add(new TokenInfo(type, text));
        }
        return tokens;
    }

    private List<TokenInfo> tokenizeNoWS(String input) throws IOException {
        List<TokenInfo> all = tokenize(input);
        List<TokenInfo> result = new ArrayList<>();
        for (TokenInfo t : all) {
            if (t.type != com.intellij.psi.TokenType.WHITE_SPACE) {
                result.add(t);
            }
        }
        return result;
    }

    // =========================================================================
    // 1. RESULT_SECTION state
    // =========================================================================

    /** Lexer should exit RESULT_SECTION on blank line + directive. */
    @Test
    public void testResultExitOnBlankLine() throws IOException {
        String input = "----\nresult data\n\nstatement ok\n";
        List<TokenInfo> tokens = tokenizeNoWS(input);
        Assert.assertEquals(TestTypes.Q_RESULT, tokens.get(0).type);
        Assert.assertEquals(TestTypes.RESULT_LINE, tokens.get(1).type);
        Assert.assertEquals(TestTypes.STATEMENT, tokens.get(2).type);
    }

    /** Lexer should exit RESULT_SECTION when it sees a directive line. */
    @Test
    public void testResultExitOnDirective() throws IOException {
        String input = "----\nresult\nstatement ok\n";
        List<TokenInfo> tokens = tokenizeNoWS(input);
        Assert.assertEquals(TestTypes.Q_RESULT, tokens.get(0).type);
        Assert.assertEquals(TestTypes.RESULT_LINE, tokens.get(1).type);
        // The newline after "result" triggers lookahead for "statement"
        Assert.assertEquals(TestTypes.STATEMENT, tokens.get(2).type);
    }

    /** Result section with no content — just ---- then blank line. */
    @Test
    public void testEmptyResultSection() throws IOException {
        String input = "----\n\nstatement ok\n";
        List<TokenInfo> tokens = tokenizeNoWS(input);
        Assert.assertEquals(TestTypes.Q_RESULT, tokens.get(0).type);
        Assert.assertEquals(TestTypes.STATEMENT, tokens.get(1).type);
    }

    /** Result section at EOF — lexer must not loop. */
    @Test
    public void testResultSectionAtEOF() throws IOException {
        String input = "----\nlast line";
        List<TokenInfo> tokens = tokenizeNoWS(input);
        Assert.assertEquals(TestTypes.Q_RESULT, tokens.get(0).type);
        Assert.assertEquals(TestTypes.RESULT_LINE, tokens.get(1).type);
        Assert.assertEquals(2, tokens.size());
    }

    /** Result section: ---- at EOF with no content. */
    @Test
    public void testResultSeparatorAtEOF() throws IOException {
        String input = "----";
        List<TokenInfo> tokens = tokenizeNoWS(input);
        Assert.assertEquals(1, tokens.size());
        Assert.assertEquals(TestTypes.Q_RESULT, tokens.get(0).type);
    }

    /** Result with apostrophes (regression for bug #10). */
    @Test
    public void testResultApostrophes() throws IOException {
        String input = "----\nit's\ndon't\ncan't\n\nquery I\n";
        List<TokenInfo> tokens = tokenizeNoWS(input);
        Assert.assertEquals(TestTypes.Q_RESULT, tokens.get(0).type);
        Assert.assertEquals("it's", tokens.get(1).text);
        Assert.assertEquals("don't", tokens.get(2).text);
        Assert.assertEquals("can't", tokens.get(3).text);
        Assert.assertEquals(TestTypes.QUERY, tokens.get(4).type);
    }

    /** Result with special chars that are operators in YYINITIAL. */
    @Test
    public void testResultWithOperators() throws IOException {
        String input = "----\n1 + 2 = 3\n(a, b)\n<html>\n\nquery I\n";
        List<TokenInfo> tokens = tokenizeNoWS(input);
        Assert.assertEquals(TestTypes.Q_RESULT, tokens.get(0).type);
        // In RESULT_SECTION, everything is RESULT_LINE
        Assert.assertEquals(TestTypes.RESULT_LINE, tokens.get(1).type);
        Assert.assertEquals("1 + 2 = 3", tokens.get(1).text);
        Assert.assertEquals(TestTypes.RESULT_LINE, tokens.get(2).type);
    }

    /** Result exit on every directive type. */
    @Test
    public void testResultExitOnAllDirectives() throws IOException {
        String[] directives = {
            "statement ok", "query I", "load test.db", "loop 3",
            "endloop", "foreach v a b", "endforeach",
            "concurrentloop 2", "concurrentforeach v a b",
            "restart", "begin", "halt", "require httpfs",
            "require-env HOME", "mode output_result",
            "skipif duckdb", "onlyif duckdb", "# comment"
        };
        for (String dir : directives) {
            String input = "----\nresult\n" + dir + "\n";
            List<TokenInfo> tokens = tokenizeNoWS(input);
            Assert.assertTrue("Should exit RESULT_SECTION before: " + dir,
                    tokens.size() >= 3);
            Assert.assertEquals("First token should be Q_RESULT for: " + dir,
                    TestTypes.Q_RESULT, tokens.get(0).type);
            Assert.assertEquals("Second token should be RESULT_LINE for: " + dir,
                    TestTypes.RESULT_LINE, tokens.get(1).type);
            // Third token should be the directive, not RESULT_LINE
            Assert.assertNotEquals("Third token should NOT be RESULT_LINE for: " + dir,
                    TestTypes.RESULT_LINE, tokens.get(2).type);
        }
    }

    /** Multiple ---- in sequence. */
    @Test
    public void testMultipleSeparators() throws IOException {
        String input = "----\n----\n----\n\nstatement ok\n";
        List<TokenInfo> tokens = tokenizeNoWS(input);
        // First ---- enters RESULT_SECTION, subsequent ---- are RESULT_LINE
        Assert.assertEquals(TestTypes.Q_RESULT, tokens.get(0).type);
        Assert.assertEquals(TestTypes.RESULT_LINE, tokens.get(1).type);
        Assert.assertEquals("----", tokens.get(1).text);
    }

    // =========================================================================
    // 2. Non-zero start offset (incremental re-lexing)
    // =========================================================================

    /** FlexAdapter calls reset with start > 0 for incremental re-lexing. */
    @Test
    public void testNonZeroStartOffset() throws IOException {
        String input = "statement ok\nSELECT 1\n";
        // Start from position 13 ("SELECT 1\n")
        List<TokenInfo> tokens = tokenize(input, 13, input.length(), _TestLexer.YYINITIAL);
        Assert.assertTrue("Should produce tokens from offset 13", tokens.size() >= 1);
        // Should see SQL token for SELECT
        boolean foundSQL = false;
        for (TokenInfo t : tokens) {
            if (t.type == TestTypes.SQL) {
                foundSQL = true;
                break;
            }
        }
        Assert.assertTrue("Should find SQL token starting from offset", foundSQL);
    }

    /** Start in RESULT_SECTION state (as FlexAdapter would after incremental change). */
    @Test
    public void testStartInResultState() throws IOException {
        String input = "prefix----\nresult data\n\nstatement ok\n";
        // Start from after "prefix" at the ---- position, in RESULT_SECTION state
        int start = 10; // position of "----"
        List<TokenInfo> tokens = tokenize(input, start, input.length(), _TestLexer.RESULT_SECTION);
        // Should get RESULT_LINE tokens
        boolean foundResult = false;
        for (TokenInfo t : tokens) {
            if (t.type == TestTypes.RESULT_LINE) {
                foundResult = true;
                break;
            }
        }
        Assert.assertTrue("Should find RESULT_LINE when starting in RESULT_SECTION", foundResult);
    }

    /** Start offset = end (empty range). */
    @Test
    public void testZeroLengthRange() throws IOException {
        String input = "test";
        List<TokenInfo> tokens = tokenize(input, 4, 4, _TestLexer.YYINITIAL);
        Assert.assertEquals("Zero-length range should produce no tokens", 0, tokens.size());
    }

    // =========================================================================
    // 3. Case insensitivity (%caseless)
    // =========================================================================

    /** Uppercase directives should be recognized. */
    @Test
    public void testUppercaseStatement() throws IOException {
        List<TokenInfo> tokens = tokenizeNoWS("STATEMENT ok");
        Assert.assertEquals(TestTypes.STATEMENT, tokens.get(0).type);
    }

    @Test
    public void testMixedCaseQuery() throws IOException {
        List<TokenInfo> tokens = tokenizeNoWS("QuErY II");
        Assert.assertEquals(TestTypes.QUERY, tokens.get(0).type);
    }

    @Test
    public void testUppercaseLoop() throws IOException {
        List<TokenInfo> tokens = tokenizeNoWS("LOOP\nENDLOOP");
        Assert.assertEquals(TestTypes.LOOP, tokens.get(0).type);
        Assert.assertEquals(TestTypes.ENDLOOP, tokens.get(1).type);
    }

    @Test
    public void testUppercaseRequire() throws IOException {
        List<TokenInfo> tokens = tokenizeNoWS("REQUIRE httpfs");
        Assert.assertEquals(TestTypes.REQUIRE, tokens.get(0).type);
    }

    @Test
    public void testUppercaseHalt() throws IOException {
        List<TokenInfo> tokens = tokenizeNoWS("HALT");
        Assert.assertEquals(TestTypes.HALT, tokens.get(0).type);
    }

    // =========================================================================
    // 4. Buffer boundary conditions
    // =========================================================================

    /** Single character input. */
    @Test
    public void testSingleCharInput() throws IOException {
        List<TokenInfo> tokens = tokenize("x");
        Assert.assertEquals(1, tokens.size());
    }

    /** Single newline. */
    @Test
    public void testSingleNewline() throws IOException {
        List<TokenInfo> tokens = tokenize("\n");
        Assert.assertEquals(1, tokens.size());
        Assert.assertEquals(com.intellij.psi.TokenType.WHITE_SPACE, tokens.get(0).type);
    }

    /** Empty input. */
    @Test
    public void testEmptyInput() throws IOException {
        List<TokenInfo> tokens = tokenize("");
        Assert.assertEquals(0, tokens.size());
    }

    /** Very long identifier. */
    @Test
    public void testVeryLongIdentifier() throws IOException {
        StringBuilder sb = new StringBuilder("a");
        for (int i = 0; i < 5000; i++) sb.append('b');
        String input = sb.toString();
        List<TokenInfo> tokens = tokenize(input);
        Assert.assertTrue("Should produce at least one token", tokens.size() >= 1);
    }

    /** Lexer consumes entire input — final position must equal end. */
    @Test
    public void testLexerConsumesAll() throws IOException {
        String[] inputs = {
            "", "\n", "x", "statement ok\n", "query I\nSELECT 1\n----\n1\n",
            "----\nresult\n\nstatement ok\n",
            "----\nlast"
        };
        for (String input : inputs) {
            _TestLexer lexer = new _TestLexer(null);
            lexer.reset(input, 0, input.length(), _TestLexer.YYINITIAL);
            IElementType type;
            int lastEnd = 0;
            while ((type = lexer.advance()) != null) {
                lastEnd = lexer.getTokenEnd();
            }
            if (!input.isEmpty()) {
                Assert.assertEquals("Lexer should consume all input: '" + input.replace("\n", "\\n") + "'",
                        input.length(), lastEnd);
            }
        }
    }

    // =========================================================================
    // 5. Special characters
    // =========================================================================

    /** Null byte in input. */
    @Test
    public void testNullByte() throws IOException {
        List<TokenInfo> tokens = tokenize("statement ok\nSELECT \0\n");
        Assert.assertTrue("Should produce tokens even with null byte", tokens.size() >= 1);
    }

    /** Unicode in YYINITIAL state. */
    @Test
    public void testUnicodeInInitial() throws IOException {
        List<TokenInfo> tokens = tokenize("\u65e5\u672c\u8a9e");
        Assert.assertTrue("Should handle unicode", tokens.size() >= 1);
    }

    /** Unicode in RESULT_SECTION state. */
    @Test
    public void testUnicodeInResult() throws IOException {
        String input = "----\n\u65e5\u672c\u8a9e\n\nstatement ok\n";
        List<TokenInfo> tokens = tokenizeNoWS(input);
        Assert.assertEquals(TestTypes.RESULT_LINE, tokens.get(1).type);
        Assert.assertEquals("\u65e5\u672c\u8a9e", tokens.get(1).text);
    }

    /** Surrogate pair (emoji). */
    @Test
    public void testSurrogatePair() throws IOException {
        String input = "----\n\uD83E\uDD86\n\nquery I\n";
        List<TokenInfo> tokens = tokenizeNoWS(input);
        Assert.assertEquals(TestTypes.RESULT_LINE, tokens.get(1).type);
        Assert.assertEquals("\uD83E\uDD86", tokens.get(1).text);
    }

    /** Tab-separated result (common DuckDB output format). */
    @Test
    public void testTabSeparatedResult() throws IOException {
        String input = "----\n1\thello\t3.14\nNULL\t\t0\n\nquery I\n";
        List<TokenInfo> tokens = tokenizeNoWS(input);
        Assert.assertEquals(TestTypes.RESULT_LINE, tokens.get(1).type);
        Assert.assertEquals("1\thello\t3.14", tokens.get(1).text);
    }

    /** CRLF line endings. */
    @Test
    public void testCRLF() throws IOException {
        String input = "statement ok\r\nSELECT 1\r\n";
        List<TokenInfo> tokens = tokenizeNoWS(input);
        Assert.assertEquals(TestTypes.STATEMENT, tokens.get(0).type);
    }

    /** Carriage return only. */
    @Test
    public void testCROnly() throws IOException {
        String input = "statement ok\rSELECT 1\r";
        List<TokenInfo> tokens = tokenizeNoWS(input);
        Assert.assertEquals(TestTypes.STATEMENT, tokens.get(0).type);
    }

    // =========================================================================
    // 6. Token position accuracy
    // =========================================================================

    /** Verify getTokenStart/getTokenEnd match actual positions. */
    @Test
    public void testTokenPositionAccuracy() throws IOException {
        String input = "statement ok\nSELECT 1\n";
        _TestLexer lexer = new _TestLexer(null);
        lexer.reset(input, 0, input.length(), _TestLexer.YYINITIAL);
        IElementType type;
        int prevEnd = 0;
        while ((type = lexer.advance()) != null) {
            int start = lexer.getTokenStart();
            int end = lexer.getTokenEnd();
            Assert.assertTrue("Token start must be >= 0", start >= 0);
            Assert.assertTrue("Token end must be > start (or equal for edge)", end >= start);
            Assert.assertTrue("Token start must be >= previous end: pos " + start + " vs " + prevEnd,
                    start >= prevEnd);
            Assert.assertTrue("Token end must be <= input length", end <= input.length());
            // Verify extracted text is valid
            String text = input.substring(start, end);
            Assert.assertNotNull(text);
            prevEnd = end;
        }
    }

    /** No gaps between tokens — every character must be covered. */
    @Test
    public void testNoGapsBetweenTokens() throws IOException {
        String[] inputs = {
            "statement ok\nSELECT 1\n",
            "query I\nSELECT 1\n----\n1\n\nstatement ok\n",
            "# comment\nrequire httpfs\nhalt\n",
            "loop 3\nstatement ok\nSELECT 1\nendloop\n"
        };
        for (String input : inputs) {
            _TestLexer lexer = new _TestLexer(null);
            lexer.reset(input, 0, input.length(), _TestLexer.YYINITIAL);
            IElementType type;
            int expectedStart = 0;
            while ((type = lexer.advance()) != null) {
                Assert.assertEquals("No gaps in token stream for: " + input.replace("\n","\\n"),
                        expectedStart, lexer.getTokenStart());
                expectedStart = lexer.getTokenEnd();
            }
            Assert.assertEquals("All input consumed for: " + input.replace("\n","\\n"),
                    input.length(), expectedStart);
        }
    }

    static class TokenInfo {
        final IElementType type;
        final String text;
        TokenInfo(IElementType type, String text) {
            this.type = type;
            this.text = text;
        }
    }
}
