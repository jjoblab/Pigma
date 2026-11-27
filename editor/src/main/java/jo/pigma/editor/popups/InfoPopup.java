package jo.pigma.editor.popups;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import java.util.ArrayList;
import java.util.List;

/**
 * La popup d'info ancrée (le paquet des popups, l'arborescence du
 * prompt) : la documentation rapide d'un mot — un TITRE (le mot,
 * encre pleine, gras) et un CORPS BALISÉ replié à la largeur (le
 * sous-ensemble des serveurs : titres, listes, code, liens — le
 * défaut du rapport des popups : le corps vivait NU et COUPÉ à
 * quatre rangées), au-dessus de la ligne d'ancrage (sous elle sans
 * la place), bornée aux bords de la vue. Le corps QUI DÉPASSE se
 * DÉFILE : le doigt qui traîne la popup y fait glisser le contenu
 * (la hauteur se borne à la vue, la queue ne se coupe plus en
 * « … » — la carte défilante du rapport) ; l'indicateur « i sur N »
 * y dit la position. L'état (titre, corps, ancrage, défilement) et
 * le dessin vivent ici ; la vue fournit la géométrie à la trame.
 *
 * <p>Le remplacement tardif ({@link #replace}) ne vaut qu'à la MÊME
 * position : une popup bougée ou rangée n'est pas évincée par une
 * réponse périmée (le survol asynchrone d'un serveur lent). Le
 * remplacement touche au CORPS — le titre (le mot questionné) reste
 * le sien.</p>
 */
public final class InfoPopup {

    /**
     * Le corps de l'ATTENTE (D-P1 du rapport des popups) : la question
     * part au fournisseur, la popup vit immédiate — « … » tient la
     * place le temps du vol, la réponse la remplace (ou rend la place
     * à la description de l'index local si le fournisseur ne sait pas).
     */
    public static final String PENDING_BODY = "\u2026";

    /** La part de la vue que la carte bornée peut couvrir (le défilement vit dessous). */
    private static final float MAX_VIEW_PART = 0.45f;

    /** Le seuil du glissement qui distingue le défilement du tap (dp). */
    private static final int DRAG_SLOP_DP = 4;

    /** Le titre montré (null : aucun titre, ou la popup est rangée). */
    private String title;

    /** Le corps montré (null : la popup est rangée). */
    private String body;

    /** Le corps est-il balisé (le Markdown du serveur) ? */
    private boolean markdown;

    /** La ligne d'ancrage. */
    private int line;

    /** La colonne d'ancrage. */
    private int column;

    private final Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bodyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint background = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint border = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint scrollPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    /** Le style posé par l'hôte : fond translucide, encres, tailles, forme. */
    private PopupStyle style = PopupStyle.defaults();

    /** Le panneau de documentation balisée (la mise en page du corps). */
    private final MarkdownPane pane = new MarkdownPane(1.0f);

    /** L'animation d'ouverture et de sortie (le fondu et la glisse). */
    private final PopupAnimation animation = new PopupAnimation();

    /** La pose de la trame d'avant — la sortie s'y joue. */
    private final RectF closingBox = new RectF();
    private String closingTitle;
    private float closingLeft;
    private float closingTop;
    private float closingRadius;
    private float closingRowHeight;
    private float closingScroll;
    private List<MarkdownPane.Row> closingRows;

    /** Les rangées du corps courant (la dernière mise en page). */
    private List<MarkdownPane.Row> rows;

    /** Le défilement du corps (px, positif vers le bas). */
    private float scrollY;

    /** Le doigt qui traîne : sa dernière ordonnée (le défilement suit). */
    private float lastDragY;

    /** Le doigt a-t-il traîné au-delà du seuil (le tap ne se confond pas) ? */
    private boolean dragged;

    /** L'étiquette du bouton ANNULER (null : aucun vol à annuler —
     * le petit bouton ne vit que pendant l'attente d'une réponse
     * en vol, l'assistant de l'hôte l'arme au départ et le range à
     * l'issue). */
    private String cancelLabel;

    /** Le rectangle du bouton annuler (posé à la trame, le doigt y
     * vise). */
    private final RectF cancelRect = new RectF();

    private float density = 1.0f;

    /**
     * Une popup neuve, ses pinceaux propres au style par défaut.
     *
     * @param density la densité de l'écran (le dp en pixels)
     */
    public InfoPopup(float density) {
        this.density = density;
        applyStyle();
    }

