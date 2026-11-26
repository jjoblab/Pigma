package jo.pigma.editor.popups;

import android.graphics.Canvas;
import android.graphics.Paint;

import java.util.List;

import jo.pigma.editor.EditorView;

/**
 * La carte de l'historique local, DESSINÉE DANS LA VUE : l'histoire
 * d'une PARTIE du code — les traces qui touchent la sélection (du
 * dernier au premier), chacune une rangée : l'heure, le geste, la
 * ligne d'alors ; le doigt sur une rangée la DÉPLIE (la ligne
 * d'alors repliée à la largeur), la pastille « Restaurer » y remet
 * la ligne du passé — une unité d'annulation, l'histoire s'y
 * enregistre comme elle s'est faite. L'en-tête porte le titre et la
 * plage, la croix la range.
 *
 * <p>La carte se pose sous la sélection (au-dessus de la barre sans
 * la place), bornée aux murs ; la carte ne prend jamais le focus,
 * elle ne survit ni à sa restauration ni à la sélection morte.</p>
 */
public final class HistoryCard {

    /** La hauteur de l'en-tête (dp). */
    private static final int HEADER_DP = 46;

    /** La hauteur du pied de carte (dp). */
    private static final int FOOTER_DP = 30;

    /** La hauteur d'une ligne de texte dépliée (dp). */
    private static final int EXPAND_ROW_DP = 22;

    /** Le plafond de lignes dépliées d'une trace (dp au-delà : « … »). */
    private static final int EXPAND_ROWS = 4;

    /** La hauteur de la pastille Restaurer (dp). */
    private static final int RESTORE_DP = 38;

    /** La marge aux murs de la vue (dp). */
    private static final int MARGIN_DP = 8;

    /** L'écart à la sélection (dp). */
    private static final int GAP_DP = 4;

    /** Le plafond de traces visibles (la carte défile au-delà). */
    private static final int MAX_VISIBLE_TRACES = 6;

    /** Les traces de la plage, du dernier au premier (vides : l'en-tête seul). */
    private List<LocalHistory.Version> traces = List.of();

    /** La carte vit-elle ? (l'ouverture la pose, la fermeture la range). */
    private boolean shown;

    /** La première ligne de la plage questionnée. */
    private int fromLine;

    /** La dernière ligne de la plage questionnée. */
    private int toLine;

    /** L'index de la trace dépliée (-1 : aucune). */
    private int expanded = -1;

    /** La première trace de la fenêtre (le défilement de la carte). */
    private int topTrace;

    private float poseX;

    private float poseY;

    private float cardW;

    private float cardH;

    /** Le nombre de traces visibles à la pose courante. */
    private int visibleTraces;

    private float density = 1.0f;

    /** Le style posé par l'hôte : fond translucide, encres, tailles, largeur, forme. */
    private PopupStyle style = PopupStyle.defaults();

    /** Les chaînes posées par l'hôte (le titre localisé — la source unique). */
    private PopupStrings strings = PopupStrings.defaults();

    private EditorView editor;

    private final Paint cardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint spanPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint timePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint snippetPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint footerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glyphPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint restorePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint restoreTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    /**
     * Une carte neuve, ses pinceaux propres au style par défaut.
     *
     * @param density la densité de l'écran (le dp en pixels)
     */
    public HistoryCard(float density) {
        this.density = density;
        applyStyle();
    }

    /**
     * Pose le style de l'hôte : le fond translucide, les encres, la
     * taille des textes, la largeur des cartes, la forme — les
     * pinceaux s'y reconfigurent entiers.
     *
     * @param fresh le style posé, null pour celui par défaut
     */
    public void setStyle(PopupStyle fresh) {
        style = fresh == null ? PopupStyle.defaults() : fresh;
        applyStyle();
    }

    /** Le style courant (le harnais le lit). */
    public PopupStyle style() {
        return style;
    }

    /**
     * Pose les chaînes de l'hôte (le titre localisé — la source unique).
     *
     * @param fresh les chaînes posées, null pour celles par défaut
     */
    public void setStrings(PopupStrings fresh) {
        strings = fresh == null ? PopupStrings.defaults() : fresh;
    }

