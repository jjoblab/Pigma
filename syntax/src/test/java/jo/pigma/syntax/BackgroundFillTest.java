package jo.pigma.syntax;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

import org.junit.Test;

import jo.pigma.core.TextBuffer;
import jo.pigma.core.TextTransaction;
import jo.pigma.core.UndoStack;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Le fil de fond annulable (E6) : les clichés du remplissage lexe
 * hors verrou et ne s'appliquent que si la révision observée du
 * moteur correspond encore — une frappe entre la prise et
 * l'application annule la tranche, jamais le document. Les entrées du
 * fond sont exactement celles du remplissage synchrone (lexeurs
 * déterministes), le vrai fil tient des éditions simultanées sans
 * jamais corrompre le document, un fill arrêté ignore les relances.
 */
public class BackgroundFillTest {

    private String corpus() throws IOException {
        Path source = LexersGoldenTest.resource("data/java-2000.java");
        return Files.readString(source, StandardCharsets.UTF_8);
    }

    /**
     * Les entrées du fil de fond (cliché par cliché, boucle manuelle)
     * sont exactement celles du remplissage synchrone — jeton pour
     * jeton, état pour état, point de contrôle pour point de contrôle
     * — y compris la ligne monstre hors plafond.
     *
     * @throws IOException si le corpus ne se lit pas
     */
    @Test
    public void backgroundSlicesMatchSynchronous() throws IOException {
        StringBuilder text = new StringBuilder(corpus());
        text.append('\n').append("x".repeat(25_000)).append("\n// fin\n");
        TextBuffer buffer = TextBuffer.create(text.toString());
        SyntaxDocument viaFill = SyntaxDocument.open(buffer, Languages.JAVA);
        SyntaxDocument direct = SyntaxDocument.open(buffer, Languages.JAVA);

        int guard = 0;
        SyntaxDocument.FillSlice slice;
        while ((slice = viaFill.takeFillSlice(200)) != null) {
            assertTrue("la boucle du fond doit finir", guard++ < 200);
            SyntaxDocument.LineTokens[] entries = viaFill.lexFillSlice(slice);
            assertTrue("sans édition, chaque cliché s'applique",
                    viaFill.applyFill(slice, entries) > 0);
        }
        direct.advanceLexing(Integer.MAX_VALUE);

        assertEquals(direct.lineCount(), viaFill.lineCount());
        assertEquals(direct.lexedLineCount(), viaFill.lexedLineCount());
        assertEquals(direct.tokenCount(), viaFill.tokenCount());
        for (int line = 0; line < direct.lineCount(); line++) {
            SyntaxDocument.LineTokens expected = direct.lineTokens(line);
            SyntaxDocument.LineTokens actual = viaFill.lineTokens(line);
            assertArrayEquals("jetons de la ligne " + line,
                    expected.tokens(), actual.tokens());
            assertEquals("état de fin de la ligne " + line,
                    expected.endState(), actual.endState());
        }
        viaFill.detach();
        direct.detach();
    }

    /**
     * Le contrat d'annulation : un cliché pris avant une frappe ne
     * s'applique pas après — la révision observée a bougé. Un cliché
     * pris après s'applique.
     *
     * @throws IOException si le corpus ne se lit pas
     */
    @Test
    public void staleSliceIsRejectedOnRevisionChange() throws IOException {
        TextBuffer buffer = TextBuffer.create(corpus());
        UndoStack undo = new UndoStack(buffer);
        SyntaxDocument document = SyntaxDocument.open(buffer, Languages.JAVA);
        document.lineTokens(2);
        SyntaxDocument.FillSlice stale = document.takeFillSlice(500);
        assertNotNull(stale);
        SyntaxDocument.LineTokens[] entries = document.lexFillSlice(stale);

        try (TextTransaction transaction = undo.newTransaction()) {
            transaction.insert(buffer.getLineStart(0) + 2, "x");
        }

        assertEquals("un cliché périmé ne s'applique jamais",
                0, document.applyFill(stale, entries));
        assertEquals("le fond n'a rien écrit", 3, document.lexedLineCount());

        SyntaxDocument.FillSlice fresh = document.takeFillSlice(500);
        assertNotNull(fresh);
        assertTrue("un cliché neuf s'applique",
                document.applyFill(fresh, document.lexFillSlice(fresh)) > 0);
        assertEquals(503, document.lexedLineCount());
        document.detach();
    }

