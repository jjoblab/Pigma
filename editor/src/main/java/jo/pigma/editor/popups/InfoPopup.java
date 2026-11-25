package jo.pigma.editor.popups;

import android.graphics.Canvas;
import android.graphics.Paint;

import java.util.ArrayList;
import java.util.List;

/**
 * La popup d'info ancrée (le paquet des popups, l'arborescence du
 * prompt) : la documentation rapide d'un mot — un TITRE (le mot,
 * encre pleine, gras) et un CORPS replié à la largeur (multi-lignes,
 * la queue abrégée en « … » au-delà de quatre rangées), au-dessus de
 * la ligne d'ancrage (sous elle sans la place), bornée aux bords de
 * la vue. L'état (titre, corps, ancrage) et le dessin vivent ici ; la
 * vue fournit la géométrie à la trame.
 *
 * <p>Le remplacement tardif ({@link #replace}) ne vaut qu'à la MÊME
 * position : une popup bougée ou rangée n'est pas évincée par une
 * réponse périmée (le survol asynchrone d'un serveur lent). Le
 * remplacement touche au CORPS — le titre (le mot questionné) reste
 * le sien.</p>
 */
public final class InfoPopup {

    /** Le nombre maximal de rangées du corps (la queue s'abrège). */
    private static final int MAX_BODY_ROWS = 8;

    /** Le titre montré (null : aucun titre, ou la popup est rangée). */
    private String title;

    /** Le corps montré (null : la popup est rangée). */
    private String body;

    /** La ligne d'ancrage. */
    private int line;

    /** La colonne d'ancrage. */
    private int column;

    private final Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bodyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint background = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint border = new Paint(Paint.ANTI_ALIAS_FLAG);

    /**
     * Une popup neuve, ses pinceaux propres.
     *
     * @param density la densité de l'écran (le dp en pixels)
     */
    public InfoPopup(float density) {
        titlePaint.setColor(0xFF202124);
        titlePaint.setTextSize(14.0f * density);
        titlePaint.setFakeBoldText(true);
        bodyPaint.setColor(0xFF3C4043);
        bodyPaint.setTextSize(14.0f * density);
        background.setStyle(Paint.Style.FILL);
        background.setColor(0xF2FFFFFF);
        border.setStyle(Paint.Style.STROKE);
        border.setStrokeWidth(density);
        border.setColor(0xFF1B6FE0);
    }

    /**
     * Montre la documentation rapide : un titre (le mot questionné,
     * null pour aucun) et un corps. Le corps vide ou nul range la
     * popup.
     *
     * @param shownTitle le titre, ou null
     * @param shownBody  le corps de la documentation
     * @param atLine     la ligne d'ancrage
     * @param atColumn   la colonne d'ancrage
     */
    public void show(String shownTitle, String shownBody, int atLine, int atColumn) {
        body = shownBody == null || shownBody.isEmpty() ? null : shownBody;
        title = body == null ? null : shownTitle;
        line = atLine;
        column = atColumn;
    }

    /**
     * Montre un corps sans titre — le texte seul à l'ancrage.
     *
     * @param shown  le texte à montrer, vide ou null pour ranger
     * @param atLine la ligne d'ancrage
     * @param atColumn la colonne d'ancrage
     */
    public void show(String shown, int atLine, int atColumn) {
        show(null, shown, atLine, atColumn);
    }

    /**
     * Remplace le CORPS par une réponse TARDIVE — à la même position
     * seulement (une popup bougée ou rangée n'est pas évincée) ; le
     * titre reste le sien.
     *
     * @param shown    la réponse tardive
     * @param atLine   la ligne d'ancrage de la question
     * @param atColumn la colonne d'ancrage de la question
     * @return vrai si la popup vivait encore à cette position
     */
    public boolean replace(String shown, int atLine, int atColumn) {
        if (body == null || line != atLine || column != atColumn) {
            return false;
        }
        body = shown == null || shown.isEmpty() ? body : shown;
        return true;
    }

    /** Range la popup (l'appui suivant, une édition, un débranchement). */
    public void dismiss() {
        body = null;
        title = null;
    }

    /** Le corps montré (null : la popup est rangée). */
    public String text() {
        return body;
    }

