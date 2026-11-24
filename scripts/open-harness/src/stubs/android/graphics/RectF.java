package android.graphics;

/**
 * Rectangle flottant de démonstration : la région destination d'un
 * blit.
 */
public final class RectF {

    /** Le bord gauche. */
    public final float left;
    /** Le bord haut. */
    public final float top;
    /** Le bord droit. */
    public final float right;
    /** Le bord bas. */
    public final float bottom;

    /**
     * Un rectangle aux bornes données.
     *
     * @param left   le bord gauche
     * @param top    le bord haut
     * @param right  le bord droit
     * @param bottom le bord bas
     */
    public RectF(float left, float top, float right, float bottom) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }
}
