package android.os;

/**
 * Boucle de message de démonstration : la boucle PRINCIPALE seule
 * vit (le harnais n'en fait qu'une) — le faux moteur de
 * démonstration y pose ses morceaux.
 */
public class Looper {

    /** La boucle principale (unique au harnais). */
    private static final Looper MAIN = new Looper();

    private Looper() {
    }

    /** La boucle du thread courant (la principale au harnais). */
    public static Looper myLooper() {
        return MAIN;
    }

    /** La boucle principale du processus. */
    public static Looper getMainLooper() {
        return MAIN;
    }
}
