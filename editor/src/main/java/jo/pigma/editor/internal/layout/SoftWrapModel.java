package jo.pigma.editor.internal.layout;

/**
 * La coupe du soft wrap, PURE : combien de rangées une ligne occupe
 * à largeur donnée. Miroir testable du {@code WrapModel} de la vue
 * (qui garde son rôle — le LayoutModel y délègue) : les règles y
 * sont les mêmes, éprouvées ici sans Android.
 *
 * <p>La coupe : une ligne tient {@code columns} colonnes ; au-delà,
 * la suite se coupe par rangées de {@code step} colonnes (au moins
 * 8), indentées du premier non-blanc de la ligne — la continuité
 * visuelle du bloc s'y lit. Une ligne plus courte que la largeur
 * tient UNE rangée.</p>
 */
final class SoftWrapModel {

    /** Le seuil d'une coupe (le pas des rangées de suite). */
    private static final int MIN_STEP = 8;

    private final int[] lengths;
    private final int[] indents;
    private final int columns;

    /**
     * La coupe d'un document.
     *
     * @param lengths la longueur de chaque ligne, en colonnes
     * @param indents le premier non-blanc de chaque ligne
     * @param columns la largeur visible, en colonnes (0 : rien ne coupe)
     */
    SoftWrapModel(int[] lengths, int[] indents, int columns) {
        this.lengths = lengths;
        this.indents = indents;
        this.columns = Math.max(0, columns);
    }

    /** La coupe vit-elle (au moins une ligne dépasse la largeur) ? */
    boolean isWrapped() {
        for (int length : lengths) {
            if (length > columns) {
                return true;
            }
        }
        return false;
    }

    /**
     * Le nombre de rangées d'une ligne : 1 si elle tient, sinon une
     * par tranches de {@code step} colonnes au-delà de la première.
     *
     * @param line l'indice de la ligne
     * @return ses rangées, au moins 1
     */
    int rowsOf(int line) {
        int length = lengths[line];
        if (length <= columns || columns <= 0) {
            return 1;
        }
        int indent = Math.min(indents[line], Math.max(0, columns - 1));
        int step = Math.max(MIN_STEP, columns - indent);
        int rest = length - columns;
        return 1 + (rest + step - 1) / step;
    }

    /**
     * La PREMIÈRE colonne d'une rangée : 0 pour la première, la
     * largeur plus les pas d'avant pour les suivantes.
     *
     * @param line l'indice de la ligne
     * @param row  la rangée, 0 à {@link #rowsOf(int)} exclu
     * @return la colonne où la rangée commence
     */
    int rowStartColumn(int line, int row) {
        if (row <= 0 || columns <= 0) {
            return 0;
        }
        int indent = Math.min(indents[line], Math.max(0, columns - 1));
        int step = Math.max(MIN_STEP, columns - indent);
        return Math.min(lengths[line], columns + (row - 1) * step);
    }
}
