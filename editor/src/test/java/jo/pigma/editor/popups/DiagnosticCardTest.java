package jo.pigma.editor.popups;

import java.util.List;

import android.graphics.Canvas;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * La carte de diagnostic à la base de liste (JVM sans canevas réel —
 * la LOGIQUE se prouve : l'état qui vit de ses entrées, la fenêtre
 * de rangées, le toucher qui referme, l'étage des correctifs). La
 * migration à la base de cartes (R-F) : la pose, les murs et le défilement
 * vivent de {@link CardBase} et {@link ListCard}.
 */
public class DiagnosticCardTest {

    /** La carte éprouvée, à la densité unité. */
    private final DiagnosticCard card = new DiagnosticCard(1.0f);

    /** Un correctif de compteur (l'exécution y compte). */
    private int played;

    private DiagnosticCard.Fix fix(String title) {
        return new DiagnosticCard.Fix(title, () -> played++);
    }

    /**
     * L'état vit de ses ENTRÉES : la liste vide range la carte, les
     * entrées muettes se filtrent à l'ouverture, la fermeture
     * ramasse les restes.
     */
    @Test
    public void theLifeLivesOfItsEntries() {
        assertFalse("la carte naît rangée", card.isShown());
        card.open(4, List.of(
                new DiagnosticCard.Entry(DiagnosticCard.SEVERITY_ERROR,
                        "l'erreur de la ligne"),
                new DiagnosticCard.Entry(DiagnosticCard.SEVERITY_WARNING, null)));
        assertTrue("la carte ouvre", card.isShown());
        assertEquals("la ligne ancrée se lit", 4, card.line());
        assertEquals("l'entrée muette se filtre", 1, card.entries().size());
        card.close();
        assertFalse("la fermeture range la carte", card.isShown());
        assertTrue("les entrées se ramassent", card.entries().isEmpty());
        assertEquals("la ligne se ramasse", -1, card.line());
        card.open(7, List.of());
        assertFalse("la liste vide n'ouvre rien", card.isShown());
    }

    /**
     * La pose : dessous la ligne ancrée quand la place y vit, DESSUS
     * sans la place — jamais sur elle, bornée aux murs.
     */
    @Test
    public void thePoseBelowThenAbove() {
        card.open(4, List.of(
                new DiagnosticCard.Entry(DiagnosticCard.SEVERITY_ERROR, "oups")));
        PopupMetrics metrics = new PopupMetrics(78.0f, 16.0f, 16.0f, 16.0f,
                1080.0f, 1920.0f, 60.0f, 20.0f, 1.0f, 120.0f, 500.0f);
        card.draw(new Canvas(), 480.0f, 540.0f, metrics);
        assertTrue("la carte se pose dessous la ligne (le milieu de la vue)",
                card.poseY() >= 540.0f);
        assertTrue("la carte reste dans les murs",
                card.poseY() + card.height() <= 1920.0f);
        card.draw(new Canvas(), 1800.0f, 1860.0f, metrics);
        assertTrue("la carte se pose dessus sans la place (le fond)",
                card.poseY() + card.height() <= 1800.0f + 4.0f);
    }

    /**
     * Le toucher : hors carte le geste poursuit sa vie, la rangée la
     * REFERME (le message se lit) — le correctif s'EXÉCUTE et la
     * referme pareillement.
     */
    @Test
    public void theTouchClosesTheCardTheFixRuns() {
        card.open(4, List.of(new DiagnosticCard.Entry(
                DiagnosticCard.SEVERITY_ERROR, "oups",
                List.of(fix("corriger l'erreur")))));
        PopupMetrics metrics = new PopupMetrics(78.0f, 16.0f, 16.0f, 16.0f,
                1080.0f, 1920.0f, 60.0f, 20.0f, 1.0f, 120.0f, 500.0f);
        card.draw(new Canvas(), 480.0f, 540.0f, metrics);
        assertFalse("le doigt hors carte ne prend rien",
                card.onTouchArea(10.0f, 10.0f));
        // la rangée de message : la carte se referme (le message se lit)
        float rowY = card.rowCenterY(0);
        assertTrue("le doigt sur la rangée se consomme",
                card.onTouchArea(card.poseX() + 40.0f, rowY));
        assertFalse("la rangée referme la carte", card.isShown());
        // LE CORRECTIF s'exécute au doigt (l'étage des correctifs vit
        // sous les rangées : le pied de 30 dp bordé par l'étage) et la
        // referme
        card.open(4, List.of(new DiagnosticCard.Entry(
                DiagnosticCard.SEVERITY_ERROR, "oups",
                List.of(fix("corriger l'erreur")))));
        card.draw(new Canvas(), 480.0f, 540.0f, metrics);
        float fixY = card.poseY() + card.height() - 30.0f - 25.0f;
        assertTrue("le doigt sur l'étage des correctifs se consomme",
                card.onTouchArea(card.poseX() + 40.0f, fixY));
        assertFalse("le correctif referme la carte", card.isShown());
        assertEquals("le correctif s'est exécuté UNE fois", 1, played);
    }

    /** Les correctifs de toutes les entrées se lisent d'une liste. */
    @Test
    public void theFixesOfEveryEntryRead() {
        card.open(4, List.of(
                new DiagnosticCard.Entry(DiagnosticCard.SEVERITY_ERROR,
                        "oups", List.of(fix("le premier"))),
                new DiagnosticCard.Entry(DiagnosticCard.SEVERITY_WARNING,
                        "attention", List.of(fix("le second")))));
        assertEquals("les correctifs des deux entrées se lisent", 2,
                card.fixes().size());
    }
}
