package jo.pigma.editor.popups;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;

import java.util.List;
import java.util.function.Consumer;

/**
 * La carte de SÉLECTEUR DE COULEUR (la leçon de la référence — son
 * ColorPickerView y pose le motif entier) : l'en-tête AVANT/APRÈS
 * (la pastille d'origine et son littéral, la courante et le sien —
 * l'aperçu vit à chaque cran du doigt), le CHAMP
 * saturation-valeur (blanc vers la teinte, transparent vers le
 * noir — le marqueur y vit, double cercle lisible sur tout fond),
 * la BANDE de teinte (le spectre, le curseur à deux traits), la
 * PALETTE Material (seize teintes, la portée liserée à l'accent) —
 * le tap d'une cellule applique, le relâchement du champ aussi.
 *
 * <p>La carte se pose au caret du littéral (#RRGGBB), le fond
 * translucide du style ; l'application remplace le littéral EN
 * PLACE (sa largeur en chiffres tient : la forme courte quantifie
 * au nibble le plus proche).</p>
 */
public final class ColorPickerCard {

    /** La hauteur de l'en-tête des deux pastilles (dp). */
    private static final float HEADER_DP = 34.0f;

    /** La hauteur du champ saturation-valeur (dp). */
    private static final float FIELD_DP = 112.0f;

    /** La hauteur de la bande de teinte (dp). */
    private static final float BAND_DP = 18.0f;

    /** Le côté d'une cellule de palette (dp). */
    private static final float CELL_DP = 22.0f;

    /** L'écart entre deux cellules (dp). */
    private static final float CELL_GAP_DP = 6.0f;

    /** L'écart entre deux blocs voisins (dp). */
    private static final float BLOCK_GAP_DP = 8.0f;

    /** Le côté d'une pastille d'en-tête (dp). */
    private static final float CHIP_DP = 22.0f;

    /** Le rayon d'une pastille ou cellule (dp). */
    private static final float RADIUS_DP = 4.0f;

    /** La marge aux murs de la vue (dp). */
    private static final int MARGIN_DP = 8;

    /** La respiration intérieure de la carte (dp). */
    private static final float PAD_DP = 8.0f;

    /** La palette Material de la référence : deux rangées de huit. */
    private static final int[] PALETTE = {
            0xFFF44336, 0xFFE91E63, 0xFF9C27B0, 0xFF673AB7,
            0xFF3F51B5, 0xFF2196F3, 0xFF009688, 0xFF4CAF50,
            0xFFFFEB3B, 0xFFFFC107, 0xFFFF9800, 0xFF795548,
            0xFF9E9E9E, 0xFF607D8B, 0xFF000000, 0xFFFFFFFF, };

    /** L'état HSV courant — la teinte, la saturation, la valeur. */
    private final float[] hsv = {0.0f, 0.0f, 0.0f};

    /** La couleur d'origine du littéral — son alpha survit au pick. */
    private int original;

    /** La largeur du littéral d'origine (ses chiffres, sans le dièse). */
    private int digits;

    /** Le rappel d'application — la teinte courante y part, opaque. */
    private Consumer<Integer> onApply;

    /** La zone pressée : 0 aucune, 1 le champ SV, 2 la bande. */
    private int pressedZone;

    /** La carte vit-elle ? */
    private boolean shown;

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
    private final Paint chipPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint trimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cursorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fieldPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint veilPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint spectrumPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF corners = new RectF();

    /**
     * Une carte neuve, ses pinceaux propres au style par défaut.
     *
     * @param density la densité de l'écran (le dp en pixels)
     */
    public ColorPickerCard(float density) {
        this.density = density;
        applyStyle();
    }

    /**
     * Pose le style de l'hôte : le fond translucide, les encres, la
     * forme — les pinceaux s'y reconfigurent entiers.
     *
     * @param fresh le style posé, null pour celui par défaut
     */
    public void setStyle(PopupStyle fresh) {
        style = fresh == null ? PopupStyle.defaults() : fresh;
        applyStyle();
    }

    /** Reconfigure les pinceaux au style courant. */
    private void applyStyle() {
        cardPaint.setColor(style.backgroundArgb());
        borderPaint.setColor(style.borderArgb());
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(1.0f);
        textPaint.setColor(style.textArgb());
        textPaint.setTextSize((style.textSizeSp() - 3.0f) * density);
        trimPaint.setStyle(Paint.Style.STROKE);
        cursorPaint.setStyle(Paint.Style.STROKE);
        cardW = 8 * dpF(CELL_DP) + 7 * dpF(CELL_GAP_DP) + 2 * dpF(PAD_DP);
        cardH = dpF(HEADER_DP) + dpF(FIELD_DP) + dpF(BAND_DP)
                + 2 * dpF(CELL_DP) + dpF(CELL_GAP_DP) + 3 * dpF(BLOCK_GAP_DP)
                + 2 * dpF(PAD_DP);
    }

