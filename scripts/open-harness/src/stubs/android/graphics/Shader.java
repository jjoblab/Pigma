package android.graphics;

/** Le stub harnais : le mode de tuilage des dégradés. */
public class Shader {

    /** Le dégradé s'arrête à ses bornes. */
    public static final Shader.TileMode CLAMP = new Shader.TileMode();


    protected Shader() {
    }

    /** Le mode de tuilage (le CLAMP du sélecteur). */
    public static final class TileMode {

        /** Le mode CLAMP (le dégradé s'arrête à ses bornes). */
        public static final TileMode CLAMP = new TileMode();

        private TileMode() {
        }
    }
}
