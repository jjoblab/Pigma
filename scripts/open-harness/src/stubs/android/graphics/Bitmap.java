package android.graphics;

/**
 * Bitmap de démonstration : la loupe y rend sa ligne une fois (le
 * rendu logiciel du champ réel), la trame la blitte à l'échelle.
 * Au harnais, le bitmap est un marqueur — les opérations qui y
 * dessinent se comptent sur son canvas propre.
 */
public final class Bitmap {

    /** La configuration des pixels (l'ordre canonique du champ réel). */
    public enum Config {
        /** Huit bits par canal, alpha compris. */
        ARGB_8888
    }

    private final int width;
    private final int height;

    private Bitmap(int width, int height) {
        this.width = width;
        this.height = height;
    }

    /**
     * Un bitmap neuf aux dimensions données.
     *
     * @param width  la largeur en pixels, au moins 1
     * @param height la hauteur en pixels, au moins 1
     * @param config la configuration (jamais null)
     * @return le bitmap
     */
    public static Bitmap createBitmap(int width, int height, Config config) {
        if (width <= 0 || height <= 0 || config == null) {
            throw new IllegalArgumentException("invalid bitmap: "
                    + width + "x" + height);
        }
        return new Bitmap(width, height);
    }

    /** La largeur en pixels. */
    public int getWidth() {
        return width;
    }

    /** La hauteur en pixels. */
    public int getHeight() {
        return height;
    }
}
