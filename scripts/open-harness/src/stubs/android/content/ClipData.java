package android.content;

/**
 * Clip de démonstration : une étiquette et un texte, le presse-
 * papiers les porte.
 */
public final class ClipData {

    private final CharSequence label;
    private final CharSequence text;

    private ClipData(CharSequence label, CharSequence text) {
        this.label = label;
        this.text = text;
    }

    /**
     * Un clip de texte brut.
     *
     * @param label étiquette du clip
     * @param text  contenu
     * @return le clip neuf
     */
    public static ClipData newPlainText(CharSequence label, CharSequence text) {
        return new ClipData(label, text);
    }

    /** L'étiquette du clip. */
    public CharSequence getLabel() {
        return label;
    }

    /** Le contenu du clip. */
    public CharSequence getText() {
        return text;
    }
}
