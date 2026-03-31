package org.intellij.sdk.language;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.HighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import org.intellij.sdk.language.psi.TestTypes;
import org.jetbrains.annotations.NotNull;

import static com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey;

public class SQLTestSyntaxHighlighter extends SyntaxHighlighterBase {

    public static final TextAttributesKey RESERVED =
            createTextAttributesKey("TEST_RESERVED", DefaultLanguageHighlighterColors.METADATA);
    public static final TextAttributesKey KEY =
            createTextAttributesKey("TEST_KEY", DefaultLanguageHighlighterColors.KEYWORD);
    public static final TextAttributesKey VALUE =
            createTextAttributesKey("TEST_VALUE", DefaultLanguageHighlighterColors.STRING);
    public static final TextAttributesKey COMMENT =
            createTextAttributesKey("TEST_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT);
    public static final TextAttributesKey BAD_CHARACTER =
            createTextAttributesKey("TEST_BAD_CHARACTER", HighlighterColors.BAD_CHARACTER);
    public static final TextAttributesKey QUERY =
            createTextAttributesKey("TEST_Query", HighlighterColors.TEXT);
    public static final TextAttributesKey SQL =
            createTextAttributesKey("TEST_SQL", DefaultLanguageHighlighterColors.CONSTANT);
    public static final TextAttributesKey RESULT =
            createTextAttributesKey("TEST_RESULT", DefaultLanguageHighlighterColors.DOC_COMMENT);

    private static final TextAttributesKey[] BAD_CHAR_KEYS = new TextAttributesKey[]{BAD_CHARACTER};
    private static final TextAttributesKey[] RESERVED_KEYS = new TextAttributesKey[]{RESERVED};
    private static final TextAttributesKey[] KEY_KEYS = new TextAttributesKey[]{KEY};
    private static final TextAttributesKey[] QUERY_KEYS = new TextAttributesKey[]{QUERY};
    private static final TextAttributesKey[] VALUE_KEYS = new TextAttributesKey[]{VALUE};
    private static final TextAttributesKey[] COMMENT_KEYS = new TextAttributesKey[]{COMMENT};
    private static final TextAttributesKey[] SQL_KEYS = new TextAttributesKey[]{SQL};
    private static final TextAttributesKey[] RESULT_KEYS = new TextAttributesKey[]{RESULT};
    private static final TextAttributesKey[] EMPTY_KEYS = new TextAttributesKey[0];

    @NotNull
    @Override
    public Lexer getHighlightingLexer() {
        return new SQLTestLexerAdapter();
    }

    @NotNull
    @Override
    public TextAttributesKey[] getTokenHighlights(IElementType tokenType) {
        // Directives
        if (tokenType.equals(TestTypes.LOOP) || tokenType.equals(TestTypes.ENDLOOP) ||
                tokenType.equals(TestTypes.FOREACH) || tokenType.equals(TestTypes.ENDFOREACH) ||
                tokenType.equals(TestTypes.CONCURRENTLOOP) || tokenType.equals(TestTypes.CONCURRENTFOREACH) ||
                tokenType.equals(TestTypes.STATEMENT) || tokenType.equals(TestTypes.QUERY) ||
                tokenType.equals(TestTypes.LOAD) || tokenType.equals(TestTypes.RESTART) ||
                tokenType.equals(TestTypes.RECONNECT) ||
                tokenType.equals(TestTypes.BEGIN) || tokenType.equals(TestTypes.TRANSACTION) ||
                tokenType.equals(TestTypes.ROLLBACK) || tokenType.equals(TestTypes.PRAGMA) ||
                tokenType.equals(TestTypes.REQUIRE) || tokenType.equals(TestTypes.REQUIRE_ENV) ||
                tokenType.equals(TestTypes.REQUIRE_REINIT) ||
                tokenType.equals(TestTypes.MODE) || tokenType.equals(TestTypes.HALT) ||
                tokenType.equals(TestTypes.SKIPIF) || tokenType.equals(TestTypes.ONLYIF) ||
                tokenType.equals(TestTypes.Q_RESULT) || tokenType.equals(TestTypes.PHYSICAL_PLAN) ||
                tokenType.equals(TestTypes.SLEEP) || tokenType.equals(TestTypes.HASH_THRESHOLD) ||
                tokenType.equals(TestTypes.UNZIP) || tokenType.equals(TestTypes.TEST_ENV) ||
                tokenType.equals(TestTypes.TAGS) || tokenType.equals(TestTypes.CONTINUE) ||
                tokenType.equals(TestTypes.TEMPLATE) || tokenType.equals(TestTypes.CACHE) ||
                tokenType.equals(TestTypes.CACHE_FILE) || tokenType.equals(TestTypes.CLEANUP) ||
                tokenType.equals(TestTypes.INIT) || tokenType.equals(TestTypes.RELOAD) ||
                tokenType.equals(TestTypes.RESULTMODE) || tokenType.equals(TestTypes.RESULT_QUERY) ||
                tokenType.equals(TestTypes.ASSERT) || tokenType.equals(TestTypes.RUN) ||
                tokenType.equals(TestTypes.INCLUDE) || tokenType.equals(TestTypes.ARGUMENT) ||
                tokenType.equals(TestTypes.SUBGROUP) || tokenType.equals(TestTypes.STORAGE) ||
                tokenType.equals(TestTypes.RETRY)) {
            return RESERVED_KEYS;
        }
        // Identifiers and return types
        if (tokenType.equals(TestTypes.ID) || tokenType.equals(TestTypes.QUERY_RETURN_TYPE)) {
            return KEY_KEYS;
        }
        // Numbers and labels
        if (tokenType.equals(TestTypes.NUMBER) || tokenType.equals(TestTypes.QUERY_LABEL)) {
            return VALUE_KEYS;
        }
        // Comments
        if (tokenType.equals(TestTypes.COMMENT)) {
            return COMMENT_KEYS;
        }
        // Result lines (dimmed)
        if (tokenType.equals(TestTypes.RESULT_LINE)) {
            return RESULT_KEYS;
        }
        // SQL keywords
        if (tokenType.equals(TestTypes.SQL)) {
            return SQL_KEYS;
        }
        // Bad characters
        if (tokenType.equals(TokenType.BAD_CHARACTER)) {
            return BAD_CHAR_KEYS;
        }
        return EMPTY_KEYS;
    }
}
