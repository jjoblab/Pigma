package android.graphics.drawable;

/**
 * Fond arrondi de démonstration : coins, couleur et liseré se
 * retiennent — la carte des barres flottantes ne se dessine pas au
 * harnais, seule son existence compte.
 */
public class GradientDrawable extends Drawable {

    /** Le rayon des coins. */
    private float cornerRadius;

    /** La couleur de fond. */
    private int color;

    /** La largeur du liseré. */
    private int strokeWidth;

    /** La couleur du liseré. */
    private int strokeColor;

    /**
     * Pose le rayon des coins.
     *
     * @param radius le rayon en pixels
     */
    public void setCornerRadius(float radius) {
        this.cornerRadius = radius;
    }

    /** Le rayon des coins (le harnais le lit). */
    public float getCornerRadius() {
        return cornerRadius;
    }

    /**
     * Pose la couleur de fond.
     *
     * @param color la couleur
     */
    public void setColor(int color) {
        this.color = color;
    }

    /** La couleur de fond (le harnais la lit). */
    public int getColor() {
        return color;
    }

    /**
     * Pose le liseré.
     *
     * @param width       la largeur en pixels
     * @param strokeColor la couleur
     */
    public void setStroke(int width, int strokeColor) {
        this.strokeWidth = width;
        this.strokeColor = strokeColor;
    }

    /** La largeur du liseré (le harnais la lit). */
    public int getStrokeWidth() {
        return strokeWidth;
    }

    /** La couleur du liseré (le harnais la lit). */
    public int getStrokeColor() {
        return strokeColor;
    }
}
