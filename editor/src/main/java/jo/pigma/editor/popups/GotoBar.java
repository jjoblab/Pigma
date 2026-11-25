package jo.pigma.editor.popups;

import android.graphics.Canvas;
import android.graphics.Paint;

import java.util.Locale;

import jo.pigma.editor.EditorView;

/**
 * La carte « aller à la ligne », DESSINÉE DANS LA VUE (jamais de
 * fenêtre flottante, pas de jeton de fenêtre, pas de conflit de
 * focus avec l'IME) ; rien ne prend le focus, le clavier reste à
 * l'éditeur, qui rend les verdicts des touches à la carte
 * (KeyboardCard — les chiffres s'y posent, la validation y va) — et
 * quand le doigt vise le champ, l'éditeur y lève le clavier. La
 * pose : AU CARET — dessous si elle tient, dessus sinon — bornée
 * aux murs, et le caret la suit.
 * L'invite reste celle d'Android Studio : « Aller à la ligne : », le
 * numéro se borne au fichier (une ligne au-delà va à la dernière —
 * le retour le dit).
 */
public final class GotoBar implements KeyboardCard {

    /** La hauteur d'une rangée de la carte (dp). */
    private static final int ROW_HEIGHT_DP = 44;

    /** La respiration intérieure de la carte (dp). */
    private static final int PAD_DP = 8;

    /** La largeur d'un glyphe d'action (dp). */
    private static final int GLYPH_DP = 44;

    /** La largeur plancher de la carte (dp). */
    private static final int MIN_WIDTH_DP = 300;

    /** La largeur plafond de la carte (dp). */
    private static final int MAX_WIDTH_DP = 360;

    private final StringBuilder digits = new StringBuilder();
    private String feedback = "";

    private boolean shown;

    /** Le doigt vient-il de viser le champ ? (l'éditeur y lève le clavier). */
    private boolean fieldTapped;

    /** La pose de la carte (px, coordonnées de la vue) — le harnais la lit. */
    private float poseX;
    private float poseY;
    private float cardW;
    private float cardH;
    private float ancreX;
    private float ancreY;
    private float density;

    private EditorView editor;

    private final Paint cardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hintPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint smallPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glyphPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    /**
     * Une carte neuve, ses pinceaux propres.
     *
     * @param density la densité de l'écran (le dp en pixels)
     */
    public GotoBar(float density) {
        cardPaint.setColor(0xFFF3F3F3);
        borderPaint.setColor(0xFFB5B5B5);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(1.0f);
        textPaint.setColor(0xFF202124);
        textPaint.setTextSize(16.0f * density);
        hintPaint.setColor(0xFF80868B);
        hintPaint.setTextSize(16.0f * density);
        smallPaint.setColor(0xFF5F6368);
        smallPaint.setTextSize(13.0f * density);
        glyphPaint.setColor(0xFF5F6368);
        glyphPaint.setStyle(Paint.Style.STROKE);
        glyphPaint.setStrokeWidth(1.6f * density);
    }

    /**
     * Branche la carte à son éditeur : l'aller-y lui parle, la
     * fermeture lui revient.
     *
     * @param editor l'éditeur parcouru
     */
    public void bind(EditorView editor) {
        this.editor = editor;
    }

    /** La carte se montre : les chiffres partent vierges, le fichier se compte. */
    public void show() {
        shown = true;
        digits.setLength(0);
        feedback = String.format(Locale.ROOT, "sur %d ligne(s)",
                editor.getBuffer().getLineCount());
    }

    /** La carte se range : les chiffres se vident. */
    public void hide() {
        shown = false;
        digits.setLength(0);
        fieldTapped = false;
    }

    /** La carte vit-elle ? */
    public boolean isShown() {
        return shown;
    }

    /**
     * Le doigt vient-il de viser le champ des chiffres ? (l'éditeur y
     * lève le clavier logiciel après le toucher).
     *
     * @return vrai quand le dernier toucher visait le champ
     */
    public boolean keyboardWanted() {
        return fieldTapped;
    }

    /**
     * Pose la carte au caret : dessous si elle tient, dessus sinon,
     * l'abscisse du caret bornée aux murs. La pose se recalcule à
     * chaque dessin : le caret la
     * suit (défilement, navigation).
     *
     * @param metrics la géométrie courante de la vue
     */
    public void poser(PopupMetrics metrics) {
        density = metrics.density();
        ancreX = metrics.caretX();
        ancreY = metrics.caretY();
        float margin = dp(8);
        float pad = dp(PAD_DP);
        float rowHeight = dp(ROW_HEIGHT_DP);
        cardW = Math.max(Math.min(metrics.viewWidth() - 2 * margin,
                dp(MAX_WIDTH_DP)), Math.min(dp(MIN_WIDTH_DP),
                        metrics.viewWidth() - 2 * margin));
        cardH = 2 * pad + rowHeight + dp(20);
        float gap = dp(4);
        float below = ancreY + metrics.lineHeight() + gap;
        poseY = below + cardH <= metrics.viewHeight() - margin
                ? below
                : Math.max(margin, ancreY - gap - cardH);
        poseX = Math.max(margin,
                Math.min(ancreX, metrics.viewWidth() - cardW - margin));
    }

