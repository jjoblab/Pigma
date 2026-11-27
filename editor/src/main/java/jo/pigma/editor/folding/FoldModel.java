package jo.pigma.editor.folding;

import jo.pigma.syntax.StructureDocument;

import java.util.TreeSet;

/**
 * Le modèle des plis (le paquet folding, l'arborescence du prompt) :
 * les en-têtes repliés et leurs caches — les lignes cachées, le
 * nombre de lignes visibles avant chaque ligne (les plis compactent
 * la hauteur), le compte des cachées. L'état et la reconstruction
 * vivent ici ; la politique (le curseur ne reste jamais caché,
 * l'en-tête se replie sur lui) reste à la vue.
 *
 * <p>La reconstruction est une passe en O(lignes), aux changements de
 * plis et aux éditions — jamais par trame. Les bornes s'y lisent à
 * l'interface {@link FoldBounds} : le document de structure (le lexer
 * local) ou les bornes composées (les plis du serveur de langage par-
 * dessus — la vue les tient).</p>
 */
public final class FoldModel {

    /** En-têtes repliés (indices de lignes). */
    private final TreeSet<Integer> folded = new TreeSet<>();

    /** Lignes cachées, null quand rien n'est replié. */
    private boolean[] hiddenByFold;

    /** Lignes visibles strictement avant chaque ligne. */
    private int[] visibleBefore;

    /** Lignes cachées en cours. */
    private int hiddenLineCount;

    /** Range tous les plis (le changement de langage, d'analyse). */
    public void clear() {
        folded.clear();
    }

    /** L'en-tête est-il replié. */
    public boolean isFolded(int header) {
        return folded.contains(header);
    }

    /** Replie l'en-tête. */
    public void fold(int header) {
        folded.add(header);
    }

    /** Déplie l'en-tête. */
    public void unfold(int header) {
        folded.remove(header);
    }

    /** Des plis tiennent-ils. */
    public boolean hasFolds() {
        return !folded.isEmpty();
    }

    /**
     * L'en-tête le plus récent dont le pli couvre la ligne — le plus
     * interne s'ouvre le premier quand le curseur y arrive.
     *
     * @param line la ligne couverte
     * @param bounds les bornes des plis (la structure, la composition)
     * @return l'indice de l'en-tête, -1 si aucun ne couvre
     */
    public int headerHiding(int line, FoldBounds bounds) {
        int container = -1;
        for (int header : folded) {
            if (header < line && line <= bounds.foldEndLine(header)) {
                container = header;
            }
        }
        return container;
    }

    /** Le nombre d'en-têtes repliés (le panneau le lit). */
    public int count() {
        return folded.size();
    }

    /**
     * Les en-têtes repliés, triés (A3 : l'état de document les
     * transporte — l'aller-retour d'un onglet y retrouve ses plis).
     *
     * @return les en-têtes triés, jamais {@code null}, non modifiable
     */
    public java.util.SortedSet<Integer> foldedHeaders() {
        return java.util.Collections.unmodifiableSortedSet(folded);
    }

    /**
     * Décale les en-têtes suivant l'édition (les lignes après le point
     * d'édition bougent du delta de lignes) ; un en-tête hors bornes
     * se perd — un pli dont l'en-tête n'ouvre plus rien se dissout à
     * la reconstruction.
     *
     * @param editLine  la ligne du point d'édition
     * @param delta     le delta de lignes de l'édition
     * @param lineCount le nombre de lignes après l'édition
     */
    public void shift(int editLine, int delta, int lineCount) {
        TreeSet<Integer> shifted = new TreeSet<>();
        for (int header : folded) {
            int moved = header < editLine ? header : header + delta;
            if (moved >= 0 && moved < lineCount) {
                shifted.add(moved);
            }
        }
        folded.clear();
        folded.addAll(shifted);
    }

    /**
     * Reconstruit les caches des plis courants.
     *
     * @param bounds     les bornes des plis (l'édition a pu y déplacer
     *                   les bornes — les en-têtes morts s'ignorent)
     * @param lineCount  le nombre de lignes du document
     */
    public void rebuild(FoldBounds bounds, int lineCount) {
        if (folded.isEmpty()) {
            hiddenByFold = null;
            visibleBefore = null;
            hiddenLineCount = 0;
            return;
        }
        boolean[] hidden = new boolean[lineCount];
        for (int header : folded) {
            if (header < 0 || header >= lineCount
                    || bounds == null || !bounds.foldStartsAt(header)) {
                continue; // l'édition a déplacé la structure : plus d'ouvrant
            }
            int end = bounds.foldEndLine(header);
            for (int line = header + 1; line <= end && line < lineCount; line++) {
                hidden[line] = true;
            }
        }
        int[] before = new int[lineCount];
        int visible = 0;
        for (int line = 0; line < lineCount; line++) {
            before[line] = visible;
            if (!hidden[line]) {
                visible++;
            }
        }
        hiddenByFold = hidden;
        visibleBefore = before;
        hiddenLineCount = lineCount - visible;
    }

    /** La ligne est-elle cachée par un pli. */
    public boolean isHidden(int line) {
        return hiddenByFold != null && line < hiddenByFold.length
                && hiddenByFold[line];
    }

    /** L'indice visible d'une ligne (les plis compactent la hauteur). */
    public int visibleIndexOf(int line) {
        return visibleBefore == null ? line : visibleBefore[line];
    }

    /**
     * La ligne d'un indice VISIBLE — l'inverse du mapping (une
     * recherche binaire sur les lignes visibles avant chaque ligne).
     *
     * @param index     l'indice visible cherché
     * @param lineCount le nombre de lignes du document
     * @return l'indice de la ligne, borné au document
     */
    public int lineOfVisibleIndex(int index, int lineCount) {
        if (visibleBefore == null) {
            return Math.max(0, Math.min(index, lineCount - 1));
        }
        int visible = lineCount - hiddenLineCount;
        if (index >= visible) {
            index = visible - 1;
        }
        int low = 0;
        int high = lineCount - 1;
        while (low < high) {
            int mid = (low + high + 1) / 2;
            if (visibleBefore[mid] <= index) {
                low = mid;
            } else {
                high = mid - 1;
            }
        }
        return low;
    }

    /** Le nombre de lignes cachées (le panneau le lit). */
    public int hiddenLineCount() {
        return hiddenLineCount;
    }
}
