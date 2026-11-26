package jo.pigma.editor.gutter;

import android.graphics.Canvas;
import android.graphics.Paint;

import jo.pigma.editor.popups.PopupStyle;

/**
 * La MINIMAP (la bande de carte du document à la référence —
 * Android Studio l'hérite du plugin CodeGlance, Sublime l'a
 * popularisée) : chaque ligne du document y vit en une rangée
 * d'environ deux pixels — les caractères y deviennent des tirets à
 * la teinte du texte, les lignes vides respirent. La FENÊTRE
 * courante s'y cadre (le rectangle translucide : ce que l'œil voit
 * du document) ; le doigt y navigue (le tap pose le curseur à la
 * ligne visée, le glissé suit le doigt).
 *
 * <p>Le panneau vit au bord droit de la vue, coiffé de LA SECTION
 * ANNULER/RÉTABLIR (deux pastilles carrées — la demande : les deux boutons en haut de la région de la
 * minimap, comme la barre d'outils de la référence au-dessus de sa
 * carte).</p>
 */
public final class Minimap {

    /** La hauteur d'une rangée du document (px). */
    private static final float ROW_PX = 2.2f;

    /** La largeur de la bande (dp). */
    private static final int WIDTH_DP = 42;

    /** Le côté d'une pastille annuler/rétablir (dp). */
    private static final int BUTTON_DP = 30;

    /** L'écart entre les pastilles (dp). */
    private static final int BUTTON_GAP_DP = 6;

    /** La hauteur de la section des pastilles (dp) — la colonne À CÔTÉ de la bande. */
    private static final int BAR_DP = 40;

    /** L'alpha du cadre de la fenêtre courante. */
    private static final float VIEWPORT_ALPHA = 0.16f;

    /** L'alpha d'un tiret de caractère. */
    private static final float DASH_ALPHA = 0.55f;

    /** L'interface des pastilles — la vue y branchera sa pile d'annulation. */
    public interface UndoRedoActions {

        /** La pile d'annulation vit-elle ? */
        boolean canUndo();

        /** La pile de rétablissement vit-elle ? */
        boolean canRedo();

        /** La dernière écriture se défait. */
        void undo();

        /** La dernière annulation se rétablit. */
        void redo();
    }

    private final Paint panelPaint = new Paint();

    private final Paint dashPaint = new Paint();

    private final Paint viewportPaint = new Paint();

    private final Paint buttonPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Paint buttonGlyphPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Paint disabledPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float density = 1.0f;

    /** La couleur du texte du thème (les tirets en prennent la teinte). */
    private int textColor = 0xFF333333;

    /** Le fond du panneau (la translucence du thème). */
    private int panelColor = 0x11000000;

    /** Les pastilles d'annulation (l'hôte les pose, null : pas de pastilles). */
    private UndoRedoActions actions;

    /** Le dernier état des pastilles (la trame le lit sans questionner la pile). */
    private boolean lastCanUndo;

    /** Le dernier état du rétablissement (pareillement). */
    private boolean lastCanRedo;

    /**
     * Une minimap neuve, ses pinceaux propres.
     *
     * @param density la densité de l'écran (le dp en pixels)
     */
    public Minimap(float density) {
        this.density = density;
        panelPaint.setStyle(Paint.Style.FILL);
        viewportPaint.setStyle(Paint.Style.FILL);
        buttonPaint.setStyle(Paint.Style.FILL);
        buttonGlyphPaint.setStyle(Paint.Style.STROKE);
        buttonGlyphPaint.setStrokeWidth(1.8f * density);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(1.0f * density);
        applyColors();
    }

    /**
     * Les couleurs du thème : les tirets à la teinte du texte, le
     * panneau translucide, le cadre de fenêtre à l'accent.
     *
     * @param text     la couleur du texte du thème
     * @param panel    le fond du panneau (translucide)
     * @param accent   l'accent du thème (le cadre, les pastilles vives)
     */
    public void setColors(int text, int panel, int accent) {
        textColor = text;
        panelColor = panel;
        applyColors();
        viewportPaint.setColor((accent & 0x00FFFFFF) | 0x2A000000);
        buttonPaint.setColor((accent & 0x00FFFFFF) | 0x33000000);
    }

    /** Reconfigure les pinceaux aux couleurs posées. */
    private void applyColors() {
        panelPaint.setColor(panelColor);
        dashPaint.setColor((textColor & 0x00FFFFFF)
                | ((int) (DASH_ALPHA * 255.0f) << 24));
        borderPaint.setColor((textColor & 0x00FFFFFF) | 0x22000000);
        buttonGlyphPaint.setColor(textColor);
        disabledPaint.setColor((textColor & 0x00FFFFFF) | 0x30000000);
    }

    /**
     * Branche les pastilles d'annulation/rétablissement (la vue y
     * pose sa pile, null les range).
     *
     * @param wired les actions, null pour rien
     */
    public void setUndoRedoActions(UndoRedoActions wired) {
        actions = wired;
    }

