package android.text;

/**
 * L'écouteur des changements du texte d'un champ : le harnais s'en
 * sert pour la recherche incrémentale (chaque frappe du champ y
 * relance la recherche, comme sur l'appareil).
 */
public interface TextWatcher {

    /**
     * Avant le remplacement (les arguments du harnais portent les
     * longueurs de l'ancien texte et du nouveau).
     *
     * @param s         le texte avant
     * @param start     le début du remplacement
     * @param count     la longueur remplacée
     * @param after     la longueur qui suit
     */
    void beforeTextChanged(CharSequence s, int start, int count, int after);

    /**
     * Après le remplacement.
     *
     * @param s         le texte après
     * @param start     le début du remplacement
     * @param before    la longueur remplacée
     * @param count     la longueur qui suit
     */
    void onTextChanged(CharSequence s, int start, int before, int count);

    /**
     * Après le changement (le texte livré est modifiable).
     *
     * @param s le texte après, modifiable
     */
    void afterTextChanged(Editable s);
}
