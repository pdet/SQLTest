package org.intellij.sdk.language;

import com.intellij.lexer.FlexAdapter;
import java.io.Reader;

public class TestLexerAdapter extends FlexAdapter {

    public TestLexerAdapter() {
        super(new TestLexer(null));
    }

}
