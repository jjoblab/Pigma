package jo.pigma.lsp;

import jo.pigma.core.TextBuffer;

import org.eclipse.lsp4j.Position;

/**
 * Position du protocole : ligne et caractère en unités de code UTF-16
 * — les mêmes que le tampon, la conversion est directe. Le type
 * d'adaptation de lsp4j se convertit dans les deux sens.
 *
 * @param line      la ligne, zéro-based
 * @param character la colonne, zéro-based, en unités de code UTF-16
 */
public record LspPosition(int line, int character) {

    /**
     * La position du protocole lsp4j équivalente.
     *
     * @return la position lsp4j
     */
    public Position toLsp() {
        return new Position(line, character);
    }

    /**
     * L'adaptation d'une position lsp4j.
     *
     * @param position la position du protocole
     * @return la position du domaine
     */
    public static LspPosition fromLsp(Position position) {
        return new LspPosition(position.getLine(), position.getCharacter());
    }

    /**
     * La position du protocole pour un offset du tampon.
     *
     * @param buffer le tampon portant le texte, jamais {@code null}
     * @param offset l'offset en unités de code UTF-16
     * @return la position équivalente
     */
    public static LspPosition ofOffset(TextBuffer buffer, int offset) {
        int clamped = Math.max(0, Math.min(offset, buffer.length()));
        var position = buffer.positionOfOffset(clamped);
        return new LspPosition(position.line(), position.column());
    }

    /**
     * L'offset du tampon pour une position du protocole : la colonne
     * se borne à la fin de la ligne (une position au-delà reste dans
     * la ligne, jamais dans la suivante).
     *
     * @param buffer le tampon portant le texte, jamais {@code null}
     * @return l'offset équivalent
     */
    public int toOffset(TextBuffer buffer) {
        int line = Math.max(0, Math.min(this.line, buffer.getLineCount() - 1));
        int start = buffer.getLineStart(line);
        int end = buffer.getLineEnd(line);
        return start + Math.max(0, Math.min(character, end - start));
    }
}
