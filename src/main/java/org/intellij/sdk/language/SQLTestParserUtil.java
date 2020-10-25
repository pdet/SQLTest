package org.intellij.sdk.language;

import com.intellij.lang.Language;
import com.intellij.lang.PsiBuilder;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.InjectedLanguagePlaces;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.tree.IElementType;
import org.intellij.sdk.language.psi.TestTokenType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;




public class SQLTestParserUtil {
    public void getLanguagesToInject(@NotNull final PsiLanguageInjectionHost host, @NotNull final InjectedLanguagePlaces places) {
        final boolean isSelectQuery = host.getText().trim().toUpperCase().startsWith("SELECT");
        final boolean isDataSetFile = host.getContainingFile().getText().startsWith("<dataset>");
        if (isDataSetFile && isSelectQuery) {
            final Language language = Language.findLanguageByID("SQL");
            if (language != null) {
                try {
                    places.addPlace(language, TextRange.from(0, host.getTextLength()), null, null);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }
    }
    public static boolean sqlResults(PsiBuilder builder, int level) {
        boolean skip = false;
        IElementType x = builder.lookAhead(1);
        IElementType ID = new TestTokenType("id");
        IElementType NUMBER = new TestTokenType("number");
        IElementType EMPTY = new TestTokenType(" ");

        while (true){
            assert x != null;
            if (!(x.toString().equals(ID.toString())  || x.toString().equals(NUMBER.toString())  || x.toString().equals(EMPTY.toString()))) break;
            builder.advanceLexer();
            x = builder.lookAhead(1);
            skip = true;
        }
        if (skip){
            builder.advanceLexer();
        }
        return true;
    }

    public static boolean sqlQuery(PsiBuilder builder, int level) {
         String text = builder.getTokenText();
        final Language language = Language.findLanguageByID("SQL");
        //PsiLanguageInjectionHost psi = new PsiLanguageInjectionHost(text);
        return true;
    }
}