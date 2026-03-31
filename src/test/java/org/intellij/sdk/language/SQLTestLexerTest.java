package org.intellij.sdk.language;

import com.intellij.psi.tree.IElementType;
import org.intellij.sdk.language.psi.TestTypes;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.io.IOException;
import java.util.List;

/**
 * Unit tests for the SQLTest lexer token streams.
 * Tests that the lexer produces the expected token types for various inputs.
 */
public class SQLTestLexerTest {

    /**
     * Tokenize input text and return list of (tokenType, tokenText) pairs.
     */
    private List<TokenInfo> tokenize(String input) throws IOException {
        _TestLexer lexer = new _TestLexer(null);
        lexer.reset(input, 0, input.length(), _TestLexer.YYINITIAL);

        List<TokenInfo> tokens = new ArrayList<>();
        IElementType type;
        while ((type = lexer.advance()) != null) {
            String text = input.substring(lexer.getTokenStart(), lexer.getTokenEnd());
            tokens.add(new TokenInfo(type, text));
        }
        return tokens;
    }

    /**
     * Get only non-whitespace tokens.
     */
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

    // ---- Directive keywords ----

    @Test
    public void testStatementKeyword() throws IOException {
        List<TokenInfo> tokens = tokenizeNoWS("statement ok");
        Assert.assertTrue(tokens.size() >= 1);
        Assert.assertEquals(TestTypes.STATEMENT, tokens.get(0).type);
    }

    @Test
    public void testQueryKeyword() throws IOException {
        List<TokenInfo> tokens = tokenizeNoWS("query II");
        Assert.assertTrue(tokens.size() >= 1);
        Assert.assertEquals(TestTypes.QUERY, tokens.get(0).type);
    }

    @Test
    public void testLoopEndloop() throws IOException {
        List<TokenInfo> tokens = tokenizeNoWS("loop\nendloop");
        Assert.assertEquals(TestTypes.LOOP, tokens.get(0).type);
        Assert.assertEquals(TestTypes.ENDLOOP, tokens.get(1).type);
    }

    @Test
    public void testForeachEndforeach() throws IOException {
        List<TokenInfo> tokens = tokenizeNoWS("foreach\nendforeach");
        Assert.assertEquals(TestTypes.FOREACH, tokens.get(0).type);
        Assert.assertEquals(TestTypes.ENDFOREACH, tokens.get(1).type);
    }

    @Test
    public void testRequire() throws IOException {
        List<TokenInfo> tokens = tokenizeNoWS("require tpch");
        Assert.assertEquals(TestTypes.REQUIRE, tokens.get(0).type);
    }

    @Test
    public void testRequireEnv() throws IOException {
        List<TokenInfo> tokens = tokenizeNoWS("require-env S3_BUCKET");
        Assert.assertEquals(TestTypes.REQUIRE_ENV, tokens.get(0).type);
    }

    @Test
    public void testMode() throws IOException {
        List<TokenInfo> tokens = tokenizeNoWS("mode output_hash");
        Assert.assertEquals(TestTypes.MODE, tokens.get(0).type);
    }

    @Test
    public void testHalt() throws IOException {
        List<TokenInfo> tokens = tokenizeNoWS("halt");
        Assert.assertEquals(TestTypes.HALT, tokens.get(0).type);
    }

    @Test
    public void testSkipif() throws IOException {
        List<TokenInfo> tokens = tokenizeNoWS("skipif mysql");
        Assert.assertEquals(TestTypes.SKIPIF, tokens.get(0).type);
    }

    @Test
    public void testOnlyif() throws IOException {
        List<TokenInfo> tokens = tokenizeNoWS("onlyif duckdb");
        Assert.assertEquals(TestTypes.ONLYIF, tokens.get(0).type);
    }

    @Test
    public void testConcurrentloop() throws IOException {
        List<TokenInfo> tokens = tokenizeNoWS("concurrentloop");
        Assert.assertEquals(TestTypes.CONCURRENTLOOP, tokens.get(0).type);
    }

    @Test
    public void testConcurrentforeach() throws IOException {
        List<TokenInfo> tokens = tokenizeNoWS("concurrentforeach");
        Assert.assertEquals(TestTypes.CONCURRENTFOREACH, tokens.get(0).type);
    }

    // ---- Result section ----

    @Test
    public void testResultSeparator() throws IOException {
        List<TokenInfo> tokens = tokenizeNoWS("----");
        Assert.assertEquals(TestTypes.Q_RESULT, tokens.get(0).type);
    }

