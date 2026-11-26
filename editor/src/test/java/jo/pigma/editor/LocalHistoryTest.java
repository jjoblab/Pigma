package jo.pigma.editor;

import org.junit.Test;

import jo.pigma.editor.popups.LocalHistory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * L'historique local repensé (B7) : les versions par geste — la
 * frappe s'y regroupe par pause, la plage porte la région occupée
 * après le geste, le budget se compte en caractères archivés.
 */
public class LocalHistoryTest {

    /** La frappe d'une phrase tient UNE version (la pause la ferme). */
    @Test
    public void typingGroupsIntoOneVersionPerPause() {
        LocalHistory history = new LocalHistory();
        long now = 1_000_000L;
        // trois touches rapprochées : UNE version étendue
        history.record(now, null, 0, 10, 11, "");
        history.record(now + 100L, null, 0, 11, 12, "");
        history.record(now + 200L, null, 0, 12, 13, "");
        assertEquals("la phrase groupée tient une version", 1, history.size());
        // la pause écoulée : la touche suivante ouvre sa version
        history.record(now + 3_000L, null, 0, 13, 14, "");
        assertEquals("la pause ferme la version", 2, history.size());
        // un geste étiqueté (collage) vit sa version à soi, sans regroupement
        history.record(now + 3_100L, "collage", 0, 20, 32, "l'ancien");
        history.record(now + 3_150L, null, 0, 32, 33, "");
        assertEquals("le geste étiqueté ne se groupe pas à la frappe",
                4, history.size());
    }

    /** La version étendue porte la réunion des plages touchées. */
    @Test
    public void extendedVersionCoversTheUnion() {
        LocalHistory history = new LocalHistory();
        long now = 1_000_000L;
        history.record(now, null, 0, 10, 11, "");
        history.record(now + 100L, null, 0, 11, 12, "");
        LocalHistory.Version version = history.chronology().get(0);
        assertEquals("la réunion commence à la première touche",
                10, version.start());
        assertEquals("la réunion finit à la dernière", 12, version.end());
    }

    /** Le budget en caractères évince les plus anciennes versions. */
    @Test
    public void budgetEvictsTheOldest() {
        LocalHistory history = new LocalHistory();
        long now = 1_000_000L;
        // des gestes étiquetés (aucun regroupement) : chacun porte un
        // gros texte d'avant — le budget de 32 768 caractères évince
        // les anciennes quand il se dépasse
        String chunk = "x".repeat(9_000);
        for (int at = 0; at < 10; at++) {
            history.record(now + at * 10_000L, "collage", 0,
                    at * 100, at * 100 + 10, chunk);
        }
        assertTrue("le budget évince (moins de 10 versions)",
                history.size() < 10);
        // les plus récentes demeurent
        assertTrue(history.size() >= 3);
    }

    /** La lecture par plage de lignes trie du dernier au premier. */
    @Test
    public void betweenReadsLatestFirst() {
        LocalHistory history = new LocalHistory();
        long now = 1_000_000L;
        history.record(now, "collage", 5, 100, 110, "a");
        history.record(now + 10L, "collage", 5, 200, 210, "b");
        history.record(now + 20L, "collage", 7, 300, 310, "c");
        java.util.List<LocalHistory.Version> found = history.between(5, 5);
        assertEquals("la plage garde les deux versions de la ligne 5",
                2, found.size());
        assertEquals("la dernière d'abord", "b", found.get(0).before());
        assertEquals("la première ensuite", "a", found.get(1).before());
    }
}
