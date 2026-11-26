package jo.pigma.editor.popups;

import jo.pigma.editor.EditorView;

import android.graphics.Canvas;
import android.graphics.Paint;

import java.util.List;

/**
 * LA CARTE DES EXTRAITS (les extraits du rapport des popups — les « live
 * templates » de l'atelier portés au tactile) : insérer un squelette
 * au caret — chaque extrait vit en rangée : son ABRÉVIATION à gauche
 * (le nom court qui se lit), son APERÇU à droite (le squelette
 * entier, replié à la largeur), le CHOIX INSCRIT le corps au caret
 * et POSE LE CHAMP sélectionné (la frappe le remplace — le trou se
 * remplit d'un geste).
 *
 * <p>La carte vit de {@link ListCard} (R-F) : la pose au caret
 * (dessous si elle tient, dessus sinon), les murs, la croix, la
 * fenêtre de rangées et sa barre de défilement, le pied qui compte.
 * Le FILTRE n'y vit pas (le registre est court — la leçon de la
 * référence : sa liste vit sans filtre aux gabarits).</p>
 *
 * <p>La carte ne prend jamais le focus ; la rangée referme la carte
 * AVANT d'insérer (le geste ne se joue pas deux fois).</p>
 */
public final class SnippetCard extends ListCard {

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

    /** La largeur de la colonne d'abréviation (dp). */
    private static final int LABEL_DP = 88;

    /**
     * L'insertion d'un extrait : le corps s'inscrit au caret, le
     * champ se pose sélectionné (la frappe le remplace).
     */
    public interface Inserter {

        /**
         * Insère le corps d'un extrait au caret, sélectionne son champ.
         *
         * @param body     le corps du squelette
         * @param fieldAt  le début du champ dans le corps
         * @param fieldEnd la fin du champ dans le corps
         */
        void insert(String body, int fieldAt, int fieldEnd);
    }

    /** Les extraits vivants (vide : la carte se range). */
    private List<SnippetRegistry.Snippet> snippets = List.of();

    /** La rangée armée par le DOWN (-1 : aucune — l'UP la joue). */
    private int armedRow = -1;

    /** La vue porteuse (l'insertion s'y exécute). */
    private EditorView editor;

    /** L'inserteur des corps (la vue le pose). */
    private Inserter inserter;

    private final Paint cardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bodyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint footerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rulePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glyphPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint scrollbarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    /**
     * Une carte neuve, ses pinceaux propres au style par défaut.
     *
     * @param density la densité de l'écran (le dp en pixels)
     */
    public SnippetCard(float density) {
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
        labelPaint.setColor(style.textArgb());
        labelPaint.setFakeBoldText(true);
        labelPaint.setTextSize((style.textSizeSp() - 3.0f) * density);
        bodyPaint.setColor(style.hintArgb());
        bodyPaint.setTextSize((style.secondarySizeSp() - 1.0f) * density);
        footerPaint.setColor(style.secondaryArgb());
        footerPaint.setTextSize((style.secondarySizeSp() - 2.0f) * density);
        rulePaint.setColor(style.borderArgb());
        glyphPaint.setColor(style.accentArgb());
        glyphPaint.setStyle(Paint.Style.STROKE);
        glyphPaint.setStrokeWidth(1.5f * density);
        scrollbarPaint.setColor((style.borderArgb() & 0x00FFFFFF) | 0x66000000);
    }

    /**
     * Branche la vue porteuse : l'insertion d'un extrait s'y exécute
     * (hors du geste — la carte vit dans la vue).
     *
     * @param view     la vue, jamais null
     * @param inserter l'inserteur des corps (la vue le pose)
     */
    public void bind(EditorView view, Inserter inserter) {
        editor = view;
        this.inserter = inserter;
    }

    /**
     * Ouvre la carte sur des extraits — la liste vide range la carte.
     *
     * @param found les extraits du registre, jamais null
     */
    public void open(List<SnippetRegistry.Snippet> found) {
        snippets = List.copyOf(found);
        topRow = 0;
        shown = !snippets.isEmpty();
    }

    /** Ramasse les restes de la fermeture. */
    @Override
    protected void onClosed() {
        snippets = List.of();
        topRow = 0;
        armedRow = -1;
    }

    /** Les extraits vivants (le harnais les lit). */
    public List<SnippetRegistry.Snippet> snippets() {
        return snippets;
    }

