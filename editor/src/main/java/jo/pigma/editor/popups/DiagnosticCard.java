package jo.pigma.editor.popups;

import android.graphics.Canvas;
import android.graphics.Paint;

import java.util.ArrayList;
import java.util.List;

/**
 * La carte de diagnostic, DESSINÉE DANS LA VUE : les erreurs et les
 * avertissements d'une LIGNE — l'icône de sévérité (le rond rouge
 * barré de l'erreur, le triangle ambre de l'avertissement, le rond
 * bleu de l'information) à gauche, le message replié à la largeur à
 * droite, coiffés d'un titre (« Diagnostics · ligne N ») et comptés
 * au pied de carte. La carte se pose SOUS la ligne touchée (au-dessus
 * sans la place), bornée aux murs de la vue ; le fond translucide et
 * la taille viennent du style posé par l'hôte.
 *
 * <p>La carte ne prend jamais le focus ; le doigt sur une rangée la
 * referme (le message se lit), le reste de la carte consomme sans
 * agir ; hors d'elle le geste poursuit sa vie vers l'éditeur, qui y
 * range la carte (un nouvel appui la ferme).</p>
 */
public final class DiagnosticCard {

    /** La sévérité d'une erreur (le rond rouge barré). */
    public static final int SEVERITY_ERROR = 1;

    /** La sévérité d'un avertissement (le triangle ambre). */
    public static final int SEVERITY_WARNING = 2;

    /** La sévérité d'une information (le rond bleu). */
    public static final int SEVERITY_INFO = 3;

    /** Le plafond de rangées visibles (la fenêtre défile au-delà). */
    private static final int MAX_VISIBLE_ROWS = 8;

    /** La respiration intérieure de la carte (dp). */
    private static final int PAD_DP = 6;

    /** La hauteur du pied de carte (dp). */
    private static final int FOOTER_DP = 30;

    /** La marge aux murs de la vue (dp). */
    private static final int MARGIN_DP = 8;

    /** L'écart entre la ligne et la carte (dp). */
    private static final int GAP_DP = 4;

    /** La largeur d'une icône de sévérité (dp). */
    private static final int ICON_DP = 22;

    /**
     * Un diagnostic d'une ligne : sa sévérité, son message — et les
     * CORRECTIFS proposés (la leçon de la référence : sa popup
     * d'erreur porte le message ET ses correctifs DANS la même
     * carte, l'ampoule n'y est qu'un autre chemin). Le correctif
     * porte son action (l'exécuter referme la carte).
     */
    public record Entry(int severity, String message,
            List<Fix> fixes) {

        /**
         * Un diagnostic sans correctif (la forme courte).
         *
         * @param severity la sévérité, une des constantes SEVERITY_
         * @param message  le message, jamais vide
         */
        public Entry(int severity, String message) {
            this(severity, message, List.of());
        }
    }

    /** Un correctif du diagnostic : son titre et son exécution. */
    public record Fix(String title, Runnable action) {
    }

    /** Les diagnostics vivants de la ligne ancrée (vide : la carte se range). */
    private List<Entry> entries = List.of();

    /** Le bas de l'étage des correctifs (le toucher y vise). */
    private float fixesBottom;

    /** La ligne ancrée (les touches s'y adressent). */
    private int line;

    /** La première rangée de la fenêtre (le défilement de la carte). */
    private int topRow;

    /** Le nombre de rangées visibles à la pose courante. */
    private int visibleRows;

    private float poseX;

    private float poseY;

    private float cardW;

    private float cardH;

    private float density = 1.0f;

    private PopupStyle style = PopupStyle.defaults();

    private final Paint cardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint headerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint footerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rulePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint errorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint warningPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint infoPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glyphPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    /** L'ampoule d'un correctif (l'étage des correctifs la porte). */
    private final Paint fixPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    /** Le titre d'un correctif (l'encre d'action de la référence). */
    private final Paint fixTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint scrollbarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    /**
     * Une carte neuve, ses pinceaux au style par défaut.
     *
     * @param density la densité de l'écran (le dp en pixels)
     */
    public DiagnosticCard(float density) {
        this.density = density;
        applyStyle(style);
    }

