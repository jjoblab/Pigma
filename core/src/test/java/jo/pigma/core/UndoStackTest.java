package jo.pigma.core;

import org.junit.Test;

import java.util.List;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/** Tests de la pile d'annulation : états, capacité, branches. */
public class UndoStackTest {

    /** Les indicateurs d'état suivent les annulations et rétablissements. */
    @Test
    public void canUndoAndCanRedoTrackOperations() {
        TextBuffer buffer = TextBuffer.create("a");
        UndoStack stack = new UndoStack(buffer);
        assertFalse(stack.canUndo());
        assertFalse(stack.canRedo());
        apply(stack, tx -> tx.insert(1, "b"));
        assertTrue(stack.canUndo());
        assertFalse(stack.canRedo());
        stack.undo();
        assertFalse(stack.canUndo());
        assertTrue(stack.canRedo());
        stack.redo();
        assertTrue(stack.canUndo());
        assertFalse(stack.canRedo());
        assertEquals("ab", buffer.toString());
    }

    /** Une nouvelle édition efface la branche de rétablissement. */
    @Test
    public void freshEditClearsTheRedoBranch() {
        TextBuffer buffer = TextBuffer.create("a");
        UndoStack stack = new UndoStack(buffer);
        apply(stack, tx -> tx.insert(1, "b"));
        apply(stack, tx -> tx.insert(2, "c"));
        stack.undo();
        assertTrue(stack.canRedo());
        apply(stack, tx -> tx.insert(2, "Z"));
        assertFalse(stack.canRedo());
        assertEquals("abZ", buffer.toString());
    }

    /** L'annulation décrit les changements appliqués au tampon. */
    @Test
    public void undoReturnsTheAppliedChanges() {
        TextBuffer buffer = TextBuffer.create("abcdef");
        UndoStack stack = new UndoStack(buffer);
        try (TextTransaction transaction = stack.newTransaction()) {
            transaction.replace(1, 3, "X");
            transaction.insert(0, "Y");
        }
        List<TextChange> undone = stack.undo();
        assertEquals(2, undone.size());
        assertEquals(0, undone.get(0).start());
        assertEquals(1, undone.get(0).end());
        assertEquals("", undone.get(0).text());
        assertEquals(1, undone.get(1).start());
        assertEquals(2, undone.get(1).end());
        assertEquals("bc", undone.get(1).text());
        assertEquals("abcdef", buffer.toString());
        List<TextChange> redone = stack.redo();
        assertEquals(2, redone.size());
        assertEquals("X", redone.get(0).text());
        assertEquals("Y", redone.get(1).text());
        assertEquals("YaXdef", buffer.toString());
    }

    /** Une pile bornée oublie les unités les plus anciennes. */
    @Test
    public void boundedStackForgetsOldestUnits() {
        TextBuffer buffer = TextBuffer.create("");
        UndoStack stack = new UndoStack(buffer, 3);
        for (int i = 0; i < 5; i++) {
            apply(stack, tx -> tx.insert(0, "x"));
        }
        assertEquals("xxxxx", buffer.toString());
        for (int i = 0; i < 3; i++) {
            assertTrue(stack.canUndo());
            stack.undo();
        }
        assertEquals("xx", buffer.toString());
        assertFalse(stack.canUndo());
    }

    /** La capacité minimale est 1 ; en dessous, refus. */
    @Test
    public void limitBelowOneIsRejected() {
        TextBuffer buffer = TextBuffer.create("a");
        UndoStack stack = new UndoStack(buffer);
        assertThrows(IllegalArgumentException.class, () -> stack.setLimit(0));
        assertThrows(IllegalArgumentException.class, () -> new UndoStack(buffer, 0));
        assertEquals(256, stack.getLimit());
    }

    /** Réduire la capacité oublie aussitôt les unités les plus anciennes. */
    @Test
    public void shrinkingTheLimitTrimsOldUnits() {
        TextBuffer buffer = TextBuffer.create("");
        UndoStack stack = new UndoStack(buffer);
        for (int i = 0; i < 5; i++) {
            apply(stack, tx -> tx.insert(0, "x"));
        }
        stack.setLimit(2);
        stack.undo();
        stack.undo();
        assertFalse(stack.canUndo());
        assertEquals("xxx", buffer.toString());
    }

    /** Annuler et rétablir sur pile vide ne lèvent pas. */
    @Test
    public void emptyOperationsAreSafe() {
        TextBuffer buffer = TextBuffer.create("a");
        UndoStack stack = new UndoStack(buffer);
        assertTrue(stack.undo().isEmpty());
        assertTrue(stack.redo().isEmpty());
        assertEquals("a", buffer.toString());
        assertEquals(0, buffer.getRevision());
    }

    /** Ouvre une transaction, applique l'édition et l'engage. */
    private static void apply(UndoStack stack, Consumer<TextTransaction> edit) {
        try (TextTransaction transaction = stack.newTransaction()) {
            edit.accept(transaction);
        }
    }
}
