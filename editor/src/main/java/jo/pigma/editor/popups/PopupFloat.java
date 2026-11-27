package jo.pigma.editor.popups;

/**
 * LE FLOT d'une popup (le retour du terrain : « l'animation des
 * popups recherche et aller a la ligne n'est pas fluide dans leur
 * façon de flotter d'une position à l'autre ») — la carte qui suit
 * son ancre (le caret) ne SAUTE plus à la pose nouvelle : elle y
 * GLISSE, en glissement court amorti (ease-out — le départ vif,
 * l'arrivée posée).
 *
 * <p>Pure et sans fil d'horloge, à la leçon du reste du paquetage
 * ({@link PopupAnimation}, {@code BadgePop}) : la vue avance la
 * progression au temps écoulé depuis le départ du flot, la TRAME
 * PORTANTE anime (elle y réclame la suivante tant que le flot vit —
 * {@link #isRunning()}). La carte qui RE-POSE pose son but ; le flot
 * retient l'ancienne pose, la dessine en route vers la neuve.</p>
 *
 * <p>La naissance pose directement (aucun flot à l'ouverture — le
 * pop d'entrée de {@link PopupAnimation} garde son rôle) ; la pose
 * IDENTIQUE ne déclenche rien (la carte qui redessine au même endroit
 * ne tremble pas).</p>
 */
public final class PopupFloat {

    /** La durée du flot (ms) — le glissement de pose, vif et posé. */
    private static final long FLOAT_MILLIS = 140L;

    /** L'horloge (le harnais y pose la sienne — la leçon de BadgePop). */
    private final java.util.function.LongSupplier clock;

    /** La pose d'où la carte part (le flot vit d'elle). */
    private float fromX;

    /** La pose d'où la carte part (le flot vit d'elle). */
    private float fromY;

    /** La pose où la carte va. */
    private float toX;

    /** La pose où la carte va. */
    private float toY;

    /** Le départ du flot (MIN_VALUE : aucun flot ne vit). */
    private long startedAt = Long.MIN_VALUE;

    /** La carte est-elle NÉE (la première pose se pose d'un trait) ? */
    private boolean born;

    /** Le flot vit-il encore (la carte n'a pas atteint sa pose). */
    private boolean running;

    /** Un flot à l'horloge du système. */
    public PopupFloat() {
        this(System::currentTimeMillis);
    }

    /** Un flot à l'horloge posée (le harnais JVM y avance le temps). */
    PopupFloat(java.util.function.LongSupplier clock) {
        this.clock = clock;
    }

    /**
     * La pose neuve de la carte : la POSE PREMIÈRE s'y pose d'un
     * trait (la naissance ne flotte pas — le pop d'entrée garde son
     * rôle), la posechangée y fait naître un flot DEPUIS la pose où la
     * carte se tenait (l'ancienne pose, pas le reste d'un flot
     * d'avant : le flot qui se RELANCE part de là où la carte se
     * VOIT — jamais d'un raccourci qui saute).
     *
     * @param x l'abscisse neuve (px)
     * @param y l'ordonnée neuve (px)
     * @return vrai quand un flot naît (la trame suivante s'y réclame)
     */
    public boolean poseTo(float x, float y) {
        if (!running && !born) {
            // la naissance : la pose se pose d'un trait (le drapeau
            // se lève — la pose suivante, si elle change, flottera)
            born = true;
            fromX = x;
            fromY = y;
            toX = x;
            toY = y;
            running = false;
            return false;
        }
        if (!running && x == toX && y == toY) {
            // la pose est celle où la carte vit déjà — rien ne flotte
            return false;
        }
        fromX = currentX();
        fromY = currentY();
        toX = x;
        toY = y;
        startedAt = now();
        running = true;
        return true;
    }

    /** L'abscisse courante de la carte (en flot : la valeur en route). */
    public float currentX() {
        return running ? fromX + (toX - fromX) * progress() : toX;
    }

    /** L'ordonnée courante de la carte (en flot : la valeur en route). */
    public float currentY() {
        return running ? fromY + (toY - fromY) * progress() : toY;
    }

    /** Le décalage horizontal du flot (px — nul au repos). */
    public float offsetX() {
        return currentX() - toX;
    }

    /** Le décalage vertical du flot (px — nul au repos). */
    public float offsetY() {
        return currentY() - toY;
    }

    /** Le flot vit-il (la vue y réclame la trame suivante). */
    public boolean isRunning() {
        if (running && now() - startedAt >= FLOAT_MILLIS) {
            running = false;
            fromX = toX;
            fromY = toY;
        }
        return running;
    }

    /** Remet le flot au repos (la pose se pose d'un trait). */
    public void reset() {
        startedAt = Long.MIN_VALUE;
        running = false;
        born = false;
    }

    /** La progression du flot (0 au départ, 1 à la pose — ease-out). */
    private float progress() {
        float linear = (now() - startedAt) / (float) FLOAT_MILLIS;
        if (linear >= 1.0f) {
            return 1.0f;
        }
        float inverted = 1.0f - linear;
        // l'ease-out cubique : le départ vif, l'arrivée posée
        return 1.0f - inverted * inverted * inverted;
    }

    private long now() {
        return clock.getAsLong();
    }
}
