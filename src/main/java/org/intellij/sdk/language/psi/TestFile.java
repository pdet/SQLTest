package org.intellij.sdk.language.psi;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;
import org.intellij.sdk.language.TestFileType;
import org.intellij.sdk.language.TestLanguage;
import org.jetbrains.annotations.NotNull;

public class TestFile extends PsiFileBase {

    public TestFile(@NotNull FileViewProvider viewProvider) {
        super(viewProvider, TestLanguage.INSTANCE);
    }

    @NotNull
    @Override
    public FileType getFileType() {
        return TestFileType.INSTANCE;
    }

    @Override
    public String toString() {
        return "Simple File";
    }

}
