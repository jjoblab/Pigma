package jo.pigma.editor.popups;

import android.graphics.Canvas;
import android.graphics.Paint;

import java.util.ArrayList;
import java.util.List;

/**
 * La carte d'actions rapides — L'AMPOULE JAUNE de la gouttière :
 * le doigt sur l'ampoule y ouvre les actions de la ligne (les
 * intentions de l'atelier) — chaque rangée porte l'éclair de
 * l'action à gauche et son libellé à droite, coiffées du titre
 * « Actions rapides · ligne N ». Les actions naissent du CONTEXTE :
 * l'éditeur y met les siennes (commenter, casse, dupliquer,
 * supprimer la ligne), l'hôte y pose les siennes par son
 * fournisseur. La carte se pose SOUS la ligne de l'ampoule
 * (au-dessus sans la place), bornée aux murs ; le fond translucide
 * et la taille viennent du style posé par l'hôte.
 *
 * <p>La carte ne prend jamais le focus ; le choix d'une rangée la
 * REFERME avant d'exécuter — l'action ne survit jamais au choix ;
 * le reste de la carte consomme sans agir, hors d'elle le geste
 * poursuit sa vie vers l'éditeur.</p>
 */
public final class QuickActionCard {

    /** Le plafond de rangées visibles (la fenêtre défile au-delà). */
    private static final int MAX_VISIBLE_ROWS = 10;

    /** La respiration intérieure de la carte (dp). */
    private static final int PAD_DP = 6;

    /** La hauteur du pied de carte (dp). */
    private static final int FOOTER_DP = 30;

    /** La marge aux murs de la vue (dp). */
    private static final int MARGIN_DP = 8;

    /** L'écart entre la ligne et la carte (dp). */
    private static final int GAP_DP = 4;

    /** La largeur d'une icône d'action (dp). */
    private static final int ICON_DP = 22;

    /**
     * Une action rapide : son libellé, ce qu'elle fait — et SON
     * GLYPHE AU TRAIT (la nature de l'action s'y lit d'un coup
     * d'œil : l'éditeur y met ses actions d'édition, l'hôte ses
     * intentions, le serveur ses fonctions, l'assistant ses tâches).
     *
     * @param label  le libellé, jamais vide
     * @param action ce qu'elle fait, jamais null
     * @param glyph  la nature de l'action (son icône au trait)
     */
    public record Entry(String label, Runnable action, Glyph glyph) {

        /**
         * Une entrée SANS nature dite : l'éclair d'action la porte
         * (le défaut d'avant — les entrées qui ne disent rien ne
         * changent pas d'icône).
         *
         * @param label  le libellé, jamais vide
         * @param action ce qu'elle fait, jamais null
         */
        public Entry(String label, Runnable action) {
            this(label, action, Glyph.ACTION);
        }
    }

    /**
     * LA NATURE D'UNE ACTION, AU TRAIT (le retour du terrain : « les
     * options ont toutes la même icône » — la carte agrégait cinq
     * familles sous un éclair unique). Chaque nature porte SON glyphe
     * dessiné à la main, à la grille 24 de la référence, le filet
     * rond de 1,7/24 — la façon de {@code SeverityIcon}.
     */
    public enum Glyph {

        /** L'éclair d'action (le défaut — l'entrée ne dit rien). */
        ACTION,
        /** Le commentaire « // » (commenter / décommenter). */
        COMMENT,
        /** La flèche haute (en majuscules). */
        UPPERCASE,
        /** La flèche basse (en minuscules). */
        LOWERCASE,
        /** Les deux feuilles superposées (dupliquer). */
        DUPLICATE,
        /** La poubelle (supprimer la ligne). */
        TRASH,
        /** Les crochets ouvrant et fermant (entourer de). */
        SURROUND,
        /** La feuille au coin plié (l'extrait de code). */
        SNIPPET,
        /** Le calendrier (la date du jour de l'hôte). */
        CALENDAR,
        /** La loupe (la recherche de l'hôte). */
        SEARCH,
        /** La flèche à la barre (l'aller à la définition). */
        GOTO,
        /** L'œil (l'aperçu de la définition). */
        EYE,
        /** Les guillemets (trouver les usages). */
        QUOTES,
        /** Le crayon (renommer le symbole). */
        PENCIL,
        /** Les lignes alignées (formater le document). */
        ALIGN,
        /** L'arbre des symboles (la structure du document). */
        TREE,
        /** La bulle de parole (expliquer — l'assistant). */
        CHAT,
        /** La feuille documentée (documenter — l'assistant). */
        DOC,
        /** La coche cerclée (écrire le test — l'assistant). */
        CHECK
    }

