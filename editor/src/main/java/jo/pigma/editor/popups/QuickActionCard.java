package jo.pigma.editor.popups;

import android.graphics.Canvas;
import android.graphics.Paint;

import java.util.ArrayList;
import java.util.List;

/**
 * La carte d'actions rapides — L'AMPOULE JAUNE de la gouttière :
 * le doigt sur l'ampoule y ouvre les actions de la ligne (les
 * intentions de l'atelier) — chaque rangée porte l'éclair de
 * l'action à gauche et son libellé à droite, coiffées du titre
 * « Actions rapides · ligne N ». Les actions naissent du CONTEXTE :
 * l'éditeur y met les siennes (commenter, casse, dupliquer,
 * supprimer la ligne), l'hôte y pose les siennes par son
 * fournisseur. La carte se pose SOUS la ligne de l'ampoule
 * (au-dessus sans la place), bornée aux murs ; le fond translucide
 * et la taille viennent du style posé par l'hôte.
 *
 * <p>La carte ne prend jamais le focus ; le choix d'une rangée la
 * REFERME avant d'exécuter — l'action ne survit jamais au choix ;
 * le reste de la carte consomme sans agir, hors d'elle le geste
 * poursuit sa vie vers l'éditeur.</p>
 */
public final class QuickActionCard {

    /** Le plafond de rangées visibles (la fenêtre défile au-delà). */
    private static final int MAX_VISIBLE_ROWS = 10;

    /** La respiration intérieure de la carte (dp). */
    private static final int PAD_DP = 6;

    /** La hauteur du pied de carte (dp). */
    private static final int FOOTER_DP = 30;

    /** La marge aux murs de la vue (dp). */
    private static final int MARGIN_DP = 8;

    /** L'écart entre la ligne et la carte (dp). */
    private static final int GAP_DP = 4;

    /** La largeur d'une icône d'action (dp). */
    private static final int ICON_DP = 22;

    /**
     * Une action rapide : son libellé et ce qu'elle fait.
     *
     * @param label  le libellé, jamais vide
     * @param action ce qu'elle fait, jamais null
     */
    public record Entry(String label, Runnable action) {
    }

    /** Les actions vivantes de la ligne ancrée (vide : la carte se range). */
    private List<Entry> entries = List.of();

    /** La ligne ancrée (les actions y naissent). */
    private int line;

    /** La première rangée de la fenêtre (le défilement de la carte). */
    private int topRow;

    /** Le nombre de rangées visibles à la pose courante. */
    private int visibleRows;

    private float poseX;

    private float poseY;

    private float cardW;

    private float cardH;

    private float density = 1.0f;

    private PopupStyle style = PopupStyle.defaults();

    private final Paint cardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint headerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint footerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rulePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bulbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glyphPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint scrollbarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    /**
     * Une carte neuve, ses pinceaux au style par défaut.
     *
     * @param density la densité de l'écran (le dp en pixels)
     */
    public QuickActionCard(float density) {
        this.density = density;
        applyStyle(style);
    }

    /**
     * Pose le style de l'hôte : le fond translucide, les encres, la
     * taille des textes, la forme — la carte s'y reconfigure entière.
     *
     * @param fresh le style posé, null pour celui par défaut
     */
    public void setStyle(PopupStyle fresh) {
        style = fresh == null ? PopupStyle.defaults() : fresh;
        applyStyle(style);
    }

    /** Le style courant (le harnais le lit). */
    public PopupStyle style() {
        return style;
    }

    /** Reconfigure les pinceaux au style courant. */
    private void applyStyle(PopupStyle style) {
        cardPaint.setColor(style.backgroundArgb());
        borderPaint.setColor(style.borderArgb());
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(1.0f);
        headerPaint.setColor(style.accentArgb());
        headerPaint.setTextSize(style.secondarySizeSp() * density);
        headerPaint.setFakeBoldText(true);
        textPaint.setColor(style.textArgb());
        textPaint.setTextSize((style.textSizeSp() - 2.0f) * density);
        footerPaint.setColor(style.secondaryArgb());
        footerPaint.setTextSize((style.secondarySizeSp() - 2.0f) * density);
        rulePaint.setColor(style.borderArgb());
        bulbPaint.setColor(style.lightbulbArgb());
        bulbPaint.setStyle(Paint.Style.FILL);
        glyphPaint.setColor(style.secondaryArgb());
        glyphPaint.setStyle(Paint.Style.STROKE);
        glyphPaint.setStrokeWidth(1.6f * density);
        scrollbarPaint.setColor(0x66B5B5B5);
    }

    /**
     * Ouvre la carte sur les actions d'une ligne — la liste vide
     * range la carte.
     *
     * @param shownLine la ligne ancrée
     * @param actions   les actions de la ligne, jamais null
     */
    public void open(int shownLine, List<Entry> actions) {
        List<Entry> alive = new ArrayList<>();
        for (Entry action : actions) {
            if (action != null && action.label() != null
                    && !action.label().isEmpty() && action.action() != null) {
                alive.add(action);
            }
        }
        entries = List.copyOf(alive);
        line = shownLine;
        topRow = 0;
    }

