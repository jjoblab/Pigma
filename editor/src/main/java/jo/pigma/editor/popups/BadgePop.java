package jo.pigma.editor.popups;

/**
 * Le POP de naissance de la pastille (le petit icône du diagnostic)
 * — la leçon de la référence, étudiée dans {@code
 * docs/etude-codeassist.md} : son ampoule flottante y NAÎT par un
 * ressort amorti (l'échelle part de la MOITIÉ, le pied ancré à la
 * ligne — la pastille monte vers le mot — l'opacité suit, le
 * rebond léger y signale l'apparition).
 *
 * <p>Pure et sans fil d'horloge : la vue avance la progression au
 * temps écoulé depuis la naissance, la TRAME PORTANTE anime (elle y
 * réclame la suivante tant que le rebond joue). Le rebond y vit en
 * courbe fermée (ease-out-back : un léger dépassement au retour, la
 * signature du ressort) — un test JVM pur l'éprouve sans canevas.</p>
 */
public final class BadgePop {

    /** La durée du pop (ms) — le ressort de la référence, ~300 ms. */
    private static final long POP_MILLIS = 300L;

    /** Le rebond du ressort (le dépassement de l'ease-out-back). */
    private static final float OVERSHOOT = 1.70158f;

    /** L'horloge (le harnais y pose la sienne). */
    private final java.util.function.LongSupplier clock;

    private long bornAt = Long.MIN_VALUE;

    /** Un pop à l'horloge du système. */
    public BadgePop() {
        this(System::currentTimeMillis);
    }

    /** Un pop à l'horloge posée (le harnais JVM y avance le temps). */
    BadgePop(java.util.function.LongSupplier clock) {
        this.clock = clock;
    }

    /** Le pop naît (la pastille vit). */
    public void born() {
        bornAt = clock.getAsLong();
    }

    /** Le pop se range (la pastille meurt ou se re-pose). */
    public void reset() {
        bornAt = Long.MIN_VALUE;
    }

    /** Le pop vit-il encore (la vue y réclame la trame suivante). */
    public boolean isRunning() {
        return bornAt != Long.MIN_VALUE
                && clock.getAsLong() - bornAt < POP_MILLIS;
    }

    /**
     * L'échelle courante : la moitié à la naissance, UN au repos — le
     * léger dépassement du ressort y vit (l'ease-out-back, la
     * signature du pop de la référence).
     */
    public float scale() {
        float t = progress();
        if (t >= 1.0f) {
            return 1.0f;
        }
        float inverted = t - 1.0f;
        float bounced = 1.0f + (OVERSHOOT + 1.0f) * inverted * inverted * inverted
                + OVERSHOOT * inverted * inverted;
        // l'échelle part de la MOITIÉ (la référence : la pastille
        // naît petite et monte vers le mot)
        return 0.5f + 0.5f * bounced;
    }

    /**
     * L'opacité courante : le fondu s'y gagne VITE (la moitié du
     * pop suffit — la pastille se lit avant la fin du rebond).
     */
    public float alpha() {
        return Math.min(1.0f, progress() * 2.0f);
    }

    /** La progression linéaire, bornée (0 à la naissance, 1 au repos). */
    private float progress() {
        if (bornAt == Long.MIN_VALUE) {
            return 1.0f;
        }
        return Math.max(0.0f, Math.min(1.0f,
                (clock.getAsLong() - bornAt) / (float) POP_MILLIS));
    }
}
