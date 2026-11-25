package jo.pigma.lsp;

import java.util.ArrayList;
import java.util.List;

/**
 * Un jeton sémantique du serveur ({@code textDocument/semanticTokens/
 * full}) : la plage couverte, le genre du jeton (le nom de la légende
 * du serveur — « variable », « function », « keyword »…) et ses
 * modificateurs (les noms de la légende — « declaration »…). La
 * coloration locale (le lexer) demeure ; les jetons du serveur la
 * surpassent là où ils tombent — la couche du protocole au-dessus de
 * la couche locale.
 *
 * @param range      la plage couverte par le jeton
 * @param type        le genre (le nom de la légende du serveur)
 * @param modifiers   les modificateurs posés (les noms de la légende),
 *                    vide pour aucun
 */
public record LspSemanticToken(LspRange range, String type, List<String> modifiers) {

    /**
     * Le décodage de la réponse du protocole : les jetons y vivent en
     * encodage RELATIF (cinq entiers par jeton — l'écart de ligne au
     * jeton d'avant, l'écart de colonne à la ligne près, la longueur,
     * l'indice du genre, le masque des modificateurs), la légende du
     * serveur y traduit les indices en noms. Un indice hors légende se
     * tait (le jeton se perd, la chaîne ne casse jamais).
     *
     * @param data            les entiers de la réponse, null pour rien
     * @param legendTypes     les genres de la légende du serveur
     * @param legendModifiers les modificateurs de la légende du serveur
     * @return les jetons du domaine, jamais null
     */
    public static List<LspSemanticToken> decode(List<Integer> data,
            List<String> legendTypes, List<String> legendModifiers) {
        List<LspSemanticToken> tokens = new ArrayList<>();
        if (data == null || data.size() % 5 != 0) {
            return tokens;
        }
        int line = 0;
        int character = 0;
        for (int at = 0; at + 5 <= data.size(); at += 5) {
            int deltaLine = data.get(at);
            int deltaCharacter = data.get(at + 1);
            int length = data.get(at + 2);
            int typeIndex = data.get(at + 3);
            int modifierBits = data.get(at + 4);
            line += deltaLine;
            character = deltaLine == 0 ? character + deltaCharacter : deltaCharacter;
            if (length <= 0 || typeIndex < 0 || typeIndex >= legendTypes.size()) {
                continue; // un genre hors légende : le jeton se perd
            }
            List<String> modifiers = new ArrayList<>();
            for (int bit = 0; bit < legendModifiers.size(); bit++) {
                if ((modifierBits & (1 << bit)) != 0) {
                    modifiers.add(legendModifiers.get(bit));
                }
            }
            tokens.add(new LspSemanticToken(new LspRange(
                    new LspPosition(line, character),
                    new LspPosition(line, character + length)),
                    legendTypes.get(typeIndex), List.copyOf(modifiers)));
        }
        return tokens;
    }
}