    /** Range la carte (le choix, un nouvel appui, la ligne morte). */
    public void close() {
        entries = List.of();
        line = -1;
        topRow = 0;
        visibleRows = 0;
    }

    /** La carte vit-elle ? */
    public boolean isShown() {
        return !entries.isEmpty();
    }

    /** Les actions vivantes (le harnais les lit). */
    public List<Entry> entries() {
        return entries;
    }

    /** La ligne ancrée, -1 quand la carte se range. */
    public int line() {
        return isShown() ? line : -1;
    }

    /**
     * Le doigt tombé sur la carte lui appartient : la rangée visée
     * se REFERME avant d'exécuter son action — l'ampoule ne survit
     * jamais au choix ; le titre et le pied consomment sans agir.
     * Hors de la carte : faux.
     *
     * @param x l'abscisse du doigt (px, coordonnées de la vue)
     * @param y l'ordonnée du doigt (px, coordonnées de la vue)
     * @return vrai quand la carte a pris le geste
     */
    public boolean onTouchArea(float x, float y) {
        if (!isShown() || x < poseX || x >= poseX + cardW
                || y < poseY || y >= poseY + cardH) {
            return false;
        }
        int row = rowAt(y);
        if (row >= 0) {
            Entry chosen = entries.get(row);
            close();
            chosen.action().run();
        }
        return true;
    }

