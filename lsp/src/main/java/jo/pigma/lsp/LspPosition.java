package jo.pigma.lsp;

import jo.pigma.core.TextBuffer;

/**
 * Position du protocole : ligne et caractère en unités de code UTF-16
 * — les mêmes que le tampon, la conversion est directe.
 *
 * @param line      la ligne, zéro-based
 * @param character la colonne, zéro-based, en unités de code UTF-16
 */
public record LspPosition(int line, int character) {

    static LspPosition fromJson(JsonObject object) {
        Double line = object.getNumber("line");
        Double character = object.getNumber("character");
        if (line == null || character == null) {
            throw new IllegalArgumentException("position sans ligne ou colonne");
        }
        return new LspPosition((int) (double) line, (int) (double) character);
    }

    /**
     * La position en JSON.
     *
     * @return l'objet JSON de la position
     */
    public JsonObject toJson() {
        return JsonObject.of("line", JsonNumber.of(line),
                "character", JsonNumber.of(character));
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
