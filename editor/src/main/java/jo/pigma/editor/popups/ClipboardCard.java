package jo.pigma.editor.popups;

import android.graphics.Canvas;
import android.graphics.Paint;

import java.util.ArrayList;
import java.util.List;

/**
 * La carte de L'HISTORIQUE DU PRESSE-PAPIERS (le choix de cibles0 du rapport des
 * popups — « Coller depuis l'historique ») : chaque copie d'avant y
 * vit en rangée — l'APERÇU troncé du texte, l'ancienneté à droite
 * (la plus récente au sommet). Le choix d'une rangée y COLLE le
 * texte au caret (le collage d'avant renaît au doigt).
 *
 * <p>La carte vit de {@link ListCard} (R-F) : la pose au caret
 * (dessous si elle tient, dessus sinon), les murs, la croix, la
 * fenêtre de rangées et sa barre de défilement, le pied qui
 * compte.</p>
 *
 * <p>La carte ne prend jamais le focus ; la rangée referme la carte
 * AVANT de coller (le geste ne se joue pas deux fois).</p>
 */
public final class ClipboardCard extends ListCard {

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

    /**
     * Une rangée : le texte de la copie, son ancienneté — le choix y
     * colle.
     */
    private record Row(String text, long when) {
    }

    /** Les rangées vivantes (vide : la carte se range). */
    private List<Row> rows = List.of();

    /** L'heure du moment (l'ancienneté s'y lit). */
    private long now;

    /** La rangée armée par le DOWN (-1 : aucune — l'UP la joue). */
    private int armedRow = -1;

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
    public ClipboardCard(float density) {
        super(density);
        applyStyle();
    }