    /** La hauteur d'une rangée repliée : le style la porte. */
    private float rowHeight() {
        return dp(style.rowHeightDp() - 2);
    }

    /** Reconfigure les pinceaux au style courant. */
    private void applyStyle() {
        cardPaint.setColor(style.backgroundArgb());
        borderPaint.setColor(style.borderArgb());
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(1.0f);
        titlePaint.setColor(style.textArgb());
        titlePaint.setFakeBoldText(true);
        titlePaint.setTextSize((style.textSizeSp() - 2.0f) * density);
        spanPaint.setColor(style.accentArgb());
        spanPaint.setTextSize((style.secondarySizeSp() - 2.0f) * density);
        textPaint.setColor(style.textArgb());
        textPaint.setTextSize((style.secondarySizeSp() - 1.0f) * density);
        timePaint.setColor(style.hintArgb());
        timePaint.setTextSize((style.secondarySizeSp() - 2.0f) * density);
        snippetPaint.setColor(style.secondaryArgb());
        snippetPaint.setTextSize((style.secondarySizeSp() - 2.0f) * density);
        footerPaint.setColor(style.hintArgb());
        footerPaint.setTextSize((style.secondarySizeSp() - 2.0f) * density);
        glyphPaint.setColor(style.secondaryArgb());
        glyphPaint.setStyle(Paint.Style.STROKE);
        glyphPaint.setStrokeWidth(1.4f);
        restorePaint.setColor(style.accentArgb());
        restoreTextPaint.setColor(0xFFFFFFFF);
        restoreTextPaint.setTextSize((style.secondarySizeSp() - 2.0f) * density);
    }

    /**
     * Branche la carte à son éditeur : la restauration y parle (une
     * unité d'annulation par restauration).
     *
     * @param editor l'éditeur restauré
     */
    public void bind(EditorView editor) {
        this.editor = editor;
    }

    /**
     * Ouvre la carte sur les traces de la plage — l'histoire d'une
     * partie du code (la plage dit laquelle), du dernier au premier ;
     * la liste vide se pose quand même (l'en-tête le dit : aucun
     * passé).
     *
     * @param lines les traces de la plage, du dernier au premier
     * @param firstLine la première ligne de la plage (humaine, 1-based)
     * @param lastLine  la dernière ligne de la plage (humaine, 1-based)
     */
    public void open(List<LocalHistory.Version> lines, int firstLine, int lastLine) {
        traces = List.copyOf(lines);
        fromLine = firstLine;
        toLine = lastLine;
        expanded = -1;
        topTrace = 0;
        shown = true;
    }

    /** Ferme la carte (la croix, la restauration, la sélection morte). */
    public void close() {
        traces = List.of();
        expanded = -1;
        topTrace = 0;
        shown = false;
    }

    /** La carte vit-elle ? (posée par l'ouverture, rangée par la croix) */
    public boolean isShown() {
        return shown;
    }

    /**
     * Le doigt tombé sur la carte lui appartient : la croix la range,
     * la rangée d'une trace la déplie (ou la replie), la pastille
     * Restaurer y remet la ligne du passé ; le reste consomme sans
     * agir. Hors de la carte : faux.
     *
     * @param x l'abscisse du doigt (px, coordonnées de la vue)
     * @param y l'ordonnée du doigt (px, coordonnées de la vue)
     * @return vrai quand la carte a pris le geste
     */
    public boolean onTouchArea(float x, float y) {
        if (!shown || x < poseX || x >= poseX + cardW
                || y < poseY || y >= poseY + cardH) {
            return false;
        }
        // la croix : la carte se range
        float crossLeft = poseX + cardW - dp(32);
        if (y < poseY + dp(HEADER_DP)
                && x >= crossLeft && x <= poseX + cardW - dp(8)) {
            close();
            return true;
        }
        if (traces.isEmpty()) {
            return true;
        }
        float cursor = poseY + dp(HEADER_DP);
        for (int at = topTrace; at < topTrace + visibleTraces
                && at < traces.size(); at++) {
            float rowH = rowHeight()
                    + (at == expanded ? expandedHeight(at) : 0.0f);
            if (y >= cursor && y < cursor + rowH) {
                if (at == expanded) {
                    // la pastille Restaurer vit au bas de la rangée
                    // dépliée : la moitié haute replie, la pastille
                    // restaure
                    float restoreTop = cursor + rowHeight()
                            + expandedHeight(at) - dp(RESTORE_DP) - dp(6);
                    if (y >= restoreTop) {
                        restore(traces.get(at));
                    } else {
                        expanded = -1;
                    }
                } else {
                    expanded = at;
                }
                return true;
            }
            cursor += rowH;
        }
        return true;
    }

