package jo.pigma.editor.popups;

import jo.pigma.editor.EditorView;

import android.graphics.Canvas;
import android.graphics.Paint;

import java.util.List;

/**
 * La carte du CHOIX DE CIBLES (« Choisir une déclaration ») : le
 * serveur y renvoie PLUSIEURS places (des surcharges, des
 * implémentations, des interfaces) — une rangée par cible : le nom,
 * le fichier et sa ligne à droite, l'EXTRAIT de la ligne dessous
 * (le code se lit, le choix y navigue). Le défaut du rapport des
 * popups (D-P3) : l'aller-à la définition SAUTAIT à la première
 * cible, aveugle aux autres.
 *
 * <p>La carte vit de {@link ListCard} (R-F) : la pose au caret
 * (dessous si elle tient, dessus sinon), les murs, la croix, la
 * fenêtre de rangées et sa barre de défilement. Posée par la façade
 * du serveur ({@code LspEditor.navigateTo}), ancrée au caret.</p>
 *
 * <p>La carte ne prend jamais le focus ; la rangée referme la carte
 * AVANT de naviguer (le geste ne se joue pas deux fois).</p>
 */
public final class TargetCard extends ListCard {

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
     * Une cible : son nom (le symbole questionné), son fichier et sa
     * ligne, l'extrait de la ligne (la cible se lit dans son code —
     * le choix y navigue).
     *
     * @param name    le nom de la cible
     * @param file    le fichier de la cible (vide : le document courant)
     * @param line    l'indice de la ligne
     * @param excerpt l'extrait de la ligne, coupé au besoin
     * @param go      le saut à la cible
     */
    public record Target(String name, String file, int line, String excerpt,
            Runnable go) {
    }

    /** Les cibles vivantes (vide : la carte se range). */
    private List<Target> targets = List.of();

    /** Le titre (« Définitions de « x » », « Implémentations de « x » »). */
    private String title = "";

    /** La rangée armée par le DOWN (-1 : aucune — l'UP la joue). */
    private int armedRow = -1;

    /** La vue porteuse (la navigation s'y poste). */
    private EditorView editor;

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
    public TargetCard(float density) {
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
     * Branche la vue porteuse : la navigation d'une rangée s'y poste
     * (hors du geste — la carte vit dans la vue).
     *
     * @param view la vue, jamais null
     */
    public void bind(EditorView view) {
        editor = view;
    }

    /**
     * Ouvre la carte sur les cibles d'une chose — la liste vide range
     * la carte.
     *
     * @param ofWhat la chose cherchée (le titre le dit), jamais null
     * @param found  les cibles, jamais null
     */
    public void open(String ofWhat, List<Target> found) {
        title = ofWhat == null ? "" : ofWhat;
        targets = List.copyOf(found);
        topRow = 0;
        shown = !targets.isEmpty();
    }

    /** Ramasse les restes de la fermeture. */
    @Override
    protected void onClosed() {
        targets = List.of();
        title = "";
        topRow = 0;
        armedRow = -1;
    }

    /** Les cibles vivantes (le harnais les lit). */
    public List<Target> targets() {
        return targets;
    }

    /** Le titre de la carte (le harnais le lit). */
    public String title() {
        return title;
    }

    /**
     * Le doigt tombé sur la carte lui appartient : la croix la
     * range, la rangée d'une cible s'ARME au DOWN (l'UP la joue) ;
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
     * Le relâchement joue la rangée armée (la navigation s'y exécute,
     * la carte se range APRÈS — le geste ne se rejoue pas au dessin).
     */
    public boolean onRelease() {
        if (armedRow < 0) {
            return false;
        }
        Target chosen = targets.get(armedRow);
        armedRow = -1;
        close();
        if (chosen != null && chosen.go() != null && editor != null) {
            // la navigation se JOUE HORS du geste (postée au tour
            // suivant) : le geste se clôt d'abord
            editor.post(chosen.go());
        }
        return true;
    }

    /**
     * Dessine la carte : la pose se recalcule d'abord — sous le
     * caret (au-dessus sans la place) —, puis l'en-tête (le titre,
     * la croix), le filet, les rangées de la fenêtre (l'icône de
     * cible, le nom, le fichier et la ligne à droite, l'extrait
     * dessous), la barre de défilement si la fenêtre cache des
     * rangées, le pied qui compte.
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
                && at < targets.size(); at++) {
            drawRow(canvas, targets.get(at), cursor);
            cursor += rowHeight();
        }
        drawScrollbar(canvas, scrollbarPaint, poseY + dp(HEADER_DP), cursor,
                poseX + cardW - dp(4));
        drawFooter(canvas);
    }

    /** Une rangée : l'icône de cible, le nom, le fichier · la ligne, l'extrait. */
    private void drawRow(Canvas canvas, Target target, float top) {
        float rowH = rowHeight();
        float cy = top + rowH / 2.0f;
        float baseline = top + rowH * 0.66f;
        // l'icône de cible : le chevron vers la cible (le trait à
        // l'accent, discret comme la carte des usages)
        float side = dp(ICON_DP) * 0.5f;
        canvas.drawLine(poseX + dp(14) + side * 0.4f, cy - side * 0.4f,
                poseX + dp(14) + side, cy, glyphPaint);
        canvas.drawLine(poseX + dp(14) + side * 0.4f, cy + side * 0.4f,
                poseX + dp(14) + side, cy, glyphPaint);
        String name = target.name() == null ? "" : target.name();
        canvas.drawText(ellipsizeTail(name, textPaint,
                cardW - dp(ICON_DP) - 3 * dp(14)),
                poseX + dp(14) + dp(ICON_DP), baseline, textPaint);
        String where = strings.fileLineAt(
                target.file() == null ? "" : target.file(), target.line());
        float whereW = detailPaint.measureText(where);
        canvas.drawText(ellipsizeTail(where, detailPaint, cardW - dp(28)),
                poseX + cardW - dp(14) - whereW, baseline, detailPaint);
        // L'EXTRAIT de la ligne (le code de la cible se lit dans la
        // carte — B7 : la rangée montre le code, pas un numéro seul)
        String excerpt = target.excerpt() == null
                ? "" : target.excerpt().trim();
        if (!excerpt.isEmpty()) {
            String squeezed = excerpt.length() > 60
                    ? excerpt.substring(0, 57) + "…" : excerpt;
            canvas.drawText(squeezed, poseX + dp(14) + dp(ICON_DP),
                    top + rowH * 0.92f, detailPaint);
        }
    }

    /** Le pied de carte : le compte des cibles. */
    private void drawFooter(Canvas canvas) {
        String count = strings.targetsCount(targets.size());
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
        visibleRows = fitRows(MAX_VISIBLE_ROWS, targets.size(),
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
        for (Target target : targets) {
            String where = strings.fileLineAt(
                    target.file() == null ? "" : target.file(), target.line());
            widest = Math.max(widest,
                    textPaint.measureText(target.name() == null
                            ? "" : target.name())
                            + detailPaint.measureText(where));
        }
        return widest + dp(ICON_DP) + 3 * dp(14);
    }

    /** Le compte des rangées vivantes. */
    @Override
    protected int rowCount() {
        return targets.size();
    }

    /** La hauteur d'une rangée : le style la porte, l'icône y respire. */
    @Override
    protected float rowHeight() {
        return dp(style.rowHeightDp() - 6);
    }

    /** Le libellé d'une rangée (le service qui parle la lit). */
    @Override
    protected String virtualRowLabelAt(int at) {
        if (at < 0 || at >= targets.size()) {
            return "";
        }
        Target target = targets.get(at);
        return (target.name() == null ? "" : target.name()) + ", "
                + strings.fileLineAt(
                        target.file() == null ? "" : target.file(),
                        target.line());
    }

    /** L'action d'une rangée (le service qui parle la joue au doigt du lecteur). */
    @Override
    protected boolean virtualRowPlay(int at) {
        if (at < 0 || at >= targets.size()) {
            return false;
        }
        Target chosen = targets.get(at);
        close();
        if (chosen != null && chosen.go() != null && editor != null) {
            editor.post(chosen.go());
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
