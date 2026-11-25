package jo.pigma.editor.popups;

import android.graphics.Canvas;
import android.graphics.Paint;

import java.util.ArrayList;
import java.util.List;

/**
 * La carte de débordement de la barre de sélection, DESSINÉE DANS LA
 * VUE : les actions repliées y vivent en ÉTAGES — une section par
 * famille (navigation, édition, presse-papiers, historique, hôte),
 * chaque étage coiffé de son titre à l'encre d'accent, les rangées
 * dessous. Les étages naissent du CONTEXTE : l'hôte qui pose ses
 * actions y gagne le sien, l'historique ne vit que d'une sélection,
 * le langage sans commentaire de ligne n'y offre pas le sien. La
 * carte se pose SOUS la sélection (jamais sur elle — le caret demeure
 * à découvert), au-dessus de la barre sans la place, bornée aux murs.
 * Le choix d'une rangée la REFERME AVANT d'exécuter — le
 * débordement ne survit jamais au choix ; la carte ne survit jamais à
 * sa barre.
 *
 * <p>La carte ne prend jamais le focus ; le doigt sur une rangée
 * l'exécute, le titre d'un étage consomme sans agir, le reste de la
 * carte consomme sans agir, hors d'elle le geste poursuit sa vie vers
 * l'éditeur.</p>
 */
public final class OverflowCard {

    /** La hauteur d'une rangée (dp). */
    private static final int ROW_HEIGHT_DP = 36;

    /** La hauteur d'un titre d'étage (dp). */
    private static final int HEADER_DP = 26;

    /** La marge aux murs de la vue (dp). */
    private static final int MARGIN_DP = 8;

    /** L'écart à la sélection (dp). */
    private static final int GAP_DP = 4;

    /** La respiration haute et basse de la carte (dp). */
    private static final int PAD_Y_DP = 4;

    /** La largeur plancher de la carte (dp). */
    private static final int MIN_WIDTH_DP = 200;

    /** La largeur plafond de la carte (dp). */
    private static final int MAX_WIDTH_DP = 320;

    /** Le plafond de rangées visibles (la fenêtre défile au-delà). */
    private static final int MAX_VISIBLE_ROWS = 12;

    /** Une action repliée : son libellé et ce qu'elle fait. */
    public record Row(String label, Runnable action) {
    }

    /**
     * Un étage : son titre (l'encre d'accent) et ses rangées — l'étage
     * vide ne se montre pas.
     *
     * @param title le titre de la famille, jamais null (vide : les
     *               rangées sans titre, la compatibilité d'une colonne)
     * @param rows  les actions de la famille, jamais null
     */
    public record Section(String title, List<Row> rows) {
    }

    /** Les étages vivants (vides : la carte se range). */
    private List<Section> sections = List.of();

    /** La première rangée de la fenêtre (le défilement de la carte). */
    private int topRow;

    /** Le nombre de rangées visibles à la pose courante. */
    private int visibleRows;

    /** Le haut de chaque rangée visible (px, coordonnées de la vue). */
    private float[] rowTops = new float[0];

    private float poseX;

    private float poseY;

    private float cardW;

    private float cardH;

    private float density = 1.0f;

