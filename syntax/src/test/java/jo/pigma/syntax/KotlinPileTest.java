package jo.pigma.syntax;

import org.junit.Test;

import jo.pigma.core.TextBuffer;
import jo.pigma.core.UndoStack;
import jo.pigma.syntax.lexer.Lexer;
import jo.pigma.syntax.lexers.generated.KotlinLexer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * La pile de contextes du lexeur Kotlin : gabarits et imbrications
 * traversent les fenêtres par l'entier d'état composé (plat dans les
 * 4 bits bas, pile au-dessus). L'encodage est bijectif — deux piles
 * égales, même entier — et une instance neuve qui reprend un état
 * composé rendu par une autre reproduit exactement le flux : c'est la
 * garantie que la règle d'arrêt du moteur et les points de contrôle
 * fonctionnent pour Kotlin.
 */
public class KotlinPileTest {

    /**
     * Une chaîne brute traverse les lignes par son état plat, pile
     * vide : l'ouvrant parti d'un code initial nul s'encode à rien.
     */
    @Test
    public void rawStringCarriesStateAcrossLines() {
        Lexer lexer = Languages.KOTLIN.newLexer();
        assertEquals(KotlinLexer.RAW, endState(lexer, "val s = \"\"\"a", 0));
        assertEquals(KotlinLexer.RAW, endState(lexer, "b", KotlinLexer.RAW));
        assertEquals(0, endState(lexer, "\"\"\"", KotlinLexer.RAW));
    }

    /**
     * Un gabarit ouvert dans une chaîne brute traverse les lignes en
     * portant la pile : l'état composé vaut le gabarit au-dessus de la
     * pile, et sa fermeture dépile puis rend l'état plat de la chaîne.
     */
    @Test
    public void templateInRawStringCarriesThePile() {
        Lexer lexer = Languages.KOTLIN.newLexer();
        int composed = endState(lexer, "val s = \"\"\"a${", 0);
        assertEquals("gabarit sur pile [brute]",
                KotlinLexer.TEMPLATE | (KotlinLexer.RAW << 4), composed);
        assertEquals("la fermeture dépile vers la chaîne brute",
                KotlinLexer.RAW, endState(lexer, "1 + 2}", composed));
        assertEquals(0, endState(lexer, "b\"\"\"", KotlinLexer.RAW));
    }

    /**
     * Une chaîne classique non fermée meurt à la frontière de fenêtre
     * (les chaînes Kotlin non brutes ne traversent pas les lignes) :
     * l'état retombe à zéro, la ligne suivante repart proprement.
     */
    @Test
    public void classicStringDiesAtLineBoundary() {
        Lexer lexer = Languages.KOTLIN.newLexer();
        assertEquals(0, endState(lexer, "val s = \"abc", 0));
        String next = describe(lexer, "val t = 1", 0);
        assertTrue("la suite se lexe comme du code : " + next,
                next.startsWith(KotlinLexer.KEYWORD + "@3 "));
    }

    /**
     * Un gabarit ouvert dans une chaîne classique meurt avec elle à la
     * frontière : la pile contenait la chaîne, tout retombe à zéro.
     */
    @Test
    public void templateInClassicStringDiesToo() {
        Lexer lexer = Languages.KOTLIN.newLexer();
        assertEquals(0, endState(lexer, "val s = \"a${1 +", 0));
    }

    /**
     * L'encodage est bijectif (l'internement de la conception) : deux
     * contextes égaux issus de textes différents rendent le même
     * entier d'état.
     */
    @Test
    public void equalPilesGiveSameInteger() {
        Lexer first = Languages.KOTLIN.newLexer();
        Lexer second = Languages.KOTLIN.newLexer();
        int left = endState(first, "val a = \"\"\"p${", 0);
        int right = endState(second, "val b = \"\"\"z${", 0);
        assertNotEquals(0, left);
        assertEquals("deux piles égales, même entier", left, right);
    }

    /**
     * Une instance neuve qui reprend l'état composé rendu par une autre
     * instance reproduit le flux exact : jetons et états de chaque
     * fenêtre suivante identiques — le redémarrage depuis la pile
     * internée est la garantie du moteur.
     */
    @Test
    public void freshInstanceRestartReproducesTheFlow() {
        String[] lines = {"val s = \"\"\"a${", "1 +", "2}b", "\"\"\""};
        String[] full = new String[lines.length];
        Lexer first = Languages.KOTLIN.newLexer();
        int state = 0;
        for (int i = 0; i < lines.length; i++) {
            full[i] = describe(first, lines[i], state) + " @" + first.state();
            state = first.state();
        }
        assertTrue("le gabarit doit traverser les fenêtres en état composé",
                full[1].endsWith("@" + (KotlinLexer.TEMPLATE | (KotlinLexer.RAW << 4))));
        int carried = Integer.parseInt(
                full[0].substring(full[0].lastIndexOf('@') + 1));
        Lexer second = Languages.KOTLIN.newLexer();
        for (int i = 1; i < lines.length; i++) {
            assertEquals("fenêtre " + i, full[i], describe(second, lines[i], carried)
                    + " @" + second.state());
            carried = second.state();
        }
    }

