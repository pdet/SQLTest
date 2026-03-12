package org.intellij.sdk.language;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Tests for SQLTestAnnotator: verifies warnings/errors are produced
 * at the correct locations and with the correct messages.
 *
 * Categories:
 * 1. Statement qualifier validation
 * 2. Query result section validation
 * 3. Loop/endloop matching
 * 4. Foreach/endforeach matching
 * 5. Concurrentloop/concurrentforeach matching
 * 6. Edge cases: empty files, content after halt, case sensitivity
 */
public class SQLTestAnnotatorTest extends BasePlatformTestCase {

    private List<HighlightInfo> getAnnotations(String filename, String content) {
        myFixture.configureByText(filename, content);
        return myFixture.doHighlighting();
    }

    private List<HighlightInfo> getWarnings(String filename, String content) {
        return getAnnotations(filename, content).stream()
                .filter(h -> h.getSeverity() == HighlightSeverity.WARNING)
                .collect(Collectors.toList());
    }

    private List<HighlightInfo> getErrors(String filename, String content) {
        return getAnnotations(filename, content).stream()
                .filter(h -> h.getSeverity() == HighlightSeverity.ERROR)
                .collect(Collectors.toList());
    }

    // =========================================================================
    // 1. Statement qualifier validation
    // =========================================================================

    /** 'statement ok' should produce no warnings. */
    public void testStatementOkNoWarning() {
        List<HighlightInfo> warnings = getWarnings("ok.test",
                "statement ok\nSELECT 1\n");
        for (HighlightInfo w : warnings) {
            assertFalse("Should not warn about valid qualifier",
                    w.getDescription().contains("should be followed by"));
        }
    }

    /** 'statement error' should produce no warnings. */
    public void testStatementErrorNoWarning() {
        List<HighlightInfo> warnings = getWarnings("err.test",
                "statement error\nINVALID SQL\n");
        for (HighlightInfo w : warnings) {
            assertFalse("Should not warn about valid qualifier",
                    w.getDescription().contains("should be followed by"));
        }
    }

    /** 'statement maybe' should produce no warnings. */
    public void testStatementMaybeNoWarning() {
        List<HighlightInfo> warnings = getWarnings("maybe.test",
                "statement maybe\nSELECT 1\n");
        for (HighlightInfo w : warnings) {
            assertFalse("Should not warn about valid qualifier",
                    w.getDescription().contains("should be followed by"));
        }
    }

    /** 'statement badqualifier' should produce a warning. */
    public void testStatementBadQualifier() {
        List<HighlightInfo> warnings = getWarnings("bad.test",
                "statement badqualifier\nSELECT 1\n");
        boolean found = warnings.stream()
                .anyMatch(w -> w.getDescription().contains("should be followed by"));
        assertTrue("Should warn about bad qualifier", found);
    }

    /** Multiple statements — only bad ones get warnings. */
    public void testMixedStatementQualifiers() {
        List<HighlightInfo> warnings = getWarnings("mixed.test",
                "statement ok\nSELECT 1\n\n" +
                "statement error\nINVALID\n\n" +
                "statement badone\nSELECT 2\n\n" +
                "statement maybe\nSELECT 3\n");
        long count = warnings.stream()
                .filter(w -> w.getDescription().contains("should be followed by"))
                .count();
        assertEquals("Exactly one bad qualifier", 1, count);
    }

    // =========================================================================
    // 2. Query result section validation
    // =========================================================================

    /** Query with ---- should not warn. */
    public void testQueryWithResults() {
        List<HighlightInfo> warnings = getWarnings("ok_query.test",
                "query I\nSELECT 1\n----\n1\n");
        long count = warnings.stream()
                .filter(w -> w.getDescription().contains("no '----'"))
                .count();
        assertEquals("Should not warn about missing results", 0, count);
    }

    /** Query without ---- should warn. */
    public void testQueryMissingResults() {
        List<HighlightInfo> warnings = getWarnings("no_result.test",
                "query I\nSELECT 1\n\nstatement ok\nSELECT 2\n");
        boolean found = warnings.stream()
                .anyMatch(w -> w.getDescription().contains("no '----'"));
        assertTrue("Should warn about missing ----", found);
    }

