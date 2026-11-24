package jo.pigma.editor.popups;

/**
 * La géométrie de la vue hôte au moment du dessin d'une popup (le
 * paquet des popups ne connaît la vue que par ces nombres).
 *
 * @param paddingLeft   marge gauche (px)
 * @param paddingTop    marge haute (px)
 * @param paddingRight  marge droite (px)
 * @param paddingBottom marge basse (px)
 * @param viewWidth     largeur de la vue (px)
 * @param viewHeight    hauteur de la vue (px)
 * @param lineHeight    hauteur d'une ligne de texte (px)
 * @param charWidth     avance d'un caractère (px, chasse fixe)
 * @param density       densité de l'écran (le dp en pixels)
 */
public record PopupMetrics(float paddingLeft, float paddingTop, float paddingRight,
        float paddingBottom, float viewWidth, float viewHeight, float lineHeight,
        float charWidth, float density) {
}
