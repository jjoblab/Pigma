package jo.pigma.lsp;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.WorkspaceSymbol;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

/**
 * Un symbole de L'ESPACE DE TRAVAIL ({@code workspace/symbol}) : le nom
 * cherché partout sans quitter le document — la réponse y porte sa
 * place (le document et l'intervalle du nom). Les DEUX formes du
 * protocole y rentrent : l'information à plat ({@code SymbolInformation},
 * la réponse ordinaire) et le symbole d'espace ({@code WorkspaceSymbol},
 * la forme neuve — sa place y vit à la position nue ou au lien du
 * document).
 *
 * @param name     le nom du symbole, jamais vide
 * @param kind     le genre du protocole (6 : méthode, 8 : champ…)
 * @param location la place du symbole (le document et l'intervalle du
 *                 nom), jamais null
 */
public record LspWorkspaceSymbol(String name, int kind, LspLocation location) {

    /**
     * L'adaptation d'une information de symbole (la forme à plat du
     * protocole — la réponse ordinaire de l'espace de travail).
     *
     * @param info l'information du protocole
     * @return le symbole du domaine, null si incomplet
     */
    public static LspWorkspaceSymbol fromInfo(SymbolInformation info) {
        if (info == null || info.getName() == null
                || info.getLocation() == null) {
            return null;
        }
        LspLocation location = LspLocation.fromLsp(info.getLocation());
        if (location == null) {
            return null;
        }
        return new LspWorkspaceSymbol(info.getName(),
                info.getKind() == null ? 0 : info.getKind().getValue(), location);
    }

    /**
     * L'adaptation d'un symbole d'espace (la forme neuve du protocole —
     * sa place y vit à la position nue ou au lien du document).
     *
     * @param symbol le symbole d'espace du protocole
     * @return le symbole du domaine, null si incomplet
     */
    public static LspWorkspaceSymbol fromWorkspaceSymbol(WorkspaceSymbol symbol) {
        if (symbol == null || symbol.getName() == null) {
            return null;
        }
        Either<Location, org.eclipse.lsp4j.WorkspaceSymbolLocation> place
                = symbol.getLocation();
        if (place == null || place.isRight()) {
            // la place relative (URI seule, sans intervalle) ne dit pas
            // où le nom vit : l'aller-à s'y perd — le symbole se laisse
            return null;
        }
        Location target = place.getLeft();
        if (target == null || target.getUri() == null) {
            return null;
        }
        LspLocation location = LspLocation.fromLsp(target);
        if (location == null) {
            return null;
        }
        return new LspWorkspaceSymbol(symbol.getName(),
                symbol.getKind() == null ? 0 : symbol.getKind().getValue(),
                location);
    }

    /**
     * L'adaptation de la réponse du protocole : les deux formes y
     * rentrent (l'information à plat à gauche, le symbole d'espace à
     * droite), la liste du domaine en sort (vide si rien).
     *
     * @param answer la réponse du protocole
     * @return les symboles, jamais null
     */
    public static List<LspWorkspaceSymbol> fromAnswer(
            Either<List<? extends SymbolInformation>, List<? extends WorkspaceSymbol>> answer) {
        List<LspWorkspaceSymbol> symbols = new ArrayList<>();
        if (answer == null) {
            return symbols;
        }
        if (answer.isLeft() && answer.getLeft() != null) {
            for (SymbolInformation info : answer.getLeft()) {
                LspWorkspaceSymbol symbol = fromInfo(info);
                if (symbol != null) {
                    symbols.add(symbol);
                }
            }
        } else if (answer.isRight() && answer.getRight() != null) {
            for (WorkspaceSymbol rich : answer.getRight()) {
                LspWorkspaceSymbol symbol = fromWorkspaceSymbol(rich);
                if (symbol != null) {
                    symbols.add(symbol);
                }
            }
        }
        return symbols;
    }
}
