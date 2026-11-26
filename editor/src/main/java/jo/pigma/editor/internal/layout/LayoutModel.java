package jo.pigma.editor.internal.layout;

import jo.pigma.core.TextBuffer;
import jo.pigma.editor.folding.FoldModel;

/**
 * La géométrie texte de la vue : la correspondance offset ↔ pixel et
 * ses dépendances (les plis, le soft wrap, les bornes de
 * défilement). Extraite de la vue pour être TESTÉE EN JVM — sans
 * Android, sans canevas : la vue ne garde que le câblage et les
 * pinceaux.
 *
 * <p>Le modèle vit sur UNE source de vérité par dépendance : le
 * tampon porte les offsets, le modèle des plis porte les lignes
 * cachées, le modèle de coupe porte les rangées. Les mesures
 * (corps, interligne, largeur de colonne) se posent par
 * {@link #metrics} — la vue les tient de sa police, le test les
 * pose à la main.</p>
 *
 * <p>LES INVARIANTS (éprouvés par le test) : l'indice visuel d'une
 * ligne cachée vaut celui de la visible qui la suit ; la première
 * ligne visible vue du défilement (y = 0) tombe sur une ligne
 * ENTIÈRE, jamais au milieu d'une ligne coupée ; le défilement
 * horizontal vit au soft wrap coupé (rien n'y dépasse, le plancher
 * reste zéro).</p>
 */
public final class LayoutModel {

    /** Les mesures de la géométrie (posées par la vue ou le test). */
    public static final class Metrics {

        /** Le bord haut du texte (le padding haut de la vue, px). */
        public final float textTop;

        /** L'interligne (une rangée, plis et coupes compris, px). */
        public final float lineHeight;

        /** La largeur d'une colonne (espacement fixe, px). */
        public final float charWidth;

        /** La hauteur visible du texte (hauteur de la vue, px). */
        public final float viewHeight;

        /**
         * Les mesures de la géométrie.
         *
         * @param textTop    le bord haut du texte, en pixels
         * @param lineHeight l'interligne, en pixels
         * @param charWidth  la largeur de colonne, en pixels
         * @param viewHeight la hauteur visible, en pixels
         */
        public Metrics(float textTop, float lineHeight, float charWidth,
                float viewHeight) {
            this.textTop = textTop;
            this.lineHeight = lineHeight;
            this.charWidth = charWidth;
            this.viewHeight = viewHeight;
        }
    }

    private final TextBuffer buffer;
    private final FoldModel folds;

    /** La coupe du soft wrap (null : le soft wrap est éteint). */
    private SoftWrapModel wrap;

    private Metrics metrics =
            new Metrics(0.0f, 1.0f, 1.0f, 0.0f);

    /**
     * Le modèle de la géométrie d'un document.
     *
     * @param buffer le tampon portant les offsets, jamais null
     * @param folds  le modèle des plis du document, jamais null
     */
    public LayoutModel(TextBuffer buffer, FoldModel folds) {
        if (buffer == null || folds == null) {
            throw new NullPointerException("buffer and folds must not be null");
        }
        this.buffer = buffer;
        this.folds = folds;
    }

    /**
     * Pose les mesures courantes (la vue au zoom, le test à la main).
     *
     * @param fresh les mesures, jamais null
     */
    public void metrics(Metrics fresh) {
        if (fresh == null) {
            throw new NullPointerException("metrics must not be null");
        }
        this.metrics = fresh;
    }

    /**
     * Reconstruit la coupe du soft wrap : les longueurs et indents
     * se lisent au tampon, les colonnes viennent de la largeur
     * visible. Une largeur nulle ou négative ÉTEINT la coupe.
     *
     * @param softWrap   vrai pour couper les lignes longues
     * @param viewWidth  la largeur visible du texte, en pixels
     */
    public void rebuildWrap(boolean softWrap, float viewWidth) {
        if (!softWrap || metrics.charWidth <= 0.0f || viewWidth <= 0.0f) {
            wrap = null;
            return;
        }
        int columns = (int) (viewWidth / metrics.charWidth);
        int count = buffer.getLineCount();
        int[] lengths = new int[count];
        int[] indents = new int[count];
        for (int line = 0; line < count; line++) {
            int start = buffer.getLineStart(line);
            int end = buffer.getLineEnd(line);
            lengths[line] = end - start;
            int indent = 0;
            while (start + indent < end
                    && buffer.charAt(start + indent) == ' ') {
                indent++;
            }
            indents[line] = indent;
        }
        wrap = new SoftWrapModel(lengths, indents, columns);
    }

    /**
     * Le nombre de RANGÉES visibles : les lignes cachées n'y
     * comptent pas, les lignes coupées y comptent leurs rangées.
     *
     * @return le nombre de rangées affichables
     */
    public int visibleRowCount() {
        int rows = 0;
        for (int line = 0; line < buffer.getLineCount(); line++) {
            if (!folds.isHidden(line)) {
                rows += rowsOf(line);
            }
        }
        return rows;
    }

    /**
     * Le nombre de rangées d'une ligne (1 sans coupe).
     *
     * @param line l'indice de la ligne
     * @return ses rangées, au moins 1
     */
    public int rowsOf(int line) {
        if (wrap == null) {
            return 1;
        }
        return wrap.rowsOf(line);
    }

