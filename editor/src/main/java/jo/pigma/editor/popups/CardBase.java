package jo.pigma.editor.popups;

import android.graphics.Canvas;
import android.graphics.Paint;

/**
 * LA BASE des cartes de popup (R-F — le code de carte répété : douze
 * classes portaient la pose, la tenue, le style et la croix, chacune
 * sa version). La migration s'y fait UNE carte à la fois — la carte
 * qui n'y vit pas encore garde sa facture d'avant, au pixel près.
 *
 * <p>La base porte LE COMMUN des cartes flottantes : l'état de vie
 * ({@link #isShown()}, {@link #close()}), la pose (l'ordonnée SOUS
 * l'ancre, DESSUS sans la place — jamais sur elle), les murs (le
 * bord gauche borné, la largeur bornée), le style ({@link #setStyle}
 * reconfigure la carte entière), la croix de fermeture au trait, le
 * texte coupé (la queue ou la tête, l'ellipse à l'autre bout) et le
 * dp.</p>
 *
 * <p>La carte ne prend jamais le focus ; l'état de vie reste simple
 * — la carte qui vit de son contenu ({@code DiagnosticCard}) y
 * surcharge {@link #isShown()}.</p>
 */
public abstract class CardBase {

    /** La densité de l'écran (le dp en pixels). */
    protected float density;

    /** Le style posé par l'hôte : fond translucide, encres, tailles, largeur, forme. */
    protected PopupStyle style = PopupStyle.defaults();

    /** Les chaînes posées par l'hôte (les libellés, les compteurs — la locale). */
    protected PopupStrings strings = PopupStrings.defaults();

    /** L'abscisse posée (px, coordonnées de la vue). */
    protected float poseX;

    /** L'ordonnée posée (px, coordonnées de la vue). */
    protected float poseY;

    /** La largeur de la carte (px). */
    protected float cardW;

    /** La hauteur de la carte (px). */
    protected float cardH;

    /** La carte vit-elle ? */
    protected boolean shown;

    /**
     * LE FLOT de pose (le retour du terrain : la carte qui suit son
     * ancre ne SAUTE plus à sa pose nouvelle — elle y GLISSE, le
     * glissement court amorti de {@link PopupFloat}).
     */
    protected final PopupFloat flot = new PopupFloat();

    /**
     * Une carte neuve, à la densité de l'écran.
     *
     * @param density la densité (le dp en pixels)
     */
    protected CardBase(float density) {
        this.density = density;
    }

    /** La carte vit-elle ? */
    public boolean isShown() {
        return shown;
    }

    /**
     * Range la carte (la croix, un nouvel appui, le changement de
     * contexte) ; {@link #onClosed()} y ramasse les restes.
     */
    public void close() {
        shown = false;
        onClosed();
    }

    /** Ramasse les restes de la fermeture (les listes, l'ancre). */
    protected void onClosed() {
        // le flot se range aussi (la prochaine naissance pose d'un trait)
        flot.reset();
    }

    /**
     * Pose le style de l'hôte : le fond translucide, les encres, la
     * taille des textes, la largeur des cartes, la forme — la carte
     * s'y reconfigure entière.
     *
     * @param fresh le style posé, null pour celui par défaut
     */
    public void setStyle(PopupStyle fresh) {
        style = fresh == null ? PopupStyle.defaults() : fresh;
        applyStyle();
    }

    /** Le style courant (le harnais le lit). */
    public PopupStyle style() {
        return style;
    }

    /**
     * Pose les chaînes de l'hôte (les libellés et les compteurs — la
     * source unique des chaînes localisées, la locale de
     * l'application hôte).
     *
     * @param fresh les chaînes posées, null pour celles par défaut
     */
    public void setStrings(PopupStrings fresh) {
        strings = fresh == null ? PopupStrings.defaults() : fresh;
    }

    /** Les chaînes courantes (le harnais les lit). */
    public PopupStrings strings() {
        return strings;
    }

    /** Reconfigure les pinceaux au style courant. */
    protected abstract void applyStyle();

    /** L'abscisse posée (le harnais la lit). */
    public float poseX() {
        return poseX;
    }

    /** L'ordonnée posée (le harnais la lit). */
    public float poseY() {
        return poseY;
    }

    /** La largeur de la carte (le harnais la lit). */
    public float width() {
        return cardW;
    }

    /** La hauteur de la carte (le harnais la lit). */
    public float height() {
        return cardH;
    }

    /**
     * Le doigt tombe-t-il dans le cadre de la carte ? La question du
     * geste se posée, l'action reste à la carte.
     *
     * @param x l'abscisse du doigt (px, coordonnées de la vue)
     * @param y l'ordonnée du doigt (px, coordonnées de la vue)
     * @return vrai quand le doigt vit dans le cadre
     */
    protected boolean contains(float x, float y) {
        return x >= poseX && x < poseX + cardW && y >= poseY && y < poseY + cardH;
    }

