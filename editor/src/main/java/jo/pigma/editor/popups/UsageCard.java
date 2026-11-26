package jo.pigma.editor.popups;

import jo.pigma.editor.EditorView;

import android.graphics.Canvas;
import android.graphics.Paint;

import java.util.List;

/**
 * La carte des USAGES d'un symbole : chaque référence y vit en rangée
 * — l'icône d'usage à gauche, le nom du symbole, la ligne et le
 * fichier à droite — le doigt sur une rangée y fait sauter le
 * curseur (la leçon de la référence : sa fenêtre Find Usages y
 * liste chaque occurrence, le choix y navigue). Coiffée d'un titre
 * (« Usages · symbole ») et d'une croix, posée au caret (dessous si
 * elle tient, dessus sinon), bornée aux murs de la vue ; le fond
 * translucide et la taille viennent du style posé par l'hôte.
 *
 * <p>La carte ne prend jamais le focus ; la rangée referme la carte
 * AVANT de naviguer (le geste ne se joue pas deux fois).</p>
 */
public final class UsageCard {

    /** La hauteur de l'en-tête (dp). */
    private static final int HEADER_DP = 46;

    /** La hauteur du pied de carte (dp). */
    private static final int FOOTER_DP = 30;

    /** La marge aux murs de la vue (dp). */
    private static final int MARGIN_DP = 8;

    /** L'écart au caret (dp). */
    private static final int GAP_DP = 4;

    /** Le plafond de rangées visibles (la carte défile au-delà). */
    private static final int MAX_VISIBLE_ROWS = 8;

    /** La largeur de la colonne d'icône (dp). */
    private static final int ICON_DP = 26;

    /**
     * Un usage du symbole : sa ligne (et son fichier quand il vit
     * ailleurs — la carte le dit à droite), le choix y navigue.
     */
    /**
     * Un usage : sa ligne, sa colonne, son fichier, l'extrait de la
     * ligne (le mot s'y voit dans son contexte — B7 : la rangée
     * montre le code, pas un numéro seul) et le saut.
     *
     * @param line    l'indice de la ligne
     * @param column  la colonne du mot
     * @param file    le fichier de l'usage (vide : le document courant)
     * @param excerpt l'extrait de la ligne, coupé au besoin
     * @param go      le saut à l'usage
     */
    public record Usage(int line, int column, String file, String excerpt,
            Runnable go) {
    }

    /** Les usages vivants (vide : la carte se range). */
    private List<Usage> usages = List.of();

    /** Le symbole dont les usages se listent. */
    private String symbol = "";

    /** La carte vit-elle ? */
    private boolean shown;

    /** La première rangée de la fenêtre (le défilement de la carte). */
    private int topRow;

    /** La rangée armée par le DOWN (-1 : aucune — l'UP la joue). */
    private int armedRow = -1;

    /** La vue porteuse (la navigation s'y poste). */
    private EditorView editor;

    /** Le nombre de rangées visibles à la pose courante. */
    private int visibleRows;

    private float poseX;

    private float poseY;

    private float cardW;

    private float cardH;

    private float density = 1.0f;

    /** Le style posé par l'hôte : fond translucide, encres, tailles, largeur, forme. */
    private PopupStyle style = PopupStyle.defaults();