    /**
     * Dessine la carte (la pose se recalcule d'abord — le caret la
     * suit), par-dessus tout.
     *
     * @param canvas  le canevas de la vue
     * @param metrics la géométrie courante de la vue
     */
    public void draw(Canvas canvas, PopupMetrics metrics) {
        poser(metrics);
        float radius = dp(12);
        canvas.drawRoundRect(poseX, poseY, poseX + cardW, poseY + cardH,
                radius, radius, cardPaint);
        canvas.drawRoundRect(poseX, poseY, poseX + cardW, poseY + cardH,
                radius, radius, borderPaint);
        float pad = dp(PAD_DP);
        float rowHeight = dp(ROW_HEIGHT_DP);
        float rowTop = poseY + pad;
        float baseline = rowTop + rowHeight * 0.7f;
        // l'invite d'Android Studio, puis les chiffres (l'indication
        // si le champ est vierge), puis la croix
        float inviteWidth = textPaint.measureText("Aller à la ligne : ");
        canvas.drawText("Aller à la ligne : ", poseX + dp(10), baseline, textPaint);
        canvas.drawText(digits.length() == 0 ? "numéro" : digits.toString(),
                poseX + dp(10) + inviteWidth + dp(4), baseline,
                digits.length() == 0 ? hintPaint : textPaint);
        float glyphW = dp(GLYPH_DP);
        float cy = rowTop + rowHeight / 2;
        float crossCx = poseX + cardW - dp(2) - glyphW / 2;
        float s = dp(6);
        canvas.drawLine(crossCx - s, cy - s, crossCx + s, cy + s, glyphPaint);
        canvas.drawLine(crossCx - s, cy + s, crossCx + s, cy - s, glyphPaint);
        // le retour grisonne dessous
        canvas.drawText(feedback, poseX + dp(10),
                rowTop + rowHeight + dp(14), smallPaint);
    }

    /**
     * Le doigt tombé sur la carte lui appartient (les popups se
     * dessinent et se touchent) : la croix range la carte,
     * le champ des chiffres lève le clavier (l'éditeur s'en charge),
     * le reste s'y consomme. Hors de la carte : faux — le geste
     * poursuit sa vie vers l'éditeur.
     *
     * @param x l'abscisse du doigt (px, coordonnées de la vue)
     * @param y l'ordonnée du doigt (px, coordonnées de la vue)
     * @return vrai quand la carte a pris le geste
     */
    public boolean onTouchArea(float x, float y) {
        if (!shown || x < poseX || x >= poseX + cardW
                || y < poseY || y >= poseY + cardH) {
            return false;
        }
        float pad = dp(PAD_DP);
        float rowHeight = dp(ROW_HEIGHT_DP);
        float glyphW = dp(GLYPH_DP);
        if (y < poseY + pad + rowHeight
                && x >= poseX + cardW - dp(2) - glyphW) {
            close();
            return true;
        }
        // le champ des chiffres : le clavier délégué lui revient —
        // l'éditeur y lève le clavier
        fieldTapped = true;
        return true;
    }

    /**
     * Les chiffres se posent (tout le texte commis y passe — le
     * clavier délègue par blocs) ; le reste s'avale (la carte garde
     * le clavier tant qu'elle vit — rien ne s'écrit dans le document).
     */
    @Override
    public boolean onText(CharSequence text) {
        if (text == null || text.length() == 0 || text.charAt(0) == '\n') {
            return true;
        }
        for (int at = 0; at < text.length() && digits.length() < 9; at++) {
            char c = text.charAt(at);
            if (Character.isDigit(c)) {
                digits.append(c);
            }
        }
        return true;
    }

    /**
     * La composition du clavier, pour l'aller-à : les chiffres s'y
     * posent comme un insert (le clavier ne compose guère des
     * nombres — et remplacer ou ajouter, seuls les chiffres passent).
     */
    @Override
    public boolean onComposition(CharSequence text) {
        return onText(text);
    }

    /** Le retour arrière efface le dernier chiffre. */
    @Override
    public boolean onBackspace() {
        if (digits.length() > 0) {
            digits.setLength(digits.length() - 1);
        }
        return true;
    }

    /** Les flèches ne font rien ici — la carte garde le clavier. */
    @Override
    public boolean onArrow(int pas) {
        return true;
    }

    /**
     * L'aller-y : le numéro se borne (1 à la dernière ligne), le
     * curseur s'y pose — caretTo l'assure visible — et le retour
     * dit où il est tombé.
     */
    @Override
    public boolean onEnter() {
        go();
        return true;
    }

    /** Les chiffres tapés (le harnais les lit). */
    public String digitsText() {
        return digits.toString();
    }

    /** Le retour courant (le harnais le lit). */
    public String feedbackLabel() {
        return feedback;
    }

    /** La croix range la carte. */
    public void close() {
        editor.dismissGotoPopup();
    }

    /** L'abscisse posée (le harnais la lit). */
    public float poseX() {
        return poseX;
    }

    /** L'ordonnée posée (le harnais la lit). */
    public float poseY() {
        return poseY;
    }

    /** L'ordonnée du caret d'ancrage (le harnais la lit). */
    public float ancreY() {
        return ancreY;
    }

    /** L'abscisse du caret d'ancrage (le harnais la lit). */
    public float ancreX() {
        return ancreX;
    }

    /** La largeur de la carte (le harnais la lit). */
    public float width() {
        return cardW;
    }

    /** La hauteur de la carte (le harnais la lit). */
    public float height() {
        return cardH;
    }

    /** L'aller-y : borne, pose du curseur, retour. */
    private void go() {
        if (editor == null) {
            return;
        }
        int lineCount = editor.getBuffer().getLineCount();
        if (digits.length() == 0 || lineCount == 0) {
            feedback = "ligne ?";
            return;
        }
        int line;
        try {
            line = Integer.parseInt(digits.toString());
        } catch (NumberFormatException notANumber) {
            feedback = "ligne ?";
            return;
        }
        int bounded = Math.max(1, Math.min(line, lineCount));
        editor.caretTo(editor.getBuffer().getLineStart(bounded - 1));
        feedback = String.format(Locale.ROOT,
                "ligne %d · sur %d", bounded, lineCount);
    }

    private float dp(int value) {
        return value * density;
    }
}
