package jo.pigma.editor.popups;

import android.graphics.Canvas;
import android.graphics.Paint;

import java.util.List;

import jo.pigma.editor.EditorView;

/**
 * La carte de complétion, DESSINÉE DANS LA VUE : les candidats du mot
 * courant en rangées verticales au caret — dessous si elle tient,
 * dessus sinon —, bornée aux murs. Les caractères du libellé qui
 * poursuivent le préfixe tapé passent à l'accent et au gras (le fil
 * flou : sous-séquence gloutonne, cas ignorée). Le candidat courant se
 * surligne ; la flèche verticale déléguée y navigue, la validation
 * déléguée l'insère (le préfixe se remplace par le candidat entier),
 * le doigt sur une rangée l'insère aussi.
 *
 * <p>La carte ne prend jamais le focus : le texte tapé poursuit sa vie
 * vers le tampon — la carte ne montre que ce qu'on lui pose, elle se
 * re-filtre à la trame suivante. Le filtrage vit chez l'éditeur ; la
 * fenêtre de rangées se resserre quand la place manque sous le caret
 * (au moins deux rangées, huit au plus) et défile pour garder le
 * candidat courant visible.</p>
 */
public final class CompletionCard {

    /** Le plafond de rangées montrées quand la place abonde. */
    private static final int MAX_ROWS = 8;

    /** Le plancher de rangées quand la place manque sous le caret. */
    private static final int MIN_ROWS = 2;

    /** La hauteur d'une rangée (dp). */
    private static final int ROW_HEIGHT_DP = 30;

    /** La respiration intérieure de la carte (dp). */
    private static final int PAD_DP = 4;

    /** La largeur plancher de la carte (dp). */
    private static final int MIN_WIDTH_DP = 150;

    /** La largeur plafond de la carte (dp). */
    private static final int MAX_WIDTH_DP = 280;

    /** La marge aux murs de la vue (dp). */
    private static final int MARGIN_DP = 8;

    /** L'écart entre le caret et la carte (dp). */
    private static final int GAP_DP = 2;

    private List<String> words = List.of();

    /** Le préfixe tapé — ses caractères passent à l'accent dans les libellés. */
    private String prefix = "";

    /** L'index du candidat courant, -1 si la carte est vide. */
    private int highlighted = -1;

    /** La première rangée de la fenêtre (le défilement la garde sur le courant). */
    private int topRow;

    private float poseX;

    private float poseY;

    private float cardW;

    private float cardH;

    /** Le nombre de rangées montrées (la place le resserre). */
    private int shownRows;

    private float density = 1.0f;

    private EditorView editor;

    private final Paint cardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint accentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint selectionPaint = new Paint();

    /**
     * Une carte neuve, ses pinceaux propres.
     *
     * @param density la densité de l'écran (le dp en pixels)
     */
    public CompletionCard(float density) {
        cardPaint.setColor(0xFFF6F8FA);
        borderPaint.setColor(0xFFB5B5B5);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(1.0f);
        textPaint.setColor(0xFF202124);
        textPaint.setTextSize(13.0f * density);
        accentPaint.setColor(0xFF1A73E8);
        accentPaint.setTextSize(13.0f * density);
        selectionPaint.setColor(0x1F1A73E8);
    }

    /**
     * Branche la carte à son éditeur : l'insertion y parle (le préfixe
     * s'y remplace par le candidat).
     *
     * @param editor l'éditeur complété
     */
    public void bind(EditorView editor) {
        this.editor = editor;
    }

    /**
     * Pose les candidats du mot courant et le préfixe tapé — le
     * premier candidat devient le courant quand la liste change ; la
     * fenêtre repart du haut. La liste vide range la carte.
     *
     * @param candidates les candidats, déjà filtrés, jamais null
     * @param typedPrefix le préfixe tapé (vide pour aucun surlignage)
     */
    public void set(List<String> candidates, String typedPrefix) {
        List<String> fresh = List.copyOf(candidates);
        boolean changed = !fresh.equals(words);
        words = fresh;
        prefix = typedPrefix == null ? "" : typedPrefix;
        if (changed) {
            highlighted = words.isEmpty() ? -1 : 0;
            topRow = 0;
        }
    }

    /** La carte vit-elle ? (au moins un candidat) */
    public boolean isShown() {
        return !words.isEmpty();
    }

    /** Les candidats montrés. */
    public List<String> words() {
        return words;
    }

