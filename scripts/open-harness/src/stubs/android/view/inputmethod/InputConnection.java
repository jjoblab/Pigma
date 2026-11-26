package android.view.inputmethod;

import android.view.KeyEvent;

/**
 * Canal d'édition du clavier : les méthodes que l'éditeur traduit
 * (commit, composition, suppression, touches, actions, sélection).
 */
public interface InputConnection {

    /**
     * Un texte arrive d'un coup.
     *
     * @param text le texte
     * @param newCursorPosition le recul du curseur depuis la fin
     * @return vrai si consommé
     */
    boolean commitText(CharSequence text, int newCursorPosition);

    /**
     * Un texte en composition arrive (remplace la zone en cours).
     *
     * @param text le texte
     * @param newCursorPosition le recul du curseur depuis la fin
     * @return vrai si consommé
     */
    boolean setComposingText(CharSequence text, int newCursorPosition);

    /**
     * Supprime autour du curseur.
     *
     * @param leftLength caractères avant le curseur
     * @param rightLength caractères après le curseur
     * @return vrai si consommé
     */
    boolean deleteSurroundingText(int leftLength, int rightLength);

    /** La composition se termine (telle quelle). */
    boolean finishComposingText();

    /**
     * Une touche passe par les événements.
     *
     * @param event l'événement de touche
     * @return vrai si consommé
     */
    boolean sendKeyEvent(KeyEvent event);

    /**
     * La touche action du clavier.
     *
     * @param actionCode le code d'action
     * @return vrai si consommé
     */
    boolean performEditorAction(int actionCode);

    /**
     * Le clavier déplace la sélection.
     *
     * @param start début
     * @param end fin
     * @return vrai si consommé
     */
    boolean setSelection(int start, int end);

    /**
     * Le texte sélectionné (null si aucune sélection).
     *
     * @param flags drapeaux (sans effet ici)
     * @return le texte sélectionné
     */
    CharSequence getSelectedText(int flags);

    /**
     * Le texte avant le curseur (la prédiction du clavier s'y
     * nourrit).
     *
     * @param length le nombre de caractères demandés
     * @param flags  drapeaux (sans effet ici)
     * @return le texte avant le curseur
     */
    CharSequence getTextBeforeCursor(int length, int flags);

    /**
     * Le texte après le curseur.
     *
     * @param length le nombre de caractères demandés
     * @param flags  drapeaux (sans effet ici)
     * @return le texte après le curseur
     */
    CharSequence getTextAfterCursor(int length, int flags);

    /**
     * La zone de composition se repose (le clavier y surligne ce
     * qu'il corrige).
     *
     * @param start le début de la zone
     * @param end   la fin de la zone
     * @return vrai si consommé
     */
    boolean setComposingRegion(int start, int end);

    /**
     * Les majuscules contextuelles (le clavier y devine la phrase).
     *
     * @param reqModes les modes demandés
     * @return les modes satisfaits
     */
    int getCursorCapsMode(int reqModes);
}
