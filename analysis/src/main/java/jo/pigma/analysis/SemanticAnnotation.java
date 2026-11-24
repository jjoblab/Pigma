package jo.pigma.analysis;

/**
 * L3 : une annotation sémantique — une plage du document, une nature
 * en chaîne ouverte (« type », « méthode », « champ »… un serveur de
 * langage branchera les siennes), des modificateurs de rendu.
 *
 * @param start     début de la plage, offset absolu
 * @param end       fin de la plage, offset absolu
 * @param nature    la nature, chaîne ouverte, jamais {@code null}
 * @param modifiers les modificateurs, OU binaire
 */
public record SemanticAnnotation(int start, int end, String nature, int modifiers) {

    /** Le modificateur « gras » (rendu). */
    public static final int BOLD = 1;

    /**
     * La plage couvre-t-elle les bornes données (offsets absolus).
     *
     * @param from début à couvrir
     * @param to   fin à couvrir
     * @return vrai si la plage contient [{@code from}, {@code to})
     */
    public boolean covers(int from, int to) {
        return start <= from && to <= end;
    }
}