    /** Les actions vivantes de la ligne ancrée (vide : la carte se range). */
    private List<Entry> entries = List.of();

    /** La ligne ancrée (les actions y naissent). */
    private int line;

    /** La première rangée de la fenêtre (le défilement de la carte). */
    private int topRow;

    /** Le nombre de rangées visibles à la pose courante. */
    private int visibleRows;

    private float poseX;

    private float poseY;

    private float cardW;

    private float cardH;

    private float density = 1.0f;

    private PopupStyle style = PopupStyle.defaults();

    /** Les chaînes posées par l'hôte (le titre localisé — la source unique). */
    private PopupStrings strings = PopupStrings.defaults();

    private final Paint cardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint headerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint footerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rulePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bulbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glyphPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint scrollbarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    /**
     * Une carte neuve, ses pinceaux au style par défaut.
     *
     * @param density la densité de l'écran (le dp en pixels)
     */
    public QuickActionCard(float density) {
        this.density = density;
        applyStyle(style);
    }

    /**
     * Pose le style de l'hôte : le fond translucide, les encres, la
     * taille des textes, la forme — la carte s'y reconfigure entière.
     *
     * @param fresh le style posé, null pour celui par défaut
     */
    public void setStyle(PopupStyle fresh) {
        style = fresh == null ? PopupStyle.defaults() : fresh;
        applyStyle(style);
    }

    /** Le style courant (le harnais le lit). */
    public PopupStyle style() {
        return style;
    }

    /**
     * Pose les chaînes de l'hôte (le titre localisé — la source unique).
     *
     * @param fresh les chaînes posées, null pour celles par défaut
     */
    public void setStrings(PopupStrings fresh) {
        strings = fresh == null ? PopupStrings.defaults() : fresh;
    }

    /** Reconfigure les pinceaux au style courant. */
    private void applyStyle(PopupStyle style) {
        cardPaint.setColor(style.backgroundArgb());
        borderPaint.setColor(style.borderArgb());
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(1.0f);
        headerPaint.setColor(style.accentArgb());
        headerPaint.setTextSize(style.secondarySizeSp() * density);
        headerPaint.setFakeBoldText(true);
        textPaint.setColor(style.textArgb());
        textPaint.setTextSize((style.textSizeSp() - 2.0f) * density);
        footerPaint.setColor(style.secondaryArgb());
        footerPaint.setTextSize((style.secondarySizeSp() - 2.0f) * density);
        rulePaint.setColor(style.borderArgb());
        bulbPaint.setColor(style.lightbulbArgb());
        bulbPaint.setStyle(Paint.Style.FILL);
        // LE TRAIT DES GLYPHES : le filet rond de la référence (la
        // grille 24 y vit, la façon de SeverityIcon)
        glyphPaint.setColor(style.secondaryArgb());
        glyphPaint.setStyle(Paint.Style.STROKE);
        glyphPaint.setStrokeWidth(1.6f * density);
        glyphPaint.setStrokeCap(Paint.Cap.ROUND);
        glyphPaint.setStrokeJoin(Paint.Join.ROUND);
        scrollbarPaint.setColor(0x66B5B5B5);
    }

    /**
     * Ouvre la carte sur les actions d'une ligne — la liste vide
     * range la carte.
     *
     * @param shownLine la ligne ancrée
     * @param actions   les actions de la ligne, jamais null
     */
    public void open(int shownLine, List<Entry> actions) {
        List<Entry> alive = new ArrayList<>();
        for (Entry action : actions) {
            if (action != null && action.label() != null
                    && !action.label().isEmpty() && action.action() != null) {
                alive.add(action);
            }
        }
        entries = List.copyOf(alive);
        line = shownLine;
        topRow = 0;
    }

