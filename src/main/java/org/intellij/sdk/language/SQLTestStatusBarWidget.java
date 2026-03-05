package org.intellij.sdk.language;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.event.MouseEvent;

/**
 * Status bar widget that shows the count of statements and queries
 * in the currently open .test file.
 */
public class SQLTestStatusBarWidget implements StatusBarWidget, StatusBarWidget.TextPresentation {

    private final Project myProject;
    private StatusBar myStatusBar;
    private String myText = "";

    private static final java.util.Set<String> TEST_EXTENSIONS = java.util.Set.of(
            "test", "test_slow", "testslow", "benchmark", "slt"
    );

    public SQLTestStatusBarWidget(@NotNull Project project) {
        myProject = project;
        project.getMessageBus().connect().subscribe(
                FileEditorManagerListener.FILE_EDITOR_MANAGER,
                new FileEditorManagerListener() {
                    @Override
                    public void selectionChanged(@NotNull FileEditorManagerEvent event) {
                        updateWidget();
                    }
                }
        );
    }

    @NotNull
    @Override
    public String ID() {
        return "SQLTestCounter";
    }

    @Override
    public void install(@NotNull StatusBar statusBar) {
        myStatusBar = statusBar;
        updateWidget();
    }

    @Override
    public void dispose() {
    }

    @NotNull
    @Override
    public String getText() {
        return myText;
    }

    @NotNull
    @Override
    public String getTooltipText() {
        return "Number of statement and query blocks in this .test file";
    }

    @Nullable
    @Override
    public Consumer<MouseEvent> getClickConsumer() {
        return null;
    }

    @Override
    public float getAlignment() {
        return 0.5f;
    }

    @NotNull
    @Override
    public WidgetPresentation getPresentation() {
        return this;
    }

    private void updateWidget() {
        Editor editor = FileEditorManager.getInstance(myProject).getSelectedTextEditor();
        if (editor == null) {
            myText = "";
            if (myStatusBar != null) myStatusBar.updateWidget(ID());
            return;
        }

        VirtualFile file = FileDocumentManager.getInstance().getFile(editor.getDocument());
        if (file == null || !isTestFile(file)) {
            myText = "";
            if (myStatusBar != null) myStatusBar.updateWidget(ID());
            return;
        }

        Document doc = editor.getDocument();
        String text = doc.getText();
        String[] lines = text.split("\n", -1);

        int statements = 0;
        int queries = 0;
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("statement ")) statements++;
            else if (trimmed.startsWith("query ")) queries++;
        }

        myText = "\uD83E\uDD86 " + statements + " stmt, " + queries + " query";
        if (myStatusBar != null) myStatusBar.updateWidget(ID());
    }

    private boolean isTestFile(@NotNull VirtualFile file) {
        String ext = file.getExtension();
        return ext != null && TEST_EXTENSIONS.contains(ext);
    }
}
