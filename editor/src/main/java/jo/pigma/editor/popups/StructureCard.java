package jo.pigma.editor.popups;

import jo.pigma.editor.EditorView;

import android.graphics.Canvas;
import android.graphics.Paint;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * La STRUCTURE DU FICHIER (la structure du fichier du rapport des popups — la table des
 * matières de l'atelier) : chaque symbole du document y vit en
 * rangée — l'icône de sa nature (la CLASSE au carré, la méthode au
 * rond, le champ au point — les glyphes PARTAGÉS de la complétion),
 * son nom, sa ligne à droite — le SYMBOLE COURANT y se détache
 * (l'encre pleine et le gras : la structure se lit depuis le
 * curseur). Le FILTRE s'y vit à la frappe : les lettres tapées ne
 * touchent pas le tampon, elles y resserrent la liste (la carte
 * reçoit le clavier — elle ne prend jamais le focus).
 *
 * <p>La carte vit de {@link ListCard} (R-F) et du contrat clavier
 * ({@link KeyboardCard} — la vue lui rend les verdicts des
 * touches) : la pose au caret, les murs, la croix, la fenêtre de
 * rangées et sa barre de défilement, le pied qui compte.</p>
 *
 * <p>Elle REMPLACE le détournement d'avant (R-D du rapport : la
 * table des symboles vivait dans la carte d'actions rapides —
 * « nom · ligne N » sans icône ni filtre, mêlée aux actions).</p>
 */
public final class StructureCard extends ListCard implements KeyboardCard {

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

    /**
     * Un symbole du document : son nom, sa nature (une des
     * constantes KIND_ de la complétion — les glyphes partagés), sa
     * ligne — le choix y navigue.
     *
     * @param name  le nom du symbole
     * @param kind  sa nature
     * @param line  l'indice de la ligne
     * @param go    le saut au symbole
     */
    public record Symbol(String name, int kind, int line, Runnable go) {
    }

    /** Les symboles du document, la liste ENTIÈRE (le filtre s'y coupe). */
    private List<Symbol> symbols = List.of();

    /** Les symboles FILTRÉS (la fenêtre y vit). */
    private List<Symbol> filtered = List.of();

