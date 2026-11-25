package jo.pigma.editor.popups;

import android.graphics.Canvas;
import android.graphics.Paint;

import java.util.List;

/**
 * La carte de débordement de la barre de sélection, DESSINÉE DANS LA
 * VUE : les actions repliées y vivent en rangées verticales — la puce
 * « ⋯ » de la barre l'ouvre. Elle se pose SOUS la sélection (jamais
 * sur elle — le caret demeure à découvert), au-dessus de la barre
 * sans la place, bornée aux murs. Le choix d'une rangée la REFERME
 * AVANT d'exécuter — le débordement ne survit jamais au choix ; la
 * carte ne survit jamais à sa barre.
 *
 * <p>La carte ne prend jamais le focus ; le doigt sur une rangée
 * l'exécute, le reste de la carte consomme sans agir, hors d'elle le
 * geste poursuit sa vie vers l'éditeur.</p>
 */
public final class OverflowCard {

    /** La hauteur d'une rangée (dp). */
    private static final int ROW_HEIGHT_DP = 30;

    /** La marge aux murs de la vue (dp). */
    private static final int MARGIN_DP = 8;

    /** L'écart à la sélection (dp). */
    private static final int GAP_DP = 4;

    /** La largeur plancher de la carte (dp). */
    private static final int MIN_WIDTH_DP = 130;

    /** La largeur plafond de la carte (dp). */
    private static final int MAX_WIDTH_DP = 260;

    /** Une action repliée : son libellé et ce qu'elle fait. */
    public record Row(String label, Runnable action) {
    }

    /** Les rangées vivantes (vides : la carte se range). */
    private List<Row> rows = List.of();

    private float poseX;

    private float poseY;

    private float cardW;

    private float cardH;

    private float density = 1.0f;

    private final Paint cardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint selectionPaint = new Paint();

    /**
     * Une carte neuve, ses pinceaux propres.
     *
     * @param density la densité de l'écran (le dp en pixels)
     */
    public OverflowCard(float density) {
        cardPaint.setColor(0xFFF6F8FA);
        borderPaint.setColor(0xFFB5B5B5);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(1.0f);
        textPaint.setColor(0xFF202124);
        textPaint.setTextSize(13.0f * density);
        selectionPaint.setColor(0x1F1A73E8);
    }

    /**
     * Ouvre la carte sur les actions repliées — la barre porteuse vit
     * déjà ; une liste vide ne pose rien.
     *
     * @param actions les actions repliées, jamais null
     */
    public void open(List<Row> actions) {
        rows = List.copyOf(actions);
    }

    /** Ferme la carte (le choix, la barre qui se range, la sélection morte). */
    public void close() {
        rows = List.of();
    }

    /** La carte vit-elle ? */
    public boolean isShown() {
        return !rows.isEmpty();
    }

    /** Les libellés des rangées vivantes (le harnais les lit). */
    public List<String> labels() {
        java.util.ArrayList<String> labels = new java.util.ArrayList<>();
        for (Row row : rows) {
            labels.add(row.label());
        }
        return labels;
    }

    /**
     * Le doigt tombé sur la carte lui appartient : la rangée visée
     * se REFERME avant d'exécuter son action — le débordement ne
     * survit jamais au choix ; le reste de la carte consomme sans
     * agir. Hors de la carte : faux.
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
        int row = (int) ((y - poseY) / dp(ROW_HEIGHT_DP));
        if (row >= 0 && row < rows.size()) {
            Row chosen = rows.get(row);
            close();
            chosen.action().run();
        }
        return true;
    }

    /**
     * Dessine la carte : la pose se recalcule d'abord — sous la
     * sélection (jamais sur elle), au-dessus de la barre sans la
     * place —, puis les rangées de libellés.
     *
     * @param canvas    le canevas de la vue
     * @param anchorTop le bord haut de la barre porteuse (px, vue)
     * @param anchorBottom le bord bas de la sélection (px, vue)
     * @param centerX   le centre horizontal de la sélection (px)
     * @param metrics   la géométrie courante de la vue
     */
    public void draw(Canvas canvas, float anchorTop, float anchorBottom,
            float centerX, PopupMetrics metrics) {
        density = metrics.density();
        if (rows.isEmpty()) {
            cardH = 0.0f;
            return;
        }
        place(anchorTop, anchorBottom, centerX, metrics);
        float radius = dp(10);
        canvas.drawRoundRect(poseX, poseY, poseX + cardW, poseY + cardH,
                radius, radius, cardPaint);
        canvas.drawRoundRect(poseX, poseY, poseX + cardW, poseY + cardH,
                radius, radius, borderPaint);
        for (int at = 0; at < rows.size(); at++) {
            float top = poseY + at * dp(ROW_HEIGHT_DP);
            canvas.drawText(rows.get(at).label(), poseX + dp(14),
                    top + dp(ROW_HEIGHT_DP) * 0.68f, textPaint);
        }
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
     * @param row l'index de la rangée
     * @return l'ordonnée du centre, -1 hors carte
     */
    public float rowCenterY(int row) {
        if (!isShown() || row < 0 || row >= rows.size()) {
            return -1.0f;
        }
        return poseY + (row + 0.5f) * dp(ROW_HEIGHT_DP);
    }

    /**
     * Pose la carte sous la sélection (jamais sur elle — le caret
     * demeure à découvert), au-dessus de la barre sans la place, le
     * centre borné aux murs.
     */
    private void place(float anchorTop, float anchorBottom,
            float centerX, PopupMetrics metrics) {
        float margin = dp(MARGIN_DP);
        float gap = dp(GAP_DP);
        cardW = Math.min(Math.max(measuredWidth(), dp(MIN_WIDTH_DP)),
                dp(MAX_WIDTH_DP));
        cardH = rows.size() * dp(ROW_HEIGHT_DP);
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

    /** La largeur qu'il faut pour le plus large libellé, respiré. */
    private float measuredWidth() {
        float widest = 0.0f;
        for (Row row : rows) {
            widest = Math.max(widest, textPaint.measureText(row.label()));
        }
        return widest + 2.0f * dp(14);
    }

    private float dp(int value) {
        return value * density;
    }
}
