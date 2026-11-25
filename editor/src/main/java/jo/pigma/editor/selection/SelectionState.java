package jo.pigma.editor.selection;

/**
 * L'état de sélection (le paquet selection, l'arborescence du prompt) :
 * le curseur et l'ancre. La sélection va de l'ancre au curseur —
 * l'ancre se pose la première, le curseur la rouvre ; ancre à -1 : le
 * curseur seul, pas de sélection.
 *
 * <p>Les bornes dérivées (la sélection tient-elle, son début, sa fin)
 * vivent ici : la vue y délègue, ses méthodes publiques ne changent
 * pas. Le texte sélectionné reste à la vue (il lit le tampon).</p>
 */
public final class SelectionState {

    /**
     * L'écouteur des mouvements : le curseur ou l'ancre bouge — la
     * vue y dit au porteur (la façade du serveur de langage y suit
     * le curseur : le surlignage des occurrences s'y redemande).
     * L'avis part sur le fil qui pose la borne (le geste, la
     * frappe, le programme — l'écouteur y marshalle s'il veut).
     */
    public interface Listener {

        /**
         * Une borne vient de bouger.
         *
         * @param caret  le curseur après le mouvement
         * @param anchor l'ancre après le mouvement (-1 sans sélection)
         */
        void onSelectionMoved(int caret, int anchor);
    }

    /** Le curseur (en unités de code UTF-16). */
    private int caret;

    /** L'ancre de sélection (-1 : pas de sélection, le curseur seul). */
    private int anchor = -1;

    /** L'écouteur du mouvement (null : personne). */
    private Listener listener;

    /** La position du curseur (en unités de code UTF-16). */
    public int caret() {
        return caret;
    }

    /**
     * Pose le curseur.
     *
     * @param caret la position (en unités de code UTF-16)
     */
    public void setCaret(int caret) {
        this.caret = caret;
        notifyMoved();
    }

    /** L'ancre de sélection (-1 : pas de sélection). */
    public int anchor() {
        return anchor;
    }

    /**
     * Pose l'ancre de sélection (-1 la range).
     *
     * @param anchor la position (en unités de code UTF-16)
     */
    public void setAnchor(int anchor) {
        this.anchor = anchor;
        notifyMoved();
    }

    /**
     * Branche l'écouteur des mouvements (null pour le ranger).
     *
     * @param listener l'écouteur, null pour le ranger
     */
    public void setListener(Listener listener) {
        this.listener = listener;
    }

    /** L'avis du mouvement part à l'écouteur branché. */
    private void notifyMoved() {
        if (listener != null) {
            listener.onSelectionMoved(caret, anchor);
        }
    }

    /** Une sélection tient-elle (l'ancre existe et diffère du curseur) ? */
    public boolean hasSelection() {
        return anchor >= 0 && anchor != caret;
    }

    /**
     * Le début de la sélection, en unités de code UTF-16.
     *
     * @return la plus petite des deux bornes, ou la position du
     *         curseur sans sélection
     */
    public int start() {
        return anchor < 0 ? caret : Math.min(anchor, caret);
    }

    /**
     * La fin de la sélection, en unités de code UTF-16.
     *
     * @return la plus grande des deux bornes, ou la position du
     *         curseur sans sélection
     */
    public int end() {
        return anchor < 0 ? caret : Math.max(anchor, caret);
    }
}