    /** Range la carte (le choix, un nouvel appui, la ligne morte). */
    public void close() {
        entries = List.of();
        line = -1;
        topRow = 0;
        visibleRows = 0;
    }

    /** La carte vit-elle ? */
    public boolean isShown() {
        return !entries.isEmpty();
    }

    /** Les actions vivantes (le harnais les lit). */
    public List<Entry> entries() {
        return entries;
    }

    /** La ligne ancrée, -1 quand la carte se range. */
    public int line() {
        return isShown() ? line : -1;
    }

    /**
     * Le doigt tombé sur la carte lui appartient : la rangée visée
     * se REFERME avant d'exécuter son action — l'ampoule ne survit
     * jamais au choix ; le titre et le pied consomment sans agir.
     * Hors de la carte : faux.
     *
     * @param x l'abscisse du doigt (px, coordonnées de la vue)
     * @param y l'ordonnée du doigt (px, coordonnées de la vue)
     * @return vrai quand la carte a pris le geste
     */
    public boolean onTouchArea(float x, float y) {
        if (!isShown() || x < poseX || x >= poseX + cardW
                || y < poseY || y >= poseY + cardH) {
            return false;
        }
        int row = rowAt(y);
        if (row >= 0) {
            Entry chosen = entries.get(row);
            close();
            chosen.action().run();
        }
        return true;
    }

    /**
     * Dessine la carte : la pose se recalcule d'abord — sous la
     * ligne ancrée (au-dessus sans la place) —, puis le titre
     * coiffant les rangées (l'éclair de l'action, le libellé), la
     * barre de défilement si la fenêtre cache des rangées, le pied
     * qui compte.
     *
     * @param canvas      le canevas de la vue
     * @param anchorTop   le bord haut de la ligne ancrée (px, vue)
     * @param anchorBottom le bord bas de la ligne ancrée (px, vue)
     * @param metrics     la géométrie courante de la vue
     */
    public void draw(Canvas canvas, float anchorTop, float anchorBottom,
            PopupMetrics metrics) {
        if (entries.isEmpty()) {
            cardH = 0.0f;
            return;
        }
        place(anchorTop, anchorBottom, metrics);
        float radius = dp(style.cornerRadiusDp());
        canvas.drawRoundRect(poseX, poseY, poseX + cardW, poseY + cardH,
                radius, radius, cardPaint);
        canvas.drawRoundRect(poseX, poseY, poseX + cardW, poseY + cardH,
                radius, radius, borderPaint);
        float rowH = rowHeight();
        float headerH = dp(style.headerHeightDp());
        float rowsTop = poseY + dp(PAD_DP);
        canvas.drawText(strings.quickActionsAt(line),
                poseX + dp(14), rowsTop + headerH * 0.72f, headerPaint);
        canvas.drawLine(poseX + dp(12), rowsTop + headerH - dp(2),
                poseX + cardW - dp(12), rowsTop + headerH - dp(2), rulePaint);
        float rowsBottom = rowsTop + headerH + visibleRows * rowH;
        for (int at = topRow; at < topRow + visibleRows
                && at < entries.size(); at++) {
            float top = rowsTop + headerH + (at - topRow) * rowH;
            drawEntry(canvas, entries.get(at), top);
        }
        drawScrollbar(canvas, rowsTop + headerH, rowsBottom);
        drawFooter(canvas, rowsBottom);
    }

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
     * L'ordonnée du centre d'une rangée (le harnais y vise le doigt).
     *
     * @param index l'index de l'action
     * @return l'ordonnée du centre, -1 hors carte ou hors fenêtre
     */
    public float rowCenterY(int index) {
        if (!isShown() || index < topRow || index >= topRow + visibleRows) {
            return -1.0f;
        }
        return poseY + dp(PAD_DP) + dp(style.headerHeightDp())
                + (index - topRow + 0.5f) * rowHeight();
    }

