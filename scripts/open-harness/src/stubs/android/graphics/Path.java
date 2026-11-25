package android.graphics;

/**
 * Chemin de démonstration : les segments s'y posent, rien ne se peint —
 * le triangle d'avertissement et l'ampoule de la gouttière y tracent
 * leurs formes (le harnais ne les lit pas, la compilation suffit).
 */
public final class Path {

    /** Les poses du chemin, pour rien : la mémoire du geste. */
    private int ops;

    /** Un chemin neuf. */
    public Path() {
    }

    /**
     * Pose le départ du chemin.
     *
     * @param x abscisse
     * @param y ordonnée
     */
    public void moveTo(float x, float y) {
        ops++;
    }

    /**
     * Poursuit le chemin d'un segment.
     *
     * @param x abscisse d'arrivée
     * @param y ordonnée d'arrivée
     */
    public void lineTo(float x, float y) {
        ops++;
    }

    /** Ferme le chemin (retour au départ). */
    public void close() {
        ops++;
    }
}
