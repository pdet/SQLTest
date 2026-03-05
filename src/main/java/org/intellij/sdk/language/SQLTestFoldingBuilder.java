package org.intellij.sdk.language;

import com.intellij.lang.ASTNode;
import com.intellij.lang.folding.FoldingBuilderEx;
import com.intellij.lang.folding.FoldingDescriptor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.intellij.sdk.language.psi.TestTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides code folding for .test files:
 * - statement blocks (from 'statement' to next directive or EOF)
 * - query blocks (from 'query' to end of result section)
 * - loop/endloop blocks
 * - foreach/endforeach blocks
 * - result sections (from ---- to end of results)
 */
public class SQLTestFoldingBuilder extends FoldingBuilderEx {

    @Override
    public FoldingDescriptor @NotNull [] buildFoldRegions(@NotNull PsiElement root,
                                                          @NotNull Document document,
                                                          boolean quick) {
        List<FoldingDescriptor> descriptors = new ArrayList<>();
        String text = document.getText();
        String[] lines = text.split("\n", -1);

        int i = 0;
        while (i < lines.length) {
            String trimmed = lines[i].trim();

            // Fold statement blocks
            if (trimmed.startsWith("statement ")) {
                int start = getLineStartOffset(lines, i);
                int end = findBlockEnd(lines, i + 1);
                if (end > start + 1) {
                    descriptors.add(new FoldingDescriptor(root.getNode(),
                            new TextRange(start, Math.min(end, text.length())),
                            null, trimmed));
                }
            }

            // Fold query blocks (includes result section)
            if (trimmed.startsWith("query ")) {
                int start = getLineStartOffset(lines, i);
                int end = findBlockEnd(lines, i + 1);
                if (end > start + 1) {
                    descriptors.add(new FoldingDescriptor(root.getNode(),
                            new TextRange(start, Math.min(end, text.length())),
                            null, trimmed));
                }
            }

            // Fold loop/endloop
            if (trimmed.startsWith("loop ")) {
                int start = getLineStartOffset(lines, i);
                int end = findMatchingEnd(lines, i + 1, "endloop");
                if (end > start + 1) {
                    descriptors.add(new FoldingDescriptor(root.getNode(),
                            new TextRange(start, Math.min(end, text.length())),
                            null, trimmed + " ... endloop"));
                }
            }

            // Fold foreach/endforeach
            if (trimmed.startsWith("foreach ")) {
                int start = getLineStartOffset(lines, i);
                int end = findMatchingEnd(lines, i + 1, "endforeach");
                if (end > start + 1) {
                    descriptors.add(new FoldingDescriptor(root.getNode(),
                            new TextRange(start, Math.min(end, text.length())),
                            null, trimmed + " ... endforeach"));
                }
            }

            i++;
        }

        return descriptors.toArray(FoldingDescriptor.EMPTY_ARRAY);
    }

    private int getLineStartOffset(String[] lines, int lineIndex) {
        int offset = 0;
        for (int i = 0; i < lineIndex; i++) {
            offset += lines[i].length() + 1; // +1 for \n
        }
        return offset;
    }

    private int getLineEndOffset(String[] lines, int lineIndex) {
        return getLineStartOffset(lines, lineIndex) + lines[lineIndex].length();
    }

    /**
     * Find the end of a statement/query block.
     * A block ends at the line before the next directive keyword or at EOF.
     */
    private int findBlockEnd(String[] lines, int startLine) {
        boolean inResults = false;
        for (int i = startLine; i < lines.length; i++) {
            String trimmed = lines[i].trim();

            if (trimmed.equals("----")) {
                inResults = true;
                continue;
            }

            if (inResults && trimmed.isEmpty()) {
                // End of result section = end of block
                return getLineEndOffset(lines, i - 1);
            }

            if (!inResults && isDirective(trimmed) && i > startLine) {
                // Hit next directive
                // Go back to skip blank lines
                int end = i - 1;
                while (end > startLine && lines[end].trim().isEmpty()) {
                    end--;
                }
                return getLineEndOffset(lines, end);
            }
        }
        // EOF
        int end = lines.length - 1;
        while (end > startLine && lines[end].trim().isEmpty()) {
            end--;
        }
        return getLineEndOffset(lines, end);
    }

    /**
     * Find matching endloop/endforeach.
     */
    private int findMatchingEnd(String[] lines, int startLine, String endKeyword) {
        int depth = 1;
        String startKeyword = endKeyword.replace("end", "");
        for (int i = startLine; i < lines.length; i++) {
            String trimmed = lines[i].trim();
            if (trimmed.startsWith(startKeyword + " ") || trimmed.equals(startKeyword)) {
                depth++;
            }
            if (trimmed.equals(endKeyword)) {
                depth--;
                if (depth == 0) {
                    return getLineEndOffset(lines, i);
                }
            }
        }
        return getLineEndOffset(lines, lines.length - 1);
    }

    private boolean isDirective(String trimmed) {
        return trimmed.startsWith("statement ") || trimmed.startsWith("query ") ||
                trimmed.startsWith("load ") || trimmed.startsWith("loop ") ||
                trimmed.equals("endloop") || trimmed.startsWith("foreach ") ||
                trimmed.equals("endforeach") || trimmed.startsWith("require ") ||
                trimmed.startsWith("require-env ") || trimmed.startsWith("mode ") ||
                trimmed.equals("halt") || trimmed.startsWith("skipif ") ||
                trimmed.startsWith("onlyif ") || trimmed.equals("restart") ||
                trimmed.startsWith("concurrentloop ") || trimmed.startsWith("concurrentforeach ");
    }

    @Nullable
    @Override
    public String getPlaceholderText(@NotNull ASTNode node) {
        return "...";
    }

    @Override
    public boolean isCollapsedByDefault(@NotNull ASTNode node) {
        return false;
    }
}
