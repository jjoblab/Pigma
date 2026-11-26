package jo.pigma.editor.popups;

import jo.pigma.editor.EditorView;

import android.graphics.Canvas;
import android.graphics.Paint;

import java.util.List;

/**
 * LA CARTE DE L'ENTOUREMENT (l'entourer de du rapport des popups — le «
 * Surround With » de l'atelier porté au tactile) : la SÉLECTION
 * s'y entoure d'un patron — les PAIRES de signes (parenthèses,
 * accolades, crochets, guillemets) et les BLOCS du langage (les
 * mots-clés d'encadrement — la vue les construit sur son langage).
 * Chaque patron vit en rangée : son NOM à gauche, son APERÇU à
 * droite (le patron entier se lit — « if … then … end » se voit
 * avant de se poser). Le choix POSE l'entourage (UNE unité
 * d'annulation — les deux insertions y passent ensemble), la
 * sélection DEMEURE (le contenu se retravaille aussitôt).
 *
 * <p>La carte vit de {@link ListCard} (R-F) : la pose au caret
 * (dessous si elle tient, dessus sinon), les murs, la croix, la
 * fenêtre de rangées et sa barre de défilement, le pied qui compte.</p>
 *
 * <p>La carte ne prend jamais le focus ; la rangée referme la carte
 * AVANT de poser l'entourage (le geste ne se joue pas deux fois).</p>
 */
public final class SurroundCard extends ListCard {

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

    /** La largeur de la colonne du nom (dp). */
    private static final int NAME_DP = 96;

    /**
     * Un patron d'entourage : son nom (le signe ou le mot-clé), son
     * aperçu (le patron entier se lit avant de se poser), sa pose.
     *
     * @param name    le nom du patron (« ( ) », « if »)
     * @param preview l'aperçu du patron (« if … then … end »)
     * @param apply   la pose de l'entourage (la sélection demeure)
     */
    public record Choice(String name, String preview, Runnable apply) {
    }

    /** Les patrons vivants (vide : la carte se range). */
    private List<Choice> choices = List.of();

    /** La rangée armée par le DOWN (-1 : aucune — l'UP la joue). */
    private int armedRow = -1;

    /** La vue porteuse (la pose s'y exécute). */
    private EditorView editor;

    private final Paint cardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint namePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint previewPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint footerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rulePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glyphPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint scrollbarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    /**
     * Une carte neuve, ses pinceaux propres au style par défaut.
     *
     * @param density la densité de l'écran (le dp en pixels)
     */
    public SurroundCard(float density) {
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
        namePaint.setColor(style.textArgb());
        namePaint.setFakeBoldText(true);
        namePaint.setTextSize((style.textSizeSp() - 3.0f) * density);
        previewPaint.setColor(style.hintArgb());
        previewPaint.setTextSize((style.secondarySizeSp() - 1.0f) * density);
        footerPaint.setColor(style.secondaryArgb());
        footerPaint.setTextSize((style.secondarySizeSp() - 2.0f) * density);
        rulePaint.setColor(style.borderArgb());
        glyphPaint.setColor(style.accentArgb());
        glyphPaint.setStyle(Paint.Style.STROKE);
        glyphPaint.setStrokeWidth(1.5f * density);
        scrollbarPaint.setColor((style.borderArgb() & 0x00FFFFFF) | 0x66000000);
    }

    /**
     * Branche la vue porteuse : la pose d'un patron s'y exécute (hors
     * du geste — la carte vit dans la vue).
     *
     * @param view la vue, jamais null
     */
    public void bind(EditorView view) {
        editor = view;
    }

    /**
     * Ouvre la carte sur des patrons — la liste vide range la carte.
     *
     * @param found les patrons d'entourage, jamais null
     */
    public void open(List<Choice> found) {
        choices = List.copyOf(found);
        topRow = 0;
        shown = !choices.isEmpty();
    }

    /** Ramasse les restes de la fermeture. */
    @Override
    protected void onClosed() {
        choices = List.of();
        topRow = 0;
        armedRow = -1;
    }

    /** Les patrons vivants (le harnais les lit). */
    public List<Choice> choices() {
        return choices;
    }

