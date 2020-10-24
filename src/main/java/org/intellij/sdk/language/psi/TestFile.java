package org.intellij.sdk.language.psi;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;
import org.intellij.sdk.language.SQLTestFileType;
import org.intellij.sdk.language.SQLTestLanguage;
import org.jetbrains.annotations.NotNull;

public class TestFile extends PsiFileBase {

    public TestFile(@NotNull FileViewProvider viewProvider) {
        super(viewProvider, SQLTestLanguage.INSTANCE);
    }

    @NotNull
    @Override
    public FileType getFileType() {
        return SQLTestFileType.INSTANCE;
    }

    @Override
    public String toString() {
        return "Test File";
    }

}
