package jo.pigma.editor.popups;

import android.graphics.Canvas;
import android.graphics.Paint;

import java.util.ArrayList;
import java.util.List;

/**
 * La carte de signature, DESSINÉE DANS LA VUE : l'aide aux paramètres
 * d'un appel en cours — les SURCHARGES du serveur y vivent (le défaut
 * du rapport des popups : l'adaptation d'avant gardait la signature
 * ACTIVE seule, les autres mouraient en chemin). La signature montrée
 * se rend en TROIS PALIERS : les paramètres déjà frappés à l'encre
 * d'accent, le paramètre ACTIF à l'accent et au gras, ceux à venir en
 * atténué — la structure (nom, parenthèses, virgules) garde l'encre
 * du texte jusqu'à l'actif puis s'atténue avec la queue : la ligne
 * devient sa propre barre de progression. La documentation du
 * paramètre ACTIF se montre sous la ligne (le serveur la porte
 * souvent — le doigt qui veut lire la retrouve) ; les surcharges se
 * parcourent : le SÉLECTEUR « i sur N » à droite de la ligne, le tap
 * à gauche de la carte recule, le tap à droite avance (la carte ne
 * prend jamais le focus, le clavier reste à l'éditeur).
 */
public final class SignatureCard {

    /** La marge aux murs de la vue (dp). */
    private static final int MARGIN_DP = 8;

    /** L'écart entre la ligne du caret et la carte (dp). */
    private static final int GAP_DP = 2;

    /** Le plafond de rangées de la documentation du paramètre actif. */
    private static final int DOC_MAX_ROWS = 3;

    /** La respiration intérieure de la carte (dp). */
    private static final int PAD_DP = 6;

    /**
     * Une surcharge : sa ligne de signature (le nom appelé, ses
     * paramètres entre parenthèses) et les documentations de ses
     * paramètres (celle du paramètre ACTIF se montre — le serveur la
     * porte par paramètre).
     *
     * @param label          la ligne de la signature
     * @param parameterDocs  la documentation de chaque paramètre (vide
     *                       pour aucune — la rangée se tait)
     */
    public record Overload(String label, List<String> parameterDocs) {

        /** La surcharge normalisée : la liste jamais nulle. */
        public Overload {
            parameterDocs = parameterDocs == null ? List.of() : List.copyOf(parameterDocs);
        }
    }

    /** Les surcharges montrées (vide : la carte est rangée). */
    private List<Overload> overloads = List.of();

    /** L'index de la surcharge MONTRÉE (le sélecteur la déplace). */
    private int shownSignature;

    /** L'index du paramètre actif (celui que la frappe remplit). */
    private int activeParameter;

    private float poseX;

    private float poseY;

    private float cardW;

    private float cardH;

    private float density = 1.0f;

    /** Le style posé par l'hôte : fond translucide, encres, tailles, forme. */
    private PopupStyle style = PopupStyle.defaults();

    private final Paint cardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint accentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mutedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint selectorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    /**
     * Une carte neuve, ses pinceaux propres au style par défaut.
     *
     * @param density la densité de l'écran (le dp en pixels)
     */
    public SignatureCard(float density) {
        this.density = density;
        applyStyle();
    }

    /**
     * Pose le style de l'hôte : le fond translucide, les encres, la
     * taille des textes, la forme — les pinceaux s'y reconfigurent.
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
        cardPaint.setColor(style.backgroundArgb());
        borderPaint.setColor(style.borderArgb());
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(1.0f);
        textPaint.setColor(style.textArgb());
        textPaint.setTextSize(style.textSizeSp() * density);
        accentPaint.setColor(style.accentArgb());
        accentPaint.setTextSize(style.textSizeSp() * density);
        mutedPaint.setColor(style.hintArgb());
        mutedPaint.setTextSize(style.secondarySizeSp() * density);
        selectorPaint.setColor(style.accentArgb());
        selectorPaint.setTextSize(style.secondarySizeSp() * density);
    }

    /**
     * Pose l'aide aux paramètres ENTIÈRE : les surcharges du serveur,
     * l'index de la signature ACTIVE (le sélecteur y naît — le tap
     * y déplace, la carte suivante y revient) et le paramètre actif.
     * Une liste vide ou nulle range la carte.
     *
     * @param served          les surcharges, jamais null
     * @param activeSignature l'index de la signature active (0 sans
     *                        désignation — hors bornes : la première)
     * @param active          l'index du paramètre actif, 0 pour le premier
     */
    public void set(List<Overload> served, int activeSignature, int active) {
        overloads = served == null ? List.of() : List.copyOf(served);
        activeParameter = Math.max(0, active);
        if (overloads.isEmpty()) {
            shownSignature = 0;
            return;
        }
        // l'index du serveur peut dériver (hors bornes) : la PREMIÈRE
        // y vit — jamais l'effondrement
        shownSignature = activeSignature >= 0 && activeSignature < overloads.size()
                ? activeSignature : 0;
    }