    /**
     * Déplace la fenêtre des rangées vers une rangée (la rangée
     * demandée y devient visible — le doigt peut la viser). La
     * fenêtre au-delà du plafond de rangées s'y sert : la carte
     * défile, la rangée demandée y entre.
     *
     * @param index l'index de la rangée à montrer
     */
    public void showRow(int index) {
        if (index < 0 || index >= entries.size()) {
            return;
        }
        if (index < topRow) {
            topRow = index;
        } else if (index >= topRow + visibleRows) {
            topRow = Math.min(index - visibleRows + 1,
                    Math.max(0, entries.size() - visibleRows));
        }
    }

    /**
     * Pose la carte sous la ligne ancrée (au-dessus sans la place,
     * jamais sur elle), le bord gauche borné aux murs ; la fenêtre
     * se resserre à ce que la vue tient, au plus
     * {@value #MAX_VISIBLE_ROWS} rangées.
     */
    private void place(float anchorTop, float anchorBottom, PopupMetrics metrics) {
        float margin = dp(MARGIN_DP);
        float gap = dp(GAP_DP);
        cardW = Math.min(Math.max(measuredWidth(),
                dp(style.minCardWidthDp() - 80)),
                Math.min(dp(style.maxCardWidthDp() - 80),
                        metrics.viewWidth() - 2 * margin));
        int rows = Math.min(MAX_VISIBLE_ROWS, entries.size());
        float available = metrics.viewHeight() - 2 * margin - 2 * dp(PAD_DP)
                - dp(style.headerHeightDp()) - dp(FOOTER_DP);
        while (rows > 1 && 2 * dp(PAD_DP) + dp(style.headerHeightDp())
                + dp(FOOTER_DP) + rows * rowHeight() > available) {
            rows--;
        }
        visibleRows = rows;
        cardH = 2 * dp(PAD_DP) + dp(style.headerHeightDp())
                + visibleRows * rowHeight() + dp(FOOTER_DP);
        float belowTop = anchorBottom + gap;
        if (belowTop + cardH <= metrics.viewHeight() - margin) {
            poseY = belowTop;
        } else {
            poseY = Math.max(margin, anchorTop - gap - cardH);
        }
        poseX = Math.max(margin,
                Math.min(metrics.paddingLeft(),
                        metrics.viewWidth() - cardW - margin));
    }

    /** La hauteur d'une rangée : le style la porte, l'icône y respire. */
    private float rowHeight() {
        return dp(style.rowHeightDp() - 8);
    }

    /** La largeur qu'il faut pour le plus large libellé, respiré. */
    private float measuredWidth() {
        float widest = 0.0f;
        for (Entry entry : entries) {
            widest = Math.max(widest, textPaint.measureText(entry.label()));
        }
        return widest + dp(ICON_DP) + dp(16) + 2 * dp(14);
    }

    /** Une rangée : la nature de l'action AU TRAIT, le libellé. */
    private void drawEntry(Canvas canvas, Entry entry, float top) {
        float rowH = rowHeight();
        float cy = top + rowH / 2.0f;
        float baseline = top + rowH * 0.68f;
        drawGlyph(canvas, entry.glyph(),
                poseX + dp(14) + dp(ICON_DP) / 2.0f, cy, dp(ICON_DP));
        canvas.drawText(entry.label(),
                poseX + dp(14) + dp(ICON_DP) + dp(8), baseline, textPaint);
    }