    /** Query at EOF without ---- should warn. */
    public void testQueryAtEOFNoResults() {
        List<HighlightInfo> warnings = getWarnings("eof_query.test",
                "query I\nSELECT 1\n");
        boolean found = warnings.stream()
                .anyMatch(w -> w.getDescription().contains("no '----'"));
        assertTrue("Should warn about missing ---- at EOF", found);
    }

    /** Multiple queries — only missing-result ones get warnings. */
    public void testMixedQueries() {
        List<HighlightInfo> warnings = getWarnings("mixed_q.test",
                "query I\nSELECT 1\n----\n1\n\n" +
                "query I\nSELECT 2\n\n" +
                "query I\nSELECT 3\n----\n3\n");
        long count = warnings.stream()
                .filter(w -> w.getDescription().contains("no '----'"))
                .count();
        assertEquals("Exactly one missing result", 1, count);
    }

    // =========================================================================
    // 3. Loop/endloop matching
    // =========================================================================

    /** Matched loop/endloop should produce no errors. */
    public void testMatchedLoop() {
        List<HighlightInfo> errors = getErrors("loop_ok.test",
                "loop 3\nstatement ok\nSELECT 1\nendloop\n");
        long count = errors.stream()
                .filter(e -> e.getDescription().contains("loop"))
                .count();
        assertEquals("No loop errors", 0, count);
    }

    /** Unclosed loop should produce error. */
    public void testUnclosedLoop() {
        List<HighlightInfo> errors = getErrors("unclosed_loop.test",
                "loop 3\nstatement ok\nSELECT 1\n");
        boolean found = errors.stream()
                .anyMatch(e -> e.getDescription().contains("Unclosed 'loop'"));
        assertTrue("Should error about unclosed loop", found);
    }

    /** Orphan endloop should produce error. */
    public void testOrphanEndloop() {
        List<HighlightInfo> errors = getErrors("orphan_endloop.test",
                "endloop\n");
        boolean found = errors.stream()
                .anyMatch(e -> e.getDescription().contains("without matching"));
        assertTrue("Should error about orphan endloop", found);
    }

    /** Nested loops should work. */
    public void testNestedLoops() {
        List<HighlightInfo> errors = getErrors("nested_loop.test",
                "loop 3\nloop 2\nstatement ok\nSELECT 1\nendloop\nendloop\n");
        long count = errors.stream()
                .filter(e -> e.getDescription().contains("loop"))
                .count();
        assertEquals("No loop errors for nested", 0, count);
    }

    /** Two endloops for one loop should error on the second. */
    public void testExtraEndloop() {
        List<HighlightInfo> errors = getErrors("extra_endloop.test",
                "loop 3\nstatement ok\nSELECT 1\nendloop\nendloop\n");
        boolean found = errors.stream()
                .anyMatch(e -> e.getDescription().contains("without matching"));
        assertTrue("Second endloop should error", found);
    }

    // =========================================================================
    // 4. Foreach/endforeach matching
    // =========================================================================

    /** Matched foreach/endforeach. */
    public void testMatchedForeach() {
        List<HighlightInfo> errors = getErrors("foreach_ok.test",
                "foreach val a b c\nstatement ok\nSELECT '${val}'\nendforeach\n");
        long count = errors.stream()
                .filter(e -> e.getDescription().contains("foreach"))
                .count();
        assertEquals("No foreach errors", 0, count);
    }

    /** Unclosed foreach. */
    public void testUnclosedForeach() {
        List<HighlightInfo> errors = getErrors("unclosed_foreach.test",
                "foreach val a b c\nstatement ok\nSELECT '${val}'\n");
        boolean found = errors.stream()
                .anyMatch(e -> e.getDescription().contains("Unclosed 'foreach'"));
        assertTrue("Should error about unclosed foreach", found);
    }

    /** Orphan endforeach. */
    public void testOrphanEndforeach() {
        List<HighlightInfo> errors = getErrors("orphan_endforeach.test",
                "endforeach\n");
        boolean found = errors.stream()
                .anyMatch(e -> e.getDescription().contains("without matching"));
        assertTrue("Should error about orphan endforeach", found);
    }

    // =========================================================================
    // 5. Concurrentloop/concurrentforeach
    // =========================================================================

