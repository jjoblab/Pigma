package jo.pigma.editor.internal.touch;

/**
 * Le PINCEMENT (la première brique du contrôleur des gestes du
 * découpage — R1) : deux doigts posés, l'écart des doigts porte le
 * FACTEUR du corps (le corps du départ l'attend), le milieu des
 * doigts porte le FOYER — la ligne sous le foyer du départ demeure
 * sous le foyer courant : le texte ne fuit pas sous les doigts, en
 * Y comme en X il y reste.
 *
 * <p>L'hôte y vit par un contrat maigre : le corps et ses bornes,
 * les murs (le padding), le défilement lu et écrit, le focus, le
 * rangement des gestes en cours (le pincement s'engage en rangeant
 * tout ce qui vivait — la poignée se lâche, la sélection reste
 * posée), l'appui restant quand un doigt s'en va et le réclam de
 * trame. Les positions des DOIGTS demeurent à l'hôte — le contrôleur
 * ne reçoit que l'écart et le foyer, calculés.</p>
 */
public final class PinchController {

    /**
     * Le contrat de l'hôte : le corps, les murs, le défilement, le
     * rangement des gestes.
     */
    public interface Host {

        /** Le corps du texte au départ du pincement (px). */
        float textPx();

        /**
         * Le corps se pose (les bornes s'y respectent — le facteur
         * ne sort jamais des murs).
         *
         * @param freshPx le corps voulu (px)
         */
        void applyTextSize(float freshPx);

        /** L'interligne (px). */
        float lineHeight();

        /** La chasse d'un glyphe (px). */
        float charWidth();

        /** Le bord haut remonté (px — le padding compris). */
        float viewportTop();

        /** Le bord gauche remonté (px — le padding compris). */
        float viewportLeft();

        /** Le défilement vertical courant (px). */
        float scrollY();

        /** Le défilement vertical s'écrit (px). */
        void setScrollY(float value);

        /** La borne basse du défilement vertical (px). */
        float maxScrollY();

        /** Le défilement horizontal courant (px). */
        float scrollX();

        /** Le défilement horizontal s'écrit (px). */
        void setScrollX(float value);

        /** La borne du défilement horizontal (px). */
        float maxScrollX();

        /** Le focus se prend (le clavier s'y prépare). */
        void takeFocus();

        /**
         * Les gestes en cours se rangent (la poignée se lâche, la
         * sélection reste posée, les traînés meurent) : le
         * pincement s'engage sur une vue nette.
         */
        void settleGestures();

        /**
         * L'appui restant se pose (un doigt s'en va — l'autre
         * repart de SA position vivante).
         *
         * @param x l'abscisse du doigt restant
         * @param y l'ordonnée du doigt restant
         */
        void touchRestartedAt(float x, float y);

        /** La mémoire du dernier tap s'efface (le geste a grandi). */
        void forgetLastTap();

        /** La trame se réclame. */
        void repaint();
    }

    private final Host host;

    /** Le pincement vit (deux doigts posés). */
    private boolean pinching;

    /** Un pincement a eu lieu dans ce geste (l'appui final ne vaut
     * pas un tap). */
    private boolean pinchedThisGesture;

    /** L'écart des deux doigts au départ. */
    private float startSpan;

    /** Le corps du texte au départ. */
    private float startPx;

    /** L'ordonnée du foyer au départ (le milieu des deux doigts). */
    private float startFocusY;

    /** L'abscisse du foyer au départ. */
    private float startFocusX;

    /**
     * Un contrôleur neuf, branché à son hôte.
     *
     * @param host l'hôte, jamais null
     */
    public PinchController(Host host) {
        this.host = host;
    }

    /**
     * Le pincement se range (le système reprend le geste — la vue
     * parente navigue, le pincement n'en fait pas partie) : tout se
     * rend, l'appui suivant repart de zéro.
     */
    public void reset() {
        pinching = false;
        pinchedThisGesture = false;
        startSpan = 0.0f;
    }

    /** Le pincement vit-il ? */
    public boolean isPinching() {
        return pinching;
    }

    /** Un pincement a-t-il eu lieu dans ce geste ? */
    public boolean hasPinchedThisGesture() {
        return pinchedThisGesture;
    }

    /**
     * Le pincement s'engage : tout geste en cours se range (la
     * poignée se lâche, la sélection reste posée), les doigts
     * deviennent le zoom — l'écart des deux portera le facteur,
     * leur milieu le foyer.
     *
     * @param span   l'écart des deux doigts (px)
     * @param focusX l'abscisse du milieu des deux doigts (px)
     * @param focusY l'ordonnée du milieu des deux doigts (px)
     */
    public void engage(float span, float focusX, float focusY) {
        host.takeFocus();
        pinching = true;
        pinchedThisGesture = true;
        host.settleGestures();
        startSpan = span;
        startPx = host.textPx();
        startFocusY = focusY;
        startFocusX = focusX;
    }

    /**
     * Le pincement vit : l'écart des doigts porte le facteur (le
     * corps du départ l'attend), la ligne sous le foyer du départ
     * demeure sous le foyer courant — le texte ne fuit pas sous les
     * doigts, il y reste (en Y comme en X).
     *
     * @param span   l'écart courant des deux doigts (px)
     * @param focusX l'abscisse courante du milieu (px)
     * @param focusY l'ordonnée courante du milieu (px)
     */
    public void move(float span, float focusX, float focusY) {
        if (span <= 0.0f || startSpan <= 0.0f) {
            return;
        }
        float lineAtFocus = (startFocusY - host.viewportTop() + host.scrollY())
                / host.lineHeight();
        // la colonne sous le foyer du départ demeure sous le foyer
        // courant : le texte ne fuit sous les doigts, en Y comme en X
        float columnAtFocus = (startFocusX - host.viewportLeft() + host.scrollX())
                / host.charWidth();
        host.applyTextSize(startPx * (span / startSpan));
        host.setScrollY(bounded(lineAtFocus * host.lineHeight()
                - (focusY - host.viewportTop()), host.maxScrollY()));
        host.setScrollX(bounded(columnAtFocus * host.charWidth()
                - (focusX - host.viewportLeft()), host.maxScrollX()));
        host.repaint();
    }

    /**
     * Le pincement se dénoue : un doigt s'en va, l'autre peut
     * glisser — son appui repart de sa position vivante (le
     * défilement n'y saute pas), la mémoire du tap s'efface.
     *
     * @param stayingX l'abscisse du doigt restant (px)
     * @param stayingY l'ordonnée du doigt restant (px)
     */
    public void end(float stayingX, float stayingY) {
        pinching = false;
        host.touchRestartedAt(stayingX, stayingY);
        host.forgetLastTap();
        host.repaint();
    }

    /** Le défilement borné (plancher zéro, mur haut posé). */
    private static float bounded(float value, float max) {
        return Math.max(0.0f, Math.min(max, value));
    }
}
