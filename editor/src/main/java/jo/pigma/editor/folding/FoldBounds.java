package jo.pigma.editor.folding;

import jo.pigma.syntax.StructureDocument;

/**
 * Les bornes des plis d'une ligne : l'ouvrant y ouvre-t-il un pli, et
 * jusqu'où le pli couvre-t-il. Le document de structure (le lexer
 * local) y répond ; les plis du SERVEUR de langage y répondent
 * pareillement — la vue y compose les deux (l'ouvrant local d'abord,
 * le pli du serveur complète).
 */
public interface FoldBounds {

    /**
     * Un pli ouvre-t-il à cette ligne.
     *
     * @param line l'indice de la ligne
     * @return vrai quand la ligne porte un en-tête de pli
     */
    boolean foldStartsAt(int line);

    /**
     * La dernière ligne couverte par le pli ouvert à cette ligne.
     *
     * @param header la ligne d'en-tête du pli
     * @return l'indice de la dernière ligne couverte
     */
    int foldEndLine(int header);

    /**
     * L'adaptateur du document de structure (les plis du lexer
     * local).
     *
     * @param structure le document de structure, null pour aucun
     * @return les bornes du document, null si le document est null
     */
    static FoldBounds of(StructureDocument structure) {
        if (structure == null) {
            return null;
        }
        return new FoldBounds() {
            @Override
            public boolean foldStartsAt(int line) {
                return structure.foldStartsAt(line);
            }

            @Override
            public int foldEndLine(int header) {
                return structure.foldEndLine(header);
            }
        };
    }
}