    /**
     * Pose le style de l'hôte : le fond translucide, les encres, la
     * taille des textes, la forme — les pinceaux s'y reconfigurent.
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

    /** Reconfigure les pinceaux au style courant. */
    private void applyStyle() {
        titlePaint.setColor(style.textArgb());
        titlePaint.setTextSize(style.textSizeSp() * density);
        titlePaint.setFakeBoldText(true);
        bodyPaint.setColor(style.hintArgb());
        bodyPaint.setTextSize(style.textSizeSp() * density);
        background.setStyle(Paint.Style.FILL);
        background.setColor(style.backgroundArgb());
        border.setStyle(Paint.Style.STROKE);
        border.setStrokeWidth(density);
        border.setColor(style.accentArgb());
        scrollPaint.setColor((style.borderArgb() & 0x00FFFFFF) | 0x99000000);
        pane.setStyle(style);
    }

    /**
     * Montre la documentation rapide : un titre (le mot questionné,
     * null pour aucun) et un corps — nu ou balisé. Le corps vide ou
     * nul range la popup. Le défilement se range aussi (le corps neuf
     * repart du début).
     *
     * @param shownTitle le titre, ou null
     * @param shownBody  le corps de la documentation
     * @param atLine     la ligne d'ancrage
     * @param atColumn   la colonne d'ancrage
     */
    public void show(String shownTitle, String shownBody, int atLine, int atColumn) {
        show(shownTitle, shownBody, false, atLine, atColumn);
    }

    /**
     * Montre la documentation rapide : un titre (le mot questionné,
     * null pour aucun) et un corps NU ou BALISÉ (le Markdown des
     * serveurs — titres, listes, code, liens y vivent). Le corps vide
     * ou nul range la popup. Le défilement se range aussi.
     *
     * @param shownTitle  le titre, ou null
     * @param shownBody   le corps de la documentation
     * @param bodyMarkdown le corps est-il balisé ?
     * @param atLine      la ligne d'ancrage
     * @param atColumn    la colonne d'ancrage
     */
    public void show(String shownTitle, String shownBody, boolean bodyMarkdown,
            int atLine, int atColumn) {
        boolean wasShown = body != null;
        body = shownBody == null || shownBody.isEmpty() ? null : shownBody;
        title = body == null ? null : shownTitle;
        markdown = bodyMarkdown && body != null;
        line = atLine;
        column = atColumn;
        scrollY = 0.0f;
        if (body != null && !wasShown) {
            animation.open();
        }
    }

    /**
     * Montre un corps sans titre — le texte seul à l'ancrage.
     *
     * @param shown  le texte à montrer, vide ou null pour ranger
     * @param atLine la ligne d'ancrage
     * @param atColumn la colonne d'ancrage
     */
    public void show(String shown, int atLine, int atColumn) {
        show(null, shown, atLine, atColumn);
    }

    /**
     * Remplace le CORPS par une réponse TARDIVE — à la même position
     * seulement (une popup bougée ou rangée n'est pas évincée) ; le
     * titre reste le sien.
     *
     * @param shown    la réponse tardive
     * @param atLine   la ligne d'ancrage de la question
     * @param atColumn la colonne d'ancrage de la question
     * @return vrai si la popup vivait encore à cette position
     */
    public boolean replace(String shown, int atLine, int atColumn) {
        return replace(shown, false, atLine, atColumn);
    }

    /**
     * Remplace le CORPS par une réponse TARDIVE, NUE ou BALISÉE — à
     * la même position seulement (une popup bougée ou rangée n'est
     * pas évincée) ; le titre reste le sien, le défilement se range.
     *
     * @param shown     la réponse tardive
     * @param bodyMarkdown la réponse est-elle balisée ?
     * @param atLine    la ligne d'ancrage de la question
     * @param atColumn  la colonne d'ancrage de la question
     * @return vrai si la popup vivait encore à cette position
     */
    public boolean replace(String shown, boolean bodyMarkdown,
            int atLine, int atColumn) {
        if (body == null || line != atLine || column != atColumn) {
            return false;
        }
        if (shown != null && !shown.isEmpty()) {
            body = shown;
            markdown = bodyMarkdown;
            scrollY = 0.0f;
        }
        return true;
    }

    /** Range la popup (l'appui suivant, une édition, un débranchement). */
    public void dismiss() {
        if (body != null) {
            animation.close();
        }
        body = null;
        title = null;
        scrollY = 0.0f;
        cancelLabel = null;
    }

    /**
     * ARME le bouton ANNULER (B2) : la réponse en vol se laisse
     * arrêter au doigt — le petit bouton vit au bas de la carte
     * tant que l'étiquette pose, le tap dessus se lit à
     * {@link #cancelHit(float, float)}.
     *
     * @param label l'étiquette du bouton (null : le bouton se range)
     */
    public void armCancel(String label) {
        cancelLabel = label;
    }

