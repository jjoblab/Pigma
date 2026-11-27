package jo.pigma.editor.ai;

import java.util.function.LongSupplier;

/**
 * LA MACHINE À ÉTATS DU TEXTE FANTÔME (B4 — la complétion en ligne) :
 * le texte proposé se DEVINE au caret, jamais écrit au tampon tant
 * qu'il n'est pas accepté. Pure et sans fil d'horloge : l'horloge
 * s'injecte (le harnais JVM y pose la sienne), le tampon ne s'y
 * connaît pas — la RÉVISION et la POSITION attendues s'y posent,
 * la réponse qui ne les dit plus SE JETE (une réponse périmée serait
 * un mensonge à l'écran).
 *
 * <p>Les phases : INACTIVE (rien ne vit) → EN ATTENTE (la frappe a
 * armé la temporisation) → EN VOL (la demande est partie, UNE seule
 * à la fois) → AFFICHÉ (le fantôme vit au caret) — puis l'issue :
 * ACCEPTÉ (un mot ou tout, le texte posé par UNE transaction), ou
 * REJETÉ, ou MASQUÉ (une composition IME, une sélection, la carte de
 * complétion, un pli, un document qui change, un éditeur figé), ou
 * PÉRIMÉ (la réponse d'un texte qui a bougé).</p>
 *
 * <p>La frappe REPREND tout : l'attente se réarme, le vol vivant se
 * COUPE (une seule demande en vol — la frappe suivante l'emporte),
 * le fantôme affiché MEURT (sa proposition parlait du texte
 * d'avant). L'acceptation d'un MOT garde le reste au caret : la
 * proposition se pose morceau par morceau, chaque morceau SA
 * transaction.</p>
 */
public final class GhostTextState {

    /** Les phases du fantôme. */
    public enum Phase {

        /** Rien ne vit (le repos). */
        INACTIVE,

        /** La temporisation court (la frappe l'a armée). */
        WAITING,

        /** La demande vole (une seule à la fois). */
        FLYING,

        /** Le fantôme vit au caret (l'acceptation le posera). */
        SHOWN
    }

    /** La temporisation par défaut (ns) — 400 ms, entre les 300 et
     * 500 du plan. */
    private static final long DEFAULT_TIMEOUT_NANOS = 400_000_000L;

    /** L'horloge (le harnais y pose la sienne). */
    private final LongSupplier clock;

    /** La temporisation armée à la frappe (ns). */
    private long timeoutNanos = DEFAULT_TIMEOUT_NANOS;

    /** La phase courante. */
    private Phase phase = Phase.INACTIVE;

    /** L'échéance de l'attente (ns — lue à l'horloge posée). */
    private long deadline;

    /** La révision du tampon attendue par la réponse en vol. */
    private int expectedRevision;

    /** La position du curseur attendue par la réponse en vol. */
    private int expectedOffset;

    /** Le texte du fantôme affiché (vide : aucun). */
    private String text = "";

    /**
     * Une machine neuve, à l'horloge du système.
     */
    public GhostTextState() {
        this(System::nanoTime);
    }

    /**
     * Une machine neuve, à l'horloge posée (le harnais JVM y avance
     * le temps).
     *
     * @param clock l'horloge (les nanosecondes qui passent)
     */
    GhostTextState(LongSupplier clock) {
        this.clock = clock;
    }

    /**
     * Pose la temporisation de la frappe (le plan la veut entre 300
     * et 500 ms — l'hôte la règle à sa main).
     *
     * @param millis la temporisation, en millisecondes
     */
    public void setTimeoutMs(long millis) {
        timeoutNanos = Math.max(0, millis) * 1_000_000L;
    }

    /** La temporisation courante (ms — l'échéance de l'attente s'y
     * pose, la vue y cadence sa question). */
    public long timeoutMs() {
        return timeoutNanos / 1_000_000L;
    }

    /**
     * Le RESTE de l'attente (ms — l'échéance jouée trop tôt s'y
     * repose : la file peut courir avant l'horloge, le reste dit le
     * temps qui manque).
     *
     * @return le temps restant avant l'échéance, 0 si elle a passé
     */
    public long remainingMs() {
        long rest = deadline - clock.getAsLong();
        return rest <= 0 ? 0L : (rest + 999_999L) / 1_000_000L;
    }

    /** La phase courante. */
    public Phase phase() {
        return phase;
    }

    /** Le texte du fantôme affiché (vide : aucun). */
    public String text() {
        return phase == Phase.SHOWN ? text : "";
    }

    /**
     * LA FRAPPE : tout se reprécède — l'attente se (ré)arme, le vol
     * vivant SE COUPE (une seule demande en vol : la frappe suivante
     * l'emporte), le fantôme affiché MEURT (sa proposition parlait
     * du texte d'avant).
     *
     * @return vrai quand le vol vivant doit se couper (la poignée
     *         s'annule)
     */
    public boolean typed() {
        boolean cancelFlight = phase == Phase.FLYING;
        arm();
        return cancelFlight;
    }