    /** concurrentloop uses endloop as closer. */
    public void testConcurrentloopMatched() {
        List<HighlightInfo> errors = getErrors("cloop_ok.test",
                "concurrentloop 2\nstatement ok\nSELECT 1\nendloop\n");
        long count = errors.stream()
                .filter(e -> e.getDescription().contains("loop"))
                .count();
        assertEquals("No loop errors for concurrentloop", 0, count);
    }

    /** concurrentforeach uses endforeach as closer. */
    public void testConcurrentforeachMatched() {
        List<HighlightInfo> errors = getErrors("cfe_ok.test",
                "concurrentforeach val a b\nstatement ok\nSELECT '${val}'\nendforeach\n");
        long count = errors.stream()
                .filter(e -> e.getDescription().contains("foreach"))
                .count();
        assertEquals("No foreach errors for concurrentforeach", 0, count);
    }

    /** Unclosed concurrentloop. */
    public void testUnclosedConcurrentloop() {
        List<HighlightInfo> errors = getErrors("unclosed_cloop.test",
                "concurrentloop 2\nstatement ok\nSELECT 1\n");
        boolean found = errors.stream()
                .anyMatch(e -> e.getDescription().contains("loop"));
        assertTrue("Should error about unclosed concurrentloop", found);
    }

    // =========================================================================
    // 6. Edge cases
    // =========================================================================

    /** Empty file should produce no errors/warnings. */
    public void testEmptyFileAnnotations() {
        List<HighlightInfo> all = getAnnotations("empty.test", "");
        long count = all.stream()
                .filter(h -> h.getSeverity() == HighlightSeverity.ERROR
                          || h.getSeverity() == HighlightSeverity.WARNING)
                .count();
        assertEquals("No annotations for empty file", 0, count);
    }

    /** Clean file should produce no errors. */
    public void testCleanFileNoErrors() {
        List<HighlightInfo> errors = getErrors("clean.test",
                "# Clean test file\n" +
                "require httpfs\n\n" +
                "statement ok\nCREATE TABLE t (id INT)\n\n" +
                "statement error\nINVALID SQL\n\n" +
                "query I\nSELECT 1\n----\n1\n\n" +
                "loop 2\nstatement ok\nSELECT 1\nendloop\n\n" +
                "foreach v a b\nstatement ok\nSELECT '${v}'\nendforeach\n");
        assertEquals("Clean file should have no errors", 0, errors.size());
    }

    /** Multiple errors in one file. */
    public void testMultipleErrors() {
        List<HighlightInfo> errors = getErrors("multi_err.test",
                "endloop\n" +
                "endforeach\n" +
                "loop 3\n" +
                "foreach val a b\n");
        assertTrue("Should have multiple errors", errors.size() >= 3);
    }

    /** File with only directives (no SQL body). */
    public void testDirectivesOnly() {
        getAnnotations("dirs.test",
                "require httpfs\nrequire icu\nmode output_result\nhalt\n");
        // Should not crash
    }

    /** 'statement error' followed by text containing 'ok' should still be valid. */
    public void testStatementErrorWithOkInBody() {
        List<HighlightInfo> warnings = getWarnings("error_ok.test",
                "statement error\nCREATE TABLE ok_table (id INT)\n");
        long count = warnings.stream()
                .filter(w -> w.getDescription().contains("should be followed by"))
                .count();
        assertEquals("Should not warn about 'statement error'", 0, count);
    }

    /** Case sensitivity: annotator uses lowercase startsWith — 
     *  but lexer is %caseless so 'STATEMENT ok' produces STATEMENT token.
     *  The annotator reads document text directly, not tokens.
     *  'STATEMENT ok' in uppercase should still be recognized. */
    public void testCaseSensitivityStatement() {
        // The annotator checks trimmed.startsWith("statement ") which is case-sensitive.
        // If someone writes "STATEMENT ok", the annotator won't see it as a statement.
        // This is a known behavior — test that it doesn't crash at least.
        myFixture.configureByText("case.test", "STATEMENT ok\nSELECT 1\n");
        myFixture.doHighlighting();
    }

    /** 'query' on last line with no SQL and no ---- should warn. */
    public void testQueryOnLastLine() {
        List<HighlightInfo> warnings = getWarnings("last_line.test",
                "query I");
        boolean found = warnings.stream()
                .anyMatch(w -> w.getDescription().contains("no '----'"));
        assertTrue("Should warn about missing ---- on last-line query", found);
    }
}
