package jo.pigma.editor.popups;

import android.graphics.Canvas;
import android.graphics.Paint;

import jo.pigma.editor.EditorView;

/**
 * LA CARTE DE L'APERÇU DE LA MODIFICATION DE L'ASSISTANT (B3) : la
 * réponse qui ÉCRIT ne se pose JAMAIS d'elle-même — elle se PROPOSE.
 * Le remplacement (ou l'insertion) s'y lit en DEUX sections, « Avant »
 * (le texte que la réponse remplace) et « Après » (le texte neuf),
 * l'instruction qui l'a suscitée au titre — et le pied porte LES TROIS
 * VERDICTS : « Accepter » POSE la modification (UNE unité
 * d'annulation, après VÉRIFICATION : un texte qui a bougé depuis la
 * demande ne se pose pas), « Rejeter » la jette (le tampon n'a jamais
 * bougé), « Réessayer » repose la QUESTION à l'identique (un vol
 * neuf — la réponse d'avant se jette).
 *
 * <p>Le contenu de la réponse est une DONNÉE NON FIABLE : chaque
 * section se BORNE (peu de lignes, chaque ligne coupée à la largeur)
 * — la carte ne montre jamais un contenu démesuré, n'exécute rien,
 * ne suit aucun lien.</p>
 *
 * <p>La carte vit de la pose au caret (dessous si elle tient, dessus
 * sinon — {@link CardBase}), la croix la range comme « Rejeter ». Les
 * trois verdicts vivent en puces d'au moins 48 dp de cible (le doigt
 * y vise à l'aise).</p>
 */
public final class AiDiffCard extends CardBase {

    /** La hauteur de l'en-tête (dp). */
    private static final int HEADER_DP = 30;

    /** La hauteur du pied (dp) — les trois verdicts y vivent. */
    private static final int FOOTER_DP = 48;

    /** La marge aux murs de la vue (dp). */
    private static final int MARGIN_DP = 8;

    /** L'écart au caret (dp). */
    private static final int GAP_DP = 4;

    /** Le plafond de lignes montrées par section (le contenu se borne). */
    private static final int MAX_LINES = 6;

    /** La cible tactile minimale d'un verdict (dp). */
    private static final int CHIP_MIN_DP = 48;

    /** L'instruction qui a suscité la proposition (le titre la porte). */
    private String instruction = "";

    /** Le texte AVANT (celui que le remplacement vise). */
    private String before = "";

    /** Le texte APRÈS (celui que la réponse propose). */
    private String after = "";

    /** Le porteur des verdicts (l'assistant y branche ses vols). */
    private Verdicts verdicts;

    /** La vue porteuse (le verdict s'y joue hors du geste). */
    private EditorView editor;

    /** Le verdict armé au DOWN (0 : accepter, 1 : rejeter, 2 : réessayer). */
    private int armedVerdict = -1;

    /** Le doigt vit-il dans le cadre ? */
    private boolean pressed;

    private final Paint cardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sectionPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint chipPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint chipBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint chipTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rejectPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rejectBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rejectTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glyphPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rulePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    /**
     * Les verdicts de l'aperçu : l'assistant y branche ses vols —
     * l'ACCEPTATION pose (UNE unité d'annulation, après vérification
     * de la révision), le REJET jette, la NOUVELLE ESSAIE repose la
     * question à l'identique.
     */
    public interface Verdicts {

        /** L'acceptation : la modification se pose. */
        void onAccept();

        /** Le rejet : la proposition se jette (le tampon n'a pas bougé). */
        void onReject();

        /** La nouvelle tentative : la question repart à l'identique. */
        void onRetry();
    }