    private final Paint cardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint detailPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint footerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rulePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glyphPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint scrollbarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    /**
     * Une carte neuve, ses pinceaux propres au style par défaut.
     *
     * @param density la densité de l'écran (le dp en pixels)
     */
    public UsageCard(float density) {
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
     * Branche la vue porteuse : la navigation d'une rangée s'y poste
     * (hors du geste — la carte vit dans la vue).
     *
     * @param view la vue, jamais null
     */
    public void bind(EditorView view) {
        editor = view;
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
        textPaint.setColor(style.textArgb());
        textPaint.setTextSize((style.textSizeSp() - 3.0f) * density);
        detailPaint.setColor(style.hintArgb());
        detailPaint.setTextSize((style.secondarySizeSp() - 2.0f) * density);
        footerPaint.setColor(style.secondaryArgb());
        footerPaint.setTextSize((style.secondarySizeSp() - 2.0f) * density);
        rulePaint.setColor(style.borderArgb());
        glyphPaint.setColor(style.accentArgb());
        glyphPaint.setStyle(Paint.Style.STROKE);
        glyphPaint.setStrokeWidth(1.5f * density);
        scrollbarPaint.setColor((style.borderArgb() & 0x00FFFFFF) | 0x66000000);
    }

    /**
     * Ouvre la carte sur les usages d'un symbole — la liste vide
     * range la carte.
     *
     * @param ofSymbol le symbole listé, jamais null
     * @param found    les usages, jamais null
     */
    public void open(String ofSymbol, List<Usage> found) {
        symbol = ofSymbol == null ? "" : ofSymbol;
        usages = List.copyOf(found);
        topRow = 0;
        shown = !usages.isEmpty();
    }

    /** Ferme la carte (la croix, la navigation, la sélection morte). */
    public void close() {
        usages = List.of();
        symbol = "";
        topRow = 0;
        shown = false;
    }

    /** La carte vit-elle ? */
    public boolean isShown() {
        return shown;
    }

    /** Les usages vivants (le harnais les lit). */
    public List<Usage> usages() {
        return usages;
    }

    /** Le symbole listé (le harnais le lit). */
    public String symbol() {
        return symbol;
    }

    /**
     * Le doigt tombé sur la carte lui appartient : la croix la
     * range, la rangée d'un usage referme la carte AVANT d'y
     * naviguer ; le reste consomme sans agir. Hors de la carte :
     * faux — le geste poursuit sa vie vers l'éditeur.
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
        // la rangée d'un usage s'ARME au DOWN, se JOUE à l'UP (les
        // cartes sœurs ne jouent qu'au relâchement : le DOWN du
        // harnais et le doigt réel n'y exécutent rien deux fois)
        float cursor = poseY + dp(HEADER_DP);
        for (int at = topRow; at < topRow + visibleRows
                && at < usages.size(); at++) {
            if (y >= cursor && y < cursor + rowHeight()) {
                armedRow = at;
                return true;
            }
            cursor += rowHeight();
        }
        return true;
    }

    /**
     * Le relâchement joue la rangée armée (la navigation s'y exécute,
     * la carte se range APRÈS — le geste ne se rejoue pas au dessin).
     */
    public boolean onRelease() {
        if (armedRow < 0) {
            return false;
        }
        Usage chosen = usages.get(armedRow);
        armedRow = -1;
        close();
        if (chosen != null && chosen.go() != null && editor != null) {
            // la navigation se JOUE HORS du geste (postée au tour
            // suivant) : la réentrance du caret pendant le traitement
            // du touch tuait le harnais — le geste se clôt d'abord
            editor.post(chosen.go());
        }
        return true;
    }

    /**
     * Dessine la carte : la pose se recalcule d'abord — sous le
     * caret (au-dessus sans la place) —, puis l'en-tête (le titre,
     * la croix), le filet, les rangées de la fenêtre (l'icône
     * d'usage, le symbole, la ligne à droite), la barre de
     * défilement si la fenêtre cache des rangées, le pied qui compte.
     *
     * @param canvas  le canevas de la vue
     * @param anchorTop le bord haut de la ligne du caret (px, vue)
     * @param anchorBottom le bord bas de la ligne du caret (px, vue)
     * @param metrics la géométrie courante de la vue
     */
    public void draw(Canvas canvas, float anchorTop, float anchorBottom,
            PopupMetrics metrics) {
        if (!shown) {
            cardH = 0.0f;
            return;
        }
        place(anchorTop, anchorBottom, metrics);
        float radius = dp(style.cornerRadiusDp());
        canvas.drawRoundRect(poseX, poseY, poseX + cardW, poseY + cardH,
                radius, radius, cardPaint);
        canvas.drawRoundRect(poseX, poseY, poseX + cardW, poseY + cardH,
                radius, radius, borderPaint);
        float headerBaseline = poseY + dp(HEADER_DP) * 0.66f;
        String title = "Usages · " + symbol;
        canvas.drawText(ellipsize(title, titlePaint, cardW - dp(76)),
                poseX + dp(14), headerBaseline, titlePaint);
        drawCross(canvas, poseX + cardW - dp(24), poseY + dp(HEADER_DP) / 2.0f);
        canvas.drawLine(poseX + dp(12), poseY + dp(HEADER_DP) - dp(2),
                poseX + cardW - dp(12), poseY + dp(HEADER_DP) - dp(2),
                rulePaint);
        float cursor = poseY + dp(HEADER_DP);
        for (int at = topRow; at < topRow + visibleRows
                && at < usages.size(); at++) {
            drawRow(canvas, usages.get(at), cursor);
            cursor += rowHeight();
        }
        drawScrollbar(canvas, cursor);
        drawFooter(canvas, cursor);
    }

    /** Une rangée : l'icône d'usage (le chevron vers le symbole), le nom, la ligne. */
    private void drawRow(Canvas canvas, Usage usage, float top) {
        float rowH = rowHeight();
        float cy = top + rowH / 2.0f;
        float baseline = top + rowH * 0.66f;
        // l'icône d'usage : le petit chevron droit (la référence y
        // met sa flèche de référence — discret, à l'accent)
        float side = dp(ICON_DP) * 0.5f;
        canvas.drawLine(poseX + dp(14) + side * 0.4f, cy - side * 0.4f,
                poseX + dp(14) + side, cy, glyphPaint);
        canvas.drawLine(poseX + dp(14) + side * 0.4f, cy + side * 0.4f,
                poseX + dp(14) + side, cy, glyphPaint);
        canvas.drawText(symbol, poseX + dp(14) + dp(ICON_DP), baseline,
                textPaint);
        String where = (usage.file() == null || usage.file().isEmpty()
                ? "" : usage.file() + " · ") + "ligne " + (usage.line() + 1);
        float whereW = detailPaint.measureText(where);
        canvas.drawText(where, poseX + cardW - dp(14) - whereW, baseline,
                detailPaint);
        // L'EXTRAIT DE LA LIGNE (B7) : le code s'y lit sous la
        // localisation, le mot en encre pleine (le reste de la ligne
        // vit dans la même encre — le mot SE DÉTACHE par sa police
        // pleine, l'entourage atténué)
        String excerpt = usage.excerpt() == null ? "" : usage.excerpt().trim();
        if (!excerpt.isEmpty()) {
            float excerptBaseline = top + rowH * 0.92f;
            String squeezed = excerpt.length() > 60
                    ? excerpt.substring(0, 57) + "…" : excerpt;
            canvas.drawText(squeezed, poseX + dp(14) + dp(ICON_DP),
                    excerptBaseline, detailPaint);
        }
    }

    /** La croix de fermeture (le coin haut droit). */
    private void drawCross(Canvas canvas, float cx, float cy) {
        float s = dp(5);
        canvas.drawLine(cx - s, cy - s, cx + s, cy + s, glyphPaint);
        canvas.drawLine(cx - s, cy + s, cx + s, cy - s, glyphPaint);
    }

    /** La barre de défilement : la fenêtre montre-t-elle moins que la liste ? */
    private void drawScrollbar(Canvas canvas, float rowsBottom) {
        if (visibleRows >= usages.size()) {
            return;
        }
        float trackTop = poseY + dp(HEADER_DP);
        float trackH = rowsBottom - trackTop;
        float thumbH = Math.max(dp(24),
                trackH * visibleRows / Math.max(1, usages.size()));
        float thumbY = trackTop + (trackH - thumbH) * topRow
                / Math.max(1, usages.size() - visibleRows);
        canvas.drawRect(poseX + cardW - dp(4), thumbY,
                poseX + cardW - dp(2), thumbY + thumbH, scrollbarPaint);
    }

    /** Le pied de carte : le compte des usages. */
    private void drawFooter(Canvas canvas, float rowsBottom) {
        String count = usages.size() + " usage(s)";
        float countW = footerPaint.measureText(count);
        canvas.drawText(count, poseX + cardW - dp(14) - countW,
                poseY + cardH - dp(FOOTER_DP) / 2.0f, footerPaint);
    }

    /**
     * Pose la carte sous la ligne du caret (au-dessus sans la place,
     * jamais sur elle), le bord gauche du texte borné aux murs.
     */
    private void place(float anchorTop, float anchorBottom,
            PopupMetrics metrics) {
        float margin = dp(MARGIN_DP);
        float gap = dp(GAP_DP);
        cardW = Math.min(Math.max(measuredWidth(),
                dp(style.minCardWidthDp() - 80)),
                Math.min(dp(style.maxCardWidthDp() - 60),
                        metrics.viewWidth() - 2 * margin));
        int rows = Math.min(MAX_VISIBLE_ROWS, usages.size());
        float available = metrics.viewHeight() - 2 * margin - dp(HEADER_DP)
                - dp(FOOTER_DP);
        while (rows > 1 && dp(HEADER_DP) + dp(FOOTER_DP)
                + rows * rowHeight() > available) {
            rows--;
        }
        visibleRows = Math.max(1, rows);
        cardH = dp(HEADER_DP) + visibleRows * rowHeight() + dp(FOOTER_DP);
        float belowTop = anchorBottom + gap;
        if (belowTop + cardH <= metrics.viewHeight() - margin) {
            poseY = belowTop;
        } else {
            poseY = Math.max(margin, anchorTop - gap - cardH);
        }
        poseX = Math.max(margin,
                Math.min(metrics.paddingLeft(),
                        metrics.viewWidth() - cardW - margin));
    }

    /** La largeur qu'il faut pour la plus large rangée, respirée. */
    private float measuredWidth() {
        float widest = titlePaint.measureText("Usages · " + symbol);
        for (Usage usage : usages) {
            String where = (usage.file() == null || usage.file().isEmpty()
                    ? "" : usage.file() + " · ") + "ligne " + (usage.line() + 1);
            widest = Math.max(widest,
                    textPaint.measureText(symbol) + detailPaint.measureText(where));
        }
        return widest + dp(ICON_DP) + 3 * dp(14);
    }

    /** La hauteur d'une rangée : le style la porte, l'icône y respire. */
    private float rowHeight() {
        return dp(style.rowHeightDp() - 6);
    }

    /** Le texte coupé à la largeur donnée, l'ellipse à la fin. */
    private String ellipsize(String text, Paint paint, float width) {
        if (paint.measureText(text) <= width) {
            return text;
        }
        StringBuilder out = new StringBuilder(text);
        while (out.length() > 1 && paint.measureText(out.toString()) > width) {
            out.deleteCharAt(out.length() - 1);
        }
        out.append('…');
        return out.toString();
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

    /** L'ordonnée du centre d'une rangée (le harnais y vise le doigt). */
    public float rowCenterY(int index) {
        if (index < topRow || index >= topRow + visibleRows) {
            return -1.0f;
        }
        return poseY + dp(HEADER_DP) + (index - topRow) * rowHeight()
                + rowHeight() / 2.0f;
    }

    private float dp(int value) {
        return value * density;
    }
}
