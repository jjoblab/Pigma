package jo.pigma.lsp;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.lsp4j.FoldingRange;

/**
 * Un pli du document ({@code textDocument/foldingRange}) : le serveur
 * y dit quelle plage de lignes se replie — l'éditeur local en tient
 * déjà (le lexer y lit ses blocs), le serveur en porte d'autres (les
 * régions d'import, les blocs de commentaires, ceux de son analyse à
 * lui). L'en-tête y replie jusqu'à la fin, la gouttière en porte le
 * marqueur.
 *
 * @param startLine la ligne de l'en-tête (celle de l'ouvrant)
 * @param endLine   la dernière ligne couverte (fermée comprise)
 * @param kind      le genre du protocole (« region », « comment »…),
 *                  null pour un pli sans genre
 */
public record LspFoldingRange(int startLine, int endLine, String kind) {

    /**
     * L'adaptation d'un pli du protocole.
     *
     * @param range le pli du protocole
     * @return le pli du domaine, null si incomplet
     */
    public static LspFoldingRange fromLsp(FoldingRange range) {
        if (range == null || range.getStartLine() < 0 || range.getEndLine() < range.getStartLine()) {
            return null;
        }
        return new LspFoldingRange(range.getStartLine(), range.getEndLine(), range.getKind());
    }

    /**
     * L'adaptation de la réponse du protocole.
     *
     * @param answer les plis du protocole
     * @return les plis du domaine, jamais null
     */
    public static List<LspFoldingRange> fromLsp(List<FoldingRange> answer) {
        List<LspFoldingRange> ranges = new ArrayList<>();
        if (answer != null) {
            for (FoldingRange range : answer) {
                LspFoldingRange adapted = fromLsp(range);
                if (adapted != null) {
                    ranges.add(adapted);
                }
            }
        }
        return ranges;
    }
}
