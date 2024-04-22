package org.intellij.sdk.language;

import com.intellij.openapi.fileTypes.LanguageFileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class SQLTestFileType extends LanguageFileType {

    public static final SQLTestFileType INSTANCE = new SQLTestFileType();

    private SQLTestFileType() {
        super(SQLTestLanguage.INSTANCE);
    }

    @NotNull
    @Override
    public String getName() {
        return "Test File";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Test language file";
    }

    @NotNull
    @Override
    public String getDefaultExtension() {
        return "test";
    }

    @Nullable
    @Override
    public Icon getIcon() {
        return SQLTestIcons.FILE;
    }

}
