package org.intellij.sdk.language;

import junit.framework.TestCase;

/**
 * Unit tests for SQLTestSqlFormatter.
 */
public class SQLTestSqlFormatterTest extends TestCase {

    private static String fmt(String sql) {
        return SQLTestSqlFormatter.format(sql);
    }

    // ── Basic keyword uppercasing ────────────────────────────────────────────

    public void testSimpleSelect() {
        assertEquals("SELECT 1", fmt("select 1"));
    }

    public void testSelectFrom() {
        assertEquals("SELECT *\nFROM t", fmt("select * from t"));
    }

    public void testSelectFromWhere() {
        assertEquals("SELECT *\nFROM t\nWHERE id > 5",
                fmt("select * from t where id > 5"));
    }

    // ── Multi-line clause formatting ─────────────────────────────────────────

    public void testMultipleColumns() {
        assertEquals("SELECT a,\n    b,\n    c\nFROM t",
                fmt("select a, b, c from t"));
    }

    public void testWhereAndOr() {
        assertEquals("SELECT *\nFROM t\nWHERE x > 1\n    AND y < 2",
                fmt("select * from t where x > 1 and y < 2"));
    }

    public void testGroupByOrderBy() {
        assertEquals("SELECT a\nFROM t\nGROUP BY a\nORDER BY a",
                fmt("select a from t group by a order by a"));
    }

    public void testLimitOffset() {
        assertEquals("SELECT *\nFROM t\nLIMIT 10\nOFFSET 5",
                fmt("select * from t limit 10 offset 5"));
    }

    // ── JOIN ─────────────────────────────────────────────────────────────────

    public void testJoin() {
        assertEquals("SELECT *\nFROM t1\nJOIN t2 ON t1.id = t2.id",
                fmt("select * from t1 join t2 on t1.id = t2.id"));
    }

    public void testLeftJoin() {
        assertEquals("SELECT *\nFROM t1\nLEFT JOIN t2 ON t1.id = t2.id",
                fmt("select * from t1 left join t2 on t1.id = t2.id"));
    }

    // ── CREATE TABLE ─────────────────────────────────────────────────────────

    public void testCreateTable() {
        assertEquals("CREATE TABLE t (\n    id INTEGER,\n    name VARCHAR\n)",
                fmt("create table t (id integer, name varchar)"));
    }

    // ── INSERT ───────────────────────────────────────────────────────────────

    public void testInsertInto() {
        assertEquals("INSERT INTO t\nVALUES (1, 'hello')",
                fmt("insert into t values (1, 'hello')"));
    }

    // ── UPDATE ───────────────────────────────────────────────────────────────

    public void testUpdate() {
        assertEquals("UPDATE t\nSET x = 1,\n    y = 2\nWHERE id = 5",
                fmt("update t set x = 1, y = 2 where id = 5"));
    }

    // ── Subqueries ───────────────────────────────────────────────────────────

    public void testSubquery() {
        assertEquals("SELECT *\nFROM (\n    SELECT 1\n) sub",
                fmt("select * from (select 1) sub"));
    }

    // ── CTE ──────────────────────────────────────────────────────────────────

    public void testCte() {
        assertEquals("WITH cte AS (\n    SELECT 1\n)\nSELECT *\nFROM cte",
                fmt("with cte as (select 1) select * from cte"));
    }

    // ── UNION ────────────────────────────────────────────────────────────────

    public void testUnionAll() {
        assertEquals("SELECT 1\nUNION ALL\nSELECT 2",
                fmt("select 1 union all select 2"));
    }

    // ── JOIN variants ─────────────────────────────────────────────────────────

    public void testRightJoin() {
        String r = fmt("select * from t1 right join t2 on t1.id = t2.id");
        assertTrue(r.contains("\nRIGHT JOIN"));
    }

    public void testFullJoin() {
        String r = fmt("select * from t1 full join t2 on t1.id = t2.id");
        assertTrue(r.contains("\nFULL JOIN"));
    }

    public void testInnerJoin() {
        String r = fmt("select * from t1 inner join t2 on t1.id = t2.id");
        assertTrue(r.contains("\nINNER JOIN"));
    }

    public void testCrossJoin() {
        String r = fmt("select * from t1 cross join t2");
        assertTrue(r.contains("\nCROSS JOIN"));
    }

    public void testNaturalJoin() {
        String r = fmt("select * from t1 natural join t2");
        assertTrue(r.contains("\nNATURAL JOIN"));
    }

    public void testLeftOuterJoin() {
        String r = fmt("select * from t1 left outer join t2 on t1.id = t2.id");
        assertTrue(r.contains("\nLEFT OUTER JOIN"));
    }