    /**
     * Dessine la carte : la pose se recalcule d'abord (sous la
     * sélection, au-dessus de la barre sans la place, bornée aux
     * murs), puis l'en-tête (le titre, la plage, la croix), les
     * traces de la fenêtre (l'heure, le geste, la ligne d'alors —
     * repliée en une rangée, dépliée à la largeur avec sa pastille
     * Restaurer), le pied (le compte).
     *
     * @param canvas    le canevas de la vue
     * @param anchorTop le bord haut de la barre de sélection (px, vue)
     * @param anchorBottom le bord bas de la sélection (px, vue)
     * @param centerX   le centre horizontal de la sélection (px)
     * @param metrics   la géométrie courante de la vue
     */
    public void draw(Canvas canvas, float anchorTop, float anchorBottom,
            float centerX, PopupMetrics metrics) {
        density = metrics.density();
        if (!shown) {
            cardH = 0.0f;
            return;
        }
        place(anchorTop, anchorBottom, centerX, metrics);
        float radius = dp(style.cornerRadiusDp());
        canvas.drawRoundRect(poseX, poseY, poseX + cardW, poseY + cardH,
                radius, radius, cardPaint);
        canvas.drawRoundRect(poseX, poseY, poseX + cardW, poseY + cardH,
                radius, radius, borderPaint);
        drawHeader(canvas);
        float cursor = poseY + dp(HEADER_DP);
        for (int at = topTrace; at < topTrace + visibleTraces
                && at < traces.size(); at++) {
            drawTrace(canvas, traces.get(at), cursor, at == expanded, at);
            cursor += rowHeight() + (at == expanded ? expandedHeight(at) : 0.0f);
        }
        drawFooter(canvas, cursor);
    }

    /** L'abscisse posée (le harnais la lit). */
    public float poseX() {
        return poseX;
    }

    /** L'ordonnée posée (le harnais la lit). */
    public float poseY() {
        return poseY;
    }

    /** La largeur de la carte (le harnais la lit). */
    public float width() {
        return cardW;
    }

    /** La hauteur de la carte (le harnais la lit). */
    public float height() {
        return cardH;
    }

    /** Les traces posées (le harnais les compte). */
    public int traceCount() {
        return traces.size();
    }

    /**
     * L'ordonnée du centre d'une trace visible (le harnais y vise le
     * doigt).
     *
     * @param trace l'index de la trace dans la fenêtre (du dernier au
     *              premier)
     * @return l'ordonnée du centre, -1 hors carte
     */
    public float traceCenterY(int trace) {
        if (!shown || trace < 0 || trace >= visibleTraces) {
            return -1.0f;
        }
        float cursor = poseY + dp(HEADER_DP);
        for (int at = topTrace; at < topTrace + visibleTraces
                && at < traces.size(); at++) {
            if (at - topTrace == trace) {
                return cursor + rowHeight() / 2.0f;
            }
            cursor += rowHeight() + (at == expanded ? expandedHeight(at) : 0.0f);
        }
        return -1.0f;
    }

    /** La trace dépliée (le harnais la lit, -1 : aucune). */
    public int expandedTrace() {
        return expanded;
    }

