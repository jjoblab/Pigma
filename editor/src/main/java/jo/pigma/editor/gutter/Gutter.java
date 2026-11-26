package jo.pigma.editor.gutter;

import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

import jo.pigma.editor.popups.PopupStyle;

/**
 * La gouttière (le paquet du même nom, l'arborescence du prompt) : le
 * numéro de chaque ligne visible, aligné à droite juste avant la zone
 * du marqueur de pli — et la colonne des MARQUEURS à gauche des
 * numéros : le rond rouge de l'erreur, le triangle ambre de
 * l'avertissement, l'ampoule jaune des actions rapides de la ligne du
 * curseur. Les pinceaux y vivent (tailles, couleurs du style posé par
 * l'hôte) ; la vue ne fournit que la géométrie à la trame — la
 * baseline de la ligne, les bords du champ des numéros et de la
 * colonne des marqueurs.
 *
 * <p>La gouttière porte son ÉCRAN à elle : le fond translucide du
 * thème (l'alpha porte la translucence — le texte s'y lit dessous)
 * et le filet de bord qui la sépare du corps, une petite ombre
 * portée au bord droit (l'élévation du panneau flottant — la
 * gouttière flotte au-dessus du texte, comme une carte). Le panneau
 * se peint AVANT les numéros, à la trame, sur toute la hauteur de
 * la fenêtre visible.</p>
 *
 * <p>Au-delà de quatre chiffres le numéro se tronque honnêtement (les
 * trois derniers, précédés d'une ellipse) : la gouttière ne s'élargit
 * pas avec le document, la mise en page ne reflow pas au millième
 * numéro.</p>
 *
 * <p>Le corps des numéros SUIT LE ZOOM : le pincement qui agrandit le
 * texte y agrandit la gouttière pareillement (le facteur du corps
 * courant sur le corps de repos — la gouttière reste à l'échelle de
 * la lecture, pas de l'écran).</p>
 */
public final class Gutter {

    private static final int MAX_DIGITS = 4;

    /** Le code du marqueur d'erreur (le rond rouge barré). */
    public static final int MARKER_ERROR = 1;

    /** Le code du marqueur d'avertissement (le triangle ambre). */
    public static final int MARKER_WARNING = 2;

    /** Le code du marqueur d'ampoule (les actions rapides de la ligne). */
    public static final int MARKER_LIGHTBULB = 3;

    /** Le code du marqueur de point d'arrêt (le rond plein de la ligne). */
    public static final int MARKER_BREAKPOINT = 4;

    /**
     * Le code du marqueur d'APERÇU (la leçon de la référence : son
     * ComposePreviewManager y pose une icône au gutter de chaque
     * fonction @Preview — le carré arrondi à la petite fente, la
     * teinte accent de Compose).
     */
    public static final int MARKER_PREVIEW = 5;

    /** Le pli REPLIÉ : le carré au plus (la référence le dessine ainsi). */
    public static final int FOLD_COLLAPSED = 10;

    /** Le pli OUVERT, borne HAUTE : le chevron pointé vers le bas. */
    public static final int FOLD_EXPANDED_TOP = 11;

    /** Le pli OUVERT, borne BASSE : le chevron pointé vers le haut. */
    public static final int FOLD_EXPANDED_BOTTOM = 12;

    /** Le côté d'un chevron de pli (dp) — la référence : min(4, lineHeight/2-2)*2. */
    private static final int FOLD_SIDE_DP = 12;

    /** Le filet vertical du pli ouvert (la ligne pointillée entre les chevrons). */
    private final Paint foldLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    /** L'encre des chevrons de pli (le gris discret de la référence). */
    private final Paint foldPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    /** Le fond des chevrons (le panneau les perfore — la référence remplit son polygone). */
    private final Paint foldFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    /** Le côté d'un marqueur (dp). */
    private static final int MARKER_SIDE_DP = 18;

    /** Le corps de repos des numéros (dp) — 15 : la référence les pose plus lisibles. */
    private static final float NUMBER_SIZE_DP = 15.0f;

    /** L'alpha du panneau de la gouttière (45 pour cent — discret). */
    private static final float PANEL_ALPHA = 0.45f;

