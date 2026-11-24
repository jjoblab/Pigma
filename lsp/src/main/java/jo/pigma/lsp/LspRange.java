package jo.pigma.lsp;

import org.eclipse.lsp4j.Range;

/**
 * Intervalle du protocole : de la position de début (incluse) à la
 * position de fin (exclue). L'adaptation du type lsp4j se convertit
 * dans les deux sens.
 *
 * @param start le début
 * @param end   la fin
 */
public record LspRange(LspPosition start, LspPosition end) {

    /**
     * L'intervalle du protocole lsp4j équivalent.
     *
     * @return l'intervalle lsp4j
     */
    public Range toLsp() {
        return new Range(start.toLsp(), end.toLsp());
    }

    /**
     * L'adaptation d'un intervalle lsp4j.
     *
     * @param range l'intervalle du protocole
     * @return l'intervalle du domaine
     */
    public static LspRange fromLsp(Range range) {
        return new LspRange(LspPosition.fromLsp(range.getStart()),
                LspPosition.fromLsp(range.getEnd()));
    }
}
