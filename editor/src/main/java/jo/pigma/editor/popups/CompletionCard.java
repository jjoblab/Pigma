package jo.pigma.editor.popups;

import android.graphics.Canvas;
import android.graphics.Paint;

import java.util.ArrayList;
import java.util.List;

import jo.pigma.editor.EditorView;

/**
 * La carte de complétion à la facture de l'atelier : les candidats du
 * mot courant en COLONNES — l'icône de nature à gauche, le nom au
 * milieu (les caractères qui poursuivent le préfixe tapé passent à
 * l'accent et au gras), le détail à droite (la nature et la ligne de
 * la déclaration, aligné à droite, atténué) — puis un PIED DE CARTE
 * (le compte des candidats, l'indice des touches), une barre de
 * défilement quand la fenêtre ne montre pas tout, et la rangée
 * courante surlignée d'un filet d'accent. La carte se pose au caret
 * (dessous si elle tient, dessus sinon), bornée aux murs.
 *
 * <p>La carte ne prend jamais le focus : le texte tapé poursuit sa
 * vie vers le tampon — la carte se re-filtre à la trame suivante. La
 * flèche verticale déléguée y navigue, la validation déléguée insère
 * (le préfixe se remplace par le candidat entier), le doigt sur une
 * rangée l'insère aussi.</p>
 */
public final class CompletionCard {

    /** Le plafond de rangées montrées quand la place abonde. */
    private static final int MAX_ROWS = 10;

    /** Le plancher de rangées quand la place manque sous le caret. */
    private static final int MIN_ROWS = 3;

    /** La hauteur du pied de carte (dp). */
    private static final int FOOTER_DP = 32;

    /** La respiration intérieure de la carte (dp). */
    private static final int PAD_DP = 4;

    /** La marge aux murs de la vue (dp). */
    private static final int MARGIN_DP = 8;

    /** L'écart entre le caret et la carte (dp). */
    private static final int GAP_DP = 2;

    /** La colonne d'icône : sa largeur (dp). */
    private static final int ICON_DP = 28;

    /** Le côté d'une icône de nature (dp). */
    private static final int ICON_SIDE_DP = 20;

    /** Le filet d'accent de la rangée courante (dp). */
    private static final int ACCENT_BAR_DP = 4;

    /** La nature inconnue : le carré neutre, aucune lettre dedans. */
    public static final int KIND_UNKNOWN = 0;

    /** Un mot du texte : la pastille neutre. */
    public static final int KIND_WORD = 1;

    /** Un mot-clé du langage. */
    public static final int KIND_KEYWORD = 2;

    /** Une méthode (une fonction déclarée). */
    public static final int KIND_FUNCTION = 3;

    /** Un type (une classe). */
    public static final int KIND_CLASS = 4;

    /** Un champ (une variable déclarée). */
    public static final int KIND_FIELD = 5;

    /** Un module (une unité importable). */
    public static final int KIND_MODULE = 6;

    /**
     * Un candidat : son nom, son détail (la colonne de droite — la
     * nature, la ligne de déclaration), sa nature (l'icône), son
     * obsolescence (le BARRÉ du nom), sa documentation (le panneau
     * adjacent du candidat CHOISI — le balisage du serveur y vit)
     * et son insertion propre (le texte à poser, la plage à remplacer
     * — le remplacement du serveur PRIME sur le préfixe tapé).
     *
     * @param name       le nom insérable, jamais vide
     * @param detail     le détail de droite, vide pour aucun
     * @param kind       la nature, une des constantes KIND_ de la carte
     * @param deprecated le candidat est-il obsolète (le barré) ?
     * @param documentation la documentation du candidat, vide pour aucune
     * @param markdownDoc  la documentation est-elle balisée ?
     * @param insertText le texte à poser, vide : le nom se pose
     * @param insertStart le début de la plage remplacée, -1 : le mot tapé
     * @param insertEnd   la fin de la plage remplacée, -1 : le curseur
     */
    public record Entry(String name, String detail, int kind, boolean deprecated,
            String documentation, boolean markdownDoc, String insertText,
            int insertStart, int insertEnd) {

        /** Le candidat normalisé : les textes jamais nuls, la plage saine. */
        public Entry {
            documentation = documentation == null ? "" : documentation;
            insertText = insertText == null ? "" : insertText;
            if (insertStart < 0 || insertEnd < insertStart) {
                insertStart = -1;
                insertEnd = -1;
            }
        }

        /**
         * Le candidat nu : ni obsolescence, ni documentation, ni
         * remplacement propre (le nom se pose, le mot tapé se remplace).
         *
         * @param name   le nom insérable, jamais vide
         * @param detail le détail de droite, vide pour aucun
         * @param kind   la nature, une des constantes KIND_ de la carte
         */
        public Entry(String name, String detail, int kind) {
            this(name, detail, kind, false, "", false, "", -1, -1);
        }
    }

    /** La largeur minimale du panneau de documentation (dp). */
    private static final int DOC_MIN_DP = 150;

    /** La largeur maximale du panneau de documentation (dp). */
    private static final int DOC_MAX_DP = 260;

    /** La marge entre la carte et le panneau de documentation (dp). */
    private static final int DOC_GAP_DP = 6;