    /** La largeur de l'ombre d'élévation au bord droit (dp). */
    private static final int SHADOW_WIDTH_DP = 4;

    /** Les marches de l'ombre (le dégradé de l'élévation). */
    private static final int SHADOW_STEPS = 2;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Paint markerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Paint markerGlyph = new Paint(Paint.ANTI_ALIAS_FLAG);

    /** Le panneau : le fond translucide de la gouttière. */
    private final Paint panelPaint = new Paint();

    /** Le filet du bord droit du panneau. */
    private final Paint panelBorder = new Paint(Paint.ANTI_ALIAS_FLAG);

    /** L'ombre d'élévation, une marche par trait. */
    private final Paint shadowPaint = new Paint();

    /** Le rectangle du panneau, réutilisé à la trame (aucune allocation). */
    private final RectF panelRect = new RectF();

    /** Le style des popups porte les teintes des marqueurs. */
    private PopupStyle style = PopupStyle.defaults();

    private float density = 1.0f;

    /** Le facteur de zoom courant (1 : le corps de repos). */
    private float zoom = 1.0f;

    /**
     * Une gouttière neuve, son pinceau propre.
     *
     * @param density la densité de l'écran (le dp en pixels)
     */
    public Gutter(float density) {
        this.density = density;
        paint.setTextSize(12.0f * density);
        paint.setColor(0xFF8A8A8E);
        markerGlyph.setStyle(Paint.Style.STROKE);
        markerGlyph.setStrokeWidth(1.6f * density);
        markerGlyph.setColor(0xFFFFFFFF);
        panelPaint.setStyle(Paint.Style.FILL);
        panelBorder.setStyle(Paint.Style.STROKE);
        panelBorder.setStrokeWidth(1.0f * density);
        panelBorder.setColor(0x14000000);
        shadowPaint.setStyle(Paint.Style.FILL);
        foldPaint.setStyle(Paint.Style.STROKE);
        foldPaint.setStrokeWidth(1.4f * density);
        foldPaint.setColor(0xFF9E9E9E);
        foldFillPaint.setStyle(Paint.Style.FILL);
        foldLinePaint.setStyle(Paint.Style.STROKE);
        foldLinePaint.setStrokeWidth(1.0f * density);
        foldLinePaint.setColor(0x509E9E9E);
        foldLinePaint.setPathEffect(new DashPathEffect(
                new float[] { 2.5f * density, 3.0f * density }, 0.0f));
        applyStyle();
    }

    /**
     * Dessine un CHEVRON DE PLI à la référence (EditorGutterComponentImpl
     * y dessine drawSquareWithPlus et drawDirectedBox) : le pli REPLIÉ
     * est un carré barré d'un plus, le pli OUVERT porte DEUX chevrons
     * — haut à la ligne d'ouverture, bas à la ligne de fermeture —
     * joints par un filet vertical pointillé.
     *
     * @param canvas le canevas de la trame
     * @param kind   FOLD_COLLAPSED, FOLD_EXPANDED_TOP ou FOLD_EXPANDED_BOTTOM
     * @param cx     l'abscisse du centre de la colonne des plis (px)
     * @param cy     l'ordonnée du CENTRE de la ligne (px)
     */
    public void drawFoldAnchor(Canvas canvas, int kind, float cx, float cy) {
        float side = dp(FOLD_SIDE_DP);
        float half = side / 2.0f;
        if (kind == FOLD_COLLAPSED) {
            // le carré au PLUS (drawSquareWithPlus de la référence) :
            // un carré au filet, la barre horizontale du moins, la
            // barre verticale du plus
            float top = cy - half;
            foldFillPaint.setColor(0x00000000);
            canvas.drawRect(cx - half, top, cx + half, top + side, foldFillPaint);
            canvas.drawRect(cx - half, top, cx + half, top + side, foldPaint);
            canvas.drawLine(cx - half + dp(2), cy, cx + half - dp(2), cy, foldPaint);
            canvas.drawLine(cx, cy - half + dp(2), cx, cy + half - dp(2), foldPaint);
        } else if (kind == FOLD_EXPANDED_TOP) {
            // le chevron pointé vers le bas (drawDirectedBox) : la
            // flèche coiffée d'un moins — la référence y voit « le
            // haut du pli ouvert »
            float top = cy - half;
            drawDirectedBox(canvas, cx, top, side, side, side - dp(2));
        } else if (kind == FOLD_EXPANDED_BOTTOM) {
            // le chevron pointé vers le haut, à la ligne de fermeture
            float bottom = cy + half;
            drawDirectedBox(canvas, cx, bottom, side, -side, -side + dp(2));
        }
    }

