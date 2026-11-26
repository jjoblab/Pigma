package android.text;

/**
 * Les utilitaires de texte du cadre (le harnais à stubs n'en tient
 * que les constantes de capitalisation — la connexion du clavier y
 * masque ses modes).
 */
public final class TextUtils {

    /** La majuscule en tête de phrase. */
    public static final int CAP_MODE_FIRST_CHAR = 0x2000;

    /** La majuscule en tête de mot. */
    public static final int CAP_MODE_WORDS = 0x2000;

    /** La majuscule en tête de phrase (après un point). */
    public static final int CAP_MODE_SENTENCES = 0x4000;

    private TextUtils() {
    }
}
