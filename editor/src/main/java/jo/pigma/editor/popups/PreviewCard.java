package jo.pigma.editor.popups;

import jo.pigma.editor.EditorView;

import android.graphics.Canvas;
import android.graphics.Paint;

import java.util.List;

/**
 * LA CARTE DE LA DÉFINITION EN APERÇU (la définition en aperçu du rapport des popups — le
 * « Quick Definition » de l'atelier) : lire la déclaration d'un symbole
 * SANS quitter la position. L'en-tête dit le fichier et sa ligne, le
 * CORPS porte les lignes de la déclaration (une douzaine au plafond —
 * la déclaration y tient), le pied porte le bouton « Ouvrir » (la
 * carte se range, le curseur y part). La leçon de la référence : sa
 * popup s'ouvre au symbole sous le doigt, le code y vit COLORÉ —
 * notre corps y dessine les lignes à la grille de l'éditeur, la ligne
 * de déclaration en gras (le nom s'y détache).
 *
 * <p>La carte vit de {@link CardBase} : la pose au caret (dessous si
 * elle tient, dessus sinon), les murs, la croix, le texte coupé. Le
 * corps défile au doigt quand la déclaration dépasse la fenêtre.</p>
 *
 * <p>La carte ne prend jamais le focus ; le bouton « Ouvrir » range
 * la carte AVANT de naviguer (le geste ne se joue pas deux fois).</p>
 */
public final class PreviewCard extends CardBase {

    /** La hauteur de l'en-tête (dp). */
    private static final int HEADER_DP = 46;

    /** La hauteur du pied (dp) — le bouton « Ouvrir » y vit (la cible
     * tactile y tient à 48 dp au moins — le pied l'englobe). */
    private static final int FOOTER_DP = 52;

    /** La hauteur d'une ligne de code (dp). */
    private static final int CODE_LINE_DP = 24;

    /** Le plafond de lignes visibles (la carte défile au-delà). */
    private static final int MAX_VISIBLE_LINES = 12;

    /** La marge aux murs de la vue (dp). */
    private static final int MARGIN_DP = 8;

    /** L'écart au caret (dp). */
    private static final int GAP_DP = 4;

    /**
     * Une définition en aperçu : le symbole questionné, son fichier et
     * sa ligne, les lignes de sa déclaration, le saut à la position.
     *
     * @param title le nom du symbole questionné
     * @param file  le fichier de la déclaration (vide : le document
     *              courant)
     * @param line  l'indice de la ligne de déclaration
     * @param lines les lignes de la déclaration, coupées au plafond
     * @param open   le saut à la position (le bouton « Ouvrir »)
     */
    public record Preview(String title, String file, int line,
            List<String> lines, Runnable open) {
    }

    /** La définition vivante (vide : la carte se range). */
    private Preview preview;

    /** La première ligne visible (le défilement du corps). */
    private int topLine;

    /** Le bouton « Ouvrir » est-il armé ? */
    private boolean openPressed;

    /** Le doigt vit-il dans le cadre ? */
    private boolean pressed;

    /** La vue porteuse (la navigation s'y poste). */
    private EditorView editor;

    private final Paint cardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint wherePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint codePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint firstPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint footerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint chipPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint chipBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint chipTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glyphPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rulePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    /**
     * Une carte neuve, ses pinceaux propres au style par défaut.
     *
     * @param density la densité de l'écran (le dp en pixels)
     */
    public PreviewCard(float density) {
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
        wherePaint.setColor(style.hintArgb());
        wherePaint.setTextSize((style.secondarySizeSp() - 2.0f) * density);
        codePaint.setColor(style.textArgb());
        codePaint.setTextSize((style.textSizeSp() - 4.0f) * density);
        firstPaint.setColor(style.textArgb());
        firstPaint.setFakeBoldText(true);
        firstPaint.setTextSize((style.textSizeSp() - 4.0f) * density);
        footerPaint.setColor(style.secondaryArgb());
        footerPaint.setTextSize((style.secondarySizeSp() - 2.0f) * density);
        // le bouton « Ouvrir » : la puce à l'accent (la leçon de la
        // barre de sélection — l'action s'y lit en teinte d'action)
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
    }