    @Test
    public void testResultSectionTokenType() throws IOException {
        String input = "----\nhello world\n42\n\nstatement ok";
        List<TokenInfo> tokens = tokenizeNoWS(input);

        // First token: ----
        Assert.assertEquals(TestTypes.Q_RESULT, tokens.get(0).type);

        // Result lines should be RESULT_LINE
        Assert.assertEquals(TestTypes.RESULT_LINE, tokens.get(1).type);
        Assert.assertEquals("hello world", tokens.get(1).text);

        Assert.assertEquals(TestTypes.RESULT_LINE, tokens.get(2).type);
        Assert.assertEquals("42", tokens.get(2).text);

        // After blank line + directive, should switch back
        Assert.assertEquals(TestTypes.STATEMENT, tokens.get(3).type);
    }

    @Test
    public void testResultSectionApostrophe() throws IOException {
        // This was bug #10 — apostrophes in results broke highlighting
        String input = "----\nit's a test\ndon't panic\n\nquery I";
        List<TokenInfo> tokens = tokenizeNoWS(input);

        Assert.assertEquals(TestTypes.Q_RESULT, tokens.get(0).type);
        Assert.assertEquals(TestTypes.RESULT_LINE, tokens.get(1).type);
        Assert.assertEquals("it's a test", tokens.get(1).text);
        Assert.assertEquals(TestTypes.RESULT_LINE, tokens.get(2).type);
        Assert.assertEquals("don't panic", tokens.get(2).text);
        Assert.assertEquals(TestTypes.QUERY, tokens.get(3).type);
    }

    // ---- SQL keywords ----

    @Test
    public void testSQLKeywords() throws IOException {
        // SQL keywords need trailing whitespace in the lexer
        String input = "SELECT FROM WHERE ";
        List<TokenInfo> tokens = tokenize(input);

        // Should have SQL tokens
        boolean foundSQL = false;
        for (TokenInfo t : tokens) {
            if (t.type == TestTypes.SQL) {
                foundSQL = true;
                break;
            }
        }
        Assert.assertTrue("Should find SQL tokens", foundSQL);
    }

    // ---- Comments ----

    @Test
    public void testComment() throws IOException {
        List<TokenInfo> tokens = tokenizeNoWS("# this is a comment");
        Assert.assertEquals(1, tokens.size());
        Assert.assertEquals(TestTypes.COMMENT, tokens.get(0).type);
    }

    // ---- Strings ----

    @Test
    public void testSingleQuotedString() throws IOException {
        List<TokenInfo> tokens = tokenizeNoWS("'hello world'");
        Assert.assertEquals(TestTypes.STRING, tokens.get(0).type);
    }

    @Test
    public void testDoubleQuotedString() throws IOException {
        List<TokenInfo> tokens = tokenizeNoWS("\"hello world\"");
        Assert.assertEquals(TestTypes.STRING, tokens.get(0).type);
    }

    // ---- Numbers ----

    @Test
    public void testInteger() throws IOException {
        List<TokenInfo> tokens = tokenizeNoWS("42");
        Assert.assertEquals(TestTypes.NUMBER, tokens.get(0).type);
    }

    @Test
    public void testDecimal() throws IOException {
        List<TokenInfo> tokens = tokenizeNoWS("3.14");
        Assert.assertEquals(TestTypes.NUMBER, tokens.get(0).type);
    }

    // ---- Operators ----

    @Test
    public void testOperators() throws IOException {
        List<TokenInfo> tokens = tokenizeNoWS(";:=().-+,*<>");
        Assert.assertEquals(TestTypes.SEMI, tokens.get(0).type);
        Assert.assertEquals(TestTypes.DDOT, tokens.get(1).type);
        Assert.assertEquals(TestTypes.EQ, tokens.get(2).type);
        Assert.assertEquals(TestTypes.LP, tokens.get(3).type);
        Assert.assertEquals(TestTypes.RP, tokens.get(4).type);
        Assert.assertEquals(TestTypes.DOT, tokens.get(5).type);
        Assert.assertEquals(TestTypes.MINUS, tokens.get(6).type);
        Assert.assertEquals(TestTypes.PLUS, tokens.get(7).type);
        Assert.assertEquals(TestTypes.COMMA, tokens.get(8).type);
        Assert.assertEquals(TestTypes.STAR, tokens.get(9).type);
        Assert.assertEquals(TestTypes.LESS, tokens.get(10).type);
        Assert.assertEquals(TestTypes.HIGHER, tokens.get(11).type);
    }

