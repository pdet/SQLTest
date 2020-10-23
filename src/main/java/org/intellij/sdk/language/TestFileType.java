package org.intellij.sdk.language;

import com.intellij.openapi.fileTypes.LanguageFileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class TestFileType extends LanguageFileType {

    public static final TestFileType INSTANCE = new TestFileType();

    private TestFileType() {
        super(TestLanguage.INSTANCE);
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
        return TestIcons.FILE;
    }

}
