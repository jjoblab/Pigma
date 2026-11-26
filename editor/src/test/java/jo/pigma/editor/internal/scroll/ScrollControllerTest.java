package jo.pigma.editor.internal.scroll;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Le balayeur aux bords (JVM, sans canevas) : le doigt qui TIENT la
 * zone programme l'échéance (une seule — jamais en rafale), chaque
 * échéance glisse d'une DEMI-LIGNE vers le doigt et reprogramme, le
 * bord du document ARRETE (le défilement borné ne glisse plus), le
 * doigt qui QUITTE la zone arrête, l'annulation range net.
 */
public final class ScrollControllerTest {

    /** L'hôte d'essai : les murs posés, les échéances collectées. */
    private static final class RecordingHost
            implements ScrollController.Host {

        private boolean dragging = true;

        private float fingerY = 500.0f;

        private final float top = 100.0f;

        private final float bottom = 900.0f;

        private final float line = 40.0f;

        private float maxScrollY = 10_000.0f;

        private float scrollY;

        private int drops;

        private int repaints;

        private final Map<Runnable, Long> posted = new HashMap<>();

        private final List<Runnable> removed = new ArrayList<>();

        @Override
        public boolean isTextDragging() {
            return dragging;
        }

        @Override
        public float fingerY() {
            return fingerY;
        }

        @Override
        public float viewportTop() {
            return top;
        }

        @Override
        public float viewportBottom() {
            return bottom;
        }

        @Override
        public float lineHeight() {
            return line;
        }

        @Override
        public float maxScrollY() {
            return maxScrollY;
        }

        @Override
        public float scrollY() {
            return scrollY;
        }

        @Override
        public void setScrollY(float value) {
            scrollY = value;
        }

        @Override
        public void recomputeDrop() {
            drops++;
        }

        @Override
        public void repaint() {
            repaints++;
        }

        @Override
        public void postDelayed(Runnable tick, long delayMs) {
            posted.put(tick, delayMs);
        }

        @Override
        public void removeCallbacks(Runnable tick) {
            removed.add(tick);
        }
    }

    private final ScrollControllerTest.RecordingHost host =
            new ScrollControllerTest.RecordingHost();

    private final ScrollController controller = new ScrollController(host);

    /**
     * LE DOIGT TIENT LA ZONE : l'échéance se programme — une seule
     * (la rafale ne s'y paie pas), au pas du balayage (16 ms).
     */
    @Test
    public void theZoneSchedulesOnce() {
        host.fingerY = 120.0f;   // dans la zone du haut (top 100 + 48)
        controller.scheduleNearEdges(48.0f);
        assertEquals("l'échéance se programme", 1, host.posted.size());
        assertEquals("le pas du balayage", Long.valueOf(16L),
                host.posted.values().iterator().next());
        // une seconde demande ne programme pas en rafale
        controller.scheduleNearEdges(48.0f);
        assertEquals(1, host.posted.size());
        // le doigt hors zone ne programme rien
        ScrollController other = new ScrollController(host);
        host.fingerY = 500.0f;
        other.scheduleNearEdges(48.0f);
        assertEquals(1, host.posted.size());
    }

    /**
     * CHAQUE ÉCHÉANCE GLISSE D'UNE DEMI-LIGNE vers le doigt : vers le
     * HAUT quand il tient le bord haut, le point de dépôt se relit,
     * la suite se reprogramme.
     */
    @Test
    public void eachTickSlidesHalfALine() {
        host.fingerY = 120.0f;   // tient le bord haut
        host.scrollY = 1000.0f;
        controller.scheduleNearEdges(48.0f);
        Runnable tick = host.posted.keySet().iterator().next();
        tick.run();
        assertEquals("la vue glisse d'une demi-ligne vers le haut",
                980.0f, host.scrollY, 0.001f);
        assertEquals("le point de dépôt se relit", 1, host.drops);
        assertEquals("la trame se réclame", 1, host.repaints);
        assertEquals("la suite se reprogramme", 1, host.posted.size());
        // le doigt au bord BAS glisse vers le bas
        host.fingerY = 880.0f;
        ScrollController down = new ScrollController(host);
        host.posted.clear();
        down.scheduleNearEdges(48.0f);
        host.posted.keySet().iterator().next().run();
        assertEquals("la vue glisse d'une demi-ligne vers le bas",
                1000.0f, host.scrollY, 0.001f);
    }

    /**
     * LE BORD DU DOCUMENT ARRETE : le défilement borné au mur ne
     * glisse plus — la moitié-ligne s'y absorbe, rien ne se paie.
     */
    @Test
    public void theDocumentEdgeStops() {
        host.fingerY = 120.0f;
        host.scrollY = 0.0f;    // déjà au plancher
        controller.scheduleNearEdges(48.0f);
        Runnable tick = host.posted.keySet().iterator().next();
        tick.run();
        assertEquals("le plancher ne descend pas", 0.0f, host.scrollY, 0.001f);
        assertEquals("rien ne se relit au mur", 0, host.drops);
    }

    /**
     * L'ANNULATION RANGE NET : l'échéance se retire, le drapeau se
     * rend — le geste finit, le balayage s'arrête.
     */
    @Test
    public void theCancelStopsClean() {
        host.fingerY = 120.0f;
        controller.scheduleNearEdges(48.0f);
        Runnable tick = host.posted.keySet().iterator().next();
        controller.cancel();
        assertEquals("l'échéance se retire", 1, host.removed.size());
        // le traîné fini : l'échéance qui reste ne glisse rien
        host.dragging = false;
        tick.run();
        assertEquals(0.0f, host.scrollY, 0.001f);
        assertEquals("rien ne se relit hors traîné", 0, host.drops);
        assertFalse("le drapeau s'est rendu", controller.isScheduled());
    }
}