    /**
     * Au-delà de la profondeur maximale (pathologique), les ouvertures
     * s'ignorent : rien ne plante, la couverture reste intacte et la
     * ligne finit hors construction traversante.
     */
    @Test
    public void deepNestingBeyondCapStaysCovered() {
        Lexer lexer = Languages.KOTLIN.newLexer();
        String pathological = "val s = \"a${\"b${\"c${\"d${\"e${\"f${\"g${\"h\"}\"}\"}\"}\"}\"}\"}\"";
        String described = describe(lexer, pathological, 0);
        assertTrue("la couverture reste intacte : " + described,
                described.length() > 0);
        assertEquals(0, lexer.state());
    }

    /**
     * Les commentaires de bloc imbriqués portent leur profondeur dans
     * la pile : une fermeture dépile, la dernière rend l'état initial.
     */
    @Test
    public void nestedBlockCommentsCarryDepth() {
        Lexer lexer = Languages.KOTLIN.newLexer();
        assertEquals(KotlinLexer.BLOCK, endState(lexer, "/* a", 0));
        assertEquals("imbrication portée par la pile",
                KotlinLexer.BLOCK | (KotlinLexer.BLOCK << 4),
                endState(lexer, "/* b", KotlinLexer.BLOCK));
        assertEquals("la fermeture interne dépile",
                KotlinLexer.BLOCK, endState(lexer, "*/ c",
                        KotlinLexer.BLOCK | (KotlinLexer.BLOCK << 4)));
        assertEquals(0, endState(lexer, "*/ d", KotlinLexer.BLOCK));
    }

    /**
     * Le moteur complet avec Kotlin : une édition au milieu d'une
     * chaîne brute relexe par la règle d'arrêt en transportant l'état
     * composé, et rend les jetons d'un document neuf.
     */
    @Test
    public void documentEditInsideRawStringStaysCorrect() {
        String text = "val s = \"\"\"a\nb\nc\"\"\"\nval t = 1\n";
        TextBuffer buffer = TextBuffer.create(text);
        UndoStack undo = new UndoStack(buffer);
        SyntaxDocument document = SyntaxDocument.open(buffer, Languages.KOTLIN);
        document.lineTokens(3);
        int offset = text.indexOf('b');
        try (var transaction = undo.newTransaction()) {
            transaction.replace(offset, offset + 1, "xyz");
        }
        SyntaxDocument fresh = SyntaxDocument.open(
                TextBuffer.create(buffer.toString()), Languages.KOTLIN);
        for (int line : new int[] {0, 1, 2, 3}) {
            assertEquals("ligne " + line,
                    tokenTexts(document.lineTokens(line).tokens()),
                    tokenTexts(fresh.lineTokens(line).tokens()));
        }
        document.detach();
        fresh.detach();
    }

    // ------------------------------------------------------------ outil

    /** Lexe la ligne depuis l'état donné et rend l'état composé final. */
    private static int endState(Lexer lexer, CharSequence line, int state) {
        describe(lexer, line, state);
        return lexer.state();
    }

    /** Jetons de la ligne (type et longueur), en vérifiant la couverture. */
    private static String describe(Lexer lexer, CharSequence line, int state) {
        lexer.start(line, 0, line.length(), state);
        lexer.advance();
        StringBuilder out = new StringBuilder();
        int covered = 0;
        while (lexer.tokenType() != Lexer.NO_TOKEN) {
            out.append(lexer.tokenType()).append('@')
                    .append(lexer.tokenEnd() - lexer.tokenStart()).append(' ');
            covered += lexer.tokenEnd() - lexer.tokenStart();
            lexer.advance();
        }
        assertEquals("trou dans la couverture : " + out, line.length(), covered);
        return out.toString();
    }

    private static String tokenTexts(SyntaxDocument.Token[] tokens) {
        StringBuilder text = new StringBuilder();
        for (SyntaxDocument.Token token : tokens) {
            text.append(token.type()).append(':').append(token.start())
                    .append('-').append(token.end()).append(' ');
        }
        return text.toString();
    }
}
