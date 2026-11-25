package jo.pigma.editor.popups;

import android.graphics.Canvas;
import android.graphics.Paint;

/**
 * La carte de signature, DESSINÉE DANS LA VUE : l'aide aux paramètres
 * d'un appel en cours — la signature de la méthode au-dessus de la
 * ligne du curseur (sous elle sans la place), bornée aux murs. La
 * ligne se rend en TROIS PALIERS : les paramètres déjà frappés à
 * l'encre d'accent, le paramètre ACTIF à l'accent et au gras, ceux à
 * venir en atténué — la structure (nom, parenthèses, virgules) garde
 * l'encre du texte jusqu'à l'actif puis s'atténue avec la queue : la
 * ligne devient sa propre barre de progression.
 *
 * <p>La carte ne prend jamais le focus et ne touche jamais au tampon :
 * la frappe poursuit sa vie vers le document, la carte se relit à la
 * trame suivante (le paramètre actif suit les virgules, la fermeture
 * de l'appel range la carte). Le doigt y est consommé sans agir — la
 * carte flotte au-dessus du texte, pas dessus le doigt.</p>
 */
public final class SignatureCard {

    /** La hauteur de la rangée (dp). */
    private static final int ROW_HEIGHT_DP = 40;

    /** La marge aux murs de la vue (dp). */
    private static final int MARGIN_DP = 8;

    /** L'écart entre la ligne du caret et la carte (dp). */
    private static final int GAP_DP = 2;

    /** La largeur plancher de la carte (dp). */
    private static final int MIN_WIDTH_DP = 240;

    /** La signature montrée (null : la carte est rangée). */
    private String label;

    /** L'index du paramètre actif (celui que la frappe remplit). */
    private int activeParameter;

    private float poseX;

    private float poseY;

    private float cardW;

    private float cardH;

    private float density = 1.0f;

    private final Paint cardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint accentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mutedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    /**
     * Une carte neuve, ses pinceaux propres.
     *
     * @param density la densité de l'écran (le dp en pixels)
     */
    public SignatureCard(float density) {
        cardPaint.setColor(0xFFF6F8FA);
        borderPaint.setColor(0xFFB5B5B5);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(1.0f);
        textPaint.setColor(0xFF202124);
        textPaint.setTextSize(14.0f * density);
        accentPaint.setColor(0xFF1A73E8);
        accentPaint.setTextSize(14.0f * density);
        mutedPaint.setColor(0xFF80868B);
        mutedPaint.setTextSize(14.0f * density);
    }

    /**
     * Pose la signature à montrer — la ligne entière, et l'index du
     * paramètre actif (celui que la frappe remplit). Une étiquette
     * vide ou nulle range la carte.
     *
     * @param shownLabel la signature (la ligne de déclaration,
     *                   coupée de son bloc ouvrant)
     * @param active     l'index du paramètre actif, 0 pour le premier
     */
    public void set(String shownLabel, int active) {
        label = shownLabel == null || shownLabel.isEmpty() ? null : shownLabel;
        activeParameter = Math.max(0, active);
    }

    /** La carte vit-elle ? */
    public boolean isShown() {
        return label != null;
    }

    /** La signature montrée (null : la carte est rangée). */
    public String label() {
        return label;
    }

    /** L'index du paramètre actif. */
    public int activeParameter() {
        return activeParameter;
    }

    /**
     * Le doigt tombé sur la carte s'y consomme sans agir — la carte
     * flotte au-dessus de la ligne frappée, le doigt qui s'y pose ne
     * doit pas déplacer le curseur. Hors de la carte : faux — le
     * geste poursuit sa vie vers l'éditeur.
     *
     * @param x l'abscisse du doigt (px, coordonnées de la vue)
     * @param y l'ordonnée du doigt (px, coordonnées de la vue)
     * @return vrai quand la carte a pris le geste
     */
    public boolean onTouchArea(float x, float y) {
        return isShown() && x >= poseX && x < poseX + cardW
                && y >= poseY && y < poseY + cardH;
    }

