package org.intellij.sdk.language;

import com.intellij.lang.TokenWrapper;
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
            createTextAttributesKey("TEST_SQL",  DefaultLanguageHighlighterColors.CONSTANT);

    private static final TextAttributesKey[] BAD_CHAR_KEYS = new TextAttributesKey[]{BAD_CHARACTER};
    private static final TextAttributesKey[] RESERVED_KEYS = new TextAttributesKey[]{RESERVED};
    private static final TextAttributesKey[] KEY_KEYS = new TextAttributesKey[]{KEY};
    private static final TextAttributesKey[] QUERY_KEYS = new TextAttributesKey[]{QUERY};

    private static final TextAttributesKey[] VALUE_KEYS = new TextAttributesKey[]{VALUE};
    private static final TextAttributesKey[] COMMENT_KEYS = new TextAttributesKey[]{COMMENT};
    private static final TextAttributesKey[] SQL_KEYS = new TextAttributesKey[]{SQL};
    private static final TextAttributesKey[] EMPTY_KEYS = new TextAttributesKey[0];

    @NotNull
    @Override
    public Lexer getHighlightingLexer() {
        return new SQLTestLexerAdapter();
    }

    @NotNull
    @Override
    public TextAttributesKey[] getTokenHighlights(IElementType tokenType) {
        if (tokenType.equals(TestTypes.LOOP) || tokenType.equals(TestTypes.Q_RESULT) ||
                tokenType.equals(TestTypes.RESTART)|| tokenType.equals(TestTypes.BEGIN)||
                tokenType.equals(TestTypes.TRANSACTION) || tokenType.equals(TestTypes.ROLLBACK)||
                tokenType.equals(TestTypes.PRAGMA) || tokenType.equals(TestTypes.LOAD) ||
                tokenType.equals(TestTypes.ENDLOOP)|| tokenType.equals(TestTypes.STATEMENT) ||
                tokenType.equals(TestTypes.QUERY) || tokenType.equals(TestTypes.PHYSICAL_PLAN)) {
            return RESERVED_KEYS;
        } else if (tokenType.equals(TestTypes.ID)|| tokenType.equals(TestTypes.QUERY_RETURN_TYPE)) {
            return KEY_KEYS;
        } else if (tokenType.equals(TestTypes.NUMBER) || tokenType.equals(TestTypes.QUERY_LABEL)) {
            return VALUE_KEYS;
        } else if (tokenType.equals(TestTypes.COMMENT)) {
            return COMMENT_KEYS;
        } else if (tokenType.equals(TokenType.BAD_CHARACTER)) {
            return BAD_CHAR_KEYS;
        } else if (tokenType.equals(TestTypes.SQL)){
            return SQL_KEYS;
        } else {
            return EMPTY_KEYS;
        }
    }

}