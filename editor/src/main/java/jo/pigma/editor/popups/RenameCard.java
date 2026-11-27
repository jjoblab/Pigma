package jo.pigma.editor.popups;

import android.graphics.Canvas;
import android.graphics.Paint;

import jo.pigma.editor.EditorView;

/**
 * La carte « renommer le symbole », DESSINÉE DANS LA VUE (jamais de
 * fenêtre flottante, pas de jeton de fenêtre, pas de conflit de
 * focus avec l'IME) — la même famille que la recherche et
 * l'aller-à : rien ne prend le focus, le clavier reste à l'éditeur,
 * qui rend les verdicts des touches à la carte (KeyboardCard — le
 * nom neuf s'y écrit, la validation y part). La pose : AU CARET —
 * dessous si elle tient, dessus sinon — bornée aux murs, et le
 * caret la suit. Le nom neuf démarre du symbole courant (la façon
 * d'Android Studio : le nom s'y corrige, il ne se retape pas
 * entier) ; la validation le porte au porteur du renommage (la
 * façade du serveur de langage y branche le protocole — le
 * remplacement de chaque occurrence).
 */
public final class RenameCard implements KeyboardCard {

    /** La respiration intérieure de la carte (dp). */
    private static final int PAD_DP = 8;

    /** Le symbole à renommer (l'invite le porte). */
    private String symbol = "";

    /** Le nom neuf tapé (il démarre du symbole). */
    private final StringBuilder name = new StringBuilder();

    /** Le retour sous le champ (la validation y parle). */
    private String feedback = "";

    /** Le porteur du renommage (la façade y branche le serveur). */
    private RenameTarget target;

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

    /** Le style posé par l'hôte : fond translucide, tailles, largeur, forme. */
    private PopupStyle style = PopupStyle.defaults();

    /**
     * LE FLOT de pose (le retour du terrain : la barre qui suit le
     * caret ne SAUTE plus à sa pose nouvelle — elle y GLISSE).
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
     * Le porteur du renommage : la validation lui porte le nom neuf
     * — la façade du serveur de langage s'y branche (le protocole y
     * remplace chaque occurrence du symbole).
     */
    public interface RenameTarget {

        /**
         * Le renommage demandé : le symbole courant et le nom neuf.
         *
         * @param symbol  le symbole à renommer (le nom d'origine)
         * @param newName le nom neuf soumis
         * @return vrai quand le renommage part (la carte se range) ;
         *         faux quand il ne peut pas (le retour le dit)
         */
        boolean onRenameSubmitted(String symbol, String newName);
    }