    /**
     * Ouvre le sélecteur sur un littéral : sa couleur d'origine
     * (son alpha survivra au pick) et sa largeur en chiffres.
     *
     * @param literalOrigin la couleur ARGB du littéral d'origine
     * @param literalDigits la largeur du littéral, sans le dièse
     * @param apply         le rappel d'application (la teinte y part)
     */
    public void open(int literalOrigin, int literalDigits,
            Consumer<Integer> apply) {
        original = literalOrigin;
        digits = literalDigits;
        onApply = apply;
        Color.colorToHSV(literalOrigin & 0xFFFFFF, hsv);
        shown = true;
    }

    /** Ferme la carte (l'application, le tap hors carte). */
    public void close() {
        shown = false;
        onApply = null;
        pressedZone = 0;
    }

    /** La carte vit-elle ? */
    public boolean isShown() {
        return shown;
    }

    /** La couleur courante, opaque. */
    public int current() {
        return compose(0xFF);
    }

    /** La couleur courante composée à l'alpha donné. */
    private int compose(int alpha) {
        return (alpha << 24) | (Color.HSVToColor(hsv) & 0xFFFFFF);
    }

    /**
     * Le littéral formaté d'une couleur — la largeur du littéral
     * d'origine tient (ses chiffres), la forme courte quantifie au
     * nibble le plus proche (la leçon de la référence).
     *
     * @param argb   la couleur au format ARGB
     * @param width la largeur voulue (sans le dièse)
     * @return le littéral, dièse compris
     */
    public static String format(int argb, int width) {
        return switch (width) {
            case 3 -> String.format("#%1$x%2$x%3$x",
                    quantize((argb >>> 16) & 0xFF),
                    quantize((argb >>> 8) & 0xFF),
                    quantize(argb & 0xFF));
            case 4 -> String.format("#%1$x%2$x%3$x%4$x",
                    quantize((argb >>> 24) & 0xFF),
                    quantize((argb >>> 16) & 0xFF),
                    quantize((argb >>> 8) & 0xFF),
                    quantize(argb & 0xFF));
            case 6 -> String.format("#%06x", argb & 0xFFFFFF);
            default -> String.format("#%08x", argb);
        };
    }

    /** La composante quantifiée au nibble le plus proche. */
    private static int quantize(int component) {
        return Math.round(component * 15.0f / 255.0f);
    }

    /**
     * Le doigt tombé sur la carte lui appartient : le champ et la
     * bande suivent le doigt (la teinte vit à chaque cran), la
     * cellule de palette applique au tap, le relâchement applique.
     * Hors de la carte : faux.
     *
     * @param x l'abscisse du doigt (px, coordonnées de la vue)
     * @param y l'ordonnée du doigt (px, coordonnées de la vue)
     * @param action l'action du geste (MotionEvent.ACTION_*)
     * @return vrai quand la carte a pris le geste
     */
    public boolean onTouchArea(float x, float y, int action) {
        if (!shown || x < poseX || x >= poseX + cardW
                || y < poseY || y >= poseY + cardH) {
            return false;
        }
        float innerX = x - poseX - dpF(PAD_DP);
        float innerY = y - poseY - dpF(PAD_DP);
        switch (action) {
            case 0 -> { // ACTION_DOWN
                pressedZone = zoneOf(innerX, innerY);
                if (pressedZone != 0) {
                    press(innerX, innerY);
                }
            }
            case 2 -> { // ACTION_MOVE
                if (pressedZone != 0) {
                    press(innerX, innerY);
                }
            }
            case 1 -> { // ACTION_UP
                int cell = cellOf(innerX, innerY);
                if (cell >= 0) {
                    Color.colorToHSV(PALETTE[cell], hsv);
                    apply();
                } else if (pressedZone != 0) {
                    apply();
                }
                pressedZone = 0;
            }
            default -> {
            }
        }
        return true;
    }

    /** La zone du point : 1 le champ, 2 la bande, 0 ailleurs. */
    private int zoneOf(float x, float y) {
        float fieldY = dpF(HEADER_DP) + dpF(BLOCK_GAP_DP);
        float bandY = fieldY + dpF(FIELD_DP) + dpF(BLOCK_GAP_DP);
        if (y >= fieldY && y < fieldY + dpF(FIELD_DP)) {
            return 1;
        }
        if (y >= bandY && y < bandY + dpF(BAND_DP)) {
            return 2;
        }
        return 0;
    }