    /**
     * Le doigt est-il sur le bouton ANNULER armé ? (Le doigt qui
     * vise la carte pendant l'attente : le bouton au bas.)
     *
     * @param x l'abscisse du doigt (px, coordonnées de la vue)
     * @param y l'ordonnée du doigt (px, coordonnées de la vue)
     * @return vrai quand le bouton vit et que le doigt y tombe
     */
    public boolean cancelHit(float x, float y) {
        if (cancelLabel == null || cancelRect.isEmpty()) {
            return false;
        }
        // LA CIBLE ≥ 48 dp (B7 — les cartes d'IA au doigt à l'aise) :
        // la pastille dessinée respire à 28 dp, le DOIGT vise plus
        // large — la cible s'étend autour d'elle, BORNÉE À LA BOÎTE
        // (le doigt hors de la popup appartient au texte)
        float grow = 10.0f * density;
        float left = Math.max(closingBox.left, cancelRect.left - grow);
        float right = Math.min(closingBox.right, cancelRect.right + grow);
        float top = Math.max(closingBox.top, cancelRect.top - grow);
        float bottom = Math.min(closingBox.bottom, cancelRect.bottom + grow);
        return x >= left && x <= right && y >= top && y <= bottom;
    }

    /**
     * Le bouton ANNULER vit-il ? (La carte s'annonce « annulable »
     * tant que la réponse vole.)
     */
    public boolean cancelArmed() {
        return cancelLabel != null;
    }

    /**
     * La pose du bouton ANNULER (la dernière trame l'a posée — le
     * harnais y vise le doigt ; vide quand le bouton ne vit pas).
     *
     * @return la pose du bouton, une copie
     */
    public RectF cancelRect() {
        return new RectF(cancelRect);
    }

    /**
     * L'animation vit-elle encore (ouverture ou sortie) — la vue y
     * invalide la trame suivante.
     */
    public boolean isAnimating() {
        return animation.isRunning();
    }

    /** Le corps montré (null : la popup est rangée). */
    public String text() {
        return body;
    }

    /** Le corps est-il balisé ? (le harnais le lit). */
    public boolean markdown() {
        return markdown;
    }

    /** Le titre montré (null : aucun ou la popup est rangée). */
    public String title() {
        return title;
    }

    /** La ligne d'ancrage (-1 : la popup est rangée). */
    public int line() {
        return body == null ? -1 : line;
    }

    /** La colonne d'ancrage (-1 : la popup est rangée). */
    public int column() {
        return body == null ? -1 : column;
    }

    /** Le défilement courant du corps (le harnais le lit). */
    public float scrollY() {
        return scrollY;
    }

    /**
     * La pose courante de la popup (le harnais y vise le doigt du
     * défilement — la trame qui précède l'a posée) ; vide si jamais
     * posée.
     *
     * @return la pose de la dernière trame
     */
    public RectF box() {
        return new RectF(closingBox);
    }

    /**
     * Le doigt qui TOMBE sur la popup : le défilement s'y arme (le
     * corps qui dépasse se traîne), le tap qui ne traîne pas se
     * reconnaîtra à son relâchement. Hors de la popup : faux.
     *
     * @param x l'abscisse du doigt (px, coordonnées de la vue)
     * @param y l'ordonnée du doigt (px, coordonnées de la vue)
     * @return vrai quand la popup a pris le doigt
     */
    public boolean onDown(float x, float y) {
        lastDragY = y;
        dragged = false;
        return body != null && closingBox.contains(x, y);
    }

    /**
     * Le doigt qui TRAÎNE : le corps suit (le défilement se borne au
     * contenu — le corps qui tient ne se traîne pas).
     *
     * @param y l'ordonnée du doigt (px, coordonnées de la vue)
     */
    public void onDrag(float y) {
        if (body == null) {
            return;
        }
        float dy = y - lastDragY;
        if (Math.abs(dy) > dp(DRAG_SLOP_DP)) {
            dragged = true;
        }
        lastDragY = y;
        scrollY = clampScroll(scrollY + dy);
    }

    /**
     * Le doigt qui RELÂCHE : vrai quand il a TRAÎNÉ (le geste de
     * défilement se consomme), faux quand le tap se rend à l'appelant
     * (le tap sur la popup la range — la façon d'avant demeurait).
     *
     * @return vrai quand la popup a pris le geste entier
     */
    public boolean onUp() {
        boolean took = dragged;
        dragged = false;
        return took;
    }