    /**
     * LE GLYPHE D'UNE NATURE : dessiné à la main, à la grille 24 (le
     * centre donné, les traits en vingt-quatrièmes du côté — la
     * façon de {@code SeverityIcon}, son filet de 1,7/24 aux caps
     * rondes). L'éclair d'action seul demeure PLEIN (la tête de
     * l'ampoule — le défaut d'avant).
     *
     * @param canvas le canevas de la vue
     * @param glyph  la nature de l'action
     * @param cx     l'abscisse du centre (px)
     * @param cy     l'ordonnée du centre (px)
     * @param side   le côté du glyphe (px)
     */
    private void drawGlyph(Canvas canvas, Glyph glyph, float cx, float cy,
            float side) {
        if (glyph == null || glyph == Glyph.ACTION) {
            drawBoltIcon(canvas, cx, cy);
            return;
        }
        float u = side / 24.0f;
        switch (glyph) {
            case COMMENT -> {
                // « // » : les deux barres du commentaire de fin de ligne
                canvas.drawLine(cx - 4 * u, cy - 6 * u, cx - 1 * u, cy + 6 * u,
                        glyphPaint);
                canvas.drawLine(cx + 1 * u, cy - 6 * u, cx + 4 * u, cy + 6 * u,
                        glyphPaint);
            }
            case UPPERCASE -> {
                // la flèche haute : la tige et son chevron
                canvas.drawLine(cx, cy + 6 * u, cx, cy - 6 * u, glyphPaint);
                canvas.drawLine(cx - 4 * u, cy - 2 * u, cx, cy - 6 * u,
                        glyphPaint);
                canvas.drawLine(cx, cy - 6 * u, cx + 4 * u, cy - 2 * u,
                        glyphPaint);
            }
            case LOWERCASE -> {
                // la flèche basse
                canvas.drawLine(cx, cy - 6 * u, cx, cy + 6 * u, glyphPaint);
                canvas.drawLine(cx - 4 * u, cy + 2 * u, cx, cy + 6 * u,
                        glyphPaint);
                canvas.drawLine(cx, cy + 6 * u, cx + 4 * u, cy + 2 * u,
                        glyphPaint);
            }
            case DUPLICATE -> {
                // les deux feuilles superposées
                canvas.drawRoundRect(cx - 8 * u, cy - 6 * u, cx + 2 * u,
                        cy + 4 * u, 2 * u, 2 * u, glyphPaint);
                canvas.drawRoundRect(cx - 2 * u, cy - 8 * u, cx + 8 * u,
                        cy + 2 * u, 2 * u, 2 * u, glyphPaint);
            }
            case TRASH -> {
                // la poubelle : le couvercle, l'anse, le corps
                canvas.drawLine(cx - 6 * u, cy - 5 * u, cx + 6 * u, cy - 5 * u,
                        glyphPaint);
                canvas.drawLine(cx - 2 * u, cy - 8 * u, cx + 2 * u, cy - 8 * u,
                        glyphPaint);
                canvas.drawLine(cx - 2 * u, cy - 8 * u, cx - 2 * u, cy - 5 * u,
                        glyphPaint);
                canvas.drawLine(cx + 2 * u, cy - 8 * u, cx + 2 * u, cy - 5 * u,
                        glyphPaint);
                canvas.drawLine(cx - 5 * u, cy - 3 * u, cx - 4 * u, cy + 8 * u,
                        glyphPaint);
                canvas.drawLine(cx + 5 * u, cy - 3 * u, cx + 4 * u, cy + 8 * u,
                        glyphPaint);
                canvas.drawLine(cx - 4 * u, cy + 8 * u, cx + 4 * u, cy + 8 * u,
                        glyphPaint);
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
                canvas.drawPath(brackets, glyphPaint);
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
                canvas.drawPath(sheet, glyphPaint);
                canvas.drawLine(cx + 2 * u, cy - 9 * u, cx + 2 * u, cy - 4 * u,
                        glyphPaint);
                canvas.drawLine(cx + 2 * u, cy - 4 * u, cx + 7 * u, cy - 4 * u,
                        glyphPaint);
            }
            case CALENDAR -> {
                // le calendrier : le corps, les deux pattes, la tête
                canvas.drawRoundRect(cx - 7 * u, cy - 4 * u, cx + 7 * u,
                        cy + 8 * u, 2 * u, 2 * u, glyphPaint);
                canvas.drawLine(cx - 3 * u, cy - 8 * u, cx - 3 * u, cy - 2 * u,
                        glyphPaint);
                canvas.drawLine(cx + 3 * u, cy - 8 * u, cx + 3 * u, cy - 2 * u,
                        glyphPaint);
                canvas.drawLine(cx - 7 * u, cy, cx + 7 * u, cy, glyphPaint);
            }
            case SEARCH -> {
                // la loupe : le cercle et sa poignée
                canvas.drawCircle(cx - 2 * u, cy - 2 * u, 5.5f * u, glyphPaint);
                canvas.drawLine(cx + 2 * u, cy + 2 * u, cx + 7 * u, cy + 7 * u,
                        glyphPaint);
            }
            case GOTO -> {
                // la flèche à la barre : l'aller-y de la définition
                canvas.drawLine(cx - 8 * u, cy, cx + 4 * u, cy, glyphPaint);
                canvas.drawLine(cx, cy - 4 * u, cx + 4 * u, cy, glyphPaint);
                canvas.drawLine(cx + 4 * u, cy, cx, cy + 4 * u, glyphPaint);
                canvas.drawLine(cx + 7 * u, cy - 6 * u, cx + 7 * u, cy + 6 * u,
                        glyphPaint);
            }
            case EYE -> {
                // l'œil : l'ovale et sa pupille
                canvas.drawOval(cx - 8 * u, cy - 4 * u, cx + 8 * u, cy + 4 * u,
                        glyphPaint);
                canvas.drawPoint(cx, cy, glyphPaint);
            }
            case QUOTES -> {
                // les guillemets : deux points et leurs queues
                canvas.drawPoint(cx - 4 * u, cy - 4 * u, glyphPaint);
                canvas.drawPoint(cx + 3 * u, cy - 4 * u, glyphPaint);
                canvas.drawLine(cx - 4 * u, cy - 4 * u, cx - 6 * u, cy + 3 * u,
                        glyphPaint);
                canvas.drawLine(cx + 3 * u, cy - 4 * u, cx + 1 * u, cy + 3 * u,
                        glyphPaint);
            }
            case PENCIL -> {
                // le crayon : la diagonale et sa pointe
                canvas.drawLine(cx - 5 * u, cy + 5 * u, cx + 6 * u, cy - 6 * u,
                        glyphPaint);
                canvas.drawLine(cx - 8 * u, cy + 4 * u, cx - 4 * u, cy + 8 * u,
                        glyphPaint);
                canvas.drawLine(cx - 5 * u, cy + 5 * u, cx - 8 * u, cy + 8 * u,
                        glyphPaint);
            }
            case ALIGN -> {
                // les lignes alignées : le formatage du document
                canvas.drawLine(cx - 8 * u, cy - 5 * u, cx + 8 * u, cy - 5 * u,
                        glyphPaint);
                canvas.drawLine(cx - 8 * u, cy, cx + 4 * u, cy, glyphPaint);
                canvas.drawLine(cx - 8 * u, cy + 5 * u, cx + 8 * u, cy + 5 * u,
                        glyphPaint);
            }
            case TREE -> {
                // l'arbre des symboles : la racine, la tige, ses fils
                canvas.drawLine(cx - 7 * u, cy - 6 * u, cx - 1 * u, cy - 6 * u,
                        glyphPaint);
                canvas.drawLine(cx - 1 * u, cy - 6 * u, cx - 1 * u, cy + 7 * u,
                        glyphPaint);
                canvas.drawLine(cx - 1 * u, cy - 2 * u, cx + 6 * u, cy - 2 * u,
                        glyphPaint);
                canvas.drawLine(cx - 1 * u, cy + 4 * u, cx + 6 * u, cy + 4 * u,
                        glyphPaint);
                canvas.drawPoint(cx - 8 * u, cy - 6 * u, glyphPaint);
                canvas.drawPoint(cx + 7 * u, cy - 2 * u, glyphPaint);
                canvas.drawPoint(cx + 7 * u, cy + 4 * u, glyphPaint);
            }
            case CHAT -> {
                // la bulle de parole : le corps, la queue, deux lignes
                canvas.drawRoundRect(cx - 8 * u, cy - 7 * u, cx + 8 * u,
                        cy + 3 * u, 3 * u, 3 * u, glyphPaint);
                canvas.drawLine(cx - 3 * u, cy + 3 * u, cx - 6 * u, cy + 7 * u,
                        glyphPaint);
                canvas.drawLine(cx - 4 * u, cy - 3 * u, cx + 4 * u, cy - 3 * u,
                        glyphPaint);
                canvas.drawLine(cx - 4 * u, cy + 1 * u, cx + 2 * u, cy + 1 * u,
                        glyphPaint);
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
                canvas.drawPath(page, glyphPaint);
                canvas.drawLine(cx + 2 * u, cy - 9 * u, cx + 2 * u, cy - 4 * u,
                        glyphPaint);
                canvas.drawLine(cx + 2 * u, cy - 4 * u, cx + 7 * u, cy - 4 * u,
                        glyphPaint);
                canvas.drawLine(cx - 3 * u, cy, cx + 4 * u, cy, glyphPaint);
                canvas.drawLine(cx - 3 * u, cy + 4 * u, cx + 4 * u, cy + 4 * u,
                        glyphPaint);
            }
            case CHECK -> {
                // la coche cerclée : le test qui passe
                canvas.drawCircle(cx, cy, 8 * u, glyphPaint);
                canvas.drawLine(cx - 4 * u, cy, cx - 1 * u, cy + 3 * u,
                        glyphPaint);
                canvas.drawLine(cx - 1 * u, cy + 3 * u, cx + 5 * u, cy - 3 * u,
                        glyphPaint);
            }
            default -> drawBoltIcon(canvas, cx, cy);
        }
    }

