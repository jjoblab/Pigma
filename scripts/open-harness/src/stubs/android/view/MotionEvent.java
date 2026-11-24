package android.view;

/**
 * Événement tactile de démonstration : action et position — le
 * harnais en fabrique pour éprouver les gestes de la vue.
 */
public final class MotionEvent {

    /** Le doigt se pose. */
    public static final int ACTION_DOWN = 0;

    /** Le doigt se lève. */
    public static final int ACTION_UP = 1;

    /** Le doigt glisse. */
    public static final int ACTION_MOVE = 2;

    /** Le geste se rompt. */
    public static final int ACTION_CANCEL = 3;

    private final int action;
    private final float x;
    private final float y;

    private MotionEvent(int action, float x, float y) {
        this.action = action;
        this.x = x;
        this.y = y;
    }

    /**
     * Fabrique un événement tactile.
     *
     * @param action l'action (DOWN, MOVE, UP, CANCEL)
     * @param x abscisse en pixels
     * @param y ordonnée en pixels
     * @return l'événement
     */
    public static MotionEvent obtain(int action, float x, float y) {
        return new MotionEvent(action, x, y);
    }

    /** L'action masquée. */
    public int getActionMasked() {
        return action;
    }

    /** L'abscisse du contact. */
    public float getX() {
        return x;
    }

    /** L'ordonnée du contact. */
    public float getY() {
        return y;
    }
}
