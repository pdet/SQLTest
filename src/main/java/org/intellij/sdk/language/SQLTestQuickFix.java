package org.intellij.sdk.language;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

/**
 * General-purpose quick-fix that replaces a text range in the document.
 * Used for fixing query column types, removing duplicate lines, etc.
 */
public class SQLTestQuickFix implements IntentionAction {

    private final String description;
    private final int startOffset;
    private final int endOffset;
    private final String replacement;

    public SQLTestQuickFix(String description, int startOffset, int endOffset, String replacement) {
        this.description = description;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
        this.replacement = replacement;
    }

    @NotNull
    @Override
    public String getText() {
        return description;
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return "SQLTest";
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        return true;
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        Document doc = editor.getDocument();
        if (startOffset < 0 || endOffset > doc.getTextLength() || startOffset > endOffset) return;
        doc.replaceString(startOffset, endOffset, replacement);
    }

    @Override
    public boolean startInWriteAction() {
        return true;
    }
}