    /** Le plafond de rangées du panneau de documentation. */
    private static final int DOC_MAX_ROWS = 16;

    /** Le pinceau du barré des candidats obsolètes. */
    private final Paint strikePaint = new Paint();

    /** Le panneau de documentation du candidat choisi. */
    private final MarkdownPane docPane = new MarkdownPane(1.0f);

    private List<Entry> entries = List.of();

    /** Le préfixe tapé — ses caractères passent à l'accent dans les noms. */
    private String prefix = "";

    /** L'index du candidat courant, -1 si la carte est vide. */
    private int highlighted = -1;

    /** La première rangée de la fenêtre (le défilement la garde sur le courant). */
    private int topRow;

    private float poseX;

    private float poseY;

    private float cardW;

    private float cardH;

    /** Le nombre de rangées montrées (la place le resserre). */
    private int shownRows;

    private float density = 1.0f;

    /** Le style posé par l'hôte : fond translucide, encres, tailles, largeur, forme. */
    private PopupStyle style = PopupStyle.defaults();

    /** L'animation d'ouverture et de sortie (le fondu et la glisse). */
    private final PopupAnimation animation = new PopupAnimation();

    /** La visibilité posée de la trame d'avant (les transitions s'y lisent). */
    private boolean wasShown;

    /** La carte s'est-elle rangée à la trame d'avant (la re-naissance s'y reconnaît). */
    private boolean wasClosing;

    /** L'horodatage du dernier rangement (ms) — la grâce de la re-naissance s'y mesure. */
    private long lastClosedAtMs;

    /** La grâce de la re-naissance (ms) : au-delà, la carte re-naît VRAIMENT (fondu). */
    private static final long REBIRTH_GRACE_MS = 400L;

    /**
     * Le côté posé : 0 indécis (le mot neuf re-choisit), 1 dessous,
     * -1 dessus — STABLE tant que le mot vit (la carte ne saute pas
     * de côté à la frappe).
     */
    private int belowSide;

    private boolean below;

    private EditorView editor;

    private final Paint cardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint accentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint detailPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint footerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint selectionPaint = new Paint();
    private final Paint accentBarPaint = new Paint();
    private final Paint scrollbarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint iconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint iconLetterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    /** Le filet des icônes plates à la référence. */
    private final Paint iconStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    /** Le plein des icônes plates à la référence. */
    private final Paint iconGlyphPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint footerRulePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    /**
     * Une carte neuve, ses pinceaux propres au style par défaut.
     *
     * @param density la densité de l'écran (le dp en pixels)
     */
    public CompletionCard(float density) {
        this.density = density;
        applyStyle();
    }

    /**
     * Pose le style de l'hôte : le fond translucide, les encres, la
     * taille des textes, la largeur des cartes, la forme — les
     * pinceaux s'y reconfigurent entiers.
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
        cardPaint.setColor(style.backgroundArgb());
        borderPaint.setColor(style.borderArgb());
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(1.0f);
        textPaint.setColor(style.textArgb());
        textPaint.setTextSize((style.textSizeSp() - 2.0f) * density);
        accentPaint.setColor(style.accentArgb());
        accentPaint.setTextSize((style.textSizeSp() - 2.0f) * density);
        detailPaint.setColor(style.hintArgb());
        detailPaint.setTextSize(style.secondarySizeSp() * density);
        footerPaint.setColor(style.secondaryArgb());
        footerPaint.setTextSize((style.secondarySizeSp() - 2.0f) * density);
        selectionPaint.setColor((style.accentArgb() & 0x00FFFFFF) | 0x1F000000);
        accentBarPaint.setColor(style.accentArgb());
        scrollbarPaint.setColor((style.borderArgb() & 0x00FFFFFF) | 0x66000000);
        strikePaint.setColor(style.hintArgb());
        docPane.setStyle(style);
        iconPaint.setStyle(Paint.Style.FILL);
        iconLetterPaint.setColor(0xFFFFFFFF);
        iconLetterPaint.setTextSize((style.secondarySizeSp() - 3.0f) * density);
        iconLetterPaint.setFakeBoldText(true);
        // les icônes PLATES de la référence : le filet de 1,5 dp, la
        // lettre prend la COULEUR de sa nature (pas le blanc sur
        // pastille — le glyphe plat se lit sur le fond de la carte)
        iconStrokePaint.setStyle(Paint.Style.STROKE);
        iconStrokePaint.setStrokeWidth(1.5f * density);
        iconGlyphPaint.setStyle(Paint.Style.FILL);
        footerRulePaint.setColor(style.borderArgb());
    }

    /**
     * Branche la carte à son éditeur : l'insertion y parle (le préfixe
     * s'y remplace par le candidat).
     *
     * @param editor l'éditeur complété
     */
    public void bind(EditorView editor) {
        this.editor = editor;
    }

