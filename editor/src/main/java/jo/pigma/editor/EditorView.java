package jo.pigma.editor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;

import java.util.List;
import java.util.TreeSet;

import jo.pigma.analysis.DiagnosticsDocument;
import jo.pigma.analysis.JavaDeclarations;
import jo.pigma.analysis.SemanticAnnotation;
import jo.pigma.analysis.SemanticDocument;
import jo.pigma.analysis.SymbolIndex;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;

import jo.pigma.core.TextBuffer;
import jo.pigma.core.TextListener;
import jo.pigma.core.TextTransaction;
import jo.pigma.core.UndoStack;
import jo.pigma.syntax.BackgroundFill;
import jo.pigma.syntax.LanguageSyntax;
import jo.pigma.syntax.Languages;
import jo.pigma.syntax.StructureDocument;
import jo.pigma.syntax.SyntaxDocument;
import jo.pigma.syntax.SyntaxTheme;

/**
 * Vue d'édition minimale : le texte affiché est le tampon du cœur,
 * coloré par le moteur de syntaxe, chaque retour de l'IME est une
 * transaction (une unité d'annulation), le curseur se place au toucher
 * et le doigt fait défiler verticalement.
 *
 * <p>Le rendu reste volontairement rudimentaire : police à chasse
 * fixe, une ligne visuelle par ligne du tampon, curseur rectangulaire,
 * soulignement de la zone de composition. Il ne dessine que les
 * lignes visibles — c'est la fenêtre paresseuse du moteur qui s'y
 * paie, le remplissage du reste court sur le fil de fond annulable :
 * les tranches s'y prennent en clichés et ne s'appliquent que si la
 * révision observée du moteur n'a pas bougé — une frappe entre deux
 * tranches annule la tranche, jamais le document. Changer de thème ne
 * retokenise rien : la résolution des couleurs se fait à la cuisson
 * du plan. Le dessin d'une ligne s'arrête à {@value #MAX_DRAWN_COLUMNS}
 * colonnes, une ligne monstre (hors plafond moteur) s'affiche en
 * texte brut. Les plans de rendu passent par un cache (validité par
 * identité d'entrée, partage par contenu pour les lignes identiques)
 * et la durée de la dernière trame s'y lit — le budget par image du
 * panneau. La sélection et la gouttière viendront avec la vue
 * complète de la bibliothèque.</p>
 */
public final class EditorView extends View {

    /** Plan de rendu d'une ligne : textes tranchés, couleurs résolues (fusion L3 comprise), abscisses, gras. */
    record LinePlan(String[] texts, int[] colors, float[] xs, boolean[] bolds) {
    }

    private static final String INITIAL_TEXT = "";

    private static final int CARET_COLOR = 0xFF2962FF;
    private static final int COMPOSE_COLOR = 0xFF7A8199;

    /** Colonnes au-delà desquelles le dessin d'une ligne s'arrête. */
    private static final int MAX_DRAWN_COLUMNS = 10_000;

    /** L'agrandissement de la loupe (la colonne exacte se lit au doigt). */
    private static final float LOUPE_SCALE = 2.0f;

    /** Colonnes par niveau d'indentation (les guides de L2). */
    private static final int GUIDE_COLUMNS = 4;

    /** Plans partagés par contenu, au-delà le plus ancien s'évince. */
    private static final int SHARED_PLANS = 256;

    /**
     * Le plafond de correspondances collectées par une recherche : le
     * document entier reste parcouru (le compte s'arrête seulement) —
     * un document piège ne remplit pas la mémoire.
     */
    private static final int SEARCH_MATCH_LIMIT = 10_000;

    private final TextBuffer buffer;
    private final UndoStack undoStack;
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint caretPaint = new Paint();
    private final Paint composePaint = new Paint();
    private final Paint.FontMetrics fontMetrics;
    private final float charWidth;
    private final float lineHeight;
    private final float caretWidth;

    private SyntaxDocument syntax;
    private LanguageSyntax language;
    private SyntaxTheme theme = EditorThemes.LIGHT;
    private int themeBackground = EditorThemes.LIGHT_BACKGROUND;

    private int caretOffset;
    private int composingStart = -1;
    private int composingEnd = -1;

    /** L'ancre de sélection (-1 : pas de sélection, le curseur seul). */
    private int selectionAnchor = -1;

    /** La requête de recherche courante (null : pas de recherche). */
    private String searchQuery;

    /** Les débuts des correspondances, triés croissants. */
    private int[] searchMatches = new int[0];

    /** L'index de la correspondance courante (-1 : aucune). */
    private int searchCurrent = -1;

    /** Le texte de la popup d'info ancrée (null : masquée). */
    private String infoPopupText;

    /** La ligne d'ancrage de la popup d'info. */
    private int infoPopupLine;

    /** La colonne d'ancrage de la popup d'info. */
    private int infoPopupColumn;

    /** L'horodatage de l'appui courant (l'appui long sélectionne le mot). */
    private long touchDownNanos;

    /** L'horodatage du dernier tap (le double-tap sélectionne le mot). */
    private long lastTapNanos;

    private float lastTapX;
    private float lastTapY;

    private float scrollY;
    private float touchStartX;
    private float touchStartY;
    private boolean dragging;

    /**
     * La poignée traînée (0 : aucune, 1 : la borne de début, 2 : la
     * borne de fin — l'appui long qui glisse s'y raccorde : la borne
     * la plus proche du doigt).
     */
    private int selectionDragBound;

    /** La borne fixe du traînement (l'autre bout de la sélection). */
    private int dragFixedBound = -1;

    /**
     * Le traîné-déposé : l'appui dans le texte d'une sélection vivante
     * l'arme (le glissé qui suit déplacera le texte au doigt).
     */
    private boolean textDragArmed;

    /** Le traîné-déposé engagé : le curseur de dépôt suit le doigt. */
    private boolean textDragging;

    /** Le décalage de dépôt courant du traîné-déposé (-1 : hors geste). */
    private int dropOffset = -1;

    /** Le rendu de la ligne sous la loupe (1×, hors trame) — blitté par trame. */
    private android.graphics.Bitmap loupeLine;

    /** La ligne du rendu de loupe en cache (-1 : aucun). */
    private int loupeLineIndex = -1;

    /** La révision du rendu de loupe en cache (-1 : aucune). */
    private int loupeLineRevision = -1;

    /** Le fil de fond du remplissage (clichés révision-vérifiés). */
    private BackgroundFill backgroundFill;

    /** L2 : la structure d'appariement par-dessus les jetons du moteur. */
    private StructureDocument structure;

    /** L3 : l'annotateur sémantique, fusionné à la cuisson des plans. */
    private SemanticDocument semantic;

    /** L'index de symboles (complétion de base), rafraîchi au ralenti. */
    private SymbolIndex symbolIndex;

    /** Les diagnostics d'appariement, rafraîchis au ralenti. */
    private DiagnosticsDocument diagnostics;

    /** Révision de l'index et des diagnostics courants. */
    private int analysisRevision = -1;

    /** Horodatage du dernier rafraîchissement d'analyse (le ralenti). */
    private long lastAnalysisNanos;

    /** Les bâtisseurs de la passe d'analyse en cours (par tranches postées). */
    private SymbolIndex.Builder indexBuilder;

    private DiagnosticsDocument.Builder diagnosticsBuilder;

    /** La ligne où la passe en cours reprend. */
    private int analysisPendingLine;

    /** Révision qui a lancé la passe en cours (une édition l'abandonne). */
    private int analysisBuildRevision;

    /** Horodatage du dernier DÉMARRAGE de passe (le ralenti tient même à froid). */
    private long lastAnalysisStartNanos = -Long.MAX_VALUE / 2;

    /** La passe est suspendue : les documents ne sont pas ceux du texte courant. */
    private boolean analysisSuspended;

    /** La taille d'une tranche d'analyse (lignes avalées par post). */
    private static final int ANALYSIS_SLICE_LINES = 256;

    /** Les candidats du bandeau de complétion (le mot courant). */
    private List<String> suggestions = List.of();

    /** La borne de début du mot courant (l'insertion remplace le préfixe). */
    private int suggestionWordStart = -1;

    /** Les abscisses des candidats dessinés (le toucher s'y teste). */
    private float[] suggestionXs = new float[0];

    /** La paire d'appariement du curseur (L2), null si rien n'adjace. */
    private StructureDocument.BracketPair bracketPair;

    /** La portée du curseur (L2), null au niveau racine. */
    private StructureDocument.BlockRange scopeRange;

    private int pairCaret = -1;

    private int pairRevision = -1;

    /** En-têtes de plis repliés (indices de lignes). */
    private final TreeSet<Integer> folded = new TreeSet<>();

    /** Lignes cachées par les plis, null quand rien n'est replié. */
    private boolean[] hiddenByFold;

    /** Lignes visibles strictement avant chaque ligne (les plis compactent). */
    private int[] visibleBefore;

    /** Lignes cachées en cours (le panneau le lit). */
    private int hiddenLineCount;

    /** Nombre de lignes au dernier avis du tampon (décale les plis à l'édition). */
    private int trackedLineCount;

    /** Le surlignage des deux bouts d'une paire (le trait du signe). */
    private final Paint bracketPaint = new Paint();

    /** Les guides d'indentation de L2 (un par niveau de profondeur). */
    private final Paint guidePaint = new Paint();

    /** La bande de fond de la portée du curseur. */
    private final Paint scopePaint = new Paint();

    private final Paint selectionPaint = new Paint();

    /** Le corps d'une poignée de sélection (le cercle sous la borne). */
    private final Paint handlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    /** Le cœur blanc d'une poignée (le point de visibilité). */
    private final Paint handleCorePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    /** Le cadre arrondi de la loupe de précision. */
    private final Paint loupeBorder = new Paint(Paint.ANTI_ALIAS_FLAG);

    /** L'ordonnée du doigt pendant le traînement d'une poignée (la loupe la fuit). */
    private float dragFingerY;

    /** La teinte des correspondances de recherche. */
    private final Paint searchPaint = new Paint();

    /** La teinte de la correspondance courante (plus dense). */
    private final Paint searchCurrentPaint = new Paint();

    /** Le texte de la popup d'info ancrée. */
    private final Paint infoPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    /** Le fond de la popup d'info. */
    private final Paint infoBackground = new Paint(Paint.ANTI_ALIAS_FLAG);

    /** Le bord de la popup d'info. */
    private final Paint infoBorder = new Paint(Paint.ANTI_ALIAS_FLAG);

    /** Le souligné des diagnostics d'appariement. */
    private final Paint diagnosticPaint = new Paint();

    /** Le texte des candidats de complétion (bandeau). */
    private final Paint suggestionPaint = new Paint();

    /** Le fond du bandeau de complétion. */
    private final Paint suggestionBackground = new Paint();

    private final RenderCache<LinePlan> renderCache = new RenderCache<>(SHARED_PLANS);

    /** Durée de la dernière trame (budget par image, diagnostic). */
    private long lastFrameNanos;

    /** Part moteur de la dernière trame : le temps passé dans lineTokens. */
    private long lastFrameEngineNanos;

    /** Pire trame depuis le dernier scénario (l'odomètre du budget). */
    private long worstFrameNanos;

    /** Part moteur de la pire trame (l'odomètre nomme le coupable). */
    private long worstFrameEngineNanos;

    /**
     * Mémoire rendue pendant la pire trame (octets, positif si un
     * ramasse-miettes y a couru) : la pause du collecteur se lit dans
     * la trame qu'elle a allongée.
     */
    private long worstFrameMemoryDelta;

    private long frameCount;

    private final TextListener modelListener = new TextListener() {
        @Override
        public void textChanged(TextBuffer changed, int start, int removedLength,
                                int insertedLength, int revision) {
            shiftSelection(start, removedLength, insertedLength);
            clampCaret();
            shiftFolds(start);
            ensureCaretVisible();
            // une édition déplace tout ce qui suit : les positions des
            // correspondances seraient des mensonges — la recherche se
            // range (la feuille la relance), la popup d'info aussi
            if (searchQuery != null) {
                searchMatches = new int[0];
                searchCurrent = -1;
            }
            dismissInfoPopup();
            // le remplissage n'est PAS relancé ici : la salve du fond ne
            // doit pas chevaucher le dessin qui suit l'édition —
            // l'odomètre l'a vu (moteur 9,2 ms à l'ouverture du gros
            // fichier) — ensureFill, en fin de trame, s'en chargera
            invalidate();
        }
    };

