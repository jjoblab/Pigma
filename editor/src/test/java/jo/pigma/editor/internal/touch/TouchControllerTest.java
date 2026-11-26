package jo.pigma.editor.internal.touch;

import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * L'état du toucher (JVM, sans canevas ni MotionEvent) : le contact
 * NAÎT puis GRANDIT — le tap demeure tap, ou franchit le seuil de
 * glisse et devient balayage, ou tient la durée et devient appui long,
 * ou saisit une poignée, ou arme puis engage le traîné-déposé, ou
 * tient la carte du document. Les VERDICTS s'y éprouvent à l'horloge
 * posée (avancée à la main), le rangement rend tout — la mémoire du
 * tap y demeure (seule la vue dit le geste qui a grandi).
 */
public final class TouchControllerTest {

    /** L'horloge posée : le temps avance à la main. */
    private final AtomicLong now = new AtomicLong(1_000_000_000L);

    /** Le contrôleur éprouvé, à l'horloge posée. */
    private final TouchController touch = new TouchController(now::get);

    /** Le seuil de glisse de l'essai (px). */
    private static final float SLOP = 24.0f;

    @Test
    public void tapFollowedByTapDoubles() {
        touch.begin(100.0f, 200.0f);
        // le lâcher compte : la mémoire se pose
        touch.rememberTap(100.0f, 200.0f);
        now.set(1_350_000_000L); // 350 ms : à la limite, pas encore
        assertFalse(touch.tappedTwice(102.0f, 202.0f, SLOP));
        now.set(1_349_999_999L); // une nanoseconde sous la fenêtre
        assertTrue(touch.tappedTwice(102.0f, 202.0f, SLOP));
        // le même endroit se lit au seuil (l'écart y compte)
        now.set(1_100_000_000L);
        assertFalse(touch.tappedTwice(100.0f + SLOP, 200.0f, SLOP));
        assertTrue(touch.tappedTwice(100.0f + SLOP * 0.9f, 200.0f, SLOP));
        // le geste grandi (un balayage) efface la mémoire
        touch.forgetTap();
        assertFalse(touch.tappedTwice(100.0f, 200.0f, SLOP));
    }

    @Test
    public void pressHeldLongEnoughSelects() {
        touch.begin(50.0f, 60.0f);
        assertFalse(touch.heldLongEnough());
        now.set(1_499_999_999L); // une nanoseconde sous le seuil
        assertFalse(touch.heldLongEnough());
        now.set(1_500_000_000L); // pile le seuil (l'appui long vit)
        assertTrue(touch.heldLongEnough());
    }

    @Test
    public void slopCrossingGrowsTheGesture() {
        touch.begin(0.0f, 0.0f);
        assertFalse(touch.beyondSlop(SLOP, 0.0f, SLOP)); // pile : pas encore
        assertTrue(touch.beyondSlop(SLOP + 0.1f, 0.0f, SLOP));
        assertTrue(touch.beyondSlop(0.0f, SLOP + 0.1f, SLOP));
        // le point de départ SE PROPOSE : le balayage y suit son delta
        touch.slideTo(40.0f, 40.0f);
        assertFalse(touch.beyondSlop(40.0f + SLOP, 40.0f + SLOP, SLOP));
        touch.engageScroll();
        assertTrue(touch.isDragging());
        // le lâcher du balayage ne vaut pas un tap : le rangement y rend
        touch.settle();
        assertFalse(touch.isDragging());
    }

    @Test
    public void restartAfterPinchKeepsStartAndEndsScroll() {
        touch.begin(10.0f, 20.0f);
        touch.engageScroll();
        assertTrue(touch.isDragging());
        touch.restartAt(80.0f, 90.0f);
        assertFalse(touch.isDragging());
        assertEquals(80.0f, touch.startX(), 0.0f);
        assertEquals(90.0f, touch.startY(), 0.0f);
        // un doigt absent ne déplace pas le point de départ
        touch.restartAt(Float.NaN, Float.NaN);
        assertEquals(80.0f, touch.startX(), 0.0f);
    }

    @Test
    public void handleHeldThenReleased() {
        touch.begin(300.0f, 300.0f);
        touch.holdsHandle(2, 40, 300.0f);
        assertTrue(touch.isHandleDragging());
        assertEquals(2, touch.draggedBound());
        assertEquals(40, touch.fixedBound());
        // le doigt vivant s'y pose (la loupe le suivra)
        touch.finger(320.0f, 340.0f);
        assertEquals(320.0f, touch.fingerX(), 0.0f);
        assertEquals(340.0f, touch.fingerY(), 0.0f);
        touch.releaseHandle();
        assertFalse(touch.isHandleDragging());
        assertEquals(-1, touch.fixedBound());
    }

    @Test
    public void textDragArmedEngagedDropped() {
        touch.begin(500.0f, 500.0f);
        touch.armTextDrag(true);
        assertTrue(touch.isTextArmed());
        assertFalse(touch.isTextDragging());
        // AVANT la durée de l'appui long, le glissé ENGAGE
        now.set(1_100_000_000L);
        assertFalse(touch.heldLongEnough());
        touch.engageTextDrag();
        assertTrue(touch.isTextDragging());
        // le point de dépôt suit le doigt
        touch.finger(520.0f, 510.0f);
        touch.dropAt(1234);
        assertEquals(1234, touch.dropOffset());
        // le dépôt rend le geste
        touch.releaseTextDrag();
        assertFalse(touch.isTextDragging());
        assertFalse(touch.isTextArmed());
        assertEquals(-1, touch.dropOffset());
    }

    @Test
    public void mapHeldThenReleased() {
        touch.begin(700.0f, 100.0f);
        touch.holdsMap();
        assertTrue(touch.isMapDragging());
        touch.releaseMap();
        assertFalse(touch.isMapDragging());
    }

    @Test
    public void settleRendersEveryGestureButTheTapMemory() {
        touch.begin(0.0f, 0.0f);
        touch.engageScroll();
        touch.holdsHandle(1, 10, 5.0f);
        touch.armTextDrag(true);
        touch.engageTextDrag();
        touch.holdsMap();
        touch.rememberTap(11.0f, 12.0f);
        touch.settle();
        assertFalse(touch.isDragging());
        assertFalse(touch.isMapDragging());
        assertFalse(touch.isHandleDragging());
        assertFalse(touch.isTextDragging());
        assertFalse(touch.isTextArmed());
        assertEquals(-1, touch.dropOffset());
        // la mémoire du tap DEMEURE (le rangement ne l'efface pas :
        // seule la vue y dit le geste qui a grandi)
        now.set(1_100_000_000L);
        assertTrue(touch.tappedTwice(11.0f, 12.0f, SLOP));
        // un contact neuf range tout pareillement
        touch.begin(1.0f, 1.0f);
        assertFalse(touch.isDragging());
        assertTrue(touch.beyondSlop(1.0f + SLOP + 1.0f, 1.0f, SLOP));
    }
}
