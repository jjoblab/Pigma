package jo.pigma.editor.internal.search;

/**
 * La recherche (le composant du découpage de la vue — R1) : la
 * requête, ses correspondances et la correspondance COURANTE. Le
 * cœur en est PUR : la collecte (au plafond de
 * {@value #MATCH_LIMIT} — le compte s'y arrête, honnête), le choix
 * de la première correspondance après le curseur, le cycle
 * suivant/précédent qui BOUCLE (après la dernière vient la
 * première, avant la première vient la dernière).
 *
 * <p>L'hôte y vit par un contrat maigre : le texte du document
 * entier, le curseur d'où la première se choisit, l'aller-à (la
 * vue y pose son curseur et sa fenêtre) et le réclam de trame. Les
 * REMPLACEMENTS demeurent à la vue (une transaction d'annulation,
 * une unité — pas l'affaire du chercheur) : ils y lisent
 * {@link #query()}, {@link #matchAt(int)} et
 * {@link #currentIndex()}.</p>
 */
public final class SearchController {

    /** Le plafond de correspondances (le compte s'y arrête). */
    public static final int MATCH_LIMIT = 10_000;

    /**
     * Le contrat de l'hôte : le texte cherché, le curseur, l'aller-à
     * de la correspondance et le réclam de trame.
     */
    public interface Host {

        /** Le texte du document entier (une vue, déjà lue). */
        CharSequence documentText();

        /** Le curseur d'où la première correspondance se choisit. */
        int caret();

        /**
         * La correspondance s'y amène (le curseur y va, la fenêtre
         * suit) — l'aller-à du cycle.
         *
         * @param start       l'offset de la correspondance
         * @param queryLength la longueur de la requête
         */
        void revealMatch(int start, int queryLength);

        /** La trame se réclame. */
        void repaint();
    }

    private final Host host;

    private String query;

    private int[] matches = new int[0];

    private int current = -1;

    /**
     * Un chercheur neuf, branché à son hôte.
     *
     * @param host l'hôte, jamais null
     */
    public SearchController(Host host) {
        this.host = host;
    }

    /**
     * Cherche la requête dans tout le document : les correspondances
     * se collectent (au plafond, le compte le dit), la première après
     * le curseur devient courante et s'amène à l'hôte. Une requête
     * vide range la recherche.
     *
     * @param asked le texte cherché, exact (la casse compte)
     */
    public void search(String asked) {
        if (asked == null || asked.isEmpty()) {
            clear();
            return;
        }
        query = asked;
        IntArray found = new IntArray();
        CharSequence text = host.documentText();
        int at = 0;
        while (found.size() < MATCH_LIMIT) {
            int hit = indexOf(text, query, at);
            if (hit < 0) {
                break;
            }
            found.add(hit);
            at = hit + query.length();
        }
        matches = found.toArray();
        current = -1;
        if (matches.length > 0) {
            current = 0;
            for (int m = 0; m < matches.length; m++) {
                if (matches[m] >= host.caret()) {
                    current = m;
                    break;
                }
            }
            reveal(current);
        }
        host.repaint();
    }

    /** La correspondance suivante (la recherche boucle : après la
     * dernière vient la première). */
    public void next() {
        if (matches.length > 0) {
            reveal((current + 1) % matches.length);
        }
    }

    /** La correspondance précédente (la recherche boucle aussi en arrière). */
    public void previous() {
        if (matches.length > 0) {
            reveal((current - 1 + matches.length) % matches.length);
        }
    }

    /**
     * La requête courante (null sans recherche — les remplacements
     * de l'hôte la lisent).
     *
     * @return la requête, null quand la recherche se range
     */
    public String query() {
        return query;
    }

    /**
     * Les positions des correspondances (la teinte de la vue y
     * peinte) — un instantané, jamais null.
     *
     * @return les offsets des correspondances
     */
    public int[] matches() {
        return matches;
    }

    /**
     * Le nombre de correspondances (le plafond compris : le compte
     * s'arrête à {@value #MATCH_LIMIT}).
     *
     * @return le nombre de correspondances trouvées, 0 sans recherche
     */
    public int matchCount() {
        return matches.length;
    }

    /**
     * L'index de la correspondance courante (0-based), -1 sans
     * correspondance.
     *
     * @return l'index de la correspondance courante
     */
    public int currentIndex() {
        return current;
    }

    /**
     * L'offset d'une correspondance (le remplacement de l'hôte y
     * lit sa plage).
     *
     * @param index l'index de la correspondance
     * @return son offset, -1 hors bornes
     */
    public int matchAt(int index) {
        if (index < 0 || index >= matches.length) {
            return -1;
        }
        return matches[index];
    }

    /** Range la recherche (les teintes disparaissent, la requête
     * s'oublie) — la trame se réclame. */
    public void clear() {
        query = null;
        matches = new int[0];
        current = -1;
        host.repaint();
    }

    /**
     * Range la recherche SANS réclamer la trame : une édition
     * déplace tout ce qui suit — les positions des correspondances
     * seraient des mensonges, la recherche se range (la feuille la
     * relance) ; l'hôte repeint déjà par ailleurs.
     */
    public void forget() {
        query = null;
        matches = new int[0];
        current = -1;
    }

    /** La correspondance courante s'amène à l'hôte (le curseur y
     * va, la fenêtre suit). */
    private void reveal(int index) {
        current = index;
        host.revealMatch(matches[index], query.length());
    }

    /**
     * La sous-chaîne suivante : le texte entier a déjà été lu, la
     * boucle reste ici (aucune dépendance au tampon).
     */
    private static int indexOf(CharSequence text, String needle, int from) {
        int limit = text.length() - needle.length();
        for (int at = Math.max(0, from); at <= limit; at++) {
            int probe = 0;
            while (probe < needle.length()
                    && text.charAt(at + probe) == needle.charAt(probe)) {
                probe++;
            }
            if (probe == needle.length()) {
                return at;
            }
        }
        return -1;
    }

    /** Un tableau d'entiers extensible sans dépendance (la collecte
     * des correspondances). */
    private static final class IntArray {

        private int[] values = new int[16];

        private int size;

        void add(int value) {
            if (size == values.length) {
                values = java.util.Arrays.copyOf(values, values.length * 2);
            }
            values[size++] = value;
        }

        int size() {
            return size;
        }

        int[] toArray() {
            return java.util.Arrays.copyOf(values, size);
        }
    }
}
