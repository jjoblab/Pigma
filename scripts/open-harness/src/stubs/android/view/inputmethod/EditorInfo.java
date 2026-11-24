package android.view.inputmethod;

/**
 * Attributs d'édition de démonstration : deux champs publics que la
 * vue remplit à la création de la connexion.
 */
public class EditorInfo {

    /** Le clavier reste dans la fenêtre (pas de plein écran). */
    public static final int IME_FLAG_NO_FULLSCREEN = 0x02000000;

    /** Le type de saisie demandé. */
    public int inputType;

    /** Les options de méthode de saisie. */
    public int imeOptions;
}
