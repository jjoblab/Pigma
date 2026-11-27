package jo.pigma.editor.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;

/**
 * B4 : la machine du TEXTE FANTÔME, pure — les phases (l'attente, le
 * vol, l'affichage), la temporisation à l'horloge posée, la
 * péremption (une réponse d'un texte qui a bougé se jette), la
 * frappe qui reprend tout (une seule demande en vol), l'acceptation
 * PARTIELLE (un mot) et TOTALE, le masquage de chaque phase.
 */
public final class GhostTextStateTest {

    /** L'horloge du harnais (le temps y avance à la main). */
    private final AtomicLong clock = new AtomicLong();

    /** La machine à l'horloge posée. */
    private final GhostTextState ghost = new GhostTextState(clock::get);

    /** Le temps passe (ms). */
    private void pass(long millis) {
        clock.addAndGet(millis * 1_000_000L);
    }

    /**
     * Les phases dans l'ordre : la frappe arme l'attente, l'échéance
     * part le vol, la réponse qui dit le texte d'avant AFFICHE le
     * fantôme.
     */
    @Test
    public void thePhasesFollowThePlan() {
        assertEquals("le repos d'abord", GhostTextState.Phase.INACTIVE,
                ghost.phase());
        assertFalse("rien ne s'affiche au repos", ghost.isShown());
        // la frappe arme
        assertFalse("aucun vol à couper au repos", ghost.typed());
        assertEquals("l'attente vit", GhostTextState.Phase.WAITING,
                ghost.phase());
        // l'échéance ne part pas avant le temps
        assertFalse("l'échéance trop tôt ne part pas", ghost.timeoutReached());
        pass(399);
        assertFalse("l'échéance à une ms près ne part pas",
                ghost.timeoutReached());
        pass(1);
        assertTrue("l'échéance échue part", ghost.timeoutReached());
        assertEquals("le vol vit", GhostTextState.Phase.FLYING,
                ghost.phase());
        // la réponse qui dit le texte d'avant affiche
        ghost.flew(7, 42);
        assertTrue("la réponse fidèle affiche",
                ghost.answered("return total;", 7, 42));
        assertEquals("le fantôme vit", GhostTextState.Phase.SHOWN,
                ghost.phase());
        assertEquals("le texte affiché se lit", "return total;",
                ghost.text());
    }

    /**
     * La réponse PÉRIMÉE se jette : une révision qui a bougé, une
     * position qui a bougé, une réponse vide — le fantôme meurt sans
     * jamais se montrer.
     */
    @Test
    public void theStaleAnswerIsDropped() {
        ghost.typed();
        pass(400);
        assertTrue(ghost.timeoutReached());
        ghost.flew(7, 42);
        assertFalse("la révision qui a bougé se jette",
                ghost.answered("un mot", 8, 42));
        assertEquals("le fantôme meurt", GhostTextState.Phase.INACTIVE,
                ghost.phase());
        // la position qui a bougé se jette aussi
        ghost.typed();
        pass(400);
        assertTrue(ghost.timeoutReached());
        ghost.flew(7, 42);
        assertFalse("la position qui a bougé se jette",
                ghost.answered("un mot", 7, 43));
        // la réponse vide se jette aussi
        ghost.typed();
        pass(400);
        assertTrue(ghost.timeoutReached());
        ghost.flew(7, 42);
        assertFalse("la réponse vide se jette", ghost.answered("", 7, 42));
    }

    /**
     * La frappe REPREND TOUT : le vol vivant se coupe (une seule
     * demande en vol — le retour le dit), le fantôme affiché meurt,
     * l'attente se réarme ; la réponse du vol coupé se jette.
     */
    @Test
    public void theTypingRetakesEverything() {
        ghost.typed();
        pass(400);
        assertTrue(ghost.timeoutReached());
        // la frappe pendant le vol : le vol se coupe, l'attente se réarme
        assertTrue("le vol vivant se coupe", ghost.typed());
        assertEquals("l'attente se réarme", GhostTextState.Phase.WAITING,
                ghost.phase());
        // le fantôme affiché meurt à la frappe
        pass(400);
        assertTrue(ghost.timeoutReached());
        ghost.flew(3, 10);
        assertTrue(ghost.answered("le reste", 3, 10));
        assertTrue(ghost.isShown());
        assertFalse("le fantôme meurt à la frappe (aucun vol à couper)",
                ghost.typed());
        assertFalse("le fantôme est mort", ghost.isShown());
        assertEquals("le texte se range", "", ghost.text());
    }

