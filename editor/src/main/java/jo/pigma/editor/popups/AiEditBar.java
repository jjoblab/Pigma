package jo.pigma.editor.popups;

import android.graphics.Canvas;
import android.graphics.Paint;

import jo.pigma.editor.EditorView;

/**
 * LA BARRE D'INSTRUCTION DE L'ASSISTANT (B3 — « Modifier avec l'IA… ») :
 * la SÉLECTION se modifie PAR INSTRUCTION — l'utilisateur y dit CE
 * qu'il veut (« ajoute la garde nulle », « renomme en total des
 * euros »…), l'IA de l'hôte y répond. La même famille que la
 * recherche, l'aller-à et le renommage : DESSINÉE DANS LA VUE, jamais
 * de fenêtre flottante, rien ne prend le focus — le clavier reste à
 * l'éditeur, qui rend les verdicts des touches à la carte
 * (KeyboardCard : l'instruction s'y écrit, la composition du clavier
 * s'y remplace, la validation y part). La pose : AU CARET — dessous
 * si elle tient, dessus sinon — bornée aux murs, et le caret la
 * suit ; le curseur du champ y clignote (FieldCaret — la frappe
 * déléguée s'y voit vivante).
 *
 * <p>La validation porte l'instruction au PORTEUR (l'assistant y
 * branche son vol — la tâche EDIT_SELECTION, la sélection et
 * l'instruction voyagent) ; une instruction vide ne part pas (le
 * retour le dit). Rien ne s'écrit dans le document : le champ vit à
 * la carte.</p>
 */
public final class AiEditBar implements KeyboardCard {

    /** La respiration intérieure de la carte (dp). */
    private static final int PAD_DP = 8;

    /** Le plafond de l'instruction (caractères — le champ se borne). */
    private static final int MAX_CHARS = 200;

    /** L'instruction tapée (elle voyage à la demande). */
    private final StringBuilder instruction = new StringBuilder();

    /** La longueur de la dernière composition posée au champ. */
    private int composed;

    /** Le retour sous le champ (la validation y parle). */
    private String feedback = "";

    private boolean shown;

    /** Le doigt vient-il de viser le champ ? (l'éditeur y lève le clavier). */
    private boolean fieldTapped;

    /** Le porteur de l'instruction (l'assistant y branche son vol). */
    private EditTarget target;

    /** La pose de la carte (px, coordonnées de la vue) — le harnais la lit. */
    private float poseX;
    private float poseY;
    private float cardW;
    private float cardH;
    private float ancreX;
    private float ancreY;
    private float density;

    /** Le style posé par l'hôte : fond translucide, tailles, largeur, forme. */
    private PopupStyle style = PopupStyle.defaults();

    /** Les chaînes posées par l'hôte (l'invite localisée — la source unique). */
    private PopupStrings strings = PopupStrings.defaults();

    /**
     * LE FLOT de pose (la barre qui suit le caret y GLISSE au lieu
     * d'y sauter).
     */
    private final PopupFloat flot = new PopupFloat();

    private EditorView editor;

    private final Paint cardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hintPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint smallPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glyphPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    /** L'encre d'accent (le curseur du champ qui clignote). */
    private final Paint accentPaint = new Paint();

    /**
     * Le porteur de l'instruction : la validation lui porte le texte
     * tapé — l'assistant y démarre son vol (la tâche
     * EDIT_SELECTION, la sélection et l'instruction voyagent).
     */
    public interface EditTarget {

        /**
         * L'instruction soumise.
         *
         * @param instruction le texte tapé, jamais vide
         * @return vrai quand le vol part (la barre se range) ; faux
         *         quand il ne peut pas (le retour le dit)
         */
        boolean onInstructionSubmitted(String instruction);
    }

    /**
     * Une barre neuve, ses pinceaux propres au style par défaut.
     *
     * @param density la densité de l'écran (le dp en pixels)
     */
    public AiEditBar(float density) {
        this.density = density;
        applyStyle();
    }

    /**
     * Pose le style de l'hôte : le fond translucide, les encres, la
     * taille des textes, la largeur des cartes, la forme — les
     * pinceaux s'y reconfigurent entiers.
     *
     * @param fresh le style posé, null pour celui par défaut
     */
    public void setStyle(PopupStyle fresh) {
        style = fresh == null ? PopupStyle.defaults() : fresh;
        applyStyle();
    }

    /** Le style courant (le harnais le lit). */
    public PopupStyle style() {
        return style;
    }

    /**
     * Pose les chaînes de l'hôte (l'invite localisée — la source
     * unique).
     *
     * @param fresh les chaînes posées, null pour celles par défaut
     */
    public void setStrings(PopupStrings fresh) {
        strings = fresh == null ? PopupStrings.defaults() : fresh;
    }