    // ---- Full test file scenario ----

    @Test
    public void testFullTestFile() throws IOException {
        String input = "# Test file\n" +
                "require tpch\n\n" +
                "statement ok\n" +
                "CREATE TABLE test (id INTEGER);\n\n" +
                "query I\n" +
                "SELECT id FROM test;\n" +
                "----\n" +
                "42\n\n" +
                "loop i 0 10\n" +
                "statement ok\n" +
                "INSERT INTO test VALUES (${i});\n" +
                "endloop\n";

        List<TokenInfo> tokens = tokenize(input);
        // Just verify it doesn't crash and produces tokens
        Assert.assertTrue("Should produce tokens", tokens.size() > 10);

        // Find key directive tokens
        boolean foundRequire = false, foundStatement = false, foundQuery = false;
        boolean foundLoop = false, foundEndloop = false, foundResult = false;
        for (TokenInfo t : tokens) {
            if (t.type == TestTypes.REQUIRE) foundRequire = true;
            if (t.type == TestTypes.STATEMENT) foundStatement = true;
            if (t.type == TestTypes.QUERY) foundQuery = true;
            if (t.type == TestTypes.LOOP) foundLoop = true;
            if (t.type == TestTypes.ENDLOOP) foundEndloop = true;
            if (t.type == TestTypes.Q_RESULT) foundResult = true;
        }
        Assert.assertTrue("Should find require", foundRequire);
        Assert.assertTrue("Should find statement", foundStatement);
        Assert.assertTrue("Should find query", foundQuery);
        Assert.assertTrue("Should find loop", foundLoop);
        Assert.assertTrue("Should find endloop", foundEndloop);
        Assert.assertTrue("Should find ----", foundResult);
    }

    @Test
    public void testResultSectionExitOnComment() throws IOException {
        // Result section should exit when it sees a comment directive
        String input = "----\nresult1\nresult2\n\n# comment after";
        List<TokenInfo> tokens = tokenizeNoWS(input);

        Assert.assertEquals(TestTypes.Q_RESULT, tokens.get(0).type);
        Assert.assertEquals(TestTypes.RESULT_LINE, tokens.get(1).type);
        Assert.assertEquals(TestTypes.RESULT_LINE, tokens.get(2).type);
        Assert.assertEquals(TestTypes.COMMENT, tokens.get(3).type);
    }

    @Test
    public void testQueryReturnType() throws IOException {
        // Return type is capital letters followed by whitespace
        String input = "II\n";
        List<TokenInfo> tokens = tokenizeNoWS(input);
        Assert.assertEquals(TestTypes.QUERY_RETURN_TYPE, tokens.get(0).type);
    }

    // ---- New directive tokens ----

