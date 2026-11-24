package android.content;

/**
 * Presse-papiers de démonstration : le dernier clip se retient — le
 * harnais y lit le texte copié.
 */
public class ClipboardManager {

    private ClipData primaryClip;

    /**
     * Pose le clip courant.
     *
     * @param clip le clip
     */
    public void setPrimaryClip(ClipData clip) {
        primaryClip = clip;
    }

    /** Le dernier clip posé (null si aucun). */
    public ClipData getPrimaryClip() {
        return primaryClip;
    }
}
