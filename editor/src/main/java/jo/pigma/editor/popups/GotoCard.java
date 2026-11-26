package jo.pigma.editor.popups;

import jo.pigma.editor.EditorView;

import android.graphics.Canvas;
import android.graphics.Paint;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * LA CARTE DE L'ALLER AU SYMBOLE / FICHIER (l'aller au symbole du rapport des popups
 * — le « Ctrl+N » de l'atelier porté au tactile) : atteindre un
 * élément sans le chercher dans une arborescence. LE FILTRE s'y vit
 * à la frappe (la carte reçoit le clavier — elle ne prend jamais le
 * focus, le tampon ne bouge pas) : les lettres tapées resserrent la
 * liste, les rangées vivent des DEUX sources — les symboles de
 * l'ESPACE DE TRAVAIL (le protocole {@code workspace/symbol}, la
 * façade y passe) et les FICHIERS de l'hôte (l'index posé par le
 * porteur — chaque fichier sa rangée, le choix part à son ouverture).
 *
 * <p>La carte vit de {@link ListCard} (R-F) et du contrat clavier
 * ({@link KeyboardCard} — la vue lui rend les verdicts des touches) :
 * la pose au caret, les murs, la croix, la fenêtre de rangées et sa
 * barre de défilement, le pied qui compte.</p>
 */
public final class GotoCard extends ListCard implements KeyboardCard {

    /** La hauteur de l'en-tête (dp). */
    private static final int HEADER_DP = 46;

    /** La hauteur du pied de carte (dp). */
    private static final int FOOTER_DP = 30;

    /** La marge aux murs de la vue (dp). */
    private static final int MARGIN_DP = 8;

    /** L'écart au caret (dp). */
    private static final int GAP_DP = 4;

    /** Le plafond de rangées visibles (la carte défile au-delà). */
    private static final int MAX_VISIBLE_ROWS = 10;

    /** La largeur de la colonne d'icône (dp). */
    private static final int ICON_DP = 26;

    /**
     * Une rangée de l'aller : le nom (le symbole ou le fichier), sa
     * nature (une des constantes KIND_ de la complétion — les glyphes
     * partagés ; le fichier y vit au module), sa place lue à droite,
     * le saut à son ouverture.
     *
     * @param name  le nom de la rangée
     * @param kind  sa nature (le fichier : KIND_MODULE)
     * @param where sa place lue à droite (le fichier · la ligne, ou le
     *              nom du fichier)
     * @param go    le saut (le choix y navigue)
     */
    public record GotoRow(String name, int kind, String where, Runnable go) {
    }

    /** Les rangées vivantes, la liste ENTIÈRE (le filtre s'y coupe). */
    private List<GotoRow> rows = List.of();

    /** Les rangées FILTRÉES (la fenêtre y vit). */
    private List<GotoRow> filtered = List.of();

    /** Le filtre tapé (vide : toute la liste). */
    private String filter = "";

    /** La rangée mise en évidence (le filtre y descend, Entrée y va). */
    private int highlighted = -1;

    /** La rangée armée par le DOWN (-1 : aucune — l'UP la joue). */
    private int armedRow = -1;

    /** Le dessinateur d'icônes partagé (les glyphes de la complétion). */
    private final CompletionCard iconPainter = new CompletionCard(1.0f);

    /** La vue porteuse (la navigation s'y poste). */
    private EditorView editor;