    /** La largeur de la bande (px — la vue y resserre son padding droit). */
    public float width() {
        return dp(WIDTH_DP);
    }

    /** La section des pastilles vit-elle ? (l'hôte a branché ses actions). */
    public boolean hasButtons() {
        return actions != null;
    }

    /**
     * Le doigt est-il sur une PASTILLE — et laquelle ? La colonne des
     * pastilles vit à GAUCHE de la bande (annuler en haut, rétablir
     * dessous — la carte du document garde TOUTE sa hauteur).
     *
     * @param x l'abscisse du doigt (px, coordonnées de la vue)
     * @param y l'ordonnée du doigt (px, coordonnées de la vue)
     * @return -1 rien, 0 annuler, 1 rétablir
     */
    public int buttonAt(float x, float y, float viewWidth) {
        if (actions == null) {
            return -1;
        }
        float columnCx = viewWidth - width() - columnWidth() / 2.0f;
        if (y >= dp(6) && y < dp(6) + dp(BUTTON_DP)
                && Math.abs(x - columnCx) <= dp(BUTTON_DP) / 2.0f + dp(2)) {
            return 0;
        }
        if (y >= dp(6) + dp(BUTTON_DP) + dp(BUTTON_GAP_DP)
                && y < dp(6) + 2 * dp(BUTTON_DP) + dp(BUTTON_GAP_DP)
                && Math.abs(x - columnCx) <= dp(BUTTON_DP) / 2.0f + dp(2)) {
            return 1;
        }
        return -1;
    }

    /** La largeur de la colonne des pastilles (dp) — elle vit à gauche de la bande. */
    public float columnWidth() {
        return dp(BUTTON_DP) + dp(8);
    }

    /**
     * La pastille touchée s'exécute (le tap de la vue y passe — le
     * retour dit si le geste s'est consommé).
     *
     * @param which  0 annuler, 1 rétablir
     * @return vrai quand l'action s'est jouée
     */
    public boolean pressButton(int which) {
        if (actions == null) {
            return false;
        }
        if (which == 0 && actions.canUndo()) {
            actions.undo();
            return true;
        }
        if (which == 1 && actions.canRedo()) {
            actions.redo();
            return true;
        }
        return false;
    }

    /**
     * Dessine la carte du document : la bande au bord droit (le
     * panneau translucide), chaque ligne visible du DOCUMENT en un
     * tiret (la largeur du tiret suit la longueur de la ligne), le
     * cadre de la fenêtre courante par-dessus, puis LA SECTION DES
     * PASTILLES en tête (annuler, rétablir — vives quand leurs piles
     * vivent, éteintes sinon).
     *
     * @param canvas        le canevas de la trame
     * @param viewHeight    la hauteur de la vue (px)
     * @param viewWidth     la largeur de la vue (px)
     * @param firstLine     la première ligne visible du document
     * @param lastLine      la dernière ligne visible du document
     * @param lineCount     le compte de lignes du document
     * @param lineLength    la longueur de chaque ligne (la vue la lit
     *                      au tampon — un itérateur indexé)
     */
    public void draw(Canvas canvas, float viewHeight, float viewWidth,
            int firstLine, int lastLine, int lineCount,
            LineLength lineLength) {
        float left = viewWidth - width();
        float top = 0.0f;
        // la carte du document prend TOUTE la hauteur (les pastilles
        // vivent À CÔTÉ, dans leur colonne à gauche — plus de section
        // en tête)
        float mapTop = 0.0f;
        float mapH = Math.max(1.0f, viewHeight - mapTop);
        canvas.drawRect(left, top, viewWidth, viewHeight, panelPaint);
        canvas.drawLine(left, top, left, viewHeight, borderPaint);
        // la carte se resserre au document ENTIER (chaque ligne y
        // tient sa rangée — le document plus haut que la carte s'y
        // comprime, la fenêtre s'y cadre au prorata)
        float rowH = Math.min(ROW_PX, mapH / Math.max(1, lineCount));
        float scaleY = lineCount * rowH > mapH
                ? mapH / (lineCount * rowH) : 1.0f;
        // les tirets : chaque ligne du document en un trait à sa
        // longueur (borné à la largeur de la bande)
        for (int line = 0; line < lineCount; line++) {
            float y = mapTop + line * rowH * scaleY;
            if (y > viewHeight) {
                break;
            }
            int length = lineLength.lengthOf(line);
            if (length <= 0) {
                continue;
            }
            float w = Math.min(width() - dp(6), length * dpF(0.9f));
            canvas.drawRect(left + dp(3), y, left + dp(3) + w,
                    y + Math.max(1.0f, rowH - 0.6f), dashPaint);
        }
        // le cadre de la fenêtre courante (ce que l'œil voit)
        float vpTop = mapTop + firstLine * rowH * scaleY;
        float vpBottom = mapTop + (lastLine + 1) * rowH * scaleY;
        canvas.drawRect(left, vpTop, viewWidth,
                Math.min(viewHeight, vpBottom), viewportPaint);
        // LA COLONNE DES PASTILLES (annuler, rétablir) À GAUCHE de la
        // bande : deux carrés arrondis empilés, la flèche recourbée à
        // la référence (annuler : la flèche revient ; rétablir : elle
        // repart) — éteinte quand sa pile est morte
        if (actions != null) {
            lastCanUndo = actions.canUndo();
            lastCanRedo = actions.canRedo();
            float cx = viewWidth - width() - columnWidth() / 2.0f;
            drawUndoButton(canvas, cx, dp(6), true);
            drawUndoButton(canvas, cx, dp(6) + dp(BUTTON_DP) + dp(BUTTON_GAP_DP),
                    false);
        }
    }

