package jo.pigma.editor.popups;

import android.graphics.Canvas;
import android.graphics.Paint;

import jo.pigma.editor.EditorView;

/**
 * La barre de sélection, DESSINÉE DANS LA VUE : les actions de la
 * sélection en pastilles — Copier, Coller (seulement quand le
 * presse-papiers porte du texte : l'action inutile ne se promène
 * jamais sur la pilule), Tout copier — puis un filet, l'icône « i »
 * (la documentation rapide du mot sélectionné) et la puce « ⋯ » de
 * débordement (les actions repliées y vivent). Elle se pose
 * AU-DESSUS de la sélection, centrée sur elle, bornée aux murs ; la
 * pose se recalcule à chaque dessin — la sélection bouge, la barre
 * suit.
 *
 * <p>La barre ne prend jamais le focus : la frappe remplace la
 * sélection (l'éditeur reçoit le clavier, la barche se range quand
 * la sélection meurt) ; le doigt sur une pastille l'exécute, le
 * reste de la barre consomme sans agir, hors d'elle le geste poursuit
 * sa vie vers l'éditeur.</p>
 */
public final class SelectionBar {

    /** La hauteur du contenu de la pilule (dp). */
    private static final int HEIGHT_DP = 28;

    /** La respiration horizontale d'un libellé (dp). */
    private static final int PAD_X_DP = 10;

    /** L'écart entre deux pastilles (dp). */
    private static final int GAP_DP = 2;

    /** Le budget du filet séparateur (dp). */
    private static final int RULE_DP = 5;

    /** La largeur d'une icône (dp). */
    private static final int ICON_DP = 22;

    /** La respiration verticale de la carte autour de la pilule (dp). */
    private static final int PAD_Y_DP = 3;

    /** L'identifiant d'action de la pastille Copier. */
    private static final int ACT_COPY = 0;

    /** L'identifiant d'action de la pastille Coller. */
    private static final int ACT_PASTE = 1;

    /** L'identifiant d'action de la pastille Tout copier. */
    private static final int ACT_COPY_ALL = 2;

    /** L'identifiant de l'icône documentation au toucher. */
    private static final int ICON_DOCS = -2;

    /** L'identifiant de la puce de débordement au toucher. */
    private static final int ICON_OVERFLOW = -3;

    /** Le doigt tombé hors de toute zone. */
    private static final int NO_HIT = -1;

    /** Les pastilles vivantes : leur libellé et leur action. */
    private final java.util.ArrayList<int[]> entries = new java.util.ArrayList<>();

    /** Le presse-papiers porte-t-il du texte ? (la pastille Coller en vit) */
    private boolean canPaste;

    private boolean shown;

    private EditorView editor;

    /** La pose de la pilule (px, coordonnées de la vue). */
    private float poseX;

    private float poseY;

    /** La largeur mesurée du contenu (px). */
    private float contentW;

    private float density = 1.0f;

    private final Paint cardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint chipPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rulePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glyphPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    /**
     * Une barre neuve, ses pinceaux propres.
     *
     * @param density la densité de l'écran (le dp en pixels)
     */
    public SelectionBar(float density) {
        cardPaint.setColor(0xFFF3F3F3);
        borderPaint.setColor(0xFFB5B5B5);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(1.0f);
        chipPaint.setColor(0x1F1A73E8);
        textPaint.setColor(0xFF202124);
        textPaint.setTextSize(13.0f * density);
        rulePaint.setColor(0xFFB5B5B5);
        glyphPaint.setColor(0xFF5F6368);
        glyphPaint.setStyle(Paint.Style.STROKE);
        glyphPaint.setStrokeWidth(1.4f);
    }

    /**
     * Branche la barre à son éditeur : les actions y parlent (copier,
     * coller, tout copier, documentation, débordement).
     *
     * @param editor l'éditeur sélectionné
     */
    public void bind(EditorView editor) {
        this.editor = editor;
    }

