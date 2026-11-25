package com.google.android.material.bottomsheet;

import android.view.View;

import androidx.coordinatorlayout.widget.CoordinatorLayout;

/**
 * Comportement de feuille basse de démonstration : l'état, la
 * hauteur de aperçu et les drapeaux se rangent, les changements
 * d'état préviennent l'auditeur — aucune glisse réelle (le harnais
 * dimensionne la vue qu'il éprouve, le cadre réel porte le geste et
 * la translation).
 *
 * @param <V> le type du panneau porté (une ViewGroup de l'écran)
 */
public final class BottomSheetBehavior<V extends View> extends CoordinatorLayout.Behavior {

    /** Les états de la feuille basse (les constantes du cadre). */
    public static final int STATE_EXPANDED = 3;

    /** L'état demi-plié. */
    public static final int STATE_HALF_EXPANDED = 6;

    /** L'état replié (seule la hauteur d'aperçu dépasse). */
    public static final int STATE_COLLAPSED = 4;

    /** L'état caché. */
    public static final int STATE_HIDDEN = 5;

    /** L'auditeur des changements d'état. */
    public interface BottomSheetCallback {

        /**
         * L'état de la feuille vient de changer.
         *
         * @param view     le panneau de la feuille
         * @param newState le nouvel état
         */
        void onStateChanged(View view, int newState);
    }

    /** L'état courant (demi-plié par défaut, celui de l'écran). */
    private int state = STATE_HALF_EXPANDED;

    /** La hauteur d'aperçu en pixels (le replié la montre seul). */
    private int peekHeight;

    /** Le drapeau « l'état replié existe » (faux : sauté). */
    private boolean skipCollapsed;

    /** L'auditeur des changements (null : aucun). */
    private BottomSheetCallback callback;

    /** L'état courant de la feuille. */
    public int getState() {
        return state;
    }

    /**
     * Pose l'état de la feuille : l'auditeur y est prévenu.
     *
     * @param newState le nouvel état
     */
    public void setState(int newState) {
        state = newState;
        if (callback != null) {
            callback.onStateChanged(null, newState);
        }
    }

    /** La hauteur d'aperçu posée. */
    public int getPeekHeight() {
        return peekHeight;
    }

    /**
     * Pose la hauteur d'aperçu.
     *
     * @param height la hauteur en pixels
     */
    public void setPeekHeight(int height) {
        peekHeight = height;
    }

    /** L'état replié est-il sauté ? */
    public boolean getSkipCollapsed() {
        return skipCollapsed;
    }

    /**
     * Pose le drapeau « l'état replié existe ».
     *
     * @param skip vrai pour sauter l'état replié
     */
    public void setSkipCollapsed(boolean skip) {
        skipCollapsed = skip;
    }

    /**
     * Pose l'auditeur des changements d'état.
     *
     * @param sheetCallback l'auditeur (null : aucun)
     */
    public void setBottomSheetCallback(BottomSheetCallback sheetCallback) {
        callback = sheetCallback;
    }
}
