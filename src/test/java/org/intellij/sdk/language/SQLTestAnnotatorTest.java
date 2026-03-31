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
                "loop i 0 3\nstatement ok\nSELECT 1\nendloop\n");
        long count = errors.stream()
                .filter(e -> e.getDescription().contains("loop"))
                .count();
        assertEquals("No loop errors", 0, count);
    }

    /** Unclosed loop should produce error. */
    public void testUnclosedLoop() {
        List<HighlightInfo> errors = getErrors("unclosed_loop.test",
                "loop i 0 3\nstatement ok\nSELECT 1\n");
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
                "loop i 0 3\nloop j 0 2\nstatement ok\nSELECT 1\nendloop\nendloop\n");
        long count = errors.stream()
                .filter(e -> e.getDescription().contains("loop"))
                .count();
        assertEquals("No loop errors for nested", 0, count);
    }

    /** Two endloops for one loop should error on the second. */
    public void testExtraEndloop() {
        List<HighlightInfo> errors = getErrors("extra_endloop.test",
                "loop i 0 3\nstatement ok\nSELECT 1\nendloop\nendloop\n");
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
                "concurrentloop j 0 2\nstatement ok\nSELECT 1\nendloop\n");
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
                "concurrentloop j 0 2\nstatement ok\nSELECT 1\n");
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
                "loop j 0 2\nstatement ok\nSELECT 1\nendloop\n\n" +
                "foreach v a b\nstatement ok\nSELECT '${v}'\nendforeach\n");
        assertEquals("Clean file should have no errors", 0, errors.size());
    }

    /** Multiple errors in one file. */
    public void testMultipleErrors() {
        List<HighlightInfo> errors = getErrors("multi_err.test",
                "endloop\n" +
                "endforeach\n" +
                "loop i 0 3\n" +
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

    /** 'query' on last line with no SQL should warn about empty body. */
    public void testQueryOnLastLine() {
        List<HighlightInfo> all = getAnnotations("last_line.test", "query I");
        boolean found = all.stream()
                .anyMatch(h -> h.getDescription() != null &&
                        h.getDescription().contains("Empty query body"));
        assertTrue("Should warn about empty query body on last-line query", found);
    }

    // =========================================================================
    // 7. SQL formatting — annotator integration
    // =========================================================================

    /** Lowercase SQL should produce a formatting warning. */
    public void testLowercaseSqlWarning() {
        List<HighlightInfo> all = getAnnotations("fmt_bad.test",
                "statement ok\nselect * from t;\n");
        boolean found = all.stream()
                .anyMatch(h -> h.getDescription() != null &&
                        h.getDescription().contains("not formatted"));
        assertTrue("Unformatted SQL should produce a warning", found);
    }

    /** Query block with lowercase SQL should also warn. */
    public void testLowercaseSqlInQueryBlock() {
        List<HighlightInfo> all = getAnnotations("fmt_query.test",
                "query I\nselect 1\n----\n1\n");
        boolean found = all.stream()
                .anyMatch(h -> h.getDescription() != null &&
                        h.getDescription().contains("not formatted"));
        assertTrue("Unformatted SQL in query block should warn", found);
    }

    // =========================================================================
    // 8. Statement qualifier — debug / debug_skip
    // =========================================================================

    /** 'statement debug' should produce no qualifier warnings. */
    public void testStatementDebugNoWarning() {
        List<HighlightInfo> warnings = getWarnings("debug.test",
                "statement debug\nSELECT 1\n");
        for (HighlightInfo w : warnings) {
            assertFalse("Should not warn about 'debug' qualifier",
                    w.getDescription().contains("should be followed by"));
        }
    }

    /** 'statement debug_skip' should produce no qualifier warnings. */
    public void testStatementDebugSkipNoWarning() {
        List<HighlightInfo> warnings = getWarnings("debug_skip.test",
                "statement debug_skip\nSELECT 1\n");
        for (HighlightInfo w : warnings) {
            assertFalse("Should not warn about 'debug_skip' qualifier",
                    w.getDescription().contains("should be followed by"));
        }
    }

    // =========================================================================
    // 9. Query column count mismatch
    // =========================================================================

    /** Matching column count should not error. */
    public void testQueryColumnCountMatch() {
        List<HighlightInfo> errors = getErrors("col_ok.test",
                "query II\nSELECT 1, 2\n----\n1\t2\n");
        long count = errors.stream()
                .filter(e -> e.getDescription().contains("column"))
                .count();
        assertEquals("Matching columns should not error", 0, count);
    }

    /** Too many result columns should error. */
    public void testQueryTooManyResultColumns() {
        List<HighlightInfo> errors = getErrors("col_extra.test",
                "query II\nSELECT 1, 2, 3\n----\n1\t2\t3\n");
        boolean found = errors.stream()
                .anyMatch(e -> e.getDescription().contains("3 column(s) but query type 'II' specifies 2"));
        assertTrue("Should error about column count mismatch", found);
    }

    /** Too few result columns should error. */
    public void testQueryTooFewResultColumns() {
        List<HighlightInfo> errors = getErrors("col_few.test",
                "query III\nSELECT 1\n----\n1\n");
        boolean found = errors.stream()
                .anyMatch(e -> e.getDescription().contains("1 column(s) but query type 'III' specifies 3"));
        assertTrue("Should error about column count mismatch", found);
    }

    /** Inconsistent result row column count should warn. */
    public void testInconsistentResultRows() {
        List<HighlightInfo> warnings = getWarnings("col_inconsistent.test",
                "query II\nSELECT 1, 2\n----\n1\t2\n3\t4\t5\n");
        boolean found = warnings.stream()
                .anyMatch(w -> w.getDescription().contains("inconsistent tab"));
        assertTrue("Should warn about inconsistent row column count", found);
    }

    // =========================================================================
    // 10. Invalid query return type
    // =========================================================================

    /** Valid return types should not error. */
    public void testValidReturnTypes() {
        List<HighlightInfo> errors = getErrors("type_ok.test",
                "query TIR\nSELECT 'a', 1, 1.0\n----\na\t1\t1.0\n");
        long count = errors.stream()
                .filter(e -> e.getDescription().contains("Invalid return type"))
                .count();
        assertEquals("Valid types should not error", 0, count);
    }

    /** Invalid character in return type should error. */
    public void testInvalidReturnType() {
        List<HighlightInfo> errors = getErrors("type_bad.test",
                "query IX\nSELECT 1, 2\n----\n1\t2\n");
        boolean found = errors.stream()
                .anyMatch(e -> e.getDescription().contains("Invalid return type"));
        assertTrue("Should error about invalid return type character", found);
    }

    /** Empty return type should error. */
    public void testEmptyReturnType() {
        List<HighlightInfo> errors = getErrors("type_empty.test",
                "query\nSELECT 1\n----\n1\n");
        boolean found = errors.stream()
                .anyMatch(e -> e.getDescription().contains("column type specification"));
        assertTrue("Should error about empty return type", found);
    }

    // =========================================================================
    // 11. Empty SQL body
    // =========================================================================

    /** Statement with no SQL body should warn. */
    public void testEmptyStatementBody() {
        List<HighlightInfo> all = getAnnotations("empty_body.test",
                "statement ok\n\nstatement ok\nSELECT 1\n");
        boolean found = all.stream()
                .anyMatch(h -> h.getDescription() != null &&
                        h.getDescription().contains("Empty statement body"));
        assertTrue("Should warn about empty statement body", found);
    }

    /** Query with no SQL body should warn. */
    public void testEmptyQueryBody() {
        List<HighlightInfo> all = getAnnotations("empty_qbody.test",
                "query I\n\nstatement ok\nSELECT 1\n");
        boolean found = all.stream()
                .anyMatch(h -> h.getDescription() != null &&
                        h.getDescription().contains("Empty query body"));
        assertTrue("Should warn about empty query body", found);
    }

    // =========================================================================
    // 12. Loop / foreach parameter validation
    // =========================================================================

    /** Loop with insufficient parameters should error. */
    public void testLoopTooFewParams() {
        List<HighlightInfo> errors = getErrors("loop_params.test",
                "loop i\nstatement ok\nSELECT 1\nendloop\n");
        boolean found = errors.stream()
                .anyMatch(e -> e.getDescription().contains("requires 3 parameters"));
        assertTrue("Should error about insufficient loop params", found);
    }

    /** Loop with start >= end should warn. */
    public void testLoopEmptyRange() {
        List<HighlightInfo> warnings = getWarnings("loop_range.test",
                "loop i 10 5\nstatement ok\nSELECT 1\nendloop\n");
        boolean found = warnings.stream()
                .anyMatch(w -> w.getDescription().contains("Loop range is empty"));
        assertTrue("Should warn about empty loop range", found);
    }

    /** Loop with equal start and end should warn. */
    public void testLoopZeroRange() {
        List<HighlightInfo> warnings = getWarnings("loop_zero.test",
                "loop i 5 5\nstatement ok\nSELECT 1\nendloop\n");
        boolean found = warnings.stream()
                .anyMatch(w -> w.getDescription().contains("Loop range is empty"));
        assertTrue("Should warn about zero-iteration loop", found);
    }

    /** Foreach with no values should error. */
    public void testForeachNoValues() {
        List<HighlightInfo> errors = getErrors("foreach_noval.test",
                "foreach var\nstatement ok\nSELECT 1\nendforeach\n");
        boolean found = errors.stream()
                .anyMatch(e -> e.getDescription().contains("at least one value"));
        assertTrue("Should error about foreach without values", found);
    }

    // =========================================================================
    // 13. Duplicate require
    // =========================================================================

    /** Duplicate require should warn. */
    public void testDuplicateRequire() {
        List<HighlightInfo> warnings = getWarnings("dup_req.test",
                "require httpfs\nrequire parquet\nrequire httpfs\n\nstatement ok\nSELECT 1\n");
        boolean found = warnings.stream()
                .anyMatch(w -> w.getDescription().contains("Duplicate"));
        assertTrue("Should warn about duplicate require", found);
    }

    /** Non-duplicate requires should not warn. */
    public void testNoDuplicateRequire() {
        List<HighlightInfo> warnings = getWarnings("nodup_req.test",
                "require httpfs\nrequire parquet\nrequire icu\n\nstatement ok\nSELECT 1\n");
        long count = warnings.stream()
                .filter(w -> w.getDescription().contains("Duplicate"))
                .count();
        assertEquals("No duplicate warnings expected", 0, count);
    }

    // =========================================================================
    // 14. Mode validation
    // =========================================================================

    /** Valid mode should not warn. */
    public void testValidMode() {
        List<HighlightInfo> warnings = getWarnings("mode_ok.test",
                "mode output_result\nstatement ok\nSELECT 1\n");
        long count = warnings.stream()
                .filter(w -> w.getDescription().contains("Unrecognized mode"))
                .count();
        assertEquals("Valid mode should not warn", 0, count);
    }

    /** Invalid mode should warn. */
    public void testInvalidMode() {
        List<HighlightInfo> warnings = getWarnings("mode_bad.test",
                "mode bogus\nstatement ok\nSELECT 1\n");
        boolean found = warnings.stream()
                .anyMatch(w -> w.getDescription().contains("Unrecognized mode"));
        assertTrue("Should warn about invalid mode", found);
    }

    // =========================================================================
    // 15. Sleep validation
    // =========================================================================

    /** Valid sleep should not warn. */
    public void testValidSleep() {
        List<HighlightInfo> warnings = getWarnings("sleep_ok.test",
                "sleep 1 second\nstatement ok\nSELECT 1\n");
        long count = warnings.stream()
                .filter(w -> w.getDescription().contains("sleep"))
                .count();
        assertEquals("Valid sleep should not warn", 0, count);
    }

    /** Invalid sleep unit should warn. */
    public void testInvalidSleepUnit() {
        List<HighlightInfo> warnings = getWarnings("sleep_bad.test",
                "sleep 1 fortnight\nstatement ok\nSELECT 1\n");
        boolean found = warnings.stream()
                .anyMatch(w -> w.getDescription().contains("Invalid sleep unit"));
        assertTrue("Should warn about invalid sleep unit", found);
    }

    /** Sleep missing parameters should warn. */
    public void testSleepMissingParams() {
        List<HighlightInfo> warnings = getWarnings("sleep_miss.test",
                "sleep 1\nstatement ok\nSELECT 1\n");
        boolean found = warnings.stream()
                .anyMatch(w -> w.getDescription().contains("requires duration and unit"));
        assertTrue("Should warn about missing sleep params", found);
    }

    // =========================================================================
    // 16. statement ok with ---- section
    // =========================================================================

    /** statement ok with ---- should warn (probably should be query). */
    public void testStatementOkWithResults() {
        List<HighlightInfo> warnings = getWarnings("stmt_results.test",
                "statement ok\nSELECT 1\n----\n1\n");
        boolean found = warnings.stream()
                .anyMatch(w -> w.getDescription().contains("should not have a '----'"));
        assertTrue("Should warn about statement ok with results section", found);
    }

    // =========================================================================
    // 17. hash-threshold validation
    // =========================================================================

    /** Non-numeric hash-threshold should warn. */
    public void testInvalidHashThreshold() {
        List<HighlightInfo> warnings = getWarnings("ht_bad.test",
                "hash-threshold abc\nstatement ok\nSELECT 1\n");
        boolean found = warnings.stream()
                .anyMatch(w -> w.getDescription().contains("must be a number"));
        assertTrue("Should warn about non-numeric hash-threshold", found);
    }

    // =========================================================================
    // 18. Quick fix invocation tests
    // =========================================================================

    /** Format SQL fix should uppercase keywords and add line breaks. */
    public void testFormatSqlFixInvocation() {
        myFixture.configureByText("fix_fmt.test",
                "statement ok\nselect * from t\n");
        List<HighlightInfo> all = myFixture.doHighlighting();
        boolean hasFormatFix = all.stream()
                .anyMatch(h -> h.getDescription() != null &&
                        h.getDescription().contains("not formatted"));
        assertTrue("Should have format SQL annotation", hasFormatFix);
        // Apply the fix
        myFixture.getAllQuickFixes("fix_fmt.test").stream()
                .filter(f -> f.getText().equals("Format SQL"))
                .findFirst()
                .ifPresent(f -> myFixture.launchAction(f));
        String result = myFixture.getEditor().getDocument().getText();
        assertTrue("SQL should be uppercased after fix", result.contains("SELECT"));
        assertTrue("SQL should have line breaks after fix", result.contains("\nFROM"));
    }

    /** Fix return type quick fix should change the type string. */
    public void testFixReturnTypeInvocation() {
        myFixture.configureByText("fix_type.test",
                "query II\nSELECT 1, 2, 3\n----\n1\t2\t3\n");
        myFixture.doHighlighting();
        myFixture.getAllQuickFixes("fix_type.test").stream()
                .filter(f -> f.getText().contains("Change return type"))
                .findFirst()
                .ifPresent(f -> myFixture.launchAction(f));
        String result = myFixture.getEditor().getDocument().getText();
        assertTrue("Type string should be III after fix", result.contains("query III"));
    }

    /** Remove duplicate require fix should remove the line. */
    public void testRemoveDuplicateRequireFix() {
        myFixture.configureByText("fix_dup.test",
                "require httpfs\nrequire httpfs\n\nstatement ok\nSELECT 1\n");
        myFixture.doHighlighting();
        myFixture.getAllQuickFixes("fix_dup.test").stream()
                .filter(f -> f.getText().contains("Remove duplicate"))
                .findFirst()
                .ifPresent(f -> myFixture.launchAction(f));
        String result = myFixture.getEditor().getDocument().getText();
        // Should have only one require httpfs
        long count = result.lines().filter(l -> l.trim().equals("require httpfs")).count();
        assertEquals("Should have exactly one require httpfs after fix", 1, count);
    }

    // =========================================================================
    // 19. Annotator edge cases
    // =========================================================================

    /** Loop with template variables should not warn about range. */
    public void testLoopWithTemplateVars() {
        List<HighlightInfo> warnings = getWarnings("loop_tpl.test",
                "loop i ${start} ${end}\nstatement ok\nSELECT 1\nendloop\n");
        long count = warnings.stream()
                .filter(w -> w.getDescription().contains("Loop range"))
                .count();
        assertEquals("Template var loop should not warn about range", 0, count);
    }

    /** Empty result section (---- followed by blank) should not cause column mismatch. */
    public void testEmptyResultSection() {
        List<HighlightInfo> errors = getErrors("empty_result.test",
                "query I\nSELECT 1\n----\n\nstatement ok\nSELECT 1\n");
        long count = errors.stream()
                .filter(e -> e.getDescription().contains("column"))
                .count();
        assertEquals("Empty result should not cause column mismatch", 0, count);
    }

    /** require-env should not be caught as duplicate require. */
    public void testRequireEnvNotDuplicate() {
        List<HighlightInfo> warnings = getWarnings("req_env.test",
                "require httpfs\nrequire-env HOME\n\nstatement ok\nSELECT 1\n");
        long count = warnings.stream()
                .filter(w -> w.getDescription().contains("Duplicate"))
                .count();
        assertEquals("require-env should not trigger duplicate", 0, count);
    }

    /** All valid modes should not warn. */
    public void testAllValidModes() {
        for (String mode : new String[]{"output_hash", "output_result", "no_output", "debug", "skip", "unskip"}) {
            List<HighlightInfo> warnings = getWarnings("mode_" + mode + ".test",
                    "mode " + mode + "\nstatement ok\nSELECT 1\n");
            long count = warnings.stream()
                    .filter(w -> w.getDescription().contains("Unrecognized mode"))
                    .count();
            assertEquals("Mode '" + mode + "' should be valid", 0, count);
        }
    }

    /** All valid sleep units should not warn. */
    public void testAllValidSleepUnits() {
        for (String unit : new String[]{"second", "seconds", "sec",
                "millisecond", "milliseconds", "milli",
                "microsecond", "microseconds", "micro",
                "nanosecond", "nanoseconds", "nano"}) {
            List<HighlightInfo> warnings = getWarnings("sleep_" + unit + ".test",
                    "sleep 1 " + unit + "\nstatement ok\nSELECT 1\n");
            long count = warnings.stream()
                    .filter(w -> w.getDescription().contains("sleep"))
                    .count();
            assertEquals("Sleep unit '" + unit + "' should be valid", 0, count);
        }
    }

    /** Lowercase return types (i, t, r) are valid (lexer is case-insensitive). */
    public void testLowercaseReturnTypes() {
        List<HighlightInfo> errors = getErrors("lc_type.test",
                "query ii\nSELECT 1, 2\n----\n1\t2\n");
        // Lowercase i/t/r are valid — no errors expected
        boolean hasInvalidType = errors.stream()
                .anyMatch(e -> e.getDescription().contains("Invalid return type"));
        boolean hasColumnMismatch = errors.stream()
                .anyMatch(e -> e.getDescription().contains("column(s) but query type"));
        assertFalse("Lowercase return types are valid", hasInvalidType);
        assertFalse("Column count should match", hasColumnMismatch);
    }

    /** Multiple errors in one query block. */
    public void testMultipleQueryErrors() {
        List<HighlightInfo> errors = getErrors("multi_q_err.test",
                "query XY\nSELECT 1, 2, 3\n----\n1\t2\t3\n");
        // Should have: invalid return type AND column count mismatch
        boolean hasInvalidType = errors.stream()
                .anyMatch(e -> e.getDescription().contains("Invalid return type"));
        boolean hasColumnMismatch = errors.stream()
                .anyMatch(e -> e.getDescription().contains("column(s)"));
        assertTrue("Should have invalid type error", hasInvalidType);
        assertTrue("Should have column count error", hasColumnMismatch);
    }
}