    /**
     * Une carte neuve, ses pinceaux propres au style par défaut.
     *
     * @param density la densité de l'écran (le dp en pixels)
     */
    public AiDiffCard(float density) {
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
        sectionPaint.setColor(style.secondaryArgb());
        sectionPaint.setFakeBoldText(true);
        sectionPaint.setTextSize((style.secondarySizeSp() - 1.0f) * density);
        textPaint.setColor(style.textArgb());
        textPaint.setTextSize((style.textSizeSp() - 3.0f) * density);
        // le verdict qui POSE : la puce à l'accent (l'action s'y lit)
        chipPaint.setColor((style.accentArgb() & 0x00FFFFFF) | 0x1A000000);
        chipBorderPaint.setColor(style.accentArgb());
        chipBorderPaint.setStyle(Paint.Style.STROKE);
        chipBorderPaint.setStrokeWidth(1.0f);
        chipTextPaint.setColor(style.accentArgb());
        chipTextPaint.setFakeBoldText(true);
        chipTextPaint.setTextSize((style.secondarySizeSp() - 1.0f) * density);
        // le verdict qui JETTE : l'encre secondaire
        rejectPaint.setColor((style.secondaryArgb() & 0x00FFFFFF) | 0x14000000);
        rejectBorderPaint.setColor(style.secondaryArgb());
        rejectBorderPaint.setStyle(Paint.Style.STROKE);
        rejectBorderPaint.setStrokeWidth(1.0f);
        rejectTextPaint.setColor(style.secondaryArgb());
        rejectTextPaint.setFakeBoldText(true);
        rejectTextPaint.setTextSize((style.secondarySizeSp() - 1.0f) * density);
        glyphPaint.setColor(style.secondaryArgb());
        glyphPaint.setStyle(Paint.Style.STROKE);
        glyphPaint.setStrokeWidth(1.5f * density);
        rulePaint.setColor(style.borderArgb());
    }

    /**
     * Branche la vue porteuse et les verdicts.
     *
     * @param view     la vue, jamais null
     * @param verdicts les verdicts de l'assistant, jamais null
     */
    public void bind(EditorView view, Verdicts verdicts) {
        editor = view;
        this.verdicts = verdicts;
    }

    /**
     * Ouvre la carte sur une proposition : l'instruction qui l'a
     * suscitée, le texte AVANT et le texte APRÈS — chacun BORNÉ à
     * l'affichage (la réponse est une donnée non fiable : peu de
     * lignes, coupées à la largeur).
     *
     * @param ofInstruction l'instruction suscitante, vide permise
     * @param beforeText    le texte remplacé, vide permis (une insertion)
     * @param afterText     le texte proposé, jamais null
     */
    public void open(String ofInstruction, String beforeText,
            String afterText) {
        instruction = ofInstruction == null ? "" : ofInstruction;
        before = beforeText == null ? "" : beforeText;
        after = afterText == null ? "" : afterText;
        armedVerdict = -1;
        pressed = false;
        shown = true;
    }

    /** Ramasse les restes de la fermeture. */
    @Override
    protected void onClosed() {
        instruction = "";
        before = "";
        after = "";
        armedVerdict = -1;
        pressed = false;
    }

    /** L'instruction suscitante (le harnais la lit). */
    public String instruction() {
        return instruction;
    }

    /** Le texte AVANT (le harnais le lit). */
    public String beforeText() {
        return before;
    }

    /** Le texte APRÈS (le harnais le lit). */
    public String afterText() {
        return after;
    }

    /**
     * Le doigt tombé sur la carte lui appartient : la croix la range
     * (rejeter), le verdict s'y ARME — l'UP le jouera. Hors de la
     * carte : faux.
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
        armedVerdict = -1;
        float crossLeft = poseX + cardW - dp(32);
        if (y < poseY + dp(HEADER_DP)
                && x >= crossLeft && x <= poseX + cardW - dp(8)) {
            close();
            return true;
        }
        if (y >= poseY + cardH - dp(FOOTER_DP)) {
            float cursor = poseX + dp(12);
            for (int verdict = 0; verdict < 3; verdict++) {
                float chipW = chipWidth(verdict);
                if (x >= cursor && x < cursor + chipW) {
                    armedVerdict = verdict;
                    break;
                }
                cursor += chipW + dp(10);
            }
        }
        return true;
    }

    /**
     * Le relâchement joue le verdict armé : l'ACCEPTATION et le REJET
     * rangent la carte (le verdict se joue HORS du geste — posté au
     * tour suivant), la NOUVELLE ESSAIE garde la proposition à
     * l'écran (le vol neuf vivra sa carte d'attente, la réponse
     * remplacera la proposition).
     */
    public boolean onRelease() {
        if (!pressed) {
            return false;
        }
        pressed = false;
        int verdict = armedVerdict;
        armedVerdict = -1;
        if (verdict < 0 || verdicts == null || editor == null) {
            return true;
        }
        final int chosen = verdict;
        if (chosen == 1) {
            close();
            editor.post(verdicts::onReject);
        } else {
            // l'acceptation et la NOUVELLE ESSAIE rangent la carte (le
            // vol neuf vivra sa carte d'attente, la réponse y
            // remplacera la proposition)
            close();
            editor.post(chosen == 0 ? verdicts::onAccept
                    : verdicts::onRetry);
        }
        return true;
    }