    public void testMultipleJoins() {
        String r = fmt("select * from t1 join t2 on t1.id = t2.id left join t3 on t2.id = t3.id");
        assertTrue(r.contains("\nJOIN t2"));
        assertTrue(r.contains("\nLEFT JOIN t3"));
    }

    // ── CASE / WHEN / END ────────────────────────────────────────────────────

    public void testCaseWhenEnd() {
        String r = fmt("select case when x > 0 then 'pos' else 'neg' end from t");
        assertTrue("CASE present", r.contains("CASE"));
        assertTrue("WHEN present", r.contains("WHEN"));
        assertTrue("THEN present", r.contains("THEN"));
        assertTrue("ELSE present", r.contains("ELSE"));
        assertTrue("END present", r.contains("END"));
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    public void testDelete() {
        String r = fmt("delete from t where id = 5");
        assertTrue(r.startsWith("DELETE"));
        assertTrue(r.contains("\nWHERE"));
    }

    // ── EXCEPT / INTERSECT ───────────────────────────────────────────────────

    public void testExcept() {
        String r = fmt("select 1 except select 2");
        assertTrue(r.contains("\nEXCEPT"));
    }

    public void testIntersect() {
        String r = fmt("select 1 intersect select 2");
        assertTrue(r.contains("\nINTERSECT"));
    }

    public void testUnionWithoutAll() {
        String r = fmt("select 1 union select 2");
        assertTrue(r.contains("\nUNION\n"));
    }

    // ── INSERT ... SELECT ────────────────────────────────────────────────────

    public void testInsertSelect() {
        String r = fmt("insert into t select * from other");
        assertTrue(r.startsWith("INSERT INTO"));
        assertTrue(r.contains("\nSELECT"));
    }

    // ── Multiple CTEs ────────────────────────────────────────────────────────

    public void testMultipleCtes() {
        String r = fmt("with a as (select 1), b as (select 2) select * from a, b");
        assertTrue(r.startsWith("WITH"));
        assertTrue(r.contains("SELECT *"));
    }

    // ── Nested subqueries ────────────────────────────────────────────────────

    public void testNestedSubqueries() {
        String r = fmt("select * from (select * from (select 1) a) b");
        // Should have multiple levels of indentation
        assertTrue(r.contains("(\n"));
    }

    // ── WHERE with IN subquery ───────────────────────────────────────────────

    public void testWhereInSubquery() {
        String r = fmt("select * from t where id in (select id from other)");
        assertTrue(r.contains("\nWHERE"));
        assertTrue(r.contains("IN ("));
    }

    // ── CREATE TABLE AS SELECT ───────────────────────────────────────────────

    public void testCreateTableAsSelect() {
        String r = fmt("create table t as select * from other");
        assertTrue(r.startsWith("CREATE TABLE"));
        assertTrue(r.contains("SELECT"));
    }

    // ── HAVING ───────────────────────────────────────────────────────────────

    public void testHaving() {
        String r = fmt("select a, count(*) from t group by a having count(*) > 1");
        assertTrue(r.contains("\nHAVING"));
    }

    // ── Preservations ────────────────────────────────────────────────────────

    public void testPreservesStrings() {
        assertTrue(fmt("insert into t values ('hello world')").contains("'hello world'"));
    }

    public void testPreservesTemplateVars() {
        assertTrue(fmt("select * from ${my_table}").contains("${my_table}"));
    }

    public void testFunctionCalls() {
        String result = fmt("select count(*), sum(x) from t");
        assertTrue("count(*) preserved", result.contains("count(*)"));
        assertTrue("sum(x) preserved", result.contains("sum(x)"));
    }

    public void testDotNotation() {
        assertTrue(fmt("select t.id from t").contains("t.id"));
    }

    public void testCastOperator() {
        assertTrue(fmt("select x::integer from t").contains("x::INTEGER"));
    }

    public void testSemicolon() {
        assertTrue(fmt("select 1;").endsWith(";"));
    }

    // ── Idempotency ─────────────────────────────────────────────────────────

    public void testIdempotent() {
        String formatted = "SELECT *\nFROM t\nWHERE id > 5";
        assertEquals(formatted, fmt(formatted));
    }

    public void testNeedsFormattingFalse() {
        assertFalse(SQLTestSqlFormatter.needsFormatting("SELECT 1"));
    }

    public void testNeedsFormattingTrue() {
        assertTrue(SQLTestSqlFormatter.needsFormatting("select * from t"));
    }
}