    /** Le préfixe tapé. */
    public String prefix() {
        return prefix;
    }

    /** L'index du candidat courant (-1 : la carte est vide). */
    public int highlighted() {
        return highlighted;
    }

    /** La première rangée de la fenêtre courante. */
    public int topRow() {
        return topRow;
    }

    /** Le nombre de rangées montrées. */
    public int shownRows() {
        return shownRows;
    }

    /**
     * La flèche verticale déléguée navigue : le candidat courant
     * avance ou recule, borné aux murs de la liste ; la fenêtre le
     * suit.
     *
     * @param step négatif vers le haut, positif vers le bas
     * @return vrai quand la carte a pris la flèche
     */
    public boolean onArrow(int step) {
        if (words.isEmpty()) {
            return false;
        }
        int target = highlighted < 0 ? 0
                : Math.min(words.size() - 1, Math.max(0, highlighted + step));
        highlighted = target;
        keepCurrentVisible();
        return true;
    }

    /**
     * La validation déléguée insère le candidat courant — le préfixe
     * se remplace par le candidat entier (une unité d'annulation).
     *
     * @return vrai quand la carte a pris la validation
     */
    public boolean onEnter() {
        if (highlighted < 0 || highlighted >= words.size()) {
            return false;
        }
        insert(words.get(highlighted));
        return true;
    }