    /**
     * Pose les candidats détaillés du mot courant et le préfixe tapé —
     * le candidat courant SURVIT au changement de liste (la leçon de
     * la référence : le préfixe croît à la frappe, la sélection ne
     * saute pas au premier — l'item sélectionné reste sélectionné tant
     * que son nom vit dans la nouvelle liste, la fenêtre le garde
     * visible). La liste vide range la carte.
     *
     * @param candidates les candidats, déjà filtrés, jamais null
     * @param typedPrefix le préfixe tapé (vide pour aucun surlignage)
     */
    public void set(List<Entry> candidates, String typedPrefix) {
        List<Entry> fresh = List.copyOf(candidates);
        boolean changed = !fresh.equals(entries);
        String kept = null;
        if (changed && highlighted >= 0 && highlighted < entries.size()) {
            kept = entries.get(highlighted).name();
        }
        boolean wasEmpty = entries.isEmpty();
        entries = fresh;
        prefix = typedPrefix == null ? "" : typedPrefix;
        if (changed) {
            // la carte qui RE-NAÎT re-choisit son côté (le mot
            // d'avant a rangé son côté — le clavier a pu bouger
            // entre-temps) ; la carte vivante garde le sien
            if (wasEmpty && !fresh.isEmpty()) {
                belowSide = 0;
            }
            // AUCUNE SURBRILLANCE À LA NAISSANCE (le bug d'usage B6 :
            // le premier candidat se surlignait d'office et l'Entrée
            // le validait — le saut de ligne demandé mourait dans la
            // carte) : la surbrillance ne vient que d'un CHOIX EXPLICITE
            // (flèche déléguée, doigt sur la rangée) ; seule la carte
            // vivante retient son candidat choisi à travers la
            // re-filtration (le « kept » ci-dessus)
            highlighted = -1;
            if (kept != null) {
                for (int at = 0; at < entries.size(); at++) {
                    if (entries.get(at).name().equals(kept)) {
                        highlighted = at;
                        break;
                    }
                }
            }
            keepCurrentVisible();
        }
    }

    /**
     * L'entrée D'AVANT qui porte la documentation du nom demandé (la
     * re-fusion de la trame y rend la documentation résolue — le nom
     * dit l'identité, le contenu demeure) ; null quand le nom vit
     * sans documentation.
     *
     * @param name le nom du candidat documenté
     * @return l'entrée qui porte la documentation, null sinon
     */
    public Entry documentationOf(String name) {
        for (Entry entry : entries) {
            if (entry.name().equals(name) && !entry.documentation().isEmpty()) {
                return entry;
            }
        }
        return null;
    }

    /**
     * Pose la documentation d'un candidat — le panneau adjacent s'y
     * nourrit (le candidat CHOISI s'y documente : la documentation
     * PORTÉE d'abord, la résolution du serveur ensuite). La
     * documentation vit DANS l'entrée : elle y survit au re-filtre.
     *
     * @param name     le nom du candidat documenté
     * @param doc      la documentation, vide pour ranger le panneau
     * @param markdown la documentation est-elle balisée ?
     * @return vrai quand le candidat vit à la carte
     */
    public boolean setDocumentation(String name, String doc, boolean markdown) {
        for (int at = 0; at < entries.size(); at++) {
            if (entries.get(at).name().equals(name)) {
                Entry old = entries.get(at);
                Entry fresh = new Entry(old.name(), old.detail(), old.kind(),
                        old.deprecated(), doc == null ? "" : doc, markdown,
                        old.insertText(), old.insertStart(), old.insertEnd());
                List<Entry> rebuilt = new ArrayList<>(entries);
                rebuilt.set(at, fresh);
                entries = List.copyOf(rebuilt);
                return true;
            }
        }
        return false;
    }

    /**
     * Pose les candidats de noms seuls — les colonnes de détail et
     * d'icône vivent vides (la nature neutre).
     *
     * @param candidates les candidats, déjà filtrés, jamais null
     * @param typedPrefix le préfixe tapé (vide pour aucun surlignage)
     */
    public void setNames(List<String> candidates, String typedPrefix) {
        List<Entry> wrapped = new ArrayList<>();
        for (String name : candidates) {
            wrapped.add(new Entry(name, "", KIND_UNKNOWN));
        }
        set(wrapped, typedPrefix);
    }

    /** La carte vit-elle ? (au moins un candidat) */
    public boolean isShown() {
        return !entries.isEmpty();
    }

    /** Les candidats montrés. */
    public List<String> words() {
        List<String> names = new ArrayList<>();
        for (Entry entry : entries) {
            names.add(entry.name());
        }
        return names;
    }

    /** Les candidats détaillés montrés (le harnais y lit les colonnes). */
    public List<Entry> entries() {
        return entries;
    }

    /** Le préfixe tapé. */
    public String prefix() {
        return prefix;
    }

    /** L'index du candidat courant (-1 : la carte est vide). */
    public int highlighted() {
        return highlighted;
    }

    /** La première rangée de la fenêtre courante. */
    public int topRow() {
        return topRow;
    }

    /** Le nombre de rangées montrées. */
    public int shownRows() {
        return shownRows;
    }

    /**
     * La flèche verticale déléguée navigue : le candidat courant
     * avance ou recule, borné aux murs de la liste ; la fenêtre le
     * suit.
     *
     * @param step négatif vers le haut, positif vers le bas
     * @return vrai quand la carte a pris la flèche
     */
    public boolean onArrow(int step) {
        if (entries.isEmpty()) {
            return false;
        }
        int target = highlighted < 0 ? 0
                : Math.min(entries.size() - 1, Math.max(0, highlighted + step));
        highlighted = target;
        keepCurrentVisible();
        return true;
    }

