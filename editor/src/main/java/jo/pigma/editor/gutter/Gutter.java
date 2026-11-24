package jo.pigma.editor.gutter;

import android.graphics.Canvas;
import android.graphics.Paint;

/**
 * La gouttière (le paquet du même nom, l'arborescence du prompt) : le
 * numéro de chaque ligne visible, aligné à droite juste avant la zone
 * du marqueur de pli. Le pinceau y vit (taille, couleur du thème) ;
 * la vue ne fournit que la géométrie à la trame — la baseline de la
 * ligne et le bord droit du champ des numéros.
 *
 * <p>Au-delà de quatre chiffres le numéro se tronque honnêtement (les
 * trois derniers, précédés d'une ellipse) : la gouttière ne s'élargit
 * pas avec le document, la mise en page ne reflow pas au millième
 * numéro.</p>
 */
public final class Gutter {

    private static final int MAX_DIGITS = 4;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    /**
     * Une gouttière neuve, son pinceau propre.
     *
     * @param density la densité de l'écran (le dp en pixels)
     */
    public Gutter(float density) {
        paint.setTextSize(10.0f * density);
        paint.setColor(0xFF8A8A8E);
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
     * Dessine le numéro d'une ligne visible.
     *
     * @param canvas   le canevas de la trame
     * @param line     l'indice de la ligne (numérotée from zero)
     * @param baseline la baseline du texte de la ligne (px écran)
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
}
