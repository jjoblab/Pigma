package jo.pigma.syntax;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;

import jo.pigma.core.TextBuffer;
import jo.pigma.core.UndoStack;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * La fenêtre paresseuse (E3) : ouvrir ne lexe rien, une demande étend
 * le préfixe jusqu'à la ligne demandée et jamais au-delà, les tranches
 * de remplissage bornent le travail, la libération des entrées garde
 * les points de contrôle (un retour lointain repart du plus proche) et
 * une édition au-delà du préfixe reste juste.
 */
public class LazyWindowTest {

    private static final int CORPUS_LINES = 2010;

    private String corpus() throws IOException {
        Path source = LexersGoldenTest.resource("data/java-2000.java");
        return Files.readString(source, StandardCharsets.UTF_8);
    }

    /**
     * Ouvrir ne lexe rien ; la première demande paie sa fenêtre seule.
     *
     * @throws IOException si le corpus ne se lit pas
     */
    @Test
    public void openingLexesNothing() throws IOException {
        TextBuffer buffer = TextBuffer.create(corpus());
        SyntaxDocument document = SyntaxDocument.open(buffer, Languages.JAVA);
        assertEquals(CORPUS_LINES, document.lineCount());
        assertEquals(0, document.lexedLineCount());
        document.lineTokens(10);
        assertEquals(11, document.lexedLineCount());
        assertEquals(11, document.lastRequestLines());
        // ligne déjà calculée : aucun travail
        document.lineTokens(5);
        assertEquals(0, document.lastRequestLines());
        // la fenêtre s'étend, elle ne revient pas
        document.lineTokens(30);
        assertEquals(31, document.lexedLineCount());
        document.detach();
    }

    /**
     * Les tranches de remplissage bornent le travail par appel.
     *
     * @throws IOException si le corpus ne se lit pas
     */
    @Test
    public void advanceLexingFillsInSlices() throws IOException {
        TextBuffer buffer = TextBuffer.create(corpus());
        SyntaxDocument document = SyntaxDocument.open(buffer, Languages.JAVA);
        assertEquals(100, document.advanceLexing(100));
        assertEquals(100, document.advanceLexing(100));
        assertEquals(100, document.advanceLexing(100));
        assertEquals(300, document.lexedLineCount());
        assertEquals(CORPUS_LINES - 300, document.remainingLines());
        assertEquals(0, document.advanceLexing(0));
        // la dernière tranche s'arrête au document
        int rest = document.advanceLexing(10_000);
        assertEquals(CORPUS_LINES - 300, rest);
        assertEquals(0, document.remainingLines());
        assertTrue(document.tokenCount() > 20_000);
        document.detach();
    }

    /**
     * La libération des entrées garde les points de contrôle : un
     * retour lointain ne relit que depuis le point de contrôle le plus
     * proche, pas depuis la frontière libérée.
     *
     * @throws IOException si le corpus ne se lit pas
     */
    @Test
    public void farJumpRestartsFromCheckpoint() throws IOException {
        TextBuffer buffer = TextBuffer.create(corpus());
        SyntaxDocument document = SyntaxDocument.open(buffer, Languages.JAVA);
        document.advanceLexing(CORPUS_LINES);
        document.releaseEntries(64);
        assertEquals(64, document.lexedLineCount());
        // saut à la ligne 500 : le point de contrôle à 256 sert
        document.lineTokens(500);
        // 500 - 256 + 1 lignes lexées pour servir la demande
        assertEquals(245, document.lastRequestLines());
        // la zone libérée avant le point de contrôle reste non calculée
        assertEquals(64, document.lexedLineCount());
        // la ligne servie est correcte : même jetons qu'un document neuf
        SyntaxDocument fresh = SyntaxDocument.open(
                TextBuffer.create(corpus()), Languages.JAVA);
        assertEquals(tokenTexts(document.lineTokens(500).tokens()),
                tokenTexts(fresh.lineTokens(500).tokens()));
        document.detach();
        fresh.detach();
    }

    /**
     * Une édition au-delà du préfixe calculé ne casse rien : les
     * jetons demandés ensuite égalent ceux d'un document neuf.
     *
     * @throws IOException si le corpus ne se lit pas
     */
    @Test
    public void editBeyondPrefixStaysCorrect() throws IOException {
        String text = corpus();
        TextBuffer buffer = TextBuffer.create(text);
        UndoStack undo = new UndoStack(buffer);
        SyntaxDocument document = SyntaxDocument.open(buffer, Languages.JAVA);
        document.lineTokens(5);
        int offset = text.indexOf('\n', text.indexOf("class")) + 1;
        edit(undo, offset, offset + 4, "zzzz");
        SyntaxDocument fresh = SyntaxDocument.open(
                TextBuffer.create(buffer.toString()), Languages.JAVA);
        for (int line : new int[] {0, 3, 6, 12}) {
            assertEquals("ligne " + line,
                    tokenTexts(document.lineTokens(line).tokens()),
                    tokenTexts(fresh.lineTokens(line).tokens()));
        }
        document.detach();
        fresh.detach();
    }

    private static void edit(UndoStack undo, int start, int end, String replacement) {
        try (var transaction = undo.newTransaction()) {
            transaction.replace(start, end, replacement);
        }
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
