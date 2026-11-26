package jo.pigma.editor.folding;

import java.util.ArrayList;
import java.util.List;

/**
 * LE MODÈLE DU RETOUR À LA LIGNE DOUX (le soft wrap à la référence :
 * ses lignes visuelles y coupent la ligne logique à la largeur de la
 * vue, la suite s'indente au premier caractère non blanc —
 * EditorSoftWrapModel dans son implémentation) : chaque ligne
 * logique se découpe en RANGÉES VISUELLES au plafond de colonnes
 * posé par la vue (le wrap se recalcule quand la largeur ou la
 * taille de police bouge).
 *
 * <p>Le modèle vit AU-DESSUS du modèle des plis : l'indice visuel
 * d'une ligne compte les rangées de toutes les lignes visibles
 * d'avant (une ligne non coupée compte UNE rangée — le modèle éteint
 * rend le mapping d'avant). Le découpage s'arrête aux plafonds posés
 * (jamais le document entier pour une largeur) : le harnais éprouve
 * la coupe, l'indent de suite, et le mapping aller-retour.</p>
 */
public final class WrapModel {

    /** Le nombre de rangées par ligne logique (1 : pas de coupe). */
    private int[] rowsPerLine = new int[0];

    /** La ligne où chaque rangée d'une ligne coupée démarre (les index de coupe). */
    private int[] cutColumns = new int[0];

    /** La largeur de coupe en colonnes (0 : le wrap est éteint). */
    private int wrapColumns;

    /** Le nombre de lignes du document au dernier calcul. */
    private int lineCount;

    /**
     * Recalcule le découpage des lignes (le wrap vit : chaque ligne
     * plus large que le plafond se coupe, sa suite s'indente au
     * premier caractère non blanc — la leçon de la référence).
     *
     * @param lengths    la longueur de chaque ligne (sans sa fin), la
     *                   vue la lit au tampon
     * @param indents    la colonne du premier caractère non blanc de
     *                   chaque ligne (l'indent de la suite)
     * @param columns    le plafond de colonnes (0 éteint le wrap)
     */
    public void rebuild(int[] lengths, int[] indents, int columns) {
        wrapColumns = columns;
        lineCount = lengths.length;
        if (rowsPerLine.length < lineCount) {
            rowsPerLine = new int[lineCount];
        }
        int cuts = 0;
        for (int line = 0; line < lineCount; line++) {
            if (columns <= 0 || lengths[line] <= columns) {
                rowsPerLine[line] = 1;
                continue;
            }
            // la coupe : la suite s'indente au premier non-blanc de la
            // ligne (les rangées suivantes y démarrent)
            int indent = Math.min(indents[line], columns - 8);
            int rows = 1;
            int at = columns;
            while (at < lengths[line]) {
                rows++;
                cuts++;
                at += Math.max(8, columns - indent);
            }
            rowsPerLine[line] = rows;
        }
        if (cutColumns.length < cuts) {
            cutColumns = new int[cuts];
        }
        this.cutCount = cuts;
    }

    /** Le compte de coupes du dernier calcul (le harnais le lit). */
    private int cutCount;

    /**
     * Le wrap vit-il ? (le plafond pose et le recalcul a vécu)
     *
     * @return vrai quand au moins une ligne se coupe
     */
    public boolean isWrapped() {
        return wrapColumns > 0 && cutCount > 0;
    }

    /**
     * Le nombre de rangées visuelles d'une ligne (1 sans coupe).
     *
     * @param line l'indice de la ligne
     * @return ses rangées, jamais moins que 1
     */
    public int rowsOf(int line) {
        if (line < 0 || line >= lineCount || wrapColumns <= 0) {
            return 1;
        }
        return rowsPerLine[line];
    }

    /**
     * La rangée d'une ligne coupée démarre à cette colonne (0 : la
     * première rangée ; la coupe d'après pour les suivantes).
     *
     * @param line l'indice de la ligne
     * @param row  la rangée (0-based)
     * @return la colonne de départ de la rangée dans la ligne logique
     */
    public int rowStartColumn(int line, int row) {
        if (wrapColumns <= 0 || row <= 0) {
            return 0;
        }
        // les rangées de la suite démarrent au pas de coupe (le
        // plafond moins l'indent de suite)
        return row * wrapColumns;
    }

    /**
     * Le nombre total de rangées des lignes d'avant (l'indice visuel
     * d'une ligne compte les rangées des lignes d'avant).
     *
     * @param until la première ligne EXCLUE
     * @return la somme des rangées
     */
    public int rowsBefore(int until) {
        if (wrapColumns <= 0 || cutCount == 0) {
            return until;
        }
        int sum = 0;
        for (int line = 0; line < Math.min(until, lineCount); line++) {
            sum += rowsPerLine[line];
        }
        return sum;
    }
}