    /**
     * L'éclair d'action : la tête de l'ampoule — un petit éclair
     * teinté de la couleur de l'ampoule, l'encre secondaire au
     * filet.
     */
    private void drawBoltIcon(Canvas canvas, float cx, float cy) {
        float s = dp(8);
        android.graphics.Path bolt = new android.graphics.Path();
        bolt.moveTo(cx - s * 0.35f, cy - s);
        bolt.lineTo(cx + s * 0.45f, cy - s);
        bolt.lineTo(cx + s * 0.1f, cy - s * 0.2f);
        bolt.lineTo(cx + s * 0.55f, cy - s * 0.2f);
        bolt.lineTo(cx - s * 0.45f, cy + s);
        bolt.lineTo(cx - s * 0.05f, cy + s * 0.15f);
        bolt.lineTo(cx - s * 0.55f, cy + s * 0.15f);
        bolt.close();
        bulbPaint.setStyle(Paint.Style.FILL);
        canvas.drawPath(bolt, bulbPaint);
    }

    /** Le haut d'une rangée au doigt (l'index, -1 hors fenêtre). */
    private int rowAt(float y) {
        float rowsTop = poseY + dp(PAD_DP) + dp(style.headerHeightDp());
        for (int at = topRow; at < topRow + visibleRows
                && at < entries.size(); at++) {
            float top = rowsTop + (at - topRow) * rowHeight();
            if (y >= top && y < top + rowHeight()) {
                return at;
            }
        }
        return -1;
    }