    public EditorView(Context context) {
        super(context);
        buffer = TextBuffer.create(INITIAL_TEXT);
        undoStack = new UndoStack(buffer);
        syntax = SyntaxDocument.open(buffer, Languages.JAVA);
        structure = StructureDocument.open(buffer, syntax);
        semantic = SemanticDocument.open(buffer, syntax, structure,
                new JavaDeclarations());
        backgroundFill = BackgroundFill.start(syntax);
        textPaint.setTypeface(Typeface.MONOSPACE);
        textPaint.setTextSize(14.0f * getResources().getDisplayMetrics().scaledDensity);
        fontMetrics = textPaint.getFontMetrics();
        charWidth = textPaint.measureText("M");
        lineHeight = (float) Math.ceil(fontMetrics.bottom - fontMetrics.top) + dp(2);
        caretWidth = dp(2);
        caretPaint.setColor(CARET_COLOR);
        composePaint.setColor(COMPOSE_COLOR);
        bracketPaint.setStyle(Paint.Style.STROKE);
        bracketPaint.setStrokeWidth(dp(1) * 1.5f);
        bracketPaint.setColor(CARET_COLOR);
        guidePaint.setStrokeWidth(dp(1));
        guidePaint.setColor(0x33808080);
        scopePaint.setStyle(Paint.Style.FILL);
        scopePaint.setColor(0x0D2962FF);
        selectionPaint.setStyle(Paint.Style.FILL);
        selectionPaint.setColor(0x332979FF);
        handlePaint.setStyle(Paint.Style.FILL);
        handlePaint.setColor(0xE62979FF);
        handleCorePaint.setStyle(Paint.Style.FILL);
        handleCorePaint.setColor(0xFFFFFFFF);
        loupeBorder.setStyle(Paint.Style.STROKE);
        loupeBorder.setStrokeWidth(dp(1));
        loupeBorder.setColor(0xFF1B6FE0);
        searchPaint.setStyle(Paint.Style.FILL);
        searchPaint.setColor(0x38FFC107);
        searchCurrentPaint.setStyle(Paint.Style.FILL);
        searchCurrentPaint.setColor(0x80FFC107);
        infoPaint.setColor(0xFF1B1B1F);
        infoPaint.setTextSize(dp(12));
        infoBackground.setStyle(Paint.Style.FILL);
        infoBackground.setColor(0xF2FFFFFF);
        infoBorder.setStyle(Paint.Style.STROKE);
        infoBorder.setStrokeWidth(dp(1));
        infoBorder.setColor(0xFF1B6FE0);
        diagnosticPaint.setStyle(Paint.Style.STROKE);
        diagnosticPaint.setStrokeWidth(dp(2));
        diagnosticPaint.setColor(0xFFFF3B30);
        suggestionPaint.setColor(0xFF1B1B1F);
        suggestionPaint.setTextSize(dp(13));
        suggestionBackground.setStyle(Paint.Style.FILL);
        suggestionBackground.setColor(0x11000000);
        trackedLineCount = buffer.getLineCount();
        buffer.addListener(modelListener);
        int padding = dp(12);
        setPadding(padding, dp(16), padding, dp(16));
        setFocusable(true);
        setFocusableInTouchMode(true);
    }

    /**
     * Tampon édité par cette vue : un hôte peut y brancher ses propres
     * écouteurs (ligne d'état, diagnostics).
     *
     * @return le tampon, jamais {@code null}
     */
    public TextBuffer getBuffer() {
        return buffer;
    }

    /**
     * Document de coloration de la vue : le panneau de diagnostic y
     * lit ses mesures.
     *
     * @return le moteur de coloration, jamais {@code null}
     */
    public SyntaxDocument getSyntaxDocument() {
        return syntax;
    }

    /**
     * Structure d'appariement de la vue (L2) : le panneau y lit la
     * profondeur et les rompus.
     *
     * @return l'analyseur tolérant, jamais {@code null}
     */
    public StructureDocument getStructureDocument() {
        return structure;
    }

    /**
     * Ligne du curseur (le panneau structure y lit sa profondeur).
     *
     * @return l'indice de la ligne du curseur
     */
    public int caretLine() {
        return buffer.lineOfOffset(Math.min(caretOffset, buffer.length()));
    }

    /**
     * Pose le curseur à un offset (borné au texte) : l'hôte place le
     * curseur par programme, la paire d'appariement s'y recalcule au
     * dessin suivant. La sélection se range — poser le curseur, c'est
     * quitter la sélection.
     *
     * @param offset la position visée
     */
    public void caretTo(int offset) {
        caretOffset = Math.max(0, Math.min(offset, buffer.length()));
        selectionAnchor = -1;
        ensureCaretVisible();
        invalidate();
    }

    /**
     * La paire d'appariement du curseur (L2) : la dernière calculée au
     * dessin — le harnais s'en sert pour vérifier la marche.
     *
     * @return la paire, ou {@code null} si aucun signe adjacent
     */
    public StructureDocument.BracketPair bracketPair() {
        return bracketPair;
    }

    /**
     * La portée du curseur (L2) : la dernière calculée au dessin — le
     * harnais la vérifie depuis le tampon.
     *
     * @return la plage du bloc englobant, ou {@code null} au niveau racine
     */
    public StructureDocument.BlockRange scopeRange() {
        return scopeRange;
    }

    /**
     * L'annotateur sémantique de la vue (le harnais vérifie la fusion).
     *
     * @return le document sémantique, jamais {@code null}
     */
    public SemanticDocument getSemanticDocument() {
        return semantic;
    }

    /**
     * Annotations sémantiques (L3) de la fenêtre visible : une passe
     * bornée à la fenêtre (le panneau la lit, jamais le document
     * entier).
     *
     * @return le nombre d'annotations des lignes visibles
     */
    public int semanticVisibleCount() {
        if (semantic == null) {
            return 0;
        }
        int count = buffer.getLineCount();
        int total = 0;
        for (int line = firstVisibleLine(); line <= lastVisibleLine()
                && line < count; line++) {
            if (isHidden(line)) {
                continue;
            }
            total += semantic.annotationsAt(line).size();
        }
        return total;
    }

    /**
     * Durée de la dernière trame dessinée — le budget par image du
     * panneau de diagnostic.
     *
     * @return la durée en nanosecondes, 0 avant le premier dessin
     */
    public long lastFrameNanos() {
        return lastFrameNanos;
    }

    /**
     * Part moteur de la dernière trame : le temps cumulé passé à demander
     * les jetons (verrou compris — un fil de fond goumand s'y verrait).
     * Trame haute avec moteur bas : le coût est au rendu, pas au moteur.
     *
     * @return la durée moteur en nanosecondes, 0 avant le premier dessin
     */
    public long lastFrameEngineNanos() {
        return lastFrameEngineNanos;
    }

    /**
     * Pire trame depuis l'ouverture du scénario courant : les pointes que
     * le tick du panneau peut manquer.
     *
     * @return la durée en nanosecondes, 0 avant le premier dessin
     */
    public long worstFrameNanos() {
        return worstFrameNanos;
    }

    /**
     * Part moteur de la pire trame : un moteur haut nomme le moteur
     * coupable, un moteur bas renvoie vers le rendu ou le collecteur.
     *
     * @return la durée moteur en nanosecondes, 0 si la pire n'a pas couru
     */
    public long worstFrameEngineNanos() {
        return worstFrameEngineNanos;
    }

    /**
     * Mémoire rendue pendant la pire trame (octets) : un bond positif
     * signe le passage du ramasse-miettes dans la trame allongée, un
     * creux négatif n'est que l'allocation du dessin.
     *
     * @return le delta en octets, 0 si la pire n'a pas couru
     */
    public long worstFrameMemoryDelta() {
        return worstFrameMemoryDelta;
    }

    /**
     * Durée de la dernière passe du fil de fond (ouverture à remplissage
     * complet) : la montre du fond.
     *
     * @return la durée en nanosecondes, 0 si aucune passe n'a fini
     */
    public long backgroundFillNanos() {
        return backgroundFill.fillNanos();
    }

    /**
     * Tranches lexées par la dernière passe du fil de fond.
     *
     * @return le nombre de tranches, 0 si aucune passe
     */
    public long backgroundFillSlices() {
        return backgroundFill.sliceCount();
    }

    /**
     * Remet l'odomètre à zéro : la pire trame repart au changement de
     * scénario (la vue le fait d'elle-même à chaque langage) — son
     * attribution (moteur, mémoire) repart avec elle.
     */
    public void resetFrameStats() {
        worstFrameNanos = 0L;
        worstFrameEngineNanos = 0L;
        worstFrameMemoryDelta = 0L;
    }

    /**
     * Nombre de trames dessinées depuis la construction (diagnostic).
     *
     * @return le nombre de dessins
     */
    public long frameCount() {
        return frameCount;
    }

    /**
     * Cuissons de plans de rendu depuis la dernière invalidation :
     * après quelques trames, le cache travaille et ce compte stagne.
     *
     * @return le nombre de cuissons
     */
    public long renderCacheBakes() {
        return renderCache.bakes();
    }

    /**
     * Reprises par contenu depuis la dernière invalidation : les lignes
     * identiques partagent leur rendu.
     *
     * @return le nombre de reprises
     */
    public long renderCacheKeyHits() {
        return renderCache.keyHits();
    }

    /**
     * Passages par le chemin rapide (identité d'entrée, aucune lecture
     * de texte par trame).
     *
     * @return le nombre de passages
     */
    public long renderCacheSlotHits() {
        return renderCache.slotHits();
    }

    /**
     * Change de langage : le texte est remplacé (une transaction, donc
     * annulable) et un nouveau document de coloration s'ouvre — le
     * haut du document s'affiche, le remplissage repart.
     *
     * @param language descripteur du langage, jamais {@code null}
     * @param text     texte de démonstration, jamais {@code null}
     */
    public void setLanguage(LanguageSyntax language, String text) {
        // le même texte au même langage : le document ouvert EST déjà
        // exact — le re-ouvrir ne doit ni relancer le fond ni recuire
        // les plans. Le relevé 15 l'impose : sept réouvertures du même
        // texte hors normes y ont relancé le lexage à chaque fois
        // (436-1101 ms de fond, ~50 Mo de jetons à recycler par passe)
        // — la réouverture ne porte plus que l'état de la vue
        if (syntax != null && language == this.language
                && text.length() == buffer.length()
                && text.contentEquals(buffer)) {
            reopenSameDocument();
            return;
        }
        this.language = language;
        // le fil de fond s'arrête avant le détachement : sa tranche en
        // cours finit sur l'ancien document, le nouveau partira sien
        backgroundFill.stop();
        // l'analyse se suspend : l'écouteur du panneau est notifié
        // APRÈS le remplacement du texte mais AVANT l'ouverture des
        // nouveaux documents — démarrer une passe dans cette fenêtre
        // lexe les ANCIENS jetons dans le NOUVEAU texte (le harnais
        // l'a épinglé au changement du fichier hors normes)
        analysisSuspended = true;
        syntax.detach();
        structure.detach();
        // l'annotateur s'éteint AVANT le remplacement : l'écouteur du
        // panneau peut lire pendant la transaction (l'ancien annotateur
        // découperait ses anciens jetons dans le nouveau texte — le
        // harnais l'a épinglé). L'analyse suit : l'index, les
        // diagnostics ET LA PASSE EN COURS de l'ancien document n'y ont
        // plus de cours (une tranche postée qui revient trouve la
        // passe close et s'arrête seule)
        semantic = null;
        symbolIndex = null;
        diagnostics = null;
        indexBuilder = null;
        diagnosticsBuilder = null;
        analysisRevision = -1;
        lastAnalysisStartNanos = -Long.MAX_VALUE / 2;
        suggestions = List.of();
        suggestionWordStart = -1;
        // autre document : la sélection n'y désigne plus rien, la
        // recherche et la popup d'info non plus
        selectionAnchor = -1;
        searchQuery = null;
        searchMatches = new int[0];
        searchCurrent = -1;
        infoPopupText = null;
        try (TextTransaction transaction = undoStack.newTransaction()) {
            transaction.replace(0, buffer.length(), text);
        }
        // le chargement n'est pas une édition à défaire : l'histoire
        // repart vide (les unités de l'ancien texte y désigneraient
        // des décalages sans sens dans le nouveau)
        undoStack.clear();
        syntax = SyntaxDocument.open(buffer, language);
        structure = StructureDocument.open(buffer, syntax);
        semantic = SemanticDocument.open(buffer, syntax, structure,
                new JavaDeclarations());
        backgroundFill = BackgroundFill.start(syntax);
        // les nouveaux documents sont ouverts : la fenêtre de
        // suspension se referme, l'analyse repartira sur la demande
        analysisSuspended = false;
        // autre document : les plis de l'ancien n'y ont plus de sens
        folded.clear();
        rebuildFoldLayout();
        resetFrameStats();
        // autre langage : les plans portent des couleurs résolues, tout
        // recuit (sans retokeniser — le nouveau document lexe à la demande)
        renderCache.invalidate();
        scrollY = 0.0f;
        clampCaret();
        ensureCaretVisible();
        // la salve du fond attend la fin de la trame d'ouverture (le
        // ensureFill de onDraw la relance) : le premier dessin du gros
        // fichier ne la paie plus
        invalidate();
    }