    /**
     * Branche la vue porteuse : le saut du bouton « Ouvrir » s'y
     * exécute (hors du geste — la carte vit dans la vue).
     *
     * @param view la vue, jamais null
     */
    public void bind(EditorView view) {
        editor = view;
    }

    /**
     * Ouvre la carte sur la définition d'un symbole — null range la
     * carte, la déclaration vide aussi.
     *
     * @param definition la définition à montrer, null pour rien
     */
    public void open(Preview definition) {
        preview = definition;
        topLine = 0;
        shown = definition != null && !definition.lines().isEmpty();
    }

    /** Ramasse les restes de la fermeture. */
    @Override
    protected void onClosed() {
        preview = null;
        topLine = 0;
        openPressed = false;
        pressed = false;
    }

    /** La définition vivante (le harnais la lit). */
    public Preview preview() {
        return preview;
    }

    /** La première ligne visible (le harnais la lit). */
    public int topLine() {
        return topLine;
    }

    /**
     * Le doigt tombé sur la carte lui appartient : la croix la range,
     * le bouton « Ouvrir » s'arme (l'UP le joue), le corps consomme
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
        pressed = true;
        openPressed = false;
        // la croix : la carte se range
        float crossLeft = poseX + cardW - dp(32);
        if (y < poseY + dp(HEADER_DP)
                && x >= crossLeft && x <= poseX + cardW - dp(8)) {
            openPressed = false;
            close();
            return true;
        }
        // le bouton « Ouvrir » du pied
        if (y >= poseY + cardH - dp(FOOTER_DP)) {
            openPressed = true;
        }
        return true;
    }

    /**
     * Le relâchement joue le bouton armé : « Ouvrir » range la carte
     * puis SAUTE à la position (le saut se joue hors du geste).
     */
    public boolean onRelease() {
        if (!pressed) {
            return false;
        }
        pressed = false;
        if (openPressed && preview != null && preview.open() != null
                && editor != null) {
            Runnable jump = preview.open();
            openPressed = false;
            close();
            editor.post(jump);
            return true;
        }
        openPressed = false;
        return true;
    }

    /**
     * Dessine la carte : la pose se recalcule d'abord — sous la ligne
     * du caret (au-dessus sans la place) —, puis l'en-tête (le symbole,
     * le fichier · la ligne, la croix), le filet, le corps (les lignes
     * de la fenêtre — la première au gras, la barre de défilement si
     * la fenêtre cache des lignes), le pied (le bouton « Ouvrir »).
     *
     * @param canvas       le canevas de la vue
     * @param anchorTop    le bord haut de la ligne du caret (px, vue)
     * @param anchorBottom le bord bas de la ligne du caret (px, vue)
     * @param metrics      la géométrie courante de la vue
     */
    public void draw(Canvas canvas, float anchorTop, float anchorBottom,
            PopupMetrics metrics) {
        if (!isShown() || preview == null) {
            cardH = 0.0f;
            return;
        }
        place(anchorTop, anchorBottom, metrics);
        float radius = dp(style.cornerRadiusDp());
        canvas.drawRoundRect(poseX, poseY, poseX + cardW, poseY + cardH,
                radius, radius, cardPaint);
        canvas.drawRoundRect(poseX, poseY, poseX + cardW, poseY + cardH,
                radius, radius, borderPaint);
        // l'en-tête : le symbole, le fichier · la ligne, la croix
        float headerBaseline = poseY + dp(HEADER_DP) * 0.66f;
        canvas.drawText(ellipsizeTail(preview.title(), titlePaint,
                cardW - dp(150)),
                poseX + dp(14), headerBaseline, titlePaint);
        String where = strings.fileLineAt(
                preview.file() == null ? "" : preview.file(),
                preview.line());
        float whereW = wherePaint.measureText(where);
        canvas.drawText(ellipsizeTail(where, wherePaint, cardW - dp(96)),
                poseX + cardW - dp(40) - whereW, headerBaseline, wherePaint);
        drawCross(canvas, poseX + cardW - dp(24),
                poseY + dp(HEADER_DP) / 2.0f, glyphPaint);
        canvas.drawLine(poseX + dp(12), poseY + dp(HEADER_DP) - dp(2),
                poseX + cardW - dp(12), poseY + dp(HEADER_DP) - dp(2),
                rulePaint);
        // le corps : les lignes de la fenêtre (la PREMIÈRE au gras —
        // le nom s'y détache), la barre de défilement si la fenêtre
        // cache des lignes
        int lines = preview.lines().size();
        int visible = Math.min(MAX_VISIBLE_LINES, lines);
        float cursor = poseY + dp(HEADER_DP);
        for (int at = topLine; at < topLine + visible && at < lines; at++) {
            String text = preview.lines().get(at) == null
                    ? "" : preview.lines().get(at);
            Paint ink = at == 0 ? firstPaint : codePaint;
            canvas.drawText(ellipsizeTail(text, ink, cardW - dp(28)),
                    poseX + dp(14), cursor + dp(CODE_LINE_DP) * 0.72f, ink);
            cursor += dp(CODE_LINE_DP);
        }
        if (visible < lines) {
            float thumbH = Math.max(dp(24), (cursor - poseY - dp(HEADER_DP))
                    * visible / lines);
            float trackH = cursor - poseY - dp(HEADER_DP) - thumbH;
            float thumbY = poseY + dp(HEADER_DP)
                    + trackH * topLine / Math.max(1, lines - visible);
            canvas.drawRect(poseX + cardW - dp(6), thumbY,
                    poseX + cardW - dp(4), thumbY + thumbH, rulePaint);
        }
        drawFooter(canvas);
    }