    /** Le verdict armé au doigt (-1 : aucun — le harnais le lit). */
    public int armedVerdict() {
        return armedVerdict;
    }

    /**
     * L'abscisse du CENTRE d'un verdict (le harnais y vise le doigt
     * — la vraie route du toucher).
     *
     * @param verdict le verdict (0 : accepter, 1 : rejeter,
     *                2 : réessayer)
     * @return l'abscisse du centre, -1 hors carte
     */
    public float verdictCenterX(int verdict) {
        if (!isShown() || verdict < 0 || verdict > 2) {
            return -1.0f;
        }
        float cursor = poseX + dp(12);
        for (int at = 0; at < verdict; at++) {
            cursor += chipWidth(at) + dp(10);
        }
        return cursor + chipWidth(verdict) / 2.0f;
    }

    /**
     * L'ordonnée du CENTRE des verdicts (le harnais y vise le doigt).
     *
     * @return l'ordonnée du centre, -1 hors carte
     */
    public float verdictCenterY() {
        if (!isShown()) {
            return -1.0f;
        }
        float chipTop = poseY + cardH - dp(FOOTER_DP) + dp(8);
        float chipBottom = poseY + cardH - dp(8);
        return (chipTop + chipBottom) / 2.0f;
    }

    /**
     * Dessine la carte : l'en-tête (le titre, la croix), l'instruction
     * suscitante, la section « Avant » puis « Après » (les lignes
     * bornées, coupées à la largeur), le pied des TROIS VERDICTS.
     *
     * @param canvas       le canevas de la vue
     * @param anchorTop    le bord haut de la ligne du caret (px, vue)
     * @param anchorBottom le bord bas de la ligne du caret (px, vue)
     * @param metrics      la géométrie courante de la vue
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
        float headerBaseline = poseY + dp(HEADER_DP) * 0.68f;
        canvas.drawText(ellipsizeTail(strings.aiDiffTitle(), titlePaint,
                cardW - dp(76)), poseX + dp(14), headerBaseline, titlePaint);
        drawCross(canvas, poseX + cardW - dp(24),
                poseY + dp(HEADER_DP) / 2.0f, glyphPaint);
        canvas.drawLine(poseX + dp(12), poseY + dp(HEADER_DP) - dp(2),
                poseX + cardW - dp(12), poseY + dp(HEADER_DP) - dp(2),
                rulePaint);
        float cursor = poseY + dp(HEADER_DP);
        // l'instruction suscitante (le proposé se RAPPELLE ce qui fut
        // demandé — la proposition se lit dans son contexte)
        if (!instruction.isEmpty()) {
            canvas.drawText(ellipsizeTail("« " + instruction + " »",
                    sectionPaint, cardW - 2 * dp(14)),
                    poseX + dp(14), cursor + dp(16), sectionPaint);
            cursor += dp(24);
        }
        // la section AVANT : le texte que le remplacement vise
        canvas.drawText(strings.aiDiffBefore(), poseX + dp(14),
                cursor + dp(16), sectionPaint);
        cursor += dp(22);
        cursor = drawLines(canvas, before, cursor, false);
        canvas.drawLine(poseX + dp(12), cursor + dp(2),
                poseX + cardW - dp(12), cursor + dp(2), rulePaint);
        cursor += dp(8);
        // la section APRÈS : le texte neuf proposé
        canvas.drawText(strings.aiDiffAfter(), poseX + dp(14),
                cursor + dp(16), sectionPaint);
        cursor += dp(22);
        cursor = drawLines(canvas, after, cursor, true);
        drawVerdicts(canvas);
    }

    /**
     * Les lignes d'une section : BORNÉES au plafond, chaque ligne
     * coupée à la largeur (le contenu est une donnée non fiable — la
     * carte ne montre jamais un contenu démesuré).
     *
     * @param canvas le canevas de la vue
     * @param body   le texte de la section
     * @param top    l'ordonnée de la première ligne (px)
     * @param accent la section APRÈS vit à l'encre d'accent
     * @return l'ordonnée après la dernière ligne (px)
     */
    private float drawLines(Canvas canvas, String body, float top,
            boolean accent) {
        String[] lines = body.split("\n", -1);
        int shown = Math.min(lines.length, MAX_LINES);
        for (int at = 0; at < shown; at++) {
            String line = lines[at].trim();
            if (line.isEmpty()) {
                line = " ";
            }
            canvas.drawText(ellipsizeTail(line, textPaint, cardW - 2 * dp(14)),
                    poseX + dp(14), top + dp(14),
                    accent ? chipTextPaint : textPaint);
            top += dp(18);
        }
        if (lines.length > MAX_LINES) {
            canvas.drawText("… (" + (lines.length - MAX_LINES) + ")",
                    poseX + dp(14), top + dp(14), sectionPaint);
            top += dp(18);
        }
        return top;
    }

