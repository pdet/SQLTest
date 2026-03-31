package org.intellij.sdk.language;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

/**
 * Annotates .test files with warnings/errors for common mistakes.
 */
public class SQLTestAnnotator implements Annotator {

    private static final Set<String> VALID_MODES = Set.of(
            "output_hash", "output_result", "no_output", "debug", "skip", "unskip");

    private static final Set<String> VALID_SLEEP_UNITS = Set.of(
            "second", "seconds", "sec",
            "millisecond", "milliseconds", "milli",
            "microsecond", "microseconds", "micro",
            "nanosecond", "nanoseconds", "nano");

    @Override
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        if (!(element instanceof PsiFile)) return;

        Document doc = PsiDocumentManager.getInstance(element.getProject()).getDocument((PsiFile) element);
        if (doc == null) return;

        String text = doc.getText();
        String[] lines = text.split("\n", -1);

        int loopDepth = 0;
        int foreachDepth = 0;
        int loopLine = -1;
        int foreachLine = -1;
        Set<String> seenRequires = new HashSet<>();

        for (int i = 0; i < lines.length; i++) {
            String trimmed = lines[i].trim();
            int lineStart = getLineStartOffset(lines, i);
            int lineEnd = lineStart + lines[i].length();

            // ─── statement checks ────────────────────────────────────
            if (trimmed.startsWith("statement ")) {
                checkStatementBlock(lines, i, trimmed, lineStart, lineEnd, holder);
                checkSqlFormatting(lines, i + 1, holder);
            }

            // ─── query checks ────────────────────────────────────────
            if (trimmed.startsWith("query ") || trimmed.equals("query")) {
                checkQueryBlock(lines, i, trimmed, lineStart, lineEnd, holder);
                if (trimmed.startsWith("query ")) {
                    checkSqlFormatting(lines, i + 1, holder);
                }
            }

            // ─── loop / endloop ──────────────────────────────────────
            if (trimmed.startsWith("loop ") || trimmed.startsWith("concurrentloop ")) {
                if (loopDepth == 0) loopLine = i;
                loopDepth++;
                checkLoopParams(trimmed, lineStart, lineEnd, holder);
            }
            if (trimmed.equals("endloop")) {
                loopDepth--;
                if (loopDepth < 0) {
                    annotate(holder, HighlightSeverity.ERROR,
                            "'endloop' without matching 'loop'", lineStart, lineEnd);
                    loopDepth = 0;
                }
            }

            // ─── foreach / endforeach ────────────────────────────────
            if (trimmed.startsWith("foreach ") || trimmed.startsWith("concurrentforeach ")) {
                if (foreachDepth == 0) foreachLine = i;
                foreachDepth++;
                checkForeachParams(trimmed, lineStart, lineEnd, holder);
            }
            if (trimmed.equals("endforeach")) {
                foreachDepth--;
                if (foreachDepth < 0) {
                    annotate(holder, HighlightSeverity.ERROR,
                            "'endforeach' without matching 'foreach'", lineStart, lineEnd);
                    foreachDepth = 0;
                }
            }

            // ─── duplicate require ───────────────────────────────────
            // Note: "require-env" and "require_reinit" don't match "require " (with space)
            if (trimmed.startsWith("require ")) {
                String arg = trimmed.substring("require ".length()).trim();
                if (!arg.isEmpty()) {
                    if (seenRequires.contains(arg)) {
                        // Include \n in the removal range if there is a following line
                        int removeEnd = (i + 1 < lines.length)
                                ? getLineStartOffset(lines, i + 1)
                                : lineEnd;
                        holder.newAnnotation(HighlightSeverity.WARNING,
                                        "Duplicate 'require " + arg + "'")
                                .range(new TextRange(lineStart, lineEnd))
                                .withFix(new SQLTestQuickFix(
                                        "Remove duplicate require",
                                        lineStart, removeEnd, ""))
                                .create();
                    } else {
                        seenRequires.add(arg);
                    }
                }
            }

            // ─── mode validation ─────────────────────────────────────
            if (trimmed.startsWith("mode ")) {
                String mode = trimmed.substring("mode ".length()).trim();
                if (!VALID_MODES.contains(mode)) {
                    annotate(holder, HighlightSeverity.WARNING,
                            "Unrecognized mode '" + mode +
                                    "' — expected output_hash, output_result, no_output, debug, skip, or unskip",
                            lineStart, lineEnd);
                }
            }

            // ─── sleep unit validation ───────────────────────────────
            if (trimmed.startsWith("sleep ")) {
                String[] parts = trimmed.split("\\s+");
                if (parts.length < 3) {
                    annotate(holder, HighlightSeverity.WARNING,
                            "'sleep' requires duration and unit (e.g., sleep 1 second)", lineStart, lineEnd);
                } else if (!VALID_SLEEP_UNITS.contains(parts[2])) {
                    annotate(holder, HighlightSeverity.WARNING,
                            "Invalid sleep unit '" + parts[2] +
                                    "' — expected second(s), millisecond(s), microsecond(s), or nanosecond(s)",
                            lineStart, lineEnd);
                }
            }

            // ─── hash-threshold validation ───────────────────────────
            if (trimmed.startsWith("hash-threshold ")) {
                String param = trimmed.substring("hash-threshold ".length()).trim();
                if (param.isEmpty()) {
                    annotate(holder, HighlightSeverity.WARNING,
                            "'hash-threshold' requires a numeric parameter", lineStart, lineEnd);
                } else {
                    try {
                        Integer.parseInt(param);
                    } catch (NumberFormatException e) {
                        annotate(holder, HighlightSeverity.WARNING,
                                "'hash-threshold' parameter must be a number", lineStart, lineEnd);
                    }
                }
            }
        }