    /**
     * Le doigt tombé sur la carte lui appartient : la croix la range,
     * la rangée s'ARME au DOWN (l'UP la pose) ; le reste consomme
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
     * Le relâchement joue la rangée armée (l'entourage s'y pose, la
     * carte se range APRÈS — le geste ne se rejoue pas au dessin).
     */
    public boolean onRelease() {
        if (armedRow < 0) {
            return false;
        }
        Choice chosen = choices.get(armedRow);
        armedRow = -1;
        close();
        if (chosen != null && chosen.apply() != null && editor != null) {
            // la pose se JOUE HORS du geste (postée au tour suivant) :
            // le geste se clôt d'abord
            editor.post(chosen.apply());
        }
        return true;
    }

    /**
     * Dessine la carte : la pose se recalcule d'abord — sous la
     * ligne du caret (au-dessus sans la place) —, puis l'en-tête (le
     * titre, la croix), le filet, les rangées de la fenêtre (le nom
     * au gras, l'aperçu à droite), la barre de défilement, le pied
     * qui compte.
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
        canvas.drawText(strings.surround(), poseX + dp(14), headerBaseline,
                titlePaint);
        drawCross(canvas, poseX + cardW - dp(24), poseY + dp(HEADER_DP) / 2.0f,
                glyphPaint);
        canvas.drawLine(poseX + dp(12), poseY + dp(HEADER_DP) - dp(2),
                poseX + cardW - dp(12), poseY + dp(HEADER_DP) - dp(2),
                rulePaint);
        float cursor = poseY + dp(HEADER_DP);
        for (int at = topRow; at < topRow + visibleRows
                && at < choices.size(); at++) {
            drawRow(canvas, choices.get(at), cursor);
            cursor += rowHeight();
        }
        drawScrollbar(canvas, scrollbarPaint, poseY + dp(HEADER_DP), cursor,
                poseX + cardW - dp(4));
        drawFooter(canvas);
    }

    /** Une rangée : le nom au gras, l'aperçu à droite. */
    private void drawRow(Canvas canvas, Choice choice, float top) {
        float rowH = rowHeight();
        float baseline = top + rowH * 0.66f;
        String name = choice.name() == null ? "" : choice.name();
        canvas.drawText(ellipsizeTail(name, namePaint, dp(NAME_DP)),
                poseX + dp(14), baseline, namePaint);
        String preview = choice.preview() == null ? "" : choice.preview();
        canvas.drawText(ellipsizeTail(preview, previewPaint,
                cardW - dp(NAME_DP) - 3 * dp(14)),
                poseX + dp(14) + dp(NAME_DP), baseline, previewPaint);
    }

    /** Le pied de carte : le compte des patrons. */
    private void drawFooter(Canvas canvas) {
        String count = strings.patternsCount(choices.size());
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
        visibleRows = fitRows(MAX_VISIBLE_ROWS, choices.size(),
                dp(HEADER_DP) + dp(FOOTER_DP), rowHeight(),
                metrics.viewHeight(), margin);
        cardH = dp(HEADER_DP) + visibleRows * rowHeight() + dp(FOOTER_DP);
        placeY(anchorTop, anchorBottom, margin, dp(GAP_DP), cardH,
                metrics.viewHeight());
        placeX(margin, metrics);
    }

    /** La largeur qu'il faut pour le plus large patron, respirée. */
    private float measuredWidth() {
        float widest = titlePaint.measureText(strings.surround());
        for (Choice choice : choices) {
            widest = Math.max(widest, namePaint.measureText(
                    choice.name() == null ? "" : choice.name())
                    + previewPaint.measureText(
                            choice.preview() == null ? "" : choice.preview()));
        }
        return widest + dp(NAME_DP) + 3 * dp(14);
    }

    /** Le compte des rangées vivantes. */
    @Override
    protected int rowCount() {
        return choices.size();
    }

    /** La hauteur d'une rangée : le style la porte. */
    @Override
    protected float rowHeight() {
        return dp(style.rowHeightDp() - 6);
    }

    /** Le libellé d'une rangée (le service qui parle la lit). */
    @Override
    protected String virtualRowLabelAt(int at) {
        if (at < 0 || at >= choices.size()) {
            return "";
        }
        Choice choice = choices.get(at);
        return (choice.name() == null ? "" : choice.name()) + ", "
                + (choice.preview() == null ? "" : choice.preview());
    }

    /** L'action d'une rangée (le service qui parle la joue au doigt du lecteur). */
    @Override
    protected boolean virtualRowPlay(int at) {
        if (at < 0 || at >= choices.size()) {
            return false;
        }
        Choice chosen = choices.get(at);
        close();
        if (chosen != null && chosen.apply() != null && editor != null) {
            editor.post(chosen.apply());
        }
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
