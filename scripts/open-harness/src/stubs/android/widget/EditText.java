package android.widget;

import android.content.Context;
import android.view.KeyEvent;
import android.view.View;

/**
 * Champ de saisie de démonstration : un texte ordinaire que le
 * harnais pose comme si le clavier l'avait tapé.
 */
public class EditText extends TextView {

    /**
     * L'écoute de la touche d'action du clavier (la touche « Aller »
     * du champ vaut le bouton).
     */
    public interface OnEditorActionListener {

        /**
         * La touche d'action se presse.
         *
         * @param view     le champ
         * @param actionId la touche (IME_ACTION_GO…)
         * @param event    l'événement brut, ou null
         * @return vrai si l'écouteur consomme la touche
         */
        boolean onEditorAction(View view, int actionId, KeyEvent event);
    }

    /** La touche d'action demandée au clavier (IME_ACTION_*). */
    private int imeOptions;

    /** L'écouteur de la touche d'action. */
    private OnEditorActionListener editorActionListener;

    /**
     * Un champ neuf, vide.
     *
     * @param context le contexte porteur
     */
    public EditText(Context context) {
        super(context);
    }

    /**
     * Pose la touche d'action du clavier (IME_ACTION_GO, DONE…).
     *
     * @param options la touche demandée
     */
    public void setImeOptions(int options) {
        this.imeOptions = options;
    }

    /** La touche d'action posée (le harnais la lit). */
    public int getImeOptions() {
        return imeOptions;
    }

    /**
     * Attache l'écouteur de la touche d'action.
     *
     * @param listener l'écouteur (null détache)
     */
    public void setOnEditorActionListener(OnEditorActionListener listener) {
        this.editorActionListener = listener;
    }

    /**
     * Simule la pression de la touche d'action (le harnais).
     *
     * @return vrai si un écouteur l'a consommée
     */
    public boolean harnessEditorAction() {
        if (editorActionListener == null) {
            return false;
        }
        return editorActionListener.onEditorAction(this, imeOptions, null);
    }
}