    /**
     * L'ordonnée du centre de la pastille Restaurer d'une trace
     * dépliée (le harnais y vise le doigt).
     *
     * @param trace l'index de la trace dans la liste posée
     * @return l'ordonnée du centre, -1 hors carte ou non dépliée
     */
    public float restoreChipY(int trace) {
        if (!shown || trace != expanded || trace < 0 || trace >= traces.size()) {
            return -1.0f;
        }
        float cursor = poseY + dp(HEADER_DP);
        for (int at = topTrace; at <= trace; at++) {
            if (at == trace) {
                return cursor + rowHeight() + expandedHeight(at)
                        - dp(RESTORE_DP) / 2.0f - dp(6);
            }
            cursor += rowHeight() + (at == expanded ? expandedHeight(at) : 0.0f);
        }
        return -1.0f;
    }

    /**
     * Pose la carte sous la sélection (au-dessus de la barre sans la
     * place), le centre borné aux murs ; la fenêtre se resserre à la
     * place de la vue.
     */
    private void place(float anchorTop, float anchorBottom,
            float centerX, PopupMetrics metrics) {
        float margin = dp(MARGIN_DP);
        float gap = dp(GAP_DP);
        cardW = Math.min(Math.min(dp(style.maxCardWidthDp() - 60),
                metrics.viewWidth() - 2 * margin),
                Math.max(dp(style.minCardWidthDp() - 60),
                        metrics.viewWidth() - 2 * margin));
        visibleTraces = Math.min(MAX_VISIBLE_TRACES, traces.size());
        cardH = dp(HEADER_DP) + visibleTraces * rowHeight()
                + (expanded >= 0 ? expandedHeight(expanded) : 0.0f) + dp(FOOTER_DP);
        float belowTop = anchorBottom + gap;
        float aboveBottom = anchorTop - gap;
        if (belowTop + cardH <= metrics.viewHeight() - margin) {
            poseY = belowTop;
        } else if (aboveBottom - cardH >= margin) {
            poseY = aboveBottom - cardH;
        } else {
            poseY = Math.max(margin,
                    Math.min(belowTop, metrics.viewHeight() - cardH - margin));
        }
        poseX = Math.max(margin,
                Math.min(centerX - cardW / 2.0f,
                        metrics.viewWidth() - cardW - margin));
    }

    /** L'en-tête : le titre, la plage questionnée, la croix. */
    private void drawHeader(Canvas canvas) {
        float titleX = poseX + dp(14);
        float titleY = poseY + dp(HEADER_DP) * 0.62f;
        canvas.drawText(strings.history(), titleX, titleY, titlePaint);
        String span = fromLine == toLine
                ? "ligne " + fromLine : "lignes " + fromLine + "–" + toLine;
        canvas.drawText(span, titleX, poseY + dp(HEADER_DP) * 0.92f, spanPaint);
        // la croix
        float cx = poseX + cardW - dp(20);
        float cy = poseY + dp(HEADER_DP) / 2.0f;
        float s = dp(5);
        canvas.drawLine(cx - s, cy - s, cx + s, cy + s, glyphPaint);
        canvas.drawLine(cx - s, cy + s, cx + s, cy - s, glyphPaint);
    }

    /**
     * Une trace : l'heure et le geste sur la première ligne, la ligne
     * d'alors dessous (une rangée repliée) ; dépliée, la ligne se
     * replie à la largeur (au plus {@value #EXPAND_ROWS} rangées) et
     * la pastille Restaurer vit au bas.
     */
    private void drawTrace(Canvas canvas, LocalHistory.Version trace,
            float top, boolean unfolded, int at) {
        float rowH = rowHeight();
        String when = moment(trace.millis());
        String caption = when + " · " + trace.gesture();
        canvas.drawText(caption, poseX + dp(14), top + rowH * 0.34f, timePaint);
        String snippet = trace.before().isEmpty() ? "— plage vidée —"
                : trace.before();
        if (!unfolded) {
            String shown = shrink(snippet, snippetPaint,
                    cardW - dp(28) - dp(14));
            canvas.drawText(shown, poseX + dp(14), top + rowH * 0.78f, snippetPaint);
            return;
        }
        // dépliée : la ligne d'alors repliée à la largeur
        float cursor = top + rowH * 0.78f;
        List<String> rows = wrap(snippet, snippetPaint,
                cardW - dp(28), EXPAND_ROWS);
        for (String row : rows) {
            canvas.drawText(row, poseX + dp(14), cursor, snippetPaint);
            cursor += dp(EXPAND_ROW_DP);
        }
        // la pastille Restaurer au bas de la rangée dépliée
        float chipW = restoreWidth();
        float chipLeft = poseX + dp(14);
        float chipTop = top + rowH + expandedHeight(at) - dp(RESTORE_DP) - dp(6);
        canvas.drawRoundRect(chipLeft, chipTop, chipLeft + chipW,
                chipTop + dp(RESTORE_DP), dp(RESTORE_DP) / 2.0f,
                dp(RESTORE_DP) / 2.0f, restorePaint);
        String label = "Restaurer la ligne";
        canvas.drawText(label,
                chipLeft + (chipW - restoreTextPaint.measureText(label)) / 2.0f,
                chipTop + dp(RESTORE_DP) * 0.66f, restoreTextPaint);
    }

