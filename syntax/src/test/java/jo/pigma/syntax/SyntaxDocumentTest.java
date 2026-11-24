package jo.pigma.syntax;

import org.junit.Test;

import jo.pigma.core.TextBuffer;
import jo.pigma.core.UndoStack;
import jo.pigma.syntax.lexers.generated.JavaLexer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Le moteur de coloration : premier lexage à la demande, règle d'arrêt
 * à l'édition (propagation d'un commentaire de bloc ouvert, arrêt à la
 * stabilisation de l'état), révision par ligne, queue épargnée,
 * annulation suivie, restructuration multi-lignes.
 */
public class SyntaxDocumentTest {

    private static final String SAMPLE = """
            int a = 1;
            /* debut
            suite
            fin */ int b = 2;
            int c = 3;
            """;

    /**
     * Le premier appel lexte tout : jetons, états enchaînés, révision
     * courante.
     */
    @Test
    public void firstPassTokenizesEverything() {
        TextBuffer buffer = TextBuffer.create(SAMPLE);
        SyntaxDocument document = SyntaxDocument.open(buffer, Languages.JAVA);
        SyntaxDocument.LineTokens first = document.lineTokens(0);
        assertEquals(buffer.getRevision(), first.revision());
        // ligne 0 : « int a = 1; » — mot, blanc, identifiant, blanc,
        // opérateur, blanc, nombre, ponctuation.
        assertEquals(8, first.tokenCount());
        assertEquals("keyword", first.scopeAt(0).name());
        assertNull(first.scopeAt(1));
        // ligne 1 ouvre le commentaire : la ligne 2 s'y chaîne
        assertEquals(JavaLexer.BLOCK, document.lineTokens(1).endState());
        assertEquals(JavaLexer.BLOCK, document.lineTokens(2).endState());
        assertEquals(JavaLexer.YYINITIAL, document.lineTokens(3).endState());
        // le bloc se termine par un saut de ligne : une ligne vide suit
        assertEquals(6, document.lineCount());
        document.detach();
    }

    /**
     * Un commentaire de bloc ouvert par édition se propage jusqu'au
     * bout : chaque ligne relue devient commentaire.
     */
    @Test
    public void openedBlockCommentPropagates() {
        TextBuffer buffer = TextBuffer.create("int a;\nint b;\nint c;");
        UndoStack undo = new UndoStack(buffer);
        SyntaxDocument document = SyntaxDocument.open(buffer, Languages.JAVA);
        // toute la région est déjà calculée : la propagation relit tout
        document.lineTokens(2);
        edit(undo, 0, 0, "/*");
        assertEquals(3, document.lastRelexedLines());
        for (int line = 0; line < 3; line++) {
            SyntaxDocument.LineTokens entry = document.lineTokens(line);
            for (int at = 0; at < entry.tokenCount(); at++) {
                assertEquals("ligne " + line, "comment", entry.scopeAt(at).name());
            }
        }
        // fermeture en fin de fichier : seule la ligne finale relit
        edit(undo, buffer.length(), buffer.length(), "*/");
        assertEquals(1, document.lastRelexedLines());
        assertEquals(0, document.lineTokens(2).endState());
        SyntaxDocument.LineTokens closed = document.lineTokens(2);
        assertEquals("comment", closed.scopeAt(closed.tokenCount() - 1).name());
        document.detach();
    }

    /**
     * Un commentaire ouvert dans une fenêtre paresseuse : seule la
     * ligne éditée relit, la suite s'enchaîne à la demande depuis le
     * point de contrôle posé à la frontière.
     */
    @Test
    public void openedCommentPropagatesLazily() {
        TextBuffer buffer = TextBuffer.create("int a;\nint b;\nint c;");
        UndoStack undo = new UndoStack(buffer);
        SyntaxDocument document = SyntaxDocument.open(buffer, Languages.JAVA);
        // seule la ligne 0 est demandée : le reste est à la demande
        document.lineTokens(0);
        edit(undo, 0, 0, "/*");
        assertEquals(1, document.lastRelexedLines());
        // les lignes suivantes se colorent en commentaire en enchaînant
        for (int line = 0; line < 3; line++) {
            SyntaxDocument.LineTokens entry = document.lineTokens(line);
            assertTrue("ligne " + line + " vide", entry.tokenCount() > 0);
            for (int at = 0; at < entry.tokenCount(); at++) {
                assertEquals("ligne " + line, "comment", entry.scopeAt(at).name());
            }
        }
        assertEquals(3, document.lexedLineCount());
        document.detach();
    }