    /** Le défilement borné au contenu (0 : le contenu entier se voit). */
    private float clampScroll(float value) {
        float contentHeight = pane.height(rows);
        float visible = closingBox.height() - 24.0f * density;
        float max = Math.max(0.0f, contentHeight - visible);
        return Math.max(0.0f, Math.min(max, value));
    }

    /**
     * Dessine la popup : au-dessus de la ligne d'ancrage (sous elle
     * sans la place), bornée aux marges — le titre sur sa rangée, le
     * corps BALISÉ replié à la largeur et DÉFILÉ (le pouce de
     * défilement se montre quand le contenu dépasse), l'indicateur de
     * position « i sur N » au titre quand le contenu déborde ; la
     * popup reste dans la vue.
     *
     * @param canvas    le canevas de la trame
     * @param anchorTop le bord haut de la ligne d'ancrage (px écran)
     * @param metrics   la géométrie de la vue
     */
    public void draw(Canvas canvas, float anchorTop, PopupMetrics metrics) {
        boolean closing = animation.isClosing();
        if (body == null && !closing) {
            return;
        }
        if (body == null) {
            // la sortie se joue sur la dernière pose connue : la
            // géométrie de la trame d'avant demeure (les champs ne se
            // touchent pas)
            drawClosingFrame(canvas);
            return;
        }
        float padding = 12.0f * density;
        float available = metrics.viewWidth() - metrics.paddingLeft()
                - metrics.paddingRight() - 2.0f * padding;
        List<MarkdownDoc.Block> blocks = blocksOf(body, markdown);
        rows = pane.layout(blocks, available, 0);
        float contentHeight = pane.height(rows);
        float rowHeight = style.textSizeSp() * density * 1.3f + 4.0f * density;
        float maxHeight = Math.min(
                metrics.viewHeight() * MAX_VIEW_PART,
                Math.max(rowHeight, metrics.viewHeight() - 2 * metrics.paddingTop()));
        float boxHeight = Math.min(padding + rowHeight + contentHeight
                + padding / 2.0f, maxHeight);
        float boxWidth = boxWidth(available, padding);
        float left = Math.min(
                Math.max(metrics.paddingLeft(),
                        metrics.paddingLeft() + column * metrics.charWidth() - padding),
                metrics.viewWidth() - metrics.paddingRight() - boxWidth);
        float top;
        if (anchorTop - boxHeight - 4.0f * density >= metrics.paddingTop()) {
            top = anchorTop - boxHeight - 4.0f * density;
        } else {
            top = Math.min(anchorTop + metrics.lineHeight() + 4.0f * density,
                    metrics.viewHeight() - boxHeight);
        }
        float radius = style.cornerRadiusDp() * density;
        // la pose se garde : la sortie s'y jouera (drawClosingFrame)
        closingBox.set(left, top, left + boxWidth, top + boxHeight);
        closingRowHeight = rowHeight;
        closingLeft = left;
        closingTop = top;
        closingRadius = radius;
        closingTitle = title;
        closingRows = rows;
        closingScroll = scrollY;
        // le fondu et la glisse de l'ouverture
        float shift = animation.shiftPx(density);
        int alpha = (int) (animation.alphaFactor() * 255.0f);
        int save = canvas.save();
        canvas.translate(0.0f, shift);
        canvas.saveLayerAlpha(left - 2.0f, top - 2.0f, left + boxWidth + 2.0f,
                top + boxHeight + 2.0f, Math.max(0, Math.min(255, alpha)));
        canvas.drawRoundRect(left, top, left + boxWidth, top + boxHeight,
                radius, radius, background);
        canvas.drawRoundRect(left, top, left + boxWidth, top + boxHeight,
                radius, radius, border);
        float baseline = top + padding + rowHeight * 0.3f;
        if (title != null) {
            canvas.drawText(title, left + padding, baseline, titlePaint);
            baseline += rowHeight;
        }
        // le corps défilé : la coupe au bas de la boîte borne la peinture
        int clip = canvas.save();
        canvas.clipRect(left, top, left + boxWidth, top + boxHeight);
        pane.draw(canvas, rows, left + padding, baseline - scrollY,
                boxWidth - 2 * padding);
        drawScrollbar(canvas, rows, contentHeight, boxHeight);
        canvas.restoreToCount(clip);
        // LE BOUTON ANNULER (B2) : la réponse en vol se laisse
        // arrêter — le petit bouton au bas de la carte, à l'encre
        // d'action, pendant l'attente seule
        if (cancelLabel != null) {
            float pillHeight = 28.0f * density;
            float pillWidth = Math.min(boxWidth - 2 * padding,
                    bodyPaint.measureText(cancelLabel) + 24.0f * density);
            float pillLeft = left + boxWidth - padding - pillWidth;
            float pillTop = Math.max(top, top + boxHeight - pillHeight
                    - 6.0f * density);
            cancelRect.set(pillLeft, pillTop, pillLeft + pillWidth,
                    pillTop + pillHeight);
            background.setColor(style.accentArgb());
            canvas.drawRoundRect(cancelRect, pillHeight / 2.0f,
                    pillHeight / 2.0f, background);
            bodyPaint.setColor(0xFFFFFFFF);
            canvas.drawText(cancelLabel,
                    cancelRect.centerX() - bodyPaint.measureText(cancelLabel)
                            / 2.0f,
                    cancelRect.centerY() + density * 2.5f, bodyPaint);
            applyStyle();
        } else {
            cancelRect.setEmpty();
        }
        canvas.restore();
        canvas.restore();
    }

