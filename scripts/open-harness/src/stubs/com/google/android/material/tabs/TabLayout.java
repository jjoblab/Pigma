package com.google.android.material.tabs;

import android.content.Context;
import android.widget.LinearLayout;

/**
 * Barre d'onglets de démonstration : les onglets s'y posent, le
 * choix déclenche l'auditeur — aucune mesure réelle (le harnais
 * dimensionne la vue qu'il éprouve, le cadre réel porte la
 * disposition des onglets).
 */
public class TabLayout extends LinearLayout {

    /** L'auditeur des choix d'onglet (l'interface publique du cadre). */
    public interface OnTabSelectedListener {

        /**
         * L'onglet vient d'être choisi.
         *
         * @param tab l'onglet choisi
         */
        void onTabSelected(Tab tab);

        /**
         * L'onglet quitte le choix.
         *
         * @param tab l'onglet déchu
         */
        void onTabUnselected(Tab tab);

        /**
         * L'onglet choisi l'est de nouveau.
         *
         * @param tab l'onglet rechoisi
         */
        void onTabReselected(Tab tab);
    }

    /** Un onglet de la barre. */
    public static final class Tab {

        /** La barre porteuse. */
        private final TabLayout owner;

        /** L'étiquette de l'onglet. */
        private CharSequence label;

        /** La position de l'onglet dans la barre. */
        private int position;

        private Tab(TabLayout owner) {
            this.owner = owner;
        }

        /** L'étiquette de l'onglet. */
        public CharSequence getText() {
            return label;
        }

        /** La position de l'onglet. */
        public int getPosition() {
            return position;
        }

        /**
         * Pose l'étiquette de l'onglet.
         *
         * @param text l'étiquette
         * @return l'onglet (l'appel chaîne)
         */
        public Tab setText(CharSequence text) {
            label = text;
            return this;
        }

        /**
         * Choisit l'onglet (la barre y prévient son auditeur).
         *
         * @return l'onglet (l'appel chaîne)
         */
        public Tab select() {
            owner.select(this);
            return this;
        }
    }

    /** L'auditeur des choix (null : aucun). */
    private OnTabSelectedListener listener;

    /**
     * Une barre d'onglets neuve, vide.
     *
     * @param context le contexte porteur
     */
    public TabLayout(Context context) {
        super(context);
        setOrientation(HORIZONTAL);
    }

    /**
     * Pose l'auditeur des choix d'onglet.
     *
     * @param tabListener l'auditeur (null : aucun)
     */
    public void addOnTabSelectedListener(OnTabSelectedListener tabListener) {
        listener = tabListener;
    }

    /**
     * Un onglet neuf, non posé.
     *
     * @return l'onglet vide
     */
    public Tab newTab() {
        return new Tab(this);
    }

    /**
     * Pose un onglet en fin de barre : sa position s'y calcule, le
     * tableau des onglets s'y agrandit (le harnais y retrouve ses
     * onglets).
     *
     * @param tab l'onglet
     * @return l'onglet posé
     */
    public Tab addTab(Tab tab) {
        tab.position = tabCount;
        Tab[] grown = new Tab[tabCount + 1];
        System.arraycopy(tabs, 0, grown, 0, tabCount);
        grown[tabCount] = tab;
        tabs = grown;
        tabCount++;
        return tab;
    }

    /** Le nombre d'onglets posés. */
    public int getTabCount() {
        return tabCount;
    }

    /** Le compteur des onglets posés (le harnais y range). */
    private int tabCount;

    /**
     * L'onglet d'indice donné.
     *
     * @param index l'indice
     * @return l'onglet, ou null si hors barre
     */
    public Tab getTabAt(int index) {
        return (index >= 0 && index < getTabCount()) ? tabs[index] : null;
    }

    /** Les onglets posés (la taille suit le compteur). */
    private Tab[] tabs = new Tab[0];

    /** L'onglet choisi (null : aucun). */
    private Tab selected;

    /**
     * L'indice de l'onglet choisi (le cadre y répond -1 à la
     * première ouverture, avant tout choix).
     */
    public int getSelectedTabPosition() {
        return selected == null ? -1 : selected.getPosition();
    }

    /**
     * Choisit un onglet par programme : l'auditeur y est prévenu.
     *
     * @param tab l'onglet à choisir
     */
    private void select(Tab tab) {
        selected = tab;
        if (listener != null && tab != null) {
            listener.onTabSelected(tab);
        }
    }
}
