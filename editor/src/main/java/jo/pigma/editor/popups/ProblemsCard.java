package jo.pigma.editor.popups;

import jo.pigma.editor.EditorView;

import android.graphics.Canvas;
import android.graphics.Paint;

import java.util.ArrayList;
import java.util.List;

/**
 * La LISTE DES PROBLÈMES du document (la liste des problèmes du rapport des popups —
 * la fenêtre Problems de l'atelier) : chaque problème y vit en
 * rangée — l'icône de sévérité à gauche, le message replié à la
 * largeur, la ligne à droite — GROUPÉ (les erreurs d'abord, les
 * avertissements ensuite, les informations en fin), l'étiquette du
 * groupe coiffe sa famille. Le choix d'une rangée y fait sauter le
 * curseur (le doigt parcourt la liste, l'erreur s'y lit dans son
 * texte).
 *
 * <p>La carte vit de {@link ListCard} (R-F) : la pose au caret
 * (dessous si elle tient, dessus sinon), les murs, la croix, la
 * fenêtre de rangées et sa barre de défilement, le pied qui compte
 * les familles.</p>
 *
 * <p>La carte ne prend jamais le focus ; la rangée referme la carte
 * AVANT de naviguer (le geste ne se joue pas deux fois).</p>
 */
public final class ProblemsCard extends ListCard {

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
    private static final int ICON_DP = 22;

    /**
     * Un problème du document : sa sévérité (les constantes
     * SEVERITY_ de {@link DiagnosticCard}), son message, sa ligne —
     * le choix y navigue.
     *
     * @param severity la sévérité
     * @param message  le message, jamais vide
     * @param line     l'indice de la ligne
     * @param go       le saut à la ligne du problème
     */
    public record Problem(int severity, String message, int line,
            Runnable go) {
    }

    /** Une rangée : un problème, ou l'étiquette de son groupe. */
    private record Row(int severity, String message, int line, Runnable go,
            boolean group) {
    }

    /** Les rangées vivantes (vide : la carte se range). */
    private List<Row> rows = List.of();

    /** La rangée armée par le DOWN (-1 : aucune — l'UP la joue). */
    private int armedRow = -1;

    /** La vue porteuse (la navigation s'y poste). */
    private EditorView editor;

    private final Paint cardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint groupPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint detailPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint footerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rulePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glyphPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint scrollbarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    /** L'icône de sévérité au trait (le rond barré, le triangle). */
    private final SeverityIcon severityIcon = new SeverityIcon();

