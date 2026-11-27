package jo.pigma.editor.popups;

import jo.pigma.editor.EditorView;

import android.graphics.Canvas;
import android.graphics.Paint;

import java.util.List;

/**
 * LA BULLE DE LA SUGGESTION D'IMPORT (la suggestion d’import du rapport des popups — «
 * Importer x.y.Z ? ») : le symbole NON RÉSOLU que le serveur sait
 * importer. La bulle COMPACTE vit AU-DESSUS DU MOT (à la leçon de la
 * pastille du diagnostic — la gouttière porte déjà le marqueur, la
 * correction parle du mot) : l'ampoule partagée au trait, la question
 * « Importer x.y.Z ? », le COMPTE des candidats à droite. Un candidat
 * SEUL s'y pose au doigt ; PLUSIEURS : la bulle s'OUVRE en liste — une
 * rangée par candidat, le choix y pose son remplacement (l'insertion
 * de l'import).
 *
 * <p>Les candidats y viennent des corrections du serveur : le genre
 * du protocole ({@code quickfix.import}) les distingue des correctifs
 * ordinaires — la façade ({@code LspEditor}) y route, la bulle prend
 * les candidats d'import, la carte d'actions rapides garde les autres.
 * La bulle vit de {@link CardBase} : la pose au mot (dessous sans la
 * place au-dessus — jamais sur lui), les murs, la croix, le texte
 * coupé.</p>
 *
 * <p>La carte ne prend jamais le focus ; le choix d'une rangée range
 * la carte AVANT de poser le remplacement (le geste ne se joue pas
 * deux fois).</p>
 */
public final class ImportBubbleCard extends CardBase implements VirtualRows {

    /** La hauteur de la bulle compacte (dp) — la cible tactile y tient
     * à 48 dp au moins (la bulle entière se joue au doigt). */
    private static final int BUBBLE_DP = 48;

    /** La hauteur d'une rangée ouverte (dp). */
    private static final int ROW_DP = 48;

    /** La marge aux murs de la vue (dp). */
    private static final int MARGIN_DP = 8;

    /** L'écart au mot (dp). */
    private static final int GAP_DP = 4;

    /** Le côté de l'ampoule (dp). */
    private static final int ICON_DP = 20;

    /** La colonne de l'ampoule (dp). */
    private static final int ICON_COL_DP = 34;

    /**
     * Un candidat d'import : son titre (celui du correctif du serveur
     * — « Importer x.y.Z du paquet… ») et la pose de son
     * remplacement (l'insertion de la ligne d'import).
     *
     * @param title le titre du candidat
     * @param apply  la pose du remplacement
     */
    public record Import(String title, Runnable apply) {
    }

    /** Les candidats vivants (vide : la bulle se range). */
    private List<Import> candidates = List.of();

    /** Le mot non résolu (la bulle parle de lui). */
    private String word = "";

    /** La bulle est-elle OUVERTE en liste ? */
    private boolean expanded;

    /** Le doigt a-t-il visé la bulle au DOWN ? */
    private boolean pressed;

    /** La rangée armée par le DOWN, -1 : aucune. */
    private int armedRow = -1;

    /** La vue porteuse (la pose s'y exécute hors du geste). */
    private EditorView editor;

    private final Paint cardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bulbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint countPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glyphPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rulePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    /**
     * Une bulle neuve, ses pinceaux propres au style par défaut.
     *
     * @param density la densité de l'écran (le dp en pixels)
     */
    public ImportBubbleCard(float density) {
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
        // l'ampoule au trait, à la teinte de l'ampoule du style (la
        // correction se lit du même œil que l'étage des correctifs)
        bulbPaint.setColor(style.lightbulbArgb());
        bulbPaint.setStyle(Paint.Style.STROKE);
        bulbPaint.setStrokeCap(Paint.Cap.ROUND);
        bulbPaint.setStrokeJoin(Paint.Join.ROUND);
        bulbPaint.setStrokeWidth(1.6f * density);
        textPaint.setColor(style.textArgb());
        textPaint.setTextSize((style.textSizeSp() - 3.0f) * density);
        countPaint.setColor(style.hintArgb());
        countPaint.setTextSize((style.secondarySizeSp() - 1.0f) * density);
        rowPaint.setColor(style.textArgb());
        rowPaint.setTextSize((style.textSizeSp() - 3.0f) * density);
        glyphPaint.setColor(style.accentArgb());
        glyphPaint.setStyle(Paint.Style.STROKE);
        glyphPaint.setStrokeWidth(1.5f * density);
        rulePaint.setColor(style.borderArgb());
    }

