package org.intellij.sdk.language;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Navigate to the previous test directive (statement/query) in the file.
 * Bound to Ctrl+Shift+Up (or via menu).
 */
public class SQLTestGoToPreviousAction extends AnAction {

    private static final Set<String> TEST_EXTENSIONS = Set.of(
            "test", "test_slow", "testslow", "benchmark", "slt"
    );

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        if (editor == null) return;

        Document doc = editor.getDocument();
        int currentLine = doc.getLineNumber(editor.getCaretModel().getOffset());
        String text = doc.getText();
        String[] lines = text.split("\n", -1);

        for (int i = currentLine - 1; i >= 0; i--) {
            String trimmed = lines[i].trim();
            if (isTestDirective(trimmed)) {
                int offset = getLineStartOffset(lines, i);
                editor.getCaretModel().moveToOffset(offset);
                editor.getScrollingModel().scrollToCaret(ScrollType.CENTER);
                return;
            }
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        boolean enabled = false;
        if (editor != null) {
            VirtualFile file = FileDocumentManager.getInstance().getFile(editor.getDocument());
            if (file != null) {
                String ext = file.getExtension();
                enabled = ext != null && TEST_EXTENSIONS.contains(ext);
            }
        }
        e.getPresentation().setEnabledAndVisible(enabled);
    }

    private boolean isTestDirective(String trimmed) {
        return trimmed.startsWith("statement ") || trimmed.startsWith("query ");
    }

    private int getLineStartOffset(String[] lines, int lineIndex) {
        int offset = 0;
        for (int j = 0; j < lineIndex; j++) {
            offset += lines[j].length() + 1;
        }
        return offset;
    }
}
