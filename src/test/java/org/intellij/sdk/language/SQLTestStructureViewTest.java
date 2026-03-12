package org.intellij.sdk.language;

import com.intellij.ide.util.treeView.smartTree.TreeElement;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

/**
 * Tests for SQLTestStructureViewElement: verifies the structure tree
 * is built correctly from document text.
 */
public class SQLTestStructureViewTest extends BasePlatformTestCase {

    private TreeElement[] getStructure(String filename, String content) {
        PsiFile file = myFixture.configureByText(filename, content);
        SQLTestStructureViewElement root = new SQLTestStructureViewElement(file);
        return root.getChildren();
    }

    /** File with statement, query, loop, foreach. */
    public void testBasicStructure() {
        TreeElement[] children = getStructure("basic.test",
                "statement ok\nCREATE TABLE t (id INT)\n\n" +
                "query I\nSELECT 1\n----\n1\n\n" +
                "loop 3\nstatement ok\nSELECT 1\nendloop\n\n" +
                "foreach val a b\nstatement ok\nSELECT '${val}'\nendforeach\n");
        // statement + query + loop + statement + foreach + statement = 6
        assertTrue("Should have multiple structure elements", children.length >= 4);
    }

    /** Empty file. */
    public void testEmptyFileStructure() {
        TreeElement[] children = getStructure("empty.test", "");
        assertEquals("Empty file should have no structure", 0, children.length);
    }

    /** File with only comments. */
    public void testCommentsOnlyStructure() {
        TreeElement[] children = getStructure("comments.test",
                "# comment 1\n# comment 2\n");
        assertEquals("Comments should not appear in structure", 0, children.length);
    }

    /** All directive types should appear. */
    public void testAllDirectivesInStructure() {
        TreeElement[] children = getStructure("all.test",
                "require httpfs\n" +
                "require-env S3_BUCKET\n" +
                "mode output_result\n" +
                "skipif duckdb\n" +
                "onlyif duckdb\n" +
                "load __TEST_DIR__/test.db\n" +
                "halt\n" +
                "restart\n" +
                "statement ok\nSELECT 1\n\n" +
                "query I\nSELECT 2\n----\n2\n\n" +
                "loop 2\nstatement ok\nSELECT 1\nendloop\n\n" +
                "foreach v a b\nstatement ok\nSELECT '${v}'\nendforeach\n\n" +
                "concurrentloop 2\nstatement ok\nSELECT 1\nendloop\n\n" +
                "concurrentforeach v x y\nstatement ok\nSELECT '${v}'\nendforeach\n");
        // require(2) + require-env + mode + skipif + onlyif + load + halt + restart
        // + statement(6) + query + loop + foreach + concurrentloop + concurrentforeach = 20
        assertTrue("Should find many structure elements", children.length >= 10);
    }

    /** Statement with SQL preview in structure. */
    public void testStatementSQLPreview() {
        TreeElement[] children = getStructure("preview.test",
                "statement ok\nCREATE TABLE employees (id INT, name VARCHAR)\n");
        assertTrue("Should have at least one element", children.length >= 1);
        String label = children[0].getPresentation().getPresentableText();
        assertNotNull("Label should not be null", label);
        assertTrue("Label should contain SQL preview",
                label.contains("CREATE TABLE"));
    }

    /** Structure with very long SQL (should be truncated). */
    public void testLongSQLTruncated() {
        StringBuilder sql = new StringBuilder("CREATE TABLE t (");
        for (int i = 0; i < 50; i++) sql.append("col").append(i).append(" INT, ");
        sql.append("last INT)");
        TreeElement[] children = getStructure("long_sql.test",
                "statement ok\n" + sql + "\n");
        assertTrue(children.length >= 1);
        String label = children[0].getPresentation().getPresentableText();
        assertTrue("Long SQL should be truncated", label.length() < sql.length());
    }

    /** Statement on last line of file (no SQL body after). */
    public void testStatementLastLine() {
        TreeElement[] children = getStructure("last.test", "statement ok");
        assertTrue("Should still create structure element", children.length >= 1);
    }

    /** Large file structure performance. */
    public void testLargeFileStructure() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 200; i++) {
            sb.append("statement ok\nSELECT ").append(i).append("\n\n");
        }
        TreeElement[] children = getStructure("large.test", sb.toString());
        assertEquals("Should have 200 statement elements", 200, children.length);
    }
}