    private final Paint cardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint filterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint namePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint wherePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint footerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rulePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glyphPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint markPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint scrollbarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    /**
     * Une carte neuve, ses pinceaux propres au style par défaut.
     *
     * @param density la densité de l'écran (le dp en pixels)
     */
    public GotoCard(float density) {
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
        filterPaint.setColor(style.hintArgb());
        filterPaint.setTextSize((style.secondarySizeSp() - 1.0f) * density);
        namePaint.setColor(style.textArgb());
        namePaint.setTextSize((style.textSizeSp() - 3.0f) * density);
        wherePaint.setColor(style.hintArgb());
        wherePaint.setTextSize((style.secondarySizeSp() - 2.0f) * density);
        footerPaint.setColor(style.secondaryArgb());
        footerPaint.setTextSize((style.secondarySizeSp() - 2.0f) * density);
        rulePaint.setColor(style.borderArgb());
        glyphPaint.setColor(style.accentArgb());
        glyphPaint.setStyle(Paint.Style.STROKE);
        glyphPaint.setStrokeWidth(1.5f * density);
        markPaint.setColor((style.accentArgb() & 0x00FFFFFF) | 0x19000000);
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
     * Ouvre la carte sur des rangées — la liste vide range la carte.
     *
     * @param found les rangées (symboles et fichiers), jamais null
     */
    public void open(List<GotoRow> found) {
        rows = List.copyOf(found);
        filter = "";
        topRow = 0;
        highlighted = rows.isEmpty() ? -1 : 0;
        refilter();
        shown = !rows.isEmpty();
    }

    /** Ramasse les restes de la fermeture. */
    @Override
    protected void onClosed() {
        rows = List.of();
        filtered = List.of();
        filter = "";
        topRow = 0;
        highlighted = -1;
        armedRow = -1;
    }

    /** Les rangées vivantes, la liste ENTIÈRE (le harnais la lit). */
    public List<GotoRow> rows() {
        return rows;
    }

    /** Les rangées filtrées (le harnais les lit). */
    public List<GotoRow> filtered() {
        return filtered;
    }

    /** Le filtre tapé (le harnais le lit). */
    public String filter() {
        return filter;
    }

    /** Le texte tapé s'écrit au filtre. */
    @Override
    public boolean onText(CharSequence text) {
        if (!isShown() || text == null || text.length() == 0) {
            return false;
        }
        filter = filter + text;
        refilter();
        return true;
    }

    /** La composition du clavier remplace le filtre entier. */
    @Override
    public boolean onComposition(CharSequence text) {
        if (!isShown() || text == null) {
            return false;
        }
        filter = text.toString();
        refilter();
        return true;
    }

    /** Le retour arrière ôte la dernière lettre du filtre. */
    @Override
    public boolean onBackspace() {
        if (!isShown() || filter.isEmpty()) {
            return false;
        }
        filter = filter.substring(0, filter.length() - 1);
        refilter();
        return true;
    }

    /** La flèche descend la mise en évidence (le doigt y revient). */
    @Override
    public boolean onArrow(int step) {
        if (!isShown() || filtered.isEmpty()) {
            return false;
        }
        highlighted = highlighted < 0 ? (step > 0 ? 0 : filtered.size() - 1)
                : Math.min(filtered.size() - 1, Math.max(0, highlighted + step));
        keepHighlightedVisible();
        return true;
    }

    /** La validation VA à la rangée mise en évidence. */
    @Override
    public boolean onEnter() {
        if (!isShown() || highlighted < 0 || highlighted >= filtered.size()) {
            return false;
        }
        GotoRow chosen = filtered.get(highlighted);
        close();
        if (chosen != null && chosen.go() != null) {
            chosen.go().run();
        }
        return true;
    }

    /** Le filtre se recoupe : la fenêtre repart au sommet, la mise en évidence revient. */
    private void refilter() {
        if (filter.isEmpty()) {
            filtered = rows;
        } else {
            String needle = filter.toLowerCase(Locale.ROOT);
            List<GotoRow> kept = new ArrayList<>();
            for (GotoRow row : rows) {
                String haystack = (row.name() == null ? "" : row.name())
                        + " " + (row.where() == null ? "" : row.where());
                if (haystack.toLowerCase(Locale.ROOT).contains(needle)) {
                    kept.add(row);
                }
            }
            filtered = kept;
        }
        topRow = 0;
        highlighted = filtered.isEmpty() ? -1 : 0;
    }

    /** La rangée mise en évidence revient à la fenêtre. */
    private void keepHighlightedVisible() {
        if (highlighted < topRow) {
            topRow = highlighted;
        } else if (highlighted >= topRow + visibleRows) {
            topRow = highlighted - visibleRows + 1;
        }
    }

    /**
     * Le doigt tombé sur la carte lui appartient : la croix la
     * range, la rangée s'ARME au DOWN (l'UP la joue) ; le reste
     * consomme sans agir. Hors de la carte : faux — le geste poursuit
     * sa vie vers l'éditeur.
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
        if (armedRow >= 0) {
            highlighted = armedRow;
        }
        return true;
    }

    /**
     * Le relâchement joue la rangée armée (la navigation s'y exécute,
     * la carte se range APRÈS).
     */
    public boolean onRelease() {
        if (armedRow < 0) {
            return false;
        }
        GotoRow chosen = filtered.get(armedRow);
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
     * caret (au-dessus sans la place) —, puis l'en-tête (le titre, le
     * FILTRE tapé, la croix), le filet, les rangées de la fenêtre
     * (l'icône de nature, le nom, la place à droite, la rangée mise
     * en évidence teintée), la barre de défilement si la fenêtre
     * cache des rangées, le pied qui compte.
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
        canvas.drawText(strings.gotoTitle(), poseX + dp(14), headerBaseline,
                titlePaint);
        // LE FILTRE TAPÉ vit en tête (les lettres resserrent la
        // liste — l'invite « … » quand rien n'y vit)
        String typed = filter.isEmpty() ? "…" : filter;
        canvas.drawText(ellipsizeTail(typed, filterPaint, cardW - dp(170)),
                poseX + dp(96), headerBaseline, filterPaint);
        drawCross(canvas, poseX + cardW - dp(24), poseY + dp(HEADER_DP) / 2.0f,
                glyphPaint);
        canvas.drawLine(poseX + dp(12), poseY + dp(HEADER_DP) - dp(2),
                poseX + cardW - dp(12), poseY + dp(HEADER_DP) - dp(2),
                rulePaint);
        float cursor = poseY + dp(HEADER_DP);
        for (int at = topRow; at < topRow + visibleRows
                && at < filtered.size(); at++) {
            drawRow(canvas, filtered.get(at), at == highlighted, cursor);
            cursor += rowHeight();
        }
        drawScrollbar(canvas, scrollbarPaint, poseY + dp(HEADER_DP), cursor,
                poseX + cardW - dp(4));
        drawFooter(canvas);
    }

