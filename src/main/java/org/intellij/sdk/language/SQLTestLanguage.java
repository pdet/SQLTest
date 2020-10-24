package org.intellij.sdk.language;

import com.intellij.lang.Language;

public class SQLTestLanguage extends Language {

    public static final SQLTestLanguage INSTANCE = new SQLTestLanguage();

    private SQLTestLanguage() {
        super("Test");
    }

}
