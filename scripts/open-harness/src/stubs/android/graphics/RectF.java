package android.graphics;

/**
 * Rectangle flottant de démonstration : la région destination d'un
 * blit, réutilisable (les bornes s'y reposent — le panneau de la
 * gouttière s'y redessine à la trame sans allocation).
 */
public final class RectF {

    /** Le bord gauche. */
    public float left;
    /** Le bord haut. */
    public float top;
    /** Le bord droit. */
    public float right;
    /** Le bord bas. */
    public float bottom;

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

    /** Un rectangle vide (les bornes se posent ensuite). */
    public RectF() {
        this(0.0f, 0.0f, 0.0f, 0.0f);
    }

    /**
     * Repose les bornes du rectangle.
     *
     * @param left   le bord gauche
     * @param top    le bord haut
     * @param right  le bord droit
     * @param bottom le bord bas
     */
    public void set(float left, float top, float right, float bottom) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }
}
