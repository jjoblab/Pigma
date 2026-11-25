package android.view;

import java.util.ArrayList;
import java.util.List;

/**
 * Événement tactile de démonstration : action et position — le
 * harnais en fabrique pour éprouver les gestes de la vue. Le
 * multi-touch vit ici aussi : les événements portent leurs doigts
 * (le pincement du zoom s'y éprouve), l'appui d'un seul doigt garde
 * sa voie courte (obtain à trois arguments, position du doigt zéro).
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

    /** Un doigt de plus se pose (le pincement commence). */
    public static final int ACTION_POINTER_DOWN = 26;

    /** Un doigt se lève (le pincement peut continuer). */
    public static final int ACTION_POINTER_UP = 27;

    /** Le masque de l'indice du doigt de l'action. */
    public static final int ACTION_POINTER_INDEX_MASK = 0xFF00;

    /** Le décalage de l'indice du doigt de l'action. */
    public static final int ACTION_POINTER_INDEX_SHIFT = 8;

    /** Les doigts vivants (abscisse, ordonnée par indice). */
    private final float[] xs;

    /** Les ordonnées des doigts vivants (par indice). */
    private final float[] ys;

    /** L'action de l'événement (le doigt seul porte son indice). */
    private final int action;

    private MotionEvent(int action, float[] xs, float[] ys) {
        this.action = action;
        this.xs = xs;
        this.ys = ys;
    }

    /**
     * Fabrique un événement tactile d'un seul doigt.
     *
     * @param action l'action (DOWN, MOVE, UP, CANCEL)
     * @param x abscisse en pixels
     * @param y ordonnée en pixels
     * @return l'événement
     */
    public static MotionEvent obtain(int action, float x, float y) {
        return new MotionEvent(action, new float[] {x}, new float[] {y});
    }

    /**
     * Fabrique un événement tactile multi-doigts : le premier indice
     * du tableau est le doigt zéro.
     *
     * @param action l'action (POINTER_DOWN et POINTER_UP portent
     *               l'indice dans les bits hauts — zéro pour les
     *               actions simples)
     * @param xs les abscisses des doigts vivants
     * @param ys les ordonnées des doigts vivants
     * @return l'événement
     */
    public static MotionEvent obtain(int action, float[] xs, float[] ys) {
        return new MotionEvent(action, xs.clone(), ys.clone());
    }

    /** L'action brute (l'indice du doigt y vit pour POINTER_DOWN/UP). */
    public int getAction() {
        return action;
    }

    /** L'action masquée (sans l'indice du doigt). */
    public int getActionMasked() {
        return action & 0xFF;
    }

    /** L'indice du doigt de l'action (POINTER_DOWN/UP). */
    public int getActionIndex() {
        return (action & ACTION_POINTER_INDEX_MASK) >> ACTION_POINTER_INDEX_SHIFT;
    }

    /** Le nombre de doigts vivants. */
    public int getPointerCount() {
        return xs.length;
    }

    /** L'abscisse du contact premier. */
    public float getX() {
        return xs[0];
    }

    /** L'ordonnée du contact premier. */
    public float getY() {
        return ys[0];
    }

    /**
     * L'abscisse d'un doigt par son indice.
     *
     * @param pointerIndex l'indice du doigt
     * @return l'abscisse en pixels
     */
    public float getX(int pointerIndex) {
        return xs[pointerIndex];
    }

    /**
     * L'ordonnée d'un doigt par son indice.
     *
     * @param pointerIndex l'indice du doigt
     * @return l'ordonnée en pixels
     */
    public float getY(int pointerIndex) {
        return ys[pointerIndex];
    }
}
