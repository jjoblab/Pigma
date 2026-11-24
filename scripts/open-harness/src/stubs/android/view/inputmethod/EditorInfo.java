package android.view.inputmethod;

/**
 * Attributs d'édition de démonstration : deux champs publics que la
 * vue remplit à la création de la connexion.
 */
public class EditorInfo {

    /** Le clavier reste dans la fenêtre (pas de plein écran). */
    public static final int IME_FLAG_NO_FULLSCREEN = 0x02000000;

    /** La touche d'action « Aller » (le champ aller-à-la-ligne). */
    public static final int IME_ACTION_GO = 0x00000002;

    /** Le type de saisie demandé. */
    public int inputType;

    /** Les options de méthode de saisie. */
    public int imeOptions;
}
