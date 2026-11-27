package jo.pigma.editor.popups;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Canvas;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * La carte de diagnostic à la base de liste (JVM sans canevas réel —
 * la LOGIQUE se prouve : l'état qui vit de ses entrées, la fenêtre
 * de rangées, le toucher qui arme au doigt et referme au
 * relâchement, la traîne qui défile, l'étage des correctifs). La
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
     * Le toucher ARME, le relâchement joue : hors carte le geste
     * poursuit sa vie ; la rangée s'arme au doigt et son relâchement
     * la REFERME (le message se lit) ; le correctif s'arme
     * pareillement, son relâchement l'EXÉCUTE et la referme — un
     * relâchement qui ne lui appartient pas n'y touche pas (les
     * cartes sœurs jouent leur propre geste).
     */
    @Test
    public void theTouchArmsTheReleasePlays() {
        card.open(4, List.of(new DiagnosticCard.Entry(
                DiagnosticCard.SEVERITY_ERROR, "oups",
                List.of(fix("corriger l'erreur")))));
        PopupMetrics metrics = new PopupMetrics(78.0f, 16.0f, 16.0f, 16.0f,
                1080.0f, 1920.0f, 60.0f, 20.0f, 1.0f, 120.0f, 500.0f);
        card.draw(new Canvas(), 480.0f, 540.0f, metrics);
        assertFalse("le doigt hors carte ne prend rien",
                card.onTouchArea(10.0f, 10.0f));
        assertFalse("un relâchement sans appui ne joue rien",
                card.onRelease());
        // la rangée de message : le DOWN arme (la carte VIT — le
        // retour du terrain : elle se fermait au DOWN, sa liste ne
        // se défilait jamais), le relâchement la referme
        float rowY = card.rowCenterY(0);
        assertTrue("le doigt sur la rangée se consomme",
                card.onTouchArea(card.poseX() + 40.0f, rowY));
        assertTrue("le DOWN laisse la carte vivre (la traîne y défilera)",
                card.isShown());
        assertTrue("le relâchement de la rangée joue",
                card.onRelease());
        assertFalse("la rangée referme la carte", card.isShown());
        // LE CORRECTIF s'arme au doigt, son relâchement l'EXÉCUTE et
        // la referme (l'étage des correctifs vit sous les rangées :
        // le pied de 30 dp bordé par l'étage)
        card.open(4, List.of(new DiagnosticCard.Entry(
                DiagnosticCard.SEVERITY_ERROR, "oups",
                List.of(fix("corriger l'erreur")))));
        card.draw(new Canvas(), 480.0f, 540.0f, metrics);
        float fixY = card.poseY() + card.height() - 30.0f - 25.0f;
        assertTrue("le doigt sur l'étage des correctifs se consomme",
                card.onTouchArea(card.poseX() + 40.0f, fixY));
        assertTrue("le correctif armé vit encore avant le relâchement",
                card.isShown());
        assertTrue("le relâchement du correctif joue",
                card.onRelease());
        assertFalse("le correctif referme la carte", card.isShown());
        assertEquals("le correctif s'est exécuté UNE fois", 1, played);
    }

    /**
     * LA TRAÎNE DÉFILE (le retour du terrain : le doigt qui voulait
     * faire glisser la liste fermait la carte) : douze diagnostics
     * dans une fenêtre de huit — la traîne monte la fenêtre au mur,
     * la carte vit toujours, la traîne redescend la rend au début.
     */
    @Test
    public void theDragScrollsTheWindow() {
        List<DiagnosticCard.Entry> many = new ArrayList<>();
        for (int at = 0; at < 12; at++) {
            many.add(new DiagnosticCard.Entry(
                    DiagnosticCard.SEVERITY_ERROR, "l'erreur " + at));
        }
        card.open(4, many);
        PopupMetrics metrics = new PopupMetrics(78.0f, 16.0f, 16.0f, 16.0f,
                1080.0f, 1920.0f, 60.0f, 20.0f, 1.0f, 120.0f, 500.0f);
        card.draw(new Canvas(), 480.0f, 540.0f, metrics);
        assertTrue("la fenêtre resserre la liste",
                card.topRow() == 0 && card.entries().size() > card.topRow());
        // le doigt sur la carte, la traîne qui monte : la fenêtre
        // suit, la carte ne se range pas
        assertTrue("le doigt sur la carte se consomme",
                card.onTouchArea(card.poseX() + 40.0f,
                        card.rowCenterY(0)));
        assertTrue("la traîne monte la fenêtre",
                card.dragRows(-1000.0f));
        assertTrue("la fenêtre vit au mur (12 rangées, 8 visibles)",
                card.topRow() == 4);
        assertTrue("la carte vit toujours (la traîne ne la range pas)",
                card.isShown());
        assertTrue("la traîne redescend la fenêtre",
                card.dragRows(1000.0f));
        assertTrue("la fenêtre rend le début", card.topRow() == 0);
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
