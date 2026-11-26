package android.graphics;

/** Le stub harnais : le dégradé linéaire du sélecteur de couleur. */
public final class LinearGradient extends Shader {

    /** Le dégradé à deux couleurs (les bornes et le mode). */
    public LinearGradient(float x0, float y0, float x1, float y1,
            int color0, int color1, TileMode mode) {
    }

    /** Le dégradé à ses couleurs posées (les bornes et le mode). */
    public LinearGradient(float x0, float y0, float x1, float y1,
            int[] colors, float[] positions, TileMode mode) {
    }
}