    /**
     * La validation déléguée insère le candidat courant — son texte
     * PROPRE se pose (le texte d'insertion ou le remplacement du
     * serveur PRIME sur le nom — l'insertion vit chez l'éditeur, une
     * unité d'annulation).
     *
     * @return vrai quand la carte a pris la validation
     */
    public boolean onEnter() {
        if (highlighted < 0 || highlighted >= entries.size()) {
            return false;
        }
        insert(entries.get(highlighted));
        return true;
    }

    /**
     * Le doigt tombé sur la carte lui appartient : la rangée visée
     * insère son candidat ; le pied de carte consomme sans agir ; le
     * reste de la carte se consomme sans agir. Hors de la carte : faux
     * — le geste poursuit sa vie vers l'éditeur.
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
        float rowH = rowHeight();
        float rowsBottom = poseY + dp(PAD_DP) + shownRows * rowH;
        if (y < rowsBottom) {
            int row = (int) ((y - poseY - dp(PAD_DP)) / rowH) + topRow;
            if (row >= topRow && row < topRow + shownRows && row < entries.size()) {
                insert(entries.get(row));
            }
        }
        return true;
    }

    /**
     * Dessine la carte : la pose se recalcule d'abord — le caret la
     * suit (défilement, navigation) — puis la fenêtre de rangées (le
     * candidat courant surligné, son filet d'accent, l'icône de
     * nature, le nom à l'accent sur le préfixe, le BARRÉ de
     * l'obsolescence, le détail à droite), la barre de défilement si
     * la fenêtre cache des rangées, le pied de carte (le compte,
     * l'indice des touches), et le PANNEAU DE DOCUMENTATION du
     * candidat choisi quand il en porte (à DROITE de la carte quand
     * la place y vit, DESSOUS sinon).
     *
     * @param canvas  le canevas de la vue
     * @param metrics la géométrie courante de la vue
     */
    public void draw(Canvas canvas, PopupMetrics metrics) {
        density = metrics.density();
        boolean shown = !entries.isEmpty();
        if (shown != wasShown) {
            // la transition de visibilité joue l'animation : la carte
            // naît au-caret, la carte qui se range s'efface — la
            // RE-FILTRATION de la liste n'y passe pas (elle reste
            // stable, la carte ne clignote pas à la frappe). LA
            // RE-NAISSANCE EN RAFALE NON PLUS (la leçon de Sora : sa
            // carte ouverte reste ouverte pendant toute la frappe du
            // mot — jamais de re-fondu par cran) : une carte qui
            // s'efface (sa liste a transité par vide) et re-vit à la
            // TRAME QUI SUIT ne rejoue PAS son ouverture — sans cela
            // chaque frappe (la liste filtrée se vide puis re-naît des
            // mots du tampon) relançait le fondu de 110 ms : la carte
            // restait quasi invisible pendant qu'on tape, vivante au
            // doigt mais jamais peinte (le popup fantôme)
            long sinceCloseMs = System.currentTimeMillis() - lastClosedAtMs;
            if (shown) {
                if (!wasClosing || sinceCloseMs > REBIRTH_GRACE_MS) {
                    animation.open();
                } else {
                    animation.reset();
                    wasShown = true;
                }
            } else {
                animation.close();
                lastClosedAtMs = System.currentTimeMillis();
            }
            wasClosing = !shown;
            wasShown = shown;
        }
        if (!shown && !animation.isClosing()) {
            cardH = 0.0f;
            shownRows = 0;
            return;
        }
        if (!shown) {
            return;
        }
        int save = canvas.save();
        // le fondu et la glisse de la transition : l'alpha porte la
        // carte entière, la glisse la fait naître au caret
        canvas.translate(0.0f, animation.shiftPx(density));
        int alpha = (int) (animation.alphaFactor() * 255.0f);
        canvas.saveLayerAlpha(poseX - dp(2), poseY - dp(2),
                poseX + cardW + dp(2), poseY + cardH + dp(2),
                Math.max(0, Math.min(255, alpha)));
        place(metrics);
        float radius = dp(style.cornerRadiusDp());
        canvas.drawRoundRect(poseX, poseY, poseX + cardW, poseY + cardH,
                radius, radius, cardPaint);
        canvas.drawRoundRect(poseX, poseY, poseX + cardW, poseY + cardH,
                radius, radius, borderPaint);
        float rowH = rowHeight();
        float pad = dp(PAD_DP);
        float rowsBottom = poseY + pad + shownRows * rowH;
        for (int at = topRow; at < topRow + shownRows && at < entries.size(); at++) {
            float top = poseY + pad + (at - topRow) * rowH;
            if (at == highlighted) {
                canvas.drawRect(poseX + 1.0f, top,
                        poseX + cardW - 1.0f, top + rowH, selectionPaint);
                canvas.drawRect(poseX + 1.0f, top,
                        poseX + 1.0f + dp(ACCENT_BAR_DP), top + rowH, accentBarPaint);
            }
            drawIcon(canvas, entries.get(at).kind(),
                    poseX + dp(10), top + rowH / 2.0f);
            String name = ellipsize(entries.get(at).name(), nameColumnWidth());
            drawLabel(canvas, name,
                    poseX + dp(ICON_DP) + dp(8), top + rowH * 0.68f, at == highlighted);
            if (entries.get(at).deprecated()) {
                // l'OBSOLÈTE : le nom se barre (la référence barre ses
                // candidats déconseillés — le serveur le dit, la carte
                // l'affiche)
                strikePaint.setStrokeWidth(1.0f);
                float nameW = textPaint.measureText(name);
                canvas.drawLine(poseX + dp(ICON_DP) + dp(8),
                        top + rowH * 0.55f,
                        poseX + dp(ICON_DP) + dp(8) + nameW,
                        top + rowH * 0.55f, strikePaint);
            }
            String detail = entries.get(at).detail();
            if (!detail.isEmpty()) {
                String shownDetail = shrink(detail, detailPaint, detailColumnWidth());
                canvas.drawText(shownDetail,
                        poseX + cardW - dp(14) - detailPaint.measureText(shownDetail),
                        top + rowH * 0.68f, detailPaint);
            }
        }
        drawScrollbar(canvas, rowsBottom);
        drawFooter(canvas, rowsBottom);
        drawDocumentation(canvas, metrics);
        canvas.restore();
        canvas.restore();
    }

