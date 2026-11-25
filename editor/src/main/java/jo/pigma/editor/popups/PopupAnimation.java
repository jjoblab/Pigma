package jo.pigma.editor.popups;

/**
 * L'animation d'une popup : l'ouverture et la sortie y vivent en
 * FONDU et GLISSEMENT — la carte naît au caret, transparente et
 * légèrement plus petite, puis gagne sa place et son opacité en une
 * centaine de millisecondes ; la sortie inverse le mouvement (plus
 * courte : la carte s'efface sans traîner). La progression vit en
 * unités réelles : la vue l'avance à CHAQUE trame du temps écoulé
 * depuis le dernier rafraîchissement (jamais de fil d'horloge — le
 * harnais et l'appareil cadancent pareil, la trame portante anime).
 *
 * <p>La courbe est un départ rapide et un frein doux (ease-out
 * cubique) : la carte répond au doigt immédiatement, l'arrivée se
 * pose. La carte qui change de CONTENU (la liste se re-filtre à la
 * frappe) ne rejoue PAS l'animation — seule l'ouverture d'une carte
 * fermée, et la fermeture d'une carte ouverte, y passent : la liste
 * multiple reste stable pendant qu'on tape.</p>
 */
public final class PopupAnimation {

    /** La durée d'ouverture (ms) — la naissance de la carte. */
    private static final long OPEN_MILLIS = 110L;

    /** La durée de sortie (ms) — plus courte, l'effacement ne traîne pas. */
    private static final long CLOSE_MILLIS = 70L;

    /** Le décalage de départ : la carte naît 3 dp plus près du caret. */
    private static final float START_SHIFT_DP = 3.0f;

    private long openStartedAt;
    private long closeStartedAt;
    private boolean opening;
    private boolean closing;

    /** Une animation neuve, au repos (rien ne se joue). */
    public PopupAnimation() {
        openStartedAt = Long.MIN_VALUE;
        closeStartedAt = Long.MIN_VALUE;
    }

    /**
     * La carte s'ouvre : l'animation part de zéro, la sortie se
     * range (une réouverture interrompt la sortie en cours — la
     * progression repart du haut).
     */
    public void open() {
        openStartedAt = now();
        closeStartedAt = Long.MIN_VALUE;
        opening = true;
        closing = false;
    }

    /**
     * La carte se range : la sortie part de UN (l'opacité courante
     * descend vers zéro). Sans ouverture en cours, la sortie part de
     * l'état posé.
     */
    public void close() {
        closeStartedAt = now();
        opening = false;
        closing = true;
    }

    /** L'animation d'ouverture vit-elle encore. */
    public boolean isOpening() {
        return opening && now() - openStartedAt < OPEN_MILLIS;
    }

    /** L'animation de sortie vit-elle encore. */
    public boolean isClosing() {
        return closing && now() - closeStartedAt < CLOSE_MILLIS;
    }

    /**
     * L'animation vit-elle (ouverture ou sortie) — la vue y invalide
     * la trame suivante tant que oui.
     */
    public boolean isRunning() {
        return isOpening() || isClosing();
    }

    /**
     * La progression d'ouverture courante : 0 à la naissance, 1 à la
     * pose (la courbe ease-out y vit — départ rapide, arrivée posée).
     */
    public float openProgress() {
        if (!opening) {
            return 0.0f;
        }
        return easeOut(clamp((now() - openStartedAt) / (float) OPEN_MILLIS));
    }

    /**
     * La progression de sortie courante : 1 au départ de la sortie,
     * 0 à l'effacement complet.
     */
    public float closeProgress() {
        if (!closing) {
            return 0.0f;
        }
        return 1.0f - easeOut(clamp((now() - closeStartedAt)
                / (float) CLOSE_MILLIS));
    }

    /**
     * Le facteur d'opacité de la carte (0 à 1) : celui de
     * l'ouverture, celui de la sortie, 1 au repos posé.
     */
    public float alphaFactor() {
        if (isOpening()) {
            return openProgress();
        }
        if (isClosing()) {
            return closeProgress();
        }
        return opening || closing ? 0.0f : 1.0f;
    }

    /**
     * Le décalage vertical de la carte (px, négatif : la carte naît
     * au-dessus de sa pose) — la glisse de l'ouverture, celle de la
     * sortie au retour.
     *
     * @param density la densité de l'écran (le dp en pixels)
     */
    public float shiftPx(float density) {
        float span = START_SHIFT_DP * density;
        if (isOpening()) {
            return (1.0f - openProgress()) * span;
        }
        if (isClosing()) {
            return (closeProgress() - 1.0f) * span;
        }
        return 0.0f;
    }

    /**
     * La carte finit-elle de se ranger (la sortie a joué entière) :
     * l'état interne se remet au repos — le prochain appel à
     * {@link #open()} repartira propre.
     */
    public boolean finishedClosing() {
        return closing && !isClosing();
    }

    /** Remet l'animation au repos (aucun mouvement ne se joue). */
    public void reset() {
        openStartedAt = Long.MIN_VALUE;
        closeStartedAt = Long.MIN_VALUE;
        opening = false;
        closing = false;
    }

    private static long now() {
        return System.currentTimeMillis();
    }

    private static float clamp(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    /** La courbe ease-out cubique : départ rapide, frein doux. */
    private static float easeOut(float linear) {
        float inverted = 1.0f - linear;
        return 1.0f - inverted * inverted * inverted;
    }
}
