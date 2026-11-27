package jo.pigma.editor.popups;

import android.graphics.Canvas;
import android.graphics.Paint;

/**
 * LE PEINTRE DES GLYPHES D'ACTION, AU TRAIT (la leçon de la
 * référence, étudiée dans {@code docs/etude-codeassist.md}) : les
 * natures des actions de l'éditeur — celles de la carte d'actions
 * rapides (l'ampoule de la gouttière) et celles de la carte de
 * débordement (le « … » de la barre de sélection). Chaque nature
 * porte SON glyphe dessiné à la main, à la grille 24 de la
 * référence, le filet rond de 1,7/24 — la façon de {@code
 * SeverityIcon}. Le peintre ne porte NI pinceau NI couleur : l'encre
 * vit chez le consommateur (la carte qui peint), le trait suit le
 * côté donné. L'ÉCLAIR D'ACTION seul se dessine en boucle FERMÉE au
 * filet : la carte d'actions rapides le peint PLEIN à la teinte de
 * l'ampoule (la tête de l'ampoule — sa façon propre, le défaut
 * d'avant), la carte de débordement le peint au trait (la boucle de
 * ce peintre — aucune teinte d'ampoule n'y vit).
 */
public final class ActionGlyphs {

    /** Un peintre, jamais d'instance. */
    private ActionGlyphs() {
    }