    /**
     * L'attente est-elle ÉCHUE ? (la temporisation a passé son
     * échéance — la demande peut partir). L'attente échue devient EN
     * VOL : l'échéance se borne à la question.
     *
     * @return vrai quand la demande doit partir (l'attente était vive
     *         et échue)
     */
    public boolean timeoutReached() {
        if (phase != Phase.WAITING) {
            return false;
        }
        if (clock.getAsLong() < deadline) {
            return false;
        }
        phase = Phase.FLYING;
        return true;
    }

    /**
     * La demande EST PARTIE : la révision et la position du curseur
     * s'y attendent (la réponse qui ne les dira plus se jettera).
     *
     * @param revision la révision du tampon au départ
     * @param offset   la position du curseur au départ
     */
    public void flew(int revision, int offset) {
        expectedRevision = revision;
        expectedOffset = offset;
    }

    /**
     * LA RÉPONSE ARRIVE : elle ne vit QUE si le texte n'a pas bougé
     * (la révision dit le tampon, la position dit le curseur) — la
     * réponse périmée SE JETE (la poser serait un mensonge à
     * l'écran). Une réponse vide ne vit pas non plus (le moteur muet
     * ne montre rien).
     *
     * @param proposed le texte proposé au caret
     * @param revision la révision du tampon au départ
     * @param offset   la position du curseur au départ
     * @return vrai quand le fantôme vit (la proposition s'affiche)
     */
    public boolean answered(String proposed, int revision, int offset) {
        if (phase != Phase.FLYING) {
            return false; // le vol est mort : la réponse se jette
        }
        if (revision != expectedRevision || offset != expectedOffset
                || proposed == null || proposed.isEmpty()) {
            phase = Phase.INACTIVE;
            text = "";
            return false;
        }
        text = proposed;
        phase = Phase.SHOWN;
        return true;
    }

    /**
     * LE PREMIER MOT du fantôme (l'acceptation au doigt — balayer) :
     * le mot se détache (l'espace qui le précède et celui qui le
     * suit voyagent avec lui — la pose n'en laisse jamais un
     * doublé), le RESTE demeure au caret (la proposition se pose
     * morceau par morceau, chaque morceau SA transaction). Le
     * dernier morceau épuise la proposition.
     *
     * @return le mot à poser (vide : la proposition est épuisée)
     */
    public String takeWord() {
        if (phase != Phase.SHOWN || text.isEmpty()) {
            return "";
        }
        int start = 0;
        while (start < text.length()
                && Character.isWhitespace(text.charAt(start))) {
            start++;
        }
        if (start >= text.length()) {
            // la proposition n'était qu'un souffle : elle meurt
            phase = Phase.INACTIVE;
            text = "";
            return "";
        }
        int at = start;
        while (at < text.length()
                && !Character.isWhitespace(text.charAt(at))) {
            at++;
        }
        if (at < text.length()) {
            at++; // l'espace qui suit le mot se pose avec lui
        }
        // le mot se rend DEPUIS LA TÊTE : l'espace qui le précède
        // voyage avec lui (la pose n'en laisse jamais un doublé)
        String word = text.substring(0, at);
        String rest = text.substring(at);
        if (rest.isEmpty()) {
            phase = Phase.INACTIVE;
            text = "";
        } else {
            text = rest;
        }
        return word;
    }

    /**
     * TOUT LE FANTÔME (l'acceptation au clavier — Tab) : la
     * proposition entière se pose, le fantôme meurt.
     *
     * @return le texte à poser (vide : rien ne vit)
     */
    public String takeAll() {
        if (phase != Phase.SHOWN) {
            return "";
        }
        String all = text;
        phase = Phase.INACTIVE;
        text = "";
        return all;
    }

    /**
     * LE MASQUAGE (le rejet au doigt, la composition IME, la
     * sélection, la carte de complétion, le pli, le document qui
     * change, l'éditeur figé) : le fantôme meurt, le vol vivant se
     * coupe.
     *
     * @return vrai quand le vol vivant doit se couper
     */
    public boolean masked() {
        boolean cancelFlight = phase == Phase.FLYING;
        phase = Phase.INACTIVE;
        text = "";
        return cancelFlight;
    }

    /** Le fantôme vit-il ? */
    public boolean isShown() {
        return phase == Phase.SHOWN;
    }

    /** L'attente s'arme : la phase devient EN ATTENTE, l'échéance
     * se pose. */
    private void arm() {
        phase = Phase.WAITING;
        deadline = clock.getAsLong() + timeoutNanos;
        text = "";
    }
}
