package android.os;

/**
 * Le construit du système du stub : les architectures supportées s'y
 * lisent (le message du serveur absent les cite sur l'appareil).
 */
public final class Build {

    /** Les architectures natives de l'appareil, la préférée d'abord. */
    public static final String[] SUPPORTED_ABIS = {"x86_64"};

    /** Le fabricant de l'appareil (une constante de lecture au harnais). */
    public static final String MANUFACTURER = "harnais";

    /** Le modèle de l'appareil (une constante de lecture au harnais). */
    public static final String MODEL = "stub";

    /** La version du cadre (le harnais y lit la sienne). */
    public static final class VERSION {

        /** La version lisible du cadre. */
        public static final String RELEASE = "0-stub";

        /** Le niveau d'API du cadre. */
        public static final int SDK_INT = 0;
    }
}
