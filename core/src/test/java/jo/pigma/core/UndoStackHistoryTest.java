package jo.pigma.core;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * L'historique transportable de la pile (A3) : l'aller-retour
 * export/import rend les unités annulables et rétablissables dans
 * leur ORDRE DE PILE — l'hôte à onglets y porte l'annulation d'un
 * document à l'autre, la pile d'un document n'est pas celle d'un
 * autre.
 */
public class UndoStackHistoryTest {

    /**
     * L'ALLER-RETOUR : deux unités engagées, l'une annulée — l'état
     * exporté porte une unité à annuler et une à rétablir ; la pile
     * neuve d'un AUTRE tampon (au texte de l'état) la rejoue pareil :
     * l'annulation et le rétablissement y recréent le texte original.
     */
    @Test
    public void theRoundTripKeepsTheUnitsInOrder() {
        TextBuffer buffer = TextBuffer.create("abcdef");
        UndoStack stack = new UndoStack(buffer);
        try (TextTransaction first = stack.newTransaction()) {
            first.replace(0, 1, "X");
        }
        try (TextTransaction second = stack.newTransaction()) {
            second.replace(3, 4, "Y");
        }
        assertEquals("XbcYef", buffer.toString());
        stack.undo();
        assertEquals("Xbcdef", buffer.toString());
        UndoStack.History history = stack.exportHistory();
        assertTrue("une unité reste à annuler", !history.undoable().isEmpty());
        assertTrue("une unité reste à rétablir", !history.redoable().isEmpty());

        // LA PILE NEUVE d'un autre document (au texte de l'état
        // exporté — les unités transportées parlent de SES offsets) :
        // l'import rend l'historique ENTIER
        TextBuffer other = TextBuffer.create("Xbcdef");
        UndoStack imported = new UndoStack(other);
        assertFalse("la pile neuve n'a rien à annuler", imported.canUndo());
        imported.importHistory(history);
        assertTrue("l'import rend l'annulable", imported.canUndo());
        assertTrue("l'import rend le rétablissable", imported.canRedo());
        // l'annulation rejoue l'INVERSE de l'unité annulable : le
        // texte d'avant revient
        imported.undo();
        assertEquals("abcdef", other.toString());
        // le rétablissement rejoue l'unité rétablissable : le texte
        // d'après revient
        imported.redo();
        assertEquals("Xbcdef", other.toString());
        // l'unité revenue se défait encore (la pile vit son cycle)
        assertTrue("l'unité revenue s'annule encore", imported.canUndo());
        imported.undo();
        assertEquals("abcdef", other.toString());
    }

    /**
     * L'historique NUL vide tout — et l'annulation d'un document
     * n'est pas celle d'un autre : l'import REMPLACE, il n'ajoute
     * pas (l'unité propre de la pile importée n'y survit pas).
     */
    @Test
    public void theImportReplacesNotAppends() {
        TextBuffer buffer = TextBuffer.create("abcdef");
        UndoStack stack = new UndoStack(buffer);
        try (TextTransaction unit = stack.newTransaction()) {
            unit.replace(0, 1, "X");
        }
        UndoStack.History history = stack.exportHistory();
        // la pile qui importe vit au texte OÙ l'unité transportée
        // s'applique (les offsets transportés parlent de lui) et
        // porte sa propre unité
        TextBuffer other = TextBuffer.create("Xbcdef");
        UndoStack imported = new UndoStack(other);
        try (TextTransaction own = imported.newTransaction()) {
            own.replace(5, 6, "Z");
        }
        assertEquals("XbcdeZ", other.toString());
        // l'import REMPLACE l'historique propre : défaire rejoue
        // l'INVERSE de l'unité TRANSPORTÉE (le « a » revient au
        // début) — si l'unité propre avait survécu dessous, défaire
        // l'aurait rejouée AVANT (le « Z » serait parti d'abord)
        imported.importHistory(history);
        imported.undo();
        assertEquals("abcdeZ", other.toString());
        assertFalse("l'unité transportée unique s'épuise",
                imported.canUndo());
        // le NUL vide tout
        imported.importHistory(null);
        assertFalse("l'historique nul vide la pile", imported.canUndo());
        assertFalse("le rétablissable s'y vide aussi", imported.canRedo());
    }
}