    /** Le pied de carte : le compte des traces, l'ordre. */
    private void drawFooter(Canvas canvas, float top) {
        String count = traces.isEmpty() ? "aucun passé pour cette plage"
                : traces.size() == 1 ? "1 trace · la plus récente d'abord"
                : traces.size() + " traces · la plus récente d'abord";
        canvas.drawText(count, poseX + dp(14), top + dp(FOOTER_DP) * 0.66f,
                footerPaint);
    }

    /** La hauteur du dépli d'une trace (au-delà de sa rangée). */
    private float expandedHeight(int at) {
        if (at < 0 || at >= traces.size()) {
            return 0.0f;
        }
        List<String> rows = wrap(traces.get(at).before().isEmpty()
                ? "— plage vidée —" : traces.get(at).before(),
                snippetPaint, cardW - dp(28), EXPAND_ROWS);
        // le dépli : les rangées au-delà de la première (la première
        // vit dans la rangée), la pastille, la respiration
        return (rows.size() - 1) * dp(EXPAND_ROW_DP) + dp(RESTORE_DP) + dp(10);
    }

    /** La largeur de la pastille Restaurer. */
    private float restoreWidth() {
        return restoreTextPaint.measureText("Restaurer la ligne") + 2 * dp(14);
    }

    /** La restauration : la ligne du passé remet, la carte se range. */
    private void restore(LocalHistory.Version trace) {
        if (editor != null) {
            editor.restoreHistoryVersion(trace);
            close();
        }
    }

    /** L'heure d'une trace (le moment du geste, à la minute près). */
    private String moment(long millis) {
        java.util.Calendar when = java.util.Calendar.getInstance();
        when.setTimeInMillis(millis);
        return String.format(java.util.Locale.ROOT, "%02d:%02d:%02d",
                when.get(java.util.Calendar.HOUR_OF_DAY),
                when.get(java.util.Calendar.MINUTE),
                when.get(java.util.Calendar.SECOND));
    }

    /**
     * Replie le texte à la largeur disponible : les mots s'y
     * suivent, la coupe se fait au mot — la queue au-delà de la
     * dernière rangée portée s'abrège en « … ».
     */
    private List<String> wrap(String text, Paint paint, float maxWidth, int maxRows) {
        List<String> rows = new java.util.ArrayList<>();
        StringBuilder row = new StringBuilder();
        int at = 0;
        while (at < text.length() && rows.size() < maxRows) {
            int wordEnd = at;
            while (wordEnd < text.length()
                    && !Character.isWhitespace(text.charAt(wordEnd))) {
                wordEnd++;
            }
            String word = text.substring(at, wordEnd);
            String candidate = row.length() == 0 ? word : row + " " + word;
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

    /**
     * Coupe un texte à la largeur : la TÊTE se garde, la queue
     * s'abrège en « … ».
     */
    private String shrink(String text, Paint paint, float maxWidth) {
        if (paint.measureText(text) <= maxWidth || text.length() < 2) {
            return text;
        }
        String head = text.substring(0, text.length() - 1);
        while (head.length() > 1 && paint.measureText(head + "…") > maxWidth) {
            head = head.substring(0, head.length() - 1);
        }
        return head + "…";
    }

    private float dp(int value) {
        return value * density;
    }
}