    /**
     * Montre la barre — les pastilles se posent VIVES : la pastille
     * Coller ne vit que lorsque le presse-papiers porte du texte.
     *
     * @param pastePossible vrai quand un texte se colle
     */
    public void show(boolean pastePossible) {
        canPaste = pastePossible;
        shown = true;
    }

    /** Range la barre (la sélection est morte, ou un geste la tient). */
    public void hide() {
        shown = false;
    }

    /** La barre vit-elle ? */
    public boolean isShown() {
        return shown;
    }

    /**
     * La pastille demandée vit-elle ? (le harnais le vérifie — la
     * pastille Coller naît du presse-papiers).
     *
     * @param label le libellé cherché
     * @return vrai quand la pastille se montre
     */
    public boolean hasChip(String label) {
        for (int[] entry : entries) {
            if (labelOf(entry[0]).equals(label)) {
                return true;
            }
        }
        return false;
    }

    /**
     * L'abscisse du CENTRE d'une pastille (le harnais y vise le doigt).
     *
     * @param label le libellé de la pastille
     * @return l'abscisse du centre, -1 si la pastille ne vit pas
     */
    public float chipCenterX(String label) {
        for (int at = 0; at < entries.size(); at++) {
            if (labelOf(entries.get(at)[0]).equals(label)) {
                return chipCenterX(entries.get(at));
            }
        }
        return -1.0f;
    }

    /** L'ordonnée du centre de la pilule (le harnais y vise le doigt). */
    public float centerY() {
        return poseY + dp(PAD_Y_DP) + dp(HEIGHT_DP) / 2.0f;
    }

    /**
     * L'abscisse du centre de l'icône « i » (le harnais y vise le
     * doigt : la documentation rapide du mot sélectionné s'y ouvre).
     *
     * @return l'abscisse du centre (après un dessin de la barre)
     */
    public float docsCenterX() {
        return iconStart() + dp(ICON_DP) / 2.0f;
    }

    /**
     * L'abscisse du centre de la puce « ⋯ » (le harnais y vise le
     * doigt : le débordement s'y ouvre).
     *
     * @return l'abscisse du centre (après un dessin de la barre)
     */
    public float overflowCenterX() {
        return iconStart() + dp(ICON_DP) + dp(ICON_DP) / 2.0f;
    }

    /** Le début du groupe d'icônes (filet compris) — le même budget
     * que le dessin : l'écart du dernier libellé, puis le filet. */
    private float iconStart() {
        float cursor = poseX;
        for (int[] entry : entries) {
            cursor += 2.0f * dp(PAD_X_DP)
                    + textPaint.measureText(labelOf(entry[0])) + dp(GAP_DP);
        }
        return cursor + dp(GAP_DP) + dp(RULE_DP);
    }

    /** L'abscisse posée (le harnais la lit). */
    public float poseX() {
        return poseX;
    }

    /** L'ordonnée posée (le harnais la lit). */
    public float poseY() {
        return poseY;
    }

    /** La largeur de la pilule (le harnais la lit). */
    public float width() {
        return contentW;
    }

    /** La hauteur de la pilule (le harnais la lit). */
    public float height() {
        return 2.0f * dp(PAD_Y_DP) + dp(HEIGHT_DP);
    }

    /**
     * Le doigt tombé sur la barre lui appartient : la pastille visée
     * s'exécute, l'icône documentation ouvre la documentation rapide
     * du mot sélectionné, la puce de débordement ouvre les actions
     * repliées ; le reste de la barre consomme sans agir. Hors de la
     * barre : faux — le geste poursuit sa vie vers l'éditeur.
     *
     * @param x l'abscisse du doigt (px, coordonnées de la vue)
     * @param y l'ordonnée du doigt (px, coordonnées de la vue)
     * @return vrai quand la barre a pris le geste
     */
    public boolean onTouchArea(float x, float y) {
        if (!shown || x < poseX || x >= poseX + contentW
                || y < poseY || y >= poseY + height()) {
            return false;
        }
        int hit = areaAt(x);
        if (hit != NO_HIT && editor != null) {
            switch (hit) {
                case ACT_COPY:
                    editor.copySelection();
                    break;
                case ACT_PASTE:
                    editor.pasteFromClipboard();
                    break;
                case ACT_COPY_ALL:
                    editor.copyAllText();
                    break;
                case ICON_DOCS:
                    editor.showSelectionDocs();
                    break;
                case ICON_OVERFLOW:
                    editor.showOverflowMenu();
                    break;
                default:
                    break;
            }
        }
        return true;
    }

