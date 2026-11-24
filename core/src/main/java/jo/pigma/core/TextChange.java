package jo.pigma.core;

/**
 * Description d'une édition atomique : remplacer la plage
 * {@code [start, end)} par un texte donné. Une insertion est le cas
 * particulier {@code start == end}, une suppression celui d'un texte vide.
 *
 * <p>Instance immuable : safe à conserver ou à comparer. Les bornes sont
 * vérifiées au moment de l'application, pas à la construction (la longueur
 * du texte n'est pas connue ici).</p>
 *
 * @param start début de la plage remplacée, en unités de code UTF-16
 * @param end   fin de la plage remplacée, exclue
 * @param text  texte de remplacement, jamais {@code null}
 */
public record TextChange(int start, int end, String text) {

    public TextChange {
        if (start > end) {
            throw new IllegalArgumentException("start must not exceed end");
        }
        if (text == null) {
            throw new NullPointerException("text must not be null");
        }
    }

    /**
     * Remplace la plage {@code [start, end)} par le texte donné.
     *
     * @param start début de la plage, inclus
     * @param end   fin de la plage, exclue
     * @param text  texte de remplacement, jamais {@code null}
     * @return le changement décrit, prêt à appliquer
     */
    public static TextChange replace(int start, int end, CharSequence text) {
        return new TextChange(start, end, text.toString());
    }

    /**
     * Insère le texte à l'offset donné.
     *
     * @param offset position d'insertion, en unités de code UTF-16
     * @param text  texte inséré, jamais {@code null}
     * @return le changement décrit, prêt à appliquer
     */
    public static TextChange insert(int offset, CharSequence text) {
        return new TextChange(offset, offset, text.toString());
    }

    /**
     * Supprime la plage {@code [start, end)}.
     *
     * @param start début de la plage, inclus
     * @param end   fin de la plage, exclue
     * @return le changement décrit, prêt à appliquer
     */
    public static TextChange delete(int start, int end) {
        return new TextChange(start, end, "");
    }
}