    /**
     * Le doigt tombé sur la carte lui appartient : la rangée visée
     * insère son candidat ; le reste de la carte se consomme sans
     * agir. Hors de la carte : faux — le geste poursuit sa vie vers
     * l'éditeur.
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
        int row = (int) ((y - poseY - dp(PAD_DP)) / dp(ROW_HEIGHT_DP)) + topRow;
        if (row >= topRow && row < topRow + shownRows && row < words.size()) {
            insert(words.get(row));
        }
        return true;
    }

    /**
     * Dessine la carte : la pose se recalcule d'abord — le caret la
     * suit (défilement, navigation) — puis les rangées de la fenêtre,
     * le candidat courant surligné, les libellés à l'accent sur les
     * caractères qui poursuivent le préfixe.
     *
     * @param canvas  le canevas de la vue
     * @param metrics la géométrie courante de la vue
     */
    public void draw(Canvas canvas, PopupMetrics metrics) {
        density = metrics.density();
        if (words.isEmpty()) {
            cardH = 0.0f;
            shownRows = 0;
            return;
        }
        place(metrics);
        float radius = dp(10);
        canvas.drawRoundRect(poseX, poseY, poseX + cardW, poseY + cardH,
                radius, radius, cardPaint);
        canvas.drawRoundRect(poseX, poseY, poseX + cardW, poseY + cardH,
                radius, radius, borderPaint);
        float rowH = dp(ROW_HEIGHT_DP);
        float pad = dp(PAD_DP);
        for (int at = topRow; at < topRow + shownRows && at < words.size(); at++) {
            float top = poseY + pad + (at - topRow) * rowH;
            if (at == highlighted) {
                canvas.drawRect(poseX + 1.0f, top,
                        poseX + cardW - 1.0f, top + rowH, selectionPaint);
            }
            drawLabel(canvas, ellipsize(words.get(at)),
                    poseX + dp(12), top + rowH * 0.68f);
        }
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
     * @param index l'index du candidat
     * @return l'ordonnée du centre, -1 hors carte
     */
    public float rowCenterY(int index) {
        if (!isShown() || index < topRow || index >= topRow + shownRows) {
            return -1.0f;
        }
        return poseY + dp(PAD_DP)
                + (index - topRow + 0.5f) * dp(ROW_HEIGHT_DP);
    }

    /**
     * Pose la carte au caret — dessous si elle tient, dessus sinon —
     * et resserre la fenêtre de rangées à la place disponible : au
     * moins {@value #MIN_ROWS}, au plus {@value #MAX_ROWS} et jamais
     * plus que les candidats posés.
     */
    private void place(PopupMetrics metrics) {
        float margin = dp(MARGIN_DP);
        float gap = dp(GAP_DP);
        float rowH = dp(ROW_HEIGHT_DP);
        float pad = dp(PAD_DP);
        cardW = Math.min(Math.max(measuredWidth(), dp(MIN_WIDTH_DP)),
                dp(MAX_WIDTH_DP));
        int rows = Math.min(MAX_ROWS, words.size());
        // la place de chaque côté : la carte vit du côté qui en a le
        // plus — le dessous d'abord quand les deux tiennent
        float belowTop = metrics.caretY() + metrics.lineHeight() + gap;
        float belowSpace = metrics.viewHeight() - margin - belowTop;
        float aboveSpace = metrics.caretY() - gap - margin;
        boolean below = belowSpace >= aboveSpace;
        float space = below ? belowSpace : aboveSpace;
        int fitting = (int) Math.floor((space - 2 * pad) / rowH);
        rows = Math.max(MIN_ROWS, Math.min(rows, Math.max(fitting, MIN_ROWS)));
        shownRows = Math.min(rows, words.size());
        cardH = 2 * pad + shownRows * rowH;
        if (below) {
            poseY = Math.min(belowTop,
                    Math.max(margin, metrics.viewHeight() - cardH - margin));
        } else {
            poseY = Math.max(margin, metrics.caretY() - gap - cardH);
        }
        poseX = Math.max(margin,
                Math.min(metrics.caretX(), metrics.viewWidth() - cardW - margin));
        clampWindow();
    }

    /**
     * La largeur qu'il faut pour le plus large libellé, respirations
     * comprises.
     */
    private float measuredWidth() {
        float widest = 0.0f;
        for (String word : words) {
            widest = Math.max(widest, textPaint.measureText(word));
        }
        return widest + 2 * dp(12);
    }

    /**
     * La fenêtre garde le candidat courant visible : elle descend ou
     * remonte d'entier, jamais plus loin que les murs de la liste.
     */
    private void keepCurrentVisible() {
        if (highlighted < topRow) {
            topRow = highlighted;
        } else if (highlighted >= topRow + shownRows) {
            topRow = highlighted - shownRows + 1;
        }
        clampWindow();
    }

    /** La fenêtre vit dans les murs de la liste. */
    private void clampWindow() {
        int maxTop = Math.max(0, words.size() - Math.max(1, shownRows));
        topRow = Math.min(Math.max(0, topRow), maxTop);
    }

    /**
     * Le libellé, caractère par caractère : les positions qui
     * poursuivent le préfixe tapé (sous-séquence gloutonne, cas
     * ignorée) passent à l'accent et au gras, les autres gardent
     * l'encre des libellés.
     */
    private void drawLabel(Canvas canvas, String label, float x, float baseline) {
        boolean[] matched = matchedPositions(label);
        float cursor = x;
        int at = 0;
        while (at < label.length()) {
            boolean isMatch = matched[at];
            int end = at;
            while (end + 1 < label.length() && matched[end + 1] == isMatch) {
                end++;
            }
            String segment = label.substring(at, end + 1);
            Paint paint = isMatch ? accentPaint : textPaint;
            paint.setFakeBoldText(isMatch);
            canvas.drawText(segment, cursor, baseline, paint);
            paint.setFakeBoldText(false);
            cursor += paint.measureText(segment);
            at = end + 1;
        }
    }

    /**
     * Les positions du libellé qui poursuivent le préfixe (sous-
     * séquence gloutonne, cas ignorée).
     */
    private boolean[] matchedPositions(String label) {
        boolean[] matched = new boolean[label.length()];
        if (prefix.isEmpty()) {
            return matched;
        }
        String target = label.toLowerCase();
        String filter = prefix.toLowerCase();
        int inFilter = 0;
        for (int at = 0; at < target.length() && inFilter < filter.length(); at++) {
            if (target.charAt(at) == filter.charAt(inFilter)) {
                matched[at] = true;
                inFilter++;
            }
        }
        return matched;
    }

    /**
     * Coupe le libellé à la largeur de la carte — la queue se garde,
     * la tête s'abrège en « … ».
     */
    private String ellipsize(String label) {
        float available = cardW - 2 * dp(12);
        if (textPaint.measureText(label) <= available || label.length() < 2) {
            return label;
        }
        String tail = label.substring(1);
        while (tail.length() > 1 && textPaint.measureText("…" + tail) > available) {
            tail = tail.substring(1);
        }
        return "…" + tail;
    }

    /**
     * Insère un candidat : le préfixe se remplace par le candidat
     * entier — l'insertion vit chez l'éditeur (une unité d'annulation).
     */
    private void insert(String candidate) {
        if (editor != null) {
            editor.insertCompletion(candidate);
        }
    }

    private float dp(int value) {
        return value * density;
    }
}
