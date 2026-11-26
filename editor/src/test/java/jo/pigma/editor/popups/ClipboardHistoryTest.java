package jo.pigma.editor.popups;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * L'HISTORIQUE du presse-papiers aux JVM pures : la copie se pose
 * (la plus récente au sommet), la même chaîne consécutive ne se
 * duplique pas (son heure se rafraîchit), le plafond tient (la plus
 * vieille chasse), l'ancienneté se lit.
 */
public class ClipboardHistoryTest {

    /** La mémoire éprouvée. */
    private final ClipboardHistory history = new ClipboardHistory();

    /** La copie se pose, la plus récente au sommet. */
    @Test
    public void theCopyLandsAtTheTop() {
        history.record("première", 1_000L);
        history.record("seconde", 2_000L);
        assertEquals("deux entrées vivrent", 2, history.size());
        assertEquals("la plus récente au sommet", "seconde",
                history.entries().get(0).text());
        assertEquals("la plus vieille dessous", "première",
                history.entries().get(1).text());
        // la copie vide ne se pose pas
        history.record("", 3_000L);
        history.record(null, 3_000L);
        assertEquals("la copie vide ne se pose pas", 2, history.size());
    }

    /** La même chaîne consécutive ne se duplique pas — son heure se rafraîchit. */
    @Test
    public void theSameConsecutiveCopyRefreshesItsHour() {
        history.record("graine", 1_000L);
        history.record("graine", 5_000L);
        assertEquals("la même chaîne ne se duplique pas", 1, history.size());
        assertEquals("son heure se rafraîchit", 5_000L,
                history.entries().get(0).when());
        // mais la même chaîne Revenue après une autre se re-pose
        history.record("autre", 6_000L);
        history.record("graine", 7_000L);
        assertEquals("la même chaîne revenue se re-pose", 3, history.size());
        assertEquals("la plus récente au sommet", "graine",
                history.entries().get(0).text());
    }

    /** Le plafond tient : la plus vieille chasse au débordement. */
    @Test
    public void theCapBoundHolds() {
        for (int at = 0; at < 30; at++) {
            history.record("copie " + at, at * 1_000L);
        }
        assertEquals("le plafond tient", 20, history.size());
        assertEquals("la plus récente au sommet", "copie 29",
                history.entries().get(0).text());
        assertEquals("la plus vieille chassée", "copie 10",
                history.entries().get(19).text());
        // la mémoire se vide
        history.clear();
        assertTrue("la mémoire se vide", history.isEmpty());
    }

    /** L'ancienneté se lit : à l'instant, les minutes, les heures, les jours. */
    @Test
    public void theAgeReads() {
        long now = 10 * 60_000L;
        assertEquals("à l'instant", "à l'instant",
                ClipboardHistory.ageOf(now, now));
        assertEquals("les minutes", "il y a 5 min",
                ClipboardHistory.ageOf(now - 5 * 60_000L, now));
        assertEquals("les heures", "il y a 2 h",
                ClipboardHistory.ageOf(now - 125 * 60_000L, now));
        assertEquals("les jours", "il y a 3 j",
                ClipboardHistory.ageOf(now - 76 * 60 * 60_000L, now));
    }
}