    /**
     * Dessine la barre : la pose se recalcule d'abord — la sélection
     * bouge, la barre la suit — puis les pastilles respirées, le
     * filet, l'icône « i » et la puce « ⋯ ».
     *
     * @param canvas    le canevas de la vue
     * @param anchorTop le bord haut de la sélection (px, coordonnées
     *                  de la vue)
     * @param anchorBottom le bord bas de la sélection (px)
     * @param centerX   le centre horizontal de la sélection (px)
     * @param metrics   la géométrie courante de la vue
     */
    public void draw(Canvas canvas, float anchorTop, float anchorBottom,
            float centerX, PopupMetrics metrics) {
        density = metrics.density();
        if (!shown) {
            return;
        }
        buildEntries();
        float margin = dp(8);
        float gap = dp(2);
        contentW = measuredWidth();
        float barH = height();
        // au-dessus de la sélection (sous elle sans la place) : la
        // sélection demeure à découvert, le caret avec elle
        if (anchorTop - gap - barH >= margin) {
            poseY = anchorTop - gap - barH;
        } else {
            poseY = Math.min(anchorBottom + gap,
                    Math.max(margin, metrics.viewHeight() - barH - margin));
        }
        poseX = Math.max(margin,
                Math.min(centerX - contentW / 2.0f,
                        metrics.viewWidth() - contentW - margin));
        float radius = barH / 2.0f;
        canvas.drawRoundRect(poseX, poseY, poseX + contentW, poseY + barH,
                radius, radius, cardPaint);
        canvas.drawRoundRect(poseX, poseY, poseX + contentW, poseY + barH,
                radius, radius, borderPaint);
        float top = poseY + dp(PAD_Y_DP);
        float cy = top + dp(HEIGHT_DP) / 2.0f;
        float baseline = top + dp(HEIGHT_DP) * 0.68f;
        float cursor = poseX;
        for (int at = 0; at < entries.size(); at++) {
            cursor = drawChip(canvas, entries.get(at), cursor, top, baseline, cy);
        }
        if (!entries.isEmpty()) {
            // le filet séparateur du groupe texte et du groupe icônes
            float ruleX = cursor + dp(GAP_DP) + dp(RULE_DP) / 2.0f;
            canvas.drawLine(ruleX, top + dp(4), ruleX,
                    top + dp(HEIGHT_DP) - dp(4), rulePaint);
            cursor += dp(GAP_DP) + dp(RULE_DP);
            drawDocsIcon(canvas, cursor, cy);
            cursor += dp(ICON_DP);
            drawOverflowPuce(canvas, cursor, cy);
        }
    }

    /**
     * Les pastilles vivantes : Copier, Coller (au presse-papiers
     * près), Tout copier — l'action inutile ne se promène pas.
     */
    private void buildEntries() {
        entries.clear();
        entries.add(new int[] {ACT_COPY, 0});
        if (canPaste) {
            entries.add(new int[] {ACT_PASTE, 0});
        }
        entries.add(new int[] {ACT_COPY_ALL, 0});
    }

    /** Le libellé d'une action. */
    private String labelOf(int action) {
        switch (action) {
            case ACT_COPY:
                return "Copier";
            case ACT_PASTE:
                return "Coller";
            default:
                return "Tout copier";
        }
    }

