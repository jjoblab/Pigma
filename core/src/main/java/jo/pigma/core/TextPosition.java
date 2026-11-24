package jo.pigma.core;

/**
 * Position dans le texte : ligne et colonne, toutes deux comptées à partir
 * de zéro. La colonne compte les unités de code UTF-16 depuis le début de
 * la ligne (une paire de substitution occupe deux colonnes).
 *
 * <p>Instance immuable : safe à conserver ou à comparer.</p>
 *
 * @param line   numéro de ligne, à partir de zéro
 * @param column colonne en unités de code UTF-16, à partir de zéro
 */
public record TextPosition(int line, int column) {

    public TextPosition {
        if (line < 0) {
            throw new IllegalArgumentException("line must not be negative");
        }
        if (column < 0) {
            throw new IllegalArgumentException("column must not be negative");
        }
    }
}