    /**
     * La queue au-delà de la stabilisation garde ses jetons et sa
     * révision : la règle d'arrêt ne relit que le nécessaire.
     */
    @Test
    public void tailBeyondStopKeepsRevision() {
        TextBuffer buffer = TextBuffer.create(SAMPLE);
        UndoStack undo = new UndoStack(buffer);
        SyntaxDocument document = SyntaxDocument.open(buffer, Languages.JAVA);
        document.lineTokens(0);
        SyntaxDocument.LineTokens tail = document.lineTokens(4);
        int tailRevision = tail.revision();
        // renommage dans la ligne 0 sans changement d'état de fin
        edit(undo, 4, 5, "z");
        assertEquals(tailRevision, document.lineTokens(4).revision());
        assertEquals(tokenTexts(tail), tokenTexts(document.lineTokens(4)));
        assertEquals(1, document.lastRelexedLines());
        document.detach();
    }

    /**
     * Une insertion multi-lignes restructure les entrées : les lignes
     * ajoutées se lexent, la queue glisse sans être relue.
     */
    @Test
    public void multiLineInsertRestructures() {
        TextBuffer buffer = TextBuffer.create("a\nb\nc");
        UndoStack undo = new UndoStack(buffer);
        SyntaxDocument document = SyntaxDocument.open(buffer, Languages.SQL);
        document.lineTokens(0);
        edit(undo, 2, 2, "x\ny\n");
        assertEquals(5, document.lineCount());
        // ligne 0 : « a » seul, identifiant neutre
        assertNull(document.lineTokens(0).scopeAt(0));
        // la queue (c) garde ses jetons : identifiant neutre aussi
        assertNull(document.lineTokens(4).scopeAt(0));
        document.detach();
    }

    /** L'annulation rexée par l'écouteur rend les jetons d'avant. */
    @Test
    public void undoRestoresTokens() {
        TextBuffer buffer = TextBuffer.create("int a;\n");
        UndoStack undo = new UndoStack(buffer);
        SyntaxDocument document = SyntaxDocument.open(buffer, Languages.JAVA);
        SyntaxDocument.LineTokens before = document.lineTokens(0);
        edit(undo, 4, 5, "/*");
        // le commentaire ouvert s'étend au reste de la ligne
        SyntaxDocument.LineTokens after = document.lineTokens(0);
        assertEquals("comment", after.scopeAt(after.tokenCount() - 1).name());
        undo.undo();
        assertEquals(tokenTexts(before), tokenTexts(document.lineTokens(0)));
        document.detach();
    }

    /** Le lexeur générique (SQL) sert le moteur comme les générés. */
    @Test
    public void genericLanguageServesEngine() {
        TextBuffer buffer = TextBuffer.create("SELECT nom\nFROM usagers\n");
        SyntaxDocument document = SyntaxDocument.open(buffer, Languages.SQL);
        SyntaxDocument.LineTokens entry = document.lineTokens(0);
        assertEquals("keyword", entry.scopeAt(0).name());
        assertNull(entry.scopeAt(2));
        document.detach();
    }

    private static void edit(UndoStack undo, int start, int end, String text) {
        try (var transaction = undo.newTransaction()) {
            transaction.replace(start, end, text);
        }
    }

    private static String tokenTexts(SyntaxDocument.LineTokens entry) {
        StringBuilder text = new StringBuilder();
        for (int at = 0; at < entry.tokenCount(); at++) {
            text.append(entry.typeAt(at)).append(':').append(entry.startAt(at))
                    .append('-').append(entry.endAt(at)).append(' ');
        }
        return text.toString();
    }
}