    @Test public void testSleep() throws IOException {
        Assert.assertEquals(TestTypes.SLEEP, tokenizeNoWS("sleep 1 second").get(0).type);
    }
    @Test public void testHashThreshold() throws IOException {
        Assert.assertEquals(TestTypes.HASH_THRESHOLD, tokenizeNoWS("hash-threshold 100").get(0).type);
    }
    @Test public void testUnzip() throws IOException {
        Assert.assertEquals(TestTypes.UNZIP, tokenizeNoWS("unzip file.gz").get(0).type);
    }
    @Test public void testTestEnv() throws IOException {
        Assert.assertEquals(TestTypes.TEST_ENV, tokenizeNoWS("test-env VAR val").get(0).type);
    }
    @Test public void testTags() throws IOException {
        Assert.assertEquals(TestTypes.TAGS, tokenizeNoWS("tags nightly").get(0).type);
    }
    @Test public void testContinue() throws IOException {
        Assert.assertEquals(TestTypes.CONTINUE, tokenizeNoWS("continue").get(0).type);
    }
    @Test public void testReconnect() throws IOException {
        Assert.assertEquals(TestTypes.RECONNECT, tokenizeNoWS("reconnect").get(0).type);
    }
    @Test public void testTemplate() throws IOException {
        Assert.assertEquals(TestTypes.TEMPLATE, tokenizeNoWS("template bench.in").get(0).type);
    }
    @Test public void testCache() throws IOException {
        Assert.assertEquals(TestTypes.CACHE, tokenizeNoWS("cache db.duckdb").get(0).type);
    }
    @Test public void testCacheFile() throws IOException {
        Assert.assertEquals(TestTypes.CACHE_FILE, tokenizeNoWS("cache_file data.csv").get(0).type);
    }
    @Test public void testCleanup() throws IOException {
        Assert.assertEquals(TestTypes.CLEANUP, tokenizeNoWS("cleanup").get(0).type);
    }
    @Test public void testInit() throws IOException {
        Assert.assertEquals(TestTypes.INIT, tokenizeNoWS("init").get(0).type);
    }
    @Test public void testReload() throws IOException {
        Assert.assertEquals(TestTypes.RELOAD, tokenizeNoWS("reload").get(0).type);
    }
    @Test public void testResultmode() throws IOException {
        Assert.assertEquals(TestTypes.RESULTMODE, tokenizeNoWS("resultmode streaming").get(0).type);
    }
    @Test public void testResultQuery() throws IOException {
        Assert.assertEquals(TestTypes.RESULT_QUERY, tokenizeNoWS("result_query 3").get(0).type);
    }
    @Test public void testAssert() throws IOException {
        Assert.assertEquals(TestTypes.ASSERT, tokenizeNoWS("assert I").get(0).type);
    }
    @Test public void testRun() throws IOException {
        Assert.assertEquals(TestTypes.RUN, tokenizeNoWS("run queries/q01.sql").get(0).type);
    }
    @Test public void testInclude() throws IOException {
        Assert.assertEquals(TestTypes.INCLUDE, tokenizeNoWS("include other.test").get(0).type);
    }
    @Test public void testArgument() throws IOException {
        Assert.assertEquals(TestTypes.ARGUMENT, tokenizeNoWS("argument sf 1").get(0).type);
    }
    @Test public void testSubgroup() throws IOException {
        Assert.assertEquals(TestTypes.SUBGROUP, tokenizeNoWS("subgroup sf1").get(0).type);
    }
    @Test public void testStorage() throws IOException {
        Assert.assertEquals(TestTypes.STORAGE, tokenizeNoWS("storage persistent").get(0).type);
    }
    @Test public void testRetry() throws IOException {
        Assert.assertEquals(TestTypes.RETRY, tokenizeNoWS("retry load 3").get(0).type);
    }
    @Test public void testRequireReinit() throws IOException {
        Assert.assertEquals(TestTypes.REQUIRE_REINIT, tokenizeNoWS("require_reinit").get(0).type);
    }

    // ---- New tokens: case insensitivity ----

    @Test public void testSleepCaseInsensitive() throws IOException {
        Assert.assertEquals(TestTypes.SLEEP, tokenizeNoWS("SLEEP 1 second").get(0).type);
    }
    @Test public void testTemplateCaseInsensitive() throws IOException {
        Assert.assertEquals(TestTypes.TEMPLATE, tokenizeNoWS("TEMPLATE bench.in").get(0).type);
    }

    // ---- Result section content should be RESULT_LINE tokens ----

    @Test public void testResultSectionContent() throws IOException {
        // Result values that look like keywords should still be RESULT_LINE
        String input = "query I\nSELECT 1\n----\nhello world\n42\n\n";
        List<TokenInfo> tokens = tokenize(input);
        boolean foundResultLine = false;
        boolean afterSeparator = false;
        for (TokenInfo t : tokens) {
            if (t.type == TestTypes.Q_RESULT) afterSeparator = true;
            if (afterSeparator && t.type == TestTypes.RESULT_LINE) foundResultLine = true;
        }
        Assert.assertTrue("Result lines should exist after ----", foundResultLine);
    }

    @Test public void testResultSectionExitsOnBlankLine() throws IOException {
        // After blank line following results, next directive should be recognized
        String input = "query I\nSELECT 1\n----\n42\n\nstatement ok\nSELECT 2\n";
        List<TokenInfo> tokens = tokenize(input);
        boolean foundStatement = false;
        boolean afterResult = false;
        for (TokenInfo t : tokens) {
            if (t.type == TestTypes.Q_RESULT) afterResult = true;
            if (afterResult && t.type == TestTypes.STATEMENT) foundStatement = true;
        }
        Assert.assertTrue("Statement should be recognized after result section ends", foundStatement);
    }

    // ---- Helper ----

    static class TokenInfo {
        final IElementType type;
        final String text;

        TokenInfo(IElementType type, String text) {
            this.type = type;
            this.text = text;
        }

        @Override
        public String toString() {
            return type + "[" + text + "]";
        }
    }
}
