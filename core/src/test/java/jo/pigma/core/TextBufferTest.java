package jo.pigma.core;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThrows;

/** Tests du modèle de texte : lectures, lignes, bornes, écouteurs. */
public class TextBufferTest {

    private static final String SURROGATE_TEXT = "avant \uD83D\uDE00 après";

    /** Un tampon vide reste lisible : une ligne, longueur nulle. */
    @Test
    public void emptyBufferHasOneLine() {
        TextBuffer buffer = TextBuffer.create("");
        assertEquals(0, buffer.length());
        assertEquals(1, buffer.getLineCount());
        assertEquals(0, buffer.getLineStart(0));
        assertEquals(0, buffer.getLineEnd(0));
        assertEquals(0, buffer.getRevision());
    }

    /** Le texte initial est restitué tel quel, ligne par ligne. */
    @Test
    public void initialTextIsPreserved() {
        TextBuffer buffer = TextBuffer.create("ab\ncd\nef");
        assertEquals(8, buffer.length());
        assertEquals("ab\ncd\nef", buffer.toString());
        assertEquals(3, buffer.getLineCount());
        assertEquals(0, buffer.getLineStart(0));
        assertEquals(2, buffer.getLineEnd(0));
        assertEquals(3, buffer.getLineStart(1));
        assertEquals(5, buffer.getLineEnd(1));
        assertEquals(6, buffer.getLineStart(2));
        assertEquals(8, buffer.getLineEnd(2));
    }

    /** Un texte fini par un saut de ligne compte une dernière ligne vide. */
    @Test
    public void trailingNewlineCountsAnEmptyLine() {
        TextBuffer buffer = TextBuffer.create("a\n\n");
        assertEquals(3, buffer.getLineCount());
        assertEquals(0, buffer.getLineStart(0));
        assertEquals(1, buffer.getLineEnd(0));
        assertEquals(2, buffer.getLineStart(1));
        assertEquals(2, buffer.getLineEnd(1));
        assertEquals(3, buffer.getLineStart(2));
        assertEquals(3, buffer.getLineEnd(2));
    }

    /** Un retour chariot seul est un caractère ordinaire, pas une fin de ligne. */
    @Test
    public void carriageReturnIsNotALineBreak() {
        TextBuffer buffer = TextBuffer.create("a\r\nb");
        assertEquals(2, buffer.getLineCount());
        assertEquals("a\r", buffer.getText(0, 2));
        assertEquals("b", buffer.getText(3, 4));
    }

