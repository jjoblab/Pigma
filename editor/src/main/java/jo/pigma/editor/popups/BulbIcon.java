package jo.pigma.editor.popups;

import android.graphics.Canvas;
import android.graphics.Paint;

/**
 * L'ampoule AU TRAIT (la leçon de la référence, étudiée dans {@code
 * docs/etude-codeassist.md}) : le corps en CERCLE, le culot en DEUX
 * BARRES — ses icônes y vivent en traits fins arrondis, l'ampoule y
 * annonce le correctif (l'action, pas la faute). Deux consommateurs
 * portent LE MÊME glyphe : l'étage des correctifs de la carte de
 * diagnostic (le correctif se lit du même œil que le message qu'il
 * corrige) et la pastille du tap (le retour du terrain : le petit
 * icône du trait diagnostiqué doit être AU-DESSUS DU MOT comme la
 * référence — la gouttière porte déjà le marqueur du diagnostic, la
 * pastille n'y parle plus de la ligne, elle parle du mot — et c'est
 * une AMPoule comme celle de la carte, pas une sévérité : le tap y
 * cherche la correction, l'encre y suit l'étage des correctifs).
 *
 * <p>Le glyphe ne porte NI pinceau NI couleur : l'encre vit chez le
 * consommateur (à l'accent les deux — la teinte d'action), le trait y
 * suit le côté donné.</p>
 */
public final class BulbIcon {

    /** Un glyphe, jamais d'instance. */
    private BulbIcon() {
    }

    /**
     * Dessine l'ampoule au trait : le CORPS en cercle (le rayon au
     * tiers du côté), le CULOT en deux barres (la large puis l'étroite
     * — la vis de l'ampoule), les caps arrondis à l'encre du pinceau
     * posé.
     *
     * @param canvas le canevas de la trame
     * @param ink    le pinceau de trait (l'encre à l'appelant)
     * @param cx     l'abscisse du centre (px)
     * @param cy     l'ordonnée du centre (px)
     * @param side   le côté du glyphe (px)
     */
    public static void draw(Canvas canvas, Paint ink, float cx, float cy,
            float side) {
        // le corps : un cercle, un peu haut dans le carré
        canvas.drawCircle(cx, cy - side * 0.08f, side * 0.34f, ink);
        // le culot : la barre large, puis l'étroite dessous
        canvas.drawLine(cx - side * 0.18f, cy + side * 0.30f,
                cx + side * 0.18f, cy + side * 0.30f, ink);
        canvas.drawLine(cx - side * 0.12f, cy + side * 0.44f,
                cx + side * 0.12f, cy + side * 0.44f, ink);
    }
}
