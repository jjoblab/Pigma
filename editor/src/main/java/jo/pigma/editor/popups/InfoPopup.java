package jo.pigma.editor.popups;

import android.graphics.Canvas;
import android.graphics.Paint;

/**
 * La popup d'info ancrée (le paquet des popups, l'arborescence du
 * prompt) : un rectangle arrondi au-dessus de la ligne d'ancrage (sous
 * elle sans la place), borné aux bords de la vue, le texte coupé à la
 * largeur disponible. L'état (texte, ancrage) et le dessin vivent ici ;
 * la vue fournit la géométrie à la trame.
 *
 * <p>Le remplacement tardif ({@link #replace}) ne vaut qu'à la MÊME
 * position : une popup bougée ou rangée n'est pas évincée par une
 * réponse périmée (le survol asynchrone d'un serveur lent).</p>
 */
public final class InfoPopup {

    /** Le texte montré (null : la popup est rangée). */
    private String text;

    /** La ligne d'ancrage. */
    private int line;

    /** La colonne d'ancrage. */
    private int column;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint background = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint border = new Paint(Paint.ANTI_ALIAS_FLAG);

    /**
     * Une popup neuve, ses pinceaux propres.
     *
     * @param density la densité de l'écran (le dp en pixels)
     */
    public InfoPopup(float density) {
        paint.setColor(0xFF1B1B1F);
        paint.setTextSize(12.0f * density);
        background.setStyle(Paint.Style.FILL);
        background.setColor(0xF2FFFFFF);
        border.setStyle(Paint.Style.STROKE);
        border.setStrokeWidth(density);
        border.setColor(0xFF1B6FE0);
    }

    /**
     * Montre (ou range) la popup : le texte vide la range.
     *
     * @param shown le texte à montrer, vide ou null pour ranger
     * @param atLine la ligne d'ancrage
     * @param atColumn la colonne d'ancrage
     */
    public void show(String shown, int atLine, int atColumn) {
        text = shown == null || shown.isEmpty() ? null : shown;
        line = atLine;
        column = atColumn;
    }

    /**
     * Remplace le texte par une réponse TARDIVE — à la même position
     * seulement (une popup bougée ou rangée n'est pas évincée).
     *
     * @param shown la réponse tardive
     * @param atLine la ligne d'ancrage de la question
     * @param atColumn la colonne d'ancrage de la question
     * @return vrai si la popup vivait encore à cette position
     */
    public boolean replace(String shown, int atLine, int atColumn) {
        if (text == null || line != atLine || column != atColumn) {
            return false;
        }
        text = shown == null || shown.isEmpty() ? text : shown;
        return true;
    }

    /** Range la popup (l'appui suivant, une édition, un débranchement). */
    public void dismiss() {
        text = null;
    }

    /** Le texte montré (null : la popup est rangée). */
    public String text() {
        return text;
    }

    /** La ligne d'ancrage (-1 : la popup est rangée). */
    public int line() {
        return text == null ? -1 : line;
    }

    /** La colonne d'ancrage (-1 : la popup est rangée). */
    public int column() {
        return text == null ? -1 : column;
    }

    /**
     * Dessine la popup : au-dessus de la ligne d'ancrage (sous elle
     * sans la place), bornée aux marges, le texte coupé à la place
     * disponible — les mots longs se tronquent, la popup reste dans
     * la vue.
     *
     * @param canvas    le canevas de la trame
     * @param anchorTop le bord haut de la ligne d'ancrage (px écran)
     * @param metrics   la géométrie de la vue
     */
    public void draw(Canvas canvas, float anchorTop, PopupMetrics metrics) {
        if (text == null) {
            return;
        }
        float density = metrics.density();
        float textWidth = paint.measureText(text);
        float padding = 8.0f * density;
        float boxWidth = Math.min(textWidth + 2 * padding,
                metrics.viewWidth() - metrics.paddingLeft() - metrics.paddingRight());
        float boxHeight = -paint.getFontMetrics().top + 2.0f * density + padding;
        float left = Math.min(
                Math.max(metrics.paddingLeft(),
                        metrics.paddingLeft() + column * metrics.charWidth() - padding),
                metrics.viewWidth() - metrics.paddingRight() - boxWidth);
        float top;
        if (anchorTop - boxHeight - 4.0f * density >= metrics.paddingTop()) {
            top = anchorTop - boxHeight - 4.0f * density;
        } else {
            top = Math.min(anchorTop + metrics.lineHeight() + 4.0f * density,
                    metrics.viewHeight() - boxHeight);
        }
        canvas.drawRoundRect(left, top, left + boxWidth, top + boxHeight,
                4.0f * density, 4.0f * density, background);
        canvas.drawRoundRect(left, top, left + boxWidth, top + boxHeight,
                4.0f * density, 4.0f * density, border);
        float maxChars = Math.max(1.0f,
                (boxWidth - 2 * padding) / metrics.charWidth());
        String shown = text.length() > maxChars
                ? text.substring(0, (int) maxChars) : text;
        canvas.drawText(shown, left + padding, top - paint.getFontMetrics().top,
                paint);
    }
}