    /**
     * Peint le glyphe d'une nature au trait, à la grille 24 (le
     * centre donné, les traits en vingt-quatrièmes du côté — la
     * façon de {@code SeverityIcon}, son filet de 1,7/24 aux caps
     * rondes). La nature morte ou l'éclair d'action rend la boucle
     * fermée de l'éclair (le défaut honnête des rangées qui ne
     * disent rien).
     *
     * @param canvas le canevas de la vue
     * @param glyph  la nature de l'action
     * @param ink    le pinceau de trait (l'encre à l'appelant)
     * @param cx     l'abscisse du centre (px)
     * @param cy     l'ordonnée du centre (px)
     * @param side   le côté du glyphe (px)
     */
    public static void draw(Canvas canvas, QuickActionCard.Glyph glyph,
            Paint ink, float cx, float cy, float side) {
        float u = side / 24.0f;
        if (glyph == null || glyph == QuickActionCard.Glyph.ACTION) {
            drawBoltStroke(canvas, ink, cx, cy, u);
            return;
        }
        switch (glyph) {
            case COMMENT -> {
                // « // » : les deux barres du commentaire de fin de ligne
                canvas.drawLine(cx - 4 * u, cy - 6 * u, cx - 1 * u, cy + 6 * u, ink);
                canvas.drawLine(cx + 1 * u, cy - 6 * u, cx + 4 * u, cy + 6 * u, ink);
            }
            case UPPERCASE -> {
                // la flèche haute : la tige et son chevron
                canvas.drawLine(cx, cy + 6 * u, cx, cy - 6 * u, ink);
                canvas.drawLine(cx - 4 * u, cy - 2 * u, cx, cy - 6 * u, ink);
                canvas.drawLine(cx, cy - 6 * u, cx + 4 * u, cy - 2 * u, ink);
            }
            case LOWERCASE -> {
                // la flèche basse
                canvas.drawLine(cx, cy - 6 * u, cx, cy + 6 * u, ink);
                canvas.drawLine(cx - 4 * u, cy + 2 * u, cx, cy + 6 * u, ink);
                canvas.drawLine(cx, cy + 6 * u, cx + 4 * u, cy + 2 * u, ink);
            }
            case DUPLICATE -> {
                // les deux feuilles superposées
                canvas.drawRoundRect(cx - 8 * u, cy - 6 * u, cx + 2 * u,
                        cy + 4 * u, 2 * u, 2 * u, ink);
                canvas.drawRoundRect(cx - 2 * u, cy - 8 * u, cx + 8 * u,
                        cy + 2 * u, 2 * u, 2 * u, ink);
            }
            case TRASH -> {
                // la poubelle : le couvercle, l'anse, le corps
                canvas.drawLine(cx - 6 * u, cy - 5 * u, cx + 6 * u, cy - 5 * u, ink);
                canvas.drawLine(cx - 2 * u, cy - 8 * u, cx + 2 * u, cy - 8 * u, ink);
                canvas.drawLine(cx - 2 * u, cy - 8 * u, cx - 2 * u, cy - 5 * u, ink);
                canvas.drawLine(cx + 2 * u, cy - 8 * u, cx + 2 * u, cy - 5 * u, ink);
                canvas.drawLine(cx - 5 * u, cy - 3 * u, cx - 4 * u, cy + 8 * u, ink);
                canvas.drawLine(cx + 5 * u, cy - 3 * u, cx + 4 * u, cy + 8 * u, ink);
                canvas.drawLine(cx - 4 * u, cy + 8 * u, cx + 4 * u, cy + 8 * u, ink);
            }
            case SURROUND -> {
                // les crochets : ouvrir à gauche, fermer à droite
                android.graphics.Path brackets = new android.graphics.Path();
                brackets.moveTo(cx - 5 * u, cy - 7 * u);
                brackets.lineTo(cx - 9 * u, cy - 7 * u);
                brackets.lineTo(cx - 9 * u, cy + 7 * u);
                brackets.lineTo(cx - 5 * u, cy + 7 * u);
                brackets.moveTo(cx + 5 * u, cy - 7 * u);
                brackets.lineTo(cx + 9 * u, cy - 7 * u);
                brackets.lineTo(cx + 9 * u, cy + 7 * u);
                brackets.lineTo(cx + 5 * u, cy + 7 * u);
                canvas.drawPath(brackets, ink);
            }
            case SNIPPET -> {
                // la feuille au coin plié (la façon des récents)
                android.graphics.Path sheet = new android.graphics.Path();
                sheet.moveTo(cx - 7 * u, cy - 9 * u);
                sheet.lineTo(cx + 2 * u, cy - 9 * u);
                sheet.lineTo(cx + 7 * u, cy - 4 * u);
                sheet.lineTo(cx + 7 * u, cy + 9 * u);
                sheet.lineTo(cx - 7 * u, cy + 9 * u);
                sheet.close();
                canvas.drawPath(sheet, ink);
                canvas.drawLine(cx + 2 * u, cy - 9 * u, cx + 2 * u, cy - 4 * u, ink);
                canvas.drawLine(cx + 2 * u, cy - 4 * u, cx + 7 * u, cy - 4 * u, ink);
            }
            case CALENDAR -> {
                // le calendrier : le corps, les deux pattes, la tête
                canvas.drawRoundRect(cx - 7 * u, cy - 4 * u, cx + 7 * u,
                        cy + 8 * u, 2 * u, 2 * u, ink);
                canvas.drawLine(cx - 3 * u, cy - 8 * u, cx - 3 * u, cy - 2 * u, ink);
                canvas.drawLine(cx + 3 * u, cy - 8 * u, cx + 3 * u, cy - 2 * u, ink);
                canvas.drawLine(cx - 7 * u, cy, cx + 7 * u, cy, ink);
            }
            case SEARCH -> {
                // la loupe : le cercle et sa poignée
                canvas.drawCircle(cx - 2 * u, cy - 2 * u, 5.5f * u, ink);
                canvas.drawLine(cx + 2 * u, cy + 2 * u, cx + 7 * u, cy + 7 * u, ink);
            }
            case GOTO -> {
                // la flèche à la barre : l'aller-y de la définition
                canvas.drawLine(cx - 8 * u, cy, cx + 4 * u, cy, ink);
                canvas.drawLine(cx, cy - 4 * u, cx + 4 * u, cy, ink);
                canvas.drawLine(cx + 4 * u, cy, cx, cy + 4 * u, ink);
                canvas.drawLine(cx + 7 * u, cy - 6 * u, cx + 7 * u, cy + 6 * u, ink);
            }
            case EYE -> {
                // l'œil : l'ovale et sa pupille
                canvas.drawOval(cx - 8 * u, cy - 4 * u, cx + 8 * u, cy + 4 * u, ink);
                canvas.drawPoint(cx, cy, ink);
            }
            case QUOTES -> {
                // les guillemets : deux points et leurs queues
                canvas.drawPoint(cx - 4 * u, cy - 4 * u, ink);
                canvas.drawPoint(cx + 3 * u, cy - 4 * u, ink);
                canvas.drawLine(cx - 4 * u, cy - 4 * u, cx - 6 * u, cy + 3 * u, ink);
                canvas.drawLine(cx + 3 * u, cy - 4 * u, cx + 1 * u, cy + 3 * u, ink);
            }
            case PENCIL -> {
                // le crayon : la diagonale et sa pointe
                canvas.drawLine(cx - 5 * u, cy + 5 * u, cx + 6 * u, cy - 6 * u, ink);
                canvas.drawLine(cx - 8 * u, cy + 4 * u, cx - 4 * u, cy + 8 * u, ink);
                canvas.drawLine(cx - 5 * u, cy + 5 * u, cx - 8 * u, cy + 8 * u, ink);
            }
            case ALIGN -> {
                // les lignes alignées : le formatage du document
                canvas.drawLine(cx - 8 * u, cy - 5 * u, cx + 8 * u, cy - 5 * u, ink);
                canvas.drawLine(cx - 8 * u, cy, cx + 4 * u, cy, ink);
                canvas.drawLine(cx - 8 * u, cy + 5 * u, cx + 8 * u, cy + 5 * u, ink);
            }
            case TREE -> {
                // l'arbre des symboles : la racine, la tige, ses fils
                canvas.drawLine(cx - 7 * u, cy - 6 * u, cx - 1 * u, cy - 6 * u, ink);
                canvas.drawLine(cx - 1 * u, cy - 6 * u, cx - 1 * u, cy + 7 * u, ink);
                canvas.drawLine(cx - 1 * u, cy - 2 * u, cx + 6 * u, cy - 2 * u, ink);
                canvas.drawLine(cx - 1 * u, cy + 4 * u, cx + 6 * u, cy + 4 * u, ink);
                canvas.drawPoint(cx - 8 * u, cy - 6 * u, ink);
                canvas.drawPoint(cx + 7 * u, cy - 2 * u, ink);
                canvas.drawPoint(cx + 7 * u, cy + 4 * u, ink);
            }
            case CHAT -> {
                // la bulle de parole : le corps, la queue, deux lignes
                canvas.drawRoundRect(cx - 8 * u, cy - 7 * u, cx + 8 * u,
                        cy + 3 * u, 3 * u, 3 * u, ink);
                canvas.drawLine(cx - 3 * u, cy + 3 * u, cx - 6 * u, cy + 7 * u, ink);
                canvas.drawLine(cx - 4 * u, cy - 3 * u, cx + 4 * u, cy - 3 * u, ink);
                canvas.drawLine(cx - 4 * u, cy + 1 * u, cx + 2 * u, cy + 1 * u, ink);
            }
            case DOC -> {
                // la feuille documentée : le coin plié et sa ligne
                android.graphics.Path page = new android.graphics.Path();
                page.moveTo(cx - 6 * u, cy - 9 * u);
                page.lineTo(cx + 2 * u, cy - 9 * u);
                page.lineTo(cx + 7 * u, cy - 4 * u);
                page.lineTo(cx + 7 * u, cy + 9 * u);
                page.lineTo(cx - 6 * u, cy + 9 * u);
                page.close();
                canvas.drawPath(page, ink);
                canvas.drawLine(cx + 2 * u, cy - 9 * u, cx + 2 * u, cy - 4 * u, ink);
                canvas.drawLine(cx + 2 * u, cy - 4 * u, cx + 7 * u, cy - 4 * u, ink);
                canvas.drawLine(cx - 3 * u, cy, cx + 4 * u, cy, ink);
                canvas.drawLine(cx - 3 * u, cy + 4 * u, cx + 4 * u, cy + 4 * u, ink);
            }
            case CHECK -> {
                // la coche cerclée : le test qui passe
                canvas.drawCircle(cx, cy, 8 * u, ink);
                canvas.drawLine(cx - 4 * u, cy, cx - 1 * u, cy + 3 * u, ink);
                canvas.drawLine(cx - 1 * u, cy + 3 * u, cx + 5 * u, cy - 3 * u, ink);
            }
            case CUT -> {
                // les ciseaux : deux anneaux, les lames en croix
                canvas.drawCircle(cx - 5 * u, cy + 6 * u, 2.5f * u, ink);
                canvas.drawCircle(cx + 5 * u, cy + 6 * u, 2.5f * u, ink);
                canvas.drawLine(cx - 4 * u, cy + 4 * u, cx + 6 * u, cy - 8 * u, ink);
                canvas.drawLine(cx + 4 * u, cy + 4 * u, cx - 6 * u, cy - 8 * u, ink);
            }
            case SELECT_ALL -> {
                // le cadre pointé : les quatre coins d'une sélection
                corner(canvas, ink, cx - 8 * u, cy - 5 * u, 4 * u, 4 * u, true, true);
                corner(canvas, ink, cx + 8 * u, cy - 5 * u, 4 * u, 4 * u, false, true);
                corner(canvas, ink, cx - 8 * u, cy + 5 * u, 4 * u, 4 * u, true, false);
                corner(canvas, ink, cx + 8 * u, cy + 5 * u, 4 * u, 4 * u, false, false);
            }
            case CLOSE -> {
                // la croix : fermer, quitter la sélection
                canvas.drawLine(cx - 5 * u, cy - 5 * u, cx + 5 * u, cy + 5 * u, ink);
                canvas.drawLine(cx - 5 * u, cy + 5 * u, cx + 5 * u, cy - 5 * u, ink);
            }
            case CLIPBOARD -> {
                // la planche et sa pince : le presse-papiers
                canvas.drawRoundRect(cx - 6 * u, cy - 4 * u, cx + 6 * u,
                        cy + 9 * u, 2 * u, 2 * u, ink);
                canvas.drawRoundRect(cx - 3 * u, cy - 7 * u, cx + 3 * u,
                        cy - 3 * u, 1.5f * u, 1.5f * u, ink);
                canvas.drawLine(cx - 3 * u, cy, cx + 3 * u, cy, ink);
                canvas.drawLine(cx - 3 * u, cy + 3 * u, cx + 2 * u, cy + 3 * u, ink);
            }
            case HISTORY -> {
                // l'horloge : l'historique du document
                canvas.drawCircle(cx, cy, 8 * u, ink);
                canvas.drawLine(cx, cy - 5 * u, cx, cy, ink);
                canvas.drawLine(cx, cy, cx + 4 * u, cy + 3 * u, ink);
            }
            case PROBLEM -> {
                // le triangle pointé : la liste des problèmes
                android.graphics.Path triangle = new android.graphics.Path();
                triangle.moveTo(cx, cy - 8 * u);
                triangle.lineTo(cx + 8 * u, cy + 7 * u);
                triangle.lineTo(cx - 8 * u, cy + 7 * u);
                triangle.close();
                canvas.drawPath(triangle, ink);
                canvas.drawLine(cx, cy - 3 * u, cx, cy + 1 * u, ink);
                canvas.drawPoint(cx, cy + 4 * u, ink);
            }
            default -> drawBoltStroke(canvas, ink, cx, cy, u);
        }
    }

