package jo.pigma.editor.popups;

import jo.pigma.editor.EditorView;

import android.graphics.Canvas;
import android.graphics.Paint;

import java.util.List;

/**
 * LA CARTE DES FICHIERS RÉCENTS (les fichiers récents du rapport des popups — le «
 * Ctrl+E » de l'atelier porté au tactile) : revenir à un document ou
 * à un emplacement d'avant. Les FAMILLES s'y groupent — les FICHIERS
 * d'abord (l'étiquette coiffe, chaque fichier vit en rangée : son
 * nom, son ancienneté à droite), les EMPLACEMENTS ensuite (le fichier
 * · la ligne, l'EXTRAIT dessous — la demande du rapport —, l'ancienneté
 * à droite). Le choix d'une rangée y revient (le document se rouvre,
 * la position y retrouve le curseur).
 *
 * <p>La carte vit de {@link ListCard} (R-F) : la pose au caret
 * (dessous si elle tient, dessus sinon), les murs, la croix, la
 * fenêtre de rangées et sa barre de défilement, le pied qui compte.</p>
 *
 * <p>La carte ne prend jamais le focus ; la rangée referme la carte
 * AVANT de revenir (le geste ne se joue pas deux fois).</p>
 */
public final class RecentCard extends ListCard {

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
     * Une rangée unifiée : un fichier, un emplacement, ou l'étiquette
     * de sa famille (le harnais la lit).
     *
     * @param title  le titre de la rangée (le nom, ou fichier · ligne)
     * @param detail l'extrait de l'emplacement (null : un fichier)
     * @param where  l'ancienneté à droite (null : l'étiquette)
     * @param go     le retour au choix (null : l'étiquette)
     * @param group  la rangée est-elle une étiquette de famille ?
     */
    public record Row(String title, String detail, String where,
            Runnable go, boolean group) {
    }

    /** Les rangées vivantes (vide : la carte se range). */
    private List<Row> rows = List.of();

    /** La rangée armée par le DOWN (-1 : aucune — l'UP la joue). */
    private int armedRow = -1;

    /** La vue porteuse (le retour s'y poste). */
    private EditorView editor;

    private final Paint cardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint groupPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint detailPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint agePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint footerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rulePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glyphPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint iconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint scrollbarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    /** Le dessinateur d'icônes partagé (les glyphes de la complétion). */
    private final CompletionCard iconPainter = new CompletionCard(1.0f);

    /**
     * Une carte neuve, ses pinceaux propres au style par défaut.
     *
     * @param density la densité de l'écran (le dp en pixels)
     */
    public RecentCard(float density) {
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
        groupPaint.setColor(style.accentArgb());
        groupPaint.setFakeBoldText(true);
        groupPaint.setTextSize((style.secondarySizeSp() - 1.0f) * density);
        textPaint.setColor(style.textArgb());
        textPaint.setTextSize((style.textSizeSp() - 3.0f) * density);
        detailPaint.setColor(style.hintArgb());
        detailPaint.setTextSize((style.secondarySizeSp() - 2.0f) * density);
        agePaint.setColor(style.hintArgb());
        agePaint.setTextSize((style.secondarySizeSp() - 2.0f) * density);
        footerPaint.setColor(style.secondaryArgb());
        footerPaint.setTextSize((style.secondarySizeSp() - 2.0f) * density);
        rulePaint.setColor(style.borderArgb());
        glyphPaint.setColor(style.accentArgb());
        glyphPaint.setStyle(Paint.Style.STROKE);
        glyphPaint.setStrokeWidth(1.5f * density);
        iconPaint.setColor(style.accentArgb());
        iconPaint.setStyle(Paint.Style.STROKE);
        iconPaint.setStrokeWidth(1.2f * density);
        scrollbarPaint.setColor((style.borderArgb() & 0x00FFFFFF) | 0x66000000);
    }

    /**
     * Branche la vue porteuse : le retour d'une rangée s'y poste
     * (hors du geste — la carte vit dans la vue).
     *
     * @param view la vue, jamais null
     */
    public void bind(EditorView view) {
        editor = view;
    }

    /**
     * Ouvre la carte sur la mémoire des récents — la mémoire vide
     * range la carte. Les familles s'y GROUPENT : les fichiers
     * d'abord, les emplacements ensuite, l'étiquette coiffe chacune.
     *
     * @param memory la mémoire des récents, jamais null
     * @param now    l'heure du moment (ms — l'ancienneté s'y lit)
     */
    public void open(RecentFiles memory, long now) {
        List<Row> built = new java.util.ArrayList<>();
        if (!memory.files().isEmpty()) {
            built.add(new Row("Fichiers", null, null, null, true));
            for (RecentFiles.RecentFile file : memory.files()) {
                built.add(new Row(file.name(), null,
                        RecentFiles.ageOf(file.when(), now), file.open(),
                        false));
            }
        }
        if (!memory.locations().isEmpty()) {
            built.add(new Row("Emplacements", null, null, null, true));
            for (RecentFiles.RecentLocation place : memory.locations()) {
                built.add(new Row(place.file() + " · ligne "
                        + (place.line() + 1), place.excerpt(),
                        RecentFiles.ageOf(place.when(), now), place.go(),
                        false));
            }
        }
        rows = built;
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

    /** Les rangées vivantes (le harnais les lit). */
    public List<Row> rows() {
        return rows;
    }

    /**
     * Le doigt tombé sur la carte lui appartient : la croix la range,
     * la rangée s'ARME au DOWN (l'UP la joue — l'étiquette d'une
     * famille n'arme rien) ; le reste consomme sans agir. Hors de la
     * carte : faux — le geste poursuit sa vie vers l'éditeur.
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
        int at = rowAt(y, poseY + dp(HEADER_DP));
        armedRow = at >= 0 && !rows.get(at).group() ? at : -1;
        return true;
    }

    /**
     * Le relâchement joue la rangée armée (le retour s'y exécute, la
     * carte se range APRÈS — le geste ne se rejoue pas au dessin).
     */
    public boolean onRelease() {
        if (armedRow < 0) {
            return false;
        }
        Row chosen = rows.get(armedRow);
        armedRow = -1;
        close();
        if (chosen.go() != null && editor != null) {
            // le retour se JOUE HORS du geste (posté au tour suivant) :
            // le geste se clôt d'abord
            editor.post(chosen.go());
        }
        return true;
    }

    /**
     * Dessine la carte : la pose se recalcule d'abord — sous le
     * caret (au-dessus sans la place) —, puis l'en-tête (le titre, la
     * croix), le filet, les rangées de la fenêtre (l'étiquette de
     * famille en encre d'accent, sinon l'icône, le titre, l'extrait
     * dessous, l'ancienneté à droite), la barre de défilement si la
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
        canvas.drawText("Récents", poseX + dp(14), headerBaseline, titlePaint);
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

    /** Une rangée : l'étiquette de famille, ou l'icône, le titre, l'extrait, l'âge. */
    private void drawRow(Canvas canvas, Row row, float top) {
        float rowH = rowHeight();
        if (row.group()) {
            canvas.drawText(row.title(), poseX + dp(14),
                    top + rowH * 0.72f, groupPaint);
            return;
        }
        float cy = top + rowH / 2.0f;
        float baseline = top + rowH * 0.62f;
        // l'icône de la rangée : le carré du document (le trait à
        // l'accent — la feuille avec son pli)
        drawDocumentIcon(canvas, poseX + dp(14) + dp(ICON_DP) / 2.0f, cy);
        String title = row.title() == null ? "" : row.title();
        canvas.drawText(ellipsizeTail(title, textPaint,
                cardW - dp(ICON_DP) - 3 * dp(14) - dp(72)),
                poseX + dp(14) + dp(ICON_DP), baseline, textPaint);
        // L'EXTRAIT de l'emplacement (le code s'y lit — la demande du
        // rapport : l'emplacement récent porte son extrait)
        String detail = row.detail() == null ? "" : row.detail().trim();
        if (!detail.isEmpty()) {
            String squeezed = detail.length() > 60
                    ? detail.substring(0, 57) + "…" : detail;
            canvas.drawText(squeezed, poseX + dp(14) + dp(ICON_DP),
                    top + rowH * 0.90f, detailPaint);
        }
        String age = row.where() == null ? "" : row.where();
        if (!age.isEmpty()) {
            float ageW = agePaint.measureText(age);
            canvas.drawText(age, poseX + cardW - dp(14) - ageW, baseline,
                    agePaint);
        }
    }

    /** L'icône du document : le carré au coin plié (la feuille). */
    private void drawDocumentIcon(Canvas canvas, float cx, float cy) {
        float side = dp(ICON_DP) * 0.52f;
        float fold = side * 0.38f;
        canvas.drawLine(cx - side / 2.0f, cy - side / 2.0f,
                cx + side / 2.0f - fold, cy - side / 2.0f, iconPaint);
        canvas.drawLine(cx + side / 2.0f - fold, cy - side / 2.0f,
                cx + side / 2.0f, cy - side / 2.0f + fold, iconPaint);
        canvas.drawLine(cx + side / 2.0f, cy - side / 2.0f + fold,
                cx + side / 2.0f, cy + side / 2.0f, iconPaint);
        canvas.drawLine(cx + side / 2.0f, cy + side / 2.0f,
                cx - side / 2.0f, cy + side / 2.0f, iconPaint);
        canvas.drawLine(cx - side / 2.0f, cy + side / 2.0f,
                cx - side / 2.0f, cy - side / 2.0f, iconPaint);
        // le pli du coin
        canvas.drawLine(cx + side / 2.0f - fold, cy - side / 2.0f,
                cx + side / 2.0f - fold, cy - side / 2.0f + fold, iconPaint);
        canvas.drawLine(cx + side / 2.0f - fold, cy - side / 2.0f + fold,
                cx + side / 2.0f, cy - side / 2.0f + fold, iconPaint);
    }

    /** Le pied de carte : le compte des deux familles. */
    private void drawFooter(Canvas canvas) {
        int files = 0;
        int places = 0;
        boolean inFiles = false;
        for (Row row : rows) {
            if (row.group()) {
                inFiles = "Fichiers".equals(row.title());
                continue;
            }
            if (inFiles) {
                files++;
            } else {
                places++;
            }
        }
        String count = files + (files > 1 ? " fichiers" : " fichier")
                + " · " + places + (places > 1 ? " emplacements"
                        : " emplacement");
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
        visibleRows = fitRows(MAX_VISIBLE_ROWS, rows.size(),
                dp(HEADER_DP) + dp(FOOTER_DP), rowHeight(),
                metrics.viewHeight(), margin);
        cardH = dp(HEADER_DP) + visibleRows * rowHeight() + dp(FOOTER_DP);
        placeY(anchorTop, anchorBottom, margin, dp(GAP_DP), cardH,
                metrics.viewHeight());
        placeX(margin, metrics);
    }

    /** La largeur qu'il faut pour la plus large rangée, respirée. */
    private float measuredWidth() {
        float widest = titlePaint.measureText("Récents");
        for (Row row : rows) {
            widest = Math.max(widest,
                    textPaint.measureText(row.title() == null ? ""
                            : row.title())
                            + agePaint.measureText(row.where() == null ? ""
                                    : row.where()));
        }
        return widest + dp(ICON_DP) + 3 * dp(14);
    }

    /** Le compte des rangées vivantes. */
    @Override
    protected int rowCount() {
        return rows.size();
    }

    /** La hauteur d'une rangée : le style la porte, l'icône y respire. */
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
