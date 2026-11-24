package jo.pigma.syntax;

import org.junit.Test;

import jo.pigma.syntax.generic.GenericLexer;
import jo.pigma.syntax.generic.LexerProfile;
import jo.pigma.syntax.lexer.Lexer;

import static org.junit.Assert.assertEquals;

/**
 * La famille générique : un profil SQL pilote le lexeur unique —
 * mots-clés insensibles à la casse, commentaires de fin de ligne et de
 * bloc, chaînes à apostrophes (multi-lignes par report d'état),
 * nombres et opérateurs. Les fenêtres suivent les lignes du texte,
 * comme le moteur le fait.
 */
public class GenericLexerTest {

    private static final LexerProfile PROFILE = Languages.SQL_PROFILE;

    /**
     * Le profil SQL : un commentaire de fin de ligne, mots-clés sans
     * casse, identifiant neutre, nombre, opérateurs, ponctuation.
     */
    @Test
    public void sqlLineShapes() {
        GenericLexer lexer = new GenericLexer(PROFILE);
        String text = "SELECT nom, age -- note\nfrom Usagers";
        lexer.start(text, 0, 23, 0);
        assertEquals("SELECT", next(lexer, text));
        assertEquals(GenericLexer.KEYWORD, lexer.tokenType());
        assertEquals(" ", next(lexer, text));
        assertEquals(GenericLexer.WHITESPACE, lexer.tokenType());
        assertEquals("nom", next(lexer, text));
        assertEquals(GenericLexer.IDENTIFIER, lexer.tokenType());
        assertEquals(",", next(lexer, text));
        assertEquals(GenericLexer.PUNCT, lexer.tokenType());
        assertEquals(" ", next(lexer, text));
        assertEquals("age", next(lexer, text));
        assertEquals(GenericLexer.IDENTIFIER, lexer.tokenType());
        assertEquals(" ", next(lexer, text));
        assertEquals("-- note", next(lexer, text));
        assertEquals(GenericLexer.COMMENT, lexer.tokenType());
        assertEquals(Lexer.NO_TOKEN, endOfWindow(lexer));
        // deuxième ligne : la casse ne distingue pas les mots-clés
        lexer.start(text, 24, 36, 0);
        assertEquals("from", next(lexer, text));
        assertEquals(GenericLexer.KEYWORD, lexer.tokenType());
        assertEquals(" ", next(lexer, text));
        assertEquals("Usagers", next(lexer, text));
        assertEquals(GenericLexer.IDENTIFIER, lexer.tokenType());
        assertEquals(Lexer.NO_TOKEN, endOfWindow(lexer));
    }

    /**
     * Un commentaire de bloc ouvert traverse les fenêtres par report
     * d'état et se referme proprement.
     */
    @Test
    public void blockCommentSpansWindows() {
        GenericLexer lexer = new GenericLexer(PROFILE);
        String text = "a /* b\nc */ d";
        lexer.start(text, 0, 6, 0);
        assertEquals("a", next(lexer, text));
        assertEquals(GenericLexer.IDENTIFIER, lexer.tokenType());
        assertEquals(" ", next(lexer, text));
        assertEquals("/*", next(lexer, text));
        assertEquals(GenericLexer.COMMENT, lexer.tokenType());
        assertEquals(" b", next(lexer, text));
        assertEquals(GenericLexer.STATE_BLOCK, lexer.state());
        lexer.start(text, 7, 13, lexer.state());
        assertEquals("c ", next(lexer, text));
        assertEquals(GenericLexer.COMMENT, lexer.tokenType());
        assertEquals("*/", next(lexer, text));
        assertEquals(GenericLexer.COMMENT, lexer.tokenType());
        assertEquals(" ", next(lexer, text));
        assertEquals("d", next(lexer, text));
        assertEquals(GenericLexer.IDENTIFIER, lexer.tokenType());
        assertEquals(GenericLexer.STATE_CODE, lexer.state());
    }

    /**
     * Une chaîne non fermée dans la fenêtre reste ouverte : l'état
     * porte la paire, la fenêtre suivante la referme.
     */
    @Test
    public void openStringCarriesPairState() {
        GenericLexer lexer = new GenericLexer(PROFILE);
        String text = "'abc\ndef' g";
        lexer.start(text, 0, 4, 0);
        assertEquals("'abc", next(lexer, text));
        assertEquals(GenericLexer.STRING, lexer.tokenType());
        int carried = lexer.state();
        assertEquals(GenericLexer.STATE_STRING_BASE, carried);
        lexer.start(text, 5, 11, carried);
        assertEquals("def'", next(lexer, text));
        assertEquals(GenericLexer.STRING, lexer.tokenType());
        assertEquals(" ", next(lexer, text));
        assertEquals("g", next(lexer, text));
        assertEquals(GenericLexer.IDENTIFIER, lexer.tokenType());
        assertEquals(GenericLexer.STATE_CODE, lexer.state());
    }

    /** Une fenêtre vide dans un commentaire ouvert garde l'état. */
    @Test
    public void emptyWindowKeepsCarriedState() {
        GenericLexer lexer = new GenericLexer(PROFILE);
        String text = "/* a\n\n*/ b";
        lexer.start(text, 0, 4, 0);
        assertEquals("/*", next(lexer, text));
        assertEquals(" a", next(lexer, text));
        int carried = lexer.state();
        assertEquals(GenericLexer.STATE_BLOCK, carried);
        lexer.start(text, 5, 5, carried);
        assertEquals(Lexer.NO_TOKEN, endOfWindow(lexer));
        assertEquals(GenericLexer.STATE_BLOCK, lexer.state());
    }

    /** Les nombres et opérateurs suivent le profil. */
    @Test
    public void numbersAndOperators() {
        GenericLexer lexer = new GenericLexer(PROFILE);
        String text = "12.5e3+-x";
        lexer.start(text, 0, text.length(), 0);
        assertEquals("12.5e3", next(lexer, text));
        assertEquals(GenericLexer.NUMBER, lexer.tokenType());
        assertEquals("+-", next(lexer, text));
        assertEquals(GenericLexer.OPERATOR, lexer.tokenType());
        assertEquals("x", next(lexer, text));
        assertEquals(GenericLexer.IDENTIFIER, lexer.tokenType());
    }

    private static String next(Lexer lexer, CharSequence text) {
        lexer.advance();
        return text.subSequence(lexer.tokenStart(), lexer.tokenEnd()).toString();
    }

    private static int endOfWindow(Lexer lexer) {
        lexer.advance();
        return lexer.tokenType();
    }
}
