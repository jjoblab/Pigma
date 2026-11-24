package android.text;

/**
 * Types de saisie de démonstration : deux drapeaux, les valeurs
 * n'importent que pour les distinguer.
 */
public final class InputType {

    /** Une classe de saisie texte. */
    public static final int TYPE_CLASS_TEXT = 0x00000001;

    /** Une classe de saisie nombre (le champ aller-à-la-ligne). */
    public static final int TYPE_CLASS_NUMBER = 0x00000002;

    /** Texte multi-lignes. */
    public static final int TYPE_TEXT_FLAG_MULTI_LINE = 0x00020000;

    private InputType() {
    }
}
