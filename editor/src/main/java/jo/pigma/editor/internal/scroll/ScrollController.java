package jo.pigma.editor.internal.scroll;

/**
 * Le défilement aux bords (le composant du découpage de la vue —
 * R1) : pendant le traîné-déposé du texte, le doigt qui TIENT la
 * zone du bord fait glisser la vue d'une demi-ligne à l'échéance
 * (16 ms — le pas du balayage), le point de dépôt se relit à sa
 * nouvelle place, la suite se reprogramme ; le doigt qui QUITTE la
 * zone (ou le bord du document atteint) l'arrête.
 *
 * <p>L'hôte y vit par un contrat maigre : le doigt tient-il le
 * traîné, son ordonnée, les murs de la vue (bord haut, bord bas,
 * interligne), la borne du défilement, l'écriture du défilement et
 * la relecture du point de dépôt. Les BORNES vivent à la géométrie
 * extraite (LayoutModel) — le contrôleur n'y fait que glisser.</p>
 */
public final class ScrollController {

    /** L'échéance du pas (ms) — le rythme du balayage. */
    private static final long TICK_MILLIS = 16L;

    /**
     * Le contrat de l'hôte : le traîné, le doigt, les murs, la borne
     * et l'écriture du défilement.
     */
    public interface Host {

        /** Le traîné-déposé vit-il (le doigt tient du texte) ? */
        boolean isTextDragging();

        /** L'ordonnée du doigt (px, coordonnées de la vue). */
        float fingerY();

        /** Le bord haut de la fenêtre (px, le padding compris). */
        float viewportTop();

        /** Le bord bas de la fenêtre (px, le padding compris). */
        float viewportBottom();

        /** L'interligne (px — le pas y vit d'une demi-ligne). */
        float lineHeight();

        /** La borne basse du défilement vertical (px). */
        float maxScrollY();

        /** Le défilement vertical courant (px). */
        float scrollY();

        /**
         * Le défilement vertical s'écrit (borné par l'hôte).
         *
         * @param value le défilement nouveau (px)
         */
        void setScrollY(float value);

        /**
         * Le point de dépôt se relit à sa nouvelle place (le doigt
         * tient toujours la zone, la vue vient de glisser).
         */
        void recomputeDrop();

        /** La trame se réclame. */
        void repaint();

        /**
         * L'échéance se programme (le retard de l'hôte).
         *
         * @param tick    le pas à jouer
         * @param delayMs l'échéance (ms)
         */
        void postDelayed(Runnable tick, long delayMs);

        /**
         * L'échéance se retire (le geste finit — le doigt lève, la
         * poignée se lâche : le balayage s'arrête net).
         *
         * @param tick le pas à retirer
         */
        void removeCallbacks(Runnable tick);
    }

    private final Host host;

    /** L'échéance vit-elle (une seule à la fois). */
    private boolean scheduled;

    /** La largeur courante de la zone du bord (px, l'hôte la pose). */
    private float zone;

    /** Le pas du balayage (un cran par échéance). */
    private final Runnable tick = this::run;

    /**
     * Un contrôleur neuf, branché à son hôte.
     *
     * @param host l'hôte, jamais null
     */
    public ScrollController(Host host) {
        this.host = host;
    }

    /**
     * Le doigt tient-il la zone du bord (de la largeur posée, en
     * px) ? L'échéance s'y programme — une seule à la fois : le
     * re-programmage se fait à l'échéance elle-même, jamais en
     * rafale.
     *
     * @param zonePx la largeur de la zone du bord (px)
     */
    public void scheduleNearEdges(float zonePx) {
        zone = zonePx;
        if (!host.isTextDragging() || scheduled) {
            return;
        }
        if (host.fingerY() <= host.viewportTop() + zone
                || host.fingerY() >= host.viewportBottom() - zone) {
            scheduled = true;
            host.postDelayed(tick, TICK_MILLIS);
        }
    }

    /**
     * Une échéance du défilement au bord : la vue glisse d'une
     * demi-ligne vers le doigt, le point de dépôt se relit à sa
     * nouvelle place — le doigt tient toujours la zone, la suite se
     * reprogramme ; le bord du document (la zone quittée) l'arrête.
     */
    private void run() {
        scheduled = false;
        if (!host.isTextDragging()) {
            return;
        }
        float step = 0.0f;
        if (host.fingerY() <= host.viewportTop() + zone) {
            step = -0.5f * host.lineHeight();
        } else if (host.fingerY() >= host.viewportBottom() - zone) {
            step = 0.5f * host.lineHeight();
        }
        if (step == 0.0f) {
            return;
        }
        float before = host.scrollY();
        float bounded = Math.max(0.0f,
                Math.min(host.maxScrollY(), before + step));
        if (bounded == before) {
            return;
        }
        host.setScrollY(bounded);
        host.recomputeDrop();
        host.repaint();
        scheduleNearEdges(zone);
    }

    /** L'échéance vit-elle ? (le harnais le lit). */
    boolean isScheduled() {
        return scheduled;
    }

    /**
     * Le balayage s'arrête NET (le geste finit — le doigt lève, la
     * poignée se lâche) : l'échéance se retire, le drapeau se rend.
     */
    public void cancel() {
        host.removeCallbacks(tick);
        scheduled = false;
    }
}