    /**
     * Une carte neuve, ses pinceaux propres au style par défaut.
     *
     * @param density la densité de l'écran (le dp en pixels)
     */
    public RenameCard(float density) {
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
     * Branche la carte à son éditeur : le rangement lui revient.
     *
     * @param editor l'éditeur parcouru
     */
    public void bind(EditorView editor) {
        this.editor = editor;
    }

    /**
     * La carte se montre pour un symbole : le nom neuf démarre du
     * symbole (la façon d'Android Studio), le porteur reçoit la
     * validation.
     *
     * @param symbol le symbole renommé
     * @param target le porteur du renommage
     */
    public void show(String symbol, RenameTarget target) {
        this.symbol = symbol == null ? "" : symbol;
        this.target = target;
        name.setLength(0);
        name.append(this.symbol);
        feedback = "Entrée valide, la croix range";
        shown = true;
        fieldTapped = false;
    }

    /** La carte se range : le nom neuf se vide. */
    public void hide() {
        shown = false;
        name.setLength(0);
        fieldTapped = false;
        flot.reset();
    }

    /** La carte vit-elle ? */
    public boolean isShown() {
        return shown;
    }

    /**
     * Le doigt vient-il de viser le champ du nom ? (l'éditeur y
     * lève le clavier logiciel après le toucher).
     */
    public boolean keyboardWanted() {
        return fieldTapped;
    }

    /**
     * Pose la carte au caret : dessous si elle tient, dessus sinon,
     * l'abscisse du caret bornée aux murs — la pose se recalcule à
     * chaque dessin : le caret la suit.
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
        // LA POSE POSÉE : le flot s'y nourrit (la barre glisse à sa pose
        // nouvelle au lieu d'y sauter — le retour du terrain)
        flot.poseTo(poseX, poseY);
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
        float radius = dp(style.cornerRadiusDp());
        canvas.drawRoundRect(poseX, poseY, poseX + cardW, poseY + cardH,
                radius, radius, cardPaint);
        canvas.drawRoundRect(poseX, poseY, poseX + cardW, poseY + cardH,
                radius, radius, borderPaint);
        float pad = dp(PAD_DP);
        float rowHeight = dp(style.rowHeightDp());
        float rowTop = poseY + pad;
        float baseline = rowTop + rowHeight * 0.7f;
        // l'invite d'Android Studio, puis le nom neuf (le symbole en
        // guise d'indication si le champ se vide), puis la croix
        String invite = "Renommer « " + symbol + " » : ";
        float inviteWidth = textPaint.measureText(invite);
        canvas.drawText(invite, poseX + dp(10), baseline, textPaint);
        canvas.drawText(name.length() == 0 ? "nom neuf" : name.toString(),
                poseX + dp(10) + inviteWidth + dp(4), baseline,
                name.length() == 0 ? hintPaint : textPaint);
        // LE CURSEUR QUI CLIGNOTE au bout du nom tapé (le retour du
        // terrain) — la frappe déléguée s'y voit VIVANTE, l'encre d'accent
        String shownName = name.toString();
        FieldCaret.draw(canvas,
                poseX + dp(10) + inviteWidth + dp(4)
                        + textPaint.measureText(shownName) + dp(3),
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
     * Le doigt tombé sur la carte lui appartient : la croix range la
     * carte, le champ du nom lève le clavier (l'éditeur s'en charge),
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
        float rowHeight = dp(style.rowHeightDp());
        float glyphW = dp(style.rowHeightDp() - 2);
        if (y < poseY + pad + rowHeight
                && x >= poseX + cardW - dp(2) - glyphW) {
            close();
            return true;
        }
        // le champ du nom : le clavier délégué lui revient — l'éditeur
        // y lève le clavier
        fieldTapped = true;
        return true;
    }

    /**
     * Le nom neuf s'écrit (tout le texte commis y passe — le clavier
     * délègue par blocs) ; la composition qui suit, s'il y en a une,
     * remplacera ces caractères. Rien ne s'écrit dans le document.
     */
    @Override
    public boolean onText(CharSequence text) {
        if (text == null || text.length() == 0 || text.charAt(0) == '\n') {
            return true;
        }
        for (int at = 0; at < text.length(); at++) {
            char c = text.charAt(at);
            if (Character.isJavaIdentifierPart(c)) {
                name.append(c);
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
        if (composed > 0 && name.length() >= composed) {
            name.setLength(name.length() - composed);
        }
        composed = 0;
        return onText(text);
    }

    /** Le retour arrière efface le dernier caractère du nom (la composition se rompt). */
    @Override
    public boolean onBackspace() {
        composed = 0;
        if (name.length() > 0) {
            name.setLength(name.length() - 1);
        }
        return true;
    }

    /** Les flèches ne font rien ici — la carte garde le clavier. */
    @Override
    public boolean onArrow(int step) {
        return true;
    }

    /**
     * La validation porte le renommage : le porteur y reçoit le
     * symbole et le nom neuf — parti, la carte se range ; refusé
     * (le serveur ne sait pas), le retour le dit.
     */
    @Override
    public boolean onEnter() {
        String newName = name.toString();
        if (newName.isEmpty()) {
            feedback = "un nom neuf manque";
            return true;
        }
        if (newName.equals(symbol)) {
            feedback = "le nom n'a pas bougé";
            return true;
        }
        if (target != null && target.onRenameSubmitted(symbol, newName)) {
            editor.dismissRenamePopup();
            return true;
        }
        feedback = "le renommage ne part pas (le serveur ne sait pas ?)";
        return true;
    }

    /** Le symbole renommé (le harnais le lit). */
    public String symbol() {
        return symbol;
    }

    /** Le nom neuf tapé (le harnais le lit). */
    public String nameText() {
        return name.toString();
    }

    /** Le retour courant (le harnais le lit). */
    public String feedbackLabel() {
        return feedback;
    }

    /** La croix range la carte. */
    public void close() {
        editor.dismissRenamePopup();
    }

    /** L'abscisse posée (le harnais la lit). */
    public float poseX() {
        return poseX;
    }

    /** L'ordonnée posée (le harnais la lit). */
    public float poseY() {
        return poseY;
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

    /** La largeur de la carte (le harnais la lit). */
    public float width() {
        return cardW;
    }

    /** La hauteur de la carte (le harnais la lit). */
    public float height() {
        return cardH;
    }

    /** La longueur de la dernière composition posée au champ. */
    private int composed;

    private float dp(int value) {
        return value * density;
    }
}
