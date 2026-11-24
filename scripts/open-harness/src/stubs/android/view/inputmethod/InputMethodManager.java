package android.view.inputmethod;

import android.view.View;

/**
 * Gestionnaire de méthode de saisie de démonstration : les demandes
 * d'affichage se comptent — le clavier ne se montre jamais.
 */
public class InputMethodManager {

    private int showCount;

    /**
     * Demande l'affichage du clavier pour la vue.
     *
     * @param view la vue qui demande
     * @param flags drapeaux (sans effet ici)
     * @return vrai si accepté
     */
    public boolean showSoftInput(View view, int flags) {
        showCount++;
        return true;
    }

    /** Le nombre de demandes d'affichage reçues. */
    public int harnessShowCount() {
        return showCount;
    }
}