    /** La cellule de palette du point, -1 hors palette. */
    private int cellOf(float x, float y) {
        float cell = dpF(CELL_DP);
        float gap = dpF(CELL_GAP_DP);
        float paletteY = dpF(HEADER_DP) + dpF(BLOCK_GAP_DP)
                + dpF(FIELD_DP) + dpF(BLOCK_GAP_DP) + dpF(BAND_DP)
                + dpF(BLOCK_GAP_DP);
        if (y < paletteY || x < 0.0f || x > innerWidth()) {
            return -1;
        }
        int column = (int) (x / (cell + gap));
        int row = (int) ((y - paletteY) / (cell + gap));
        if (column > 7 || row > 1
                || x > column * (cell + gap) + cell
                || y > paletteY + row * (cell + gap) + cell) {
            return -1;
        }
        return row * 8 + column;
    }

    /** Le point pressé suit sa zone, borné au rect. */
    private void press(float x, float y) {
        if (pressedZone == 1) {
            hsv[1] = clamp(x / innerWidth());
            hsv[2] = clamp(1.0f - (y - dpF(HEADER_DP) - dpF(BLOCK_GAP_DP))
                    / dpF(FIELD_DP));
        } else if (pressedZone == 2) {
            hsv[0] = clamp(x / innerWidth()) * 360.0f;
        }
    }

    /** L'application : la teinte courante part au rappel. */
    private void apply() {
        if (onApply != null) {
            onApply.accept(compose(0xFF));
        }
    }

    /** La largeur intérieure de la carte. */
    private float innerWidth() {
        return cardW - 2 * dpF(PAD_DP);
    }

    /**
     * Dessine la carte : la pose d'abord (le caret l'ancre), puis
     * l'en-tête AVANT/APRÈS, le champ saturation-valeur, la bande
     * de teinte, la palette.
     *
     * @param canvas  le canevas de la vue
     * @param anchorTop le bord haut de la ligne du littéral (px)
     * @param anchorBottom le bord bas de la ligne du littéral (px)
     * @param metrics la géométrie courante de la vue
     */
    public void draw(Canvas canvas, float anchorTop, float anchorBottom,
            PopupMetrics metrics) {
        if (!shown) {
            return;
        }
        float margin = dp(MARGIN_DP);
        float gap = dp(4);
        float belowTop = anchorBottom + gap;
        if (belowTop + cardH <= metrics.viewHeight() - margin) {
            poseY = belowTop;
        } else {
            poseY = Math.max(margin, anchorTop - gap - cardH);
        }
        poseX = Math.max(margin, Math.min(metrics.caretX(),
                metrics.viewWidth() - cardW - margin));
        float radius = dp(style.cornerRadiusDp());
        canvas.drawRoundRect(poseX, poseY, poseX + cardW, poseY + cardH,
                radius, radius, cardPaint);
        canvas.drawRoundRect(poseX, poseY, poseX + cardW, poseY + cardH,
                radius, radius, borderPaint);
        canvas.save();
        canvas.translate(poseX + dpF(PAD_DP), poseY + dpF(PAD_DP));
        float width = innerWidth();
        drawHeader(canvas, width);
        float fieldY = dpF(HEADER_DP) + dpF(BLOCK_GAP_DP);
        drawField(canvas, width, fieldY);
        float bandY = fieldY + dpF(FIELD_DP) + dpF(BLOCK_GAP_DP);
        drawBand(canvas, width, bandY);
        drawPalette(canvas, bandY + dpF(BAND_DP) + dpF(BLOCK_GAP_DP));
        canvas.restore();
    }

    /** L'en-tête : la pastille d'origine et son littéral, la courante et le sien. */
    private void drawHeader(Canvas canvas, float width) {
        float side = dpF(CHIP_DP);
        float radius = dpF(RADIUS_DP);
        float center = dpF(HEADER_DP) / 2.0f;
        chipPaint.setColor(original);
        corners.set(0.0f, center - side / 2.0f, side, center + side / 2.0f);
        canvas.drawRoundRect(corners, radius, radius, chipPaint);
        traceTrim(canvas, corners, radius);
        canvas.drawText(format(original, digits),
                side + dpF(BLOCK_GAP_DP), baseline(center), textPaint);
        int courante = compose(original >>> 24);
        String label = format(courante, digits);
        float labelW = textPaint.measureText(label);
        chipPaint.setColor(courante);
        corners.set(width - side, center - side / 2.0f,
                width, center + side / 2.0f);
        canvas.drawRoundRect(corners, radius, radius, chipPaint);
        traceTrim(canvas, corners, radius);
        canvas.drawText(label,
                width - side - dpF(BLOCK_GAP_DP) - labelW,
                baseline(center), textPaint);
    }

