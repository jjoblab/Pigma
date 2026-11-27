package jo.pigma.editor.popups;

import android.graphics.Canvas;
import android.graphics.Paint;

/**
 * LE CURSEUR DE CHAMP QUI CLIGNOTE (le retour du terrain : « quand
 * j'appuie sur le popup recherche, le champ où je dois taper le texte
 * ne montre aucun signe d'avoir le focus, d'habitude avec le curseur
 * qui clignote — de même pour le popup aller à la ligne »). Les barres
 * à champ ne prennent jamais le focus (le clavier reste à l'éditeur,
 * qui rend les frappes à la carte) : le signe du focus délégué y vit
 * au CLIGNOTEMENT — la barre du champ actif, à la phase de l'horloge
 * de disponibilité, à l'encre d'accent.
 *
 * <p>Pure : la phase se lit de l'horloge (la trame porte le clignote —
 * la vue réclame la suivante tant qu'une barre vit, comme le reste des
 * animations du paquetage).</p>
 */
final class FieldCaret {

    /** La demi-période du clignotement (ms) — le rythme du cadre. */
    private static final long BLINK_HALF_MS = 530L;

    private FieldCaret() {
    }

    /** Le clignotement est-il ALLUMÉ (la phase ouverte du cycle). */
    static boolean on() {
        return (android.os.SystemClock.uptimeMillis() / BLINK_HALF_MS) % 2L == 0L;
    }

    /**
     * Dessine la barre du curseur au champ actif — rien quand le
     * clignotement est éteint (le cycle se voit : la barre vit).
     *
     * @param canvas le canevas de la vue
     * @param x       l'abscisse du bord gauche de la barre (px — après
     *                le texte tapé)
     * @param top     le bord haut de la barre (px)
     * @param bottom  le bord bas de la barre (px)
     * @param width   la largeur de la barre (px — ~2 dp)
     * @param paint   l'encre d'accent (la barre du champ actif)
     */
    static void draw(Canvas canvas, float x, float top, float bottom,
            float width, Paint paint) {
        if (!on()) {
            return;
        }
        canvas.drawRect(x, top, x + width, bottom, paint);
    }
}