    /**
     * La réouverture du même document : le lexage, l'analyse et les
     * plans cuits restent (le texte n'a pas bougé) — la sélection, la
     * recherche, la popup, les plis, l'historique à défaire et le
     * défilement repartent, comme à toute ouverture. Le fond, s'il
     * court encore, continue : il remplit déjà ce document.
     */
    private void reopenSameDocument() {
        selectionAnchor = -1;
        selectionDragBound = 0;
        searchQuery = null;
        searchMatches = new int[0];
        searchCurrent = -1;
        infoPopupText = null;
        undoStack.clear();
        folded.clear();
        rebuildFoldLayout();
        resetFrameStats();
        scrollY = 0.0f;
        clampCaret();
        ensureCaretVisible();
        invalidate();
    }

    /**
     * Change de thème à chaud : aucune retokenisation, les jetons
     * portent des portées, la couleur se résout au dessin.
     *
     * @param newTheme     thème résolu, jamais {@code null}
     * @param newBackground couleur ARGB du fond
     */
    public void applyTheme(SyntaxTheme newTheme, int newBackground) {
        theme = newTheme;
        themeBackground = newBackground;
        // les plans portent des couleurs résolues : re-rendu, jamais
        // retokenisation (la première coloration du panneau ne bouge pas)
        renderCache.invalidate();
        // le rendu de la loupe porte les couleurs du thème aussi
        loupeLine = null;
        loupeLineIndex = -1;
        invalidate();
    }

    /**
     * Pile d'annulation de la vue : les boutons de l'hôte l'appellent.
     *
     * @return la pile, jamais {@code null}
     */
    public UndoStack getUndoStack() {
        return undoStack;
    }

    /**
     * Offset du curseur, en unités de code UTF-16.
     *
     * @return la position du curseur dans le texte
     */
    public int getCaretOffset() {
        return caretOffset;
    }

    /**
     * La largeur d'un glyphe de la police à espacement fixe (l'hôte
     * calcule ses propres repères de toucher avec).
     *
     * @return la largeur de « M » en pixels
     */
    public float charWidth() {
        return charWidth;
    }

    /**
     * Une sélection est-elle en cours (l'ancre et le curseur
     * désignent des positions différentes) ?
     *
     * @return vrai si du texte est sélectionné
     */
    public boolean hasSelection() {
        return selectionAnchor >= 0 && selectionAnchor != caretOffset;
    }

    /**
     * Le début de la sélection, en unités de code UTF-16.
     *
     * @return la plus petite des deux bornes, ou la position du
     *         curseur sans sélection
     */
    public int selectionStart() {
        return selectionAnchor < 0
                ? caretOffset : Math.min(selectionAnchor, caretOffset);
    }

    /**
     * La fin de la sélection, en unités de code UTF-16.
     *
     * @return la plus grande des deux bornes, ou la position du
     *         curseur sans sélection
     */
    public int selectionEnd() {
        return selectionAnchor < 0
                ? caretOffset : Math.max(selectionAnchor, caretOffset);
    }

    /**
     * Le texte sélectionné (le copier de l'hôte, le presse-papiers).
     *
     * @return le texte entre l'ancre et le curseur, vide sans sélection
     */
    public CharSequence selectedText() {
        if (!hasSelection()) {
            return "";
        }
        return buffer.getText(selectionStart(), selectionEnd());
    }

    /**
     * Pose la sélection : l'ancre et le curseur (les bornes se
     * bornent, l'ordre importe peu — le curseur porte la fin visible).
     * Un début égal à la fin réduit la sélection au curseur.
     *
     * @param start une borne (l'ancre si elle précède)
     * @param end   l'autre borne
     */
    public void setSelection(int start, int end) {
        int from = clampToRange(start, 0, buffer.length());
        int to = clampToRange(end, 0, buffer.length());
        // l'ancre se pose APRÈS le curseur : poser le curseur range la
        // sélection, l'ancre la rouvre
        selectionAnchor = -1;
        caretTo(to);
        selectionAnchor = from == to ? -1 : from;
        invalidate();
    }

    /**
     * Sélectionne le document entier (le bouton de l'hôte, le menu du
     * clavier) : l'ancre au début, le curseur à la fin.
     */
    public void selectAll() {
        setSelection(0, buffer.length());
    }

    /**
     * Cherche la requête dans tout le document : les correspondances
     * se collectent (au plafond de {@value #SEARCH_MATCH_LIMIT}, le
     * compte le dit), la première après le curseur devient courante
     * et s'amène dans la fenêtre. Une requête vide range la recherche.
     *
     * @param query le texte cherché, exact (la casse compte)
     */
    public void search(String query) {
        dismissInfoPopup();
        if (query == null || query.isEmpty()) {
            clearSearch();
            return;
        }
        searchQuery = query;
        IntArray found = new IntArray();
        CharSequence text = buffer.getText(0, buffer.length());
        int at = 0;
        while (found.size() < SEARCH_MATCH_LIMIT) {
            int hit = indexOf(text, query, at);
            if (hit < 0) {
                break;
            }
            found.add(hit);
            at = hit + query.length();
        }
        searchMatches = found.toArray();
        searchCurrent = -1;
        if (searchMatches.length > 0) {
            searchCurrent = 0;
            for (int m = 0; m < searchMatches.length; m++) {
                if (searchMatches[m] >= caretOffset) {
                    searchCurrent = m;
                    break;
                }
            }
            gotoSearchMatch(searchCurrent);
        }
        invalidate();
    }

    /**
     * La correspondance suivante (la recherche boucle : après la
     * dernière vient la première).
     */
    public void searchNext() {
        if (searchMatches.length > 0) {
            gotoSearchMatch((searchCurrent + 1) % searchMatches.length);
        }
    }

    /** La correspondance précédente (la recherche boucle aussi en arrière). */
    public void searchPrevious() {
        if (searchMatches.length > 0) {
            gotoSearchMatch((searchCurrent - 1 + searchMatches.length)
                    % searchMatches.length);
        }
    }

    /**
     * Le nombre de correspondances de la recherche courante (le
     * plafond compris : le compte s'arrête à
     * {@value #SEARCH_MATCH_LIMIT}).
     *
     * @return le nombre de correspondances trouvées, 0 sans recherche
     */
    public int searchMatchCount() {
        return searchMatches.length;
    }

    /**
     * L'index de la correspondance courante (0-based), -1 sans
     * correspondance — la feuille du bas affiche « courant+1/total ».
     *
     * @return l'index de la correspondance courante
     */
    public int searchCurrentIndex() {
        return searchCurrent;
    }

    /** Range la recherche (les teintes disparaissent, la requête s'oublie). */
    public void clearSearch() {
        searchQuery = null;
        searchMatches = new int[0];
        searchCurrent = -1;
        invalidate();
    }

    /**
     * Remplace la correspondance courante : une transaction, une
     * unité d'annulation, la recherche se relance après (les
     * positions suivantes ont bougé).
     *
     * @param replacement le texte de remplacement
     */
    public void replaceCurrent(String replacement) {
        if (searchQuery == null || searchCurrent < 0
                || searchCurrent >= searchMatches.length) {
            return;
        }
        int start = searchMatches[searchCurrent];
        int end = start + searchQuery.length();
        if (end > buffer.length()) {
            return;
        }
        applyEdit(start, end, replacement);
        search(searchQuery);
    }

    /**
     * Remplace TOUTES les correspondances en une transaction (une
     * seule unité d'annulation) : les remplacements se font de la
     * fin vers le début — les positions antérieures restent valides,
     * rien ne se recalcule en cours de passe. La recherche se relance
     * après.
     *
     * @param replacement le texte de remplacement
     * @return le nombre de remplacements faits
     */
    public int replaceAll(String replacement) {
        if (searchQuery == null || searchMatches.length == 0) {
            return 0;
        }
        // un instantané avant la transaction : l'écouteur du tampon
        // (notifié au PREMIER remplacement) range la recherche — la
        // boucle doit tenir ses positions, pas celles du champ
        int[] matches = searchMatches;
        int count = matches.length;
        int firstStart = matches[0];
        try (TextTransaction transaction = undoStack.newTransaction()) {
            for (int m = count - 1; m >= 0; m--) {
                int start = matches[m];
                int end = start + searchQuery.length();
                if (end > buffer.length()) {
                    continue;
                }
                transaction.replace(start, end, replacement);
            }
        }
        selectionAnchor = -1;
        caretOffset = Math.min(firstStart + replacement.length(), buffer.length());
        search(searchQuery);
        return count;
    }

    /**
     * La popup d'info ancrée : le texte se montre au-dessus de la
     * ligne donnée (sous elle sans la place), bornée aux bords de la
     * vue. L'appui suivant la masque ; une édition la masque aussi.
     *
     * @param text   le texte à montrer, vide ou null pour masquer
     * @param line   la ligne d'ancrage
     * @param column la colonne d'ancrage
     */
    public void showInfoPopup(String text, int line, int column) {
        infoPopupText = text == null || text.isEmpty() ? null : text;
        infoPopupLine = line;
        infoPopupColumn = column;
        invalidate();
    }

    /**
     * Le fournisseur d'info de la popup ancrée : l'hôte y branche une
     * source extérieure (un serveur de langage par exemple) — la vue
     * reste au-dessus du protocole, elle ne connaît que la question
     * (où, quel mot) et la réponse (un texte, ou null pour dire
     * « je ne sais pas, demande à l'index local »).
     */
    public interface InfoPopupProvider {

        /**
         * L'info du mot demandé.
         *
         * @param line   la ligne du doigt
         * @param column la colonne du doigt
         * @param word   le mot sous le doigt
         * @return le texte à montrer, null pour retomber sur l'index
         */
        String infoFor(int line, int column, String word);
    }

    /**
     * Branche un fournisseur d'info (null débranche : l'index local
     * reprend la popup).
     *
     * @param provider le fournisseur, ou null
     */
    public void setInfoProvider(InfoPopupProvider provider) {
        infoProvider = provider;
    }

    private InfoPopupProvider infoProvider;

    /**
     * Le fournisseur de complétion : le bandeau peut s'y nourrir
     * d'une source extérieure (un serveur de langage) plutôt que de
     * l'index local. La vue tire à chaque trame — le fournisseur
     * d'aplomb y cache sa réponse (la sienne revient asynchrone :
     * qu'il serve la dernière reçue et invalide la vue quand la
     * neuve arrive, la trame suivante la porte).
     */
    public interface SuggestionProvider {

        /**
         * Les complétions du préfixe demandé.
         *
         * @param prefix le préfixe tapé (du début du mot au curseur)
         * @param line   la ligne du curseur
         * @param column la colonne du curseur
         * @return les candidats, jamais null (vide : bandeau vide)
         */
        List<String> completionsFor(String prefix, int line, int column);
    }

    /**
     * Branche un fournisseur de complétion (null débranche : l'index
     * local reprend le bandeau).
     *
     * @param provider le fournisseur, ou null
     */
    public void setSuggestionProvider(SuggestionProvider provider) {
        suggestionProvider = provider;
    }

    private SuggestionProvider suggestionProvider;

    /** Masque la popup d'info. */
    public void dismissInfoPopup() {
        if (infoPopupText != null) {
            infoPopupText = null;
            invalidate();
        }
    }

    /** La popup d'info est-elle visible ? */
    public boolean isInfoPopupVisible() {
        return infoPopupText != null;
    }

