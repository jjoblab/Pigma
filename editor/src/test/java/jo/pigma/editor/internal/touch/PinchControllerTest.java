package jo.pigma.editor.internal.touch;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Le pincement (JVM, sans canevas) : l'écart des doigts porte le
 * FACTEUR (le corps du départ l'attend), la ligne sous le FOYER du
 * départ demeure sous le foyer courant (le texte ne fuit pas sous
 * les doigts, en Y comme en X), le dénouement repose l'appui restant,
 * la rangée rend tout.
 */
public final class PinchControllerTest {

    /** L'hôte d'essai : les murs et le corps posés, les écritures
     * collectées. */
    private static final class RecordingHost
            implements PinchController.Host {

        private float textPx = 50.0f;

        private final float lineHeight = 40.0f;

        private final float charWidth = 20.0f;

        private final float viewportTop = 100.0f;

        private final float viewportLeft = 60.0f;

        private float scrollY;

        private float maxScrollY = 100_000.0f;

        private float scrollX;

        private float maxScrollX = 50_000.0f;

        private int focusTaken;

        private int settled;

        private final List<float[]> restarts = new ArrayList<>();

        private int tapsForgotten;

        private int repaints;

        @Override
        public float textPx() {
            return textPx;
        }

        @Override
        public void applyTextSize(float freshPx) {
            textPx = freshPx;
        }

        @Override
        public float lineHeight() {
            return lineHeight;
        }

        @Override
        public float charWidth() {
            return charWidth;
        }

        @Override
        public float viewportTop() {
            return viewportTop;
        }

        @Override
        public float viewportLeft() {
            return viewportLeft;
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
        public float maxScrollY() {
            return maxScrollY;
        }

        @Override
        public float scrollX() {
            return scrollX;
        }

        @Override
        public void setScrollX(float value) {
            scrollX = value;
        }

        @Override
        public float maxScrollX() {
            return maxScrollX;
        }

        @Override
        public void takeFocus() {
            focusTaken++;
        }

        @Override
        public void settleGestures() {
            settled++;
        }

        @Override
        public void touchRestartedAt(float x, float y) {
            restarts.add(new float[] {x, y});
        }

        @Override
        public void forgetLastTap() {
            tapsForgotten++;
        }

        @Override
        public void repaint() {
            repaints++;
        }
    }

    private final PinchControllerTest.RecordingHost host =
            new PinchControllerTest.RecordingHost();

    private final PinchController controller = new PinchController(host);

    /**
     * L'ÉCART PORTE LE FACTEUR : le corps du départ l'attend —
     * l'écart doublé double le corps.
     */
    @Test
    public void theSpanCarriesTheFactor() {
        host.textPx = 40.0f;
        controller.engage(100.0f, 200.0f, 300.0f);
        assertTrue("le pincement vit", controller.isPinching());
        assertEquals("le focus se prend", 1, host.focusTaken);
        assertEquals("les gestes se rangent", 1, host.settled);
        // l'écart doublé : le corps double
        controller.move(200.0f, 200.0f, 300.0f);
        assertEquals("l'écart doublé double le corps",
                80.0f, host.textPx, 0.001f);
        // l'écart réduit de moitié : le corps aussi
        controller.move(50.0f, 200.0f, 300.0f);
        assertEquals("l'écart réduit de moitié réduit le corps",
                20.0f, host.textPx, 0.001f);
    }

    /**
     * LA LIGNE SOUS LE FOYER Y DEMEURE : le pincement tient la ligne
     * du départ sous le foyer courant — le texte ne fuit pas sous les
     * doigts (la colonne aussi, en X).
     */
    @Test
    public void theLineUnderTheFocusStays() {
        host.scrollY = 300.0f;   // ligne 4 sous le foyer du départ
        controller.engage(100.0f, 200.0f, 180.0f);
        controller.move(200.0f, 200.0f, 180.0f);
        // le foyer ne bouge pas : la ligne sous lui demeure — le
        // défilement ne bouge pas
        assertEquals("le foyer immobile tient sa ligne",
                300.0f, host.scrollY, 0.001f);
        // le foyer descend de deux interlignes : la vue remonte d'autant
        controller.move(200.0f, 200.0f, 260.0f);
        assertEquals("le foyer descendu d'un cran tient sa ligne",
                220.0f, host.scrollY, 0.001f);
    }

    /**
     * LE DÉNOUEMENT REPOSE L'APPUI RESTANT : un doigt s'en va —
     * l'autre repart de SA position vivante, la mémoire du tap
     * s'efface.
     */
    @Test
    public void theEndRestartsTheStayingFinger() {
        controller.engage(100.0f, 200.0f, 300.0f);
        controller.end(240.0f, 320.0f);
        assertFalse("le pincement meurt", controller.isPinching());
        assertEquals("l'appui restant se pose", 1, host.restarts.size());
        assertEquals(240.0f, host.restarts.get(0)[0], 0.001f);
        assertEquals(320.0f, host.restarts.get(0)[1], 0.001f);
        assertEquals("la mémoire du tap s'efface", 1, host.tapsForgotten);
    }

    /**
     * LA RANGÉE REND TOUT : le système reprend le geste — les
     * drapeaux se rendent, l'appui suivant repart de zéro (un
     * pincement rendu ne vaut plus un pinchedThisGesture).
     */
    @Test
    public void theResetSurrenders() {
        controller.engage(100.0f, 200.0f, 300.0f);
        assertTrue(controller.hasPinchedThisGesture());
        controller.reset();
        assertFalse("le drapeau du geste se rend", controller.hasPinchedThisGesture());
        assertFalse(controller.isPinching());
        // l'écart nul ne bouge rien (la rangée s'est rendue)
        controller.move(0.0f, 200.0f, 300.0f);
        assertEquals("l'écart nul ne bouge rien", 50.0f, host.textPx, 0.001f);
    }
}
