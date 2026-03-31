package org.intellij.sdk.language;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

/**
 * Quick-fix that formats SQL inside statement/query blocks:
 * uppercases keywords, breaks into multi-line clauses, indents.
 */
public class SQLTestFormatSqlFix implements IntentionAction {

    private final int sqlStartOffset;
    private final int sqlEndOffset;

    public SQLTestFormatSqlFix(int sqlStartOffset, int sqlEndOffset) {
        this.sqlStartOffset = sqlStartOffset;
        this.sqlEndOffset = sqlEndOffset;
    }

    @NotNull
    @Override
    public String getText() {
        return "Format SQL";
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
        if (sqlStartOffset < 0 || sqlEndOffset > doc.getTextLength() || sqlStartOffset > sqlEndOffset) return;

        String sql = doc.getText().substring(sqlStartOffset, sqlEndOffset);
        String formatted = SQLTestSqlFormatter.format(sql);
        if (!formatted.equals(sql)) {
            doc.replaceString(sqlStartOffset, sqlEndOffset, formatted);
        }
    }

    @Override
    public boolean startInWriteAction() {
        return true;
    }
}