    /**
     * Un coin de cadre : deux traits en équerre — la sélection qui
     * s'étend aux quatre coins de son texte.
     */
    private static void corner(Canvas canvas, Paint ink, float x, float y,
            float spanX, float spanY, boolean left, boolean top) {
        canvas.drawLine(x, y, left ? x - spanX : x + spanX, y, ink);
        canvas.drawLine(x, y, x, top ? y - spanY : y + spanY, ink);
    }

    /**
     * L'éclair AU TRAIT (le défaut des rangées qui ne disent rien) :
     * une boucle fermée au filet — le débordement n'a pas de tête
     * d'ampoule à teinter, son éclair se lit par son contour.
     */
    private static void drawBoltStroke(Canvas canvas, Paint ink,
            float cx, float cy, float u) {
        android.graphics.Path bolt = new android.graphics.Path();
        bolt.moveTo(cx - 5.5f * u, cy - 8 * u);
        bolt.lineTo(cx + 3.5f * u, cy - 8 * u);
        bolt.lineTo(cx - 1.5f * u, cy);
        bolt.lineTo(cx + 5.5f * u, cy);
        bolt.lineTo(cx - 5.5f * u, cy + 8 * u);
        bolt.lineTo(cx - 0.5f * u, cy + 1.5f * u);
        bolt.lineTo(cx - 5.5f * u, cy + 1.5f * u);
        bolt.close();
        canvas.drawPath(bolt, ink);
    }
}
