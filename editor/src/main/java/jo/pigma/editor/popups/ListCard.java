package jo.pigma.editor.popups;

import android.graphics.Canvas;
import android.graphics.Paint;

/**
 * LA BASE des cartes à LISTE (R-F — quatre cartes portaient chacune
 * leur barre de défilement, leur fenêtre de rangées et leur compte
 * au pied). Une carte de liste vit d'UNE fenêtre sur des rangées :
 * l'en-tête au-dessus, les rangées au milieu (la fenêtre y défile),
 * le pied en dessous, la barre de défilement au bord quand la
 * fenêtre cache des rangées.
 *
 * <p>La base règle LA FENÊTRE : le nombre de rangées visibles s'y
 * resserre à ce que la vue tient (au plafond propre à la carte), la
 * hauteur de la carte en découle, la pose suit ({@link CardBase}).
 * Les rangées s'y ARMEMENT au doigt ({@link #rowAt(float, float)})
 * — la carte sœur qui n'arme pas n'y touche pas, la carte qui arme y
 * lit son index.</p>
 */
public abstract class ListCard extends CardBase {

    /** La première rangée de la fenêtre (le défilement de la carte). */
    protected int topRow;

    /** Le nombre de rangées visibles à la pose courante. */
    protected int visibleRows;

    /**
     * Une carte de liste neuve, à la densité de l'écran.
     *
     * @param density la densité (le dp en pixels)
     */
    protected ListCard(float density) {
        super(density);
    }

    /**
     * La fenêtre se resserre : le plafond de la carte, puis ce que
     * la vue tient — une rangée au moins quand la carte vit.
     *
     * @param maxRows       le plafond propre à la carte
     * @param rows          les rangées vivantes
     * @param fixed         la hauteur fixe autour des rangées (px —
     *                      l'en-tête, le pied, les étages propres)
     * @param rowHeight     la hauteur d'une rangée (px)
     * @param viewHeight    la hauteur de la vue (px)
     * @param margin        la marge aux murs (px)
     * @return le nombre de rangées visibles
     */
    protected int fitRows(int maxRows, int rows, float fixed,
            float rowHeight, float viewHeight, float margin) {
        int window = Math.min(maxRows, rows);
        float available = viewHeight - 2 * margin - fixed;
        while (window > 1 && fixed + window * rowHeight > available) {
            window--;
        }
        return Math.max(1, window);
    }

    /**
     * La rangée que le doigt vise, dans la fenêtre vivante —
     * l'index de la rangée, -1 hors fenêtre (le doigt tombé entre
     * les rangées ne vise rien).
     *
     * @param y        l'ordonnée du doigt (px, coordonnées de la vue)
     * @param rowsTop  le bord haut des rangées (px)
     * @return l'index de la rangée visée, -1 hors rangées
     */
    protected int rowAt(float y, float rowsTop) {
        float cursor = rowsTop;
        for (int at = topRow; at < topRow + visibleRows && at < rowCount(); at++) {
            if (y >= cursor && y < cursor + rowHeight()) {
                return at;
            }
            cursor += rowHeight();
        }
        return -1;
    }

    /** Le nombre de rangées vivantes de la liste. */
    protected abstract int rowCount();

    /** La hauteur d'une rangée (px). */
    protected abstract float rowHeight();

    /**
     * La barre de défilement au bord droit : la fenêtre montre-t-elle
     * moins que la liste ? Le pouce se pose au prorata des rangées
     * visibles, jamais plus petit que 24 dp (le doigt y vit).
     *
     * @param canvas       le canevas de la vue
     * @param paint        l'encre du pouce
     * @param trackTop     le bord haut de la piste (px)
     * @param trackBottom  le bord bas de la piste (px)
     * @param thumbLeft    l'abscisse du bord gauche du pouce (px)
     */
    protected void drawScrollbar(Canvas canvas, Paint paint,
            float trackTop, float trackBottom, float thumbLeft) {
        int rows = rowCount();
        if (visibleRows >= rows || rows <= 1) {
            return;
        }
        float trackH = trackBottom - trackTop;
        float thumbH = Math.max(dp(24),
                trackH * visibleRows / Math.max(1, rows));
        float thumbY = trackTop + (trackH - thumbH) * topRow
                / Math.max(1, rows - visibleRows);
        canvas.drawRect(thumbLeft, thumbY, thumbLeft + dp(2),
                thumbY + thumbH, paint);
    }
}