    /** La carte vit-elle ? (au moins une surcharge) */
    public boolean isShown() {
        return !overloads.isEmpty();
    }

    /** La signature montrée (null : la carte est rangée). */
    public String label() {
        return overloads.isEmpty() ? null : overloads.get(shownSignature).label();
    }

    /** Le nombre de surcharges portées. */
    public int overloadCount() {
        return overloads.size();
    }

    /** L'index de la surcharge montrée (le sélecteur la déplace). */
    public int shownSignature() {
        return shownSignature;
    }

    /** L'index du paramètre actif. */
    public int activeParameter() {
        return activeParameter;
    }

    /**
     * La documentation du paramètre actif de la surcharge MONTRÉE (le
     * harnais la lit ; vide quand le serveur n'en porte pas).
     */
    public String parameterDocumentation() {
        if (overloads.isEmpty()) {
            return "";
        }
        Overload shown = overloads.get(shownSignature);
        if (activeParameter >= shown.parameterDocs().size()) {
            return "";
        }
        return shown.parameterDocs().get(activeParameter);
    }

    /**
     * Le doigt tombé sur la carte s'y consomme : le tap à GAUCHE
     * recule la surcharge montrée, le tap à DROITE l'avance (une
     * surcharge seule se consomme sans agir — le doigt ne doit pas
     * déplacer le curseur, la carte flotte au-dessus de la ligne
     * frappée). Hors de la carte : faux — le geste poursuit sa vie
     * vers l'éditeur.
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
        if (overloads.size() > 1) {
            cycle(x < poseX + cardW / 2.0f ? -1 : 1);
        }
        return true;
    }

    /**
     * Le sélecteur déplace la surcharge montrée : le cycle BOUCLE
     * (la dernière repart à la première — le parcours ne meurt pas
     * aux murs).
     *
     * @param step négatif vers la première, positif vers la dernière
     */
    public void cycle(int step) {
        if (overloads.isEmpty()) {
            return;
        }
        int size = overloads.size();
        shownSignature = ((shownSignature + step) % size + size) % size;
    }

