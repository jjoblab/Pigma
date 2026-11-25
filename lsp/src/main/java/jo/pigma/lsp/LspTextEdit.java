package jo.pigma.lsp;

import org.eclipse.lsp4j.TextEdit;

/**
 * Le remplacement d'une plage par un texte : la brique du protocole
 * dont se servent la correction rapide (le correctif), le
 * renommage (chaque occurrence remplacée) et le formatage (chaque
 * ligne réécrite). La plage vit dans les coordonnées du document
 * AVANT le remplacement — les remplacements d'une même réponse se
 * posent du dernier au premier pour que les plages ne bougent pas.
 *
 * @param range   la plage remplacée
 * @param newText le texte mis à la place
 */
public record LspTextEdit(LspRange range, String newText) {

    /**
     * L'adaptation d'un remplacement lsp4j.
     *
     * @param edit le remplacement du protocole
     * @return le remplacement du domaine
     */
    public static LspTextEdit fromLsp(TextEdit edit) {
        return new LspTextEdit(LspRange.fromLsp(edit.getRange()),
                edit.getNewText() == null ? "" : edit.getNewText());
    }
}