    /** Reconfigure les pinceaux au style courant. */
    @Override
    protected void applyStyle() {
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
     * Ouvre la carte sur les entrées de l'historique — la liste vide
     * range la carte.
     *
     * @param entries les entrées (la plus récente au sommet), jamais null
     * @param now     l'heure du moment (l'ancienneté s'y lit)
     */
    public void open(List<ClipboardHistory.Entry> entries, long now) {
        List<Row> built = new ArrayList<>();
        for (ClipboardHistory.Entry entry : entries) {
            if (entry != null && entry.text() != null
                    && !entry.text().isEmpty()) {
                built.add(new Row(entry.text(), entry.when()));
            }
        }
        rows = built;
        this.now = now;
        topRow = 0;
        shown = !rows.isEmpty();
    }

    /** Ramasse les restes de la fermeture. */
    @Override
    protected void onClosed() {
        rows = List.of();
        topRow = 0;
        armedRow = -1;
    }

    /** Les entrées vivantes (le harnais les lit). */
    public List<ClipboardHistory.Entry> entries() {
        List<ClipboardHistory.Entry> found = new ArrayList<>();
        for (Row row : rows) {
            found.add(new ClipboardHistory.Entry(row.text(), row.when()));
        }
        return found;
    }

    /**
     * Le doigt tombé sur la carte lui appartient : la croix la
     * range, la rangée d'une copie s'ARME au DOWN (l'UP la joue) ;
     * le reste consomme sans agir. Hors de la carte : faux — le
     * geste poursuit sa vie vers l'éditeur.
     *
     * @param x l'abscisse du doigt (px, coordonnées de la vue)
     * @param y l'ordonnée du doigt (px, coordonnées de la vue)
     * @return vrai quand la carte a pris le geste
     */
    public boolean onTouchArea(float x, float y) {
        if (!isShown() || !contains(x, y)) {
            return false;
        }
        // la croix : la carte se range
        float crossLeft = poseX + cardW - dp(32);
        if (y < poseY + dp(HEADER_DP)
                && x >= crossLeft && x <= poseX + cardW - dp(8)) {
            close();
            return true;
        }
        armedRow = rowAt(y, poseY + dp(HEADER_DP));
        return true;
    }

    /**
     * Le relâchement joue la rangée armée — le texte de la copie y
     * PART (le colleur le prend, la carte se range APRÈS).
     *
     * @return vrai quand une rangée s'est jouée
     */
    public boolean onRelease() {
        if (armedRow < 0) {
            return false;
        }
        Row chosen = rows.get(armedRow);
        armedRow = -1;
        close();
        if (chosen != null) {
            pendingText = chosen.text();
        }
        return true;
    }

    /** Le texte à coller du dernier choix, null si aucun (le colleur le prend). */
    private String pendingText;

    /** Le texte à coller du dernier choix, null si aucun. */
    public String pendingPaste() {
        return pendingText;
    }

    /** Le texte à coller se prend (le colleur le pose). */
    public void takePendingPaste() {
        pendingText = null;
    }

    /**
     * Dessine la carte : la pose se recalcule d'abord — sous le
     * caret (au-dessus sans la place) —, puis l'en-tête (le titre,
     * la croix), le filet, les rangées de la fenêtre (l'aperçu
     * tronqué, l'ancienneté à droite), la barre de défilement si la
     * fenêtre cache des rangées, le pied qui compte.
     *
     * @param canvas  le canevas de la vue
     * @param anchorTop le bord haut de la ligne du caret (px, vue)
     * @param anchorBottom le bord bas de la ligne du caret (px, vue)
     * @param metrics la géométrie courante de la vue
     */
    public void draw(Canvas canvas, float anchorTop, float anchorBottom,
            PopupMetrics metrics) {
        if (!isShown()) {
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
        canvas.drawText(strings.clipboard(), poseX + dp(14), headerBaseline,
                titlePaint);
        drawCross(canvas, poseX + cardW - dp(24), poseY + dp(HEADER_DP) / 2.0f,
                glyphPaint);
        canvas.drawLine(poseX + dp(12), poseY + dp(HEADER_DP) - dp(2),
                poseX + cardW - dp(12), poseY + dp(HEADER_DP) - dp(2),
                rulePaint);
        float cursor = poseY + dp(HEADER_DP);
        for (int at = topRow; at < topRow + visibleRows
                && at < rows.size(); at++) {
            drawRow(canvas, rows.get(at), cursor);
            cursor += rowHeight();
        }
        drawScrollbar(canvas, scrollbarPaint, poseY + dp(HEADER_DP), cursor,
                poseX + cardW - dp(4));
        drawFooter(canvas);
    }

    /** Une rangée : l'aperçu du texte (troncé), l'ancienneté à droite. */
    private void drawRow(Canvas canvas, Row row, float top) {
        float rowH = rowHeight();
        float baseline = top + rowH * 0.66f;
        // L'APERÇU TRONCÉ : les blancs de tête se retirent, une
        // ligne seule s'y montre (la copie se lit du premier coup
        // d'œil — la ligne suivante la complète au collage)
        String preview = row.text().stripLeading().replace('\n', ' ');
        String squeezed = preview.length() > 48
                ? preview.substring(0, 45) + "…" : preview;
        canvas.drawText(ellipsizeTail(squeezed, textPaint, cardW - dp(120)),
                poseX + dp(14), baseline, textPaint);
        String age = ClipboardHistory.ageOf(row.when(), now);
        float ageW = detailPaint.measureText(age);
        canvas.drawText(age, poseX + cardW - dp(14) - ageW, baseline,
                detailPaint);
    }

    /** Le pied de carte : le compte des copies. */
    private void drawFooter(Canvas canvas) {
        String count = strings.copiesCount(rows.size());
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
        cardW = clampWidth(measuredWidth(), style.minCardWidthDp() - 100,
                style.maxCardWidthDp() - 60, margin, metrics);
        visibleRows = fitRows(MAX_VISIBLE_ROWS, rows.size(),
                dp(HEADER_DP) + dp(FOOTER_DP), rowHeight(),
                metrics.viewHeight(), margin);
        cardH = dp(HEADER_DP) + visibleRows * rowHeight() + dp(FOOTER_DP);
        placeY(anchorTop, anchorBottom, margin, dp(GAP_DP), cardH,
                metrics.viewHeight());
        placeX(margin, metrics);
        // LA POSE POSÉE : le flot s'y nourrit (la carte glisse à sa pose
        // nouvelle au lieu d'y sauter — le retour du terrain)
        poseSettled();
    }

    /** La largeur qu'il faut pour le plus large aperçu, respiré. */
    private float measuredWidth() {
        float widest = titlePaint.measureText("Presse-papiers");
        for (Row row : rows) {
            widest = Math.max(widest, textPaint.measureText(
                    row.text().stripLeading().replace('\n', ' ')));
        }
        return widest + 2 * dp(14);
    }

    /** Le compte des rangées vivantes. */
    @Override
    protected int rowCount() {
        return rows.size();
    }

    /** La hauteur d'une rangée : le style la porte. */
    @Override
    protected float rowHeight() {
        return dp(style.rowHeightDp() - 10);
    }

    /** L'ordonnée du centre d'une rangée (le harnais y vise le doigt). */
    public float rowCenterY(int index) {
        if (index < topRow || index >= topRow + visibleRows) {
            return -1.0f;
        }
        return poseY + dp(HEADER_DP) + (index - topRow) * rowHeight()
                + rowHeight() / 2.0f;
    }
}