    /**
     * L'acceptation TOTALE : toute la proposition se rend, le
     * fantôme meurt — le rejet jette sans rien rendre.
     */
    @Test
    public void theFullAcceptanceAndTheRejection() {
        shown("un deux trois");
        assertEquals("tout se rend", "un deux trois", ghost.takeAll());
        assertEquals("le fantôme meurt", GhostTextState.Phase.INACTIVE,
                ghost.phase());
        assertEquals("rien après la mort", "", ghost.takeAll());
        // le rejet jette sans rien rendre
        shown("perdue");
        ghost.masked();
        assertFalse("le rejet tue le fantôme", ghost.isShown());
    }

    /**
     * L'acceptation PARTIELLE : le PREMIER MOT se détache (son espace
     * suit), le RESTE demeure au caret — le dernier mot épuise la
     * proposition.
     */
    @Test
    public void theWordByWordAcceptance() {
        shown("un deux trois");
        assertEquals("le premier mot se pose", "un ", ghost.takeWord());
        assertTrue("le reste demeure", ghost.isShown());
        assertEquals("le reste se lit", "deux trois", ghost.text());
        assertEquals("le deuxième mot se pose", "deux ", ghost.takeWord());
        assertEquals("le dernier mot se pose", "trois", ghost.takeWord());
        assertEquals("la proposition épuisée meurt",
                GhostTextState.Phase.INACTIVE, ghost.phase());
        assertEquals("rien après l'épuisement", "", ghost.takeWord());
        // un mot seul s'épuise d'un coup
        shown("seul");
        assertEquals("le mot seul se pose entier", "seul", ghost.takeWord());
        assertEquals("la proposition épuisée meurt encore",
                GhostTextState.Phase.INACTIVE, ghost.phase());
        // un ESPACE DE TÊTE voyage avec le mot (la pose n'en laisse
        // jamais un doublé)
        shown(" deux trois");
        assertEquals("l'espace de tête suit le mot", " deux ",
                ghost.takeWord());
        assertEquals("le reste se lit sans souffle", "trois",
                ghost.text());
    }

    /**
     * Le MASQUAGE de chaque phase : l'attente meurt, le vol vivant
     * se coupe (le retour le dit), le fantôme affiché meurt — la
     * réponse d'un vol masqué se jette.
     */
    @Test
    public void theMaskingCutsEveryPhase() {
        // l'attente meurt
        ghost.typed();
        assertFalse("aucun vol à couper à l'attente", ghost.masked());
        assertEquals("l'attente meurt", GhostTextState.Phase.INACTIVE,
                ghost.phase());
        // le vol vivant se coupe
        ghost.typed();
        pass(400);
        assertTrue(ghost.timeoutReached());
        assertTrue("le vol vivant se coupe", ghost.masked());
        assertFalse("la réponse du vol masqué se jette",
                ghost.answered("trop tard", 0, 0));
        // le fantôme affiché meurt
        shown("encore");
        assertFalse("le fantôme meurt (aucun vol à couper)",
                ghost.masked());
        assertFalse("le fantôme est mort", ghost.isShown());
    }

    /**
     * La temporisation se règle : l'hôte la resserre ou la lâche
     * (le plan la veut entre 300 et 500 ms).
     */
    @Test
    public void theTimeoutIsAdjustable() {
        ghost.setTimeoutMs(300);
        assertEquals("la temporisation posée se lit", 300,
                ghost.timeoutMs());
        ghost.typed();
        pass(299);
        assertFalse("la temporisation posée ne part pas avant",
                ghost.timeoutReached());
        pass(1);
        assertTrue("la temporisation posée part à son heure",
                ghost.timeoutReached());
    }

    /** Le fantôme s'affiche (l'attente, l'échéance, la réponse). */
    private void shown(String text) {
        ghost.typed();
        pass(400);
        assertTrue(ghost.timeoutReached());
        ghost.flew(1, 2);
        assertTrue(ghost.answered(text, 1, 2));
    }
}