    /**
     * Pose le style de l'hôte : le fond translucide, les encres, la
     * taille des textes, la forme — la carte s'y reconfigure entière.
     *
     * @param fresh le style posé, null pour celui par défaut
     */
    public void setStyle(PopupStyle fresh) {
        style = fresh == null ? PopupStyle.defaults() : fresh;
        applyStyle(style);
    }

    /** Le style courant (le harnais le lit). */
    public PopupStyle style() {
        return style;
    }

    /** Reconfigure les pinceaux au style courant. */
    private void applyStyle(PopupStyle style) {
        cardPaint.setColor(style.backgroundArgb());
        borderPaint.setColor(style.borderArgb());
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(1.0f);
        headerPaint.setColor(style.accentArgb());
        headerPaint.setTextSize(style.secondarySizeSp() * density);
        headerPaint.setFakeBoldText(true);
        textPaint.setColor(style.textArgb());
        textPaint.setTextSize((style.textSizeSp() - 2.0f) * density);
        footerPaint.setColor(style.secondaryArgb());
        footerPaint.setTextSize((style.secondarySizeSp() - 2.0f) * density);
        rulePaint.setColor(style.borderArgb());
        errorPaint.setColor(style.errorArgb());
        errorPaint.setStyle(Paint.Style.FILL);
        warningPaint.setColor(style.warningArgb());
        warningPaint.setStyle(Paint.Style.FILL);
        infoPaint.setColor(style.accentArgb());
        infoPaint.setStyle(Paint.Style.FILL);
        glyphPaint.setColor(0xFFFFFFFF);
        glyphPaint.setStyle(Paint.Style.STROKE);
        glyphPaint.setStrokeWidth(1.6f * density);
        fixPaint.setColor(style.accentArgb());
        fixTextPaint.setColor(style.accentArgb());
        fixTextPaint.setTextSize((style.textSizeSp() - 3.0f) * density);
        scrollbarPaint.setColor(0x66B5B5B5);
    }

    /**
     * Ouvre la carte sur les diagnostics d'une ligne — la liste vide
     * range la carte.
     *
     * @param shownLine la ligne ancrée
     * @param items     les diagnostics de la ligne, jamais null
     */
    public void open(int shownLine, List<Entry> items) {
        List<Entry> alive = new ArrayList<>();
        for (Entry item : items) {
            if (item != null && item.message() != null
                    && !item.message().isEmpty()) {
                alive.add(item);
            }
        }
        entries = List.copyOf(alive);
        line = shownLine;
        topRow = 0;
    }

    /** Range la carte (le choix, un nouvel appui, l'édition). */
    public void close() {
        entries = List.of();
        line = -1;
        topRow = 0;
        visibleRows = 0;
    }

    /** La carte vit-elle ? */
    public boolean isShown() {
        return !entries.isEmpty();
    }

    /** Les diagnostics vivants (le harnais les lit). */
    public List<Entry> entries() {
        return entries;
    }

    /** La ligne ancrée, -1 quand la carte se range. */
    public int line() {
        return isShown() ? line : -1;
    }

    /**
     * Le doigt tombé sur la carte lui appartient : la rangée visée la
     * REFERME (le message se lit, le diagnostic ne s'exécute pas) ;
     * le reste de la carte consomme sans agir. Hors de la carte :
     * faux — le geste poursuit sa vie vers l'éditeur.
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
        // L'ÉTAGE DES CORRECTIFS D'ABORD : le doigt sur un correctif
        // l'EXÉCUTE (la leçon de la référence : le correctif de sa
        // popup s'applique au clic — « Import class » y insère
        // l'import) et la carte se range ; le correctif en vol
        // (l'action asynchrone) se referme pareillement
        if (!fixes().isEmpty() && y >= rowsBottomCached
                && y < fixesBottom) {
            float at = rowsBottomCached + dp(3);
            for (Fix fix : fixes()) {
                if (y >= at && y < at + fixRowHeight()) {
                    close();
                    fix.action().run();
                    return true;
                }
                at += fixRowHeight();
            }
        }
        close();
        return true;
    }

    /** Les correctifs de la carte (les diagnostics les portent). */
    public List<Fix> fixes() {
        List<Fix> found = new ArrayList<>();
        for (Entry entry : entries) {
            if (entry.fixes() != null) {
                found.addAll(entry.fixes());
            }
        }
        return found;
    }

