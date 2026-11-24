package android.view;

import java.util.ArrayList;
import java.util.List;

/**
 * Groupe de vues de démonstration : les enfants se rangent en liste
 * dans l'ordre d'ajout — aucune mesure, aucune disposition, le
 * harnais dimensionne la vue qu'il éprouve.
 */
public class ViewGroup extends View {

    /**
     * Paramètres de disposition : dimensions demandées.
     */
    public static class LayoutParams {

        /** La taille épouse le parent. */
        public static final int MATCH_PARENT = -1;

        /** La taille épouse le contenu. */
        public static final int WRAP_CONTENT = -2;

        /** Largeur demandée. */
        public int width;

        /** Hauteur demandée. */
        public int height;

        /**
         * Des paramètres de disposition.
         *
         * @param width largeur demandée
         * @param height hauteur demandée
         */
        public LayoutParams(int width, int height) {
            this.width = width;
            this.height = height;
        }
    }

    /**
     * Paramètres avec marges extérieures.
     */
    public static class MarginLayoutParams extends LayoutParams {

        /** Marge extérieure gauche. */
        public int leftMargin;

        /** Marge extérieure haute. */
        public int topMargin;

        /** Marge extérieure droite. */
        public int rightMargin;

        /** Marge extérieure basse. */
        public int bottomMargin;

        /**
         * Des paramètres à marges nulles.
         *
         * @param width largeur demandée
         * @param height hauteur demandée
         */
        public MarginLayoutParams(int width, int height) {
            super(width, height);
        }

        /**
         * Pose les marges extérieures.
         *
         * @param left gauche
         * @param top haut
         * @param right droite
         * @param bottom bas
         */
        public void setMargins(int left, int top, int right, int bottom) {
            leftMargin = left;
            topMargin = top;
            rightMargin = right;
            bottomMargin = bottom;
        }
    }

    private final List<View> children = new ArrayList<>();

    /**
     * Un groupe neuf.
     *
     * @param context le contexte porteur
     */
    public ViewGroup(android.content.Context context) {
        super(context);
    }

    /**
     * Ajoute un enfant sans paramètres.
     *
     * @param child l'enfant
     */
    public void addView(View child) {
        children.add(child);
        child.harnessSetParent(this);
    }

    /**
     * Ajoute un enfant avec ses paramètres de disposition (les
     * paramètres se rangent, aucun effet de mesure).
     *
     * @param child l'enfant
     * @param params paramètres de disposition
     */
    public void addView(View child, LayoutParams params) {
        children.add(child);
        child.harnessSetParent(this);
    }

    /** Le nombre d'enfants. */
    public int getChildCount() {
        return children.size();
    }

    /**
     * L'enfant d'indice donné.
     *
     * @param index l'indice
     * @return l'enfant
     */
    public View getChildAt(int index) {
        return children.get(index);
    }

    /**
     * Attache le groupe et ses enfants récursivement.
     */
    @Override
    public void harnessAttach() {
        super.harnessAttach();
        for (View child : children) {
            child.harnessAttach();
        }
    }
}
