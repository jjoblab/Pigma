package jo.pigma.editor.popups;

import android.graphics.Canvas;
import android.graphics.Paint;

/**
 * L'icône de sévérité d'un diagnostic, AU TRAIT (la leçon de la
 * référence, étudiée dans {@code docs/etude-codeassist.md} : ses
 * icônes y vivent en traits fins arrondis sur une grille 24 — le
 * cercle barré de l'erreur, le triangle pointé de l'avertissement,
 * le « i » de l'information — la couleur y suit la sévérité :
 * ROUGE pour l'erreur, AMBRE pour l'avertissement, la teinte
 * d'accent pour l'information). Aucune forme pleine : le glyphe se
 * lit par son trait, à la couleur de sa sévérité, sur le fond où il
 * vit — la gouttière, la carte, la pastille.
 *
 * <p>Une instance tient ses trois pinceaux (un par sévérité, la
 * couleur du style de l'hôte) ; le dessin ne prend que la sévérité,
 * le centre et le côté. Le trait y vit au dix-sept vingt-quatrième
 * du côté, les caps arrondis — à la taille de la gouttière comme à
 * celle de la carte.</p>
 */
public final class SeverityIcon {

    /** La sévérité d'une erreur (le cercle barré, rouge). */
    public static final int SEVERITY_ERROR = DiagnosticCard.SEVERITY_ERROR;

    /** La sévérité d'un avertissement (le triangle, ambre). */
    public static final int SEVERITY_WARNING = DiagnosticCard.SEVERITY_WARNING;

    /** La sévérité d'une information (le « i », accent). */
    public static final int SEVERITY_INFO = DiagnosticCard.SEVERITY_INFO;

    /** Le trait au côté de l'icône (la référence : 1,7 sur sa grille 24). */
    private static final float STROKE_RATIO = 1.7f / 24.0f;

    private final Paint errorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Paint warningPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Paint infoPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    /**
     * Une icône neuve, ses pinceaux au style par défaut.
     */
    public SeverityIcon() {
        applyStyle(PopupStyle.defaults());
    }

    /**
     * Le style de l'hôte : les trois encres de sévérité (rouge,
     * ambre, accent).
     *
     * @param fresh le style posé, null pour celui par défaut
     */
    public void setStyle(PopupStyle fresh) {
        applyStyle(fresh == null ? PopupStyle.defaults() : fresh);
    }

    /** Reconfigure les pinceaux au style donné. */
    private void applyStyle(PopupStyle style) {
        stroke(errorPaint, style.errorArgb());
        stroke(warningPaint, style.warningArgb());
        stroke(infoPaint, style.accentArgb());
    }

    /** Un pinceau de trait, cap arrondi, à l'encre donnée. */
    private static void stroke(Paint paint, int color) {
        paint.setColor(color);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
    }

    /**
     * L'encre de sévérité (la gouttière y teinte ses pastilles, la
     * vue sa pastille flottante).
     *
     * @param severity la sévérité, une des constantes SEVERITY_
     * @return l'ARGB de l'encre
     */
    public int inkOf(int severity) {
        return severity == SEVERITY_ERROR ? errorPaint.getColor()
                : severity == SEVERITY_WARNING ? warningPaint.getColor()
                : infoPaint.getColor();
    }

    /**
     * Dessine le glyphe de sévérité au trait : le CERCLE BARRÉ de
     * l'erreur, le TRIANGLE EXCLAMÉ de l'avertissement, le CERCLE
     * « i » de l'information — les proportions viennent de la grille
     * 24 de la référence (le rayon au 8,5 vingt-quatrième du côté,
     * la croix de 9,9 à 15,15, l'apex du triangle au 4,5).
     *
     * @param canvas   le canevas de la trame
     * @param severity la sévérité, une des constantes SEVERITY_
     * @param cx       l'abscisse du centre (px)
     * @param cy       l'ordonnée du centre (px)
     * @param side     le côté de l'icône (px)
     */
    public void draw(Canvas canvas, int severity, float cx, float cy,
            float side) {
        Paint ink = severity == SEVERITY_ERROR ? errorPaint
                : severity == SEVERITY_WARNING ? warningPaint : infoPaint;
        ink.setStrokeWidth(Math.max(1.0f, side * STROKE_RATIO));
        if (severity == SEVERITY_WARNING) {
            drawWarning(canvas, ink, cx, cy, side);
            return;
        }
        // le cercle (erreur ou information) : rayon 8,5 sur 24
        canvas.drawCircle(cx, cy, side * 8.5f / 24.0f, ink);
        if (severity == SEVERITY_ERROR) {
            // la barre en croix : de (9,9) à (15,15) sur la grille
            float s = side * 6.0f / 24.0f;
            canvas.drawLine(cx - s, cy - s, cx + s, cy + s, ink);
            canvas.drawLine(cx - s, cy + s, cx + s, cy - s, ink);
            return;
        }
        // le « i » : le trait de (12,11) à (12,16,5), le point à (12,8)
        canvas.drawLine(cx, cy - side * 1.0f / 24.0f,
                cx, cy + side * 4.5f / 24.0f, ink);
        canvas.drawPoint(cx, cy - side * 4.0f / 24.0f, ink);
    }

    /**
     * Le triangle averti : l'apex en haut (12, 4,5), la base en bas
     * (de 3,5 à 20,5 à l'ordonnée 19), l'exclamation au centre — le
     * trait de (12,10) à (12,14,5), le point à (12,17) — chacun à
     * l'encre du trait, le cap arrondi.
     */
    private void drawWarning(Canvas canvas, Paint ink, float cx,
            float cy, float side) {
        float apexY = cy - side * 7.5f / 24.0f;
        float baseY = cy + side * 7.0f / 24.0f;
        float half = side * 8.5f / 24.0f;
        android.graphics.Path triangle = new android.graphics.Path();
        triangle.moveTo(cx, apexY);
        triangle.lineTo(cx + half, baseY);
        triangle.lineTo(cx - half, baseY);
        triangle.close();
        canvas.drawPath(triangle, ink);
        canvas.drawLine(cx, cy - side * 2.0f / 24.0f,
                cx, cy + side * 2.5f / 24.0f, ink);
        canvas.drawPoint(cx, cy + side * 5.0f / 24.0f, ink);
    }
}
