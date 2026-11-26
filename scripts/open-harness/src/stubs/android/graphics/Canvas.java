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
    private final List<BitmapOp> bitmapOps = new ArrayList<>();
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
    private int bitmapMark = 0;

    /** Le crochet du harnais : couru au premier texte dessiné, puis retiré. */
    private Runnable drawHook;

    /** Le canevas de la vue (ses opérations s'y enregistrent). */
    public Canvas() {
    }

    /**
     * Un canevas qui dessine dans un bitmap (le rendu hors trame de
     * la loupe) — ses opérations s'y enregistrent, personne d'autre
     * ne les lit.
     *
     * @param bitmap le bitmap cible
     */
    public Canvas(Bitmap bitmap) {
        // le rendu du bitmap se compte sur ce canevas propre
    }

    /**
     * Un bitmap blitté : le bitmap, la source, la destination — la
     * loupe y rend sa ligne une fois, la trame la blitte.
     */
    public static final class BitmapOp {
        /** Le bitmap blitté. */
        public final Bitmap bitmap;
        /** La source gauche. */
        public final float srcLeft;
        /** La source droite. */
        public final float srcRight;
        /** La destination gauche. */
        public final float dstLeft;
        /** La destination droite. */
        public final float dstRight;

        BitmapOp(Bitmap bitmap, float srcLeft, float srcRight,
                float dstLeft, float dstRight) {
            this.bitmap = bitmap;
            this.srcLeft = srcLeft;
            this.srcRight = srcRight;
            this.dstLeft = dstLeft;
            this.dstRight = dstRight;
        }
    }

    /** Peint la couleur de fond : se compte. */
    public void drawColor(int color) {
        colorCount++;
    }

    /**
     * Blitte un bitmap : la région source vers la région destination
     * (la loupe y rend sa ligne rendue une fois, à l'échelle).
     *
     * @param bitmap le bitmap
     * @param src    la région source
     * @param dst    la région destination
     * @param paint  la peinture (null : aucune)
     */
    public void drawBitmap(Bitmap bitmap, Rect src, RectF dst, Paint paint) {
        bitmapOps.add(new BitmapOp(bitmap, src.left, src.right,
                dst.left, dst.right));
    }

    /** Les bitmaps blittés depuis la dernière marque du harnais. */
    public List<BitmapOp> harnessBitmapsSinceMark() {
        return new ArrayList<>(bitmapOps.subList(bitmapMark, bitmapOps.size()));
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
     * Peint un rectangle (les bornes en objet réutilisé).
     *
     * @param rect  le rectangle
     * @param paint pinceau
     */
    public void drawRoundRect(RectF rect, float rx, float ry, Paint paint) {
    }

    public void drawRect(RectF rect, Paint paint) {
        drawRect(rect.left, rect.top, rect.right, rect.bottom, paint);
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
     * Peint un point (le point d'exclamation d'un marqueur).
     *
     * @param cx    abscisse du point
     * @param cy    ordonnée du point
     * @param paint pinceau
     */
    public void drawPoint(float cx, float cy, Paint paint) {
        lineCount++;
    }

    /**
     * Peint un chemin (le triangle d'un marqueur, l'ampoule).
     *
     * @param path  le chemin
     * @param paint pinceau
     */
    public void drawPath(Path path, Paint paint) {
        roundRectCount++;
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

    /**
     * Empile l'état avec un canal d'alpha (le fondu des popups) — le
     * stub ne le compose pas (les dessins comptent pareil), la
     * sémantique d'empilement y vit.
     *
     * @param left   bord gauche du calque
     * @param top    bord haut du calque
     * @param right  bord droit du calque
     * @param bottom bord bas du calque
     * @param alpha  le canal d'alpha (0 à 255)
     * @return profondeur de pile
     */
    public int saveLayerAlpha(float left, float top, float right, float bottom,
            int alpha) {
        return save();
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
        bitmapMark = bitmapOps.size();
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