    /**
     * Le texte de la popup d'info (le harnais le vérifie, l'hôte peut
     * le journaliser).
     *
     * @return le texte affiché, null si masquée
     */
    public String infoPopupText() {
        return infoPopupText;
    }

    private void gotoSearchMatch(int index) {
        searchCurrent = index;
        int offset = searchMatches[index];
        selectionAnchor = -1;
        caretOffset = Math.min(offset + searchQuery.length(), buffer.length());
        ensureCaretVisible();
        invalidate();
    }

    /**
     * La sous-chaîne suivante : le tampon n'offre pas d'indexOf sur
     * vue, la boucle reste ici — le texte entier a déjà été lu.
     */
    private static int indexOf(CharSequence text, String needle, int from) {
        int limit = text.length() - needle.length();
        for (int at = Math.max(0, from); at <= limit; at++) {
            int probe = 0;
            while (probe < needle.length() && text.charAt(at + probe) == needle.charAt(probe)) {
                probe++;
            }
            if (probe == needle.length()) {
                return at;
            }
        }
        return -1;
    }

    /** Un tableau d'entiers extensible sans dépendance (la collecte des correspondances). */
    private static final class IntArray {

        private int[] values = new int[16];

        private int size;

        void add(int value) {
            if (size == values.length) {
                values = java.util.Arrays.copyOf(values, values.length * 2);
            }
            values[size++] = value;
        }

        int size() {
            return size;
        }

        int[] toArray() {
            return java.util.Arrays.copyOf(values, size);
        }
    }