    /** Reconfigure les pinceaux au style courant. */
    private void applyStyle() {
        accentPaint.setColor(style.accentArgb());
        cardPaint.setColor(style.backgroundArgb());
        borderPaint.setColor(style.borderArgb());
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(1.0f);
        textPaint.setColor(style.textArgb());
        textPaint.setTextSize(style.textSizeSp() * density);
        hintPaint.setColor(style.hintArgb());
        hintPaint.setTextSize(style.textSizeSp() * density);
        smallPaint.setColor(style.secondaryArgb());
        smallPaint.setTextSize(style.secondarySizeSp() * density);
        glyphPaint.setColor(style.secondaryArgb());
        glyphPaint.setStyle(Paint.Style.STROKE);
        glyphPaint.setStrokeWidth(1.6f * density);
    }

    /**
     * Branche la barre à son éditeur : le rangement lui revient.
     *
     * @param editor l'éditeur parcouru
     */
    public void bind(EditorView editor) {
        this.editor = editor;
    }

    /**
     * La barre se montre : l'instruction part vierge, le porteur
     * reçoit la validation.
     *
     * @param target le porteur de l'instruction
     */
    public void show(EditTarget target) {
        this.target = target;
        instruction.setLength(0);
        composed = 0;
        feedback = "";
        shown = true;
        fieldTapped = false;
    }

    /** La barre se range : l'instruction se vide. */
    public void hide() {
        shown = false;
        instruction.setLength(0);
        composed = 0;
        fieldTapped = false;
        flot.reset();
    }

    /** La barre vit-elle ? */
    public boolean isShown() {
        return shown;
    }

    /**
     * Le doigt vient-il de viser le champ de l'instruction ?
     * (l'éditeur y lève le clavier logiciel après le toucher).
     *
     * @return vrai quand le dernier toucher visait le champ
     */
    public boolean keyboardWanted() {
        return fieldTapped;
    }

    /**
     * Pose la barre au caret : dessous si elle tient, dessus sinon,
     * l'abscisse du caret bornée aux murs — la pose se recalcule à
     * chaque dessin, le caret la suit.
     *
     * @param metrics la géométrie courante de la vue
     */
    public void poser(PopupMetrics metrics) {
        density = metrics.density();
        ancreX = metrics.caretX();
        ancreY = metrics.caretY();
        float margin = dp(8);
        float pad = dp(PAD_DP);
        float rowHeight = dp(style.rowHeightDp());
        cardW = Math.max(Math.min(metrics.viewWidth() - 2 * margin,
                dp(style.maxCardWidthDp())), Math.min(dp(style.minCardWidthDp()),
                        metrics.viewWidth() - 2 * margin));
        cardH = 2 * pad + rowHeight + dp(22);
        float gap = dp(4);
        float below = ancreY + metrics.lineHeight() + gap;
        poseY = below + cardH <= metrics.viewHeight() - margin
                ? below
                : Math.max(margin, ancreY - gap - cardH);
        poseX = Math.max(margin,
                Math.min(ancreX, metrics.viewWidth() - cardW - margin));
        flot.poseTo(poseX, poseY);
    }

    /**
     * Dessine la barre (la pose se recalcule d'abord — le caret la
     * suit), par-dessus tout : l'invite, l'instruction (l'indication
     * si le champ est vierge), le curseur qui clignote au bout du
     * tapé, la croix, le retour dessous.
     *
     * @param canvas  le canevas de la vue
     * @param metrics la géométrie courante de la vue
     */
    public void draw(Canvas canvas, PopupMetrics metrics) {
        poser(metrics);
        float radius = dp(style.cornerRadiusDp());
        canvas.drawRoundRect(poseX, poseY, poseX + cardW, poseY + cardH,
                radius, radius, cardPaint);
        canvas.drawRoundRect(poseX, poseY, poseX + cardW, poseY + cardH,
                radius, radius, borderPaint);
        float pad = dp(PAD_DP);
        float rowHeight = dp(style.rowHeightDp());
        float rowTop = poseY + pad;
        float baseline = rowTop + rowHeight * 0.7f;
        String typed = instruction.toString();
        float inviteWidth = textPaint.measureText(strings.aiEditInvite());
        canvas.drawText(strings.aiEditInvite(), poseX + dp(10), baseline,
                textPaint);
        canvas.drawText(typed.isEmpty() ? strings.aiEditHint() : typed,
                poseX + dp(10) + inviteWidth + dp(4), baseline,
                typed.isEmpty() ? hintPaint : textPaint);
        // LE CURSEUR QUI CLIGNOTE au bout de l'instruction tapée (la
        // frappe déléguée s'y voit vivante, l'encre d'accent)
        FieldCaret.draw(canvas,
                poseX + dp(10) + inviteWidth + dp(4)
                        + textPaint.measureText(typed) + dp(3),
                rowTop + rowHeight * 0.22f,
                rowTop + rowHeight * 0.82f, dp(2), accentPaint);
        float glyphW = dp(style.rowHeightDp() - 2);
        float cy = rowTop + rowHeight / 2;
        float crossCx = poseX + cardW - dp(2) - glyphW / 2;
        float s = dp(7);
        canvas.drawLine(crossCx - s, cy - s, crossCx + s, cy + s, glyphPaint);
        canvas.drawLine(crossCx - s, cy + s, crossCx + s, cy - s, glyphPaint);
        // le retour grisonne dessous
        canvas.drawText(feedback, poseX + dp(10),
                rowTop + rowHeight + dp(16), smallPaint);
    }

