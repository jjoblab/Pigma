package jo.pigma.editor.popups;

import jo.pigma.editor.EditorView;

import android.graphics.Canvas;
import android.graphics.Paint;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * LA CARTE DE L'APERÇU DU RENOMMAGE (l'aperçu du renommage du rapport des popups) : le
 * renommage se VOIT avant de se poser — chaque occurrence du symbole
 * vit en rangée (sa place à droite, l'EXTRAIT dessous — le code s'y
 * lit), la CASE À COCHER à gauche la retient ou l'ôte (le renommage
 * partiel vit : les occurrences cochées seules se remplacent), le
 * bouton « Appliquer » du pied POSE le remplacement (UNE unité
 * d'annulation — la leçon du protocole : le renommage entier se
 * défait d'un geste).
 *
 * <p>La carte vit de {@link ListCard} (R-F) : la pose au caret
 * (dessous si elle tient, dessus sinon), les murs, la croix, la
 * fenêtre de rangées et sa barre de défilement. Le pied compte et
 * porte le bouton.</p>
 *
 * <p>La carte ne prend jamais le focus ; l'application range la carte
 * AVANT de poser les remplacements (le geste ne se joue pas deux
 * fois). Les remplacements COCHÉS seuls partent — l'apporteur de la
 * vue y reçoit les occurrences retenues.</p>
 */
public final class RenamePreviewCard extends ListCard {

    /** La hauteur de l'en-tête (dp). */
    private static final int HEADER_DP = 46;

    /** La hauteur du pied de carte (dp) — le bouton « Appliquer » y vit. */
    private static final int FOOTER_DP = 48;

    /** La marge aux murs de la vue (dp). */
    private static final int MARGIN_DP = 8;

    /** L'écart au caret (dp). */
    private static final int GAP_DP = 4;

    /** Le plafond de rangées visibles (la carte défile au-delà). */
    private static final int MAX_VISIBLE_ROWS = 8;

    /** Le côté de la case à cocher (dp). */
    private static final int CHECK_DP = 22;

    /** La colonne de la case à cocher (dp). */
    private static final int CHECK_COL_DP = 40;

    /**
     * Une occurrence du renommage : sa place (le fichier · la ligne),
     * l'extrait de la ligne (le code s'y lit), son remplacement (les
     * éditions du protocole — cochée, elles se posent).
     *
     * @param where   la place de l'occurrence
     * @param excerpt l'extrait de sa ligne
     * @param edits   ses remplacements du protocole
     */
    public record Occurrence(String where, String excerpt,
            List<jo.pigma.lsp.LspTextEdit> edits) {
    }

    /** Les occurrences vivantes, cochées ou non (vide : la carte se range). */
    private List<Occurrence> occurrences = List.of();

    /** L'état coché de chaque rangée. */
    private boolean[] checked = new boolean[0];

    /** Le titre (« Renommer « x » en « y » »). */
    private String title = "";

    /** La rangée armée par le DOWN (-1 : aucune — l'UP y bascule). */
    private int armedRow = -1;

    /** Le bouton « Appliquer » est-il armé ? */
    private boolean applyPressed;

    /** Le doigt vit-il dans le cadre ? */
    private boolean pressed;

    /** L'apporteur des occurrences retenues (la vue le pose). */
    private Consumer<List<Occurrence>> applier;

    /** La vue porteuse (l'application s'y exécute). */
    private EditorView editor;

    private final Paint cardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint detailPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint boxPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint footerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint chipPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint chipBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint chipTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glyphPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rulePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint scrollbarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    /**
     * Une carte neuve, ses pinceaux propres au style par défaut.
     *
     * @param density la densité de l'écran (le dp en pixels)
     */
    public RenamePreviewCard(float density) {
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
        // la case à cocher : le carré au trait, la coche à l'accent
        boxPaint.setColor(style.textArgb());
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(1.4f * density);
        tickPaint.setColor(style.accentArgb());
        tickPaint.setStyle(Paint.Style.STROKE);
        tickPaint.setStrokeCap(Paint.Cap.ROUND);
        tickPaint.setStrokeWidth(1.8f * density);
        footerPaint.setColor(style.secondaryArgb());
        footerPaint.setTextSize((style.secondarySizeSp() - 2.0f) * density);
        // le bouton « Appliquer » : la puce à l'accent (l'action s'y
        // lit en teinte d'action — la leçon de la barre de sélection)
        chipPaint.setColor((style.accentArgb() & 0x00FFFFFF) | 0x1A000000);
        chipBorderPaint.setColor(style.accentArgb());
        chipBorderPaint.setStyle(Paint.Style.STROKE);
        chipBorderPaint.setStrokeWidth(1.0f);
        chipTextPaint.setColor(style.accentArgb());
        chipTextPaint.setFakeBoldText(true);
        chipTextPaint.setTextSize((style.secondarySizeSp() - 1.0f) * density);
        glyphPaint.setColor(style.accentArgb());
        glyphPaint.setStyle(Paint.Style.STROKE);
        glyphPaint.setStrokeWidth(1.5f * density);
        rulePaint.setColor(style.borderArgb());
        scrollbarPaint.setColor((style.borderArgb() & 0x00FFFFFF) | 0x66000000);
    }

    /**
     * Branche la vue porteuse : l'application des occurrences
     * retenues s'y exécute (hors du geste — la carte vit dans la vue).
     *
     * @param view    la vue, jamais null
     * @param applier l'apporteur des occurrences retenues (les
     *                remplacements s'y posent — UNE unité
     *                d'annulation)
     */
    public void bind(EditorView view, Consumer<List<Occurrence>> applier) {
        editor = view;
        this.applier = applier;
    }

    /**
     * Ouvre la carte sur les occurrences d'un renommage — la liste
     * vide range la carte. Les cases partent COCHÉES (le renommage
     * entier se propose — l'ôte y retranche).
     *
     * @param ofTitle     le titre (« Renommer « x » en « y » »)
     * @param found       les occurrences, jamais null
     */
    public void open(String ofTitle, List<Occurrence> found) {
        title = ofTitle == null ? "" : ofTitle;
        occurrences = List.copyOf(found);
        checked = new boolean[occurrences.size()];
        for (int at = 0; at < checked.length; at++) {
            checked[at] = true;
        }
        topRow = 0;
        shown = !occurrences.isEmpty();
    }

    /** Ramasse les restes de la fermeture. */
    @Override
    protected void onClosed() {
        occurrences = List.of();
        checked = new boolean[0];
        title = "";
        topRow = 0;
        armedRow = -1;
        applyPressed = false;
        pressed = false;
    }

    /** Les occurrences vivantes (le harnais les lit). */
    public List<Occurrence> occurrences() {
        return occurrences;
    }

    /** L'état coché d'une rangée (le harnais le lit). */
    public boolean checked(int index) {
        return index >= 0 && index < checked.length && checked[index];
    }

    /** Le titre de la carte (le harnais le lit). */
    public String title() {
        return title;
    }

    /**
     * Le doigt tombé sur la carte lui appartient : la croix la range,
     * la rangée s'ARME au DOWN (l'UP Y BASCULE la case — le
     * renommage partiel vit), le bouton « Appliquer » s'arme. Hors de
     * la carte : faux — le geste poursuit sa vie vers l'éditeur.
     *
     * @param x l'abscisse du doigt (px, coordonnées de la vue)
     * @param y l'ordonnée du doigt (px, coordonnées de la vue)
     * @return vrai quand la carte a pris le geste
     */
    public boolean onTouchArea(float x, float y) {
        if (!isShown() || !contains(x, y)) {
            return false;
        }
        pressed = true;
        applyPressed = false;
        // la croix : la carte se range
        float crossLeft = poseX + cardW - dp(32);
        if (y < poseY + dp(HEADER_DP)
                && x >= crossLeft && x <= poseX + cardW - dp(8)) {
            close();
            return true;
        }
        // le bouton « Appliquer » du pied
        if (y >= poseY + cardH - dp(FOOTER_DP)) {
            applyPressed = true;
        }
        armedRow = rowAt(y, poseY + dp(HEADER_DP));
        return true;
    }

    /**
     * Le relâchement joue : la rangée armée BASCULE sa case, le
     * bouton « Appliquer » range la carte puis POSE les remplacements
     * des occurrences retenues (hors du geste) — les retenues se
     * LISENT AVANT la fermeture (la fermeture ramasse les restes).
     */
    public boolean onRelease() {
        if (!pressed) {
            return false;
        }
        pressed = false;
        if (applyPressed) {
            applyPressed = false;
            armedRow = -1;
            // LES RETENUES se lisent AVANT la fermeture (close()
            // ramasse les occurrences — la liste vide n'y pose rien)
            List<Occurrence> retained = new ArrayList<>();
            for (int at = 0; at < occurrences.size(); at++) {
                if (checked[at]) {
                    retained.add(occurrences.get(at));
                }
            }
            close();
            if (applier != null && editor != null) {
                // l'application se JOUE HORS du geste (postée au tour
                // suivant) : le geste se clôt d'abord
                final List<Occurrence> chosen = List.copyOf(retained);
                editor.post(() -> applier.accept(chosen));
            }
            return true;
        }
        if (armedRow >= 0) {
            checked[armedRow] = !checked[armedRow];
            armedRow = -1;
            return true;
        }
        return true;
    }

    /**
     * Dessine la carte : la pose se recalcule d'abord — sous la ligne
     * du caret (au-dessus sans la place) —, puis l'en-tête (le titre,
     * la croix), le filet, les rangées de la fenêtre (la case à
     * cocher, la place, l'extrait dessous), la barre de défilement,
     * le pied (le compte, le bouton « Appliquer (N) »).
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
        canvas.drawText(ellipsizeTail(title, titlePaint, cardW - dp(76)),
                poseX + dp(14), headerBaseline, titlePaint);
        drawCross(canvas, poseX + cardW - dp(24), poseY + dp(HEADER_DP) / 2.0f,
                glyphPaint);
        canvas.drawLine(poseX + dp(12), poseY + dp(HEADER_DP) - dp(2),
                poseX + cardW - dp(12), poseY + dp(HEADER_DP) - dp(2),
                rulePaint);
        float cursor = poseY + dp(HEADER_DP);
        for (int at = topRow; at < topRow + visibleRows
                && at < occurrences.size(); at++) {
            drawRow(canvas, occurrences.get(at), checked[at], cursor);
            cursor += rowHeight();
        }
        drawScrollbar(canvas, scrollbarPaint, poseY + dp(HEADER_DP), cursor,
                poseX + cardW - dp(4));
        drawFooter(canvas);
    }

    /** Une rangée : la case à cocher, la place, l'extrait dessous. */
    private void drawRow(Canvas canvas, Occurrence occurrence, boolean on,
            float top) {
        float rowH = rowHeight();
        float cy = top + rowH / 2.0f;
        // LA CASE À COCHER : le carré au trait, la coche à l'accent
        float half = dp(CHECK_DP) / 2.0f;
        canvas.drawRect(poseX + dp(12), cy - half,
                poseX + dp(12) + dp(CHECK_DP), cy + half, boxPaint);
        if (on) {
            canvas.drawLine(poseX + dp(12) + dp(5), cy,
                    poseX + dp(12) + dp(CHECK_DP) / 2.0f,
                    cy + half - dp(4), tickPaint);
            canvas.drawLine(poseX + dp(12) + dp(CHECK_DP) / 2.0f,
                    cy + half - dp(4),
                    poseX + dp(12) + dp(CHECK_DP) - dp(5), cy - half + dp(6),
                    tickPaint);
        }
        float baseline = top + rowH * 0.58f;
        String where = occurrence.where() == null ? "" : occurrence.where();
        canvas.drawText(ellipsizeTail(where, textPaint,
                cardW - dp(CHECK_COL_DP) - 2 * dp(14)),
                poseX + dp(CHECK_COL_DP), baseline, textPaint);
        // L'EXTRAIT de la ligne (le code s'y lit — B7 : la rangée
        // montre le code, pas un numéro seul)
        String excerpt = occurrence.excerpt() == null
                ? "" : occurrence.excerpt().trim();
        if (!excerpt.isEmpty()) {
            String squeezed = excerpt.length() > 60
                    ? excerpt.substring(0, 57) + "…" : excerpt;
            canvas.drawText(squeezed, poseX + dp(CHECK_COL_DP),
                    top + rowH * 0.90f, detailPaint);
        }
    }

    /** Le pied de carte : le compte à gauche, le bouton « Appliquer (N) ». */
    private void drawFooter(Canvas canvas) {
        int retained = 0;
        for (boolean on : checked) {
            if (on) {
                retained++;
            }
        }
        float footerBaseline = poseY + cardH - dp(FOOTER_DP) / 2.0f;
        String count = retained + "/" + occurrences.size();
        canvas.drawText(count, poseX + dp(14), footerBaseline, footerPaint);
        // le bouton « Appliquer » : la puce à l'accent (l'action s'y lit)
        String label = retained > 0 ? strings.applyWith(retained)
                : strings.apply();
        float labelW = chipTextPaint.measureText(label);
        float chipW = labelW + dp(24);
        float chipLeft = poseX + cardW - dp(14) - chipW;
        float chipTop = poseY + cardH - dp(FOOTER_DP) + dp(7);
        float chipBottom = poseY + cardH - dp(7);
        float chipRadius = dp(10);
        canvas.drawRoundRect(chipLeft, chipTop, chipLeft + chipW, chipBottom,
                chipRadius, chipRadius, chipPaint);
        canvas.drawRoundRect(chipLeft, chipTop, chipLeft + chipW, chipBottom,
                chipRadius, chipRadius, chipBorderPaint);
        canvas.drawText(label, chipLeft + dp(12),
                (chipTop + chipBottom) / 2.0f + dp(4), chipTextPaint);
    }

    /**
     * Pose la carte sous la ligne du caret (au-dessus sans la place,
     * jamais sur elle), le bord gauche du texte borné aux murs.
     */
    private void place(float anchorTop, float anchorBottom,
            PopupMetrics metrics) {
        float margin = dp(MARGIN_DP);
        cardW = clampWidth(measuredWidth(), style.minCardWidthDp() - 60,
                style.maxCardWidthDp() - 40, margin, metrics);
        visibleRows = fitRows(MAX_VISIBLE_ROWS, occurrences.size(),
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

    /** La largeur qu'il faut pour la plus large rangée, respirée. */
    private float measuredWidth() {
        float widest = titlePaint.measureText(title);
        for (Occurrence occurrence : occurrences) {
            widest = Math.max(widest, textPaint.measureText(
                    occurrence.where() == null ? "" : occurrence.where()));
        }
        return widest + dp(CHECK_COL_DP) + 2 * dp(14);
    }

    /** Le compte des rangées vivantes. */
    @Override
    protected int rowCount() {
        return occurrences.size();
    }

    /** La hauteur d'une rangée : le style la porte, l'extrait y respire. */
    @Override
    protected float rowHeight() {
        return dp(style.rowHeightDp() - 2);
    }

    /** Le libellé d'une rangée (le service qui parle la lit — la case suit). */
    @Override
    protected String virtualRowLabelAt(int at) {
        if (at < 0 || at >= occurrences.size()) {
            return "";
        }
        RenamePreviewCard.Occurrence occurrence = occurrences.get(at);
        String label = occurrence.where() == null ? "" : occurrence.where();
        if (occurrence.excerpt() != null && !occurrence.excerpt().trim().isEmpty()) {
            label += " — " + occurrence.excerpt().trim();
        }
        return label + (checked[at] ? " — " + strings.apply() : "");
    }

    /** L'action d'une rangée (le service qui parle la BASCULE — le
     * renommage partiel vit au lecteur d'écran aussi). */
    @Override
    protected boolean virtualRowPlay(int at) {
        if (at < 0 || at >= occurrences.size()) {
            return false;
        }
        checked[at] = !checked[at];
        return true;
    }

    /** L'ordonnée du centre d'une rangée (le harnais y vise le doigt). */
    @Override
    public float rowCenterY(int index) {
        if (index < topRow || index >= topRow + visibleRows) {
            return -1.0f;
        }
        return poseY + dp(HEADER_DP) + (index - topRow) * rowHeight()
                + rowHeight() / 2.0f;
    }
}