    /**
     * Le doigt tombé sur la carte lui appartient : la croix la range,
     * la rangée s'ARME au DOWN (l'UP l'inscrit) ; le reste consomme
     * sans agir. Hors de la carte : faux — le geste poursuit sa vie
     * vers l'éditeur.
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
     * Le relâchement joue la rangée armée : l'extrait s'inscrit au
     * caret, son champ se pose sélectionné — la carte se range
     * APRÈS (le geste ne se rejoue pas au dessin).
     */
    public boolean onRelease() {
        if (armedRow < 0) {
            return false;
        }
        SnippetRegistry.Snippet chosen = snippets.get(armedRow);
        armedRow = -1;
        close();
        if (chosen != null && inserter != null && editor != null) {
            final SnippetRegistry.Snippet snippet = chosen;
            // l'insertion se JOUE HORS du geste (postée au tour
            // suivant) : le geste se clôt d'abord
            editor.post(() -> inserter.insert(snippet.body(),
                    snippet.fieldAt(), snippet.fieldEnd()));
        }
        return true;
    }

    /**
     * Dessine la carte : la pose se recalcule d'abord — sous la ligne
     * du caret (au-dessus sans la place) —, puis l'en-tête (le titre,
     * la croix), le filet, les rangées de la fenêtre (l'abréviation
     * au gras, l'aperçu replié à droite), la barre de défilement, le
     * pied qui compte.
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
        canvas.drawText("Extraits", poseX + dp(14), headerBaseline, titlePaint);
        drawCross(canvas, poseX + cardW - dp(24), poseY + dp(HEADER_DP) / 2.0f,
                glyphPaint);
        canvas.drawLine(poseX + dp(12), poseY + dp(HEADER_DP) - dp(2),
                poseX + cardW - dp(12), poseY + dp(HEADER_DP) - dp(2),
                rulePaint);
        float cursor = poseY + dp(HEADER_DP);
        for (int at = topRow; at < topRow + visibleRows
                && at < snippets.size(); at++) {
            drawRow(canvas, snippets.get(at), cursor);
            cursor += rowHeight();
        }
        drawScrollbar(canvas, scrollbarPaint, poseY + dp(HEADER_DP), cursor,
                poseX + cardW - dp(4));
        drawFooter(canvas);
    }

    /** Une rangée : l'abréviation au gras, l'aperçu replié à droite. */
    private void drawRow(Canvas canvas, SnippetRegistry.Snippet snippet,
            float top) {
        float rowH = rowHeight();
        float baseline = top + rowH * 0.66f;
        String label = snippet.label() == null ? "" : snippet.label();
        canvas.drawText(ellipsizeTail(label, labelPaint, dp(LABEL_DP)),
                poseX + dp(14), baseline, labelPaint);
        // l'aperçu : le corps replié à une ligne (les sauts de ligne
        // y deviennent l'espace — le squelette se lit d'un coup d'œil)
        String preview = oneLine(snippet.body());
        canvas.drawText(ellipsizeTail(preview, bodyPaint,
                cardW - dp(LABEL_DP) - 3 * dp(14)),
                poseX + dp(14) + dp(LABEL_DP), baseline, bodyPaint);
    }

    /** Le corps replié à une ligne (les sauts y deviennent l'espace). */
    private static String oneLine(String body) {
        if (body == null || body.isEmpty()) {
            return "";
        }
        String squeezed = body.replace('\n', ' ').trim();
        return squeezed.replace("    ", " ");
    }

    /** Le pied de carte : le compte des extraits. */
    private void drawFooter(Canvas canvas) {
        String count = snippets.size() == 1
                ? "1 extrait" : snippets.size() + " extraits";
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
        cardW = clampWidth(measuredWidth(), style.minCardWidthDp() - 80,
                style.maxCardWidthDp() - 60, margin, metrics);
        visibleRows = fitRows(MAX_VISIBLE_ROWS, snippets.size(),
                dp(HEADER_DP) + dp(FOOTER_DP), rowHeight(),
                metrics.viewHeight(), margin);
        cardH = dp(HEADER_DP) + visibleRows * rowHeight() + dp(FOOTER_DP);
        placeY(anchorTop, anchorBottom, margin, dp(GAP_DP), cardH,
                metrics.viewHeight());
        placeX(margin, metrics);
    }

    /** La largeur qu'il faut pour la plus large rangée, respirée. */
    private float measuredWidth() {
        float widest = titlePaint.measureText("Extraits");
        for (SnippetRegistry.Snippet snippet : snippets) {
            widest = Math.max(widest, labelPaint.measureText(
                    snippet.label() == null ? "" : snippet.label())
                    + bodyPaint.measureText(oneLine(snippet.body())));
        }
        return widest + dp(LABEL_DP) + 3 * dp(14);
    }

    /** Le compte des rangées vivantes. */
    @Override
    protected int rowCount() {
        return snippets.size();
    }

    /** La hauteur d'une rangée : le style la porte. */
    @Override
    protected float rowHeight() {
        return dp(style.rowHeightDp() - 6);
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