    /** La pastille d'annulation ou de rétablissement (la flèche recourbée). */
    private void drawUndoButton(Canvas canvas, float cx, float top,
            boolean undo) {
        float side = dp(BUTTON_DP);
        float left = cx - side / 2.0f;
        float cy = top + side / 2.0f;
        boolean alive = undo ? lastCanUndo : lastCanRedo;
        Paint fill = alive ? buttonPaint : panelPaint;
        canvas.drawRoundRect(left, top, left + side, top + side,
                dp(6), dp(6), fill);
        Paint glyph = alive ? buttonGlyphPaint : disabledPaint;
        // la flèche recourbée de la référence : l'arc qui revient
        // (annuler) ou repart (rétablir), la pointe au bout
        float r = side * 0.26f;
        // l'arc se dessine au COMPAS (le Path l'arcTo — le stub du
        // harnais n'a pas drawArc, le Path y vit) : la courbe qui
        // revient (annuler) ou repart (rétablir)
        float arcCx = cx + (undo ? -r * 0.2f : r * 0.2f);
        android.graphics.RectF oval = new android.graphics.RectF(
                arcCx - r, cy - r, arcCx + r, cy + r);
        android.graphics.Path arc = new android.graphics.Path();
        if (undo) {
            arc.arcTo(oval, 180.0f, 160.0f, false);
        } else {
            arc.arcTo(oval, 0.0f, 160.0f, false);
        }
        canvas.drawPath(arc, glyph);
        // la pointe de la flèche (le petit triangle au bout de l'arc)
        float tipX = undo ? cx - r * 1.2f : cx + r * 1.2f;
        android.graphics.Path head = new android.graphics.Path();
        head.moveTo(tipX, cy - r * 0.8f);
        head.lineTo(tipX + (undo ? dp(5) : -dp(5)), cy - r * 0.5f);
        head.lineTo(tipX + (undo ? dp(1) : -dp(1)), cy - r * 1.2f);
        head.close();
        canvas.drawPath(head, glyph);
    }

    /** La lecture des longueurs de lignes — la vue la fournit au tampon. */
    public interface LineLength {

        /**
         * La longueur d'une ligne (sans sa fin de ligne).
         *
         * @param line l'indice de la ligne
         * @return sa longueur en caractères
         */
        int lengthOf(int line);
    }

    /**
     * La ligne du document au doigt (le tap de la vue y convertit —
     * le curseur y saute).
     *
     * @param y          l'ordonnée du doigt (px, coordonnées de la vue)
     * @param viewHeight la hauteur de la vue (px)
     * @param lineCount  le compte de lignes du document
     * @return l'indice de ligne visé, borné
     */
    /**
     * Le bas de la carte dessinée : le document rendu s'y arrête (un
     * document court ne peint rien au-dessous — la zone tactile s'y
     * borne, le doigt y retombe sur le texte).
     *
     * @param viewHeight la hauteur de la vue
     * @param lineCount  le nombre de lignes du document
     * @return l'ordonnée du bas du document rendu
     */
    public float mapBottom(float viewHeight, int lineCount) {
        float mapH = Math.max(1.0f, viewHeight);
        float rowH = Math.min(ROW_PX, mapH / Math.max(1, lineCount));
        float scaleY = lineCount * rowH > mapH
                ? mapH / (lineCount * rowH) : 1.0f;
        return Math.min(viewHeight, lineCount * rowH * scaleY);
    }

    public int lineAt(float y, float viewHeight, int lineCount) {
        float mapTop = 0.0f;
        float mapH = Math.max(1.0f, viewHeight - mapTop);
        float rowH = Math.min(ROW_PX, mapH / Math.max(1, lineCount));
        float scaleY = lineCount * rowH > mapH
                ? mapH / (lineCount * rowH) : 1.0f;
        int line = (int) ((y - mapTop) / (rowH * scaleY));
        return Math.max(0, Math.min(lineCount - 1, line));
    }

    private float dp(int value) {
        return value * density;
    }

    /** Le dp fractionnaire (les tirets y respirent à 0,9 dp par caractère). */
    private float dpF(float value) {
        return value * density;
    }
}