    /**
     * Une carte neuve, ses pinceaux propres au style par défaut.
     *
     * @param density la densité de l'écran (le dp en pixels)
     */
    public ProblemsCard(float density) {
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
        footerPaint.setColor(style.secondaryArgb());
        footerPaint.setTextSize((style.secondarySizeSp() - 2.0f) * density);
        rulePaint.setColor(style.borderArgb());
        glyphPaint.setColor(style.accentArgb());
        glyphPaint.setStyle(Paint.Style.STROKE);
        glyphPaint.setStrokeWidth(1.5f * density);
        scrollbarPaint.setColor((style.borderArgb() & 0x00FFFFFF) | 0x66000000);
        severityIcon.setStyle(style);
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
     * Ouvre la carte sur les problèmes du document — la liste vide
     * range la carte. Les familles s'y GROUPENT : les erreurs
     * d'abord, les avertissements ensuite, les informations en fin —
     * l'étiquette du groupe coiffe sa famille.
     *
     * @param found les problèmes, jamais null
     */
    public void open(List<Problem> found) {
        rows = grouped(found);
        topRow = 0;
        shown = !rows.isEmpty();
    }

    /** Les familles s'y groupent : la sévérité ordonne, l'étiquette coiffe. */
    private List<Row> grouped(List<Problem> found) {
        List<Problem> ordered = new ArrayList<>();
        for (int severity : new int[]{DiagnosticCard.SEVERITY_ERROR,
                DiagnosticCard.SEVERITY_WARNING, DiagnosticCard.SEVERITY_INFO}) {
            for (Problem problem : found) {
                if (problem != null && problem.severity() == severity
                        && problem.message() != null && !problem.message().isEmpty()) {
                    ordered.add(problem);
                }
            }
        }
        List<Row> built = new ArrayList<>();
        int previous = 0;
        for (Problem problem : ordered) {
            if (problem.severity() != previous) {
                built.add(new Row(problem.severity(), labelOf(problem.severity()),
                        -1, null, true));
                previous = problem.severity();
            }
            built.add(new Row(problem.severity(), problem.message(),
                    problem.line(), problem.go(), false));
        }
        return built;
    }

    /** L'étiquette d'une famille. */
    private String labelOf(int severity) {
        if (severity == DiagnosticCard.SEVERITY_WARNING) {
            return "Avertissements";
        }
        if (severity == DiagnosticCard.SEVERITY_INFO) {
            return "Informations";
        }
        return "Erreurs";
    }

    /** Ramasse les restes de la fermeture. */
    @Override
    protected void onClosed() {
        rows = List.of();
        topRow = 0;
        armedRow = -1;
    }

    /** Les problèmes vivants (le harnais les lit — étiquettes exclues). */
    public List<Problem> problems() {
        List<Problem> found = new ArrayList<>();
        for (Row row : rows) {
            if (!row.group()) {
                found.add(new Problem(row.severity(), row.message(),
                        row.line(), row.go()));
            }
        }
        return found;
    }

    /**
     * Le doigt tombé sur la carte lui appartient : la croix la
     * range, la rangée d'un problème s'ARME au DOWN (l'UP la joue —
     * l'étiquette d'un groupe n'arme rien) ; le reste consomme sans
     * agir. Hors de la carte : faux — le geste poursuit sa vie vers
     * l'éditeur.
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
     * Le relâchement joue la rangée armée (la navigation s'y exécute,
     * la carte se range APRÈS — le geste ne se rejoue pas au dessin).
     */
    public boolean onRelease() {
        if (armedRow < 0) {
            return false;
        }
        Row chosen = rows.get(armedRow);
        armedRow = -1;
        close();
        if (chosen.go() != null && editor != null) {
            // la navigation se JOUE HORS du geste (postée au tour
            // suivant) : le geste se clôt d'abord
            editor.post(chosen.go());
        }
        return true;
    }

    /**
     * Dessine la carte : la pose se recalcule d'abord — sous le
     * caret (au-dessus sans la place) —, puis l'en-tête (le titre,
     * la croix), le filet, les rangées de la fenêtre (l'étiquette du
     * groupe en encre d'accent, sinon l'icône de sévérité, le
     * message, la ligne à droite), la barre de défilement si la
     * fenêtre cache des rangées, le pied qui compte les familles.
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
        canvas.drawText("Problèmes", poseX + dp(14), headerBaseline,
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

    /** Une rangée : l'étiquette du groupe, ou l'icône, le message, la ligne. */
    private void drawRow(Canvas canvas, Row row, float top) {
        float rowH = rowHeight();
        if (row.group()) {
            canvas.drawText(row.message(), poseX + dp(14),
                    top + rowH * 0.72f, groupPaint);
            return;
        }
        float cy = top + rowH / 2.0f;
        float baseline = top + rowH * 0.66f;
        severityIcon.draw(canvas, row.severity(),
                poseX + dp(14) + dp(ICON_DP) / 2.0f, cy, dp(ICON_DP));
        String message = ellipsizeTail(row.message(), textPaint,
                cardW - dp(ICON_DP) - 3 * dp(14));
        canvas.drawText(message, poseX + dp(14) + dp(ICON_DP) + dp(8),
                baseline, textPaint);
        String where = "ligne " + (row.line() + 1);
        float whereW = detailPaint.measureText(where);
        canvas.drawText(where, poseX + cardW - dp(14) - whereW, baseline,
                detailPaint);
    }

    /** Le pied de carte : le compte par famille. */
    private void drawFooter(Canvas canvas) {
        int errors = 0;
        int warnings = 0;
        int infos = 0;
        for (Row row : rows) {
            if (row.group()) {
                continue;
            }
            if (row.severity() == DiagnosticCard.SEVERITY_ERROR) {
                errors++;
            } else if (row.severity() == DiagnosticCard.SEVERITY_WARNING) {
                warnings++;
            } else {
                infos++;
            }
        }
        StringBuilder count = new StringBuilder();
        if (errors > 0) {
            count.append(errors).append(" erreur(s)");
        }
        if (warnings > 0) {
            if (count.length() > 0) {
                count.append(" · ");
            }
            count.append(warnings).append(" avertissement(s)");
        }
        if (infos > 0) {
            if (count.length() > 0) {
                count.append(" · ");
            }
            count.append(infos).append(" info(s)");
        }
        if (count.length() == 0) {
            count.append("aucun problème");
        }
        float countW = footerPaint.measureText(count.toString());
        canvas.drawText(count.toString(), poseX + cardW - dp(14) - countW,
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

    /** La largeur qu'il faut pour le plus large message, respiré. */
    private float measuredWidth() {
        float widest = titlePaint.measureText("Problèmes");
        for (Row row : rows) {
            widest = Math.max(widest,
                    textPaint.measureText(row.message())
                            + detailPaint.measureText("ligne 999"));
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
