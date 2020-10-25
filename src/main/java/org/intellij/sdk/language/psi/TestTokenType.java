package org.intellij.sdk.language.psi;

import com.intellij.psi.tree.IElementType;
import org.intellij.sdk.language.SQLTestLanguage;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class TestTokenType extends IElementType {
    String debugName;
    public TestTokenType(@NotNull @NonNls String debugName) {
        super(debugName, SQLTestLanguage.INSTANCE);
    }

    @Override
    public String toString() {
        return "TestTokenType." + super.toString();
    }

}
