package android.graphics;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Canevas de démonstration : rien ne se peint, tout s'enregistre —
 * les vérifications du harnais lisent ce qui fut dessiné, où, à
 * quelle échelle, dans quelle fenêtre de découpe. La pile de
 * transformations (translation, homothétie) se tient comme sur un
 * vrai canevas : un dessin sous homothétie arrive agrandi.
 */
public final class Canvas {

    /**
     * Un texte dessiné : position écran, échelle courante au moment
     * du dessin, contenu.
     */
    public static final class TextOp {
        /** Le texte dessiné. */
        public final String text;
        /** Abscisse écran. */
        public final float x;
        /** Ordonnée écran (ligne de base). */
        public final float y;
        /** Échelle horizontale au moment du dessin. */
        public final float scaleX;

        TextOp(String text, float x, float y, float scaleX) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.scaleX = scaleX;
        }
    }

    /**
     * Une fenêtre de découpe posée : bornes écran après transformation.
     */
    public static final class ClipOp {
        /** Bord gauche écran. */
        public final float left;
        /** Bord haut écran. */
        public final float top;
        /** Bord droit écran. */
        public final float right;
        /** Bord bas écran. */
        public final float bottom;

        ClipOp(float left, float top, float right, float bottom) {
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
        }
    }

    private final List<TextOp> textOps = new ArrayList<>();
    private final List<ClipOp> clipOps = new ArrayList<>();
    private final Deque<float[]> matrixStack = new ArrayDeque<>();
    private float scaleX = 1.0f;
    private float scaleY = 1.0f;
    private float translateX = 0.0f;
    private float translateY = 0.0f;
    private int colorCount;
    private int rectCount;
    private int circleCount;
    private int lineCount;
    private int roundRectCount;
    private int mark = 0;

    /** Le crochet du harnais : couru au premier texte dessiné, puis retiré. */
    private Runnable drawHook;

    /** Peint la couleur de fond : se compte. */
    public void drawColor(int color) {
        colorCount++;
    }

    /**
     * Peint un rectangle.
     *
     * @param left   bord gauche
     * @param top    bord haut
     * @param right  bord droit
     * @param bottom bord bas
     * @param paint  pinceau
     */
    public void drawRect(float left, float top, float right, float bottom, Paint paint) {
        rectCount++;
    }

    /**
     * Peint un cercle (une poignée de sélection par exemple).
     *
     * @param cx      abscisse du centre
     * @param cy      ordonnée du centre
     * @param radius  rayon
     * @param paint   pinceau
     */
    public void drawCircle(float cx, float cy, float radius, Paint paint) {
        circleCount++;
    }

    /**
     * Peint un segment (un guide d'indentation par exemple).
     *
     * @param startX origine horizontale
     * @param startY origine verticale
     * @param stopX  extrémité horizontale
     * @param stopY  extrémité verticale
     * @param paint  pinceau
     */
    public void drawLine(float startX, float startY, float stopX, float stopY, Paint paint) {
        lineCount++;
    }

    /**
     * Peint un rectangle arrondi (une popup, un bandeau).
     *
     * @param left   bord gauche
     * @param top    bord haut
     * @param right  bord droit
     * @param bottom bord bas
     * @param rx     rayon horizontal des coins
     * @param ry     rayon vertical des coins
     * @param paint  pinceau
     */
    public void drawRoundRect(float left, float top, float right, float bottom,
            float rx, float ry, Paint paint) {
        roundRectCount++;
    }

    /**
     * Peint un texte : la position écran et l'échelle courante
     * s'enregistrent (un dessin sous homothétie arrive agrandi).
     *
     * @param text  texte
     * @param x     abscisse (espace courant)
     * @param y     ligne de base (espace courant)
     * @param paint pinceau
     */
    public void drawText(String text, float x, float y, Paint paint) {
        if (drawHook != null) {
            Runnable hook = drawHook;
            drawHook = null;
            hook.run();
        }
        textOps.add(new TextOp(text, scaleX * x + translateX, scaleY * y + translateY, scaleX));
    }

    /**
     * Pose un crochet qui court au premier texte dessiné (il se retire
     * seul) — le harnais y provoque un ramasse-miettes AU MILIEU d'une
     * trame, pour vérifier que l'odomètre l'attribue à la pire.
     *
     * @param hook la suite à courir, une fois
     */
    public void harnessDrawHook(Runnable hook) {
        drawHook = hook;
    }

    /**
     * Empile l'état courant (transformations) : un dessin sous
     * homothétie revient à l'échelle un au rétablissement.
     *
     * @return profondeur de pile
     */
    public int save() {
        matrixStack.push(new float[] {scaleX, scaleY, translateX, translateY});
        return matrixStack.size();
    }

    /** Rétablit l'état empilé (sans effet si la pile est vide). */
    public void restore() {
        if (!matrixStack.isEmpty()) {
            float[] state = matrixStack.pop();
            scaleX = state[0];
            scaleY = state[1];
            translateX = state[2];
            translateY = state[3];
        }
    }

    /**
     * Translate l'espace courant (la translation se compose avec
     * l'échelle en place).
     *
     * @param dx décalage horizontal
     * @param dy décalage vertical
     */
    public void translate(float dx, float dy) {
        translateX += scaleX * dx;
        translateY += scaleY * dy;
    }

    /**
     * Homothétie de l'espace courant (autour de l'origine courante) :
     * les dessins suivants arrivent agrandis ou réduits.
     *
     * @param sx facteur horizontal
     * @param sy facteur vertical
     */
    public void scale(float sx, float sy) {
        scaleX *= sx;
        scaleY *= sy;
    }

    /**
     * Pose une fenêtre de découpe (bornes transformées vers l'écran).
     *
     * @param left   bord gauche
     * @param top    bord haut
     * @param right  bord droit
     * @param bottom bord bas
     */
    public void clipRect(float left, float top, float right, float bottom) {
        clipOps.add(new ClipOp(scaleX * left + translateX, scaleY * top + translateY,
                scaleX * right + translateX, scaleY * bottom + translateY));
    }

    /**
     * Marque la fin des enregistrements passés : les lectures qui
     * suivent ne voient que les dessins à venir.
     */
    public void harnessMark() {
        mark = textOps.size();
        clipOps.clear();
        colorCount = 0;
        rectCount = 0;
        circleCount = 0;
        lineCount = 0;
        roundRectCount = 0;
    }

    /** Les textes dessinés depuis la marque. */
    public List<TextOp> harnessTextsSinceMark() {
        return new ArrayList<>(textOps.subList(mark, textOps.size()));
    }

    /** Les fenêtres de découpe posées depuis la marque. */
    public List<ClipOp> harnessClipsSinceMark() {
        return new ArrayList<>(clipOps);
    }

    /** Les rectangles peints depuis la marque. */
    public int harnessRectCount() {
        return rectCount;
    }

    /** Les cercles peints depuis la marque (les poignées). */
    public int harnessCircleCount() {
        return circleCount;
    }

    /** Les rectangles arrondis peints depuis la marque. */
    public int harnessRoundRectCount() {
        return roundRectCount;
    }
}
