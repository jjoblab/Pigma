package android.os;

/**
 * Le construit du système du stub : les architectures supportées s'y
 * lisent (le message du serveur absent les cite sur l'appareil).
 */
public final class Build {

    /** Les architectures natives de l'appareil, la préférée d'abord. */
    public static final String[] SUPPORTED_ABIS = {"x86_64"};
}