    /** Le pied de carte : le compte à gauche, le bouton « Ouvrir » à droite. */
    private void drawFooter(Canvas canvas) {
        float footerBaseline = poseY + cardH - dp(FOOTER_DP) / 2.0f;
        int lines = preview.lines().size();
        String count = strings.bodyLinesCount(lines);
        canvas.drawText(count, poseX + dp(14), footerBaseline, footerPaint);
        // le bouton « Ouvrir » : la puce à l'accent (l'action s'y lit)
        String label = strings.open();
        float labelW = chipTextPaint.measureText(label);
        float chipW = labelW + dp(24);
        float chipLeft = poseX + cardW - dp(14) - chipW;
        float chipTop = poseY + cardH - dp(FOOTER_DP) + dp(6);
        float chipBottom = poseY + cardH - dp(6);
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
        int lines = preview.lines().size();
        int visible = Math.min(MAX_VISIBLE_LINES, lines);
        float widest = titlePaint.measureText(preview.title());
        for (String line : preview.lines()) {
            widest = Math.max(widest, codePaint.measureText(
                    line == null ? "" : line));
        }
        cardW = Math.min(Math.max(widest + dp(28), dp(260)),
                Math.min(dp(420), metrics.viewWidth() - 2 * margin));
        cardH = dp(HEADER_DP) + visible * dp(CODE_LINE_DP) + dp(FOOTER_DP);
        placeY(anchorTop, anchorBottom, margin, dp(GAP_DP), cardH,
                metrics.viewHeight());
        placeX(margin, metrics);
        // LA POSE POSÉE : le flot s'y nourrit (la carte glisse à sa pose
        // nouvelle au lieu d'y sauter — le retour du terrain)
        poseSettled();
    }

    /**
     * Le défilement du corps : la moitié d'une ligne par tranche (le
     * harnais y éprouve la fenêtre), bornée aux lignes vivantes.
     *
     * @param step le nombre de lignes à descendre (négatif : monter)
     */
    public void scroll(int step) {
        if (!isShown() || preview == null) {
            return;
        }
        int lines = preview.lines().size();
        int visible = Math.min(MAX_VISIBLE_LINES, lines);
        topLine = Math.max(0,
                Math.min(lines - visible, topLine + step));
    }
}