    @Override
    public boolean onCheckIsTextEditor() {
        return true;
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        outAttrs.inputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE;
        outAttrs.imeOptions = EditorInfo.IME_FLAG_NO_FULLSCREEN;
        return new EditorInputConnection(this);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                touchStartX = event.getX();
                touchStartY = event.getY();
                touchDownNanos = System.nanoTime();
                // un nouvel appui masque la popup d'info (l'appui long
                // pourra en rouvrir une sur le mot touché)
                dismissInfoPopup();
                dragging = false;
                selectionDragBound = 0;
                dragFixedBound = -1;
                // une sélection vivante se saisit d'abord par ses
                // poignées (le défilement n'y a pas droit)
                grabSelectionHandle(event.getX(), event.getY());
                // le traîné-déposé : un appui dans le TEXTE de la
                // sélection (pas sur une poignée) l'arme — le glissé
                // qui suit, avant le seuil de l'appui long, déplacera
                // le texte au doigt
                textDragArmed = selectionDragBound == 0
                        && event.getX() >= getPaddingLeft()
                        && hasSelection()
                        && selectionStart() <= offsetNearest(event.getX(), event.getY())
                        && offsetNearest(event.getX(), event.getY()) <= selectionEnd();
                textDragging = false;
                dropOffset = -1;
                break;
            case MotionEvent.ACTION_MOVE:
                if (selectionDragBound != 0) {
                    dragSelectionBound(event.getX(), event.getY());
                    break;
                }
                if (textDragArmed && !textDragging && !heldLongEnough()
                        && (Math.abs(event.getX() - touchStartX) > touchSlop()
                                || Math.abs(event.getY() - touchStartY) > touchSlop())) {
                    // le traîné s'engage : le texte de la sélection
                    // suivra le doigt jusqu'au dépôt (l'appui long qui
                    // glisse garde sa voie : l'étendue de sélection)
                    requestFocus();
                    clearComposing();
                    dismissInfoPopup();
                    textDragging = true;
                }
                if (textDragging) {
                    // le curseur de dépôt suit le doigt : le caractère
                    // le plus proche (il se dessine à la place du
                    // curseur le temps du geste)
                    dropOffset = offsetNearest(event.getX(), event.getY());
                    invalidate();
                    break;
                }
                if (!dragging && heldLongEnough()
                        && (Math.abs(event.getX() - touchStartX) > touchSlop()
                                || Math.abs(event.getY() - touchStartY) > touchSlop())) {
                    // l'appui long qui glisse : la sélection s'étend au
                    // doigt — le mot sous l'appui sème la sélection
                    // s'il n'y en a pas, la borne la plus proche du
                    // doigt suit ensuite (le défilement n'y a pas
                    // droit non plus)
                    requestFocus();
                    clearComposing();
                    if (!hasSelection()) {
                        selectWordAt(touchStartX, touchStartY);
                    }
                    dismissInfoPopup();
                    engageSelectionDrag(event.getX(), event.getY());
                    dragSelectionBound(event.getX(), event.getY());
                    break;
                }
                if (!dragging && Math.abs(event.getY() - touchStartY) > touchSlop()) {
                    dragging = true;
                }
                if (dragging) {
                    scrollY = clampToRange(scrollY - (event.getY() - touchStartY),
                            0.0f, maxScroll());
                    touchStartY = event.getY();
                    touchStartX = event.getX();
                    invalidate();
                }
                break;
            case MotionEvent.ACTION_UP:
                if (selectionDragBound != 0) {
                    // la poignée se lâche : la sélection reste posée,
                    // l'appui ne compte pas pour un tap (le suivant ne
                    // serait pas un double-tap)
                    selectionDragBound = 0;
                    dragFixedBound = -1;
                    lastTapNanos = 0L;
                    break;
                }
                if (textDragging) {
                    // le dépôt : le texte sélectionné se déplace au
                    // curseur de dépôt — l'appui ne compte pas pour un
                    // tap non plus
                    dropTextAtSelection(dropOffset);
                    textDragging = false;
                    textDragArmed = false;
                    dropOffset = -1;
                    lastTapNanos = 0L;
                    break;
                }
                if (dragging) {
                    dragging = false;
                } else if (event.getX() < getPaddingLeft()) {
                    // la gouttière : le pli de la ligne touchée se
                    // retourne (le clavier ne s'y ouvre pas)
                    requestFocus();
                    toggleFoldAt(lineOfVisibleIndex(
                            (int) ((event.getY() - getPaddingTop() + scrollY) / lineHeight)));
                    clearComposing();
                } else if (!suggestions.isEmpty() && event.getY() > getHeight()
                        - dp(28) - getPaddingBottom()) {
                    // le bandeau de complétion : le candidat désigné
                    // complète le mot courant
                    requestFocus();
                    insertSuggestion(event.getX());
                    clearComposing();
                } else if (heldLongEnough()
                        || tappedTwice(event.getX(), event.getY())) {
                    // l'appui long ou le double-tap : le mot sous le
                    // doigt se sélectionne (la frappe suivante le
                    // remplacera)
                    requestFocus();
                    selectWordAt(event.getX(), event.getY());
                    clearComposing();
                } else {
                    requestFocus();
                    placeCaretAt(event.getX(), event.getY());
                    clearComposing();
                    showSoftInput();
                }
                lastTapNanos = System.nanoTime();
                lastTapX = event.getX();
                lastTapY = event.getY();
                break;
            default:
                break;
        }
        return true;
    }

    /** L'appui courant a-t-il duré au-delà du seuil de l'appui long ? */
    private boolean heldLongEnough() {
        return System.nanoTime() - touchDownNanos > 500_000_000L;
    }

    /** Le tap courant suit-il de près un tap au même endroit ? */
    private boolean tappedTwice(float x, float y) {
        return lastTapNanos > 0L
                && System.nanoTime() - lastTapNanos < 350_000_000L
                && Math.abs(x - lastTapX) < touchSlop()
                && Math.abs(y - lastTapY) < touchSlop();
    }

    /**
     * L'offset le plus proche d'un point de vue : la ligne par la
     * position défilée, la colonne par la largeur de caractère (la
     * colonne se borne à la ligne — un doigt sous la dernière ligne
     * y reste).
     */
    private int offsetNearest(float x, float y) {
        int line = buffer.getLineCount() - 1;
        if (lineHeight > 0.0f) {
            line = lineOfVisibleIndex(
                    (int) ((y - getPaddingTop() + scrollY) / lineHeight));
        }
        int lineStart = buffer.getLineStart(line);
        int lineEnd = buffer.getLineEnd(line);
        int column = charWidth > 0.0f
                ? clampToRange((int) ((x - getPaddingLeft()) / charWidth), 0, lineEnd - lineStart)
                : 0;
        return buffer.offsetOfPosition(line, column);
    }

    /**
     * Le centre d'une poignée de sélection : sous la borne (le cercle
     * du bas de ligne), au-dessus si la ligne touche le bas de la vue
     * — le dessin et la saisie partagent la géométrie.
     */
    private float[] handleCenter(int offset) {
        int line = buffer.lineOfOffset(offset);
        int column = offset - buffer.getLineStart(line);
        float x = getPaddingLeft() + column * charWidth;
        float top = getPaddingTop() + visibleIndexOf(line) * lineHeight - scrollY;
        float y = top + lineHeight + dp(5);
        if (y + dp(7) > getHeight() - getPaddingBottom()) {
            y = top - dp(5);
        }
        return new float[] {x, y};
    }

    /**
     * La poignée sous le doigt se saisit : la borne la plus proche
     * devient la borne traînée, l'autre se fixe. Une cible de
     * 24 dp de rayon (48 dp de diamètre — la consigne d'ergonomie).
     */
    private void grabSelectionHandle(float x, float y) {
        if (!hasSelection()) {
            return;
        }
        float[] start = handleCenter(selectionStart());
        float[] end = handleCenter(selectionEnd());
        float toStart = distanceTo(x, y, start[0], start[1]);
        float toEnd = distanceTo(x, y, end[0], end[1]);
        float radius = dp(24);
        if (Math.min(toStart, toEnd) > radius) {
            return;
        }
        requestFocus();
        clearComposing();
        selectionDragBound = toStart <= toEnd ? 1 : 2;
        dragFixedBound = selectionDragBound == 1 ? selectionEnd() : selectionStart();
        dragFingerY = y;
    }

    /**
     * L'engagement du traînement sans poignée saisie (l'appui long
     * qui glisse) : la borne la plus proche du doigt devient la borne
     * traînée.
     */
    private void engageSelectionDrag(float x, float y) {
        float[] start = handleCenter(selectionStart());
        float[] end = handleCenter(selectionEnd());
        selectionDragBound = distanceTo(x, y, start[0], start[1])
                <= distanceTo(x, y, end[0], end[1]) ? 1 : 2;
        dragFixedBound = selectionDragBound == 1 ? selectionEnd() : selectionStart();
        dragFingerY = y;
    }

    /**
     * La borne traînée suit le doigt : l'offset sous le doigt borne
     * la sélection côté traîné, la borne fixe la garde de l'autre
     * côté — la sélection ne s'inverse pas en cours de traînement.
     * Le doigt près des bords fait défiler (une ligne par événement).
     */
    private void dragSelectionBound(float x, float y) {
        autoScrollNearEdges(y);
        dragFingerY = y;
        int offset = offsetNearest(x, y);
        if (selectionDragBound == 1) {
            caretOffset = Math.min(offset, dragFixedBound);
            selectionAnchor = dragFixedBound;
        } else {
            caretOffset = Math.max(offset, dragFixedBound);
            selectionAnchor = dragFixedBound;
        }
        invalidate();
    }

    /**
     * Le défilement de suivi : le doigt dans la bande du haut ou du
     * bas fait défiler d'une ligne (la saisie suit alors le texte
     * révélé).
     */
    private void autoScrollNearEdges(float y) {
        float band = dp(24);
        float top = getPaddingTop();
        float bottom = getHeight() - getPaddingBottom();
        if (y < top + band) {
            scrollY = clampToRange(scrollY - lineHeight, 0.0f, maxScroll());
        } else if (y > bottom - band) {
            scrollY = clampToRange(scrollY + lineHeight, 0.0f, maxScroll());
        }
    }

    private static float distanceTo(float x, float y, float toX, float toY) {
        float dx = x - toX;
        float dy = y - toY;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        long frameBegin = System.nanoTime();
        long memoryAtFrameBegin = Runtime.getRuntime().freeMemory();
        lastFrameEngineNanos = 0L;
        super.onDraw(canvas);
        canvas.drawColor(themeBackground);
        int count = buffer.getLineCount();
        int first = firstVisibleLine();
        int last = lastVisibleLine();
        updateStructureCaches();
        // la bande de portée se peint avant le texte (le texte se
        // dessine dessus), la sélection par-dessus elle, la recherche
        // par-dessus tout ça (la correspondance courante domine), les
        // guides et la paire après
        drawScopeBand(canvas, first, last, count);
        drawSelection(canvas, first, last, count);
        drawSearchHighlights(canvas, first, last, count);
        for (int line = first; line <= last && line < count; line++) {
            if (isHidden(line)) {
                continue;
            }
            int start = buffer.getLineStart(line);
            int end = buffer.getLineEnd(line);
            float baseline = lineBaseline(line);
            if (end > start) {
                drawTokens(canvas, line, start, baseline);
            }
            if (composingStart >= 0) {
                drawComposeUnderline(canvas, line, start, end, baseline);
            }
        }
        drawStructure(canvas, first, last, count);
        drawCaret(canvas);
        drawSelectionHandles(canvas);
        drawLoupe(canvas);
        // les candidats AVANT leur dessin (un déplacement de curseur
        // rend ses candidats dans la même trame), les soulignés de
        // diagnostic par-dessus le texte, le bandeau par-dessus tout,
        // la popup d'info par-dessus le bandeau
        refreshAnalysis();
        updateSuggestions();
        drawDiagnostics(canvas, first, last, count);
        drawSuggestions(canvas);
        drawInfoPopup(canvas);
        // la fenêtre visible est payée : le reste remplit au fil de fond
        ensureFill();
        lastFrameNanos = System.nanoTime() - frameBegin;
        if (lastFrameNanos > worstFrameNanos) {
            worstFrameNanos = lastFrameNanos;
            worstFrameEngineNanos = lastFrameEngineNanos;
            // la mémoire rendue pendant la trame : un ramasse-miettes
            // qui y court laisse sa signature (un bond de mémoire libre)
            worstFrameMemoryDelta =
                    Runtime.getRuntime().freeMemory() - memoryAtFrameBegin;
        }
        frameCount++;
    }

    /**
     * Recalcule la paire et la portée quand le curseur ou le texte
     * bouge (les deux lectures partagent le même cache — un
     * déplacement ne paie les marches qu'une fois).
     */
    private void updateStructureCaches() {
        if (caretOffset == pairCaret && buffer.getRevision() == pairRevision) {
            return;
        }
        pairCaret = caretOffset;
        pairRevision = buffer.getRevision();
        int caret = Math.min(caretOffset, buffer.length());
        bracketPair = structure.pairAt(caret);
        scopeRange = structure.scopeAt(caret);
    }

    /**
     * La bande de portée : le bloc englobant du curseur se teinte en
     * fond — la plage se borne à la fenêtre visible, les lignes
     * cachées par un pli ne l'élargissent pas.
     */
    private void drawScopeBand(Canvas canvas, int first, int last, int count) {
        if (scopeRange == null) {
            return;
        }
        int from = Math.max(scopeRange.fromLine(), first);
        int to = Math.min(Math.min(scopeRange.toLine(), last), count - 1);
        while (from <= to && isHidden(from)) {
            from++;
        }
        while (to >= from && isHidden(to)) {
            to--;
        }
        if (from > to) {
            return;
        }
        float top = getPaddingTop() + visibleIndexOf(from) * lineHeight - scrollY;
        float bottom = getPaddingTop() + (visibleIndexOf(to) + 1) * lineHeight - scrollY;
        canvas.drawRect(getPaddingLeft(), top, getWidth() - getPaddingRight(),
                bottom, scopePaint);
    }

    /**
     * La teinte de sélection, ligne visible par ligne visible : de la
     * borne d'entrée à la fin du texte de la ligne (les lignes
     * entièrement dedans se teintent entières, celles cachées par un
     * pli ne se dessinent pas — le plis les remplace visuellement).
     */
    private void drawSelection(Canvas canvas, int first, int last, int count) {
        if (!hasSelection()) {
            return;
        }
        int from = selectionStart();
        int to = selectionEnd();
        for (int line = first; line <= last && line < count; line++) {
            if (isHidden(line)) {
                continue;
            }
            int lineStart = buffer.getLineStart(line);
            int lineEnd = buffer.getLineEnd(line);
            int start = Math.max(from, lineStart);
            int end = Math.min(to, lineEnd);
            if (end <= start) {
                continue;
            }
            float left = getPaddingLeft() + (start - lineStart) * charWidth;
            float right = getPaddingLeft() + (end - lineStart) * charWidth;
            float top = getPaddingTop() + visibleIndexOf(line) * lineHeight - scrollY;
            canvas.drawRect(left, top, right, top + lineHeight, selectionPaint);
        }
    }

    /**
     * Les teintes de recherche : chaque correspondance DANS la fenêtre
     * visible se teinte, la courante plus dense — le parcours des
     * correspondances reste linéaire (le plafond le borne), le test
     * d'appartenance écarte l'essentiel.
     */
    private void drawSearchHighlights(Canvas canvas, int first, int last, int count) {
        if (searchQuery == null || searchMatches.length == 0) {
            return;
        }
        int queryLength = searchQuery.length();
        int windowStart = buffer.getLineStart(first);
        int windowEnd = last + 1 < count
                ? buffer.getLineStart(last + 1) : buffer.length();
        for (int m = 0; m < searchMatches.length; m++) {
            int start = searchMatches[m];
            if (start >= windowEnd) {
                break;
            }
            int end = start + queryLength;
            if (end <= windowStart) {
                continue;
            }
            int line = buffer.lineOfOffset(start);
            if (isHidden(line)) {
                continue;
            }
            int lineStart = buffer.getLineStart(line);
            int columnStart = Math.max(start, windowStart) - lineStart;
            int columnEnd = Math.min(end, windowEnd) - lineStart;
            float left = getPaddingLeft() + columnStart * charWidth;
            float right = getPaddingLeft() + columnEnd * charWidth;
            float top = getPaddingTop() + visibleIndexOf(line) * lineHeight - scrollY;
            canvas.drawRect(left, top, right, top + lineHeight,
                    m == searchCurrent ? searchCurrentPaint : searchPaint);
        }
    }

    /**
     * La popup d'info ancrée : un rectangle arrondi au-dessus de la
     * ligne d'ancrage (sous elle sans la place), borné aux bords de la
     * vue, le texte coupé à la largeur disponible — elle se dessine en
     * dernier, par-dessus le bandeau.
     */
    private void drawInfoPopup(Canvas canvas) {
        if (infoPopupText == null) {
            return;
        }
        float textWidth = infoPaint.measureText(infoPopupText);
        float padding = dp(8);
        float boxWidth = Math.min(textWidth + 2 * padding,
                getWidth() - getPaddingLeft() - getPaddingRight());
        float boxHeight = -infoPaint.getFontMetrics().top + dp(2) + padding;
        float anchorTop = getPaddingTop() + visibleIndexOf(infoPopupLine) * lineHeight
                - scrollY;
        float left = Math.min(
                Math.max(getPaddingLeft(),
                        getPaddingLeft() + infoPopupColumn * charWidth - padding),
                getWidth() - getPaddingRight() - boxWidth);
        float top;
        if (anchorTop - boxHeight - dp(4) >= getPaddingTop()) {
            top = anchorTop - boxHeight - dp(4);
        } else {
            top = Math.min(anchorTop + lineHeight + dp(4),
                    getHeight() - boxHeight);
        }
        canvas.drawRoundRect(left, top, left + boxWidth, top + boxHeight,
                dp(4), dp(4), infoBackground);
        canvas.drawRoundRect(left, top, left + boxWidth, top + boxHeight,
                dp(4), dp(4), infoBorder);
        // le texte coupé à la place disponible (les mots longs se
        // tronquent, la popup reste dans la vue)
        float maxChars = Math.max(1.0f, (boxWidth - 2 * padding) / charWidth);
        String shown = infoPopupText.length() > maxChars
                ? infoPopupText.substring(0, (int) maxChars) : infoPopupText;
        canvas.drawText(shown, left + padding,
                top - infoPaint.getFontMetrics().top, infoPaint);
    }

    /**
     * Dessine la structure de L2 par-dessus les plans : les guides
     * d'indentation (un trait par niveau de profondeur, la chaîne
     * d'appariement — pas les espaces), le marqueur de pli dans la
     * gouttière, et le surlignage de la paire du curseur.
     */
    private void drawStructure(Canvas canvas, int first, int last, int count) {
        for (int line = first; line <= last && line < count; line++) {
            if (isHidden(line)) {
                continue;
            }
            boolean opens = structure.foldStartsAt(line);
            if (opens) {
                drawFoldMarker(canvas, line, folded.contains(line));
            }
            // le niveau ne guide que s'il couvre TOUTE la ligne : le fil
            // s'arrête UNE LIGNE AVANT le fermant, et ne commence
            // qu'APRÈS la ligne de l'ouvrant
            int depthIn = line == 0 ? 0 : structure.structureOf(line - 1).depthOut();
            int guides = Math.min(depthIn, structure.structureOf(line).depthOut());
            if (guides <= 0) {
                continue;
            }
            float baseline = lineBaseline(line);
            float top = baseline + fontMetrics.top;
            float bottom = baseline + fontMetrics.bottom;
            float left = getPaddingLeft();
            for (int level = 1; level <= guides; level++) {
                // un guide par bande de 4 colonnes, centré sur la bande
                float x = left + (GUIDE_COLUMNS * level - 2) * charWidth;
                canvas.drawLine(x, top, x, bottom, guidePaint);
            }
        }
        if (bracketPair == null) {
            return;
        }
        highlightPairEnd(canvas, first, last, bracketPair.fromLine(),
                bracketPair.fromColumn());
        highlightPairEnd(canvas, first, last, bracketPair.toLine(),
                bracketPair.toColumn());
    }

    /**
     * Le marqueur de pli dans la gouttière : un cadre et son signe —
     * moins quand le bloc est ouvert, plus quand il est replié.
     */
    private void drawFoldMarker(Canvas canvas, int line, boolean isFolded) {
        float baseline = lineBaseline(line);
        float mid = baseline + (fontMetrics.top + fontMetrics.bottom) / 2.0f;
        float left = getPaddingLeft() - dp(9);
        float right = left + dp(7);
        canvas.drawRect(left, mid - dp(3), right, mid + dp(3), bracketPaint);
        canvas.drawLine(left + dp(1), mid, right - dp(1), mid, bracketPaint);
        if (isFolded) {
            float midX = (left + right) / 2.0f;
            canvas.drawLine(midX, mid - dp(2), midX, mid + dp(2), bracketPaint);
        }
    }

    /**
     * Surligne un bout de paire s'il est visible : un cadre fin autour
     * du signe (la bande verticale de la ligne).
     */
    private void highlightPairEnd(Canvas canvas, int first, int last,
            int line, int column) {
        if (line < first || line > last || isHidden(line)) {
            return;
        }
        float baseline = lineBaseline(line);
        float x = getPaddingLeft() + column * charWidth;
        canvas.drawRect(x - dp(1), baseline + fontMetrics.top + dp(2),
                x + charWidth + dp(1), baseline + fontMetrics.bottom - dp(2),
                bracketPaint);
    }

    // --------------------------------------------------- plis (régions repliables)

    /**
     * Replie ou déplie le bloc ouvert à cette ligne (la gouttière le
     * demande au toucher) ; une ligne sans ouvrant ne fait rien.
     *
     * @param line indice de la ligne d'en-tête
     */
    public void toggleFoldAt(int line) {
        if (structure == null || !structure.foldStartsAt(line)) {
            return;
        }
        if (folded.contains(line)) {
            folded.remove(line);
        } else {
            folded.add(line);
            // le curseur dans la région repliée : il remonte à la fin de
            // l'en-tête (juste après l'ouvrant, la paire s'y encadre)
            int end = structure.foldEndLine(line);
            int caretLine = buffer.lineOfOffset(Math.min(caretOffset, buffer.length()));
            if (caretLine > line && caretLine <= end) {
                caretOffset = buffer.getLineEnd(line);
            }
        }
        rebuildFoldLayout();
        ensureCaretVisible();
        invalidate();
    }

    /**
     * Lignes cachées par les plis courants (le panneau de diagnostic le
     * lit, le harnais le vérifie).
     *
     * @return le nombre de lignes invisibles
     */
    public int hiddenLineCount() {
        return hiddenLineCount;
    }

    /**
     * Plis actuellement repliés (le panneau le lit).
     *
     * @return le nombre d'en-têtes repliés
     */
    public int foldedCount() {
        return folded.size();
    }

    /**
     * Décale les en-têtes de plis suivant l'édition (les lignes après
     * le point d'édition bougent du delta de lignes), puis reconstruit
     * le plan des cachées — un pli dont l'en-tête n'ouvre plus rien se
     * dissout à la reconstruction.
     */
    private void shiftFolds(int start) {
        if (folded.isEmpty()) {
            trackedLineCount = buffer.getLineCount();
            return;
        }
        int line = buffer.lineOfOffset(start);
        int delta = buffer.getLineCount() - trackedLineCount;
        TreeSet<Integer> shifted = new TreeSet<>();
        for (int header : folded) {
            int moved = header < line ? header : header + delta;
            if (moved >= 0 && moved < buffer.getLineCount()) {
                shifted.add(moved);
            }
        }
        folded.clear();
        folded.addAll(shifted);
        rebuildFoldLayout();
        revealCaret();
    }

    /**
     * Le curseur ne vit jamais dans une ligne cachée : le pli le plus
     * interne qui le cache s'ouvre (l'édition y arrive).
     */
    private void revealCaret() {
        if (hiddenByFold == null) {
            return;
        }
        int line = buffer.lineOfOffset(Math.min(caretOffset, buffer.length()));
        // la reconstruction peut annuler le tableau (dernier pli ouvert) :
        // la garde se relit à chaque tour
        while (hiddenByFold != null && line < hiddenByFold.length
                && hiddenByFold[line]) {
            int container = -1;
            for (int header : folded) {
                if (header < line && line <= structure.foldEndLine(header)) {
                    container = header;
                }
            }
            if (container < 0) {
                return;
            }
            folded.remove(container);
            rebuildFoldLayout();
        }
    }

    /**
     * Reconstruit les lignes cachées et le mapping visible des plis
     * courants (une passe en O(lignes), aux changements de plis et aux
     * éditions — jamais par trame).
     */
    private void rebuildFoldLayout() {
        int count = buffer.getLineCount();
        trackedLineCount = count;
        if (folded.isEmpty()) {
            hiddenByFold = null;
            visibleBefore = null;
            hiddenLineCount = 0;
            return;
        }
        boolean[] hidden = new boolean[count];
        for (int header : folded) {
            if (header < 0 || header >= count || !structure.foldStartsAt(header)) {
                continue; // l'édition a déplacé la structure : plus d'ouvrant
            }
            int end = structure.foldEndLine(header);
            for (int line = header + 1; line <= end && line < count; line++) {
                hidden[line] = true;
            }
        }
        int[] before = new int[count];
        int visible = 0;
        for (int line = 0; line < count; line++) {
            before[line] = visible;
            if (!hidden[line]) {
                visible++;
            }
        }
        hiddenByFold = hidden;
        visibleBefore = before;
        hiddenLineCount = count - visible;
    }

    /** La ligne est-elle cachée par un pli. */
    private boolean isHidden(int line) {
        return hiddenByFold != null && line < hiddenByFold.length
                && hiddenByFold[line];
    }

    /** L'indice visible d'une ligne (les plis compactent la hauteur). */
    private int visibleIndexOf(int line) {
        return visibleBefore == null ? line : visibleBefore[line];
    }

    /** Nombre de lignes visibles (le document moins les cachées). */
    private int visibleLineCount() {
        return buffer.getLineCount() - hiddenLineCount;
    }

    /**
     * La ligne visible d'indice donné : la dernière ligne dont
     * l'indice visible la précède ou l'égale (les cachées partagent
     * l'indice de la visible qui les suit).
     */
    private int lineOfVisibleIndex(int index) {
        int count = buffer.getLineCount();
        if (visibleBefore == null) {
            return clampToRange(index, 0, Math.max(0, count - 1));
        }
        if (index >= visibleLineCount()) {
            index = visibleLineCount() - 1;
        }
        int low = 0;
        int high = count - 1;
        while (low < high) {
            int mid = (low + high + 1) / 2;
            if (visibleBefore[mid] <= index) {
                low = mid;
            } else {
                high = mid - 1;
            }
        }
        return low;
    }

    /** Ligne de base d'une ligne VISIBLE (le mapping des plis s'applique). */
    private float lineBaseline(int line) {
        int visible = visibleBefore == null ? line : visibleBefore[line];
        return getPaddingTop() - fontMetrics.top + visible * lineHeight - scrollY;
    }

    /**
     * Dessine la ligne depuis son plan de rendu : le plan se valide par
     * identité d'entrée (aucune lecture de texte sur le chemin rapide),
     * se partage par contenu (les lignes identiques ne cuisent qu'une
     * fois) et porte les couleurs résolues.
     */
    private void drawTokens(Canvas canvas, int line, int start, float baseline) {
        long engineBegin = System.nanoTime();
        SyntaxDocument.LineTokens entry = syntax.lineTokens(line);
        lastFrameEngineNanos += System.nanoTime() - engineBegin;
        int lineEnd = buffer.getLineEnd(line);
        LinePlan plan = renderCache.frame(line, entry,
                () -> buffer.getText(start, lineEnd), this::bakeLine);
        String[] texts = plan.texts();
        int[] colors = plan.colors();
        float[] xs = plan.xs();
        boolean[] bolds = plan.bolds();
        float left = getPaddingLeft();
        for (int at = 0; at < texts.length; at++) {
            textPaint.setColor(colors[at]);
            // le gras sémantique (L3) : un drapeau de peinture, pas une
            // seconde police
            textPaint.setFakeBoldText(bolds != null && bolds[at]);
            canvas.drawText(texts[at], left + xs[at], baseline, textPaint);
        }
        textPaint.setFakeBoldText(false);
    }

    /**
     * Cuit le plan d'une ligne : textes tranchés jusqu'au plafond de
     * dessin, couleurs résolues depuis les portées, abscisses en
     * colonnes (la marge s'ajoute au dessin). Une ligne sans jeton
     * (ligne monstre, hors plafond moteur) se cuit en texte brut.
     */
    private LinePlan bakeLine(int line, Object stamp, CharSequence content) {
        SyntaxDocument.Token[] tokens = ((SyntaxDocument.LineTokens) stamp).tokens();
        if (tokens.length == 0) {
            int drawn = Math.min(content.length(), MAX_DRAWN_COLUMNS);
            return new LinePlan(
                    new String[] {content.subSequence(0, drawn).toString()},
                    new int[] {theme.defaultColor()},
                    new float[] {0.0f},
                    null);
        }
        // la fusion L3 se paie par cuisson (pas par trame) : les
        // annotations de la ligne, une passe
        List<SemanticAnnotation> notes = semantic == null
                ? List.of() : semantic.annotationsAt(line);
        int lineStart = buffer.getLineStart(line);
        int kept = 0;
        for (SyntaxDocument.Token token : tokens) {
            if (token.end() <= token.start()) {
                continue;
            }
            if (token.start() >= MAX_DRAWN_COLUMNS) {
                break;
            }
            kept++;
        }
        String[] texts = new String[kept];
        int[] colors = new int[kept];
        float[] xs = new float[kept];
        boolean[] bolds = new boolean[kept];
        int at = 0;
        for (SyntaxDocument.Token token : tokens) {
            if (token.end() <= token.start()) {
                continue;
            }
            if (token.start() >= MAX_DRAWN_COLUMNS) {
                break;
            }
            int endColumn = Math.min(token.end(), MAX_DRAWN_COLUMNS);
            texts[at] = content.subSequence(token.start(), endColumn).toString();
            colors[at] = token.scope() == null
                    ? theme.defaultColor() : theme.colorOf(token.scope());
            xs[at] = token.start() * charWidth;
            if (token.scope() == null && !notes.isEmpty()) {
                // fusion L3 > L2 > L1 : seul un neutre peut se colorer —
                // une couleur L1 affichée ne change jamais
                for (SemanticAnnotation note : notes) {
                    if (note.covers(lineStart + token.start(), lineStart + endColumn)) {
                        colors[at] = EditorThemes.semanticColor(theme, note.nature());
                        bolds[at] = (note.modifiers() & SemanticAnnotation.BOLD) != 0;
                        break;
                    }
                }
            }
            at++;
        }
        return new LinePlan(texts, colors, xs, bolds);
    }

    // -------------------------------------------- complétion et diagnostics

    /**
     * Les candidats du mot courant : le préfixe se lit au curseur (au
     * moins deux caractères, sinon le bandeau se vide), la requête
     * interroge le dernier index — un index périmé reste une bonne
     * indication (les noms, pas les positions).
     */
    private void updateSuggestions() {
        int caret = Math.min(caretOffset, buffer.length());
        int wordStart = wordStartAt(caret);
        if (wordStart < 0) {
            suggestions = List.of();
            suggestionWordStart = -1;
            return;
        }
        String prefix = buffer.getText(wordStart, caret).toString();
        suggestionWordStart = wordStart;
        if (suggestionProvider != null) {
            // la source extérieure tire à la trame : elle cache sa
            // réponse asynchrone (le contrat y est documenté)
            int line = buffer.lineOfOffset(caret);
            suggestions = suggestionProvider.completionsFor(prefix,
                    line, caret - buffer.getLineStart(line));
            return;
        }
        suggestions = symbolIndex == null
                ? List.of() : symbolIndex.query(prefix, 8);
    }

    /**
     * Le début du mot au curseur, ou -1 s'il est trop court. La fenêtre
     * de lecture est bornée : jamais le document pour un mot.
     */
    private int wordStartAt(int caret) {
        if (caret <= 0) {
            return -1;
        }
        int window = Math.min(64, caret);
        CharSequence text = buffer.getText(caret - window, caret);
        int at = window;
        while (at > 0 && Character.isJavaIdentifierPart(text.charAt(at - 1))) {
            at--;
        }
        if (at < window && Character.isJavaIdentifierStart(text.charAt(at))
                && window - at >= 2) {
            return caret - window + at;
        }
        return -1;
    }

    /**
     * Ralentit l'analyse (l'index de symboles et les diagnostics) : au
     * plus une passe par tranche de 300 ms — la frappe ne paie jamais
     * l'index, les trames tiennent leur budget. La première
     * construction non plus : le relevé 8 a mesuré 108 ms de trame —
     * la passe forçait le lexage du gros fichier entier dans le dessin
     * d'ouverture (le garde-fou des 300 ms ne couvrait que les
     * reconstructions). Désormais l'analyse attend que le fond ait
     * rempli le document, et se construit par tranches POSTÉES —
     * jamais dans une trame.
     */
    private void refreshAnalysis() {
        if (analysisSuspended) {
            // les documents ne sont pas ceux du texte courant (changement
            // de document en cours) : aucune passe ne part sur ce mélange
            return;
        }
        if (indexBuilder == null && symbolIndex != null
                && buffer.getRevision() == analysisRevision) {
            return;
        }
        if (syntax.lexedLineCount() < syntax.lineCount()) {
            // le fond n'a pas fini : forcer le lexage restant ici
            // reviendrait à le payer dans la trame
            return;
        }
        long now = System.nanoTime();
        if (now - lastAnalysisStartNanos < 300_000_000L) {
            return;
        }
        if (indexBuilder != null) {
            // une passe court déjà (ses tranches s'enchaînent seules)
            return;
        }
        indexBuilder = new SymbolIndex.Builder(buffer, syntax, structure,
                new JavaDeclarations());
        diagnosticsBuilder = new DiagnosticsDocument.Builder(buffer, syntax, structure);
        analysisPendingLine = 0;
        analysisBuildRevision = buffer.getRevision();
        lastAnalysisStartNanos = now;
        postAnalysisSlice();
    }

    /** Une tranche de l'analyse, postée : la trame qui la déclenche ne la paie pas. */
    private void postAnalysisSlice() {
        post(() -> {
            if (indexBuilder == null) {
                // abandonnée (changement de langage) : rien à avancer
                return;
            }
            int from = analysisPendingLine;
            int to = Math.min(from + ANALYSIS_SLICE_LINES, syntax.lineCount());
            indexBuilder.addLines(from, to);
            diagnosticsBuilder.addLines(from, to);
            analysisPendingLine = to;
            if (to < syntax.lineCount()) {
                postAnalysisSlice();
            } else if (buffer.getRevision() == analysisBuildRevision) {
                // la passe est close à la révision qui l'a lancée ; une
                // édition passée au milieu l'abandonne — le ralenti
                // reprendra, l'ancien index reste une indication
                symbolIndex = indexBuilder.build();
                diagnostics = diagnosticsBuilder.build();
                analysisRevision = analysisBuildRevision;
                lastAnalysisNanos = System.nanoTime();
                indexBuilder = null;
                diagnosticsBuilder = null;
                invalidate();
            } else {
                indexBuilder = null;
                diagnosticsBuilder = null;
            }
        });
    }

    /**
     * Point d'entrée de l'hôte : la même passe ralentie que la trame,
     * jamais bloquante — le panneau cadence l'arrivée des symboles et
     * des diagnostics quand l'éditeur ne se redessine pas.
     */
    public void ensureAnalysis() {
        refreshAnalysis();
    }

    /**
     * Les soulignés de diagnostic de la fenêtre visible : un trait rouge
     * sous chaque signe mésapparié — uniquement quand la passe est à la
     * révision courante (un souligné périmé serait mal placé).
     */
    private void drawDiagnostics(Canvas canvas, int first, int last, int count) {
        if (diagnostics == null || analysisRevision != buffer.getRevision()) {
            return;
        }
        for (DiagnosticsDocument.Diagnostic diagnostic
                : diagnostics.betweenLines(first, last)) {
            int line = buffer.lineOfOffset(Math.min(diagnostic.start(),
                    Math.max(0, buffer.length() - 1)));
            if (line < first || line > last || isHidden(line)) {
                continue;
            }
            int column = diagnostic.start() - buffer.getLineStart(line);
            float baseline = lineBaseline(line);
            float x = getPaddingLeft() + column * charWidth;
            canvas.drawLine(x, baseline + dp(3), x + charWidth, baseline + dp(3),
                    diagnosticPaint);
        }
    }

    /**
     * Le bandeau de complétion : les candidats du mot courant côte à
     * côte, en bas de la vue — les abscisses se gardent pour le toucher.
     */
    private void drawSuggestions(Canvas canvas) {
        if (suggestions.isEmpty()) {
            suggestionXs = new float[0];
            return;
        }
        float height = dp(28);
        float top = getHeight() - height - getPaddingBottom();
        if (top < 0.0f) {
            suggestionXs = new float[0];
            return;
        }
        canvas.drawRect(0.0f, top, getWidth(), top + height, suggestionBackground);
        float x = getPaddingLeft();
        float limit = getWidth() - getPaddingRight();
        float[] xs = new float[suggestions.size()];
        int drawn = 0;
        for (String candidate : suggestions) {
            float width = suggestionPaint.measureText(candidate) + dp(16);
            if (x + width > limit) {
                break;
            }
            xs[drawn] = x;
            canvas.drawText(candidate, x + dp(8),
                    top + height / 2.0f + dp(4), suggestionPaint);
            x += width;
            drawn++;
        }
        suggestionXs = drawn == xs.length ? xs : java.util.Arrays.copyOf(xs, drawn);
    }

    /**
     * Un toucher dans le bandeau : le candidat désigné complète le mot
     * courant (le préfixe se remplace par le candidat entier).
     *
     * @param x l'abscisse du toucher
     */
    private void insertSuggestion(float x) {
        for (int at = 0; at < suggestionXs.length; at++) {
            float next = at + 1 < suggestionXs.length
                    ? suggestionXs[at + 1] : getWidth();
            if (x >= suggestionXs[at] && x < next) {
                applyEdit(suggestionWordStart, Math.min(caretOffset, buffer.length()),
                        suggestions.get(at));
                return;
            }
        }
    }

    /**
     * Les candidats courants du bandeau (le harnais les vérifie, l'hôte
     * peut les afficher ailleurs).
     *
     * @return les candidats du mot courant, jamais {@code null}
     */
    public List<String> suggestions() {
        return suggestions;
    }

    /**
     * L'index courant (le panneau lit ses tailles).
     *
     * @return le nombre de déclarations, ou 0 sans index
     */
    public int symbolCount() {
        return symbolIndex == null ? 0 : symbolIndex.symbols().size();
    }

    /**
     * Les mots distincts de l'index courant (le panneau le lit).
     *
     * @return le nombre de mots connus
     */
    public int wordCount() {
        return symbolIndex == null ? 0 : symbolIndex.wordCount();
    }

    /**
     * Les diagnostics courants (le panneau le lit, le harnais le vérifie).
     *
     * @return le nombre de diagnostics
     */
    public int diagnosticCount() {
        return diagnostics == null ? 0 : diagnostics.diagnostics().size();
    }

    // ------------------------------------------------------------ interne

    private void drawComposeUnderline(Canvas canvas, int line, int start, int end, float baseline) {
        int from = Math.max(composingStart, start);
        int to = Math.min(composingEnd, end);
        if (to <= from) {
            return;
        }
        float x1 = getPaddingLeft() + (from - start) * charWidth;
        float x2 = getPaddingLeft() + (to - start) * charWidth;
        canvas.drawRect(x1, baseline + dp(1), x2, baseline + dp(3), composePaint);
    }

    private void drawCaret(Canvas canvas) {
        // le temps du traîné-déposé, le curseur se dessine au point de
        // dépôt : le doigt y voit où le texte se posera
        int offset = Math.min(textDragging ? dropOffset : caretOffset, buffer.length());
        int line = buffer.lineOfOffset(offset);
        if (line < firstVisibleLine() || line > lastVisibleLine() || isHidden(line)) {
            return;
        }
        int column = offset - buffer.getLineStart(line);
        float x = getPaddingLeft() + column * charWidth;
        float top = getPaddingTop() + visibleIndexOf(line) * lineHeight - scrollY;
        canvas.drawRect(x, top + dp(2), x + caretWidth, top + lineHeight - dp(2), caretPaint);
    }

    /**
     * Les poignées de sélection : un cercle sous chaque borne (au-
     * dessus de la ligne si elle touche le bas de la vue), un cœur
     * blanc pour la visibilité — les bornes hors fenêtre ne portent
     * pas de poignée (on n'y toucherait pas).
     */
    private void drawSelectionHandles(Canvas canvas) {
        if (!hasSelection()) {
            return;
        }
        drawHandle(canvas, selectionStart());
        drawHandle(canvas, selectionEnd());
    }

    private void drawHandle(Canvas canvas, int offset) {
        int line = buffer.lineOfOffset(offset);
        if (line < firstVisibleLine() || line > lastVisibleLine() || isHidden(line)) {
            return;
        }
        float[] center = handleCenter(offset);
        canvas.drawCircle(center[0], center[1], dp(7), handlePaint);
        canvas.drawCircle(center[0], center[1], dp(3), handleCorePaint);
    }

    /**
     * La loupe de précision : pendant le traînement d'une poignée, la
     * ligne de la borne traînée se redessine agrandie au-dessus du
     * doigt — la colonne exacte de la borne se lit, le doigt ne la
     * cache pas. Le contenu se peint sous homothétie dans une fenêtre
     * découpée (la teinte de sélection, les jetons, le curseur — la
     * borne traînée EST le curseur), le cadre arrondi la borne
     * par-dessus ; la loupe ne vit que le temps du traînement.
     */
    private void drawLoupe(Canvas canvas) {
        if (selectionDragBound == 0) {
            return;
        }
        int offset = Math.min(caretOffset, buffer.length());
        int line = buffer.lineOfOffset(offset);
        if (isHidden(line)) {
            return;
        }
        int lineStart = buffer.getLineStart(line);
        int column = offset - lineStart;
        // la loupe se centre sur la borne (le contenu suit), fuit le
        // doigt en hauteur (le doigt cache la ligne, jamais la loupe)
        float centerX = getPaddingLeft() + column * charWidth;
        float lineTop = getPaddingTop() + visibleIndexOf(line) * lineHeight - scrollY;
        float width = Math.min(getWidth() - getPaddingLeft() - getPaddingRight(),
                dp(160));
        float height = lineHeight * LOUPE_SCALE + dp(8);
        float left = clampToRange(centerX - width / 2.0f, getPaddingLeft(),
                Math.max(getPaddingLeft(), getWidth() - getPaddingRight() - width));
        float top = dragFingerY - dp(12) - height;
        if (top < getPaddingTop()) {
            top = Math.min(dragFingerY + dp(12),
                    Math.max(getPaddingTop(), getHeight() - getPaddingBottom() - height));
        }
        // le contenu : la ligne rendue UNE FOIS à l'échelle 1 (hors
        // trame), blittée à l'échelle — le relevé 16 : le pire du
        // geste de copie (16,55, ni moteur ni collecteur) venait des
        // glyphes rastérisés à l'échelle 2 à chaque trame ; le blit
        // ne paie plus que la texture, la première trame non plus
        ensureLoupeLine(line, lineStart, lineTop);
        float srcLeft = clampToRange(
                centerX - width / (2.0f * LOUPE_SCALE), getPaddingLeft(),
                Math.max(getPaddingLeft(),
                        getPaddingLeft() + loupeLine.getWidth() - width / LOUPE_SCALE));
        canvas.save();
        canvas.clipRect(left, top, left + width, top + height);
        // l'assiette : la couleur de fond du thème remplit la découpe
        canvas.drawColor(themeBackground);
        canvas.drawBitmap(loupeLine, srcLeft, 0.0f,
                srcLeft + width / LOUPE_SCALE, lineHeight,
                left, top + dp(4), left + width, top + height - dp(4), null);
        // la teinte de sélection par-dessus (directe : la région
        // source, à l'échelle)
        if (hasSelection()) {
            int selStart = Math.max(selectionStart(), lineStart);
            int selEnd = Math.min(selectionEnd(), buffer.getLineEnd(line));
            if (selEnd > selStart) {
                float tintLeft = left + (getPaddingLeft()
                        + (selStart - lineStart) * charWidth - srcLeft) * LOUPE_SCALE;
                float tintRight = left + (getPaddingLeft()
                        + (selEnd - lineStart) * charWidth - srcLeft) * LOUPE_SCALE;
                canvas.drawRect(tintLeft, top + dp(4), tintRight,
                        top + height - dp(4), selectionPaint);
            }
        }
        // la borne au centre (la loupe s'y centre par construction) :
        // un trait direct, pas de texte à rastériser
        float caretCenter = left + width / 2.0f;
        canvas.drawRect(caretCenter - caretWidth * LOUPE_SCALE, top + dp(4),
                caretCenter, top + height - dp(4), caretPaint);
        canvas.restore();
        canvas.drawRoundRect(left, top, left + width, top + height,
                dp(6), dp(6), loupeBorder);
    }

    /**
     * Le rendu de la ligne sous la loupe, à l'échelle 1, hors trame :
     * les jetons y dessinent une fois (le rendu logiciel), la trame
     * la blitte. Le rendu se cache par ligne et révision — le
     * traînement ne change ni l'un ni l'autre, le rendu ne se paie
     * qu'aux traversées de ligne.
     */
    private void ensureLoupeLine(int line, int lineStart, float lineTop) {
        if (loupeLine != null && loupeLineIndex == line
                && loupeLineRevision == buffer.getRevision()) {
            return;
        }
        int width = Math.max(1, getWidth() - getPaddingLeft() - getPaddingRight());
        int tall = Math.max(1, (int) lineHeight);
        loupeLine = android.graphics.Bitmap.createBitmap(width, tall,
                android.graphics.Bitmap.Config.ARGB_8888);
        Canvas render = new Canvas(loupeLine);
        // les coordonnées de la vue se ramènent au bitmap : la ligne
        // occupe [0, largeur] × [0, hauteur-de-ligne]
        render.translate(-getPaddingLeft(), -lineTop);
        if (buffer.getLineEnd(line) > lineStart) {
            drawTokens(render, line, lineStart, lineBaseline(line));
        }
        loupeLineIndex = line;
        loupeLineRevision = buffer.getRevision();
    }

    private void placeCaretAt(float x, float y) {
        int offset = offsetNearest(x, y);
        caretOffset = offset;
        // un tap pose le curseur : la sélection se range
        selectionAnchor = -1;
        invalidate();
    }

    /**
     * Le dépôt du traîné-déposé : le texte sélectionné se déplace au
     * point de dépôt, en une seule unité à défaire. Un dépôt dans la
     * sélection (ou sur elle) ne déplace rien : le texte reste où il
     * est, aucune unité ne naît.
     *
     * @param drop le décalage de dépôt, caractère le plus proche du
     *             doigt au lâcher
     */
    private void dropTextAtSelection(int drop) {
        int start = selectionStart();
        int end = selectionEnd();
        String moved = buffer.getText(start, end);
        if (drop < start || drop > end) {
            try (TextTransaction transaction = undoStack.newTransaction()) {
                transaction.replace(start, end, "");
                // un dépôt après la sélection : la suppression a
                // décalé la cible d'autant
                if (drop > end) {
                    drop -= moved.length();
                }
                transaction.replace(drop, drop, moved);
            }
            caretTo(Math.min(drop + moved.length(), buffer.length()));
            ensureCaretVisible();
        }
        // la sélection se range : le texte déposé s'achève au curseur
        selectionAnchor = -1;
        invalidate();
    }

    /**
     * Sélectionne le mot sous le doigt : les bornes du mot se lisent
     * dans la fenêtre de la ligne (jamais le document pour un mot).
     */
    private void selectWordAt(float x, float y) {
        int offset = offsetNearest(x, y);
        int line = buffer.lineOfOffset(Math.min(offset, buffer.length()));
        int lineStart = buffer.getLineStart(line);
        int lineEnd = buffer.getLineEnd(line);
        int wordStart = wordBoundsStart(offset, lineStart);
        int wordEnd = wordBoundsEnd(offset, lineEnd);
        // le curseur d'abord (le poser range la sélection), l'ancre
        // ensuite la rouvre
        selectionAnchor = -1;
        caretTo(offset);
        if (wordEnd > wordStart) {
            selectionAnchor = wordStart;
            caretOffset = wordEnd;
            showSymbolInfo(wordStart, wordEnd, line, offset - lineStart);
            invalidate();
        }
    }

    /**
     * La popup d'info du mot sélectionné : le fournisseur de l'hôte
     * répond le premier (un serveur de langage par exemple), l'index
     * de symboles décrit sinon (nom, nature, ligne de déclaration),
     * un mot ordinaire ne dit que lui-même — l'index peut être en
     * cours de construction, la popup dit alors le mot sans la nature.
     */
    private void showSymbolInfo(int wordStart, int wordEnd, int line, int column) {
        String word = buffer.getText(wordStart, wordEnd);
        if (infoProvider != null) {
            String provided = infoProvider.infoFor(line, column, word);
            if (provided != null) {
                showInfoPopup(provided, line, column);
                return;
            }
        }
        String text = "« " + word + " » — mot du texte";
        if (symbolIndex != null) {
            for (SymbolIndex.Symbol symbol : symbolIndex.symbols()) {
                if (symbol.name().equals(word)) {
                    text = "« " + word + " » — " + symbol.nature()
                            + ", déclaré ligne " + (symbol.line() + 1);
                    break;
                }
            }
        }
        showInfoPopup(text, line, column);
    }

    /** Le début du mot qui contient l'offset (ou l'offset lui-même). */
    private int wordBoundsStart(int offset, int lineStart) {
        int at = offset;
        while (at > lineStart
                && isWordChar(buffer.charAt(at - 1))) {
            at--;
        }
        return at;
    }

    /** La fin du mot qui contient l'offset (ou l'offset lui-même). */
    private int wordBoundsEnd(int offset, int lineEnd) {
        int at = offset;
        while (at < lineEnd
                && isWordChar(buffer.charAt(at))) {
            at++;
        }
        return at;
    }

    /** Un caractère de mot : identifiant Java ou lettre large (accentuée). */
    private static boolean isWordChar(char c) {
        return Character.isJavaIdentifierPart(c) || Character.isLetter(c);
    }

    /** Amène le curseur dans la fenêtre visible (frappe en bord bas) ; un pli qui le cache s'ouvre. */
    private void ensureCaretVisible() {
        revealCaret();
        int line = buffer.lineOfOffset(Math.min(caretOffset, buffer.length()));
        float top = getPaddingTop() + visibleIndexOf(line) * lineHeight - scrollY;
        float bottom = top + lineHeight;
        if (top < 0.0f) {
            scrollY += top;
        } else if (bottom > getHeight()) {
            scrollY += bottom - getHeight();
        }
        scrollY = clampToRange(scrollY, 0.0f, maxScroll());
    }

    /** Première ligne visible (indice — les plis cachent des lignes). */
    private int firstVisibleLine() {
        if (lineHeight <= 0.0f) {
            return 0;
        }
        return lineOfVisibleIndex((int) (scrollY / lineHeight));
    }

    /** Dernière ligne visible (indice, bornée au document). */
    private int lastVisibleLine() {
        if (lineHeight <= 0.0f) {
            return 0;
        }
        int index = (int) Math.ceil((scrollY + getHeight()) / lineHeight);
        return lineOfVisibleIndex(Math.min(index, visibleLineCount() - 1));
    }

    /** Borne haute du défilement : la dernière ligne visible en bas. */
    private float maxScroll() {
        float content = getPaddingTop() + visibleLineCount() * lineHeight
                + getPaddingBottom();
        return Math.max(0.0f, content - getHeight());
    }

    /** Seuil de déplacement avant qu'un contact devienne un défilement. */
    private float touchSlop() {
        return dp(8);
    }

    /** Relance le fil de fond si du travail reste (dessin, édition, ouverture). */
    private void ensureFill() {
        backgroundFill.kick();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (backgroundFill != null && backgroundFill.stopped()) {
            backgroundFill = BackgroundFill.start(syntax);
        }
        ensureFill();
    }

    @Override
    protected void onDetachedFromWindow() {
        // annulation douce : la tranche en cours finit, le fil s'arrête
        backgroundFill.stop();
        super.onDetachedFromWindow();
    }

    /**
     * Attend le remplissage complet (harnais, tests) : le fil de fond
     * ne laisse jamais le document incohérent, l'attente n'observe.
     *
     * @param timeoutMillis borne d'attente en millisecondes
     * @return vrai si le préfixe est complet au retour
     */
    public boolean awaitBackgroundFill(long timeoutMillis) {
        return backgroundFill.awaitIdle(timeoutMillis);
    }

    private void applyEdit(int start, int end, CharSequence text) {
        try (TextTransaction transaction = undoStack.newTransaction()) {
            transaction.replace(start, end, text);
        }
        caretOffset = Math.min(start + text.length(), buffer.length());
        // toute édition directe réduit la sélection (le curseur porte
        // la fin, l'ancre ne survit pas à ce qu'elle désignait)
        selectionAnchor = -1;
        invalidate();
    }

    /**
     * L'ancre survit aux éditions qui ne viennent pas d'elle : décalée
     * après la zone remplacée, tenue avant, ramenée au début de la
     * zone si elle vivait dedans (une ancre intérieure ne désigne plus
     * rien de stable).
     *
     * @param start          début de la zone remplacée
     * @param removedLength  caractères retirés
     * @param insertedLength caractères insérés
     */
    private void shiftSelection(int start, int removedLength, int insertedLength) {
        if (selectionAnchor < 0) {
            return;
        }
        if (selectionAnchor <= start) {
            return;
        }
        if (selectionAnchor >= start + removedLength) {
            selectionAnchor += insertedLength - removedLength;
        } else {
            selectionAnchor = start;
        }
    }

    /**
     * Supprime autour du curseur (le chemin Suppr : la demande IME et
     * l'événement clavier y passent). Une sélection en cours passe
     * avant : c'est ELLE que Suppr efface, pas le voisin du curseur.
     *
     * @param leftLength  caractères à gauche du curseur
     * @param rightLength caractères à droite du curseur
     */
    void deleteAround(int leftLength, int rightLength) {
        clearComposing();
        if (hasSelection()) {
            applyEdit(selectionStart(), selectionEnd(), "");
            return;
        }
        int start = Math.max(0, caretOffset - leftLength);
        int end = Math.min(buffer.length(), caretOffset + rightLength);
        if (end > start) {
            applyEdit(start, end, "");
        }
    }

    /** Déplace le curseur d'un caractère (les flèches du clavier). */
    void moveCaret(int delta) {
        caretTo(clampToRange(caretOffset + delta, 0, buffer.length()));
    }

    /** Déplace le curseur d'une ligne, colonne conservée au plus près. */
    void moveCaretVertically(int lines) {
        int current = buffer.lineOfOffset(Math.min(caretOffset, buffer.length()));
        int column = caretOffset - buffer.getLineStart(current);
        int line = clampToRange(current + lines, 0, buffer.getLineCount() - 1);
        int lineStart = buffer.getLineStart(line);
        caretTo(lineStart + Math.min(column, buffer.getLineEnd(line) - lineStart));
    }

    private void clearComposing() {
        if (composingStart < 0) {
            return;
        }
        composingStart = -1;
        composingEnd = -1;
        invalidate();
    }

    private void clampCaret() {
        if (caretOffset > buffer.length()) {
            caretOffset = buffer.length();
        }
    }

    private void showSoftInput() {
        InputMethodManager manager =
                (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (manager != null) {
            manager.showSoftInput(this, 0);
        }
    }

    private static int clampToRange(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float clampToRange(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    /**
     * Connexion IME : chaque retour du clavier est une transaction, donc
     * une unité d'annulation. La zone de composition est remplacée à chaque
     * mise à jour de l'IME puis validée telle quelle au commit.
     */
    private static final class EditorInputConnection extends BaseInputConnection {

        private final EditorView editor;

        EditorInputConnection(EditorView target) {
            super(target, false);
            editor = target;
        }

        @Override
        public boolean commitText(CharSequence text, int newCursorPosition) {
            int start = editor.composingStart >= 0 ? editor.composingStart
                    : editor.hasSelection() ? editor.selectionStart() : editor.caretOffset;
            int end = editor.composingStart >= 0 ? editor.composingEnd
                    : editor.hasSelection() ? editor.selectionEnd() : editor.caretOffset;
            editor.applyEdit(start, end, text);
            editor.clearComposing();
            return true;
        }

        @Override
        public boolean setComposingText(CharSequence text, int newCursorPosition) {
            int start = editor.composingStart >= 0 ? editor.composingStart : editor.caretOffset;
            int end = editor.composingStart >= 0 ? editor.composingEnd : editor.caretOffset;
            editor.applyEdit(start, end, text);
            editor.composingStart = start;
            editor.composingEnd = Math.min(start + text.length(), editor.buffer.length());
            return true;
        }

        @Override
        public boolean deleteSurroundingText(int leftLength, int rightLength) {
            editor.deleteAround(leftLength, rightLength);
            return true;
        }

        @Override
        public boolean finishComposingText() {
            editor.clearComposing();
            return true;
        }

        /**
         * Les claviers qui passent par les événements (Suppr, Entrée,
         * flèches) : le mode factice de la classe de base les perdrait —
         * la connexion traduit, le clavier ne peut pas être muet.
         */
        @Override
        public boolean sendKeyEvent(KeyEvent event) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                switch (event.getKeyCode()) {
                    case KeyEvent.KEYCODE_DEL:
                        editor.deleteAround(1, 0);
                        return true;
                    case KeyEvent.KEYCODE_FORWARD_DEL:
                        editor.deleteAround(0, 1);
                        return true;
                    case KeyEvent.KEYCODE_ENTER:
                    case KeyEvent.KEYCODE_NUMPAD_ENTER:
                        editor.applyEdit(editor.caretOffset, editor.caretOffset, "\n");
                        return true;
                    case KeyEvent.KEYCODE_DPAD_LEFT:
                        editor.moveCaret(-1);
                        return true;
                    case KeyEvent.KEYCODE_DPAD_RIGHT:
                        editor.moveCaret(1);
                        return true;
                    case KeyEvent.KEYCODE_DPAD_UP:
                        editor.moveCaretVertically(-1);
                        return true;
                    case KeyEvent.KEYCODE_DPAD_DOWN:
                        editor.moveCaretVertically(1);
                        return true;
                    default:
                        break;
                }
            }
            return super.sendKeyEvent(event);
        }

        /**
         * La touche action du clavier (Entrée) : un saut de ligne —
         * l'éditeur ne déclare pas d'action spéciale.
         */
        @Override
        public boolean performEditorAction(int actionCode) {
            editor.applyEdit(editor.hasSelection()
                    ? editor.selectionStart() : editor.caretOffset,
                    editor.hasSelection() ? editor.selectionEnd() : editor.caretOffset,
                    "\n");
            return true;
        }

        /**
         * Le clavier déplace la sélection (le champ de recherche du
         * clavier, la poignée du texte sélectionné) : l'ancre et le
         * curseur suivent, la sélection vient.
         */
        @Override
        public boolean setSelection(int start, int end) {
            editor.setSelection(start, end);
            return true;
        }

        /**
         * Le texte sélectionné (le copier du clavier le demande).
         */
        @Override
        public CharSequence getSelectedText(int flags) {
            return editor.hasSelection() ? editor.selectedText() : null;
        }
    }
}
