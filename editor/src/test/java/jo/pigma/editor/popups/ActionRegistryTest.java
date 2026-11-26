package jo.pigma.editor.popups;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Le registre des actions (JVM, sans Android) : une SEULE définition
 * par action (le libellé, la course, la disponibilité), les deux
 * cartes y mènent leurs rangées — la liste VIVE ne porte que les
 * disponibles, l'ordre d'ajout y demeure, la dérive des deux listes
 * d'avant n'y peut plus vivre (une action ajoutée au registre PARAÎT
 * aux DEUX cartes d'un coup).
 */
public final class ActionRegistryTest {

    /** Le drapeau d'un essai (la disponibilité se retourne). */
    private boolean flag;

    /** Les courses jouées (les libellés y tombent). */
    private final List<String> played = new ArrayList<>();

    @Test
    public void liveListsOnlyTheAvailable() {
        ActionRegistry registry = new ActionRegistry()
                .add("Commenter / décommenter", () -> played.add("commenter"),
                        () -> flag)
                .add("En majuscules", () -> played.add("majuscules"),
                        () -> true)
                .add("En minuscules", () -> played.add("minuscules"),
                        () -> flag)
                .add("Dupliquer la ligne", () -> played.add("dupliquer"),
                        () -> true)
                .add("Supprimer la ligne", () -> played.add("supprimer"),
                        () -> true);
        // la sélection vit : les cinq se montrent, à l'ordre d'ajout
        flag = true;
        List<ActionRegistry.Action> withSelection = registry.live();
        assertEquals(5, withSelection.size());
        assertEquals("Commenter / décommenter", withSelection.get(0).label());
        assertEquals("En majuscules", withSelection.get(1).label());
        assertEquals("En minuscules", withSelection.get(2).label());
        assertEquals("Dupliquer la ligne", withSelection.get(3).label());
        assertEquals("Supprimer la ligne", withSelection.get(4).label());
        // la sélection meurt : la casse se cache (le commentaire suit
        // son langage — le drapeau y tient lieu)
        flag = false;
        List<ActionRegistry.Action> withoutSelection = registry.live();
        assertEquals(3, withoutSelection.size());
        assertEquals("En majuscules", withoutSelection.get(0).label());
        assertEquals("Dupliquer la ligne", withoutSelection.get(1).label());
        assertEquals("Supprimer la ligne", withoutSelection.get(2).label());
    }

    @Test
    public void theTwoCardsReadTheSameRun() {
        ActionRegistry registry = new ActionRegistry()
                .add("Dupliquer la ligne", () -> played.add("dupliquer"),
                        () -> true)
                .add("En majuscules", () -> played.add("majuscules"),
                        () -> flag);
        // la carte d'actions rapides mène SES rangées, l'étage du
        // débordement les SIENNES — la course est LA MÊME (le
        // registre la tient, les deux listes ne vivent plus)
        List<ActionRegistry.Action> live = registry.live();
        List<String> quickRows = new ArrayList<>();
        for (ActionRegistry.Action action : live) {
            quickRows.add(action.label());
        }
        List<String> overflowRows = new ArrayList<>();
        for (ActionRegistry.Action action : registry.live()) {
            overflowRows.add(action.label());
        }
        assertEquals(quickRows, overflowRows);
        // la course se joue UNE fois par rangée — les deux cartes y
        // tombent ensemble
        for (ActionRegistry.Action action : live) {
            action.run();
        }
        assertTrue(played.contains("dupliquer"));
        // la course morte ne se joue pas (la rangée n'y vit pas) :
        // la casse absente du registre vivant ne court pas
        played.clear();
        flag = false;
        for (ActionRegistry.Action action : registry.live()) {
            action.run();
        }
        assertEquals("la course morte ne se joue pas",
                List.of("dupliquer"), played);
    }

    @Test
    public void malformedRegistrationsIgnore() {
        ActionRegistry registry = new ActionRegistry()
                .add(null, () -> { }, () -> true)
                .add("", () -> { }, () -> true)
                .add("Sans course", null, () -> true)
                .add("Sans disponibilité", () -> { }, null)
                .add("La bonne", () -> played.add("bonne"), () -> true);
        List<ActionRegistry.Action> live = registry.live();
        assertEquals(1, live.size());
        assertEquals("La bonne", live.get(0).label());
    }
}
