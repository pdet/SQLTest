package org.intellij.sdk.language.psi;

import com.intellij.psi.tree.IElementType;
import org.intellij.sdk.language.TestLanguage;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class TestElementType extends IElementType {

    public TestElementType(@NotNull @NonNls String debugName) {
        super(debugName, TestLanguage.INSTANCE);
    }

}