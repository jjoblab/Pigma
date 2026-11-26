package android.graphics;

/**
 * Pinceau de démonstration : la couleur, le style, le corps et le
 * gras se retiennent ; les métriques se calculent depuis le corps
 * (espacement fixe : 0,6 corps par colonne, hauteur 1,15 corps).
 */
public class Paint {

    /** Drapeau d'anticrénelage (conservé, sans effet ici). */
    public static final int ANTI_ALIAS_FLAG = 1;

    /**
     * Style de trait : remplissage, contour, ou les deux.
     */
    public enum Style {
        /** Remplit les formes. */
        FILL,
        /** Trace les contours. */
        STROKE,
        /** Remplit et trace. */
        FILL_AND_STROKE
    }

    /**
     * Alignement du texte dessiné : gauche, centre ou droite de
     * l'abscisse donnée.
     */
    public enum Align {
        /** Le texte commence à l'abscisse. */
        LEFT,
        /** Le texte se centre sur l'abscisse. */
        CENTER,
        /** Le texte finit à l'abscisse. */
        RIGHT
    }

    /**
     * Métriques d'une police : distances signées depuis la ligne de
     * base, en pixels.
     */
    public static class FontMetrics {
        /** Distance au-dessus de la ligne de base du plus haut glyphe. */
        public float top;
        /** Montée conseillée. */
        public float ascent;
        /** Descente conseillée. */
        public float descent;
        /** Distance sous la ligne de base du plus bas glyphe. */
        public float bottom;
        /** Interligne conseillé. */
        public float leading;
    }

    private float textSize = 12.0f;
    private int color = 0xFF000000;
    private Style style = Style.FILL;
    private float strokeWidth = 1.0f;
    private boolean fakeBold;

    /** L'alignement courant du texte (gauche par défaut). */
    private Align align = Align.LEFT;
    private Typeface typeface;

    /** Un pinceau neuf aux valeurs par défaut. */
    public Paint() {
    }

    /**
     * Un pinceau neuf : les drapeaux se retiennent sans effet (la
     * qualité du trait ne se peint pas ici).
     *
     * @param flags drapeaux (p.ex. {@code ANTI_ALIAS_FLAG})
     */
    public Paint(int flags) {
    }

    /** L'anticrénelage : accepté, sans effet. */
    public void setAntiAlias(boolean aa) {
    }

    /**
     * La couleur de trait.
     *
     * @param color couleur entière ARVB
     */
    public void setColor(int color) {
        this.color = color;
    }

    /** La couleur courante. */
    public int getColor() {
        return color;
    }

    /**
     * Le style de trait.
     *
     * @param style remplissage, contour ou les deux
     */
    public void setShader(Shader shader) {
    }

    public void setStrokeCap(Cap cap) {
    }

    /** Le bout d'un trait (le ROUND des ligatures). */
    public enum Cap {
        ROUND,
        BUTT,
        SQUARE
    }

    public void setStyle(Style style) {
        this.style = style;
    }

    /** Le style courant. */
    public Style getStyle() {
        return style;
    }

    /**
     * L'épaisseur de contour.
     *
     * @param width épaisseur en pixels
     */
    public void setPathEffect(DashPathEffect effect) {
    }

    public void setStrokeWidth(float width) {
        this.strokeWidth = width;
    }

    /** L'épaisseur courante. */
    public float getStrokeWidth() {
        return strokeWidth;
    }

    /**
     * Le corps du texte.
     *
     * @param size corps en pixels
     */
    public void setTextSize(float size) {
        this.textSize = size;
    }

    /** Le corps courant. */
    public float getTextSize() {
        return textSize;
    }

    /**
     * La police du texte.
     *
     * @param typeface police (peut être nulle : celle par défaut)
     * @return la police posée
     */
    public Typeface setTypeface(Typeface typeface) {
        this.typeface = typeface;
        return typeface;
    }

    /** La police courante. */
    public Typeface getTypeface() {
        return typeface;
    }

    /**
     * Le gras synthétique : un drapeau de peinture, pas une seconde
     * police.
     *
     * @param fakeBold vrai pour épaissir le trait
     */
    public void setFakeBoldText(boolean fakeBold) {
        this.fakeBold = fakeBold;
    }

    /**
     * L'alignement du texte (conservé pour les cartes centrées —
     * l'espacement fixe du stub le lit à la mesure).
     */
    public void setTextAlign(Align align) {
        this.align = align;
    }

    /** L'alignement courant du texte. */
    public Align getTextAlign() {
        return align;
    }

    /** Le gras synthétique courant. */
    public boolean isFakeBoldText() {
        return fakeBold;
    }

    /**
     * Les métriques de la police courante : une capture neuve à chaque
     * appel (l'appelant la garde comme instantané).
     */
    public FontMetrics getFontMetrics() {
        FontMetrics metrics = new FontMetrics();
        float size = textSize;
        metrics.top = -0.90f * size;
        metrics.ascent = -0.80f * size;
        metrics.descent = 0.20f * size;
        metrics.bottom = 0.25f * size;
        metrics.leading = 0.0f;
        return metrics;
    }

    /**
     * La largeur d'un texte : espacement fixe, 0,6 corps par colonne.
     *
     * @param text texte mesuré
     * @return largeur en pixels
     */
    public float measureText(String text) {
        return 0.60f * textSize * text.length();
    }
}