    /** Le titre montré (null : aucun ou la popup est rangée). */
    public String title() {
        return title;
    }

    /** La ligne d'ancrage (-1 : la popup est rangée). */
    public int line() {
        return body == null ? -1 : line;
    }

    /** La colonne d'ancrage (-1 : la popup est rangée). */
    public int column() {
        return body == null ? -1 : column;
    }

    /**
     * Dessine la popup : au-dessus de la ligne d'ancrage (sous elle
     * sans la place), bornée aux marges — le titre sur sa rangée,
     * le corps replié à la largeur, la queue abrégée au-delà de
     * quatre rangées ; la popup reste dans la vue.
     *
     * @param canvas    le canevas de la trame
     * @param anchorTop le bord haut de la ligne d'ancrage (px écran)
     * @param metrics   la géométrie de la vue
     */
    public void draw(Canvas canvas, float anchorTop, PopupMetrics metrics) {
        if (body == null) {
            return;
        }
        float density = metrics.density();
        float padding = 10.0f * density;
        float available = metrics.viewWidth() - metrics.paddingLeft()
                - metrics.paddingRight() - 2.0f * padding;
        List<String> rows = wrap(body, bodyPaint, available, MAX_BODY_ROWS);
        float rowHeight = -bodyPaint.getFontMetrics().top + 3.0f * density;
        int rowCount = (title == null ? 0 : 1) + rows.size();
        float boxWidth = boxWidth(available, rows, padding);
        float boxHeight = padding + rowCount * rowHeight + padding / 2.0f;
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
                8.0f * density, 8.0f * density, background);
        canvas.drawRoundRect(left, top, left + boxWidth, top + boxHeight,
                8.0f * density, 8.0f * density, border);
        float baseline = top + padding - bodyPaint.getFontMetrics().top;
        if (title != null) {
            canvas.drawText(title, left + padding, baseline, titlePaint);
            baseline += rowHeight;
        }
        for (String row : rows) {
            canvas.drawText(row, left + padding, baseline, bodyPaint);
            baseline += rowHeight;
        }
    }

    /** La largeur de la boîte : la plus large rangée, bornée. */
    private float boxWidth(float available, List<String> rows, float padding) {
        float widest = 0.0f;
        if (title != null) {
            widest = titlePaint.measureText(title);
        }
        for (String row : rows) {
            widest = Math.max(widest, bodyPaint.measureText(row));
        }
        return Math.min(available, widest) + 2.0f * padding;
    }

    /**
     * Replie le texte à la largeur disponible : les mots s'y suivent,
     * la coupe se fait au mot — un mot seul plus large que la rangée
     * s'y coupe à la largeur ; la queue au-delà de la dernière rangée
     * portée s'abrège en « … ».
     */
    private List<String> wrap(String text, Paint paint, float maxWidth, int maxRows) {
        List<String> rows = new ArrayList<>();
        StringBuilder row = new StringBuilder();
        int at = 0;
        while (at < text.length() && rows.size() < maxRows) {
            int wordEnd = at;
            while (wordEnd < text.length()
                    && !Character.isWhitespace(text.charAt(wordEnd))) {
                wordEnd++;
            }
            String word = text.substring(at, wordEnd);
            if (paint.measureText(word) > maxWidth) {
                while (word.length() > 1
                        && paint.measureText(word + "…") > maxWidth) {
                    word = word.substring(0, word.length() - 1);
                }
                word = word + "…";
            }
            String candidate = row.length() == 0 ? word
                    : row + " " + word;
            if (paint.measureText(candidate) <= maxWidth || row.length() == 0) {
                row.replace(0, row.length(), candidate);
            } else {
                rows.add(row.toString());
                row.setLength(0);
                row.append(word);
            }
            at = wordEnd + 1;
        }
        if (rows.size() == maxRows && at < text.length()) {
            // la queue reste : la dernière rangée l'abrège
            String last = rows.get(maxRows - 1);
            while (last.length() > 1
                    && paint.measureText(last + "…") > maxWidth) {
                last = last.substring(0, last.length() - 1);
            }
            rows.set(maxRows - 1, last + "…");
        } else if (row.length() > 0) {
            rows.add(row.toString());
        }
        return rows;
    }
}