    /**
     * L'INDICE VISUEL d'une ligne : les rangées des lignes visibles
     * d'avant se comptent (une ligne cachée vaut l'indice de la
     * visible qui la suit — la règle du modèle des plis reprise au
     * rang près).
     *
     * @param line l'indice de la ligne
     * @return son indice visuel (rangée d'ouverture)
     */
    public int visibleIndexOf(int line) {
        if (wrap == null) {
            return folds.visibleIndexOf(line);
        }
        int rows = 0;
        for (int at = 0; at < line; at++) {
            if (!folds.isHidden(at)) {
                rows += wrap.rowsOf(at);
            }
        }
        return rows;
    }

    /**
     * La LIGNE d'un indice visuel : la dernière ligne dont l'indice
     * visible la précède ou l'égale.
     *
     * @param index l'indice visuel (rangée)
     * @return la ligne qui y commence ou la survit
     */
    public int lineOfVisibleIndex(int index) {
        if (wrap == null) {
            return folds.lineOfVisibleIndex(index, buffer.getLineCount());
        }
        int line = 0;
        int rows = 0;
        while (line < buffer.getLineCount()) {
            if (!folds.isHidden(line)) {
                int ofLine = wrap.rowsOf(line);
                if (rows + ofLine > index) {
                    return line;
                }
                rows += ofLine;
            }
            line++;
        }
        return Math.max(0, buffer.getLineCount() - 1);
    }

    /**
     * Le bord HAUT d'une ligne, défilement exclus : la rangée
     * d'ouverture fois l'interligne, posée depuis le bord haut du
     * texte.
     *
     * @param line l'indice de la ligne
     * @return son bord haut, en pixels hors défilement
     */
    public float lineTop(int line) {
        return metrics.textTop + visibleIndexOf(line) * metrics.lineHeight;
    }

    /**
     * Le défilement VERTICAL maximal : le contenu moins la hauteur
     * visible, jamais négatif (un document court ne défile pas).
     *
     * @return la borne du défilement vertical, en pixels
     */
    public float maxScrollY() {
        float content = metrics.textTop
                + visibleRowCount() * metrics.lineHeight;
        return Math.max(0.0f, content - metrics.viewHeight);
    }

    /**
     * Le défilement HORIZONTAL maximal : la plus large ligne du
     * document moins la largeur visible — NUL au soft wrap (les
     * lignes y coupent, rien ne dépasse, le plancher reste zéro).
     *
     * @param viewWidth la largeur visible du texte, en pixels
     * @return la borne du défilement horizontal, en pixels
     */
    public float maxScrollX(float viewWidth) {
        if (wrap != null) {
            return 0.0f;
        }
        float widest = 0.0f;
        int count = buffer.getLineCount();
        for (int line = 0; line < count; line++) {
            float width = (buffer.getLineEnd(line)
                    - buffer.getLineStart(line)) * metrics.charWidth;
            widest = Math.max(widest, width);
        }
        return Math.max(0.0f, widest - viewWidth);
    }

    /**
     * Le défilement qui RÉVÈLE une ligne entière : le bord haut de
     * la ligne vu du défilement courant, ramené dans la fenêtre (la
     * première ligne visible y tombe ENTIÈRE — jamais au milieu
     * d'une coupée).
     *
     * @param line    l'indice de la ligne à révéler
     * @param scrollY le défilement vertical courant, en pixels
     * @return le défilement corrigé, borné à {@link #maxScrollY()}
     */
    public float revealLine(int line, float scrollY) {
        float top = lineTop(line) - scrollY;
        float bottom = top + rowsOf(line) * metrics.lineHeight;
        float fixed = scrollY;
        if (top < 0.0f) {
            fixed = scrollY + top;
        } else if (bottom > metrics.viewHeight) {
            fixed = scrollY + bottom - metrics.viewHeight;
        }
        return clamp(fixed, 0.0f, maxScrollY());
    }

    /**
     * La LIGNE du doigt : l'indice visuel sous l'ordonnée, borné à
     * la dernière ligne (le doigt sous le texte y tombe).
     *
     * @param y       l'ordonnée du doigt, défilement déduit, px
     * @param maxHeight la hauteur où le doigt se borne (la vue)
     * @return la ligne visée
     */
    public int lineAt(float y, float maxHeight) {
        if (metrics.lineHeight <= 0.0f) {
            return buffer.getLineCount() - 1;
        }
        int index = (int) (y / metrics.lineHeight);
        if (index < 0) {
            return lineOfVisibleIndex(0);
        }
        return lineOfVisibleIndex(
                Math.min(index, visibleRowCount() - 1));
    }

    /**
     * La COLONNE du doigt : l'abscisse en colonnes, bornée à la
     * fin de la ligne visée (le doigt à droite du texte n'y sort
     * pas — l'offset reste DANS la ligne).
     *
     * @param x    l'abscisse du doigt, défilement déduit, px
     * @param line la ligne visée
     * @return la colonne, entre 0 et la longueur de la ligne
     */
    public int columnAt(float x, int line) {
        if (metrics.charWidth <= 0.0f) {
            return 0;
        }
        int column = (int) (x / metrics.charWidth);
        int length = buffer.getLineEnd(line) - buffer.getLineStart(line);
        return Math.max(0, Math.min(column, length));
    }

    /** La borne d'une valeur, aux murs donnés. */
    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(value, max));
    }
}
