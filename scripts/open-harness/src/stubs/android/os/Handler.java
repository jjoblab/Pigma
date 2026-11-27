package android.os;

/**
 * File de démonstration : les affichages différés se jouent À LA
 * FILE (le harnais vide la sienne) — le faux moteur de démonstration
 * y pose ses morceaux et l'annulation les retire.
 */
public class Handler {

    /** La boucle porteuse (sans effet au harnais : la file est
     * unique). */
    private final Looper looper;

    /** Une file neuve sur la boucle courante. */
    public Handler() {
        this(Looper.myLooper());
    }

    /**
     * Une file neuve sur la boucle donnée.
     *
     * @param looper la boucle porteuse
     */
    public Handler(Looper looper) {
        this.looper = looper;
    }

    /** La boucle porteuse. */
    public Looper getLooper() {
        return looper;
    }

    /**
     * Pose un affichage différé.
     *
     * @param tick      l'affichage
     * @param delayMillis le délai (indicatif au harnais : la file
     *                     l'ignore, l'ordre y suffit)
     * @return vrai
     */
    public boolean postDelayed(Runnable tick, long delayMillis) {
        return true;
    }

    /**
     * Retire un affichage de la file.
     *
     * @param tick l'affichage retiré
     */
    public void removeCallbacks(Runnable tick) {
        // rien au harnais : la file se vide d'elle-même
    }
}