    /**
     * Le vrai fil tient des éditions simultanées : après trente
     * frappes croisées avec le remplissage, le document est exactement
     * celui d'un moteur neuf ouvert sur le même texte final — jeton
     * pour jeton, état pour état.
     *
     * @throws IOException si le corpus ne se lit pas
     */
    @Test
    public void concurrentEditsKeepTheDocumentExact() throws IOException {
        TextBuffer buffer = TextBuffer.create(corpus());
        UndoStack undo = new UndoStack(buffer);
        SyntaxDocument document = SyntaxDocument.open(buffer, Languages.JAVA);
        BackgroundFill fill = BackgroundFill.start(document);
        document.lineTokens(5);
        fill.kick();

        String[] replacements = {"x", "", "\n", "/*", "*/", "\n\n", "int"};
        Random random = new Random(20261124L);
        for (int round = 0; round < 30; round++) {
            int line = random.nextInt(200);
            while (buffer.getLineEnd(line) - buffer.getLineStart(line) < 4) {
                line = random.nextInt(200);
            }
            int at = buffer.getLineStart(line) + 1;
            try (TextTransaction transaction = undo.newTransaction()) {
                transaction.replace(at, at + 1,
                        replacements[random.nextInt(replacements.length)]);
            }
            fill.kick();
            if (round % 3 == 0) {
                document.lineTokens(100 + random.nextInt(300));
            }
        }
        assertTrue("le remplissage finit", fill.awaitIdle(30_000));
        fill.stop();

        TextBuffer reference = TextBuffer.create(buffer.getText(0, buffer.length()));
        SyntaxDocument direct = SyntaxDocument.open(reference, Languages.JAVA);
        assertEquals(direct.lineCount(), document.lineCount());
        for (int line = 0; line < direct.lineCount(); line++) {
            SyntaxDocument.LineTokens expected = direct.lineTokens(line);
            SyntaxDocument.LineTokens actual = document.lineTokens(line);
            assertArrayEquals("jetons de la ligne " + line,
                    expected.tokens(), actual.tokens());
            assertEquals("état de fin de la ligne " + line,
                    expected.endState(), actual.endState());
        }
        assertEquals(direct.tokenCount(), document.tokenCount());
        document.detach();
        direct.detach();
    }

    /**
     * Le vrai fil remplit le préfixe complet, sans aide du fil
     * d'appel.
     *
     * @throws IOException si le corpus ne se lit pas
     */
    @Test
    public void theRealThreadFillsThePrefix() throws IOException {
        TextBuffer buffer = TextBuffer.create(corpus());
        SyntaxDocument document = SyntaxDocument.open(buffer, Languages.JAVA);
        BackgroundFill fill = BackgroundFill.start(document);
        document.lineTokens(3);
        fill.kick();
        assertTrue(fill.awaitIdle(10_000));
        assertEquals(document.lineCount(), document.lexedLineCount());
        fill.stop();
        document.detach();
    }

    /**
     * Un fill arrêté ignore les relances : rien ne se lexe.
     *
     * @throws IOException si le corpus ne se lit pas
     * @throws InterruptedException si l'attente est interrompue
     */
    @Test
    public void stoppedFillIgnoresKicks() throws IOException, InterruptedException {
        TextBuffer buffer = TextBuffer.create(corpus());
        SyntaxDocument document = SyntaxDocument.open(buffer, Languages.JAVA);
        BackgroundFill fill = BackgroundFill.start(document);
        fill.stop();
        fill.kick();
        fill.kick();
        Thread.sleep(100L);
        assertEquals("un fill arrêté ignore les relances",
                0, document.lexedLineCount());
        assertFalse(fill.awaitIdle(50L));
        document.detach();
    }
}
