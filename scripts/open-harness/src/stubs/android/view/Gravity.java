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

    /** La fenêtre se pose en bas de son parent. */
    public static final int BOTTOM = 0x50;

    private Gravity() {
    }
}
