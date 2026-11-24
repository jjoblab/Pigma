package android.widget;

import android.view.Gravity;
import android.view.View;

/**
 * Fenêtre flottante de démonstration : elle pose son contenu au-dessus
 * d'un parent, retient ses réglages et prévient sa fermeture — le
 * harnais y vérifie les popups de l'éditeur sans fenêtre réelle.
 */
public class PopupWindow {

    /**
     * L'écoute de fermeture (la recherche se range avec la popup).
     */
    public interface OnDismissListener {

        /** La fenêtre se ferme. */
        void onDismiss();
    }

    private final View content;
    private final int height;
    private final boolean focusable;
    private int width;
    private boolean outsideTouchable;
    private OnDismissListener dismissListener;
    private boolean showing;
    private int gravity;

    /** La dernière fenêtre ouverte (le harnais y lit la barre portée). */
    private static PopupWindow lastShown;

    /**
     * Une fenêtre neuve, close.
     *
     * @param contentView le contenu porté
     * @param width       la largeur demandée
     * @param height      la hauteur demandée
     * @param focusable   vrai : la fenêtre prend le focus (le clavier
     *                    lui revient)
     */
    public PopupWindow(View contentView, int width, int height, boolean focusable) {
        this.content = contentView;
        this.width = width;
        this.height = height;
        this.focusable = focusable;
    }

    /**
     * Pose la largeur (la popup s'ouvre à la largeur de l'éditeur).
     *
     * @param newWidth la largeur en pixels
     */
    public void setWidth(int newWidth) {
        this.width = newWidth;
    }

    /**
     * L'appui hors de la fenêtre la range-t-il ?
     *
     * @param touchable vrai : l'appui dehors ferme
     */
    public void setOutsideTouchable(boolean touchable) {
        this.outsideTouchable = touchable;
    }

    /**
     * Attache l'écoute de fermeture.
     *
     * @param listener l'écouteur (null détache)
     */
    public void setOnDismissListener(OnDismissListener listener) {
        this.dismissListener = listener;
    }

    /** La fenêtre vit-elle ? */
    public boolean isShowing() {
        return showing;
    }

    /**
     * Ouvre la fenêtre au-dessus de son parent.
     *
     * @param parent  la vue d'ancrage
     * @param gravity l'ancrage (Gravity.TOP…)
     * @param x       le décalage horizontal
     * @param y       le décalage vertical
     */
    public void showAtLocation(View parent, int gravity, int x, int y) {
        this.gravity = gravity;
        this.showing = true;
        lastShown = this;
    }

    /** La dernière fenêtre ouverte (le harnais y lit la barre portée). */
    public static PopupWindow harnessLast() {
        return lastShown;
    }

    /** Ferme la fenêtre (l'écoute part avec elle). */
    public void dismiss() {
        if (!showing) {
            return;
        }
        showing = false;
        if (dismissListener != null) {
            dismissListener.onDismiss();
        }
    }

    /** Le contenu porté (le harnais y lit la barre). */
    public View harnessContent() {
        return content;
    }

    /** L'ancrage de la dernière ouverture (le harnais le lit). */
    public int harnessGravity() {
        return gravity;
    }

    /** La largeur posée (le harnais la lit). */
    public int harnessWidth() {
        return width;
    }

    /** La hauteur posée (le harnais la lit). */
    public int harnessHeight() {
        return height;
    }

    /** La fenêtre prend-elle le focus ? (le harnais le lit) */
    public boolean harnessFocusable() {
        return focusable;
    }

    /** L'appui dehors ferme-t-il ? (le harnais le lit) */
    public boolean harnessOutsideTouchable() {
        return outsideTouchable;
    }
}
