package android.view;

/**
 * Les constantes d'ancrage d'une fenêtre flottante — le harnais
 * n'enseigne que le haut (l'endroit des popups de l'éditeur).
 */
public final class Gravity {

    /** La fenêtre se pose en haut de son parent. */
    public static final int TOP = 0x30;

    /** La fenêtre s'aligne au bord de fin (la droite, en français). */
    public static final int END = 0x00800005;

    /** La fenêtre se centre horizontalement. */
    public static final int CENTER_HORIZONTAL = 0x00000001;

    /** La fenêtre se pose au centre de son parent. */
    public static final int CENTER = 0x11;

    /** La fenêtre se pose en bas de son parent. */
    public static final int BOTTOM = 0x50;

    private Gravity() {
    }
}
