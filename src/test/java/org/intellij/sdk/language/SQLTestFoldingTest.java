package org.intellij.sdk.language;

import com.intellij.lang.folding.FoldingDescriptor;
import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

/**
 * Tests for SQLTestFoldingBuilder: verifies fold regions are created
 * for statement blocks, query blocks, loops, and foreach blocks.
 */
public class SQLTestFoldingTest extends BasePlatformTestCase {

    private FoldingDescriptor[] getFolds(String filename, String content) {
        PsiFile file = myFixture.configureByText(filename, content);
        Document doc = myFixture.getEditor().getDocument();
        SQLTestFoldingBuilder builder = new SQLTestFoldingBuilder();
        return builder.buildFoldRegions(file, doc, false);
    }

    /** Statement block should be foldable. */
    public void testStatementFold() {
        FoldingDescriptor[] folds = getFolds("stmt.test",
                "statement ok\nCREATE TABLE t (id INT)\n\nstatement ok\nSELECT 1\n");
        assertTrue("Should have fold regions for statements", folds.length >= 2);
    }

    /** Query block (with results) should be foldable. */
    public void testQueryFold() {
        FoldingDescriptor[] folds = getFolds("query.test",
                "query I\nSELECT 1\n----\n1\n\nstatement ok\nSELECT 2\n");
        assertTrue("Should have fold regions", folds.length >= 1);
    }

    /** Loop/endloop should be foldable. */
    public void testLoopFold() {
        FoldingDescriptor[] folds = getFolds("loop.test",
                "loop 3\nstatement ok\nSELECT 1\nendloop\n");
        boolean foundLoop = false;
        for (FoldingDescriptor fd : folds) {
            if (fd.getPlaceholderText() != null && fd.getPlaceholderText().contains("endloop")) {
                foundLoop = true;
            }
        }
        assertTrue("Should have loop fold with endloop placeholder", foundLoop);
    }

    /** Foreach/endforeach should be foldable. */
    public void testForeachFold() {
        FoldingDescriptor[] folds = getFolds("foreach.test",
                "foreach val a b c\nstatement ok\nSELECT '${val}'\nendforeach\n");
        boolean foundForeach = false;
        for (FoldingDescriptor fd : folds) {
            if (fd.getPlaceholderText() != null && fd.getPlaceholderText().contains("endforeach")) {
                foundForeach = true;
            }
        }
        assertTrue("Should have foreach fold with endforeach placeholder", foundForeach);
    }

    /** Empty file should produce no folds. */
    public void testEmptyFileFolds() {
        FoldingDescriptor[] folds = getFolds("empty.test", "");
        assertEquals("Empty file should have no folds", 0, folds.length);
    }

    /** File with only comments should produce no folds. */
    public void testCommentOnlyFolds() {
        FoldingDescriptor[] folds = getFolds("comments.test",
                "# comment 1\n# comment 2\n");
        assertEquals("Comments-only file should have no folds", 0, folds.length);
    }

    /** Nested loops should produce folds for both levels. */
    public void testNestedLoopFolds() {
        FoldingDescriptor[] folds = getFolds("nested.test",
                "loop 3\nloop 2\nstatement ok\nSELECT 1\nendloop\nendloop\n");
        int loopFolds = 0;
        for (FoldingDescriptor fd : folds) {
            if (fd.getPlaceholderText() != null && fd.getPlaceholderText().contains("endloop")) {
                loopFolds++;
            }
        }
        assertEquals("Should have 2 loop folds for nested", 2, loopFolds);
    }

    /** Single-line statement should not crash (fold region needs start < end). */
    public void testSingleLineStatement() {
        getFolds("single.test", "statement ok\n");
        // No crash = pass. May or may not create fold depending on block end logic.
    }

    /** Folding with very long file. */
    public void testLargeFileFolds() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            sb.append("statement ok\nSELECT ").append(i).append("\n\n");
        }
        FoldingDescriptor[] folds = getFolds("large.test", sb.toString());
        assertTrue("Large file should have many folds", folds.length >= 50);
    }

    /** Statement at EOF (no trailing newline). */
    public void testStatementAtEOF() {
        getFolds("eof.test", "statement ok\nSELECT 1");
        // Should not crash
    }

    /** Query at EOF with no result and no newline. */
    public void testQueryAtEOF() {
        getFolds("query_eof.test", "query I\nSELECT 1");
        // Should not crash
    }

    /** Loop without endloop should not crash folding. */
    public void testUnclosedLoopFold() {
        getFolds("unclosed.test", "loop 3\nstatement ok\nSELECT 1\n");
        // Should not crash — fold extends to EOF
    }

    /** concurrentloop fold. */
    public void testConcurrentloopFold() {
        getFolds("cloop.test",
                "concurrentloop 2\nstatement ok\nSELECT 1\nendloop\n");
        // concurrentloop is NOT handled by folding builder (only "loop ") 
        // This test verifies it doesn't crash.
    }
}