    /** Le filtre tapé (vide : tout le document). */
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
    private final Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint filterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint namePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint currentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint detailPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint footerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rulePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glyphPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint markPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint scrollbarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    /**
     * Une carte neuve, ses pinceaux propres au style par défaut.
     *
     * @param density la densité de l'écran (le dp en pixels)
     */
    public StructureCard(float density) {
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
        currentPaint.setColor(style.textArgb());
        currentPaint.setFakeBoldText(true);
        currentPaint.setTextSize((style.textSizeSp() - 3.0f) * density);
        detailPaint.setColor(style.hintArgb());
        detailPaint.setTextSize((style.secondarySizeSp() - 2.0f) * density);
        footerPaint.setColor(style.secondaryArgb());
        footerPaint.setTextSize((style.secondarySizeSp() - 2.0f) * density);
        rulePaint.setColor(style.borderArgb());
        glyphPaint.setColor(style.accentArgb());
        glyphPaint.setStyle(Paint.Style.STROKE);
        glyphPaint.setStrokeWidth(1.5f * density);
        markPaint.setColor((style.accentArgb() & 0x00FFFFFF) | 0x33000000);
        markPaint.setStyle(Paint.Style.FILL);
        scrollbarPaint.setColor((style.borderArgb() & 0x00FFFFFF) | 0x66000000);
        iconPainter.setStyle(style);
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
     * Ouvre la carte sur les symboles du document — la liste vide
     * range la carte ; le filtre repart vide.
     *
     * @param found les symboles (les natures à l'échelle de la
     *              complétion), jamais null
     */
    public void open(List<Symbol> found) {
        symbols = List.copyOf(found);
        filter = "";
        filtered = symbols;
        topRow = 0;
        highlighted = -1;
        shown = !symbols.isEmpty();
    }

    /** Ramasse les restes de la fermeture. */
    @Override
    protected void onClosed() {
        symbols = List.of();
        filtered = List.of();
        filter = "";
        topRow = 0;
        highlighted = -1;
        armedRow = -1;
    }

    /** Les symboles vivants du document (le harnais les lit). */
    public List<Symbol> symbols() {
        return symbols;
    }

    /** Les symboles FILTRÉS (le harnais les lit). */
    public List<Symbol> filtered() {
        return filtered;
    }

    /** Le filtre tapé (le harnais le lit). */
    public String filter() {
        return filter;
    }

    /**
     * LE FILTRE se resserre : le texte tapé s'y pose (la casse ne
     * compte pas, le nom qui le CONTIENT demeure) — le tampon ne
     * bouge pas (la carte a pris la frappe).
     */
    @Override
    public boolean onText(CharSequence text) {
        if (!isShown() || text == null || text.length() == 0) {
            return false;
        }
        filter += text.toString();
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

    /** La validation VA au symbole mis en évidence. */
    @Override
    public boolean onEnter() {
        if (!isShown() || highlighted < 0 || highlighted >= filtered.size()) {
            return false;
        }
        Symbol chosen = filtered.get(highlighted);
        close();
        if (chosen != null && chosen.go() != null) {
            chosen.go().run();
        }
        return true;
    }

    /** Le filtre se recoupe : la fenêtre repart au sommet, la mise en évidence revient. */
    private void refilter() {
        if (filter.isEmpty()) {
            filtered = symbols;
        } else {
            String needle = filter.toLowerCase(Locale.ROOT);
            List<Symbol> kept = new ArrayList<>();
            for (Symbol symbol : symbols) {
                if (symbol.name() != null
                        && symbol.name().toLowerCase(Locale.ROOT).contains(needle)) {
                    kept.add(symbol);
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
     * range, la rangée d'un symbole s'ARME au DOWN (l'UP la joue) ;
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
        if (armedRow >= 0) {
            highlighted = armedRow;
        }
        return true;
    }

    /**
     * Le relâchement joue la rangée armée (le symbole y va, la carte
     * se range APRÈS).
     */
    public boolean onRelease() {
        if (armedRow < 0) {
            return false;
        }
        Symbol chosen = filtered.get(armedRow);
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
     * le FILTRE tapé, la croix), le filet, les rangées de la
     * fenêtre (l'icône de nature, le nom — le symbole COURANT au
     * gras —, la ligne à droite, la rangée mise en évidence teintée),
     * la barre de défilement si la fenêtre cache des rangées, le
     * pied qui compte.
     *
     * @param canvas  le canevas de la vue
     * @param anchorTop le bord haut de la ligne du caret (px, vue)
     * @param anchorBottom le bord bas de la ligne du caret (px, vue)
     * @param currentLine la ligne du curseur (le symbole courant s'y
     *                    détache — l'englobante la plus proche)
     * @param metrics la géométrie courante de la vue
     */
    public void draw(Canvas canvas, float anchorTop, float anchorBottom,
            int currentLine, PopupMetrics metrics) {
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
        canvas.drawText(strings.structure(), poseX + dp(14), headerBaseline,
                titlePaint);
        // LE FILTRE TAPÉ vit en tête (les lettres resserrent la
        // liste — l'invite « … » quand rien n'y vit)
        String typed = filter.isEmpty() ? "…" : filter;
        canvas.drawText(ellipsizeTail(typed, filterPaint, cardW - dp(220)),
                poseX + dp(120), headerBaseline, filterPaint);
        drawCross(canvas, poseX + cardW - dp(24), poseY + dp(HEADER_DP) / 2.0f,
                glyphPaint);
        canvas.drawLine(poseX + dp(12), poseY + dp(HEADER_DP) - dp(2),
                poseX + cardW - dp(12), poseY + dp(HEADER_DP) - dp(2),
                rulePaint);
        float cursor = poseY + dp(HEADER_DP);
        int currentAt = currentIndexFor(currentLine);
        for (int at = topRow; at < topRow + visibleRows
                && at < filtered.size(); at++) {
            drawRow(canvas, filtered.get(at), at == highlighted,
                    at == currentAt, cursor);
            cursor += rowHeight();
        }
        drawScrollbar(canvas, scrollbarPaint, poseY + dp(HEADER_DP), cursor,
                poseX + cardW - dp(4));
        drawFooter(canvas);
    }

    /**
     * L'indice du symbole COURANT — l'englobante la plus proche au-
     * dessus du curseur (la structure se lit depuis lui : sa rangée
     * se détache au gras).
     *
     * @param line la ligne du curseur
     * @return l'indice dans la liste filtrée, -1 avant le premier
     */
    private int currentIndexFor(int line) {
        int found = -1;
        for (int at = 0; at < filtered.size(); at++) {
            if (filtered.get(at).line() <= line) {
                found = at;
            } else {
                break;
            }
        }
        return found;
    }

    /** Une rangée : l'icône de nature, le nom (le courant au gras), la ligne. */
    private void drawRow(Canvas canvas, Symbol symbol, boolean emphasized,
            boolean current, float top) {
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
        iconPainter.drawKindIcon(canvas, symbol.kind(),
                poseX + dp(14), cy);
        Paint paint = current ? currentPaint : namePaint;
        String name = symbol.name() == null ? "" : symbol.name();
        canvas.drawText(ellipsizeTail(name, paint, cardW - dp(ICON_DP) - 2 * dp(14)),
                poseX + dp(14) + dp(ICON_DP) + dp(6), baseline, paint);
        String where = "ligne " + (symbol.line() + 1);
        float whereW = detailPaint.measureText(where);
        canvas.drawText(where, poseX + cardW - dp(14) - whereW, baseline,
                detailPaint);
    }

    /** La largeur de la colonne d'icône (dp). */
    private static final int ICON_DP = 26;

    /** Le pied de carte : le compte des symboles (le filtre se dit). */
    private void drawFooter(Canvas canvas) {
        String count = strings.symbolsCount(filtered.size());
        if (!filter.isEmpty() && filtered.size() != symbols.size()) {
            count = strings.filteredCount(filtered.size(), symbols.size());
        }
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
        cardW = clampWidth(measuredWidth(), style.minCardWidthDp() - 60,
                style.maxCardWidthDp() - 60, margin, metrics);
        visibleRows = fitRows(MAX_VISIBLE_ROWS, filtered.size(),
                dp(HEADER_DP) + dp(FOOTER_DP), rowHeight(),
                metrics.viewHeight(), margin);
        cardH = dp(HEADER_DP) + visibleRows * rowHeight() + dp(FOOTER_DP);
        placeY(anchorTop, anchorBottom, margin, dp(GAP_DP), cardH,
                metrics.viewHeight());
        placeX(margin, metrics);
    }

    /** La largeur qu'il faut pour le plus large nom, respiré. */
    private float measuredWidth() {
        float widest = titlePaint.measureText("Structure");
        for (Symbol symbol : filtered) {
            widest = Math.max(widest,
                    currentPaint.measureText(symbol.name() == null
                            ? "" : symbol.name()));
        }
        return widest + dp(ICON_DP) + 3 * dp(14);
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
        Symbol symbol = filtered.get(at);
        return (symbol.name() == null ? "" : symbol.name()) + ", "
                + strings.lineAt(symbol.line());
    }

    /** L'action d'une rangée (le service qui parle la joue au doigt du lecteur). */
    @Override
    protected boolean virtualRowPlay(int at) {
        if (at < 0 || at >= filtered.size()) {
            return false;
        }
        Symbol chosen = filtered.get(at);
        close();
        if (chosen != null && chosen.go() != null) {
            chosen.go().run();
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
