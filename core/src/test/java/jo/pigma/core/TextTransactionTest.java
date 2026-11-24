package jo.pigma.core;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/** Tests des transactions : engagement, retour arrière, imbrication. */
public class TextTransactionTest {

    /** Une transaction engagée s'annule en une seule opération. */
    @Test
    public void committedTransactionIsUndoableAsOneUnit() {
        TextBuffer buffer = TextBuffer.create("abcdef");
        UndoStack stack = new UndoStack(buffer);
        try (TextTransaction transaction = stack.newTransaction()) {
            transaction.replace(0, 0, "1");
            transaction.replace(1, 3, "23");
            transaction.delete(4, 5);
        }
        assertEquals("123cef", buffer.toString());
        assertTrue(stack.canUndo());
        stack.undo();
        assertEquals("abcdef", buffer.toString());
    }

    /** Le retour arrière rétablit le texte exact d'avant la transaction. */
    @Test
    public void rollbackRestoresTheExactPriorText() {
        TextBuffer buffer = TextBuffer.create("bonjour");
        UndoStack stack = new UndoStack(buffer);
        TextTransaction transaction = stack.newTransaction();
        transaction.replace(0, 3, "sal");
        transaction.insert(7, "!");
        transaction.delete(1, 2);
        transaction.rollback();
        assertEquals("bonjour", buffer.toString());
        assertFalse(stack.canUndo());
        assertEquals(6, buffer.getRevision());
    }

    /** Une transaction fermée refuse toute nouvelle édition. */
    @Test
    public void closedTransactionRejectsEdits() {
        TextBuffer buffer = TextBuffer.create("a");
        UndoStack stack = new UndoStack(buffer);
        TextTransaction transaction = stack.newTransaction();
        transaction.insert(0, "b");
        transaction.commit();
        assertThrows(IllegalStateException.class, () -> transaction.insert(0, "c"));
    }

    /** Une seule transaction peut être ouverte à la fois. */
    @Test
    public void secondTransactionIsRejectedWhileOneIsOpen() {
        TextBuffer buffer = TextBuffer.create("a");
        UndoStack stack = new UndoStack(buffer);
        TextTransaction first = stack.newTransaction();
        assertThrows(IllegalStateException.class, stack::newTransaction);
        first.rollback();
        stack.newTransaction().commit();
    }

    /** Annuler pendant une transaction ouverte est interdit. */
    @Test
    public void undoIsRejectedWhileTransactionIsOpen() {
        TextBuffer buffer = TextBuffer.create("a");
        UndoStack stack = new UndoStack(buffer);
        try (TextTransaction transaction = stack.newTransaction()) {
            transaction.insert(0, "b");
            assertThrows(IllegalStateException.class, stack::undo);
        }
        stack.undo();
        assertEquals("a", buffer.toString());
    }

    /** Une édition aux bornes invalides n'enregistre rien et ne casse rien. */
    @Test
    public void invalidEditLeavesTransactionUsable() {
        TextBuffer buffer = TextBuffer.create("abc");
        UndoStack stack = new UndoStack(buffer);
        TextTransaction transaction = stack.newTransaction();
        assertThrows(IndexOutOfBoundsException.class, () -> transaction.insert(9, "x"));
        transaction.insert(3, "d");
        transaction.commit();
        assertEquals("abcd", buffer.toString());
        stack.undo();
        assertEquals("abc", buffer.toString());
    }

    /** Le try-with-resources engage à la fermeture, même après exception. */
    @Test
    public void closeCommitsEvenAfterException() throws Exception {
        TextBuffer buffer = TextBuffer.create("a");
        UndoStack stack = new UndoStack(buffer);
        try (TextTransaction transaction = stack.newTransaction()) {
            transaction.insert(1, "b");
            throw new IllegalStateException("panique du bloc try");
        } catch (IllegalStateException expected) {
            // le close() du try-with-resources a déjà engagé la transaction
        }
        assertEquals("ab", buffer.toString());
        assertTrue(stack.canUndo());
    }

    /** Une transaction sans édition effective n'engage aucune unité. */
    @Test
    public void emptyTransactionCommitsNothing() {
        TextBuffer buffer = TextBuffer.create("a");
        UndoStack stack = new UndoStack(buffer);
        try (TextTransaction transaction = stack.newTransaction()) {
            transaction.replace(0, 0, "");
        }
        assertFalse(stack.canUndo());
        assertEquals(0, buffer.getRevision());
    }

    /** L'annulation puis le rétablissement font un aller-retour exact. */
    @Test
    public void undoAndRedoRoundTrip() {
        TextBuffer buffer = TextBuffer.create("un\ndeux");
        UndoStack stack = new UndoStack(buffer);
        try (TextTransaction transaction = stack.newTransaction()) {
            transaction.replace(0, 5, "UN");
            transaction.insert(2, "!");
        }
        assertEquals("UN!ux", buffer.toString());
        stack.undo();
        assertEquals("un\ndeux", buffer.toString());
        stack.redo();
        assertEquals("UN!ux", buffer.toString());
    }
}
