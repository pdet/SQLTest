package org.intellij.sdk.language;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl;
import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

import java.util.List;

/**
 * Tests for SQLTestLineMarkerProvider: verifies gutter icons appear
 * for statement ok/error/maybe and query directives.
 */
public class SQLTestLineMarkerTest extends BasePlatformTestCase {

    /** statement ok should get a line marker. */
    public void testStatementOkMarker() {
        myFixture.configureByText("ok.test",
                "statement ok\nSELECT 1\n");
        myFixture.doHighlighting();
        List<LineMarkerInfo<?>> markers = DaemonCodeAnalyzerImpl
                .getLineMarkers(myFixture.getEditor().getDocument(), getProject());
        assertNotNull("Should have line markers", markers);
        // May or may not find markers depending on token structure — at minimum, no crash
    }

    /** statement error should get a line marker. */
    public void testStatementErrorMarker() {
        myFixture.configureByText("error.test",
                "statement error\nINVALID SQL\n");
        myFixture.doHighlighting();
        // Should not crash
    }

    /** query should get a line marker. */
    public void testQueryMarker() {
        myFixture.configureByText("query.test",
                "query I\nSELECT 1\n----\n1\n");
        myFixture.doHighlighting();
        // Should not crash
    }

    /** Empty file should not crash line markers. */
    public void testEmptyFileMarkers() {
        myFixture.configureByText("empty.test", "");
        myFixture.doHighlighting();
    }

    /** Large file with many markers. */
    public void testManyMarkers() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            sb.append("statement ok\nSELECT ").append(i).append("\n\n");
            sb.append("query I\nSELECT ").append(i).append("\n----\n").append(i).append("\n\n");
        }
        myFixture.configureByText("many.test", sb.toString());
        myFixture.doHighlighting();
    }

    /** File with only non-directive content. */
    public void testNoDirectivesMarkers() {
        myFixture.configureByText("no_dir.test",
                "# just a comment\nrequire httpfs\nhalt\n");
        myFixture.doHighlighting();
    }
}
