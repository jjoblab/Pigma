package android.graphics;

/**
 * Police de démonstration : trois instances nommées, aucune métrique
 * réelle — les tailles vivent dans le pinceau.
 */
public final class Typeface {

    /** La police à espacement fixe (l'éditeur de code la demande). */
    public static final Typeface MONOSPACE = new Typeface("monospace");

    /** La police ordinaire. */
    public static final Typeface DEFAULT = new Typeface("default");

    /** La police ordinaire en gras. */
    public static final Typeface DEFAULT_BOLD = new Typeface("default-bold");

    private final String name;

    private Typeface(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