    /**
     * Dessine la carte : la pose se recalcule d'abord — le caret la
     * suit (défilement, navigation) — puis la signature en trois
     * paliers, la structure en barre de progression.
     *
     * @param canvas  le canevas de la vue
     * @param metrics la géométrie courante de la vue
     */
    public void draw(Canvas canvas, PopupMetrics metrics) {
        density = metrics.density();
        if (label == null) {
            cardH = 0.0f;
            return;
        }
        place(metrics);
        float radius = dp(10);
        canvas.drawRoundRect(poseX, poseY, poseX + cardW, poseY + cardH,
                radius, radius, cardPaint);
        canvas.drawRoundRect(poseX, poseY, poseX + cardW, poseY + cardH,
                radius, radius, borderPaint);
        drawSignature(canvas, poseX + dp(12), poseY + dp(ROW_HEIGHT_DP) * 0.68f);
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
     * Pose la carte au-dessus de la ligne du caret (l'aide aux
     * paramètres vit au-dessus de la ligne frappée — sous elle sans la
     * place), l'abscisse du caret bornée aux murs.
     */
    private void place(PopupMetrics metrics) {
        float margin = dp(MARGIN_DP);
        float gap = dp(GAP_DP);
        cardW = Math.min(Math.max(textPaint.measureText(label) + 2 * dp(12),
                dp(MIN_WIDTH_DP)),
                metrics.viewWidth() - 2 * margin);
        cardH = dp(ROW_HEIGHT_DP);
        float aboveBottom = metrics.caretY() - gap;
        float aboveTop = aboveBottom - cardH;
        if (aboveTop >= margin) {
            poseY = aboveTop;
        } else {
            poseY = Math.min(metrics.caretY() + metrics.lineHeight() + gap,
                    Math.max(margin, metrics.viewHeight() - cardH - margin));
        }
        poseX = Math.max(margin,
                Math.min(metrics.caretX(), metrics.viewWidth() - cardW - margin));
    }

    /**
     * La signature, segment par segment : les plages de paramètres
     * se découpent aux virgules de profondeur nulle entre les
     * parenthèses extérieures — chaque paramètre déjà frappé à
     * l'accent, l'actif à l'accent et au gras, ceux à venir en
     * atténué ; la structure garde l'encre du texte jusqu'à l'actif
     * puis s'atténue avec la queue.
     */
    private void drawSignature(Canvas canvas, float x, float baseline) {
        int open = label.indexOf('(');
        int close = label.lastIndexOf(')');
        if (open < 0 || close <= open) {
            cut(canvas, label, x, baseline, textPaint, false);
            return;
        }
        int[][] ranges = parameterRanges(open, close);
        int active = Math.max(0, Math.min(activeParameter, ranges.length - 1));
        float cursor = x;
        // l'ouverture : le nom de la méthode et sa parenthèse
        cursor = cut(canvas, label.substring(0, open + 1), cursor, baseline,
                textPaint, false);
        int at = open + 1;
        for (int param = 0; param < ranges.length; param++) {
            if (ranges[param][0] > at) {
                cursor = cut(canvas, label.substring(at, ranges[param][0]),
                        cursor, baseline,
                        param <= active ? textPaint : mutedPaint, false);
            }
            boolean isActive = param == active;
            cursor = cut(canvas, label.substring(ranges[param][0], ranges[param][1]),
                    cursor, baseline,
                    param <= active ? accentPaint : mutedPaint, isActive);
            at = ranges[param][1];
        }
        if (at < label.length()) {
            cut(canvas, label.substring(at), cursor, baseline, mutedPaint, false);
        }
    }

    /**
     * Les plages des paramètres [début, fin) — séparées par les
     * virgules de profondeur nulle entre les parenthèses
     * extérieures, sans leurs espaces de bord ; les plages vides
     * (l'appel sans argument) s'ignorent.
     */
    private int[][] parameterRanges(int open, int close) {
        java.util.ArrayList<int[]> ranges = new java.util.ArrayList<>();
        int depth = 0;
        int start = open + 1;
        for (int at = open + 1; at < close; at++) {
            char c = label.charAt(at);
            if (c == '(' || c == '[' || c == '{') {
                depth++;
            } else if (c == ')' || c == ']' || c == '}') {
                depth--;
            } else if (c == ',' && depth == 0) {
                addTrimmed(ranges, start, at);
                start = at + 1;
            }
        }
        addTrimmed(ranges, start, close);
        return ranges.toArray(new int[0][]);
    }

    /** Ajoute la plage sans ses espaces de bord — une plage vide s'ignore. */
    private void addTrimmed(java.util.ArrayList<int[]> ranges, int start, int end) {
        while (start < end && Character.isWhitespace(label.charAt(start))) {
            start++;
        }
        while (end > start && Character.isWhitespace(label.charAt(end - 1))) {
            end--;
        }
        if (start < end) {
            ranges.add(new int[] {start, end});
        }
    }

    /**
     * Coupe un segment à la place restante (la queue s'abrège en
     * « … » — la signature reste dans la carte) et le peint.
     */
    private float cut(Canvas canvas, String segment, float x, float baseline,
            Paint paint, boolean bold) {
        if (segment.isEmpty()) {
            return x;
        }
        paint.setFakeBoldText(bold);
        canvas.drawText(segment, x, baseline, paint);
        paint.setFakeBoldText(false);
        return x + paint.measureText(segment);
    }

    private float dp(int value) {
        return value * density;
    }
}