    /**
     * Le panneau de documentation du candidat CHOISI (la référence
     * documente son candidat retenu — la documentation du serveur
     * y vit, la résolution y répond) : À DROITE de la carte quand
     * la place y vit (la hauteur s'y accorde), DESSOUS sinon (la
     * largeur de la carte y sert, la hauteur s'y borne) ; le candidat
     * sans documentation n'y peint RIEN.
     */
    private void drawDocumentation(Canvas canvas, PopupMetrics metrics) {
        if (highlighted < 0 || highlighted >= entries.size()) {
            return;
        }
        Entry entry = entries.get(highlighted);
        if (entry.documentation().isEmpty()) {
            return;
        }
        boolean rich = entry.markdownDoc()
                || MarkdownDoc.isRich(entry.documentation());
        List<MarkdownDoc.Block> blocks = rich
                ? MarkdownDoc.parse(entry.documentation())
                : plainBlocks(entry.documentation());
        float margin = dp(MARGIN_DP);
        float gap = dp(DOC_GAP_DP);
        float roomRight = metrics.viewWidth() - margin - (poseX + cardW + gap);
        float paneW;
        float paneX;
        float paneY;
        float paneH;
        if (roomRight >= dp(DOC_MIN_DP)) {
            paneW = Math.min(roomRight, dp(DOC_MAX_DP));
            paneX = poseX + cardW + gap;
            paneY = poseY;
            paneH = cardH;
        } else {
            paneW = cardW;
            paneX = poseX;
            paneY = poseY + cardH + gap;
            paneH = Math.min(dp(220),
                    metrics.viewHeight() - margin - paneY);
        }
        if (paneH < dp(40)) {
            return;
        }
        List<MarkdownPane.Row> rows = docPane.layout(blocks,
                paneW - 2 * dp(8), DOC_MAX_ROWS);
        float contentH = docPane.height(rows);
        paneH = Math.min(paneH, contentH + 2 * dp(8));
        canvas.drawRoundRect(paneX, paneY, paneX + paneW, paneY + paneH,
                dp(style.cornerRadiusDp()), dp(style.cornerRadiusDp()), cardPaint);
        canvas.drawRoundRect(paneX, paneY, paneX + paneW, paneY + paneH,
                dp(style.cornerRadiusDp()), dp(style.cornerRadiusDp()), borderPaint);
        int save = canvas.save();
        canvas.clipRect(paneX + dp(2), paneY + dp(2),
                paneX + paneW - dp(2), paneY + paneH - dp(2));
        docPane.draw(canvas, rows, paneX + dp(8), paneY + dp(8),
                paneW - 2 * dp(8));
        canvas.restoreToCount(save);
    }

    /** Les paragraphes d'un texte NU (le panneau y fait la même facture). */
    private List<MarkdownDoc.Block> plainBlocks(String text) {
        List<MarkdownDoc.Block> blocks = new ArrayList<>();
        for (String line : text.split("\n")) {
            if (!line.isBlank()) {
                blocks.add(new MarkdownDoc.Block(MarkdownDoc.PARAGRAPH, 0,
                        List.of(new MarkdownDoc.Span(line, false, false, false,
                                "")), "", ""));
            }
        }
        return blocks;
    }

    /**
     * L'animation vit-elle encore (la vue y invalide la trame
     * suivante — le fondu se joue trame après trame).
     */
    public boolean isAnimating() {
        return animation.isRunning();
    }

