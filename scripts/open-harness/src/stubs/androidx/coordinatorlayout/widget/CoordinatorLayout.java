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

    /** Un comportement de coordinateur (la feuille basse en pose un). */
    public static class Behavior {

        /** Un comportement neuf, sans effet au harnais. */
        public Behavior() {
        }
    }

    /**
     * Paramètres de disposition du coordinateur : le comportement
     * s'y règle (l'attachement réel de la feuille basse — le
     * harnais le range, l'appareil le vit).
     */
    public static class LayoutParams extends ViewGroup.MarginLayoutParams {

        /** Le comportement posé (null : aucun). */
        private Behavior behavior;

        /**
         * Des paramètres de coordinateur.
         *
         * @param width largeur demandée
         * @param height hauteur demandée
         */
        public LayoutParams(int width, int height) {
            super(width, height);
        }

        /** Le comportement posé (null : aucun). */
        public Behavior getBehavior() {
            return behavior;
        }

        /**
         * Pose le comportement du panneau.
         *
         * @param newBehavior le comportement (null : aucun)
         */
        public void setBehavior(Behavior newBehavior) {
            behavior = newBehavior;
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