    /**
     * Pose l'ordonnée : SOUS l'ancre d'abord (le bord bas de la ligne
     * + l'écart), DESSUS sans la place (le bord haut - l'écart - la
     * carte), collée au mur du haut au pire — jamais SUR la ligne.
     *
     * @param anchorTop    le bord haut de la ligne d'ancre (px)
     * @param anchorBottom le bord bas de la ligne d'ancre (px)
     * @param margin       la marge aux murs (px)
     * @param gap          l'écart à la ligne (px)
     * @param height       la hauteur de la carte (px)
     * @param viewHeight   la hauteur de la vue (px)
     */
    protected void placeY(float anchorTop, float anchorBottom,
            float margin, float gap, float height, float viewHeight) {
        float belowTop = anchorBottom + gap;
        if (belowTop + height <= viewHeight - margin) {
            poseY = belowTop;
        } else {
            poseY = Math.max(margin, anchorTop - gap - height);
        }
    }

    /**
     * Pose l'abscisse au bord gauche du texte, bornée aux murs de la
     * vue.
     *
     * @param margin   la marge aux murs (px)
     * @param metrics  la géométrie courante de la vue
     */
    protected void placeX(float margin, PopupMetrics metrics) {
        poseX = Math.max(margin,
                Math.min(metrics.paddingLeft(),
                        metrics.viewWidth() - cardW - margin));
    }

    /**
     * La largeur de la carte : la largeur voulue, bornée au style
     * (moins l'amortissement propre à la carte) et aux murs de la vue.
     *
     * @param wanted   la largeur mesurée (px)
     * @param minStyle la borne basse du style, amortie (dp)
     * @param maxStyle la borne haute du style, amortie (dp)
     * @param margin   la marge aux murs (px)
     * @param metrics  la géométrie courante de la vue
     * @return la largeur posée (px)
     */
    protected float clampWidth(float wanted, int minStyle, int maxStyle,
            float margin, PopupMetrics metrics) {
        return Math.min(Math.max(wanted, dp(minStyle)),
                Math.min(dp(maxStyle), metrics.viewWidth() - 2 * margin));
    }

    /**
     * La croix de fermeture au trait (le coin haut droit — sept
     * fichiers la traçaient à la main).
     *
     * @param canvas le canevas de la vue
     * @param cx     le centre de la croix (px)
     * @param cy     le centre de la croix (px)
     * @param paint  l'encre du trait
     */
    protected void drawCross(Canvas canvas, float cx, float cy, Paint paint) {
        float s = dp(5);
        canvas.drawLine(cx - s, cy - s, cx + s, cy + s, paint);
        canvas.drawLine(cx - s, cy + s, cx + s, cy - s, paint);
    }

    /**
     * Coupe le texte à la largeur donnée, l'ellipse À LA FIN (la
     * tête se garde).
     */
    protected String ellipsizeTail(String text, Paint paint, float width) {
        if (paint.measureText(text) <= width || text.length() < 2) {
            return text;
        }
        StringBuilder out = new StringBuilder(text);
        while (out.length() > 1 && paint.measureText(out.toString()) > width) {
            out.deleteCharAt(out.length() - 1);
        }
        out.append('…');
        return out.toString();
    }

    /**
     * Coupe le texte à la largeur disponible, l'ellipse AU DÉBUT
     * (la queue se garde — le chemin d'un fichier s'y lit par sa fin).
     */
    protected String ellipsizeHead(String text, Paint paint, float maxWidth) {
        if (paint.measureText(text) <= maxWidth || text.length() < 2) {
            return text;
        }
        String tail = text.substring(1);
        while (tail.length() > 1 && paint.measureText("…" + tail) > maxWidth) {
            tail = tail.substring(1);
        }
        return "…" + tail;
    }

    /** Le dp de l'écran (px). */
    protected float dp(int value) {
        return value * density;
    }

    /** Le dp de l'écran (px). */
    protected float dp(float value) {
        return value * density;
    }

    /**
     * La pose vient d'être posée (la FIN de {@code place()} de la carte
     * l'appelle) : le flot s'y nourrit — la pose IDENTIQUE ne déclenche
     * rien (la carte qui redessine au même endroit ne tremble pas), la
     * pose changée y fait naître un glissement DEPUIS la pose courante.
     */
    protected void poseSettled() {
        flot.poseTo(poseX, poseY);
    }

    /** Le décalage horizontal du flot (px — nul au repos ; la vue y
     * translate le dessin de la carte). */
    public float floatOffsetX() {
        return flot.offsetX();
    }

    /** Le décalage vertical du flot (px — nul au repos ; la vue y
     * translate le dessin de la carte). */
    public float floatOffsetY() {
        return flot.offsetY();
    }

    /** Le flot vit-il (la vue y réclame la trame suivante). */
    public boolean floating() {
        return flot.isRunning();
    }

    /**
     * Le doigt TRAÎNE dans la carte (au-delà du seuil de glisse) : la
     * fenêtre de rangées suit la traîne — LE DÉFILEMENT DES LISTES
     * (le retour du terrain : « même celui avec Scroll je ne peux
     * scroller » — la barre se dessinait, le doigt ne la servait pas).
     * La base ne défile pas (les cartes sans liste y consomment).
     *
     * @param dy le delta du doigt depuis la trame d'avant (px, positif
     *           vers le bas)
     * @return vrai quand la fenêtre a bougé
     */
    public boolean dragRows(float dy) {
        return false;
    }
}