    /**
     * Le facteur d'opacité PEINT de la carte (0 : invisible, 1 :
     * pleine) — le harnais y éprouve le fantôme (une carte vivante
     * au doigt mais peinte éteinte : le bug de l'alpha au repos).
     */
    public float paintedAlpha() {
        return animation.alphaFactor();
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
     * @param index l'index du candidat
     * @return l'ordonnée du centre, -1 hors carte
     */
    public float rowCenterY(int index) {
        if (!isShown() || index < topRow || index >= topRow + shownRows) {
            return -1.0f;
        }
        return poseY + dp(PAD_DP)
                + (index - topRow + 0.5f) * rowHeight();
    }

    /** La hauteur d'une rangée : le style la porte, la carte y respire. */
    private float rowHeight() {
        return dp(style.rowHeightDp() - 8);
    }

    /**
     * Pose la carte au caret — DESSOUS d'abord (le côté naturel d'une
     * complétion : la liste descend du mot tapé), AU-DESSUS seulement
     * quand le dessous ne tient pas le plancher de rangées — et la
     * fenêtre de rangées prend TOUTE la place du côté choisi : au
     * moins {@value #MIN_ROWS}, au plus {@value #MAX_ROWS}, jamais
     * plus que les candidats posés. La carte ne rétrécit plus à une
     * rangée au premier caret haut : le côté qui a la place la garde.
     */
    private void place(PopupMetrics metrics) {
        float margin = dp(MARGIN_DP);
        float gap = dp(GAP_DP);
        float rowH = rowHeight();
        float pad = dp(PAD_DP);
        cardW = Math.min(Math.max(measuredWidth(),
                dp(style.minCardWidthDp() - 80)),
                Math.min(dp(style.maxCardWidthDp() - 60),
                        metrics.viewWidth() - 2 * margin));
        int rows = Math.min(MAX_ROWS, entries.size());
        float belowTop = metrics.caretY() + metrics.lineHeight() + gap;
        float belowSpace = metrics.viewHeight() - margin - belowTop;
        float aboveSpace = metrics.caretY() - gap - margin;
        // LE CÔTÉ EST STABLE (la leçon de la référence : la lookup ne
        // saute pas de côté à la frappe — le clavier logiciel réduit
        // la place du dessous trame après trame, une carte qui
        // re-choisit son côté à chaque pose DISPARAÎT aux yeux de
        // l'utilisateur). Le côté choisi au premier mot vit tant que
        // le mot vit : il ne se re-choisit qu'au mot neuf (le préfixe
        // qui raccourcit à rien) — et le côté qui ne tient PLUS
        // (moins de MIN_ROWS) cède à l'autre.
        if (belowSide == 0) {
            below = belowSpace >= aboveSpace
                    || rowsFit(belowSpace, pad, rowH) >= MIN_ROWS;
            belowSide = below ? 1 : -1;
        } else {
            below = belowSide > 0;
            if (below && rowsFit(belowSpace, pad, rowH) < MIN_ROWS
                    && rowsFit(aboveSpace, pad, rowH) > rowsFit(belowSpace,
                            pad, rowH)) {
                below = false;
                belowSide = -1;
            } else if (!below
                    && rowsFit(aboveSpace, pad, rowH) < MIN_ROWS
                    && rowsFit(belowSpace, pad, rowH) > rowsFit(aboveSpace,
                            pad, rowH)) {
                below = true;
                belowSide = 1;
            }
        }
        float space = below ? belowSpace : aboveSpace;
        int fitting = rowsFit(space, pad, rowH);
        rows = Math.max(MIN_ROWS, Math.min(rows, Math.max(fitting, MIN_ROWS)));
        shownRows = Math.min(rows, entries.size());
        cardH = 2 * pad + shownRows * rowH + dp(FOOTER_DP);
        if (below) {
            poseY = Math.min(belowTop,
                    Math.max(margin, metrics.viewHeight() - cardH - margin));
        } else {
            poseY = Math.max(margin, metrics.caretY() - gap - cardH);
        }
        poseX = Math.max(margin,
                Math.min(metrics.caretX(), metrics.viewWidth() - cardW - margin));
        clampWindow();
    }

    /**
     * Le mot neuf : le côté se re-choisit (la carte rangeait son côté
     * au mot d'avant — le clavier peut avoir bougé entre-temps, la
     * place du dessous n'est plus celle du mot précédent).
     */
    public void newWord() {
        belowSide = 0;
    }

    /**
     * Le nombre de rangées que la place donnée tient (le pied de
     * carte et les deux respirations payés d'abord).
     */
    private int rowsFit(float space, float pad, float rowH) {
        if (rowH <= 0.0f) {
            return 0;
        }
        return (int) Math.floor((space - 2 * pad - dp(FOOTER_DP)) / rowH);
    }

    /**
     * La largeur qu'il faut pour le plus large nom + l'icône + le plus
     * large détail, respirations comprises.
     */
    private float measuredWidth() {
        float widestName = 0.0f;
        float widestDetail = 0.0f;
        for (Entry entry : entries) {
            widestName = Math.max(widestName, textPaint.measureText(entry.name()));
            if (!entry.detail().isEmpty()) {
                widestDetail = Math.max(widestDetail,
                        detailPaint.measureText(entry.detail()));
            }
        }
        return dp(ICON_DP) + dp(8) + widestName
                + (widestDetail > 0 ? dp(16) + widestDetail : 0)
                + 2 * dp(14);
    }

    /** La largeur de la colonne du nom. */
    private float nameColumnWidth() {
        return cardW - dp(ICON_DP) - dp(8) - detailReserved() - 2 * dp(14);
    }

    /** La largeur de la colonne du détail. */
    private float detailColumnWidth() {
        return detailReserved() - dp(8);
    }

    /** La largeur réservée au détail (nulle sans aucun détail). */
    private float detailReserved() {
        for (Entry entry : entries) {
            if (!entry.detail().isEmpty()) {
                // un tiers de la carte, plafonné au plus large détail
                return Math.min(dp(140), measuredDetailWidth() + dp(8));
            }
        }
        return 0.0f;
    }

    /** Le plus large détail des candidats. */
    private float measuredDetailWidth() {
        float widest = 0.0f;
        for (Entry entry : entries) {
            if (!entry.detail().isEmpty()) {
                widest = Math.max(widest, detailPaint.measureText(entry.detail()));
            }
        }
        return widest;
    }

    /**
     * La fenêtre garde le candidat courant visible : elle descend ou
     * remonte d'entier, jamais plus loin que les murs de la liste.
     */
    private void keepCurrentVisible() {
        if (highlighted < topRow) {
            topRow = highlighted;
        } else if (highlighted >= topRow + shownRows) {
            topRow = highlighted - shownRows + 1;
        }
        clampWindow();
    }

    /** La fenêtre vit dans les murs de la liste. */
    private void clampWindow() {
        int maxTop = Math.max(0, entries.size() - Math.max(1, shownRows));
        topRow = Math.min(Math.max(0, topRow), maxTop);
    }

    /**
     * Le nom, caractère par caractère : les positions qui poursuivent
     * le préfixe tapé (sous-séquence gloutonne, cas ignorée) passent à
     * l'accent et au gras, les autres gardent l'encre des noms.
     */
    private void drawLabel(Canvas canvas, String label, float x,
            float baseline, boolean boldPlain) {
        boolean[] matched = matchedPositions(label);
        float cursor = x;
        int at = 0;
        while (at < label.length()) {
            boolean isMatch = matched[at];
            int end = at;
            while (end + 1 < label.length() && matched[end + 1] == isMatch) {
                end++;
            }
            String segment = label.substring(at, end + 1);
            Paint paint = isMatch ? accentPaint : textPaint;
            paint.setFakeBoldText(isMatch || boldPlain);
            canvas.drawText(segment, cursor, baseline, paint);
            paint.setFakeBoldText(false);
            cursor += paint.measureText(segment);
            at = end + 1;
        }
    }

    /**
     * L'icône de nature : un carré arrondi teinté par la nature, une
     * lettre au centre (f pour une méthode, C pour un type…) ; la
     * nature neutre garde le carré vide et pâle.
     */
    /**
     * L'icône de nature À LA RÉFÉRENCE (ses icônes de complétion y
     * sont des GLYPHES PLATS au filet coloré — jamais des pastilles
     * pleines à lettre : la classe y est un carré au « C », la
     * méthode un « m » dans un rond, le mot-clé un losange plein
     * discret, le champ un point) : le glyphe porte la couleur de sa
     * nature, le filet l'accompagne.
     */
    private void drawIcon(Canvas canvas, int kind, float left, float cy) {
        float side = dp(ICON_SIDE_DP);
        float cx = left + side / 2.0f;
        iconStrokePaint.setColor(iconColor(kind));
        iconGlyphPaint.setColor(iconColor(kind));
        switch (kind) {
            case KIND_CLASS: {
                // la CLASSE : le carré aux coins doux, le « C » au
                // centre (l'icône de classe de la référence)
                canvas.drawRoundRect(left + dp(1), cy - side * 0.42f,
                        left + side - dp(1), cy + side * 0.42f,
                        dp(2), dp(2), iconStrokePaint);
                drawIconLetter(canvas, "C", cx, cy);
                return;
            }
            case KIND_FUNCTION: {
                // la MÉTHODE : le rond au « m » (le violet de la
                // référence y vit)
                canvas.drawCircle(cx, cy, side * 0.42f, iconStrokePaint);
                drawIconLetter(canvas, "m", cx, cy);
                return;
            }
            case KIND_FIELD: {
                // le CHAMP : le petit rond plein (la pastille discrète)
                canvas.drawCircle(cx, cy, side * 0.22f, iconGlyphPaint);
                return;
            }
            case KIND_KEYWORD: {
                // le MOT-CLÉ : le losange plein discret (le gris de la
                // référence — un mot du langage ne crie pas)
                float s = side * 0.30f;
                android.graphics.Path diamond = new android.graphics.Path();
                diamond.moveTo(cx, cy - s);
                diamond.lineTo(cx + s, cy);
                diamond.lineTo(cx, cy + s);
                diamond.lineTo(cx - s, cy);
                diamond.close();
                canvas.drawPath(diamond, iconGlyphPaint);
                return;
            }
            case KIND_MODULE: {
                // le MODULE : le carré plein discret
                canvas.drawRect(cx - side * 0.22f, cy - side * 0.22f,
                        cx + side * 0.22f, cy + side * 0.22f, iconGlyphPaint);
                return;
            }
            case KIND_WORD: {
                // le MOT DU TEXTE : le rond creux discret
                canvas.drawCircle(cx, cy, side * 0.26f, iconStrokePaint);
                return;
            }
            default: {
                // la NATURE INCONNUE : le rond pointillé discret
                iconStrokePaint.setPathEffect(
                        new android.graphics.DashPathEffect(
                                new float[] { dp(2), dp(2) }, 0.0f));
                canvas.drawCircle(cx, cy, side * 0.26f, iconStrokePaint);
                iconStrokePaint.setPathEffect(null);
            }
        }
    }

    /** La lettre d'icône au centre de son cercle (le glyphe plat). */
    private void drawIconLetter(Canvas canvas, String letter, float cx,
            float cy) {
        // la lettre prend la couleur de sa nature (le glyphe plat de
        // la référence — pas le blanc sur pastille pleine)
        iconLetterPaint.setColor(iconStrokePaint.getColor());
        float letterW = iconLetterPaint.measureText(letter);
        canvas.drawText(letter, cx - letterW / 2.0f, cy + dp(4),
                iconLetterPaint);
    }

    /** La teinte de l'icône selon la nature. */
    private int iconColor(int kind) {
        switch (kind) {
            case KIND_FUNCTION:
                return 0xFF8E44AD;
            case KIND_CLASS:
                return 0xFF2878B5;
            case KIND_FIELD:
                return 0xFF00897B;
            case KIND_KEYWORD:
                return 0xFF5F6368;
            case KIND_MODULE:
                return 0xFFC2185B;
            case KIND_WORD:
                return 0xFF95A5A6;
            default:
                return 0xFFCFD8DC;
        }
    }

    /** La lettre de l'icône selon la nature (vide pour la neutre). */
    /**
     * La barre de défilement : la fenêtre montre-t-elle moins que la
     * liste ? Le pouce se pose au prorata des rangées visibles.
     */
    private void drawScrollbar(Canvas canvas, float rowsBottom) {
        if (shownRows >= entries.size()) {
            return;
        }
        float trackTop = poseY + dp(PAD_DP);
        float trackBottom = rowsBottom - dp(2);
        float thumb = (trackBottom - trackTop) * shownRows / entries.size();
        float trackSpan = trackBottom - trackTop - thumb;
        float thumbTop = entries.size() <= 1 ? trackTop
                : trackTop + trackSpan * topRow / (entries.size() - shownRows);
        canvas.drawRect(poseX + cardW - dp(5), thumbTop,
                poseX + cardW - dp(3), thumbTop + thumb, scrollbarPaint);
    }

    /**
     * Le pied de carte : le compte des candidats (ce que la fenêtre
     * cache le dit aussi), l'indice des touches à droite.
     */
    private void drawFooter(Canvas canvas, float footerTop) {
        float baseline = footerTop + dp(FOOTER_DP) * 0.68f;
        String count = entries.size() == 1 ? "1 candidat"
                : entries.size() + " candidats";
        if (shownRows < entries.size()) {
            count += " · " + (entries.size() - shownRows) + " cachés";
        }
        canvas.drawText(count, poseX + dp(14), baseline, footerPaint);
        String hint = "↑↓ naviguer · Entrée insère";
        canvas.drawText(hint,
                poseX + cardW - dp(14) - footerPaint.measureText(hint),
                baseline, footerPaint);
        // un filet sépare les rangées du pied
        footerRulePaint.setStrokeWidth(1.0f);
        footerRulePaint.setStyle(Paint.Style.STROKE);
        canvas.drawLine(poseX + dp(8), footerTop, poseX + cardW - dp(8),
                footerTop, footerRulePaint);
    }

    /**
     * Les positions du nom qui poursuivent le préfixe (sous-séquence
     * gloutonne, cas ignorée).
     */
    private boolean[] matchedPositions(String label) {
        boolean[] matched = new boolean[label.length()];
        if (prefix.isEmpty()) {
            return matched;
        }
        String target = label.toLowerCase();
        String filter = prefix.toLowerCase();
        int inFilter = 0;
        for (int at = 0; at < target.length() && inFilter < filter.length(); at++) {
            if (target.charAt(at) == filter.charAt(inFilter)) {
                matched[at] = true;
                inFilter++;
            }
        }
        return matched;
    }

    /**
     * Coupe le nom à la largeur de sa colonne — la queue se garde, la
     * tête s'abrège en « … ».
     */
    private String ellipsize(String label, float available) {
        if (textPaint.measureText(label) <= available || label.length() < 2) {
            return label;
        }
        String tail = label.substring(1);
        while (tail.length() > 1 && textPaint.measureText("…" + tail) > available) {
            tail = tail.substring(1);
        }
        return "…" + tail;
    }

    /**
     * Coupe un texte de droite à la largeur : la TÊTE se garde (le
     * détail se lit par sa fin — la ligne de déclaration), la queue
     * s'abrège en « … » devant.
     */
    private String shrink(String text, Paint paint, float maxWidth) {
        if (paint.measureText(text) <= maxWidth || text.length() < 2) {
            return text;
        }
        String head = text.substring(0, text.length() - 1);
        while (head.length() > 1 && paint.measureText(head + "…") > maxWidth) {
            head = head.substring(0, head.length() - 1);
        }
        return head + "…";
    }

    /**
     * Insère un candidat : son texte PROPRE se pose sur SA plage (le
     * remplacement du serveur PRIME — le texte d'insertion sinon, le
     * nom en dernier) ; l'insertion vit chez l'éditeur (une unité
     * d'annulation).
     */
    private void insert(Entry entry) {
        if (editor == null) {
            return;
        }
        String text = !entry.insertText().isEmpty() ? entry.insertText()
                : entry.name();
        editor.insertCompletion(text, entry.insertStart(), entry.insertEnd());
    }

    private float dp(int value) {
        return value * density;
    }
}