    /**
     * Dessine la carte : la pose se recalcule d'abord — sous la
     * ligne ancrée (au-dessus sans la place) —, puis le titre
     * coiffant les rangées (l'éclair de l'action, le libellé), la
     * barre de défilement si la fenêtre cache des rangées, le pied
     * qui compte.
     *
     * @param canvas      le canevas de la vue
     * @param anchorTop   le bord haut de la ligne ancrée (px, vue)
     * @param anchorBottom le bord bas de la ligne ancrée (px, vue)
     * @param metrics     la géométrie courante de la vue
     */
    public void draw(Canvas canvas, float anchorTop, float anchorBottom,
            PopupMetrics metrics) {
        if (entries.isEmpty()) {
            cardH = 0.0f;
            return;
        }
        place(anchorTop, anchorBottom, metrics);
        float radius = dp(style.cornerRadiusDp());
        canvas.drawRoundRect(poseX, poseY, poseX + cardW, poseY + cardH,
                radius, radius, cardPaint);
        canvas.drawRoundRect(poseX, poseY, poseX + cardW, poseY + cardH,
                radius, radius, borderPaint);
        float rowH = rowHeight();
        float headerH = dp(style.headerHeightDp());
        float rowsTop = poseY + dp(PAD_DP);
        canvas.drawText("Actions rapides · ligne " + (line + 1),
                poseX + dp(14), rowsTop + headerH * 0.72f, headerPaint);
        canvas.drawLine(poseX + dp(12), rowsTop + headerH - dp(2),
                poseX + cardW - dp(12), rowsTop + headerH - dp(2), rulePaint);
        float rowsBottom = rowsTop + headerH + visibleRows * rowH;
        for (int at = topRow; at < topRow + visibleRows
                && at < entries.size(); at++) {
            float top = rowsTop + headerH + (at - topRow) * rowH;
            drawEntry(canvas, entries.get(at), top);
        }
        drawScrollbar(canvas, rowsTop + headerH, rowsBottom);
        drawFooter(canvas, rowsBottom);
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

    /**
     * L'ordonnée du centre d'une rangée (le harnais y vise le doigt).
     *
     * @param index l'index de l'action
     * @return l'ordonnée du centre, -1 hors carte ou hors fenêtre
     */
    public float rowCenterY(int index) {
        if (!isShown() || index < topRow || index >= topRow + visibleRows) {
            return -1.0f;
        }
        return poseY + dp(PAD_DP) + dp(style.headerHeightDp())
                + (index - topRow + 0.5f) * rowHeight();
    }

    /**
     * Déplace la fenêtre des rangées vers une rangée (la rangée
     * demandée y devient visible — le doigt peut la viser). La
     * fenêtre au-delà du plafond de rangées s'y sert : la carte
     * défile, la rangée demandée y entre.
     *
     * @param index l'index de la rangée à montrer
     */
    public void showRow(int index) {
        if (index < 0 || index >= entries.size()) {
            return;
        }
        if (index < topRow) {
            topRow = index;
        } else if (index >= topRow + visibleRows) {
            topRow = Math.min(index - visibleRows + 1,
                    Math.max(0, entries.size() - visibleRows));
        }
    }

    /**
     * Pose la carte sous la ligne ancrée (au-dessus sans la place,
     * jamais sur elle), le bord gauche borné aux murs ; la fenêtre
     * se resserre à ce que la vue tient, au plus
     * {@value #MAX_VISIBLE_ROWS} rangées.
     */
    private void place(float anchorTop, float anchorBottom, PopupMetrics metrics) {
        float margin = dp(MARGIN_DP);
        float gap = dp(GAP_DP);
        cardW = Math.min(Math.max(measuredWidth(),
                dp(style.minCardWidthDp() - 80)),
                Math.min(dp(style.maxCardWidthDp() - 80),
                        metrics.viewWidth() - 2 * margin));
        int rows = Math.min(MAX_VISIBLE_ROWS, entries.size());
        float available = metrics.viewHeight() - 2 * margin - 2 * dp(PAD_DP)
                - dp(style.headerHeightDp()) - dp(FOOTER_DP);
        while (rows > 1 && 2 * dp(PAD_DP) + dp(style.headerHeightDp())
                + dp(FOOTER_DP) + rows * rowHeight() > available) {
            rows--;
        }
        visibleRows = rows;
        cardH = 2 * dp(PAD_DP) + dp(style.headerHeightDp())
                + visibleRows * rowHeight() + dp(FOOTER_DP);
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

    /** La hauteur d'une rangée : le style la porte, l'icône y respire. */
    private float rowHeight() {
        return dp(style.rowHeightDp() - 8);
    }

    /** La largeur qu'il faut pour le plus large libellé, respiré. */
    private float measuredWidth() {
        float widest = 0.0f;
        for (Entry entry : entries) {
            widest = Math.max(widest, textPaint.measureText(entry.label()));
        }
        return widest + dp(ICON_DP) + dp(16) + 2 * dp(14);
    }

    /** Une rangée : l'éclair d'action, le libellé. */
    private void drawEntry(Canvas canvas, Entry entry, float top) {
        float rowH = rowHeight();
        float cy = top + rowH / 2.0f;
        float baseline = top + rowH * 0.68f;
        drawBoltIcon(canvas, poseX + dp(14) + dp(ICON_DP) / 2.0f, cy);
        canvas.drawText(entry.label(),
                poseX + dp(14) + dp(ICON_DP) + dp(8), baseline, textPaint);
    }

    /**
     * L'éclair d'action : la tête de l'ampoule — un petit éclair
     * teinté de la couleur de l'ampoule, l'encre secondaire au
     * filet.
     */
    private void drawBoltIcon(Canvas canvas, float cx, float cy) {
        float s = dp(8);
        android.graphics.Path bolt = new android.graphics.Path();
        bolt.moveTo(cx - s * 0.35f, cy - s);
        bolt.lineTo(cx + s * 0.45f, cy - s);
        bolt.lineTo(cx + s * 0.1f, cy - s * 0.2f);
        bolt.lineTo(cx + s * 0.55f, cy - s * 0.2f);
        bolt.lineTo(cx - s * 0.45f, cy + s);
        bolt.lineTo(cx - s * 0.05f, cy + s * 0.15f);
        bolt.lineTo(cx - s * 0.55f, cy + s * 0.15f);
        bolt.close();
        bulbPaint.setStyle(Paint.Style.FILL);
        canvas.drawPath(bolt, bulbPaint);
    }

    /** Le haut d'une rangée au doigt (l'index, -1 hors fenêtre). */
    private int rowAt(float y) {
        float rowsTop = poseY + dp(PAD_DP) + dp(style.headerHeightDp());
        for (int at = topRow; at < topRow + visibleRows
                && at < entries.size(); at++) {
            float top = rowsTop + (at - topRow) * rowHeight();
            if (y >= top && y < top + rowHeight()) {
                return at;
            }
        }
        return -1;
    }

    /**
     * La barre de défilement : la fenêtre montre-t-elle moins que la
     * liste ? Le pouce se pose au prorata des rangées visibles.
     */
    private void drawScrollbar(Canvas canvas, float trackTop, float trackBottom) {
        if (visibleRows >= entries.size()) {
            return;
        }
        float thumb = (trackBottom - trackTop) * visibleRows / entries.size();
        float trackSpan = trackBottom - trackTop - thumb;
        float thumbTop = entries.size() <= 1 ? trackTop
                : trackTop + trackSpan * topRow / (entries.size() - visibleRows);
        canvas.drawRect(poseX + cardW - dp(5), thumbTop,
                poseX + cardW - dp(3), thumbTop + thumb, scrollbarPaint);
    }

    /** Le pied de carte : le compte des actions de la ligne. */
    private void drawFooter(Canvas canvas, float footerTop) {
        float baseline = footerTop + dp(FOOTER_DP) * 0.66f;
        String count = entries.size() == 1 ? "1 action"
                : entries.size() + " actions";
        canvas.drawText(count, poseX + dp(14), baseline, footerPaint);
        String hint = "toucher pour agir";
        canvas.drawText(hint,
                poseX + cardW - dp(14) - footerPaint.measureText(hint),
                baseline, footerPaint);
    }

    private float dp(int value) {
        return value * density;
    }

    private float dp(float value) {
        return value * density;
    }
}
