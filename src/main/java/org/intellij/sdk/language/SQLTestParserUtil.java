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
        String text = builder.getTokenText();
        IElementType cur = builder.getTokenType();
        IElementType x = builder.lookAhead(1);
        IElementType ID = new TestTokenType("id");
        IElementType NUMBER = new TestTokenType("number");
        IElementType EMPTY = new TestTokenType(" ");
        if (cur != null){
            while (cur.toString().equals(ID.toString()) || cur.toString().equals(NUMBER.toString()) || cur.toString().equals(EMPTY.toString())){
                builder.advanceLexer();
                cur = builder.getTokenType();
                text = builder.getTokenText();
            }
        }
        return true;
    }

    public static boolean accept_all(PsiBuilder builder, int level) {
        IElementType cur = builder.getTokenType();
        if (cur != null){
            builder.advanceLexer();
            cur = builder.getTokenType();
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