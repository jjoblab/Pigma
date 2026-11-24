package android.graphics;

/**
 * Rectangle entier de démonstration : la région source d'un blit.
 */
public final class Rect {

    /** Le bord gauche. */
    public final int left;
    /** Le bord haut. */
    public final int top;
    /** Le bord droit. */
    public final int right;
    /** Le bord bas. */
    public final int bottom;

    /**
     * Un rectangle aux bornes données.
     *
     * @param left   le bord gauche
     * @param top    le bord haut
     * @param right  le bord droit
     * @param bottom le bord bas
     */
    public Rect(int left, int top, int right, int bottom) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }
}
