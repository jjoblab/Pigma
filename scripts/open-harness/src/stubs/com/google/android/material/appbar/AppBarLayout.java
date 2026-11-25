package com.google.android.material.appbar;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

/**
 * Barre d'application de démonstration : la barre du menu et les
 * onglets s'y posent, aucune mesure réelle (le harnais dimensionne
 * la vue qu'il éprouve — le cadre réel porte la disposition et
 * l'épinglage au sommet du contenu).
 */
public class AppBarLayout extends LinearLayout {

    /**
     * Paramètres de disposition de la barre d'application : le
     * défilement y règle (accepté, rangé sans effet au harnais).
     */
    public static class LayoutParams extends LinearLayout.LayoutParams {

        /** Le comportement au défilement (l'épinglage du cadre). */
        public int scrollFlags;

        /**
         * Des paramètres de barre d'application.
         *
         * @param width largeur demandée
         * @param height hauteur demandée
         */
        public LayoutParams(int width, int height) {
            super(width, height);
        }
    }

    /**
     * Une barre d'application neuve, vide.
     *
     * @param context le contexte porteur
     */
    public AppBarLayout(Context context) {
        super(context);
        setOrientation(VERTICAL);
    }
}