    /**
     * La trame de SORTIE : la popup se joue sur sa dernière pose
     * connue — l'alpha descend, la glisse remonte au caret ; les
     * champs de pose se relisent tels quels.
     */
    private void drawClosingFrame(Canvas canvas) {
        int alpha = (int) (animation.alphaFactor() * 255.0f);
        if (alpha <= 0) {
            return;
        }
        int save = canvas.save();
        canvas.translate(0.0f, animation.shiftPx(1.5f));
        canvas.saveLayerAlpha(closingBox.left - 2.0f, closingBox.top - 2.0f,
                closingBox.right + 2.0f, closingBox.bottom + 2.0f,
                Math.max(0, Math.min(255, alpha)));
        canvas.drawRoundRect(closingBox.left, closingBox.top, closingBox.right,
                closingBox.bottom, closingRadius, closingRadius, background);
        canvas.drawRoundRect(closingBox.left, closingBox.top, closingBox.right,
                closingBox.bottom, closingRadius, closingRadius, border);
        float baseline = closingTop + 12.0f * 1.5f + closingRowHeight * 0.3f;
        if (closingTitle != null) {
            canvas.drawText(closingTitle, closingLeft + 12.0f * 1.5f, baseline,
                    titlePaint);
            baseline += closingRowHeight;
        }
        if (closingRows != null) {
            pane.draw(canvas, closingRows, closingLeft + 12.0f * 1.5f,
                    baseline - closingScroll,
                    closingBox.width() - 24.0f * 1.5f);
        }
        canvas.restore();
        canvas.restore();
    }

    /** Les blocs du corps : balisé s'il l'est, paragraphes sinon. */
    private List<MarkdownDoc.Block> blocksOf(String text, boolean rich) {
        if (rich || MarkdownDoc.isRich(text)) {
            return MarkdownDoc.parse(text);
        }
        List<MarkdownDoc.Block> blocks = new ArrayList<>();
        for (String row : text.split("\n")) {
            if (!row.isBlank()) {
                blocks.add(new MarkdownDoc.Block(MarkdownDoc.PARAGRAPH, 0,
                        List.of(new MarkdownDoc.Span(row, false, false,
                                false, "")), "", ""));
            }
        }
        return blocks;
    }

    /** La largeur de la boîte : la plus large LIGNE du corps (le
     * repli ne l'élargit jamais), bornée à la place. */
    private float boxWidth(float available, float padding) {
        float widest = title == null ? 0.0f : titlePaint.measureText(title);
        for (String row : body.split("\n")) {
            widest = Math.max(widest, bodyPaint.measureText(row));
        }
        return Math.min(available, widest) + 2.0f * padding;
    }

    /**
     * Le pouce de défilement : le contenu qui dépasse se montre — le
     * pouce au prorata du visible, la course bornée à la boîte.
     */
    private void drawScrollbar(Canvas canvas, List<MarkdownPane.Row> rows,
            float contentHeight, float boxHeight) {
        float visible = boxHeight - 24.0f * density;
        if (contentHeight <= visible || visible <= 0.0f) {
            return;
        }
        float trackTop = closingBox.top + 12.0f * density;
        float trackBottom = closingBox.bottom - 6.0f * density;
        float thumb = (trackBottom - trackTop) * visible / contentHeight;
        float trackSpan = trackBottom - trackTop - thumb;
        float max = Math.max(1.0f, contentHeight - visible);
        float thumbTop = trackTop + trackSpan * scrollY / max;
        canvas.drawRoundRect(closingBox.right - 4.0f * density, thumbTop,
                closingBox.right - 2.0f * density, thumbTop + thumb,
                1.5f * density, 1.5f * density, scrollPaint);
    }

    private float dp(int value) {
        return value * density;
    }
}
