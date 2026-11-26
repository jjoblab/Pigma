package android.os;

/**
 * L'horloge du cadre (l'époque du démarrage) — le harnais y rend
 * l'heure réelle du système : la chronologie des scénarios vit.
 */
public final class SystemClock {

    private SystemClock() {
    }

    /**
     * Le temps écoulé depuis le démarrage (ms) — le harnais y rend
     * l'heure du système (aucun démarrage à mesurer).
     *
     * @return le temps écoulé (ms)
     */
    public static long uptimeMillis() {
        return System.currentTimeMillis();
    }
}
