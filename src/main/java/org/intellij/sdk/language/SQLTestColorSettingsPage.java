package org.intellij.sdk.language;

import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.*;
import com.intellij.openapi.options.colors.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Map;

public class SQLTestColorSettingsPage implements ColorSettingsPage {

    private static final AttributesDescriptor[] DESCRIPTORS = new AttributesDescriptor[]{
            new AttributesDescriptor("Directive", SQLTestSyntaxHighlighter.RESERVED),
            new AttributesDescriptor("Identifier", SQLTestSyntaxHighlighter.KEY),
            new AttributesDescriptor("Value/Number", SQLTestSyntaxHighlighter.VALUE),
            new AttributesDescriptor("Comment", SQLTestSyntaxHighlighter.COMMENT),
            new AttributesDescriptor("SQL Keyword", SQLTestSyntaxHighlighter.SQL),
            new AttributesDescriptor("Result Output", SQLTestSyntaxHighlighter.RESULT),
            new AttributesDescriptor("Bad Character", SQLTestSyntaxHighlighter.BAD_CHARACTER)
    };

    @Nullable
    @Override
    public Icon getIcon() {
        return SQLTestIcons.FILE;
    }

    @NotNull
    @Override
    public SyntaxHighlighter getHighlighter() {
        return new SQLTestSyntaxHighlighter();
    }

    @NotNull
    @Override
    public String getDemoText() {
        return "# Test file example\n" +
                "require tpch\n\n" +
                "statement ok\n" +
                "CREATE TABLE test (id INTEGER, name VARCHAR);\n\n" +
                "query II\n" +
                "SELECT id, name FROM test ORDER BY id;\n" +
                "----\n" +
                "1\tAlice\n" +
                "2\tBob\n\n" +
                "loop i 0 10\n" +
                "statement ok\n" +
                "INSERT INTO test VALUES (${i}, 'user');\n" +
                "endloop\n";
    }

    @Nullable
    @Override
    public Map<String, TextAttributesKey> getAdditionalHighlightingTagToDescriptorMap() {
        return null;
    }

    @NotNull
    @Override
    public AttributesDescriptor[] getAttributeDescriptors() {
        return DESCRIPTORS;
    }

    @NotNull
    @Override
    public ColorDescriptor[] getColorDescriptors() {
        return ColorDescriptor.EMPTY_ARRAY;
    }

    @NotNull
    @Override
    public String getDisplayName() {
        return "SQLTest";
    }
}