        // Unclosed loops/foreach
        if (loopDepth > 0 && loopLine >= 0) {
            int ls = getLineStartOffset(lines, loopLine);
            annotate(holder, HighlightSeverity.ERROR,
                    "Unclosed 'loop' — missing 'endloop'", ls, ls + lines[loopLine].length());
        }
        if (foreachDepth > 0 && foreachLine >= 0) {
            int ls = getLineStartOffset(lines, foreachLine);
            annotate(holder, HighlightSeverity.ERROR,
                    "Unclosed 'foreach' — missing 'endforeach'", ls, ls + lines[foreachLine].length());
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Statement block checks
    // ═════════════════════════════════════════════════════════════════════════

    private void checkStatementBlock(String[] lines, int i, String trimmed,
                                     int lineStart, int lineEnd, AnnotationHolder holder) {
        String rest = trimmed.substring("statement ".length()).trim();
        String qualifier = rest.toLowerCase().split("\\s+")[0];

        // 1. Invalid qualifier
        if (!qualifier.equals("ok") && !qualifier.equals("error") && !qualifier.equals("maybe")
                && !qualifier.equals("debug") && !qualifier.equals("debug_skip")) {
            annotate(holder, HighlightSeverity.WARNING,
                    "Statement should be followed by 'ok', 'error', 'maybe', 'debug', or 'debug_skip'",
                    lineStart, lineEnd);
        }

        // 2. Empty SQL body
        int sqlLine = findFirstSqlLine(lines, i + 1);
        if (sqlLine < 0) {
            annotate(holder, HighlightSeverity.WARNING,
                    "Empty statement body — no SQL after directive", lineStart, lineEnd);
            return;
        }

        // 3. statement ok with ---- section (probably should be query)
        if (qualifier.equals("ok")) {
            for (int j = sqlLine; j < lines.length; j++) {
                String t = lines[j].trim();
                if (t.isEmpty() || isDirective(t)) break;
                if (t.equals("----")) {
                    annotate(holder, HighlightSeverity.WARNING,
                            "'statement ok' should not have a '----' result section — use 'query' instead",
                            lineStart, lineEnd);
                    break;
                }
            }
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Query block checks
    // ═════════════════════════════════════════════════════════════════════════

    private void checkQueryBlock(String[] lines, int i, String trimmed,
                                 int lineStart, int lineEnd, AnnotationHolder holder) {
        String afterQuery = trimmed.length() > "query ".length()
                ? trimmed.substring("query ".length()).trim() : "";

        // Extract the type string (first token, should be [TIRtir]+)
        String typeStr = "";
        int typeStrFileOffset = afterQuery.isEmpty()
                ? lineEnd : lineStart + trimmed.indexOf(afterQuery);
        for (int c = 0; c < afterQuery.length(); c++) {
            if (Character.isWhitespace(afterQuery.charAt(c))) break;
            typeStr += afterQuery.charAt(c);
        }
        int typeStrEndOffset = typeStrFileOffset + typeStr.length();

        // 1. Empty type string
        if (typeStr.isEmpty()) {
            annotate(holder, HighlightSeverity.ERROR,
                    "Query requires a column type specification (e.g., query I, query TT)",
                    lineStart, lineEnd);
            return;
        }

        // 2. Invalid characters in type string
        StringBuilder fixedType = new StringBuilder();
        boolean hasInvalid = false;
        for (char c : typeStr.toCharArray()) {
            char upper = Character.toUpperCase(c);
            if (upper == 'T' || upper == 'I' || upper == 'R') {
                fixedType.append(upper);
            } else {
                fixedType.append('I');
                hasInvalid = true;
            }
        }
        if (hasInvalid) {
            holder.newAnnotation(HighlightSeverity.ERROR,
                            "Invalid return type character in '" + typeStr +
                                    "' — only T (text), I (integer), R (real) are allowed")
                    .range(new TextRange(typeStrFileOffset, typeStrEndOffset))
                    .withFix(new SQLTestQuickFix(
                            "Fix return type to '" + fixedType + "'",
                            typeStrFileOffset, typeStrEndOffset, fixedType.toString()))
                    .create();
        }

        int expectedCols = typeStr.length();

        // 3. Empty SQL body
        int sqlLine = findFirstSqlLine(lines, i + 1);
        if (sqlLine < 0) {
            annotate(holder, HighlightSeverity.WARNING,
                    "Empty query body — no SQL after directive", lineStart, lineEnd);
            return;
        }

        // 4. Find ---- separator and validate results
        int separatorLine = -1;
        for (int j = sqlLine; j < lines.length; j++) {
            String t = lines[j].trim();
            if (t.equals("----")) {
                separatorLine = j;
                break;
            }
            if (isDirective(t)) break;
        }

        if (separatorLine < 0) {
            annotate(holder, HighlightSeverity.WARNING,
                    "Query block has no '----' result section", lineStart, lineEnd);
            return;
        }

        // 5. Check result column counts
        checkResultColumns(lines, separatorLine, expectedCols, typeStr,
                typeStrFileOffset, typeStrEndOffset, holder);
    }

    /**
     * Validate result rows: column count must match the type string length.
     */
    private void checkResultColumns(String[] lines, int separatorLine, int expectedCols,
                                    String typeStr, int typeStrFileOffset, int typeStrEndOffset,
                                    AnnotationHolder holder) {
        int actualCols = -1;
        boolean mismatchReported = false;

        for (int j = separatorLine + 1; j < lines.length; j++) {
            String row = lines[j];
            if (row.trim().isEmpty() || isDirective(row.trim())) break;

            int cols = row.split("\t", -1).length;

            // Track actual column count from first row
            if (actualCols < 0) actualCols = cols;

            // Check: column count differs from type string
            if (cols != expectedCols && !mismatchReported) {
                String newType = buildTypeString(typeStr, cols);
                holder.newAnnotation(HighlightSeverity.ERROR,
                                "Result has " + cols + " column(s) but query type '" + typeStr +
                                        "' specifies " + expectedCols)
                        .range(new TextRange(typeStrFileOffset, typeStrEndOffset))
                        .withFix(new SQLTestQuickFix(
                                "Change return type to '" + newType + "'",
                                typeStrFileOffset, typeStrEndOffset, newType))
                        .create();
                mismatchReported = true;
            }

            // Check: inconsistent column count between rows
            if (cols != actualCols) {
                int rowStart = getLineStartOffset(lines, j);
                annotate(holder, HighlightSeverity.WARNING,
                        "Result row has " + cols + " column(s) but first row has " + actualCols +
                                " — inconsistent tab separation",
                        rowStart, rowStart + row.length());
            }
        }
    }

    /**
     * Build a new type string matching the target column count.
     * Reuses characters from the original type string where possible,
     * fills new columns with 'I'.
     */
    private String buildTypeString(String original, int targetCols) {
        StringBuilder sb = new StringBuilder();
        for (int c = 0; c < targetCols; c++) {
            if (c < original.length()) {
                sb.append(Character.toUpperCase(original.charAt(c)));
            } else {
                sb.append('I');
            }
        }
        return sb.toString();
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Loop / foreach parameter checks
    // ═════════════════════════════════════════════════════════════════════════

    private void checkLoopParams(String trimmed, int lineStart, int lineEnd,
                                 AnnotationHolder holder) {
        String[] parts = trimmed.split("\\s+");
        // loop VAR START END  or  concurrentloop VAR START END
        if (parts.length < 4) {
            annotate(holder, HighlightSeverity.ERROR,
                    "'" + parts[0] + "' requires 3 parameters: variable, start, end " +
                            "(e.g., " + parts[0] + " i 0 10)",
                    lineStart, lineEnd);
            return;
        }
        // Validate start < end (skip if template variables)
        String startStr = parts[2];
        String endStr = parts[3];
        if (!startStr.contains("$") && !endStr.contains("$")) {
            try {
                long start = Long.parseLong(startStr);
                long end = Long.parseLong(endStr);
                if (start >= end) {
                    annotate(holder, HighlightSeverity.WARNING,
                            "Loop range is empty — start (" + start + ") >= end (" + end + ")",
                            lineStart, lineEnd);
                }
            } catch (NumberFormatException ignored) {
                // Not numeric literals, skip validation
            }
        }
    }

    private void checkForeachParams(String trimmed, int lineStart, int lineEnd,
                                    AnnotationHolder holder) {
        String[] parts = trimmed.split("\\s+");
        // foreach VAR val1 val2 ...
        if (parts.length < 3) {
            annotate(holder, HighlightSeverity.ERROR,
                    "'" + parts[0] + "' requires a variable name and at least one value " +
                            "(e.g., " + parts[0] + " type INT VARCHAR)",
                    lineStart, lineEnd);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  SQL formatting check
    // ═════════════════════════════════════════════════════════════════════════

    private void checkSqlFormatting(String[] lines, int sqlStart, AnnotationHolder holder) {
        if (sqlStart >= lines.length) return;

        int sqlEnd = sqlStart;
        for (int j = sqlStart; j < lines.length; j++) {
            String t = lines[j].trim();
            if (t.isEmpty() || t.equals("----") || isDirective(t)) break;
            sqlEnd = j + 1;
        }
        if (sqlEnd <= sqlStart) return;

        int blockStartOffset = getLineStartOffset(lines, sqlStart);
        int blockEndOffset = getLineStartOffset(lines, sqlEnd - 1) + lines[sqlEnd - 1].length();

        StringBuilder sqlBuilder = new StringBuilder();
        for (int j = sqlStart; j < sqlEnd; j++) {
            if (j > sqlStart) sqlBuilder.append('\n');
            sqlBuilder.append(lines[j]);
        }
        String sql = sqlBuilder.toString();

        if (SQLTestSqlFormatter.needsFormatting(sql)) {
            int firstLineEnd = blockStartOffset + lines[sqlStart].length();
            holder.newAnnotation(HighlightSeverity.WEAK_WARNING, "SQL is not formatted")
                    .range(new TextRange(blockStartOffset, firstLineEnd))
                    .withFix(new SQLTestFormatSqlFix(blockStartOffset, blockEndOffset))
                    .create();
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Utilities
    // ═════════════════════════════════════════════════════════════════════════

    /** Find the first non-blank, non-directive line after startLine. Returns -1 if none. */
    private int findFirstSqlLine(String[] lines, int startLine) {
        if (startLine >= lines.length) return -1;
        String t = lines[startLine].trim();
        if (t.isEmpty() || isDirective(t)) return -1;
        return startLine;
    }

    private void annotate(AnnotationHolder holder, HighlightSeverity severity,
                          String message, int start, int end) {
        holder.newAnnotation(severity, message)
                .range(new TextRange(start, end))
                .create();
    }

    private int getLineStartOffset(String[] lines, int lineIndex) {
        int offset = 0;
        for (int j = 0; j < lineIndex; j++) {
            offset += lines[j].length() + 1;
        }
        return offset;
    }

    private boolean isDirective(String trimmed) {
        return trimmed.startsWith("statement ") || trimmed.startsWith("query ") ||
                trimmed.startsWith("load") || trimmed.startsWith("loop ") ||
                trimmed.equals("endloop") || trimmed.startsWith("foreach ") ||
                trimmed.equals("endforeach") || trimmed.startsWith("require ") ||
                trimmed.startsWith("require-env ") || trimmed.startsWith("require_reinit") ||
                trimmed.startsWith("mode ") ||
                trimmed.equals("halt") || trimmed.startsWith("skipif ") ||
                trimmed.startsWith("onlyif ") || trimmed.equals("restart") ||
                trimmed.equals("reconnect") ||
                trimmed.startsWith("concurrentloop ") || trimmed.startsWith("concurrentforeach ") ||
                trimmed.startsWith("sleep ") || trimmed.startsWith("hash-threshold ") ||
                trimmed.startsWith("unzip ") || trimmed.startsWith("test-env ") ||
                trimmed.startsWith("tags ") || trimmed.equals("continue") ||
                trimmed.startsWith("template ") || trimmed.startsWith("cache_file ") ||
                trimmed.startsWith("cache ") || trimmed.startsWith("cleanup") ||
                trimmed.startsWith("init") || trimmed.startsWith("reload") ||
                trimmed.startsWith("resultmode ") || trimmed.startsWith("result_query ") ||
                trimmed.startsWith("assert ") || trimmed.startsWith("run") ||
                trimmed.startsWith("include ") || trimmed.startsWith("argument ") ||
                trimmed.startsWith("subgroup ") || trimmed.startsWith("storage ") ||
                trimmed.startsWith("retry ") || trimmed.startsWith("physical_plan") ||
                trimmed.startsWith("set ") || trimmed.startsWith("#");
    }
}
