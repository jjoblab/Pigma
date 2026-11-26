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
     * La copie d'un rectangle (la pose courante se rend sans son
     * original — le harnais y vise le doigt sans toucher à la pose).
     *
     * @param other le rectangle copié
     */
    public RectF(RectF other) {
        this(other.left, other.top, other.right, other.bottom);
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

    /** La largeur du rectangle (bord droit moins bord gauche). */
    public float width() {
        return right - left;
    }

    /** La hauteur du rectangle (bord bas moins bord haut). */
    public float height() {
        return bottom - top;
    }

    /** L'abscisse du centre du rectangle. */
    public float centerX() {
        return (left + right) / 2.0f;
    }

    /** L'ordonnée du centre du rectangle. */
    public float centerY() {
        return (top + bottom) / 2.0f;
    }

    /**
     * Le point vit-il dans le rectangle (les bords droit et bas
     * exclus — la convention du cadre) ?
     *
     * @param x l'abscisse du point
     * @param y l'ordonnée du point
     * @return vrai quand le point s'y trouve
     */
    public boolean contains(float x, float y) {
        return x >= left && x < right && y >= top && y < bottom;
    }
}
