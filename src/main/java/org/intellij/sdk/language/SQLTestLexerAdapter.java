package org.intellij.sdk.language;

import com.intellij.lexer.FlexAdapter;

public class SQLTestLexerAdapter extends FlexAdapter {

    public SQLTestLexerAdapter() {
        super(new _TestLexer(null));
    }

}
