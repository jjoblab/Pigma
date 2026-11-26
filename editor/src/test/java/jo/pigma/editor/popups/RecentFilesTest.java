package jo.pigma.editor.popups;

import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * LA MÉMOIRE DES FICHIERS RÉCENTS aux JVM pures : le fichier se pose
 * (la même consécutive ne se duplique pas), l'emplacement suit (le
 * même fichier ET la même ligne consécutifs ne se dupliquent pas), le
 * plafond tient, l'ancienneté se lit.
 */
public class RecentFilesTest {

    /** La mémoire éprouvée. */
    private final RecentFiles recents = new RecentFiles();

    /** Le fichier se pose, la même consécutive ne se duplique pas. */
    @Test
    public void theFileLandsAtTheTop() {
        recents.recordFile("Java (démo)", 1_000L, () -> { });
        recents.recordFile("Lua (démo)", 2_000L, () -> { });
        assertEquals("deux fichiers vivrent", 2, recents.files().size());
        assertEquals("le plus récent au sommet", "Lua (démo)",
                recents.files().get(0).name());
        // la même consécutive ne se duplique pas (son heure se rafraîchit)
        recents.recordFile("Lua (démo)", 5_000L, () -> { });
        assertEquals("la même consécutive ne se duplique pas", 2,
                recents.files().size());
        assertEquals("son heure se rafraîchit", 5_000L,
                recents.files().get(0).when());
        // le fichier nul ou sans retour ne se pose pas
        recents.recordFile(null, 6_000L, () -> { });
        recents.recordFile("", 6_000L, () -> { });
        recents.recordFile("Seul", 6_000L, null);
        assertEquals("le fichier sans retour ne se pose pas", 2,
                recents.files().size());
    }

    /** L'emplacement se pose, la même consécutive ne se duplique pas. */
    @Test
    public void theLocationLandsAtTheTop() {
        recents.recordLocation("Java (démo)", 3, "l'extrait", 1_000L, () -> { });
        recents.recordLocation("Java (démo)", 8, "l'autre", 2_000L, () -> { });
        assertEquals("deux emplacements vivent", 2,
                recents.locations().size());
        assertEquals("le plus récent au sommet", 8,
                recents.locations().get(0).line());
        // le MÊME fichier ET la MÊME ligne consécutifs ne se dupliquent pas
        recents.recordLocation("Java (démo)", 8, "l'autre neuf", 5_000L,
                () -> { });
        assertEquals("la même consécutive ne se duplique pas", 2,
                recents.locations().size());
        assertEquals("son heure se rafraîchit", 5_000L,
                recents.locations().get(0).when());
        // mais la même ligne revenue après une autre se re-pose
        recents.recordLocation("Java (démo)", 3, "revenue", 6_000L, () -> { });
        assertEquals("la ligne revenue se re-pose", 3,
                recents.locations().size());
    }

    /** Le plafond tient : le plus vieux chasse au débordement. */
    @Test
    public void theCapBoundHolds() {
        for (int at = 0; at < 30; at++) {
            recents.recordFile("fichier " + at, at * 1_000L, () -> { });
            recents.recordLocation("fichier " + at, at, "l'extrait",
                    at * 1_000L, () -> { });
        }
        assertEquals("le plafond des fichiers tient", 10,
                recents.files().size());
        assertEquals("le plafond des emplacements tient", 10,
                recents.locations().size());
        assertEquals("le plus récent au sommet", "fichier 29",
                recents.files().get(0).name());
    }

    /** L'ancienneté se lit : l'instant, les minutes, les heures, les jours. */
    @Test
    public void theAgeReads() {
        assertEquals("à l'instant", RecentFiles.ageOf(10_000L, 10_000L));
        assertEquals("il y a 1 min", "il y a 1 min",
                RecentFiles.ageOf(0L, 60_000L));
        assertEquals("il y a 5 min", RecentFiles.ageOf(0L, 300_000L));
        assertEquals("il y a 2 h", "il y a 2 h", RecentFiles.ageOf(0L, 7_200_000L));
        assertEquals("il y a 3 j", "il y a 3 j",
                RecentFiles.ageOf(0L, 3L * 86_400_000L));
    }

    /** La mémoire vide ne vit pas ; le retour y lit les listes. */
    @Test
    public void theEmptyAndClearLife() {
        assertTrue("la mémoire neuve est vide", recents.isEmpty());
        recents.recordFile("Java (démo)", 1_000L, () -> { });
        assertFalse("la mémoire vivante n'est pas vide", recents.isEmpty());
        List<RecentFiles.RecentFile> files = recents.files();
        assertEquals("le fichier se lit", 1, files.size());
        recents.clear();
        assertTrue("la mémoire se vide", recents.isEmpty());
    }
}