    /**
     * Le polygone du chevron de la référence (drawDirectedBox) : un
     * trapèze pointé, rempli du fond, au filet — le moins y barre la
     * base.
     */
    private void drawDirectedBox(Canvas canvas, float cx, float y,
            float width, float height, float baseHeight) {
        float half = width / 2.0f;
        Path box = new Path();
        box.moveTo(cx - half, y);
        box.lineTo(cx + half, y);
        box.lineTo(cx + half, y + baseHeight);
        box.lineTo(cx, y + height);
        box.lineTo(cx - half, y + baseHeight);
        box.close();
        foldFillPaint.setColor(0x00000000);
        canvas.drawPath(box, foldFillPaint);
        canvas.drawPath(box, foldPaint);
        // le MOINS à mi-base (la référence y barre la base du chevron)
        float minusY = y + baseHeight / 2.0f + (height - baseHeight) / 4.0f;
        canvas.drawLine(cx - half + dp(2), minusY, cx + half - dp(2), minusY,
                foldPaint);
    }

    /**
     * Le FILET VERTICAL du pli ouvert (paintFoldingLines de la
     * référence) : la ligne pointillée qui joint le chevron haut au
     * chevron bas, à la colonne des plis.
     *
     * @param canvas le canevas de la trame
     * @param cx     l'abscisse de la colonne des plis (px)
     * @param fromY  l'ordonnée du bas du chevron haut (px)
     * @param toY    l'ordonnée du haut du chevron bas (px)
     */
    public void drawFoldLine(Canvas canvas, float cx, float fromY, float toY) {
        if (toY > fromY) {
            canvas.drawLine(cx, fromY, cx, toY, foldLinePaint);
        }
    }

    /**
     * La colonne des plis demande sa largeur (le chevron y respire) :
     * la référence y met le chevron plus deux pixels.
     *
     * @return la largeur de la colonne des plis (px)
     */
    public float foldColumnWidth() {
        return dp(FOLD_SIDE_DP) + dp(2);
    }

    /**
     * La couleur des numéros — le thème l'appelle (le fond décide :
     * gris discret sur clair, gris clair sur sombre).
     *
     * @param color la couleur du texte des numéros
     */
    public void setTextColor(int color) {
        paint.setColor(color);
    }

    /**
     * Le zoom du corps de l'éditeur : le corps des numéros le suit
     * (le pincement agrandit le texte, la gouttière y reste à
     * l'échelle de la lecture — facteur 1 au corps de repos).
     *
     * @param factor le facteur de zoom (le corps courant sur le
     *               corps de repos, jamais négatif)
     */
    public void setTextZoom(float factor) {
        zoom = Math.max(0.5f, factor);
        paint.setTextSize(NUMBER_SIZE_DP * density * zoom);
    }

    /**
     * Le facteur de zoom courant de la gouttière (le harnais le lit).
     *
     * @return le facteur (1 : le corps de repos)
     */
    public float textZoom() {
        return zoom;
    }

    /**
     * La teinte du panneau de la gouttière : le fond du thème y
     * devient translucide (l'alpha porte la translucence — le texte
     * se lit dessous, la gouttière flotte au-dessus du corps).
     *
     * @param background le fond du thème courant (ARGB opaque)
     */
    public void setPanelColor(int background) {
        panelPaint.setColor((background & 0x00FFFFFF)
                | ((int) (PANEL_ALPHA * 255.0f) << 24));
    }

    /**
     * Le style de l'hôte : les teintes des marqueurs y vivent (erreur,
     * avertissement, ampoule) — la gouttière s'y reconfigure.
     *
     * @param fresh le style posé, null pour celui par défaut
     */
    public void setStyle(PopupStyle fresh) {
        style = fresh == null ? PopupStyle.defaults() : fresh;
        applyStyle();
    }

