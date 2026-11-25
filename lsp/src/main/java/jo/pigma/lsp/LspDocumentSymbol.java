package jo.pigma.lsp;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

/**
 * Un symbole du document : son nom, son genre (le genre du protocole —
 * méthode, champ, type…), l'intervalle de sa déclaration entière et
 * celui du nom seul (la sélection du genre). Les symboles du document
 * ({@code textDocument/documentSymbol}) en rendent autant — le plan du
 * document, la table des matières que l'hôte y lit (une arborescence
 * côté protocole : la version à plat s'y tient, le document s'y lit
 * d'une traite).
 *
 * @param name           le nom du symbole, jamais vide
 * @param kind           le genre du protocole (6 : méthode, 8 : champ…)
 * @param range          l'intervalle de la déclaration entière
 * @param selectionRange l'intervalle du nom seul
 */
public record LspDocumentSymbol(String name, int kind, LspRange range,
        LspRange selectionRange) {

    /**
     * L'adaptation d'un symbole du protocole (la forme riche).
     *
     * @param symbol le symbole du protocole
     * @return le symbole du domaine, null si incomplet
     */
    public static LspDocumentSymbol fromLsp(DocumentSymbol symbol) {
        if (symbol == null || symbol.getName() == null
                || symbol.getRange() == null || symbol.getSelectionRange() == null) {
            return null;
        }
        return new LspDocumentSymbol(symbol.getName(),
                symbol.getKind() == null ? 0 : symbol.getKind().getValue(),
                LspRange.fromLsp(symbol.getRange()),
                LspRange.fromLsp(symbol.getSelectionRange()));
    }

    /**
     * L'adaptation d'une information de symbole (la forme à plat — le
     * genre et la place y vivent, l'intervalle de sélection est la
     * place même).
     *
     * @param info l'information du protocole
     * @return le symbole du domaine, null si incomplet
     */
    public static LspDocumentSymbol fromInfo(SymbolInformation info) {
        if (info == null || info.getName() == null
                || info.getLocation() == null || info.getLocation().getRange() == null) {
            return null;
        }
        LspRange range = LspRange.fromLsp(info.getLocation().getRange());
        return new LspDocumentSymbol(info.getName(),
                info.getKind() == null ? 0 : info.getKind().getValue(),
                range, range);
    }

    /**
     * L'adaptation de la réponse du protocole : les deux formes du
     * protocole y rentrent (le symbole riche ou l'information à plat),
     * la liste du domaine en sort (vide si rien).
     *
     * @param answer la réponse du protocole
     * @return les symboles, jamais null
     */
    public static List<LspDocumentSymbol> fromAnswer(
            List<Either<SymbolInformation, DocumentSymbol>> answer) {
        List<LspDocumentSymbol> symbols = new ArrayList<>();
        if (answer == null) {
            return symbols;
        }
        for (Either<SymbolInformation, DocumentSymbol> entry : answer) {
            if (entry == null) {
                continue;
            }
            LspDocumentSymbol symbol = entry.isLeft()
                    ? fromInfo(entry.getLeft()) : fromLsp(entry.getRight());
            if (symbol != null) {
                symbols.add(symbol);
            }
        }
        return symbols;
    }
}