    /**
     * Branche la vue porteuse : la pose d'un remplacement s'y exécute
     * (hors du geste — la bulle vit dans la vue).
     *
     * @param view la vue, jamais null
     */
    public void bind(EditorView view) {
        editor = view;
    }

    /**
     * Ouvre la bulle sur les candidats d'un mot — la liste vide range
     * la bulle.
     *
     * @param ofWord     le mot non résolu, jamais null
     * @param candidates les candidats d'import, jamais null
     */
    public void open(String ofWord, List<Import> candidates) {
        word = ofWord == null ? "" : ofWord;
        this.candidates = List.copyOf(candidates);
        expanded = false;
        shown = !this.candidates.isEmpty();
    }

    /** Ramasse les restes de la fermeture. */
    @Override
    protected void onClosed() {
        candidates = List.of();
        word = "";
        expanded = false;
        pressed = false;
        armedRow = -1;
    }

    /** Les candidats vivants (le harnais les lit). */
    public List<Import> imports() {
        return candidates;
    }

    /** Le mot non résolu (le harnais le lit). */
    public String word() {
        return word;
    }

    /** La bulle est-elle ouverte en liste (le harnais le lit) ? */
    public boolean expanded() {
        return expanded;
    }

    /**
     * Le doigt tombé sur la bulle lui appartient : la croix la range ;
     * la bulle COMPACTE s'arme (l'UP pose le candidat seul ou ouvre la
     * liste) ; la RANGÉE d'une bulle ouverte s'arme (l'UP la pose).
     * Hors de la bulle : faux — le geste poursuit sa vie vers
     * l'éditeur.
     *
     * @param x l'abscisse du doigt (px, coordonnées de la vue)
     * @param y l'ordonnée du doigt (px, coordonnées de la vue)
     * @return vrai quand la bulle a pris le geste
     */
    public boolean onTouchArea(float x, float y) {
        if (!isShown() || !contains(x, y)) {
            return false;
        }
        pressed = true;
        // la croix : la bulle se range
        float crossLeft = poseX + cardW - dp(30);
        if (y < poseY + dp(BUBBLE_DP) && x >= crossLeft
                && x <= poseX + cardW - dp(8)) {
            armedRow = -2;
        } else if (expanded && y >= poseY + dp(BUBBLE_DP)) {
            armedRow = (int) ((y - poseY - dp(BUBBLE_DP)) / dp(ROW_DP));
            if (armedRow >= candidates.size()) {
                armedRow = -1;
            }
        } else {
            armedRow = -1;
        }
        return true;
    }

    /**
     * Le relâchement joue la bulle armée : la croix la range, la bulle
     * compacte OUVRÉ la liste (plusieurs candidats) ou POSE le candidat
     * seul, la rangée ouverte POSE son candidat — la pose s'exécute
     * HORS du geste (postée au tour suivant), la bulle se range avant.
     */
    public boolean onRelease() {
        if (!pressed) {
            return false;
        }
        pressed = false;
        if (armedRow == -2) {
            armedRow = -1;
            close();
            return true;
        }
        if (armedRow >= 0) {
            Import chosen = candidates.get(armedRow);
            armedRow = -1;
            close();
            if (chosen != null && chosen.apply() != null && editor != null) {
                editor.post(chosen.apply());
            }
            return true;
        }
        armedRow = -1;
        if (!expanded) {
            if (candidates.size() == 1) {
                Import chosen = candidates.get(0);
                close();
                if (chosen != null && chosen.apply() != null && editor != null) {
                    editor.post(chosen.apply());
                }
            } else {
                // PLUSIEURS : la bulle s'OUVRE en liste (le doigt y
                // choisira — la demande du rapport)
                expanded = true;
            }
        }
        return true;
    }

