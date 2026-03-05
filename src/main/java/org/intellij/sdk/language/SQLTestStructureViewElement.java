package org.intellij.sdk.language;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.ide.util.treeView.smartTree.SortableTreeElement;
import com.intellij.ide.util.treeView.smartTree.TreeElement;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.editor.Document;
import com.intellij.icons.AllIcons;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Root structure element for .test files.
 * Parses the document text to find directives and builds a tree.
 */
public class SQLTestStructureViewElement implements StructureViewTreeElement, SortableTreeElement {

    private final PsiFile myFile;

    public SQLTestStructureViewElement(@NotNull PsiFile file) {
        this.myFile = file;
    }

    @Override
    public Object getValue() {
        return myFile;
    }

    @NotNull
    @Override
    public String getAlphaSortKey() {
        return myFile.getName();
    }

    @NotNull
    @Override
    public ItemPresentation getPresentation() {
        return new PresentationData(myFile.getName(), null, SQLTestIcons.FILE, null);
    }

    @NotNull
    @Override
    public TreeElement @NotNull [] getChildren() {
        Document doc = PsiDocumentManager.getInstance(myFile.getProject()).getDocument(myFile);
        if (doc == null) return TreeElement.EMPTY_ARRAY;

        String text = doc.getText();
        String[] lines = text.split("\n", -1);
        List<TreeElement> elements = new ArrayList<>();

        for (int i = 0; i < lines.length; i++) {
            String trimmed = lines[i].trim();
            String label = null;
            String iconType = null;

            if (trimmed.startsWith("statement ")) {
                String sql = (i + 1 < lines.length) ? lines[i + 1].trim() : "";
                if (sql.length() > 60) sql = sql.substring(0, 57) + "...";
                label = trimmed + (sql.isEmpty() ? "" : "  \u2192  " + sql);
                iconType = "statement";
            } else if (trimmed.startsWith("query ")) {
                String sql = (i + 1 < lines.length) ? lines[i + 1].trim() : "";
                if (sql.length() > 60) sql = sql.substring(0, 57) + "...";
                label = trimmed + (sql.isEmpty() ? "" : "  \u2192  " + sql);
                iconType = "query";
            } else if (trimmed.startsWith("loop ") || trimmed.startsWith("foreach ") ||
                       trimmed.startsWith("concurrentloop ") || trimmed.startsWith("concurrentforeach ")) {
                label = trimmed;
                iconType = "loop";
            } else if (trimmed.startsWith("require ") || trimmed.startsWith("require-env ")) {
                label = trimmed;
                iconType = "require";
            } else if (trimmed.startsWith("load ")) {
                label = trimmed;
                iconType = "load";
            } else if (trimmed.startsWith("skipif ") || trimmed.startsWith("onlyif ")) {
                label = trimmed;
                iconType = "condition";
            } else if (trimmed.startsWith("mode ")) {
                label = trimmed;
                iconType = "mode";
            } else if (trimmed.equals("halt")) {
                label = "halt";
                iconType = "halt";
            } else if (trimmed.equals("restart")) {
                label = "restart";
                iconType = "restart";
            }

            if (label != null) {
                int offset = getLineOffset(lines, i);
                elements.add(new DirectiveElement(myFile, label, offset, iconType));
            }
        }

        return elements.toArray(TreeElement.EMPTY_ARRAY);
    }

    private int getLineOffset(String[] lines, int lineIndex) {
        int offset = 0;
        for (int j = 0; j < lineIndex; j++) {
            offset += lines[j].length() + 1;
        }
        return offset;
    }

    @Override
    public void navigate(boolean requestFocus) {
        myFile.navigate(requestFocus);
    }

    @Override
    public boolean canNavigate() {
        return myFile.canNavigate();
    }

    @Override
    public boolean canNavigateToSource() {
        return myFile.canNavigateToSource();
    }

    /**
     * Leaf element representing a single directive in the structure view.
     */
    static class DirectiveElement implements StructureViewTreeElement, SortableTreeElement {
        private final PsiFile myFile;
        private final String myLabel;
        private final int myOffset;
        private final String myIconType;

        DirectiveElement(@NotNull PsiFile file, @NotNull String label, int offset, String iconType) {
            this.myFile = file;
            this.myLabel = label;
            this.myOffset = offset;
            this.myIconType = iconType;
        }

        @Override
        public Object getValue() {
            return myLabel;
        }

        @NotNull
        @Override
        public String getAlphaSortKey() {
            return myLabel;
        }

        @NotNull
        @Override
        public ItemPresentation getPresentation() {
            Icon icon;
            switch (myIconType) {
                case "query":
                    icon = AllIcons.Nodes.Method;
                    break;
                case "statement":
                    icon = AllIcons.Nodes.Function;
                    break;
                case "loop":
                    icon = AllIcons.Actions.Refresh;
                    break;
                case "require":
                    icon = AllIcons.Nodes.Plugin;
                    break;
                case "load":
                    icon = AllIcons.Actions.Upload;
                    break;
                case "condition":
                    icon = AllIcons.Debugger.Db_set_breakpoint;
                    break;
                case "halt":
                    icon = AllIcons.Actions.Suspend;
                    break;
                case "restart":
                    icon = AllIcons.Actions.Restart;
                    break;
                default:
                    icon = AllIcons.Nodes.Tag;
                    break;
            }
            return new PresentationData(myLabel, null, icon, null);
        }

        @NotNull
        @Override
        public TreeElement @NotNull [] getChildren() {
            return TreeElement.EMPTY_ARRAY;
        }

        @Override
        public void navigate(boolean requestFocus) {
            PsiElement elementAt = myFile.findElementAt(myOffset);
            if (elementAt instanceof com.intellij.pom.Navigatable) {
                ((com.intellij.pom.Navigatable) elementAt).navigate(requestFocus);
            }
        }

        @Override
        public boolean canNavigate() {
            return true;
        }

        @Override
        public boolean canNavigateToSource() {
            return true;
        }
    }
}
