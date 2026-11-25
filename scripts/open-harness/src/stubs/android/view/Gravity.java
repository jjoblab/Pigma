package android.view;

/**
 * Les constantes d'ancrage d'une fenêtre flottante — le harnais
 * n'enseigne que le haut (l'endroit des popups de l'éditeur).
 */
public final class Gravity {

    /** La fenêtre se pose en haut de son parent. */
    public static final int TOP = 0x30;

    /** La fenêtre se pose au centre de son parent. */
    public static final int CENTER = 0x11;

    /** Le panneau s'ancre au bord gauche (le menu du tiroir y
     * coulisse). */
    public static final int LEFT = 0x3;

    /** La fenêtre se pose en bas de son parent. */
    public static final int BOTTOM = 0x50;

    /** Aucune gravité posée (le contenu du tiroir). */
    public static final int NO_GRAVITY = 0;

    private Gravity() {
    }
}