    /** Une rangée : l'icône de nature, le nom, la place à droite. */
    private void drawRow(Canvas canvas, GotoRow row, boolean emphasized,
            float top) {
        float rowH = rowHeight();
        float cy = top + rowH / 2.0f;
        float baseline = top + rowH * 0.66f;
        // la rangée mise en évidence se TEINTE (la flèche y descend,
        // le doigt s'y pose — le choix s'y lit avant le saut)
        if (emphasized) {
            canvas.drawRect(poseX + dp(4), top,
                    poseX + cardW - dp(4), top + rowH, markPaint);
        }
        // l'icône de nature — les GLYPHES PARTAGÉS de la complétion
        iconPainter.drawKindIcon(canvas, row.kind(), poseX + dp(14), cy);
        String name = row.name() == null ? "" : row.name();
        canvas.drawText(ellipsizeTail(name, namePaint,
                cardW - dp(ICON_DP) - 2 * dp(14) - dp(96)),
                poseX + dp(14) + dp(ICON_DP) + dp(6), baseline, namePaint);
        String where = row.where() == null ? "" : row.where();
        if (!where.isEmpty()) {
            float whereW = wherePaint.measureText(where);
            canvas.drawText(ellipsizeTail(where, wherePaint, cardW - dp(28)),
                    poseX + cardW - dp(14) - whereW, baseline, wherePaint);
        }
    }

    /** Le pied de carte : le compte des rangées. */
    private void drawFooter(Canvas canvas) {
        String count = strings.resultsCount(filtered.size());
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
        visibleRows = fitRows(MAX_VISIBLE_ROWS, filtered.size(),
                dp(HEADER_DP) + dp(FOOTER_DP), rowHeight(),
                metrics.viewHeight(), margin);
        cardH = dp(HEADER_DP) + visibleRows * rowHeight() + dp(FOOTER_DP);
        placeY(anchorTop, anchorBottom, margin, dp(GAP_DP), cardH,
                metrics.viewHeight());
        placeX(margin, metrics);
    }

    /** La largeur qu'il faut pour la plus large rangée, respirée. */
    private float measuredWidth() {
        float widest = titlePaint.measureText(strings.gotoTitle());
        for (GotoRow row : rows) {
            widest = Math.max(widest, namePaint.measureText(
                    row.name() == null ? "" : row.name())
                    + wherePaint.measureText(
                            row.where() == null ? "" : row.where()));
        }
        return widest + dp(ICON_DP) + 3 * dp(14) + dp(96);
    }

    /** Le compte des rangées vivantes. */
    @Override
    protected int rowCount() {
        return filtered.size();
    }

    /** La hauteur d'une rangée : le style la porte, l'icône y respire. */
    @Override
    protected float rowHeight() {
        return dp(style.rowHeightDp() - 6);
    }

    /** Le libellé d'une rangée (le service qui parle la lit). */
    @Override
    protected String virtualRowLabelAt(int at) {
        if (at < 0 || at >= filtered.size()) {
            return "";
        }
        GotoRow row = filtered.get(at);
        return (row.name() == null ? "" : row.name())
                + (row.where() == null || row.where().isEmpty() ? ""
                        : ", " + row.where());
    }

    /** L'action d'une rangée (le service qui parle la joue au doigt du lecteur). */
    @Override
    protected boolean virtualRowPlay(int at) {
        if (at < 0 || at >= filtered.size()) {
            return false;
        }
        GotoRow chosen = filtered.get(at);
        close();
        if (chosen != null && chosen.go() != null && editor != null) {
            editor.post(chosen.go());
        }
        return true;
    }

    /**
     * L'ordonnée du centre d'une rangée (le harnais y vise le
     * doigt), -1 hors fenêtre.
     *
     * @param index l'indice de la rangée
     * @return le centre vertical (px, vue), -1 si hors fenêtre
     */
    @Override
    public float rowCenterY(int index) {
        if (index < topRow || index >= topRow + visibleRows) {
            return -1.0f;
        }
        return poseY + dp(HEADER_DP) + (index - topRow) * rowHeight()
                + rowHeight() / 2.0f;
    }
}