    /** Reconfigure les pinceaux des marqueurs au style courant. */
    private void applyStyle() {
        markerPaint.setStyle(Paint.Style.FILL);
        markerGlyph.setColor(0xFFFFFFFF);
    }

    /**
     * Dessine le panneau de la gouttière : le fond translucide, le
     * filet du bord droit, et l'ombre d'élévation qui le détache du
     * corps (la gouttière flotte au-dessus du texte — une carte à
     * gauche de la fenêtre).
     *
     * @param canvas le canevas de la trame
     * @param top    le bord haut de la fenêtre (px écran)
     * @param bottom le bord bas de la fenêtre (px écran)
     * @param right  le bord droit du panneau (px écran)
     */
    public void drawPanel(Canvas canvas, float top, float bottom,
            float right) {
        float left = 0.0f;
        panelRect.set(left, top, right, bottom);
        canvas.drawRect(panelRect, panelPaint);
        // l'élévation : l'ombre se pose en marches décroissantes au
        // bord droit (un dégradé honnête, sans canevas hors écran)
        float step = dp(SHADOW_WIDTH_DP) / SHADOW_STEPS;
        for (int at = 0; at < SHADOW_STEPS; at++) {
            float edge = right + step * at;
            int alpha = (int) (22.0f * (SHADOW_STEPS - at) / SHADOW_STEPS);
            shadowPaint.setColor((alpha << 24) | 0x00000000);
            canvas.drawRect(edge, top, edge + step, bottom, shadowPaint);
        }
        // le filet du bord : la ligne qui sépare la gouttière du corps
        canvas.drawLine(right, top, right, bottom, panelBorder);
    }

    /**
     * Dessine le numéro d'une ligne visible.
     *
     * @param canvas    le canevas de la trame
     * @param line      l'indice de la ligne (numérotée from zero)
     * @param baseline  la baseline du texte de la ligne (px écran)
     * @param rightEdge le bord droit du champ des numéros (px)
     */
    public void drawNumber(Canvas canvas, int line, float baseline,
            float rightEdge) {
        String number = Integer.toString(line + 1);
        if (number.length() > MAX_DIGITS) {
            number = "…" + number.substring(number.length() - 3);
        }
        float width = paint.measureText(number);
        canvas.drawText(number, rightEdge - width, baseline, paint);
    }

    /**
     * Dessine le marqueur d'une ligne visible : le rond rouge barré
     * de l'erreur, le triangle ambre de l'avertissement (le point
     * d'exclamation y vit), l'ampoule jaune des actions rapides —
     * centré sur la colonne des marqueurs, à la hauteur de la ligne.
     *
     * @param canvas le canevas de la trame
     * @param kind   le code du marqueur (MARKER_)
     * @param cx     l'abscisse du centre de la colonne (px écran)
     * @param cy     l'ordonnée du centre de la ligne (px écran)
     */
    public void drawMarker(Canvas canvas, int kind, float cx, float cy) {
        if (kind == MARKER_ERROR) {
            markerPaint.setColor(style.errorArgb());
            float radius = dp(MARKER_SIDE_DP) / 2.0f;
            canvas.drawCircle(cx, cy, radius, markerPaint);
            float s = dp(MARKER_SIDE_DP) * 0.22f;
            canvas.drawLine(cx - s, cy - s, cx + s, cy + s, markerGlyph);
            canvas.drawLine(cx - s, cy + s, cx + s, cy - s, markerGlyph);
        } else if (kind == MARKER_WARNING) {
            markerPaint.setColor(style.warningArgb());
            drawTriangleMarker(canvas, cx, cy, true);
        } else if (kind == MARKER_LIGHTBULB) {
            drawLightbulbMarker(canvas, cx, cy);
        } else if (kind == MARKER_BREAKPOINT) {
            // le point d'arrêt : le rond plein à l'accent (le style
            // du débogueur — l'hôte y connectera le sien)
            markerPaint.setColor(style.accentArgb());
            canvas.drawCircle(cx, cy, dp(MARKER_SIDE_DP) / 2.0f, markerPaint);
        } else if (kind == MARKER_PREVIEW) {
            // L'APERÇU COMPOSE à la référence : le petit ÉCRAN (le
            // carré arrondi, la fente en bas — l'icône de son
            // gestionnaire d'aperçu), la teinte accent
            float side = dp(MARKER_SIDE_DP);
            float half = side / 2.0f;
            markerPaint.setColor(style.accentArgb());
            markerPaint.setStyle(Paint.Style.STROKE);
            markerPaint.setStrokeWidth(1.6f * density);
            canvas.drawRoundRect(cx - half * 0.9f, cy - half * 0.75f,
                    cx + half * 0.9f, cy + half * 0.6f,
                    dp(2), dp(2), markerPaint);
            // le pied de l'écran (la petite barre en bas — le pied
            // du panneau d'aperçu)
            canvas.drawRect(cx - half * 0.35f, cy + half * 0.85f,
                    cx + half * 0.35f, cy + half * 1.05f, markerPaint);
            markerPaint.setStyle(Paint.Style.FILL);
            // l'éclair discret au centre (l'aperçu qui SE CONSTRUIT —
            // la teinte pleine du Compose)
            markerPaint.setColor(style.accentArgb());
            android.graphics.Path bolt = new android.graphics.Path();
            bolt.moveTo(cx + half * 0.1f, cy - half * 0.55f);
            bolt.lineTo(cx - half * 0.25f, cy + half * 0.05f);
            bolt.lineTo(cx, cy + half * 0.05f);
            bolt.lineTo(cx - half * 0.1f, cy + half * 0.55f);
            bolt.lineTo(cx + half * 0.25f, cy - half * 0.05f);
            bolt.lineTo(cx, cy - half * 0.05f);
            bolt.close();
            canvas.drawPath(bolt, markerPaint);
        }
    }