    /** Le pied des TROIS VERDICTS : Accepter (l'accent), Rejeter,
     * Réessayer — chacun d'une cible d'au moins 48 dp. */
    private void drawVerdicts(Canvas canvas) {
        float cursor = poseX + dp(12);
        float chipTop = poseY + cardH - dp(FOOTER_DP) + dp(8);
        float chipBottom = poseY + cardH - dp(8);
        for (int verdict = 0; verdict < 3; verdict++) {
            String label = verdict == 0 ? strings.aiDiffAccept()
                    : verdict == 1 ? strings.aiDiffReject()
                    : strings.aiDiffRetry();
            Paint fill = verdict == 0 ? chipPaint : rejectPaint;
            Paint border = verdict == 0 ? chipBorderPaint : rejectBorderPaint;
            Paint ink = verdict == 0 ? chipTextPaint : rejectTextPaint;
            float chipW = chipWidth(verdict);
            canvas.drawRoundRect(cursor, chipTop, cursor + chipW, chipBottom,
                    dp(10), dp(10), fill);
            canvas.drawRoundRect(cursor, chipTop, cursor + chipW, chipBottom,
                    dp(10), dp(10), border);
            canvas.drawText(label, cursor + dp(14),
                    (chipTop + chipBottom) / 2.0f + dp(4), ink);
            cursor += chipW + dp(10);
        }
    }

    /** La largeur d'une puce de verdict (la cible ≥ 48 dp). */
    private float chipWidth(int verdict) {
        String label = verdict == 0 ? strings.aiDiffAccept()
                : verdict == 1 ? strings.aiDiffReject()
                : strings.aiDiffRetry();
        return Math.max(dp(CHIP_MIN_DP),
                (verdict == 0 ? chipTextPaint : rejectTextPaint)
                        .measureText(label) + dp(28));
    }

    /**
     * Pose la carte sous la ligne du caret (au-dessus sans la place,
     * jamais sur elle), le bord gauche du texte borné aux murs — la
     * hauteur suit les sections montrées, bornée aux murs de la vue.
     */
    private void place(float anchorTop, float anchorBottom,
            PopupMetrics metrics) {
        float margin = dp(MARGIN_DP);
        cardW = clampWidth(dp(style.minCardWidthDp() + 60),
                style.minCardWidthDp(), style.maxCardWidthDp() - 40,
                margin, metrics);
        float body = dp(HEADER_DP) + dp(FOOTER_DP)
                + (instruction.isEmpty() ? 0 : dp(24))
                + dp(22) + countedLines(before) * dp(18)
                + dp(8) + dp(22) + countedLines(after) * dp(18)
                + dp(10);
        cardH = Math.min(body, metrics.viewHeight() - 2 * margin);
        placeY(anchorTop, anchorBottom, margin, dp(GAP_DP), cardH,
                metrics.viewHeight());
        placeX(margin, metrics);
        poseSettled();
    }

    /** Le compte des lignes montrées d'une section (borné au plafond). */
    private int countedLines(String body) {
        int lines = body.split("\n", -1).length;
        return Math.min(lines, MAX_LINES) + (lines > MAX_LINES ? 1 : 0);
    }
}
