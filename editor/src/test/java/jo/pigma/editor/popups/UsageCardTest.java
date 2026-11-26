package jo.pigma.editor.popups;

import java.util.List;

import android.graphics.Canvas;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * La carte des usages à la base de liste (JVM sans canevas réel —
 * les pinceaux y vivent des valeurs par défaut, la LOGIQUE se
 * prouve : l'état, l'armement des rangées, la croix, la fenêtre).
 * La migration à la base de cartes (R-F) : la pose, les murs, la croix et le
 * défilement vivent de {@link CardBase} et {@link ListCard}.
 */
public class UsageCardTest {

    /** La carte éprouvée, à la densité unité. */
    private final UsageCard card = new UsageCard(1.0f);

    /** Un usage de compteur (la navigation y compte). */
    private int played;

    private UsageCard.Usage usage(int line) {
        return new UsageCard.Usage(line, 0, "", "l'extrait de la ligne",
                () -> played++);
    }

    /**
     * L'état de vie : la liste vide range la carte, la liste vivante
     * l'ouvre, la fermeture ramasse les restes.
     */
    @Test
    public void theLifeOfTheCard() {
        assertFalse("la carte naît rangée", card.isShown());
        card.open("symbole", List.of(usage(1)));
        assertTrue("la carte ouvre", card.isShown());
        assertEquals("le symbole se lit", "symbole", card.symbol());
        assertEquals("les usages se lisent", 1, card.usages().size());
        card.close();
        assertFalse("la fermeture range la carte", card.isShown());
        assertTrue("les usages se ramassent", card.usages().isEmpty());
        assertEquals("le symbole se ramasse", "", card.symbol());
        card.open("vide", List.of());
        assertFalse("la liste vide n'ouvre rien", card.isShown());
    }

    /**
     * La pose : dessous l'ancre quand la place y vit, DESSUS sans la
     * place — jamais sur elle, bornée aux murs de la vue.
     */
    @Test
    public void thePoseBelowThenAbove() {
        card.open("symbole", List.of(usage(1), usage(2), usage(3)));
        PopupMetrics metrics = new PopupMetrics(78.0f, 16.0f, 16.0f, 16.0f,
                1080.0f, 1920.0f, 60.0f, 20.0f, 1.0f, 120.0f, 500.0f);
        // l'ancre au MILIEU : la carte vit dessous
        card.draw(new Canvas(), 480.0f, 540.0f, metrics);
        assertTrue("la carte se pose dessous l'ancre (le milieu de la vue)",
                card.poseY() >= 540.0f);
        assertTrue("la carte reste dans les murs",
                card.poseY() + card.height() <= 1920.0f);
        assertTrue("le bord gauche reste dans les murs",
                card.poseX() >= 0.0f && card.poseX() + card.width() <= 1080.0f);
        // l'ancre au FOND : la carte vit dessus
        card.draw(new Canvas(), 1800.0f, 1860.0f, metrics);
        assertTrue("la carte se pose dessus sans la place (le fond de la vue)",
                card.poseY() + card.height() <= 1800.0f + 4.0f);
        // l'ordonnée du centre d'une rangée se lit dans la fenêtre
        assertTrue("le centre d'une rangée vit dans la carte",
                card.rowCenterY(0) >= card.poseY()
                        && card.rowCenterY(0) < card.poseY() + card.height());
        assertEquals("la rangée hors fenêtre ne se lit pas", -1.0f,
                card.rowCenterY(3), 0.0f);
    }

    /**
     * Le toucher : hors carte le geste poursuit sa vie, la croix la
     * range, la rangée s'arme au DOWN et se JOUE à l'UP (la carte se
     * range après le choix — la navigation part à la vue liée, le
     * harnais réel l'y éprouve).
     */
    @Test
    public void theCrossAndTheArmedRow() {
        card.open("symbole", List.of(usage(1), usage(2)));
        PopupMetrics metrics = new PopupMetrics(78.0f, 16.0f, 16.0f, 16.0f,
                1080.0f, 1920.0f, 60.0f, 20.0f, 1.0f, 120.0f, 500.0f);
        card.draw(new Canvas(), 480.0f, 540.0f, metrics);
        // hors carte : le geste poursuit sa vie
        assertFalse("le doigt hors carte ne prend rien",
                card.onTouchArea(10.0f, 10.0f));
        // la croix (le coin haut droit de l'en-tête) : la carte se range
        assertTrue("le doigt sur la carte se consomme",
                card.onTouchArea(card.poseX() + card.width() - 20.0f,
                        card.poseY() + 12.0f));
        assertFalse("la croix range la carte", card.isShown());
        // la rangée s'ARME puis se JOUE au relâchement (la carte se
        // range — jamais deux fois)
        card.open("symbole", List.of(usage(1), usage(2)));
        card.draw(new Canvas(), 480.0f, 540.0f, metrics);
        float rowY = card.rowCenterY(0);
        assertTrue("le doigt sur la rangée se consomme",
                card.onTouchArea(card.poseX() + 40.0f, rowY));
        assertTrue("le relâchement joue la rangée", card.onRelease());
        assertFalse("la carte se range après le choix", card.isShown());
        assertFalse("le relâchement sans arme ne joue rien", card.onRelease());
    }

    /** Le style posé par l'hôte se pose et se relit (la même instance). */
    @Test
    public void theStyleIsCarried() {
        PopupStyle posed = PopupStyle.defaults();
        card.setStyle(posed);
        assertTrue("le style posé se relit (la même instance)",
                card.style() == posed);
        card.setStyle(null);
        assertFalse("le style nul retombe à une instance neuve du défaut",
                card.style() == posed);
    }
}