    private final Paint cardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint headerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rulePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

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
        textPaint.setTextSize(14.0f * density);
        headerPaint.setColor(0xFF1A73E8);
        headerPaint.setTextSize(11.0f * density);
        headerPaint.setFakeBoldText(true);
        rulePaint.setColor(0xFFDDDDDD);
    }

    /**
     * Ouvre la carte sur les étages des actions repliées — la barre
     * porteuse vit déjà ; les étages vides ne se posent pas, la liste
     * vide range la carte.
     *
     * @param families les étages, jamais null
     */
    public void openSections(List<Section> families) {
        List<Section> alive = new ArrayList<>();
        for (Section family : families) {
            if (family != null && !family.rows().isEmpty()) {
                alive.add(family);
            }
        }
        sections = List.copyOf(alive);
        topRow = 0;
    }

    /**
     * Ouvre la carte sur les actions repliées d'un seul étage — la
     * compatibilité d'une colonne (le titre s'omet : les rangées
     * seules se montrent).
     *
     * @param actions les actions repliées, jamais null
     */
    public void open(List<Row> actions) {
        openSections(List.of(new Section("", actions)));
    }

    /** Ferme la carte (le choix, la barre qui se range, la sélection morte). */
    public void close() {
        sections = List.of();
        topRow = 0;
        visibleRows = 0;
        rowTops = new float[0];
    }

    /** La carte vit-elle ? */
    public boolean isShown() {
        return !sections.isEmpty();
    }

    /**
     * Les libellés des rangées vivantes, étages compris dans l'ordre
     * (le harnais les lit) — les titres d'étages ne s'y mêlent pas.
     */
    public List<String> labels() {
        List<String> labels = new ArrayList<>();
        for (Section family : sections) {
            for (Row row : family.rows()) {
                labels.add(row.label());
            }
        }
        return labels;
    }

    /** Les titres des étages vivants (le harnais les lit). */
    public List<String> sectionTitles() {
        List<String> titles = new ArrayList<>();
        for (Section family : sections) {
            if (!family.title().isEmpty()) {
                titles.add(family.title());
            }
        }
        return titles;
    }

    /**
     * Le doigt tombé sur la carte lui appartient : la rangée visée
     * se REFERME avant d'exécuter son action — le débordement ne
     * survit jamais au choix ; le titre d'un étage consomme sans
     * agir, le reste de la carte consomme sans agir. Hors de la
     * carte : faux.
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
        int flat = rowAt(y);
        if (flat >= 0) {
            Row chosen = flatRow(flat);
            if (chosen != null) {
                close();
                chosen.action().run();
            }
        }
        return true;
    }

    /**
     * Dessine la carte : la pose se recalcule d'abord — sous la
     * sélection (jamais sur elle), au-dessus de la barre sans la
     * place —, puis les étages : leur titre coiffe les rangées, un
     * filet sépare la famille suivante.
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
        if (sections.isEmpty()) {
            cardH = 0.0f;
            return;
        }
        place(anchorTop, anchorBottom, centerX, metrics);
        float radius = dp(10);
        canvas.drawRoundRect(poseX, poseY, poseX + cardW, poseY + cardH,
                radius, radius, cardPaint);
        canvas.drawRoundRect(poseX, poseY, poseX + cardW, poseY + cardH,
                radius, radius, borderPaint);
        for (int flat = topRow; flat < topRow + visibleRows
                && flat < flatCount(); flat++) {
            if (leadsFamily(flat) && !familyOf(flat).title().isEmpty()) {
                drawHeader(canvas, familyOf(flat), rowTops[flat - topRow]
                        - dp(HEADER_DP));
            }
            drawRowLabel(canvas, flatRow(flat), rowTops[flat - topRow]);
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
     * L'ordonnée du centre d'une rangée (le harnais y vise le doigt)
     * — l'index PLAT compte les rangées des étages dans l'ordre, les
     * titres ne comptent pas.
     *
     * @param row l'index plat de la rangée
     * @return l'ordonnée du centre, -1 hors carte ou hors fenêtre
     */
    public float rowCenterY(int row) {
        if (!isShown() || row < topRow || row >= topRow + visibleRows
                || row >= flatCount()) {
            return -1.0f;
        }
        return rowTops[row - topRow] + dp(ROW_HEIGHT_DP) / 2.0f;
    }

    /**
     * Pose la carte sous la sélection (jamais sur elle — le caret
     * demeure à découvert), au-dessus de la barre sans la place, le
     * centre borné aux murs ; la fenêtre se resserre au plus
     * {@value #MAX_VISIBLE_ROWS} rangées, au plus ce que la vue
     * tient — la géométrie des rangées s'y pose (le haut de chacune,
     * les titres au fil).
     */
    private void place(float anchorTop, float anchorBottom,
            float centerX, PopupMetrics metrics) {
        float margin = dp(MARGIN_DP);
        float gap = dp(GAP_DP);
        cardW = Math.min(Math.max(measuredWidth(), dp(MIN_WIDTH_DP)),
                dp(MAX_WIDTH_DP));
        int total = flatCount();
        int rows = Math.min(MAX_VISIBLE_ROWS, total);
        float available = metrics.viewHeight() - 2 * margin
                - dp(GAP_DP) - 2 * dp(PAD_Y_DP);
        topRow = Math.min(topRow, Math.max(0, total - rows));
        while (rows > 1 && heightOfWindow(topRow, rows) > available) {
            rows--;
            topRow = Math.min(topRow, Math.max(0, total - rows));
        }
        visibleRows = rows;
        cardH = heightOfWindow(topRow, visibleRows);
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
        layoutRows();
    }

    /**
     * La géométrie des rangées visibles : le haut de chacune, les
     * titres d'étages au fil — la fenêtre part du haut, une rangée
     * cachée ne consomme rien.
     */
    private void layoutRows() {
        rowTops = new float[Math.max(1, visibleRows)];
        float cursor = poseY + dp(PAD_Y_DP);
        int flat = 0;
        for (Section family : sections) {
            for (int inFamily = 0; inFamily < family.rows().size(); inFamily++) {
                boolean visible = flat >= topRow && flat < topRow + visibleRows;
                if (visible) {
                    // l'étage ouvre ici (dans la fenêtre) : son titre
                    // se paie avant sa première rangée visible
                    if (leadsFamily(flat) && !family.title().isEmpty()) {
                        cursor += dp(HEADER_DP);
                    }
                    rowTops[flat - topRow] = cursor;
                    cursor += dp(ROW_HEIGHT_DP);
                }
                flat++;
            }
        }
    }

    /**
     * La hauteur de la fenêtre : respirations, titres, rangées — la
     * fenêtre part de {@code fromRow}, les rangées d'avant ne
     * consomnent rien.
     */
    private float heightOfWindow(int fromRow, int rows) {
        float height = 2 * dp(PAD_Y_DP);
        int flat = 0;
        int visible = 0;
        for (Section family : sections) {
            for (int inFamily = 0; inFamily < family.rows().size(); inFamily++) {
                boolean inWindow = flat >= fromRow && visible < rows;
                if (inWindow) {
                    // l'étage ouvre dans la fenêtre : son titre se
                    // paie avant sa première rangée visible
                    if (leadsFamily(flat) && !family.title().isEmpty()) {
                        height += dp(HEADER_DP);
                    }
                    height += dp(ROW_HEIGHT_DP);
                    visible++;
                }
                flat++;
            }
        }
        return height;
    }

    /** Le nombre de rangées posées, étages confondus. */
    private int flatCount() {
        int count = 0;
        for (Section family : sections) {
            count += family.rows().size();
        }
        return count;
    }

    /** La rangée d'index plat (null hors bornes). */
    private Row flatRow(int flat) {
        int at = 0;
        for (Section family : sections) {
            for (Row row : family.rows()) {
                if (at == flat) {
                    return row;
                }
                at++;
            }
        }
        return null;
    }

    /** L'étage de la rangée d'index plat (null hors bornes). */
    private Section familyOf(int flat) {
        int at = 0;
        for (Section family : sections) {
            if (at <= flat && flat < at + family.rows().size()) {
                return family;
            }
            at += family.rows().size();
        }
        return null;
    }

    /** La rangée d'index plat ouvre-t-elle son étage ? */
    private boolean leadsFamily(int flat) {
        int at = 0;
        for (Section family : sections) {
            if (at == flat) {
                return true;
            }
            at += family.rows().size();
            if (at > flat) {
                return false;
            }
        }
        return false;
    }

    /** Le haut d'une rangée au doigt (l'index plat, -1 hors carte). */
    private int rowAt(float y) {
        for (int flat = topRow; flat < topRow + visibleRows
                && flat < flatCount(); flat++) {
            float top = rowTops[flat - topRow];
            if (y >= top && y < top + dp(ROW_HEIGHT_DP)) {
                return flat;
            }
        }
        return -1;
    }

    /** Le titre d'un étage : l'encre d'accent, un filet dessous. */
    private void drawHeader(Canvas canvas, Section family, float top) {
        if (family == null || family.title().isEmpty()) {
            return;
        }
        canvas.drawText(family.title(), poseX + dp(14),
                top + dp(HEADER_DP) * 0.72f, headerPaint);
        canvas.drawLine(poseX + dp(12), top + dp(HEADER_DP) - dp(2),
                poseX + cardW - dp(12), top + dp(HEADER_DP) - dp(2), rulePaint);
    }

    /** Une rangée : son libellé dans la hauteur respirée. */
    private void drawRowLabel(Canvas canvas, Row row, float top) {
        if (row == null) {
            return;
        }
        canvas.drawText(row.label(), poseX + dp(16),
                top + dp(ROW_HEIGHT_DP) * 0.68f, textPaint);
    }

    /** La largeur qu'il faut pour le plus large libellé, respiré. */
    private float measuredWidth() {
        float widest = 0.0f;
        for (Section family : sections) {
            for (Row row : family.rows()) {
                widest = Math.max(widest, textPaint.measureText(row.label()));
            }
        }
        return widest + 2.0f * dp(16);
    }

    private float dp(int value) {
        return value * density;
    }
}
