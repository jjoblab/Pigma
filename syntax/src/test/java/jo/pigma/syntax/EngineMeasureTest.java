package jo.pigma.syntax;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;

import jo.pigma.core.TextBuffer;
import jo.pigma.core.UndoStack;

import static org.junit.Assert.assertTrue;

/**
 * Mesures du moteur sur le corpus de 2 000 lignes, imprimées (pas
 * d'attente chiffrée : elles dépendent de la machine) : premier lexage
 * complet, coût d'une frappe ordinaire (une ligne relue) et d'une
 * frappe qui propage un état (commentaire ouvert). Ces nombres
 * alimentent {@code docs/perf.md} côté JVM ; l'appareil les relèvera
 * avec le panneau de diagnostic du sample.
 */
public class EngineMeasureTest {

    /**
     * Impression des durées du moteur sur le corpus Java.
     *
     * @throws IOException si le corpus ne se lit pas
     */
    @Test
    public void printEngineTimings() throws IOException {
        Path corpus = LexersGoldenTest.resource("data/java-2000.java");
        String text = Files.readString(corpus, StandardCharsets.UTF_8);
        TextBuffer buffer = TextBuffer.create(text);
        UndoStack undo = new UndoStack(buffer);
        SyntaxDocument document = SyntaxDocument.open(buffer, Languages.JAVA);
        int tokens = document.tokenCount();
        long firstPass = document.firstLexNanos();
        assertTrue(tokens > 20_000);

        edit(undo, 32, 33, "z");
        long ordinary = document.lastEditNanos();
        int ordinaryLines = document.lastRelexedLines();

        int openAt = text.indexOf("/*", text.indexOf("/**") + 1);
        int closeAt = text.indexOf("*/", openAt);
        edit(undo, closeAt, closeAt + 2, "  ");
        long reopening = document.lastEditNanos();
        int reopeningLines = document.lastRelexedLines();

        System.out.printf(
                "MESURE moteur java-2000 : %d lignes, %d jetons, premier lexage %.1f ms, "
                        + "frappe ordinaire %.2f ms (%d ligne relue), "
                        + "frappe propagée %.2f ms (%d lignes relues)%n",
                document.lineCount(), tokens, firstPass / 1_000_000.0,
                ordinary / 1_000_000.0, ordinaryLines,
                reopening / 1_000_000.0, reopeningLines);
        document.detach();
    }

    private static void edit(UndoStack undo, int start, int end, String replacement) {
        try (var transaction = undo.newTransaction()) {
            transaction.replace(start, end, replacement);
        }
    }
}