    /**
     * Dessine la carte : la pose se recalcule d'abord — le caret la
     * suit (défilement, navigation) — puis la signature montrée en
     * trois paliers, le sélecteur « i sur N » à droite quand le
     * serveur porte des surcharges, et la documentation du paramètre
     * actif sous la ligne quand le serveur la porte.
     *
     * @param canvas  le canevas de la vue
     * @param metrics la géométrie courante de la vue
     */
    public void draw(Canvas canvas, PopupMetrics metrics) {
        density = metrics.density();
        if (overloads.isEmpty()) {
            cardH = 0.0f;
            return;
        }
        place(metrics);
        float radius = dp(style.cornerRadiusDp());
        canvas.drawRoundRect(poseX, poseY, poseX + cardW, poseY + cardH,
                radius, radius, cardPaint);
        canvas.drawRoundRect(poseX, poseY, poseX + cardW, poseY + cardH,
                radius, radius, borderPaint);
        float pad = dp(PAD_DP);
        float lineH = dp(style.rowHeightDp() - 10);
        String selector = selectorText();
        float selectorW = selector.isEmpty() ? 0.0f
                : selectorPaint.measureText(selector);
        drawSignature(canvas, poseX + pad,
                poseY + pad + lineH * 0.68f,
                cardW - 2 * pad - selectorW - dp(6));
        if (!selector.isEmpty()) {
            selectorPaint.setFakeBoldText(true);
            canvas.drawText(selector,
                    poseX + cardW - pad - selectorW,
                    poseY + pad + lineH * 0.68f, selectorPaint);
            selectorPaint.setFakeBoldText(false);
        }
        drawParameterDocumentation(canvas, poseX + pad,
                poseY + pad + lineH, cardW - 2 * pad);
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

    /** Le texte du sélecteur (vide pour une surcharge seule). */
    private String selectorText() {
        if (overloads.size() <= 1) {
            return "";
        }
        return (shownSignature + 1) + "/" + overloads.size();
    }

    /**
     * Pose la carte au-dessus de la ligne du caret (l'aide aux
     * paramètres vit au-dessus de la ligne frappée — sous elle sans la
     * place), l'abscisse du caret bornée aux murs ; la hauteur suit
     * le contenu (la ligne, la documentation du paramètre actif).
     */
    private void place(PopupMetrics metrics) {
        float margin = dp(MARGIN_DP);
        float gap = dp(GAP_DP);
        float pad = dp(PAD_DP);
        float lineH = dp(style.rowHeightDp() - 10);
        String label = overloads.get(shownSignature).label();
        float labelW = textPaint.measureText(label);
        String selector = selectorText();
        float selectorW = selector.isEmpty() ? 0.0f
                : selectorPaint.measureText(selector) + dp(6);
        cardW = Math.min(Math.max(labelW + selectorW + 2 * pad,
                dp(style.minCardWidthDp() - 80)),
                metrics.viewWidth() - 2 * margin);
        float docH = documentationHeight(cardW - 2 * pad);
        cardH = pad + lineH + docH + pad;
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

    /** La hauteur de la rangée de documentation du paramètre actif. */
    private float documentationHeight(float width) {
        String documentation = parameterDocumentation();
        if (documentation.isEmpty()) {
            return 0.0f;
        }
        return wrap(documentation, mutedPaint, width).size()
                * (style.textSizeSp() * density) + dp(4);
    }

    /**
     * La documentation du paramètre actif : repliée à la largeur, le
     * plafond borne la carte (la queue s'abrège en « … »).
     */
    private void drawParameterDocumentation(Canvas canvas, float x, float top,
            float width) {
        String documentation = parameterDocumentation();
        if (documentation.isEmpty()) {
            return;
        }
        float rowH = style.textSizeSp() * density;
        List<String> rows = wrap(documentation, mutedPaint, width);
        if (rows.size() > DOC_MAX_ROWS) {
            rows = new ArrayList<>(rows.subList(0, DOC_MAX_ROWS));
            String last = rows.get(DOC_MAX_ROWS - 1);
            while (last.length() > 1
                    && mutedPaint.measureText(last + "\u2026") > width) {
                last = last.substring(0, last.length() - 1);
            }
            rows.set(DOC_MAX_ROWS - 1, last + "\u2026");
        }
        float baseline = top + rowH * 0.8f;
        for (String row : rows) {
            canvas.drawText(row, x, baseline, mutedPaint);
            baseline += rowH;
        }
    }

    /**
     * La signature, segment par segment : les plages de paramètres
     * se découpent aux virgules de profondeur nulle entre les
     * parenthèses extérieures — chaque paramètre déjà frappé à
     * l'accent, l'actif à l'accent et au gras, ceux à venir en
     * atténué ; la structure garde l'encre du texte jusqu'à l'actif
     * puis s'atténue avec la queue. La queue qui ne tient pas la
     * place s'abrège en « … ».
     */
    private void drawSignature(Canvas canvas, float x, float baseline,
            float room) {
        String label = overloads.get(shownSignature).label();
        if (textPaint.measureText(label) > room) {
            while (label.length() > 1
                    && textPaint.measureText(label + "\u2026") > room) {
                label = label.substring(0, label.length() - 1);
            }
            label = label + "\u2026";
        }
        int open = label.indexOf('(');
        int close = label.lastIndexOf(')');
        if (open < 0 || close <= open) {
            cut(canvas, label, x, baseline, textPaint, false);
            return;
        }
        int[][] ranges = parameterRanges(label, open, close);
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
    private int[][] parameterRanges(String label, int open, int close) {
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
                addTrimmed(ranges, label, start, at);
                start = at + 1;
            }
        }
        addTrimmed(ranges, label, start, close);
        return ranges.toArray(new int[0][]);
    }

    /** Ajoute la plage sans ses espaces de bord — une plage vide s'ignore. */
    private void addTrimmed(java.util.ArrayList<int[]> ranges, String label,
            int start, int end) {
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
     * Peint un segment et rend sa largeur (le gras se pose le temps
     * du segment).
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

    /** Replie le texte au mot à la largeur (le harnais y lit la coupe). */
    private List<String> wrap(String text, Paint paint, float maxWidth) {
        List<String> rows = new ArrayList<>();
        StringBuilder row = new StringBuilder();
        int at = 0;
        while (at < text.length()) {
            int wordEnd = at;
            while (wordEnd < text.length()
                    && !Character.isWhitespace(text.charAt(wordEnd))) {
                wordEnd++;
            }
            String word = text.substring(at, wordEnd);
            String candidate = row.length() == 0 ? word : row + " " + word;
            if (paint.measureText(candidate) <= maxWidth || row.length() == 0) {
                row.replace(0, row.length(), candidate);
            } else {
                rows.add(row.toString());
                row.setLength(0);
                row.append(word);
            }
            at = wordEnd + 1;
        }
        if (row.length() > 0) {
            rows.add(row.toString());
        }
        return rows;
    }

    private float dp(int value) {
        return value * density;
    }
}