    /** Le champ saturation-valeur : blanc vers la teinte, transparent vers le noir. */
    private void drawField(Canvas canvas, float width, float y) {
        float height = dpF(FIELD_DP);
        int pureHue = Color.HSVToColor(new float[] {hsv[0], 1.0f, 1.0f});
        fieldPaint.setShader(new LinearGradient(0.0f, 0.0f, width, 0.0f,
                Color.WHITE, pureHue, Shader.TileMode.CLAMP));
        canvas.drawRect(0.0f, y, width, y + height, fieldPaint);
        veilPaint.setShader(new LinearGradient(0.0f, y, 0.0f, y + height,
                Color.TRANSPARENT, Color.BLACK, Shader.TileMode.CLAMP));
        canvas.drawRect(0.0f, y, width, y + height, veilPaint);
        trimPaint.setColor(style.borderArgb());
        trimPaint.setStrokeWidth(1.0f);
        canvas.drawRect(0.0f, y, width, y + height, trimPaint);
        // le marqueur : double cercle, sombre dehors, blanc dedans
        float mx = hsv[1] * width;
        float my = (1.0f - hsv[2]) * height;
        float radius = 6.0f * density;
        cursorPaint.setColor(0xFF1F1F1F);
        cursorPaint.setStrokeWidth(2.0f * density);
        canvas.drawCircle(mx, y + my, radius, cursorPaint);
        cursorPaint.setColor(Color.WHITE);
        cursorPaint.setStrokeWidth(1.5f * density);
        canvas.drawCircle(mx, y + my, radius - 1.5f * density, cursorPaint);
    }

    /** La bande de teinte : le spectre, le curseur à deux traits. */
    private void drawBand(Canvas canvas, float width, float y) {
        float height = dpF(BAND_DP);
        spectrumPaint.setShader(new LinearGradient(0.0f, 0.0f, width, 0.0f,
                new int[] {Color.RED, Color.YELLOW, Color.GREEN,
                        Color.CYAN, Color.BLUE, Color.MAGENTA, Color.RED},
                new float[] {0.0f, 1.0f / 6.0f, 2.0f / 6.0f, 3.0f / 6.0f,
                        4.0f / 6.0f, 5.0f / 6.0f, 1.0f},
                Shader.TileMode.CLAMP));
        canvas.drawRect(0.0f, y, width, y + height, spectrumPaint);
        trimPaint.setColor(style.borderArgb());
        trimPaint.setStrokeWidth(1.0f);
        canvas.drawRect(0.0f, y, width, y + height, trimPaint);
        float x = hsv[0] / 360.0f * width;
        float overflow = 2.0f * density;
        cursorPaint.setColor(0xFF1F1F1F);
        cursorPaint.setStrokeWidth(2.0f);
        canvas.drawLine(x, y - overflow, x, y + height + overflow, cursorPaint);
        cursorPaint.setColor(Color.WHITE);
        cursorPaint.setStrokeWidth(1.0f);
        canvas.drawLine(x, y - overflow, x, y + height + overflow, cursorPaint);
    }

    /** La palette : seize teintes Material, la portée liserée à l'accent. */
    private void drawPalette(Canvas canvas, float y) {
        float side = dpF(CELL_DP);
        float gap = dpF(CELL_GAP_DP);
        float radius = dpF(RADIUS_DP);
        int courante = compose(0xFF) & 0xFFFFFF;
        for (int at = 0; at < PALETTE.length; at++) {
            float x = (at % 8) * (side + gap);
            float cy = y + (at / 8) * (side + gap);
            chipPaint.setColor(PALETTE[at]);
            corners.set(x, cy, x + side, cy + side);
            canvas.drawRoundRect(corners, radius, radius, chipPaint);
            boolean worn = (PALETTE[at] & 0xFFFFFF) == courante;
            trimPaint.setColor(worn ? style.accentArgb() : style.borderArgb());
            trimPaint.setStrokeWidth((worn ? 2.0f : 1.0f) * density);
            canvas.drawRoundRect(corners, radius, radius, trimPaint);
        }
    }

    /** Le liseré d'une pastille. */
    private void traceTrim(Canvas canvas, RectF rect, float radius) {
        trimPaint.setColor(style.borderArgb());
        trimPaint.setStrokeWidth(1.0f);
        canvas.drawRoundRect(rect, radius, radius, trimPaint);
    }

    /** La ligne de base d'un texte centré verticalement. */
    private float baseline(float center) {
        return center + textPaint.measureText("A") * 0.35f;
    }

    /** La valeur bornée à [0, 1]. */
    private static float clamp(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
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

    private float dp(int value) {
        return value * density;
    }

    /** Le dp fractionnaire (les hauteurs de blocs y respirent). */
    private float dpF(float value) {
        return value * density;
    }
}
