package androidx.coordinatorlayout.widget;

import android.content.Context;
import android.view.ViewGroup;

/**
 * Coordinateur de démonstration : le contenu et la feuille basse
 * s'y posent, aucun comportement réel ne s'y attache (le harnais
 * dimensionne la vue qu'il éprouve — le cadre réel y attache la
 * feuille, l'app-bar s'y épinglant au défilement).
 */
public class CoordinatorLayout extends ViewGroup {

    /**
     * Paramètres de disposition du coordinateur : le comportement
     * s'y règle (accepté, rangé sans effet au harnais).
     */
    public static class LayoutParams extends ViewGroup.MarginLayoutParams {

        /**
         * Des paramètres de coordinateur.
         *
         * @param width largeur demandée
         * @param height hauteur demandée
         */
        public LayoutParams(int width, int height) {
            super(width, height);
        }
    }

    /**
     * Un coordinateur neuf, vide.
     *
     * @param context le contexte porteur
     */
    public CoordinatorLayout(Context context) {
        super(context);
    }
}
