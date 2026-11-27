package jo.pigma.editor.popups;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.function.LongSupplier;

import org.junit.Test;

/**
 * LE FLOT de pose (JVM, sans canevas — la leçon de BadgePopTest) : la
 * pose IDENTIQUE ne tremble pas, la pose changée GLISSE (la valeur en
 * route, l'arrivée posée), la naissance POSE d'un trait, le flot se
 * range au bout de son temps — le tout à l'horloge posée du harnais.
 */
public class PopupFloatTest {

    /** L'horloge contrôlée du test (l'unique case du temps courant). */
    private long now;

    /** Le flot éprouvé, à l'horloge contrôlée. */
    private final PopupFloat flot = new PopupFloat(() -> now);

    /**
     * LA NAISSANCE POSE : le premier `poseTo` ne flotte pas (la carte
     * naît à sa pose — le pop d'entrée garde son rôle), la pose
     * identique qui suit ne déclenche rien.
     */
    @Test
    public void theBirthPosesWithoutFloating() {
        now = 1000L;
        assertFalse("la naissance pose sans flotter",
                flot.poseTo(120.0f, 340.0f));
        assertFalse("aucun flot ne vit à la naissance", flot.isRunning());
        assertEquals("la pose est posée d'un trait",
                120.0f, flot.currentX(), 0.0f);
        assertEquals("la pose est posée d'un trait (hauteur)",
                340.0f, flot.currentY(), 0.0f);
        now = 1016L;
        assertFalse("la pose IDENTIQUE ne déclenche rien",
                flot.poseTo(120.0f, 340.0f));
        assertFalse("la carte qui redessine au même endroit ne tremble pas",
                flot.isRunning());
    }

    /**
     * LA POSE CHANGÉE GLISSE : le départ vif, la valeur en route, la
     * pose atteinte au bout du temps — jamais au-delà (le frein doux
     * de l'ease-out).
     */
    @Test
    public void theChangedPoseGlides() {
        now = 1000L;
        flot.poseTo(120.0f, 340.0f);
        now = 1016L;
        assertTrue("la pose changée fait naître un flot",
                flot.poseTo(160.0f, 440.0f));
        assertTrue("le flot vit", flot.isRunning());
        // le DÉCALAGE vit (la carte se dessine en route vers sa pose)
        assertEquals("le décalage horizontal vit",
                -40.0f, flot.offsetX(), 0.5f);
        assertEquals("le décalage vertical vit",
                -100.0f, flot.offsetY(), 0.5f);
        // EN ROUTE : la valeur s'approche de la pose (jamais au-delà)
        float enRouteX = flot.currentX();
        now = 1040L;
        assertTrue("la valeur en route avance", flot.currentX() > enRouteX);
        assertTrue("la valeur en route ne dépasse pas la pose",
                flot.currentX() < 160.0f);
        // AU BOUT du temps, la pose est atteinte et le flot se range
        now = 1160L;
        assertFalse("le flot se range au bout de son temps",
                flot.isRunning());
        assertEquals("la pose est atteinte", 160.0f,
                flot.currentX(), 0.0f);
        assertEquals("la pose est atteinte (hauteur)", 440.0f,
                flot.currentY(), 0.0f);
        assertEquals("le décalage rendu est nul", 0.0f,
                flot.offsetX(), 0.0f);
    }

    /**
     * LE FLOT RELANCÉ part de là où la carte se VOIT : la valeur en
     * route devient le départ du flot neuf — jamais de raccourci qui
     * saute à la pose d'avant.
     */
    @Test
    public void theRestartDepartsFromTheVisiblePose() {
        now = 1000L;
        flot.poseTo(120.0f, 340.0f);
        now = 1016L;
        flot.poseTo(160.0f, 340.0f);
        // en route, à un tiers du temps
        now = 1063L;
        float enRoute = flot.currentX();
        assertTrue("la carte est en route", enRoute > 120.0f
                && enRoute < 160.0f);
        // la pose change ENCORE : le flot neuf part de la valeur VUE
        flot.poseTo(200.0f, 340.0f);
        assertEquals("le flot neuf part de la pose vue", enRoute,
                flot.currentX(), 0.75f);
    }

    /**
     * LE RESET range le flot : la pose suivante se pose d'un trait
     * (la fermeture de la carte ramasse le flot — la prochaine
     * naissance ne glisse pas du passé).
     */
    @Test
    public void theResetRangsTheFloat() {
        now = 1000L;
        flot.poseTo(120.0f, 340.0f);
        now = 1016L;
        flot.poseTo(160.0f, 340.0f);
        flot.reset();
        assertFalse("le reset range le flot", flot.isRunning());
        now = 1032L;
        // la pose d'après la remise : la NAISSANCE règne à nouveau
        flot.poseTo(40.0f, 90.0f);
        assertFalse("la pose d'après le reset se pose d'un trait",
                flot.isRunning());
        assertEquals("la pose d'après le reset est posée", 40.0f,
                flot.currentX(), 0.0f);
    }

    /** Le flot construit à l'horloge posée vit pareil (le constructeur du paquet). */
    @Test
    public void theInjectedClockCarriesTheFloat() {
        long[] holder = {1000L};
        LongSupplier clock = () -> holder[0];
        PopupFloat atClock = new PopupFloat(clock);
        assertFalse(atClock.poseTo(120.0f, 340.0f));
        holder[0] = 1016L;
        assertTrue(atClock.poseTo(160.0f, 340.0f));
        assertTrue(atClock.isRunning());
        holder[0] = 1160L;
        assertFalse(atClock.isRunning());
    }
}