    /**
     * Dessine la bulle : la pose se recalcule d'abord — au-dessus du
     * mot (dessous sans la place, jamais sur lui) —, puis la bulle
     * compacte (l'ampoule, la question, le compte) ou la liste ouverte
     * (la bulle coiffée, une rangée par candidat).
     *
     * @param canvas       le canevas de la vue
     * @param anchorTop    le bord haut de la ligne du mot (px, vue)
     * @param anchorBottom le bord bas de la ligne du mot (px, vue)
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
        // la bulle compacte : l'ampoule, la question, le compte
        float baseline = poseY + dp(BUBBLE_DP) * 0.66f;
        BulbIcon.draw(canvas, bulbPaint, poseX + dp(ICON_COL_DP) / 2.0f,
                poseY + dp(BUBBLE_DP) / 2.0f, dp(ICON_DP));
        String question = strings.importQuestion(word);
        canvas.drawText(ellipsizeTail(question, textPaint,
                cardW - dp(ICON_COL_DP) - dp(64)),
                poseX + dp(ICON_COL_DP), baseline, textPaint);
        if (!expanded && candidates.size() > 1) {
            String count = strings.candidatesCount(candidates.size());
            float countW = countPaint.measureText(count);
            canvas.drawText(count, poseX + cardW - dp(30) - countW,
                    baseline, countPaint);
        }
        drawCross(canvas, poseX + cardW - dp(24),
                poseY + dp(BUBBLE_DP) / 2.0f, glyphPaint);
        // la liste ouverte : le filet, une rangée par candidat
        if (expanded) {
            canvas.drawLine(poseX + dp(12), poseY + dp(BUBBLE_DP) - dp(2),
                    poseX + cardW - dp(12), poseY + dp(BUBBLE_DP) - dp(2),
                    rulePaint);
            float cursor = poseY + dp(BUBBLE_DP);
            for (int at = 0; at < candidates.size(); at++) {
                drawRow(canvas, candidates.get(at), cursor);
                cursor += dp(ROW_DP);
            }
        }
    }

    /** Une rangée de candidat : l'ampoule, le titre coupé. */
    private void drawRow(Canvas canvas, Import candidate, float top) {
        float cy = top + dp(ROW_DP) / 2.0f;
        BulbIcon.draw(canvas, bulbPaint,
                poseX + dp(ICON_COL_DP) / 2.0f, cy, dp(ICON_DP));
        canvas.drawText(ellipsizeTail(
                candidate.title() == null ? "" : candidate.title(),
                rowPaint, cardW - dp(ICON_COL_DP) - dp(16)),
                poseX + dp(ICON_COL_DP), top + dp(ROW_DP) * 0.62f, rowPaint);
    }

    /**
     * Pose la bulle au-dessus du mot (dessous sans la place — jamais
     * sur lui), le bord gauche du mot borné aux murs.
     */
    private void place(float anchorTop, float anchorBottom,
            PopupMetrics metrics) {
        float margin = dp(MARGIN_DP);
        cardH = dp(BUBBLE_DP) + (expanded
                ? candidates.size() * dp(ROW_DP) : 0.0f);
        float widest = textPaint.measureText(
                "Importer « " + word + " » ?") + dp(ICON_COL_DP) + dp(64);
        for (Import candidate : candidates) {
            widest = Math.max(widest, rowPaint.measureText(
                    candidate.title() == null ? "" : candidate.title())
                    + dp(ICON_COL_DP) + dp(16));
        }
        cardW = Math.min(Math.max(widest, dp(200)),
                Math.min(dp(360), metrics.viewWidth() - 2 * margin));
        // AU-DESSUS du mot d'abord, dessous sans la place
        float aboveBottom = anchorTop - dp(GAP_DP);
        if (aboveBottom - cardH >= margin) {
            poseY = aboveBottom - cardH;
        } else {
            poseY = Math.min(anchorBottom + dp(GAP_DP),
                    metrics.viewHeight() - cardH - margin);
        }
        poseX = Math.max(margin,
                Math.min(metrics.paddingLeft(),
                        metrics.viewWidth() - cardW - margin));
        // LA POSE POSÉE : le flot s'y nourrit (la carte glisse à sa pose
        // nouvelle au lieu d'y sauter — le retour du terrain)
        poseSettled();
    }

    /**
     * LES RANGÉES VIRTUELLES (le service qui parle lit les candidats
     * de la bulle ouverte) — l'interface y vit sur la liste ouverte.
     */
    @Override
    public int virtualRowCount() {
        return isShown() && expanded ? candidates.size() : 0;
    }

    @Override
    public String virtualRowLabel(int index) {
        if (index < 0 || index >= virtualRowCount()) {
            return "";
        }
        Import candidate = candidates.get(index);
        return candidate == null || candidate.title() == null
                ? "" : candidate.title();
    }

    @Override
    public boolean virtualRowActivate(int index) {
        if (index < 0 || index >= virtualRowCount()) {
            return false;
        }
        Import chosen = candidates.get(index);
        close();
        if (chosen != null && chosen.apply() != null && editor != null) {
            editor.post(chosen.apply());
        }
        return true;
    }

    @Override
    public float virtualRowCenterY(int index) {
        return rowCenterY(index);
    }

    /**
     * L'ordonnée du centre d'une rangée ouverte (le harnais y vise le
     * doigt), -1 hors liste.
     *
     * @param index l'indice de la rangée
     * @return le centre vertical (px, vue), -1 si hors liste
     */
    public float rowCenterY(int index) {
        if (!expanded || index < 0 || index >= candidates.size()) {
            return -1.0f;
        }
        return poseY + dp(BUBBLE_DP) + index * dp(ROW_DP) + dp(ROW_DP) / 2.0f;
    }
}