    /**
     * Le doigt tombé sur la barre lui appartient : la croix la range,
     * le champ de l'instruction lève le clavier (l'éditeur s'en
     * charge), le reste s'y consomme. Hors de la barre : faux — le
     * geste poursuit sa vie vers l'éditeur.
     *
     * @param x l'abscisse du doigt (px, coordonnées de la vue)
     * @param y l'ordonnée du doigt (px, coordonnées de la vue)
     * @return vrai quand la barre a pris le geste
     */
    public boolean onTouchArea(float x, float y) {
        if (!shown || x < poseX || x >= poseX + cardW
                || y < poseY || y >= poseY + cardH) {
            return false;
        }
        float pad = dp(PAD_DP);
        float rowHeight = dp(style.rowHeightDp());
        if (y <= poseY + Math.max(pad + rowHeight, dp(48))
                && x >= poseX + cardW - dp(48)) {
            // LA CROIX — la cible ≥ 48 dp (B7 : les cartes d'IA au
            // doigt à l'aise, le glyphe dessiné respire moins)
            close();
            return true;
        }
        // le champ de l'instruction : le clavier délégué lui revient
        fieldTapped = true;
        return true;
    }

    /**
     * L'instruction s'écrit (tout le texte commis y passe — le clavier
     * délègue par blocs) ; la composition qui suit, s'il y en a une,
     * remplacera ces caractères. Rien ne s'écrit dans le document.
     */
    @Override
    public boolean onText(CharSequence text) {
        if (text == null || text.length() == 0 || text.charAt(0) == '\n') {
            return true;
        }
        for (int at = 0; at < text.length()
                && instruction.length() < MAX_CHARS; at++) {
            char c = text.charAt(at);
            if (c != '\n') {
                instruction.append(c);
            }
        }
        composed = text.length();
        return true;
    }

    /**
     * La composition du clavier remplace ce qu'elle avait posé : le
     * champ rend sa dernière pose (l'insert ou la composition
     * d'avant), la nouvelle s'y écrit.
     */
    @Override
    public boolean onComposition(CharSequence text) {
        if (composed > 0 && instruction.length() >= composed) {
            instruction.setLength(instruction.length() - composed);
        }
        composed = 0;
        return onText(text);
    }

    /** Le retour arrière efface le dernier caractère (la composition se rompt). */
    @Override
    public boolean onBackspace() {
        composed = 0;
        if (instruction.length() > 0) {
            instruction.setLength(instruction.length() - 1);
        }
        return true;
    }

    /** Les flèches ne font rien ici — la barre garde le clavier. */
    @Override
    public boolean onArrow(int step) {
        return true;
    }

    /**
     * La validation porte l'instruction : le porteur y reçoit le
     * texte tapé — parti, la barre se range (le vol prend le relais,
     * sa carte d'attente vit) ; vide, le retour le dit.
     */
    @Override
    public boolean onEnter() {
        String typed = instruction.toString().trim();
        if (typed.isEmpty()) {
            feedback = strings.aiEditEmpty();
            return true;
        }
        if (target != null && target.onInstructionSubmitted(typed)) {
            editor.dismissAiEditPopup();
            return true;
        }
        return true;
    }

    /** L'instruction tapée (le harnais la lit). */
    public String instructionText() {
        return instruction.toString();
    }

    /** Le retour courant (le harnais le lit). */
    public String feedbackLabel() {
        return feedback;
    }

    /** La croix range la barre. */
    public void close() {
        editor.dismissAiEditPopup();
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

    /** Le décalage horizontal du flot de pose (px — nul au repos). */
    public float floatOffsetX() {
        return flot.offsetX();
    }

    /** Le décalage vertical du flot de pose (px — nul au repos). */
    public float floatOffsetY() {
        return flot.offsetY();
    }

    /** Le flot de pose vit-il (la vue y réclame la trame suivante). */
    public boolean floating() {
        return flot.isRunning();
    }

    /** La largeur de la barre (le harnais la lit). */
    public float width() {
        return cardW;
    }

    /** La hauteur de la barre (le harnais la lit). */
    public float height() {
        return cardH;
    }

    private float dp(int value) {
        return value * density;
    }
}
