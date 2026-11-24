package android.widget;

import android.view.ViewGroup;

/**
 * Rangée ou colonne de démonstration : l'orientation et les poids
 * se retiennent, aucune mesure ni disposition.
 */
public class LinearLayout extends ViewGroup {

    /** Les enfants en rangée. */
    public static final int HORIZONTAL = 0;

    /** Les enfants en colonne. */
    public static final int VERTICAL = 1;

    private int orientation = HORIZONTAL;

    /**
     * Un groupe neuf, en rangée.
     *
     * @param context le contexte porteur
     */
    public LinearLayout(android.content.Context context) {
        super(context);
    }

    /**
     * Pose l'orientation.
     *
     * @param orientation HORIZONTAL ou VERTICAL
     */
    public void setOrientation(int orientation) {
        this.orientation = orientation;
    }

    /** L'orientation courante. */
    public int getOrientation() {
        return orientation;
    }

    /**
     * Paramètres de disposition linéaire : dimensions, marges et
     * poids de partage de la place.
     */
    public static class LayoutParams extends ViewGroup.MarginLayoutParams {

        /** Le poids de partage de la place restante. */
        public float weight;

        /**
         * Des paramètres sans poids (poids nul).
         *
         * @param width largeur demandée
         * @param height hauteur demandée
         */
        public LayoutParams(int width, int height) {
            this(width, height, 0.0f);
        }

        /**
         * Des paramètres pondérés.
         *
         * @param width largeur demandée
         * @param height hauteur demandée
         * @param weight poids de partage
         */
        public LayoutParams(int width, int height, float weight) {
            super(width, height);
            this.weight = weight;
        }
    }
}