    /**
     * Le triangle d'avertissement (le point d'exclamation au centre)
     * ou celui de l'ampoule éteinte.
     */
    private void drawTriangleMarker(Canvas canvas, float cx, float cy,
            boolean exclamation) {
        float side = dp(MARKER_SIDE_DP);
        Path triangle = new Path();
        triangle.moveTo(cx, cy - side * 0.55f);
        triangle.lineTo(cx + side * 0.62f, cy + side * 0.48f);
        triangle.lineTo(cx - side * 0.62f, cy + side * 0.48f);
        triangle.close();
        canvas.drawPath(triangle, markerPaint);
        if (exclamation) {
            float s = side * 0.22f;
            canvas.drawLine(cx, cy - s, cx, cy + s * 0.4f, markerGlyph);
            canvas.drawPoint(cx, cy + s * 1.1f, markerGlyph);
        }
    }

    /**
     * L'ampoule jaune : le corps de l'ampoule (un cercle coiffé d'un
     * petit entonnoir) à la teinte du style, posée à la hauteur de
     * la ligne — le doigt dessus ouvre les actions rapides.
     */
    private void drawLightbulbMarker(Canvas canvas, float cx, float cy) {
        markerPaint.setColor(style.lightbulbArgb());
        float side = dp(MARKER_SIDE_DP);
        float radius = side * 0.42f;
        // le corps de l'ampoule
        canvas.drawCircle(cx, cy - side * 0.08f, radius, markerPaint);
        // le culot : un petit trapèze dessous
        Path base = new Path();
        base.moveTo(cx - side * 0.18f, cy + side * 0.24f);
        base.lineTo(cx + side * 0.18f, cy + side * 0.24f);
        base.lineTo(cx + side * 0.12f, cy + side * 0.5f);
        base.lineTo(cx - side * 0.12f, cy + side * 0.5f);
        base.close();
        canvas.drawPath(base, markerPaint);
        // deux traits de filament pour dire l'ampoule allumée
        markerGlyph.setColor(0xFFFFFFFF);
        canvas.drawLine(cx - side * 0.1f, cy + side * 0.28f,
                cx + side * 0.1f, cy + side * 0.28f, markerGlyph);
        canvas.drawLine(cx - side * 0.07f, cy + side * 0.38f,
                cx + side * 0.07f, cy + side * 0.38f, markerGlyph);
    }

    private float dp(int value) {
        return value * density;
    }
}