    /** La hauteur d'une rangée de correctif (le doigt y vise large). */
    private float fixRowHeight() {
        return dp(style.rowHeightDp() - 10);
    }

    /** Le bas des rangées de messages (l'étage des correctifs y naît). */
    private float rowsBottomCached;

    /** Une rangée de correctif : la petite ampoule, le titre à l'accent. */
    private void drawFix(Canvas canvas, Fix fix, float top) {
        float rowH = fixRowHeight();
        float cy = top + rowH / 2.0f;
        float baseline = top + rowH * 0.68f;
        // l'ampoule de la taille d'une icône de sévérité (le correctif
        // se lit du même œil que le message qu'il corrige)
        float side = dp(ICON_DP) * 0.7f;
        fixPaint.setStyle(Paint.Style.STROKE);
        fixPaint.setStrokeWidth(1.4f * density);
        canvas.drawCircle(cy, cy, side * 0.32f, fixPaint);
        canvas.drawLine(cy - side * 0.16f, cy + side * 0.42f,
                cy + side * 0.16f, cy + side * 0.42f, fixPaint);
        canvas.drawLine(cy - side * 0.08f, cy + side * 0.55f,
                cy + side * 0.08f, cy + side * 0.55f, fixPaint);
        // le titre du correctif RESTE DANS LE CADRE (le retour du
        // terrain : « Corriger : … » y débordait) : la largeur
        // disponible se mesure au pinceau du correctif, l'ellipsize
        // coupe à l'excédent, jamais au-delà du mur
        float available = Math.max(0.0f, poseX + cardW - dp(14)
                - (poseX + dp(14) + dp(ICON_DP) + dp(8)));
        String title = ellipsize("Corriger : " + fix.title(), fixTextPaint,
                available);
        canvas.drawText(title, poseX + dp(14) + dp(ICON_DP) + dp(8),
                baseline, fixTextPaint);
    }