    /**
     * La barre de défilement : la fenêtre montre-t-elle moins que la
     * liste ? Le pouce se pose au prorata des rangées visibles.
     */
    private void drawScrollbar(Canvas canvas, float trackTop, float trackBottom) {
        if (visibleRows >= entries.size()) {
            return;
        }
        float thumb = (trackBottom - trackTop) * visibleRows / entries.size();
        float trackSpan = trackBottom - trackTop - thumb;
        float thumbTop = entries.size() <= 1 ? trackTop
                : trackTop + trackSpan * topRow / (entries.size() - visibleRows);
        canvas.drawRect(poseX + cardW - dp(5), thumbTop,
                poseX + cardW - dp(3), thumbTop + thumb, scrollbarPaint);
    }

    /** Le pied de carte : le compte des actions de la ligne. */
    private void drawFooter(Canvas canvas, float footerTop) {
        float baseline = footerTop + dp(FOOTER_DP) * 0.66f;
        String count = entries.size() == 1 ? "1 action"
                : entries.size() + " actions";
        canvas.drawText(count, poseX + dp(14), baseline, footerPaint);
        String hint = "toucher pour agir";
        canvas.drawText(hint,
                poseX + cardW - dp(14) - footerPaint.measureText(hint),
                baseline, footerPaint);
    }

    private float dp(int value) {
        return value * density;
    }

    private float dp(float value) {
        return value * density;
    }
}