    /** Une pastille : son libellé au centre de sa respiration. */
    private float drawChip(Canvas canvas, int[] entry, float left,
            float top, float baseline, float cy) {
        float chipW = 2.0f * dp(PAD_X_DP)
                + textPaint.measureText(labelOf(entry[0]));
        canvas.drawRoundRect(left + dp(GAP_DP) / 2.0f, top + dp(2),
                left + chipW - dp(GAP_DP) / 2.0f, top + dp(HEIGHT_DP) - dp(2),
                dp(HEIGHT_DP) / 2.0f, dp(HEIGHT_DP) / 2.0f, chipPaint);
        float labelW = textPaint.measureText(labelOf(entry[0]));
        canvas.drawText(labelOf(entry[0]), left + (chipW - labelW) / 2.0f,
                baseline, textPaint);
        return left + chipW + dp(GAP_DP);
    }

    /** L'icône « i » de documentation : le cercle cerclé, la lettre au centre. */
    private void drawDocsIcon(Canvas canvas, float left, float cy) {
        float cx = left + dp(ICON_DP) / 2.0f;
        glyphPaint.setStrokeWidth(1.1f * density);
        canvas.drawCircle(cx, cy, 5.5f * density, glyphPaint);
        float letterSize = textPaint.getTextSize();
        float letterW = textPaint.measureText("i");
        textPaint.setTextSize(9.5f * density);
        canvas.drawText("i", cx - textPaint.measureText("i") / 2.0f,
                cy + letterW * 0.35f, textPaint);
        textPaint.setTextSize(letterSize);
        glyphPaint.setStrokeWidth(1.4f * density);
    }

    /** La puce « ⋯ » de débordement : trois points à l'encre atténuée. */
    private void drawOverflowPuce(Canvas canvas, float left, float cy) {
        float cx = left + dp(ICON_DP) / 2.0f;
        for (int dot = 0; dot < 3; dot++) {
            float dotY = cy + (dot - 1) * 3.0f * density;
            canvas.drawCircle(cx, dotY, 1.2f * density, glyphPaint);
        }
    }

    /**
     * La largeur du contenu : les pastilles respirées, leur écart, le
     * filet, les deux icônes.
     */
    private float measuredWidth() {
        float width = 0.0f;
        for (int[] entry : entries) {
            width += 2.0f * dp(PAD_X_DP)
                    + textPaint.measureText(labelOf(entry[0])) + dp(GAP_DP);
        }
        return width + dp(RULE_DP) + 2.0f * dp(ICON_DP) + dp(GAP_DP);
    }

    /**
     * L'aire touchée à l'abscisse : la pastille qui la porte, l'icône
     * documentation ou la puce de débordement, -1 si le doigt tombe
     * hors des zones.
     */
    private int areaAt(float x) {
        float cursor = poseX;
        for (int[] entry : entries) {
            float chipW = 2.0f * dp(PAD_X_DP)
                    + textPaint.measureText(labelOf(entry[0]));
            if (x >= cursor && x < cursor + chipW + dp(GAP_DP)) {
                return entry[0];
            }
            cursor += chipW + dp(GAP_DP);
        }
        float iconsStart = cursor + dp(GAP_DP) + dp(RULE_DP);
        if (x >= iconsStart && x < iconsStart + dp(ICON_DP)) {
            return ICON_DOCS;
        }
        float puceStart = iconsStart + dp(ICON_DP);
        if (x >= puceStart && x < puceStart + dp(ICON_DP)) {
            return ICON_OVERFLOW;
        }
        return -1;
    }

    /** L'abscisse du centre d'une pastille. */
    private float chipCenterX(int[] entry) {
        float cursor = poseX;
        for (int[] other : entries) {
            float chipW = 2.0f * dp(PAD_X_DP)
                    + textPaint.measureText(labelOf(other[0]));
            if (other == entry) {
                return cursor + chipW / 2.0f;
            }
            cursor += chipW + dp(GAP_DP);
        }
        return -1.0f;
    }

    private float dp(int value) {
        return value * density;
    }
}