    /** Les bornes invalides lèvent une exception avec un message clair. */
    @Test
    public void invalidRangesAreRejected() {
        TextBuffer buffer = TextBuffer.create("abc");
        assertThrows(IndexOutOfBoundsException.class, () -> buffer.getText(1, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> buffer.getText(-1, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> buffer.getText(2, 4));
        assertThrows(IndexOutOfBoundsException.class, () -> buffer.charAt(3));
        assertThrows(IndexOutOfBoundsException.class, () -> buffer.lineOfOffset(4));
        assertThrows(IndexOutOfBoundsException.class, () -> buffer.getLineStart(1));
        assertThrows(IndexOutOfBoundsException.class, () -> buffer.offsetOfPosition(0, 4));
        assertThrows(IndexOutOfBoundsException.class, () -> buffer.offsetOfPosition(1, 0));
    }

    /** Les conversions offset vers position et retour font le tour complet. */
    @Test
    public void offsetAndPositionConvertBothWays() {
        TextBuffer buffer = TextBuffer.create("ab\ncd\nef");
        for (int offset = 0; offset <= buffer.length(); offset++) {
            TextPosition position = buffer.positionOfOffset(offset);
            assertEquals(offset, buffer.offsetOfPosition(position.line(), position.column()));
        }
        assertEquals(0, buffer.lineOfOffset(0));
        assertEquals(0, buffer.lineOfOffset(2));
        assertEquals(1, buffer.lineOfOffset(3));
        assertEquals(1, buffer.lineOfOffset(5));
        assertEquals(2, buffer.lineOfOffset(6));
        assertEquals(2, buffer.lineOfOffset(7));
        assertEquals(1, buffer.columnOfOffset(7));
    }

    /** Le remplacement direct (interne) maintient le texte et les lignes. */
    @Test
    public void directReplaceMaintainsTextAndLines() {
        TextBuffer buffer = TextBuffer.create("ab\ncd\nef");
        buffer.replace(3, 6, "XY\nZ");
        assertEquals("ab\nXY\nZef", buffer.toString());
        assertEquals(3, buffer.getLineCount());
        assertEquals(6, buffer.getLineStart(2));
        assertEquals(1, buffer.getRevision());
        buffer.replace(0, 0, "\n");
        assertEquals("\nab\nXY\nZef", buffer.toString());
        assertEquals(4, buffer.getLineCount());
        assertEquals(2, buffer.getRevision());
    }

    /** Une édition sans effet ne change ni la révision, ni les écouteurs. */
    @Test
    public void noOpReplaceChangesNothing() {
        TextBuffer buffer = TextBuffer.create("abc");
        List<int[]> events = new ArrayList<>();
        buffer.addListener((b, start, removed, inserted, revision) ->
                events.add(new int[] {start, removed, inserted, revision}));
        buffer.replace(1, 1, "");
        assertEquals(0, buffer.getRevision());
        assertEquals(0, events.size());
    }

    /** Chaque remplacement effectif notifie avec ses longueurs exactes. */
    @Test
    public void listenersAreNotifiedForEachEffectiveChange() {
        TextBuffer buffer = TextBuffer.create("hello");
        List<int[]> events = new ArrayList<>();
        TextListener listener = (b, start, removed, inserted, revision) ->
                events.add(new int[] {start, removed, inserted, revision});
        buffer.addListener(listener);
        buffer.replace(1, 3, "!!");
        buffer.replace(4, 4, "xyz");
        assertEquals(2, events.size());
        assertArrayEquals(new int[] {1, 2, 2, 1}, events.get(0));
        assertArrayEquals(new int[] {4, 0, 3, 2}, events.get(1));
        buffer.removeListener(listener);
        buffer.replace(0, 1, "H");
        assertEquals(3, buffer.getRevision());
        assertEquals(2, events.size());
    }

    /** Un écouteur retiré ne reçoit plus rien. */
    @Test
    public void removedListenerStopsReceivingEvents() {
        TextBuffer buffer = TextBuffer.create("a");
        List<Integer> revisions = new ArrayList<>();
        TextListener listener = (b, start, removed, inserted, revision) -> revisions.add(revision);
        buffer.addListener(listener);
        buffer.replace(0, 0, "b");
        buffer.removeListener(listener);
        buffer.replace(0, 0, "c");
        assertEquals(1, revisions.size());
    }

    /** De nombreuses insertions font croître le tampon sans perte. */
    @Test
    public void manyInsertsGrowTheBuffer() {
        TextBuffer buffer = TextBuffer.create("");
        StringBuilder expected = new StringBuilder();
        for (int i = 0; i < 400; i++) {
            String chunk = "ligne-" + i + "\n";
            int offset = (i % 3) * (expected.length() / 3 + 1);
            offset = Math.min(offset, expected.length());
            buffer.replace(offset, offset, chunk);
            expected.insert(offset, chunk);
        }
        assertEquals(expected.toString(), buffer.toString());
        assertEquals(401, buffer.getLineCount());
    }

    /** Les paires de substitution traversent les éditions sans dommage. */
    @Test
    public void surrogatePairsSurviveEdits() {
        TextBuffer buffer = TextBuffer.create(SURROGATE_TEXT);
        buffer.replace(6, 6, "x");
        assertEquals("avant x\uD83D\uDE00 après", buffer.toString());
        buffer.replace(7, 9, "");
        assertEquals("avant x après", buffer.toString());
        buffer.replace(6, 7, "\uD83D\uDE00");
        assertEquals(SURROGATE_TEXT, buffer.toString());
    }

    /** charAt lit à travers le trou dans les deux sens. */
    @Test
    public void charAtReadsAcrossTheGap() {
        TextBuffer buffer = TextBuffer.create("abcdef");
        buffer.replace(2, 4, "XY");
        assertEquals("abXYef", buffer.toString());
        assertEquals('a', buffer.charAt(0));
        assertEquals('X', buffer.charAt(2));
        assertEquals('Y', buffer.charAt(3));
        assertEquals('e', buffer.charAt(4));
        buffer.replace(0, 0, "Z");
        assertEquals("ZabXYef", buffer.toString());
        assertEquals('Z', buffer.charAt(0));
        assertEquals('f', buffer.charAt(6));
    }

    /**
     * L'ordre document-avant-interface : un écouteur en tête court
     * avant la queue, MÊME enregistré après elle (un document ré-ouvert
     * au changement de langage) — sinon un lecteur d'interface lit des
     * jetons périmés au beau milieu du remplacement.
     */
    @Test
    public void frontListenersRunBeforeTheQueueEvenWhenRegisteredLater() {
        TextBuffer buffer = TextBuffer.create("abc");
        List<String> order = new ArrayList<>();
        TextListener back = (b, start, removed, inserted, revision) -> order.add("queue");
        buffer.addListener(back);
        // le document s'enregistre APRÈS l'écouteur d'interface (le
        // scénario du ré-enregistrement) : il court pourtant en premier
        TextListener front = (b, start, removed, inserted, revision) -> order.add("tête");
        buffer.addListenerFront(front);
        buffer.replace(0, 0, "\n");
        assertEquals(List.of("tête", "queue"), order);
        // le retrait fait son œuvre des deux listes
        buffer.removeListener(front);
        order.clear();
        buffer.replace(0, 0, "\n");
        assertEquals(List.of("queue"), order);
        buffer.removeListener(back);
        order.clear();
        buffer.replace(0, 0, "\n");
        assertEquals(List.of(), order);
    }

    /** Oublier l'histoire : plus rien d'annulable ni de rétablissable. */
    @Test
    public void clearForgetsUndoAndRedo() {
        TextBuffer buffer = TextBuffer.create("abc");
        UndoStack stack = new UndoStack(buffer);
        try (TextTransaction transaction = stack.newTransaction()) {
            transaction.replace(3, 3, "def");
        }
        stack.clear();
        assertFalse(stack.canUndo());
        assertFalse(stack.canRedo());
        // une nouvelle édition après l'oubli redevient annulable
        try (TextTransaction transaction = stack.newTransaction()) {
            transaction.replace(0, 0, "x");
        }
        assertTrue(stack.canUndo());
    }
}
