package org.intellij.sdk.language;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.psi.PsiElement;
import org.intellij.sdk.language.psi.TestTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Adds gutter icons for test directives:
 * - Green checkmark for 'statement ok'
 * - Red X for 'statement error'
 * - Blue arrow for 'query'
 */
public class SQLTestLineMarkerProvider implements LineMarkerProvider {

    @Override
    public @Nullable LineMarkerInfo<?> getLineMarkerInfo(@NotNull PsiElement element) {
        if (element.getNode() == null ||
            (element.getNode().getElementType() != TestTypes.STATEMENT &&
             element.getNode().getElementType() != TestTypes.QUERY)) {
            return null;
        }

        // Look at the text after the keyword
        PsiElement next = element.getNextSibling();
        StringBuilder rest = new StringBuilder();
        while (next != null && !next.getText().contains("\n")) {
            rest.append(next.getText());
            next = next.getNextSibling();
        }

        String afterKeyword = rest.toString().trim().toLowerCase();

        Icon icon;
        String tooltip;

        if (element.getNode().getElementType() == TestTypes.STATEMENT) {
            if (afterKeyword.startsWith("ok")) {
                icon = AllIcons.RunConfigurations.TestPassed;
                tooltip = "Statement expects success";
            } else if (afterKeyword.startsWith("error")) {
                icon = AllIcons.RunConfigurations.TestFailed;
                tooltip = "Statement expects error";
            } else if (afterKeyword.startsWith("maybe")) {
                icon = AllIcons.RunConfigurations.TestIgnored;
                tooltip = "Statement expects maybe";
            } else if (afterKeyword.startsWith("debug_skip")) {
                icon = AllIcons.RunConfigurations.TestIgnored;
                tooltip = "Statement debug skip";
            } else if (afterKeyword.startsWith("debug")) {
                icon = AllIcons.RunConfigurations.TestIgnored;
                tooltip = "Statement debug output";
            } else {
                return null;
            }
        } else {
            // Query
            icon = AllIcons.Actions.Execute;
            tooltip = "Query block: " + afterKeyword;
        }

        return new LineMarkerInfo<>(
                element,
                element.getTextRange(),
                icon,
                e -> tooltip,
                null,
                GutterIconRenderer.Alignment.LEFT,
                () -> tooltip
        );
    }
}
