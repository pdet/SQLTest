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

/**
 * Annotates .test files with warnings/errors:
 * - query block without ---- result separator
 * - statement without ok/error/maybe qualifier
 * - Mismatched loop/endloop
 * - Mismatched foreach/endforeach
 * - endloop/endforeach without opening
 */
public class SQLTestAnnotator implements Annotator {

    @Override
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        // Only run on the file-level element to avoid per-token overhead
        if (!(element instanceof PsiFile)) return;

        Document doc = PsiDocumentManager.getInstance(element.getProject()).getDocument((PsiFile) element);
        if (doc == null) return;

        String text = doc.getText();
        String[] lines = text.split("\n", -1);

        int loopDepth = 0;
        int foreachDepth = 0;
        int loopLine = -1;
        int foreachLine = -1;

        for (int i = 0; i < lines.length; i++) {
            String trimmed = lines[i].trim();
            int lineStart = getLineStartOffset(lines, i);

            // Check statement qualifiers
            if (trimmed.startsWith("statement ")) {
                String rest = trimmed.substring("statement ".length()).trim().toLowerCase();
                if (!rest.startsWith("ok") && !rest.startsWith("error") && !rest.startsWith("maybe")) {
                    holder.newAnnotation(HighlightSeverity.WARNING,
                                    "Statement should be followed by 'ok', 'error', or 'maybe'")
                            .range(new TextRange(lineStart, lineStart + lines[i].length()))
                            .create();
                }
            }

            // Check query blocks have ---- result section
            if (trimmed.startsWith("query ")) {
                boolean hasResults = false;
                for (int j = i + 1; j < lines.length; j++) {
                    String nextTrimmed = lines[j].trim();
                    if (nextTrimmed.equals("----")) {
                        hasResults = true;
                        break;
                    }
                    // Hit next directive = no results found
                    if (isDirective(nextTrimmed)) break;
                }
                if (!hasResults) {
                    holder.newAnnotation(HighlightSeverity.WARNING,
                                    "Query block has no '----' result section")
                            .range(new TextRange(lineStart, lineStart + lines[i].length()))
                            .create();
                }
            }

            // Track loop/endloop
            if (trimmed.startsWith("loop ")) {
                if (loopDepth == 0) loopLine = i;
                loopDepth++;
            }
            if (trimmed.equals("endloop")) {
                loopDepth--;
                if (loopDepth < 0) {
                    holder.newAnnotation(HighlightSeverity.ERROR,
                                    "'endloop' without matching 'loop'")
                            .range(new TextRange(lineStart, lineStart + lines[i].length()))
                            .create();
                    loopDepth = 0;
                }
            }

            // Track foreach/endforeach
            if (trimmed.startsWith("foreach ") || trimmed.startsWith("concurrentforeach ")) {
                if (foreachDepth == 0) foreachLine = i;
                foreachDepth++;
            }
            if (trimmed.equals("endforeach")) {
                foreachDepth--;
                if (foreachDepth < 0) {
                    holder.newAnnotation(HighlightSeverity.ERROR,
                                    "'endforeach' without matching 'foreach'")
                            .range(new TextRange(lineStart, lineStart + lines[i].length()))
                            .create();
                    foreachDepth = 0;
                }
            }

            // Track concurrentloop — uses endloop
            if (trimmed.startsWith("concurrentloop ")) {
                if (loopDepth == 0) loopLine = i;
                loopDepth++;
            }
        }

        // Unclosed loops
        if (loopDepth > 0 && loopLine >= 0) {
            int lineStart = getLineStartOffset(lines, loopLine);
            holder.newAnnotation(HighlightSeverity.ERROR,
                            "Unclosed 'loop' — missing 'endloop'")
                    .range(new TextRange(lineStart, lineStart + lines[loopLine].length()))
                    .create();
        }

        // Unclosed foreach
        if (foreachDepth > 0 && foreachLine >= 0) {
            int lineStart = getLineStartOffset(lines, foreachLine);
            holder.newAnnotation(HighlightSeverity.ERROR,
                            "Unclosed 'foreach' — missing 'endforeach'")
                    .range(new TextRange(lineStart, lineStart + lines[foreachLine].length()))
                    .create();
        }
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
                trimmed.startsWith("load ") || trimmed.startsWith("loop ") ||
                trimmed.equals("endloop") || trimmed.startsWith("foreach ") ||
                trimmed.equals("endforeach") || trimmed.startsWith("require ") ||
                trimmed.startsWith("require-env ") || trimmed.startsWith("mode ") ||
                trimmed.equals("halt") || trimmed.startsWith("skipif ") ||
                trimmed.startsWith("onlyif ") || trimmed.equals("restart") ||
                trimmed.startsWith("concurrentloop ") || trimmed.startsWith("concurrentforeach ") ||
                trimmed.startsWith("#");
    }
}