    /**
     * Dessine la carte : la pose se recalcule d'abord — sous la ligne
     * ancrée (au-dessus sans la place) —, puis le titre coiffant les
     * rangées (l'icône de sévérité, le message replié), la barre de
     * défilement si la fenêtre cache des rangées, le pied qui compte.
     *
     * @param canvas      le canevas de la vue
     * @param anchorTop   le bord haut de la ligne ancrée (px, vue)
     * @param anchorBottom le bord bas de la ligne ancrée (px, vue)
     * @param metrics     la géométrie courante de la vue
     */
    public void draw(Canvas canvas, float anchorTop, float anchorBottom,
            PopupMetrics metrics) {
        if (entries.isEmpty()) {
            cardH = 0.0f;
            return;
        }
        place(anchorTop, anchorBottom, metrics);
        float radius = dp(style.cornerRadiusDp());
        canvas.drawRoundRect(poseX, poseY, poseX + cardW, poseY + cardH,
                radius, radius, cardPaint);
        canvas.drawRoundRect(poseX, poseY, poseX + cardW, poseY + cardH,
                radius, radius, borderPaint);
        float rowH = rowHeight();
        float headerH = dp(style.headerHeightDp());
        float rowsTop = poseY + padY();
        float headerBaseline = rowsTop + headerH * 0.72f;
        canvas.drawText("Diagnostics · ligne " + (line + 1),
                poseX + dp(14), headerBaseline, headerPaint);
        canvas.drawLine(poseX + dp(12), rowsTop + headerH - dp(2),
                poseX + cardW - dp(12), rowsTop + headerH - dp(2), rulePaint);
        // LES ÉTAGES (la leçon de la référence : sa popup d'erreur
        // montre le message, PUIS ses correctifs — un filet les
        // sépare, les correctifs s'exécutent au doigt) : les
        // messages d'abord, le filet, les correctifs ensuite
        float rowsBottom = rowsTop + headerH + visibleRows * rowH;
        rowsBottomCached = rowsBottom;
        for (int at = topRow; at < topRow + visibleRows
                && at < entries.size(); at++) {
            float top = rowsTop + headerH + (at - topRow) * rowH;
            drawEntry(canvas, entries.get(at), top);
        }
        float fixesTop = rowsBottom;
        if (!fixes().isEmpty()) {
            canvas.drawLine(poseX + dp(12), fixesTop,
                    poseX + cardW - dp(12), fixesTop, rulePaint);
            float at = fixesTop + dp(3);
            for (Fix fix : fixes()) {
                drawFix(canvas, fix, at);
                at += fixRowHeight();
            }
            fixesBottom = at;
        } else {
            fixesBottom = fixesTop;
        }
        drawScrollbar(canvas, rowsTop + headerH, rowsBottom);
        drawFooter(canvas, Math.max(rowsBottom, fixesBottom));
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
     * L'ordonnée du centre d'une rangée (le harnais y vise le doigt).
     *
     * @param index l'index du diagnostic
     * @return l'ordonnée du centre, -1 hors carte ou hors fenêtre
     */
    public float rowCenterY(int index) {
        if (!isShown() || index < topRow || index >= topRow + visibleRows) {
            return -1.0f;
        }
        return poseY + padY() + dp(style.headerHeightDp())
                + (index - topRow + 0.5f) * rowHeight();
    }

    /**
     * Pose la carte sous la ligne ancrée (au-dessus sans la place,
     * jamais sur elle), le bord gauche de la ligne borné aux murs ;
     * la fenêtre se resserre à ce que la vue tient, au plus
     * {@value #MAX_VISIBLE_ROWS} rangées.
     */
    private void place(float anchorTop, float anchorBottom, PopupMetrics metrics) {
        float margin = dp(MARGIN_DP);
        float gap = dp(GAP_DP);
        cardW = Math.min(Math.max(measuredWidth(),
                dp(style.minCardWidthDp() - 80)),
                Math.min(dp(style.maxCardWidthDp() - 80),
                        metrics.viewWidth() - 2 * margin));
        int rows = Math.min(MAX_VISIBLE_ROWS, entries.size());
        // l'étage des correctifs PAIE sa hauteur (la carte qui le
        // porte se pose plus haute — le pied compte les deux étages)
        int fixCount = fixes().size();
        float fixesHeight = fixCount == 0 ? 0.0f
                : dp(3) + fixCount * fixRowHeight() + dp(3);
        float available = metrics.viewHeight() - 2 * margin - 2 * padY()
                - dp(style.headerHeightDp()) - dp(FOOTER_DP) - fixesHeight;
        while (rows > 1 && 2 * padY() + dp(style.headerHeightDp())
                + dp(FOOTER_DP) + fixesHeight
                + rows * rowHeight() > available) {
            rows--;
        }
        visibleRows = rows;
        cardH = 2 * padY() + dp(style.headerHeightDp())
                + visibleRows * rowHeight() + fixesHeight + dp(FOOTER_DP);
        float belowTop = anchorBottom + gap;
        if (belowTop + cardH <= metrics.viewHeight() - margin) {
            poseY = belowTop;
        } else {
            poseY = Math.max(margin, anchorTop - gap - cardH);
        }
        poseX = Math.max(margin,
                Math.min(metrics.paddingLeft(),
                        metrics.viewWidth() - cardW - margin));
    }

    /** La hauteur d'une rangée : le style la porte, l'icône y respire. */
    private float rowHeight() {
        return dp(style.rowHeightDp() - 8);
    }

    /** La respiration intérieure (px). */
    private float padY() {
        return dp(PAD_DP);
    }

    /** La largeur qu'il faut pour le plus large message, respiré. */
    private float measuredWidth() {
        float widest = 0.0f;
        for (Entry entry : entries) {
            widest = Math.max(widest, textPaint.measureText(entry.message()));
        }
        // l'étage des correctifs PAIE sa largeur aussi (le titre y
        // respire, la carte l'englobe — le débordement y naissait :
        // la carte trop étroite pour son propre correctif)
        for (Fix fix : fixes()) {
            widest = Math.max(widest,
                    fixTextPaint.measureText("Corriger : " + fix.title()));
        }
        return widest + dp(ICON_DP) + dp(16) + 2 * dp(14);
    }

    /** Une rangée : l'icône de sévérité, le message replié. */
    private void drawEntry(Canvas canvas, Entry entry, float top) {
        float rowH = rowHeight();
        float cy = top + rowH / 2.0f;
        float baseline = top + rowH * 0.68f;
        drawSeverityIcon(canvas, entry.severity(),
                poseX + dp(14) + dp(ICON_DP) / 2.0f, cy);
        String message = ellipsize(entry.message(), textPaint,
                cardW - dp(ICON_DP) - 3 * dp(14));
        canvas.drawText(message, poseX + dp(14) + dp(ICON_DP) + dp(8),
                baseline, textPaint);
    }

    /**
     * L'icône de sévérité : le rond rouge barré de l'erreur, le
     * triangle ambre de l'avertissement (le point d'exclamation y
     * vit), le rond bleu de l'information (le « i » y vit).
     */
    private void drawSeverityIcon(Canvas canvas, int severity,
            float cx, float cy) {
        float side = dp(ICON_DP);
        Paint fill = severity == SEVERITY_ERROR ? errorPaint
                : severity == SEVERITY_WARNING ? warningPaint : infoPaint;
        if (severity == SEVERITY_WARNING) {
            // le triangle ambre, le point d'exclamation au centre
            float half = side / 2.0f;
            float topY = cy - half * 0.75f;
            canvas.drawPath(trianglePath(cx, topY, side * 0.9f), fill);
            canvas.drawLine(cx, cy - side * 0.18f, cx, cy + side * 0.08f,
                    glyphPaint);
            canvas.drawPoint(cx, cy + side * 0.26f, glyphPaint);
            return;
        }
        float radius = side / 2.0f;
        canvas.drawCircle(cx, cy, radius, fill);
        if (severity == SEVERITY_ERROR) {
            float s = side * 0.24f;
            canvas.drawLine(cx - s, cy - s, cx + s, cy + s, glyphPaint);
            canvas.drawLine(cx - s, cy + s, cx + s, cy - s, glyphPaint);
        } else {
            float s = side * 0.22f;
            canvas.drawPoint(cx, cy + s * 1.4f, glyphPaint);
            canvas.drawLine(cx, cy - s * 0.6f, cx, cy + s * 0.4f, glyphPaint);
        }
    }

    /** Le chemin du triangle d'avertissement, pointe en haut. */
    private android.graphics.Path trianglePath(float cx, float topY, float width) {
        android.graphics.Path path = new android.graphics.Path();
        path.moveTo(cx, topY);
        path.lineTo(cx + width / 2.0f, topY + width * 0.55f);
        path.lineTo(cx - width / 2.0f, topY + width * 0.55f);
        path.close();
        return path;
    }

    /**
     * La barre de défilement : la fenêtre montre-t-elle moins que la
     * liste ? Le pouce se pose au prorata des rangées visibles.
     */
    private void drawScrollbar(Canvas canvas, float trackTop, float trackBottom) {
        if (visibleRows >= entries.size()) {
            return;
        }
        float thumb = (trackBottom - trackTop) * visibleRows / entries.size();
        float trackSpan = trackBottom - trackTop - thumb;
        float thumbTop = entries.size() <= 1 ? trackTop
                : trackTop + trackSpan * topRow / (entries.size() - visibleRows);
        canvas.drawRect(poseX + cardW - dp(5), thumbTop,
                poseX + cardW - dp(3), thumbTop + thumb, scrollbarPaint);
    }

    /** Le pied de carte : le compte des diagnostics de la ligne. */
    private void drawFooter(Canvas canvas, float footerTop) {
        float baseline = footerTop + dp(FOOTER_DP) * 0.66f;
        String count = entries.size() == 1 ? "1 diagnostic"
                : entries.size() + " diagnostics";
        canvas.drawText(count, poseX + dp(14), baseline, footerPaint);
        String hint = "toucher pour fermer";
        canvas.drawText(hint,
                poseX + cardW - dp(14) - footerPaint.measureText(hint),
                baseline, footerPaint);
    }

    /**
     * Coupe un texte à la largeur disponible — la queue se garde,
     * la tête s'abrège en « … ».
     */
    private String ellipsize(String text, Paint paint, float maxWidth) {
        if (paint.measureText(text) <= maxWidth || text.length() < 2) {
            return text;
        }
        String tail = text.substring(1);
        while (tail.length() > 1
                && paint.measureText("…" + tail) > maxWidth) {
            tail = tail.substring(1);
        }
        return "…" + tail;
    }

    private float dp(int value) {
        return value * density;
    }

    private float dp(float value) {
        return value * density;
    }
}
