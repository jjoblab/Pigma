package jo.pigma.editor;

import jo.pigma.editor.folding.FoldModel;
import jo.pigma.editor.gutter.Gutter;
import jo.pigma.editor.input.EditorContract;
import jo.pigma.editor.input.EditorInputConnection;
import jo.pigma.editor.selection.SelectionRenderer;
import jo.pigma.editor.selection.SelectionState;


import jo.pigma.editor.popups.KeyboardCard;
import jo.pigma.editor.popups.GotoBar;
import jo.pigma.editor.popups.HistoryCard;
import jo.pigma.editor.popups.InfoPopup;
import jo.pigma.editor.popups.LocalHistory;
import jo.pigma.editor.popups.DiagnosticCard;
import jo.pigma.editor.popups.PopupStyle;
import jo.pigma.editor.popups.QuickActionCard;
import jo.pigma.editor.popups.RenameCard;
import jo.pigma.editor.render.RenderCache;
import jo.pigma.editor.popups.PopupMetrics;
import jo.pigma.editor.popups.SearchBar;
import jo.pigma.editor.popups.CompletionCard;
import jo.pigma.editor.popups.OverflowCard;
import jo.pigma.editor.popups.SelectionBar;
import jo.pigma.editor.popups.SignatureCard;
import jo.pigma.lsp.LspRange;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;

import java.util.ArrayList;
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
    private Paint.FontMetrics fontMetrics;
    private float charWidth;
    private float lineHeight;
    private float caretWidth;

    /** Le corps du texte en pixels (le pincement y accède). */
    private float textPx;

    /** La borne basse du pincement en sp. */
    private float minTextSp = 8.0f;

    /** La borne haute du pincement en sp. */
    private float maxTextSp = 40.0f;

    /** Le pincement vit (deux doigts posés). */
    private boolean pinching;

    /** Un pincement a eu lieu dans ce geste (l'appui final ne vaut pas un tap). */
    private boolean pinchedThisGesture;

    /** L'écart des deux doigts au départ du pincement. */
    private float pinchStartSpan;

    /** Le corps du texte au départ du pincement. */
    private float pinchStartPx;

    /** L'ordonnée du foyer du pincement (le milieu des deux doigts au départ). */
    private float pinchFocusY;

    private SyntaxDocument syntax;
    private LanguageSyntax language;
    private SyntaxTheme theme = EditorThemes.LIGHT;
    private int themeBackground = EditorThemes.LIGHT_BACKGROUND;

    private int composingStart = -1;
    private int composingEnd = -1;

    /** L'état de sélection — le curseur et l'ancre au paquet selection/. */
    private final SelectionState selection = new SelectionState();

    /** La requête de recherche courante (null : pas de recherche). */
    private String searchQuery;

    /** Les débuts des correspondances, triés croissants. */
    private int[] searchMatches = new int[0];

    /** L'index de la correspondance courante (-1 : aucune). */
    private int searchCurrent = -1;

    /** La popup d'info ancrée — état et dessin au paquet des popups. */
    private InfoPopup infoPopup;

    /** La carte de recherche — dessinée dans la vue, jamais de fenêtre flottante. */
    private SearchBar searchBar;

    /** La carte « aller à la ligne » — dessinée dans la vue. */
    private GotoBar gotoBar;

    /**
     * La carte « renommer le symbole », dessinée dans la vue, posée
     * au caret, le clavier délégué — le nom neuf s'y écrit, la
     * validation y part au porteur du renommage (la façade du serveur
     * de langage s'y branche).
     */
    private RenameCard renameBar;

    /**
     * Les plages surlignées par le serveur de langage : les
     * occurrences du symbole au curseur (le surlignage du protocole,
     * la teinte s'y distingue de la recherche).
     */
    private List<LspRange> serverHighlights = List.of();

    /** Le doigt est-il tombé sur une carte ouverte ? — le geste entier lui revient. */
    private boolean downOnCard;

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

    /** L'abscisse du doigt pendant le traîné (l'aperçu flottant la suit). */
    private float dragFingerX;

    /**
     * Le défilement au bord du traîné-déposé est-il programmé ? (une
     * échéance à la fois — le doigt tient la zone, la suite se
     * reprogramme).
     */
    private boolean edgeScrollScheduled;

    /** Une échéance du défilement au bord du traîné-déposé. */
    private final Runnable edgeScroll = this::runEdgeScroll;

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

    /** La carte de complétion au caret — état et dessin au paquet des popups. */
    private CompletionCard completionCard;

    /** La carte de signature au caret — l'aide aux paramètres d'un appel. */
    private SignatureCard signatureCard;

    /** La barre de sélection — les actions de la sélection en pastilles. */
    private SelectionBar selectionBar;

    /** La carte de débordement de la barre — les étages d'actions repliées. */
    private OverflowCard overflowCard;

    /** L'historique local — chaque écriture y laisse sa trace. */
    private LocalHistory localHistory;

    /** La carte de l'historique local — l'histoire d'une plage de lignes. */
    private HistoryCard historyCard;

    /** La carte de diagnostic — les erreurs et avertissements d'une ligne. */
    private DiagnosticCard diagnosticCard;

    /** La carte d'actions rapides — l'ampoule de la gouttière. */
    private QuickActionCard quickActionCard;

    /** Le style des popups posé par l'hôte (fond translucide, tailles, forme). */
    private PopupStyle popupStyle = PopupStyle.defaults();

    /** Le fournisseur de diagnostics de l'hôte (le LSP y monte ses erreurs). */
    private DiagnosticProvider diagnosticProvider;

    /** Le fournisseur d'actions rapides de l'hôte (l'ampoule les porte). */
    private QuickActionProvider quickActionProvider;

    /** Le fournisseur d'actions rapides du serveur de langage (la façade y pose les fonctions du protocole). */
    private QuickActionProvider serverQuickActionProvider;

    /** Les actions rapides vivantes de la ligne du curseur (l'ampoule en vit). */
    private List<QuickActionCard.Entry> quickActions = List.of();

    /** La ligne des actions rapides courantes (-1 : jamais calculées). */
    private int quickActionsLine = -1;

    /** La révision des actions rapides courantes. */
    private int quickActionsRevision = -1;

    /** Le caret des actions rapides courantes. */
    private int quickActionsCaret = -1;

    /** Les actions de l'hôte sur la barre de sélection (posées par l'app). */
    private final List<SelectionBar.HostAction> hostSelectionActions =
            new ArrayList<>();

    /**
     * L'étiquette du geste en cours (l'historique local la porte sur sa
     * trace : collage, complétion, remplacement… null : le geste par
     * défaut se calcule de la transaction).
     */
    private String pendingEditLabel;

    /** La trame de la dernière sonde du presse-papiers (la sonde s'amortit). */
    private long clipboardProbeFrame = -1;

    /** Le presse-papiers portait-il du texte à la dernière sonde ? */
    private boolean clipboardHasText;

    /**
     * Le plafond des copies au presse-papiers, en caractères : la
     * transaction de la plateforme plafonne à un méga-octet — au-delà,
     * le système tue l'appelant. La copie tronque au plafond.
     */
    private static final int MAX_CLIPBOARD_CHARS = 200_000;

    /** La gouttière — les numéros de ligne, au paquet du même nom. */
    private Gutter gutter;

    /** La borne de début du mot courant (l'insertion remplace le préfixe). */
    private int suggestionWordStart = -1;

    /** La paire d'appariement du curseur (L2), null si rien n'adjace. */
    private StructureDocument.BracketPair bracketPair;

    /** La portée du curseur (L2), null au niveau racine. */
    private StructureDocument.BlockRange scopeRange;

    private int pairCaret = -1;

    private int pairRevision = -1;

    /** Les plis — l'état et ses caches au paquet folding/. */
    private final FoldModel folds = new FoldModel();

    /** Le rendu de la sélection — pinceau et dessin au paquet selection/. */
    private final SelectionRenderer selectionRenderer = new SelectionRenderer();

    /** Le texte vu du paquet selection (contrat interne, hors API). */
    private final SelectionRenderer.Lines selectionLines =
            new SelectionRenderer.Lines() {
                @Override
                public int lineStart(int line) {
                    return buffer.getLineStart(line);
                }

                @Override
                public int lineEnd(int line) {
                    return buffer.getLineEnd(line);
                }

                @Override
                public boolean isHidden(int line) {
                    return folds.isHidden(line);
                }

                @Override
                public int visibleIndexOf(int line) {
                    return EditorView.this.visibleIndexOf(line);
                }
            };

    /** Le pont du clavier (contrat interne du paquet input, hors API). */
    private EditorContract inputContract;

    /** Nombre de lignes au dernier avis du tampon (décale les plis à l'édition). */
    private int trackedLineCount;

    /** Le surlignage des deux bouts d'une paire (le trait du signe). */
    private final Paint bracketPaint = new Paint();

    /** Les guides d'indentation de L2 (un par niveau de profondeur). */
    private final Paint guidePaint = new Paint();

    /** La bande de fond de la portée du curseur. */
    private final Paint scopePaint = new Paint();


    /** Le corps d'une poignée de sélection (le cercle sous la borne). */
    private final Paint handlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    /** Le cœur blanc d'une poignée (le point de visibilité). */
    private final Paint handleCorePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    /** Le cadre arrondi de la loupe de précision. */
    private final Paint loupeBorder = new Paint(Paint.ANTI_ALIAS_FLAG);

    /**
     * L'ordonnée du doigt pendant le traînement (la loupe la fuit —
     * poignée comme traîné-déposé).
     */
    private float dragFingerY;

    /** La teinte des correspondances de recherche. */
    private final Paint searchPaint = new Paint();

    /** La teinte de la correspondance courante (plus dense). */
    private final Paint searchCurrentPaint = new Paint();

    /**
     * La teinte des occurrences du serveur de langage (le surlignage
     * du protocole — bleu, la teinte de la recherche est ambre : les
     * deux se lisent d'un coup d'œil).
     */
    private final Paint serverHighlightPaint = new Paint();

    /** Le souligné des diagnostics d'appariement. */
    private final Paint diagnosticPaint = new Paint();

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
            recordLocalTrace(start, removedLength, insertedLength);
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
        applyTextSize(14.0f * getResources().getDisplayMetrics().scaledDensity);
        caretPaint.setColor(CARET_COLOR);
        composePaint.setColor(COMPOSE_COLOR);
        bracketPaint.setStyle(Paint.Style.STROKE);
        bracketPaint.setStrokeWidth(dp(1) * 1.5f);
        bracketPaint.setColor(CARET_COLOR);
        guidePaint.setStrokeWidth(dp(1));
        guidePaint.setColor(0x33808080);
        scopePaint.setStyle(Paint.Style.FILL);
        scopePaint.setColor(0x0D2962FF);
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
        serverHighlightPaint.setStyle(Paint.Style.FILL);
        serverHighlightPaint.setColor(0x332979FF);
        infoPopup = new InfoPopup(dp(1));
        diagnosticPaint.setStyle(Paint.Style.STROKE);
        diagnosticPaint.setStrokeWidth(dp(2));
        diagnosticPaint.setColor(0xFFFF3B30);
        completionCard = new CompletionCard(dp(1));
        completionCard.bind(this);
        signatureCard = new SignatureCard(dp(1));
        selectionBar = new SelectionBar(dp(1));
        selectionBar.bind(this);
        overflowCard = new OverflowCard(dp(1));
        localHistory = new LocalHistory();
        historyCard = new HistoryCard(dp(1));
        historyCard.bind(this);
        diagnosticCard = new DiagnosticCard(dp(1));
        quickActionCard = new QuickActionCard(dp(1));
        gutter = new Gutter(dp(1));
        inputContract = createInputContract();
        trackedLineCount = buffer.getLineCount();
        buffer.addListener(modelListener);
        int padding = dp(12);
        // la gouttière élargie : la colonne des marqueurs (erreur,
        // avertissement, ampoule) vit AVANT les numéros — le pli
        // demeure au bord du texte
        setPadding(dp(64), dp(16), padding, dp(16));
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
     * Le corps du texte en unités sp (le pincement le fait vivre —
     * l'hôte le lit pour sa ligne d'état).
     *
     * @return le corps courant en sp
     */
    public float getTextSizeSp() {
        return textPx / getResources().getDisplayMetrics().scaledDensity;
    }

    /**
     * Pose le corps du texte en unités sp : la métrique se recalcule
     * (la hauteur de ligne, la chasse), les plans de rendu se rangent
     * (les abscisses y sont cuites), le défilement se borne — la ligne
     * au milieu de l'écran y demeure (le zoom ne saute pas la lecture).
     *
     * @param sizeSp le corps voulu en sp (les bornes le serront)
     */
    public void setTextSizeSp(float sizeSp) {
        float scaledDensity = getResources().getDisplayMetrics().scaledDensity;
        float centerLine = (getHeight() / 2.0f - getPaddingTop() + scrollY) / lineHeight;
        applyTextSize(sizeSp * scaledDensity);
        scrollY = clampToRange(centerLine * lineHeight - (getHeight() / 2.0f - getPaddingTop()),
                0.0f, maxScroll());
        invalidate();
    }

    /**
     * Les bornes du pincement en sp (l'hôte resserre ou les lâche —
     * le corps courant s'y recale s'il en sort).
     *
     * @param minSp la borne basse en sp
     * @param maxSp la borne haute en sp
     */
    public void setZoomBoundsSp(float minSp, float maxSp) {
        float lo = Math.min(minSp, maxSp);
        float hi = Math.max(minSp, maxSp);
        minTextSp = Math.max(4.0f, lo);
        maxTextSp = Math.max(minTextSp, hi);
        applyTextSize(textPx);
    }

    /**
     * La borne basse du pincement en sp.
     *
     * @return la borne basse
     */
    public float minZoomSp() {
        return minTextSp;
    }

    /**
     * La borne haute du pincement en sp.
     *
     * @return la borne haute
     */
    public float maxZoomSp() {
        return maxTextSp;
    }

    /**
     * Le corps se recalcule : la peinture le porte, les métriques se
     * relisent (la chasse du monospace, la hauteur de ligne, le filet
     * du curseur), les plans de rendu se rangent (les abscisses y sont
     * cuites à la chasse d'avant), le défilement se borne. Le corps
     * vit borné — le pincement n'en sort jamais, l'hôte non plus.
     *
     * @param freshPx le corps voulu en pixels
     */
    private void applyTextSize(float freshPx) {
        float scaledDensity = getResources().getDisplayMetrics().scaledDensity;
        float clamped = clampToRange(freshPx, minTextSp * scaledDensity, maxTextSp * scaledDensity);
        if (clamped == textPx) {
            return;
        }
        textPx = clamped;
        textPaint.setTextSize(clamped);
        fontMetrics = textPaint.getFontMetrics();
        charWidth = textPaint.measureText("M");
        lineHeight = (float) Math.ceil(fontMetrics.bottom - fontMetrics.top) + dp(2);
        caretWidth = dp(2);
        renderCache.invalidate();
        scrollY = clampToRange(scrollY, 0.0f, maxScroll());
        invalidate();
    }

    /**
     * Le pincement s'engage : tout geste en cours se range (la
     * poignée se lâche, la sélection reste posée, le traîné se
     * range), les doigts deviennent le zoom — l'écart des deux doigts
     * portera le facteur, leur milieu le foyer.
     *
     * @param event l'événement du doigt qui s'ajoute
     */
    private void engagePinch(MotionEvent event) {
        requestFocus();
        pinching = true;
        pinchedThisGesture = true;
        dragging = false;
        selectionDragBound = 0;
        dragFixedBound = -1;
        textDragging = false;
        textDragArmed = false;
        dropOffset = -1;
        dismissInfoPopup();
        pinchStartSpan = pinchSpan(event);
        pinchStartPx = textPx;
        pinchFocusY = (event.getY(0) + event.getY(1)) / 2.0f;
    }

    /**
     * Le pincement vit : l'écart des doigts porte le facteur (le
     * corps du départ l'attend), la ligne sous le foyer du départ
     * demeure sous le foyer courant — le texte ne fuit pas sous les
     * doigts, il y reste.
     *
     * @param event l'événement des doigts qui bougent
     */
    private void pinchMove(MotionEvent event) {
        float span = pinchSpan(event);
        if (span <= 0.0f || pinchStartSpan <= 0.0f) {
            return;
        }
        float focusY = (event.getY(0) + event.getY(1)) / 2.0f;
        float lineAtFocus = (pinchFocusY - getPaddingTop() + scrollY) / lineHeight;
        applyTextSize(pinchStartPx * (span / pinchStartSpan));
        scrollY = clampToRange(lineAtFocus * lineHeight - (focusY - getPaddingTop()),
                0.0f, maxScroll());
        invalidate();
    }

    /**
     * Le pincement se dénoue : un doigt s'en va, l'autre peut
     * glisser — son appui repart de sa position vivante (le
     * défilement n'y saute pas).
     *
     * @param event l'événement du doigt qui s'en va
     */
    private void endPinch(MotionEvent event) {
        pinching = false;
        int staying = 1 - event.getActionIndex();
        if (staying >= 0 && staying < event.getPointerCount()) {
            touchStartX = event.getX(staying);
            touchStartY = event.getY(staying);
        }
        dragging = false;
        lastTapNanos = 0L;
        invalidate();
    }

    /**
     * L'écart des deux premiers doigts de l'événement.
     *
     * @param event l'événement à deux doigts
     * @return la distance en pixels (0 : un seul doigt)
     */
    private static float pinchSpan(MotionEvent event) {
        if (event.getPointerCount() < 2) {
            return 0.0f;
        }
        float dx = event.getX(0) - event.getX(1);
        float dy = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(dx * dx + dy * dy);
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
        return buffer.lineOfOffset(Math.min(selection.caret(), buffer.length()));
    }

    /**
     * Pose le curseur à un offset (borné au text) : l'hôte place le
     * curseur par programme, la paire d'appariement s'y recalcule au
     * dessin suivant. La sélection se range — poser le curseur, c'est
     * quitter la sélection.
     *
     * @param offset la position visée
     */
    public void caretTo(int offset) {
        selection.setCaret(Math.max(0, Math.min(offset, buffer.length())));
        selection.setAnchor(-1);
        ensureCaretVisible();
        invalidate();
    }

    /**
     * Branche l'écouteur des mouvements du curseur et de l'ancre : la
     * façade du serveur de langage y suit le curseur (le surlignage
     * des occurrences s'y redemande — la leçon de la référence : les
     * fonctions du protocole vivent sur la vue, l'hôte n'y touche
     * pas). Un seul écouteur : le porteur y marshalle s'il veut.
     *
     * @param listener l'écouteur, null pour le ranger
     */
    public void setSelectionListener(SelectionState.Listener listener) {
        selection.setListener(listener);
    }

    /**
     * Pose le STYLE DES POPUPS de l'hôte : le fond translucide, les
     * encres, la taille des textes, la hauteur des rangées, la
     * largeur et la forme des cartes — toutes les cartes et la
     * gouttière s'y reconfigurent, la trame suivante les porte.
     *
     * @param fresh le style posé, null pour celui par défaut
     */
    public void setPopupStyle(PopupStyle fresh) {
        popupStyle = fresh == null ? PopupStyle.defaults() : fresh;
        infoPopup.setStyle(popupStyle);
        completionCard.setStyle(popupStyle);
        signatureCard.setStyle(popupStyle);
        selectionBar.setStyle(popupStyle);
        overflowCard.setStyle(popupStyle);
        historyCard.setStyle(popupStyle);
        diagnosticCard.setStyle(popupStyle);
        quickActionCard.setStyle(popupStyle);
        gutter.setStyle(popupStyle);
        if (renameBar != null) {
            renameBar.setStyle(popupStyle);
        }
        if (searchBar != null) {
            searchBar.setStyle(popupStyle);
        }
        if (gotoBar != null) {
            gotoBar.setStyle(popupStyle);
        }
        invalidate();
    }

    /** Le style des popups courant (le harnais le lit). */
    public PopupStyle popupStyle() {
        return popupStyle;
    }

    /**
     * La carte de diagnostic (les erreurs et avertissements d'une
     * ligne — le harnais la lit).
     *
     * @return la carte, jamais null
     */
    public DiagnosticCard diagnosticCard() {
        return diagnosticCard;
    }

    /**
     * La carte d'actions rapides (l'ampoule de la gouttière — le
     * harnais la lit).
     *
     * @return la carte, jamais null
     */
    public QuickActionCard quickActionCard() {
        return quickActionCard;
    }

    /**
     * Le fournisseur de diagnostics de l'hôte : l'application
     * porteuse (le serveur de langage) y monte les erreurs et les
     * avertissements d'une ligne — la carte de diagnostic les lit au
     * toucher, le marqueur de la gouttière en vit.
     *
     * @param provider le fournisseur, null pour le ranger
     */
    public void setDiagnosticProvider(DiagnosticProvider provider) {
        diagnosticProvider = provider;
        invalidate();
    }

    /**
     * Le fournisseur d'actions rapides de l'hôte : l'application
     * porteuse y pose ses intentions — l'ampoule de la gouttière
     * les porte avec celles de l'éditeur.
     *
     * @param provider le fournisseur, null pour le ranger
     */
    public void setQuickActionProvider(QuickActionProvider provider) {
        quickActionProvider = provider;
        quickActionsRevision = -1;
        invalidate();
    }

    /**
     * Le fournisseur d'actions rapides du SERVEUR DE LANGAGE : la
     * façade du serveur y pose les fonctions du protocole (l'aller-à
     * la définition, les références, le renommage, le formatage, les
     * corrections rapides) — l'ampoule les porte APRÈS celles de
     * l'hôte : les intentions de l'éditeur et de l'hôte se lisent
     * d'abord, les fonctions du serveur ensuite (la façade y veut,
     * l'hôte n'a RIEN à câbler — le second slot, pas la place de
     * l'hôte).
     *
     * @param provider le fournisseur du serveur, null pour le ranger
     */
    public void setServerQuickActionProvider(QuickActionProvider provider) {
        serverQuickActionProvider = provider;
        quickActionsRevision = -1;
        invalidate();
    }

    /**
     * Le fournisseur de diagnostics de l'hôte — les erreurs et les
     * avertissements d'une ligne, à la sévérité près.
     */
    public interface DiagnosticProvider {

        /**
         * Les diagnostics d'une ligne.
         *
         * @param line l'indice de la ligne
         * @return les diagnostics, jamais null (vide : aucun)
         */
        List<DiagnosticCard.Entry> diagnosticsAt(int line);
    }

    /**
     * Le fournisseur d'actions rapides de l'hôte — ses intentions à
     * une ligne.
     */
    public interface QuickActionProvider {

        /**
         * Les actions rapides d'une ligne.
         *
         * @param line   l'indice de la ligne
         * @param column la colonne du curseur sur la ligne
         * @return les actions, jamais null (vide : aucune)
         */
        List<QuickActionCard.Entry> quickActionsAt(int line, int column);
    }

    /**
     * Les diagnostics d'une ligne : ceux de l'hôte d'abord (le
     * serveur de langage y dit ses erreurs et ses avertissements),
     * les signes mésappariés de l'appariement ensuite (toujours des
     * erreurs) — le harnais les lit.
     *
     * @param line l'indice de la ligne
     * @return les diagnostics, jamais null
     */
    public List<DiagnosticCard.Entry> diagnosticsOfLine(int line) {
        List<DiagnosticCard.Entry> found = new ArrayList<>();
        if (diagnosticProvider != null) {
            List<DiagnosticCard.Entry> hosted = diagnosticProvider.diagnosticsAt(line);
            if (hosted != null) {
                found.addAll(hosted);
            }
        }
        if (diagnostics != null && analysisRevision == buffer.getRevision()) {
            int lineStart = buffer.getLineStart(line);
            int lineEnd = buffer.getLineEnd(line);
            for (DiagnosticsDocument.Diagnostic local
                    : diagnostics.betweenLines(line, line)) {
                if (local.start() >= lineStart && local.start() < lineEnd) {
                    found.add(new DiagnosticCard.Entry(
                            DiagnosticCard.SEVERITY_ERROR, local.message()));
                }
            }
        }
        return found;
    }

    /**
     * La ligne porte-t-elle un diagnostic ? (le marqueur de la
     * gouttière en vit — le harnais le vérifie.)
     *
     * @param line l'indice de la ligne
     * @return vrai quand la ligne porte erreur ou avertissement
     */
    public boolean lineHasDiagnostics(int line) {
        return !diagnosticsOfLine(line).isEmpty();
    }

    /**
     * Les actions rapides d'une ligne : celles de l'éditeur (le
     * commentaire au préfixe du langage, la casse d'une sélection,
     * dupliquer, supprimer la ligne) et celles de l'hôte — le
     * harnais les lit.
     *
     * @param line l'indice de la ligne
     * @return les actions, jamais null
     */
    public List<QuickActionCard.Entry> quickActionsOfLine(int line) {
        List<QuickActionCard.Entry> actions = new ArrayList<>();
        if (!lineCommentPrefix().isEmpty()) {
            actions.add(new QuickActionCard.Entry(
                    "Commenter / décommenter", this::toggleLineComment));
        }
        if (hasSelection()) {
            actions.add(new QuickActionCard.Entry(
                    "En majuscules", this::uppercaseSelection));
            actions.add(new QuickActionCard.Entry(
                    "En minuscules", this::lowercaseSelection));
        }
        actions.add(new QuickActionCard.Entry(
                "Dupliquer la ligne", this::duplicateLine));
        actions.add(new QuickActionCard.Entry(
                "Supprimer la ligne", this::deleteLines));
        if (quickActionProvider != null) {
            int caretOffset = Math.min(selection.caret(), buffer.length());
            int column = caretOffset - buffer.getLineStart(line);
            List<QuickActionCard.Entry> hosted =
                    quickActionProvider.quickActionsAt(line, column);
            if (hosted != null) {
                actions.addAll(hosted);
            }
        }
        // les fonctions du serveur de langage se lisent APRÈS les
        // intentions de l'hôte (la façade y pose l'aller-à la
        // définition, les références, le renommage, le formatage, les
        // corrections rapides du protocole)
        if (serverQuickActionProvider != null) {
            int caretOffset = Math.min(selection.caret(), buffer.length());
            int column = caretOffset - buffer.getLineStart(line);
            List<QuickActionCard.Entry> served =
                    serverQuickActionProvider.quickActionsAt(line, column);
            if (served != null) {
                actions.addAll(served);
            }
        }
        return actions;
    }

    /**
     * Ouvre la carte de diagnostic d'une ligne — le doigt sur le
     * marqueur de la gouttière ou sur le souligné rouge y va, l'hôte
     * y va aussi ; la ligne sans diagnostic ne l'ouvre pas.
     *
     * @param line l'indice de la ligne
     * @return vrai quand la carte s'est ouverte
     */
    public boolean openDiagnosticsAtLine(int line) {
        if (line < 0 || line >= buffer.getLineCount()) {
            return false;
        }
        List<DiagnosticCard.Entry> found = diagnosticsOfLine(line);
        if (found.isEmpty()) {
            return false;
        }
        quickActionCard.close();
        diagnosticCard.open(line, found);
        invalidate();
        return true;
    }

    /**
     * Ouvre la carte d'actions rapides de la ligne — l'ampoule de la
     * gouttière y va, l'hôte y va aussi.
     *
     * @param line l'indice de la ligne
     * @return vrai quand la carte s'est ouverte
     */
    public boolean openQuickActionsAtLine(int line) {
        if (line < 0 || line >= buffer.getLineCount()) {
            return false;
        }
        List<QuickActionCard.Entry> actions = quickActionsOfLine(line);
        if (actions.isEmpty()) {
            return false;
        }
        diagnosticCard.close();
        quickActionCard.open(line, actions);
        invalidate();
        return true;
    }

    /**
     * Les actions rapides vivantes de la ligne du curseur (l'ampoule
     * en vit — la trame les recalcule au caret et à la révision).
     */
    private void updateQuickActions() {
        int caretOffset = Math.min(selection.caret(), buffer.length());
        int line = buffer.lineOfOffset(caretOffset);
        if (line == quickActionsLine && buffer.getRevision() == quickActionsRevision
                && caretOffset == quickActionsCaret) {
            return;
        }
        quickActionsLine = line;
        quickActionsRevision = buffer.getRevision();
        quickActionsCaret = caretOffset;
        quickActions = quickActionsOfLine(line);
        if (quickActionCard.isShown() && quickActionCard.line() != line) {
            quickActionCard.close();
        }
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
        completionCard.set(List.of(), "");
        suggestionWordStart = -1;
        signatureCard.set(null, 0);
        selectionBar.hide();
        overflowCard.close();
        historyCard.close();
        diagnosticCard.close();
        quickActionCard.close();
        quickActionsRevision = -1;
        quickActions = List.of();
        // autre document : l'histoire locale du précédent n'y désigne
        // plus rien (les lignes d'avant parlent d'un autre texte)
        localHistory.clear();
        // autre document : la sélection n'y désigne plus rien, la
        // recherche et la popup d'info non plus
        selection.setAnchor(-1);
        searchQuery = null;
        searchMatches = new int[0];
        searchCurrent = -1;
        infoPopup.dismiss();
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
        folds.clear();
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
        selection.setAnchor(-1);
        selectionDragBound = 0;
        searchQuery = null;
        searchMatches = new int[0];
        searchCurrent = -1;
        infoPopup.dismiss();
        undoStack.clear();
        folds.clear();
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
        int luminance = ((newBackground >>> 16) & 0xFF) * 30
                + ((newBackground >>> 8) & 0xFF) * 59
                + (newBackground & 0xFF) * 11;
        gutter.setTextColor(luminance >= 12800 ? 0xFF8A8A8E : 0xFF8E8E93);
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
     * Annule la dernière unité (le bouton de l'hôte y passe : le geste
     * porte son étiquette à l'historique local).
     */
    public void undo() {
        pendingEditLabel = "annulation";
        undoStack.undo();
    }

    /**
     * Rétablit la dernière unité annulée (le bouton de l'hôte y passe :
     * le geste porte son étiquette à l'historique local).
     */
    public void redo() {
        pendingEditLabel = "rétablissement";
        undoStack.redo();
    }

    /**
     * Offset du curseur, en unités de code UTF-16.
     *
     * @return la position du curseur dans le texte
     */
    public int getCaretOffset() {
        return selection.caret();
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
        return selection.hasSelection();
    }

    /**
     * Le début de la sélection, en unités de code UTF-16.
     *
     * @return la plus petite des deux bornes, ou la position du
     *         curseur sans sélection
     */
    public int selectionStart() {
        return selection.start();
    }

    /**
     * La fin de la sélection, en unités de code UTF-16.
     *
     * @return la plus grande des deux bornes, ou la position du
     *         curseur sans sélection
     */
    public int selectionEnd() {
        return selection.end();
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
        selection.setAnchor(-1);
        caretTo(to);
        selection.setAnchor(from == to ? -1 : from);
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
                if (searchMatches[m] >= selection.caret()) {
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
        pendingEditLabel = "remplacement";
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
        pendingEditLabel = "remplacement global";
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
        selection.setAnchor(-1);
        selection.setCaret(Math.min(firstStart + replacement.length(), buffer.length()));
        search(searchQuery);
        return count;
    }

    /**
     * Pose les remplacements du serveur de langage (la correction
     * rapide, le renommage, le formatage) : chaque plage LSP se
     * remplace par son texte neuf, TOUT en une transaction — une
     * seule unité d'annulation (la façon d'Android Studio : un
     * renommage s'annule d'un geste). Les remplacements se posent du
     * DERNIER au PREMIER pour que les plages ne bougent pas (la règle
     * du protocole : les plages vivent dans le document d'avant).
     *
     * @param edits les remplacements du serveur, jamais null
     * @return vrai quand au moins un remplacement s'est posé
     */
    public boolean applyTextEdits(List<jo.pigma.lsp.LspTextEdit> edits) {
        if (edits == null || edits.isEmpty()) {
            return false;
        }
        boolean applied = false;
        try (TextTransaction transaction = undoStack.newTransaction()) {
            for (int edit = edits.size() - 1; edit >= 0; edit--) {
                jo.pigma.lsp.LspTextEdit change = edits.get(edit);
                int start = buffer.offsetOfPosition(
                        change.range().start().line(), change.range().start().character());
                int end = buffer.offsetOfPosition(
                        change.range().end().line(), change.range().end().character());
                if (start >= 0 && end >= start && end <= buffer.length()) {
                    transaction.replace(start, end, change.newText());
                    applied = true;
                }
            }
        }
        if (applied) {
            selection.setAnchor(-1);
            selection.setCaret(Math.min(selection.caret(), buffer.length()));
            invalidate();
        }
        return applied;
    }

    /**
     * Le mot au curseur (les bornes du mot se lisent dans la fenêtre
     * de la ligne) : le renommage du serveur le porte, la façade y
     * lit le symbole. Vide quand le curseur ne porte pas de mot.
     *
     * @return le mot au curseur, vide sinon
     */
    public String wordAtCaret() {
        int caret = Math.min(selection.caret(), buffer.length());
        int line = buffer.lineOfOffset(caret);
        int lineStart = buffer.getLineStart(line);
        int lineEnd = buffer.getLineEnd(line);
        int wordStart = wordBoundsStart(caret, lineStart);
        int wordEnd = wordBoundsEnd(caret, lineEnd);
        return wordEnd > wordStart ? buffer.getText(wordStart, wordEnd) : "";
    }

    /**
     * La carte de recherche, DESSINÉE DANS LA VUE — jamais de
     * fenêtre flottante, pas de jeton de fenêtre, pas de conflit de
     * focus avec l'IME. La carte ne prend
     * jamais le focus ; le clavier reste à l'éditeur, qui lui rend
     * les verdicts des touches (le contrat d'entrée route la frappe
     * vers la carte ouverte). Elle se pose AU CARET (dessous si elle
     * tient, dessus sinon, bornée aux murs) et le suit à chaque
     * dessin. Un nouvel appel la range aussi — le bouton
     * « Rechercher » de l'hôte fait bascule.
     */
    public void toggleSearchPopup() {
        if (searchBar != null && searchBar.isShown()) {
            dismissSearchPopup();
            return;
        }
        if (searchBar == null) {
            searchBar = new SearchBar(dp(1));
            searchBar.bind(this);
            searchBar.setStyle(popupStyle);
        }
        searchBar.show();
        // la carte naît clavier levé : le champ de requête attend une
        // frappe — le clavier logiciel se montre dès l'ouverture, sinon
        // le doigt qui vise le champ ne lève jamais rien (la carte ne
        // prend jamais le focus elle-même : le clavier reste à l'éditeur)
        openKeyboard();
        invalidate();
    }

    /**
     * Range la carte de recherche (la croix, la bascule) : la
     * requête se vide, la recherche se range avec.
     */
    public void dismissSearchPopup() {
        if (searchBar != null && searchBar.isShown()) {
            searchBar.hide();
            clearSearch();
            invalidate();
        }
    }

    /**
     * La carte « aller à la ligne », dessinée dans la vue,
     * posée au caret, le clavier délégué — les chiffres s'y posent, la
     * validation y va, la croix la range. La
     * même bascule, le même rangement que la recherche.
     */
    public void toggleGotoPopup() {
        if (gotoBar != null && gotoBar.isShown()) {
            dismissGotoPopup();
            return;
        }
        if (gotoBar == null) {
            gotoBar = new GotoBar(dp(1));
            gotoBar.bind(this);
            gotoBar.setStyle(popupStyle);
        }
        gotoBar.show();
        // la carte naît clavier levé : les chiffres attendent une
        // frappe — même règle que la recherche
        openKeyboard();
        invalidate();
    }

    /** Range la carte « aller à la ligne ». */
    public void dismissGotoPopup() {
        if (gotoBar != null && gotoBar.isShown()) {
            gotoBar.hide();
            invalidate();
        }
    }

    /**
     * Ouvre la carte « renommer le symbole » au caret : le nom neuf
     * démarre du symbole (la façon d'Android Studio — il se corrige,
     * il ne se retape pas), la validation part au porteur du
     * renommage (la façade du serveur de langage y branche le
     * protocole), la croix la range. Le clavier se lève : les lettres
     * attendent une frappe — même règle que la recherche.
     *
     * @param symbol le symbole à renommer
     * @param target le porteur du renommage
     */
    public void showRenamePopup(String symbol, RenameCard.RenameTarget target) {
        if (renameBar == null) {
            renameBar = new RenameCard(dp(1));
            renameBar.bind(this);
        }
        renameBar.setStyle(popupStyle);
        renameBar.show(symbol, target);
        openKeyboard();
        invalidate();
    }

    /** Range la carte « renommer le symbole ». */
    public void dismissRenamePopup() {
        if (renameBar != null && renameBar.isShown()) {
            renameBar.hide();
            invalidate();
        }
    }

    /** La carte « renommer le symbole », null si jamais ouverte (le harnais la lit). */
    public RenameCard renameBar() {
        return renameBar;
    }

    /** La carte de recherche, null si jamais ouverte (le harnais la lit). */
    public SearchBar searchBar() {
        return searchBar;
    }

    /** La carte « aller à la ligne », null si jamais ouverte (le harnais la lit). */
    public GotoBar gotoBar() {
        return gotoBar;
    }

    /**
     * La carte ouverte qui reçoit le clavier (la règle des popups
     * dessinées dans la vue) : la recherche, l'aller-à ou le
     * renommage montrés, aucune sinon.
     */
    private KeyboardCard openCard() {
        if (searchBar != null && searchBar.isShown()) {
            return searchBar;
        }
        if (gotoBar != null && gotoBar.isShown()) {
            return gotoBar;
        }
        if (renameBar != null && renameBar.isShown()) {
            return renameBar;
        }
        return null;
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
        infoPopup.show(null, text, line, column);
        invalidate();
    }

    /**
     * La popup d'info ancrée en documentation rapide : le TITRE (le
     * mot questionné, encre pleine et gras) au-dessus du CORPS (le
     * texte replié à la largeur, multi-lignes) — la réponse d'un
     * survol y passe, la description de l'index local aussi.
     *
     * @param title  le mot questionné, ou null pour aucun titre
     * @param body   le texte du corps, vide ou null pour masquer
     * @param line   la ligne d'ancrage
     * @param column la colonne d'ancrage
     */
    public void showInfoPopup(String title, String body, int line, int column) {
        infoPopup.show(title, body, line, column);
        invalidate();
    }

    /**
     * Remplace la popup d'info par une réponse TARDIVE — le survol
     * asynchrone d'un serveur lent : la question a montré la popup
     * locale (ou rien), la réponse du serveur y revient à son arrivée.
     * Le remplacement ne vaut qu'à la MÊME position : une popup
     * bougée ou rangée n'est pas évincée par une réponse périmée.
     *
     * @param text   la réponse tardive
     * @param line   la ligne d'ancrage de la question
     * @param column la colonne d'ancrage de la question
     * @return vrai si la popup vivait encore à cette position
     */
    public boolean replaceInfoPopup(String text, int line, int column) {
        if (!infoPopup.replace(text, line, column)) {
            return false;
        }
        invalidate();
        return true;
    }

    /** La ligne d'ancrage de la popup (-1 : aucune). */
    public int infoPopupLine() {
        return infoPopup.line();
    }

    /** La colonne d'ancrage de la popup (-1 : aucune). */
    public int infoPopupColumn() {
        return infoPopup.column();
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

    /**
     * Le fournisseur d'aide aux paramètres : l'hôte y branche une
     * source extérieure (un serveur de langage par exemple) — sans
     * fournisseur, l'index local des symboles porte la carte (le nom
     * de l'appel se cherche dans les déclarations, la ligne de la
     * déclaration devient la signature).
     */
    public interface SignatureProvider {

        /**
         * L'aide aux paramètres de l'appel au curseur.
         *
         * @param caretOffset le décalage du curseur dans le tampon
         * @return la signature et son paramètre actif, null pour
         *         ranger la carte
         */
        SignatureHelp signatureAt(int caretOffset);
    }

    /** La signature montrée : la ligne entière et le paramètre actif. */
    public record SignatureHelp(String label, int activeParameter) {
    }

    /**
     * Branche un fournisseur d'aide aux paramètres (null débranche :
     * l'index local reprend la carte).
     *
     * @param provider le fournisseur, ou null
     */
    public void setSignatureProvider(SignatureProvider provider) {
        signatureProvider = provider;
    }

    private SignatureProvider signatureProvider;

    /** La carte de signature (le harnais la lit). */
    public SignatureCard signatureCard() {
        return signatureCard;
    }

    /** Masque la popup d'info. */
    public void dismissInfoPopup() {
        if (infoPopup.text() != null) {
            infoPopup.dismiss();
            invalidate();
        }
    }

    /** La popup d'info est-elle visible ? */
    public boolean isInfoPopupVisible() {
        return infoPopup.text() != null;
    }

    /**
     * Le texte de la popup d'info (le harnais le vérifie, l'hôte peut
     * le journaliser).
     *
     * @return le texte affiché, null si masquée
     */
    public String infoPopupText() {
        return infoPopup.text();
    }

    /**
     * Le titre de la popup d'info (le mot questionné de la
     * documentation rapide — le harnais le vérifie).
     *
     * @return le titre affiché, null si aucun ou masquée
     */
    public String infoPopupTitle() {
        return infoPopup.title();
    }

    private void gotoSearchMatch(int index) {
        searchCurrent = index;
        int offset = searchMatches[index];
        selection.setAnchor(-1);
        selection.setCaret(Math.min(offset + searchQuery.length(), buffer.length()));
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
        return new EditorInputConnection(this, inputContract);
    }

    /**
     * Le pont du clavier vers l'éditeur : le contrat interne du paquet
     * input — la classe anonyme lit les membres privés de la vue, rien
     * ne se publie (l'API publique reste petite).
     */
    private EditorContract createInputContract() {
        return new EditorContract() {
            @Override
            public int caret() {
                return selection.caret();
            }

            @Override
            public int composingStart() {
                return composingStart;
            }

            @Override
            public int composingEnd() {
                return composingEnd;
            }

            @Override
            public void setComposing(int start, int end) {
                composingStart = start;
                composingEnd = end;
            }

            @Override
            public boolean hasSelection() {
                return EditorView.this.hasSelection();
            }

            @Override
            public int selectionStart() {
                return EditorView.this.selectionStart();
            }

            @Override
            public int selectionEnd() {
                return EditorView.this.selectionEnd();
            }

            @Override
            public CharSequence selectedText() {
                return EditorView.this.selectedText();
            }

            @Override
            public int length() {
                return buffer.length();
            }

            @Override
            public void applyEdit(int start, int end, CharSequence text) {
                // la règle des popups : la carte ouverte
                // reçoit le clavier — l'éditeur garde la connexion et
                // lui rend les verdicts des touches (la validation
                // Entrée, le texte tapé, la composition du clavier
                // logiciel qui remplace ce qu'elle avait posé). La
                // carte décline : la touche retourne à la vie normale
                // de l'éditeur.
                KeyboardCard carte = openCard();
                if (carte != null) {
                    if (text != null && text.length() == 1
                            && text.charAt(0) == '\n') {
                        if (carte.onEnter()) {
                            return;
                        }
                    } else if (composingStart >= 0
                            && start == composingStart && end == composingEnd
                            && carte.onComposition(text)) {
                        return;
                    } else if (carte.onText(text)) {
                        return;
                    }
                }
                // la carte de COMPLÉTION suit : elle ne prend jamais
                // le texte tapé (il poursuit vers le tampon — la
                // carte se re-filtre à la trame suivante), seule la
                // validation lui revient
                if (completionCard != null && text != null && text.length() == 1
                        && text.charAt(0) == '\n'
                        && completionCard.onEnter()) {
                    return;
                }
                EditorView.this.applyEdit(start, end, text);
            }

            @Override
            public void clearComposing() {
                EditorView.this.clearComposing();
            }

            @Override
            public void deleteAround(int left, int right) {
                // le retour arrière de la carte ouverte efface dans la
                // carte (le clavier lui est délégué)
                KeyboardCard carte = openCard();
                if (carte != null && left == 1 && right == 0
                        && carte.onBackspace()) {
                    return;
                }
                EditorView.this.deleteAround(left, right);
            }

            @Override
            public void moveCaret(int delta) {
                EditorView.this.moveCaret(delta);
            }

            @Override
            public void moveCaretVertically(int delta) {
                // la flèche verticale de la carte ouverte navigue la
                // carte (les correspondances de la recherche) ; celle
                // de la carte de complétion navigue ses candidats — le
                // texte tapé, lui, poursuit vers le tampon
                KeyboardCard carte = openCard();
                if (carte != null && carte.onArrow(delta)) {
                    return;
                }
                if (completionCard != null && completionCard.isShown()
                        && completionCard.onArrow(delta)) {
                    return;
                }
                EditorView.this.moveCaretVertically(delta);
            }

            @Override
            public void setSelection(int start, int end) {
                EditorView.this.setSelection(start, end);
            }
        };
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // les cartes vivantes, dessinées dans la
        // vue : le doigt qui tombe SUR une carte lui appartient —
        // elle agit et le geste entier se consomme ; le doigt hors
        // des cartes poursuit sa vie vers l'éditeur (le caret bouge,
        // la sélection suit)
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            boolean touchedSearch = searchBar != null && searchBar.isShown()
                    && searchBar.onTouchArea(event.getX(), event.getY());
            boolean touchedGoto = !touchedSearch && gotoBar != null && gotoBar.isShown()
                    && gotoBar.onTouchArea(event.getX(), event.getY());
            boolean touchedRename = !touchedSearch && !touchedGoto
                    && renameBar != null && renameBar.isShown()
                    && renameBar.onTouchArea(event.getX(), event.getY());
            // la barre et ses cartes ne prennent le doigt que d'une
            // sélection VIVANTE : une sélection morte sans trame
            // intermédiaire y laisserait une barre périmée — le doigt
            // passerait à travers le texte (la sélection se range à
            // la trame suivante, la barre l'ignore entre-temps)
            boolean selectionAlive = hasSelection() && selectionDragBound == 0;
            downOnCard = touchedSearch || touchedGoto || touchedRename
                    || (completionCard != null && completionCard.isShown()
                            && completionCard.onTouchArea(event.getX(), event.getY()))
                    || (signatureCard != null && signatureCard.isShown()
                            && signatureCard.onTouchArea(event.getX(), event.getY()))
                    || (historyCard != null && historyCard.isShown() && selectionAlive
                            && historyCard.onTouchArea(event.getX(), event.getY()))
                    || (diagnosticCard != null && diagnosticCard.isShown()
                            && diagnosticCard.onTouchArea(event.getX(), event.getY()))
                    || (quickActionCard != null && quickActionCard.isShown()
                            && quickActionCard.onTouchArea(event.getX(), event.getY()))
                    || (selectionBar != null && selectionBar.isShown() && selectionAlive
                            && selectionBar.onTouchArea(event.getX(), event.getY()))
                    || (overflowCard != null && overflowCard.isShown() && selectionAlive
                            && overflowCard.onTouchArea(event.getX(), event.getY()));
            if (downOnCard) {
                // le doigt vise un CHAMP de la carte : le clavier se
                // lève — la carte ne prend jamais le focus, le clavier
                // reste à l'éditeur qui route les frappes à la carte
                if ((touchedSearch && searchBar.keyboardWanted())
                        || (touchedGoto && gotoBar.keyboardWanted())
                        || (touchedRename && renameBar.keyboardWanted())) {
                    openKeyboard();
                }
                return true;
            }
        } else if (downOnCard) {
            if (event.getActionMasked() == MotionEvent.ACTION_UP
                    || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                downOnCard = false;
            }
            return true;
        }
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                touchStartX = event.getX();
                touchStartY = event.getY();
                touchDownNanos = System.nanoTime();
                pinching = false;
                pinchedThisGesture = false;
                // un nouvel appui masque la popup d'info (l'appui long
                // pourra en rouvrir une sur le mot touché) et range les
                // cartes de diagnostic et d'ampoule
                dismissInfoPopup();
                diagnosticCard.close();
                quickActionCard.close();
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
            case MotionEvent.ACTION_POINTER_DOWN:
                // le second doigt se pose : le pincement s'engage (les
                // gestes en cours se rangent — la poignée se lâche, la
                // sélection reste posée)
                if (event.getPointerCount() >= 2) {
                    engagePinch(event);
                }
                break;
            case MotionEvent.ACTION_POINTER_UP:
                // un doigt du pincement s'en va : le zoom se dénoue, le
                // doigt qui demeure peut glisser sans sauter
                if (pinching) {
                    endPinch(event);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (pinching) {
                    if (event.getPointerCount() >= 2) {
                        pinchMove(event);
                    }
                    break;
                }
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
                    // curseur le temps du geste) ; le doigt près du
                    // bord programme le défilement — le point de dépôt
                    // reste lisible pendant que la vue glisse
                    dragFingerX = event.getX();
                    dragFingerY = event.getY();
                    scheduleEdgeScroll();
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
                if (pinching || pinchedThisGesture) {
                    // le pincement se referme : l'appui final ne vaut
                    // pas un tap (le curseur ne saute pas au doigt qui
                    // reste le dernier levé)
                    pinching = false;
                    pinchedThisGesture = false;
                    lastTapNanos = 0L;
                    invalidate();
                    break;
                }
                if (selectionDragBound != 0) {
                    // la poignée se lâche : la sélection reste posée,
                    // l'appui ne compte pas pour un tap (le suivant ne
                    // serait pas un double-tap) ; la trame se redemande —
                    // la loupe s'y range, la barre de sélection y naît
                    // (sans elle, la dernière trame — loupe comprise —
                    // resterait gelée à l'écran)
                    selectionDragBound = 0;
                    dragFixedBound = -1;
                    lastTapNanos = 0L;
                    invalidate();
                    break;
                }
                if (textDragging) {
                    // le dépôt : le texte sélectionné se déplace au
                    // curseur de dépôt — l'appui ne compte pas pour un
                    // tap non plus ; le défilement au bord se range
                    removeCallbacks(edgeScroll);
                    edgeScrollScheduled = false;
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
                    // la gouttière : la colonne des marqueurs d'abord —
                    // l'erreur ouvre sa carte de diagnostic, l'ampoule
                    // ouvre ses actions rapides — le pli de la ligne
                    // touchée se retourne sinon (le clavier ne s'y ouvre
                    // pas)
                    requestFocus();
                    int line = lineOfVisibleIndex(
                            (int) ((event.getY() - getPaddingTop() + scrollY) / lineHeight));
                    if (event.getX() < dp(24)
                            && handleGutterMarkerTap(line)) {
                        clearComposing();
                        lastTapNanos = 0L;
                        break;
                    }
                    toggleFoldAt(line);
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
                    // le souligné rouge d'un diagnostic : la carte
                    // s'y ouvre — le doigt vise le signe mésapparié, le
                    // curseur ne bouge pas
                    int offset = offsetNearest(event.getX(), event.getY());
                    if (!openDiagnosticAtTap(offset)) {
                        placeCaretAt(event.getX(), event.getY());
                        clearComposing();
                        showSoftInput();
                    }
                }
                lastTapNanos = System.nanoTime();
                lastTapX = event.getX();
                lastTapY = event.getY();
                break;
            case MotionEvent.ACTION_CANCEL:
                // le système reprend le geste (la navigation par geste,
                // une vue parente) : les traînements se rangent comme
                // au lâcher — sinon la loupe resterait posée à jamais
                // et la barre de sélection ne naîtrait jamais (l'état
                // de poignée tenue tuerait les deux) ; le pincement
                // s'y range pareillement
                pinching = false;
                pinchedThisGesture = false;
                if (selectionDragBound != 0 || textDragging || dragging) {
                    removeCallbacks(edgeScroll);
                    edgeScrollScheduled = false;
                    selectionDragBound = 0;
                    dragFixedBound = -1;
                    textDragging = false;
                    textDragArmed = false;
                    dropOffset = -1;
                    dragging = false;
                    lastTapNanos = 0L;
                    invalidate();
                }
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
            selection.setCaret(Math.min(offset, dragFixedBound));
            selection.setAnchor(dragFixedBound);
        } else {
            selection.setCaret(Math.max(offset, dragFixedBound));
            selection.setAnchor(dragFixedBound);
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
        drawGutter(canvas, first, last, count);
        drawScopeBand(canvas, first, last, count);
        drawSelection(canvas, first, last, count);
        drawSearchHighlights(canvas, first, last, count);
        drawServerHighlights(canvas, first, last, count);
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
        drawDragPreview(canvas);
        drawLoupe(canvas);
        // les candidats AVANT leur dessin (un déplacement de curseur
        // rend ses candidats dans la même trame), les soulignés de
        // diagnostic par-dessus le texte, le bandeau par-dessus tout,
        // la popup d'info par-dessus le bandeau
        refreshAnalysis();
        updateSuggestions();
        updateSignature();
        updateSelectionBar();
        updateQuickActions();
        drawDiagnostics(canvas, first, last, count);
        drawCompletionCard(canvas);
        drawInfoPopup(canvas);
        drawSignatureCard(canvas);
        drawDiagnosticCard(canvas);
        drawQuickActionCard(canvas);
        drawSelectionBar(canvas);
        // les cartes de recherche et d'aller-à, par-dessus tout :
        // dessinées dans la vue, posées au
        // caret, le clavier délégué (la pose se recalcule ici : le
        // caret les suit, le harnais la lit)
        if (searchBar != null && searchBar.isShown()) {
            searchBar.draw(canvas, popupMetrics());
        }
        if (gotoBar != null && gotoBar.isShown()) {
            gotoBar.draw(canvas, popupMetrics());
        }
        // la carte de renommage, par-dessus tout elle aussi : posée au
        // caret, le clavier délégué — la pose se recalcule ici, le
        // caret la suit (le harnais la lit)
        if (renameBar != null && renameBar.isShown()) {
            renameBar.draw(canvas, popupMetrics());
        }
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
        if (selection.caret() == pairCaret && buffer.getRevision() == pairRevision) {
            return;
        }
        pairCaret = selection.caret();
        pairRevision = buffer.getRevision();
        int caret = Math.min(selection.caret(), buffer.length());
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
        selectionRenderer.draw(canvas, selectionLines, selectionStart(),
                selectionEnd(), first, last, count,
                new SelectionRenderer.Geometry(getPaddingLeft(), getPaddingTop(),
                        lineHeight, charWidth, scrollY));
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
     * Les teintes du serveur de langage : les occurrences du symbole
     * au curseur (le surlignage du protocole) se teintent en bleu —
     * la recherche reste ambre, les deux familles se lisent d'un
     * coup d'œil. Chaque plage vit en lignes/colonnes : la fenêtre
     * la découpe ligne par ligne (une plage peut franchir une borne
     * de ligne).
     */
    private void drawServerHighlights(Canvas canvas, int first, int last, int count) {
        if (serverHighlights.isEmpty()) {
            return;
        }
        int windowStart = buffer.getLineStart(first);
        int windowEnd = last + 1 < count
                ? buffer.getLineStart(last + 1) : buffer.length();
        for (LspRange range : serverHighlights) {
            int start = buffer.offsetOfPosition(
                    range.start().line(), range.start().character());
            int end = buffer.offsetOfPosition(
                    range.end().line(), range.end().character());
            if (start >= end || start >= windowEnd || end <= windowStart) {
                continue;
            }
            // chaque tranche de ligne de la plage se teinte : la
            // colonne borne la première et la dernière, les
            // intermédiaires prennent la ligne entière
            int line = buffer.lineOfOffset(Math.max(start, windowStart));
            int lastLine = buffer.lineOfOffset(Math.min(end, windowEnd));
            for (; line <= lastLine; line++) {
                if (isHidden(line)) {
                    continue;
                }
                int lineStart = buffer.getLineStart(line);
                int columnStart = line == buffer.lineOfOffset(start)
                        ? start - lineStart : 0;
                int lineEnd = buffer.getLineEnd(line);
                int columnEnd = line == buffer.lineOfOffset(end)
                        ? end - lineStart : lineEnd - lineStart;
                float left = getPaddingLeft() + columnStart * charWidth;
                float right = getPaddingLeft() + columnEnd * charWidth;
                float top = getPaddingTop() + visibleIndexOf(line) * lineHeight - scrollY;
                canvas.drawRect(left, top, right, top + lineHeight,
                        serverHighlightPaint);
            }
        }
    }

    /**
     * Pose les plages surlignées par le serveur de langage : les
     * occurrences du symbole au curseur (le surlignage du protocole
     * — la façade du serveur les y pose, le doigt les voit). La liste
     * entière remplace la précédente ; null ou vide les range.
     *
     * @param ranges les plages surlignées (coordonnées LSP), null
     *               pour ranger
     */
    public void setServerHighlights(List<LspRange> ranges) {
        serverHighlights = ranges == null ? List.of() : List.copyOf(ranges);
        invalidate();
    }

    /**
     * Les plages surlignées par le serveur (le harnais les lit).
     *
     * @return les plages en coordonnées LSP, jamais null
     */
    public List<LspRange> serverHighlights() {
        return serverHighlights;
    }

    /**
     * La popup d'info ancrée : un rectangle arrondi au-dessus de la
     * ligne d'ancrage (sous elle sans la place), borné aux bords de la
     * vue, le texte coupé à la largeur disponible — elle se dessine en
     * dernier, par-dessus le bandeau.
     */
    private void drawInfoPopup(Canvas canvas) {
        if (infoPopup.text() == null) {
            return;
        }
        float anchorTop = getPaddingTop()
                + visibleIndexOf(infoPopup.line()) * lineHeight - scrollY;
        // la barre de sélection vit : la documentation s'y empile —
        // la popup s'ancre au-dessus de la barre (le « i » l'a ouverte
        // sur la sélection)
        if (selectionBar.isShown()) {
            anchorTop = Math.min(anchorTop, selectionBar.poseY());
        }
        infoPopup.draw(canvas, anchorTop, popupMetrics());
    }

    /**
     * La géométrie de la vue au dessin des popups : le paquet des
     * popups n'en connaît que ces nombres.
     */
    private PopupMetrics popupMetrics() {
        int caretOffset = Math.min(selection.caret(), buffer.length());
        int caretLineIndex = buffer.lineOfOffset(caretOffset);
        float caretX = getPaddingLeft()
                + (caretOffset - buffer.getLineStart(caretLineIndex)) * charWidth;
        float caretY = getPaddingTop()
                + visibleIndexOf(caretLineIndex) * lineHeight - scrollY;
        return new PopupMetrics(getPaddingLeft(), getPaddingTop(),
                getPaddingRight(), getPaddingBottom(), getWidth(), getHeight(),
                lineHeight, charWidth, dp(1), caretX, caretY);
    }

    /**
     * La gouttière : le numéro de chaque ligne visible, aligné à
     * droite avant la zone du marqueur de pli — puis la colonne des
     * MARQUEURS à gauche des numéros : le rond rouge de l'erreur, le
     * triangle ambre de l'avertissement (la ligne qui porte un
     * diagnostic), l'ampoule jaune (les actions rapides de la ligne
     * du curseur) — le pinceau vit au paquet, la vue ne fournit que
     * la géométrie.
     */
    private void drawGutter(Canvas canvas, int first, int last, int count) {
        float markerCx = dp(12);
        int caretLine = caretLine();
        for (int line = first; line <= last && line < count; line++) {
            if (isHidden(line)) {
                continue;
            }
            gutter.drawNumber(canvas, line, lineBaseline(line),
                    getPaddingLeft() - dp(14));
            boolean error = false;
            boolean warning = false;
            if (diagnostics != null && analysisRevision == buffer.getRevision()) {
                for (DiagnosticsDocument.Diagnostic diagnostic
                        : diagnostics.betweenLines(line, line)) {
                    if (diagnostic.start() >= buffer.getLineStart(line)
                            && diagnostic.start() < buffer.getLineEnd(line)) {
                        error = true;
                    }
                }
            }
            if (diagnosticProvider != null) {
                for (DiagnosticCard.Entry hosted
                        : diagnosticsOfLine(line)) {
                    if (hosted.severity() == DiagnosticCard.SEVERITY_ERROR) {
                        error = true;
                    } else {
                        warning = true;
                    }
                }
            }
            float cy = lineBaseline(line)
                    + (fontMetrics.top + fontMetrics.bottom) / 2.0f;
            if (error) {
                gutter.drawMarker(canvas, Gutter.MARKER_ERROR, markerCx, cy);
            } else if (warning) {
                gutter.drawMarker(canvas, Gutter.MARKER_WARNING, markerCx, cy);
            }
            if (line == caretLine && !quickActions.isEmpty()) {
                gutter.drawMarker(canvas, Gutter.MARKER_LIGHTBULB, markerCx, cy);
            }
        }
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
                drawFoldMarker(canvas, line, folds.isFolded(line));
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
     * Le doigt sur la colonne des marqueurs de la gouttière : la
     * ligne qui porte un diagnostic y ouvre sa carte, l'ampoule de
     * la ligne du curseur y ouvre ses actions rapides — une ligne
     * sans marqueur ne prend rien (le pli vit derrière).
     *
     * @param line la ligne touchée
     * @return vrai quand un marqueur a pris le geste
     */
    private boolean handleGutterMarkerTap(int line) {
        if (line < 0 || line >= buffer.getLineCount()) {
            return false;
        }
        if (lineHasDiagnostics(line)) {
            return openDiagnosticsAtLine(line);
        }
        if (line == caretLine()) {
            return openQuickActionsAtLine(line);
        }
        return false;
    }

    /**
     * Le doigt sur le SOULIGNÉ ROUGE d'un diagnostic (le signe
     * mésapparié, à une colonne près) : la carte de diagnostic de la
     * ligne s'y ouvre — le curseur ne bouge pas.
     *
     * @param offset le tap le plus proche du doigt
     * @return vrai quand un souligné a pris le geste
     */
    private boolean openDiagnosticAtTap(int offset) {
        if (diagnostics == null || analysisRevision != buffer.getRevision()) {
            return false;
        }
        int line = buffer.lineOfOffset(Math.min(offset, buffer.length()));
        int column = Math.min(offset, buffer.length()) - buffer.getLineStart(line);
        for (DiagnosticsDocument.Diagnostic local
                : diagnostics.betweenLines(line, line)) {
            int startCol = local.start() - buffer.getLineStart(line);
            int endCol = local.end() - buffer.getLineStart(line);
            if (column >= startCol - 1 && column <= endCol + 1) {
                return openDiagnosticsAtLine(line);
            }
        }
        return false;
    }

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
        if (folds.isFolded(line)) {
            folds.unfold(line);
        } else {
            folds.fold(line);
            // le curseur dans la région repliée : il remonte à la fin de
            // l'en-tête (juste après l'ouvrant, la paire s'y encadre)
            int end = structure.foldEndLine(line);
            int caretLine = buffer.lineOfOffset(Math.min(selection.caret(), buffer.length()));
            if (caretLine > line && caretLine <= end) {
                selection.setCaret(buffer.getLineEnd(line));
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
        return folds.hiddenLineCount();
    }

    /**
     * Plis actuellement repliés (le panneau le lit).
     *
     * @return le nombre d'en-têtes repliés
     */
    public int foldedCount() {
        return folds.count();
    }

    /**
     * Décale les en-têtes de plis suivant l'édition (les lignes après
     * le point d'édition bougent du delta de lignes), puis reconstruit
     * le plan des cachées — un pli dont l'en-tête n'ouvre plus rien se
     * dissout à la reconstruction.
     */
    private void shiftFolds(int start) {
        if (!folds.hasFolds()) {
            trackedLineCount = buffer.getLineCount();
            return;
        }
        int line = buffer.lineOfOffset(start);
        int delta = buffer.getLineCount() - trackedLineCount;
        folds.shift(line, delta, buffer.getLineCount());
        rebuildFoldLayout();
        revealCaret();
    }

    /**
     * Le curseur ne vit jamais dans une ligne cachée : le pli le plus
     * interne qui le cache s'ouvre (l'édition y arrive).
     */
    private void revealCaret() {
        if (!folds.hasFolds()) {
            return;
        }
        int line = buffer.lineOfOffset(Math.min(selection.caret(), buffer.length()));
        // la reconstruction peut annuler les plis (dernier ouvert) :
        // la garde se relit à chaque tour
        while (folds.isHidden(line)) {
            int container = folds.headerHiding(line, structure);
            if (container < 0) {
                return;
            }
            folds.unfold(container);
            rebuildFoldLayout();
        }
    }

    /**
     * Reconstruit les lignes cachées et le mapping visible des plis
     * courants (une passe en O(lignes), aux changements de plis et aux
     * éditions — jamais par trame).
     */
    private void rebuildFoldLayout() {
        trackedLineCount = buffer.getLineCount();
        folds.rebuild(structure, trackedLineCount);
    }

    /** La ligne est-elle cachée par un pli. */
    private boolean isHidden(int line) {
        return folds.isHidden(line);
    }

    /** L'indice visible d'une ligne (les plis compactent la hauteur). */
    private int visibleIndexOf(int line) {
        return folds.visibleIndexOf(line);
    }

    /** Nombre de lignes visibles (le document moins les cachées). */
    private int visibleLineCount() {
        return buffer.getLineCount() - folds.hiddenLineCount();
    }

    /**
     * La ligne visible d'indice donné : la dernière ligne dont
     * l'indice visible la précède ou l'égale (les cachées partagent
     * l'indice de la visible qui les suit).
     */
    private int lineOfVisibleIndex(int index) {
        return folds.lineOfVisibleIndex(index, buffer.getLineCount());
    }

    /** Ligne de base d'une ligne VISIBLE (le mapping des plis s'applique). */
    private float lineBaseline(int line) {
        int visible = folds.visibleIndexOf(line);
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
        SyntaxDocument.LineTokens entry = (SyntaxDocument.LineTokens) stamp;
        if (entry.tokenCount() == 0) {
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
        for (int at = 0; at < entry.tokenCount(); at++) {
            int start = entry.startAt(at);
            int end = entry.endAt(at);
            if (end <= start) {
                continue;
            }
            if (start >= MAX_DRAWN_COLUMNS) {
                break;
            }
            kept++;
        }
        String[] texts = new String[kept];
        int[] colors = new int[kept];
        float[] xs = new float[kept];
        boolean[] bolds = new boolean[kept];
        int at = 0;
        for (int token = 0; token < entry.tokenCount(); token++) {
            int start = entry.startAt(token);
            int end = entry.endAt(token);
            if (end <= start) {
                continue;
            }
            if (start >= MAX_DRAWN_COLUMNS) {
                break;
            }
            int endColumn = Math.min(end, MAX_DRAWN_COLUMNS);
            jo.pigma.syntax.Scope scope = entry.scopeAt(token);
            texts[at] = content.subSequence(start, endColumn).toString();
            colors[at] = scope == null
                    ? theme.defaultColor() : theme.colorOf(scope);
            xs[at] = start * charWidth;
            if (scope == null && !notes.isEmpty()) {
                // fusion L3 > L2 > L1 : seul un neutre peut se colorer —
                // une couleur L1 affichée ne change jamais
                for (SemanticAnnotation note : notes) {
                    if (note.covers(lineStart + start, lineStart + endColumn)) {
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
        int caret = Math.min(selection.caret(), buffer.length());
        int wordStart = wordStartAt(caret);
        if (wordStart < 0) {
            completionCard.set(List.of(), "");
            suggestionWordStart = -1;
            return;
        }
        String prefix = buffer.getText(wordStart, caret).toString();
        suggestionWordStart = wordStart;
        if (suggestionProvider != null) {
            // la source extérieure tire à la trame : elle cache sa
            // réponse asynchrone (le contrat y est documenté) — les
            // colonnes de détail vivent vides (la nature neutre) quand
            // le fournisseur ne rend que des noms
            int line = buffer.lineOfOffset(caret);
            completionCard.setNames(suggestionProvider.completionsFor(prefix,
                    line, caret - buffer.getLineStart(line)), prefix);
            return;
        }
        completionCard.set(localCompletionEntries(prefix), prefix);
    }

    /**
     * Les candidats détaillés de l'index local : la nature y porte
     * l'icône, la nature et la ligne de déclaration portent la
     * colonne de droite (la facture de l'atelier).
     *
     * @param prefix le préfixe tapé
     * @return les candidats détaillés, jamais null
     */
    private List<CompletionCard.Entry> localCompletionEntries(String prefix) {
        if (symbolIndex == null) {
            return List.of();
        }
        List<CompletionCard.Entry> entries = new ArrayList<>();
        for (SymbolIndex.Candidate candidate : symbolIndex.queryDetailed(prefix, 8)) {
            String nature = candidate.nature();
            int kind = CompletionCard.KIND_WORD;
            if ("type".equals(nature)) {
                kind = CompletionCard.KIND_CLASS;
            } else if ("méthode".equals(nature)) {
                kind = CompletionCard.KIND_FUNCTION;
            } else if ("champ".equals(nature)) {
                kind = CompletionCard.KIND_FIELD;
            }
            String detail = "mot".equals(nature) ? ""
                    : nature + (candidate.line() >= 0 ? " · ligne " + (candidate.line() + 1)
                            : "");
            entries.add(new CompletionCard.Entry(candidate.name(), detail, kind));
        }
        return entries;
    }

    /**
     * L'aide aux paramètres du curseur : le fournisseur de l'hôte
     * répond le premier (un serveur de langage par exemple), l'index
     * local des symboles porte sinon la carte — une position hors
     * appel, ou un nom sans déclaration, la range.
     */
    private void updateSignature() {
        // la barre de sélection tient l'espace au-dessus de la ligne :
        // tant qu'une sélection vit, la carte de signature y cède (la
        // frappe la ramène — elle remplace la sélection)
        if (selectionBarAlive()) {
            signatureCard.set(null, 0);
            return;
        }
        int caret = Math.min(selection.caret(), buffer.length());
        SignatureHelp help = signatureProvider != null
                ? signatureProvider.signatureAt(caret) : localSignatureAt(caret);
        signatureCard.set(help == null ? null : help.label(),
                help == null ? 0 : help.activeParameter());
    }

    /**
     * L'aide aux paramètres par l'index local : l'appel se lit en
     * remontant du curseur vers sa parenthèse ouvrante non fermée (la
     * fenêtre se borne à 256 caractères — jamais le document pour un
     * appel) ; le nom de l'appel se cherche dans les déclarations, la
     * ligne de la déclaration devient la signature (coupée de son
     * bloc ouvrant) ; les virgules de profondeur nulle entre
     * l'ouvrante et le curseur disent le paramètre actif.
     *
     * @param caret le décalage du curseur dans le tampon
     * @return la signature et son paramètre actif, null hors appel
     */
    private SignatureHelp localSignatureAt(int caret) {
        if (caret <= 0 || caret > buffer.length() || symbolIndex == null) {
            return null;
        }
        int windowStart = Math.max(0, caret - 256);
        // l'ouvrante non fermée : en remontant du curseur, les
        // fermantes se consomment, l'ouvrante à profondeur nulle est
        // celle de l'appel ; un finisseur d'instruction avant elle
        // coupe la recherche
        int depth = 0;
        int openParen = -1;
        for (int at = caret - 1; at >= windowStart; at--) {
            char c = buffer.charAt(at);
            if (c == ')') {
                depth++;
            } else if (c == '(') {
                if (depth == 0) {
                    openParen = at;
                    break;
                }
                depth--;
            } else if (c == ';' || c == '{' || c == '}') {
                return null;
            }
        }
        if (openParen < 0) {
            return null;
        }
        // le nom de l'appel : le mot avant l'ouvrante
        int nameEnd = openParen;
        while (nameEnd > windowStart
                && Character.isWhitespace(buffer.charAt(nameEnd - 1))) {
            nameEnd--;
        }
        int nameStart = nameEnd;
        while (nameStart > windowStart && isWordChar(buffer.charAt(nameStart - 1))) {
            nameStart--;
        }
        if (nameStart == nameEnd) {
            return null;
        }
        String callName = buffer.getText(nameStart, nameEnd).toString();
        // le paramètre actif : les virgules de profondeur nulle
        int active = 0;
        int nested = 0;
        for (int at = openParen + 1; at < caret; at++) {
            char c = buffer.charAt(at);
            if (c == '(' || c == '[' || c == '{') {
                nested++;
            } else if (c == ')' || c == ']' || c == '}') {
                nested--;
            } else if (c == ',' && nested == 0) {
                active++;
            }
        }
        // la déclaration du nom : sa ligne devient la signature
        for (SymbolIndex.Symbol symbol : symbolIndex.symbols()) {
            if (symbol.name().equals(callName)) {
                int lineStart = buffer.getLineStart(symbol.line());
                int lineEnd = buffer.getLineEnd(symbol.line());
                String text = buffer.getText(lineStart, lineEnd).toString().trim();
                if (text.endsWith("{")) {
                    text = text.substring(0, text.length() - 1).trim();
                }
                if (text.isEmpty() || text.indexOf('(') < 0) {
                    return null;
                }
                return new SignatureHelp(text, active);
            }
        }
        return null;
    }

    /**
     * La barre de sélection (le harnais la lit) : les actions de la
     * sélection en pastilles.
     *
     * @return la barre de sélection
     */
    public SelectionBar selectionBar() {
        return selectionBar;
    }

    /**
     * La carte de débordement de la barre de sélection (le harnais la
     * lit) : les actions repliées.
     *
     * @return la carte de débordement
     */
    public OverflowCard overflowCard() {
        return overflowCard;
    }

    /**
     * Copie la sélection au presse-papiers du système (tronquée au
     * plafond de transaction — la copie monstre ne tue pas l'appel).
     * La barre s'y rafraîchit : la pastille Coller en naît.
     */
    public void copySelection() {
        if (!hasSelection()) {
            return;
        }
        CharSequence selected = selectedText();
        placeClipboard("sélection Pigma", selected);
        clipboardProbeFrame = -1;
        invalidate();
    }

    /**
     * Colle le texte du presse-papiers sur la sélection (une unité
     * d'annulation) ; sans sélection, le texte se pose au curseur.
     */
    public void pasteFromClipboard() {
        CharSequence clip = clipboardText();
        if (clip == null || clip.length() == 0) {
            return;
        }
        int start = hasSelection() ? selectionStart() : Math.min(selection.caret(), buffer.length());
        int end = hasSelection() ? selectionEnd() : start;
        pendingEditLabel = "collage";
        applyEdit(start, end, clip.toString());
    }

    /**
     * Copie TOUT le document au presse-papiers du système — « tout
     * copier », tronqué au plafond de transaction. La barre s'y
     * rafraîchit : la pastille Coller en naît.
     */
    public void copyAllText() {
        placeClipboard("document Pigma", buffer.toString());
        clipboardProbeFrame = -1;
        invalidate();
    }

    /**
     * La documentation rapide du mot sélectionné : la popup d'info
     * y répond (le fournisseur de l'hôte d'abord, l'index local
     * sinon) — le « i » de la barre de sélection.
     */
    public void showSelectionDocs() {
        if (!hasSelection()) {
            return;
        }
        int start = selectionStart();
        int end = selectionEnd();
        int line = buffer.lineOfOffset(start);
        showSymbolInfo(start, end, line, start - buffer.getLineStart(line));
    }

    /**
     * Ouvre la carte de débordement de la barre : les actions
     * repliées en ÉTAGES — la navigation (aller à la ligne, rechercher),
     * l'édition (commenter, casse, dupliquer, supprimer la ligne), le
     * presse-papiers (couper, tout sélectionner, quitter la sélection),
     * l'historique local de la sélection (une sélection vit), l'hôte
     * (ses actions que la pilule ne portait pas). Les étages naissent
     * du contexte : le langage sans commentaire de ligne n'y offre pas
     * le sien, l'étage hôte ne vit que de ses actions. La puce la
     * referme (le menu se rabat) ; le choix d'une rangée la referme
     * AVANT d'exécuter ; elle ne survit jamais à sa barre (la
     * sélection morte la ferme).
     */
    public void showOverflowMenu() {
        if (overflowCard.isShown()) {
            overflowCard.close();
            invalidate();
            return;
        }
        List<OverflowCard.Section> families = new ArrayList<>();
        families.add(new OverflowCard.Section("Navigation", List.of(
                new OverflowCard.Row("Aller à la ligne…", this::toggleGotoPopup),
                new OverflowCard.Row("Rechercher…", this::toggleSearchPopup))));
        List<OverflowCard.Row> edition = new ArrayList<>();
        if (!lineCommentPrefix().isEmpty()) {
            edition.add(new OverflowCard.Row("Commenter / décommenter",
                    this::toggleLineComment));
        }
        if (hasSelection()) {
            edition.add(new OverflowCard.Row("En majuscules", this::uppercaseSelection));
            edition.add(new OverflowCard.Row("En minuscules", this::lowercaseSelection));
        }
        edition.add(new OverflowCard.Row("Dupliquer la ligne", this::duplicateLine));
        edition.add(new OverflowCard.Row("Supprimer la ligne", this::deleteLines));
        families.add(new OverflowCard.Section("Édition", edition));
        families.add(new OverflowCard.Section("Presse-papiers", List.of(
                new OverflowCard.Row("Couper", this::cutSelection),
                new OverflowCard.Row("Tout sélectionner", this::selectAll),
                new OverflowCard.Row("Quitter la sélection", this::clearSelection))));
        if (hasSelection()) {
            families.add(new OverflowCard.Section("Historique", List.of(
                    new OverflowCard.Row("Historique de la sélection",
                            this::showHistoryPopup))));
        }
        List<OverflowCard.Row> hosts = new ArrayList<>();
        for (SelectionBar.HostAction action : selectionBar.hostOverflowActions()) {
            hosts.add(new OverflowCard.Row(action.label(), action.action()));
        }
        if (!hosts.isEmpty()) {
            families.add(new OverflowCard.Section("Hôte", hosts));
        }
        overflowCard.openSections(families);
        invalidate();
    }

    /**
     * La barre de sélection vit-elle à cette trame ? Une sélection
     * vivante, tenue par aucun geste (la poignée traînée, le traîné
     * ou le défilement la tiennent) — la barre se pose au-dessus
     * d'elle, les pastilles vives (la pastille Coller naît du
     * presse-papiers).
     */
    private boolean selectionBarAlive() {
        return hasSelection() && selectionDragBound == 0
                && !textDragging && !dragging;
    }

    private void updateSelectionBar() {
        if (!selectionBarAlive()) {
            selectionBar.hide();
            overflowCard.close();
            historyCard.close();
            clipboardProbeFrame = -1;
            return;
        }
        selectionBar.show(clipboardHasText(), hostSelectionActions);
    }

    /**
     * L'hôte pose SA pastille sur la barre de sélection : l'action vit
     * en pastille tant que la pilule tient au budget, le trop se
     * replie à la carte de débordement (l'étage « Hôte »).
     *
     * @param label  le libellé de la pastille, jamais vide
     * @param action ce qu'elle fait, jamais null
     */
    public void addSelectionAction(String label, Runnable action) {
        if (label == null || label.isEmpty() || action == null) {
            return;
        }
        hostSelectionActions.add(new SelectionBar.HostAction(label, action));
        invalidate();
    }

    /** Range les pastilles de l'hôte (l'application les retire). */
    public void clearSelectionActions() {
        hostSelectionActions.clear();
        invalidate();
    }

    /**
     * Le préfixe de commentaire de fin de ligne du langage courant —
     * vide quand le langage n'en porte pas (XML, JSON : l'action de
     * commentaire s'y cache).
     */
    private String lineCommentPrefix() {
        String name = language == null ? "" : language.name();
        switch (name) {
            case "Java":
            case "Kotlin":
                return "//";
            case "Lua":
            case "SQL":
                return "--";
            default:
                return "";
        }
    }

    /**
     * Commente ou décommente les lignes de la sélection (la ligne du
     * curseur sans sélection) : les lignes déjà commentées perdent
     * leur préfixe, les autres le gagnent à leur indentation — une
     * seule unité d'annulation, un seul geste pour l'historique.
     */
    public void toggleLineComment() {
        String prefix = lineCommentPrefix();
        if (prefix.isEmpty()) {
            return;
        }
        int from = buffer.lineOfOffset(Math.min(selectionStart(), buffer.length()));
        int to = buffer.lineOfOffset(Math.max(selectionEnd(), selection.caret()));
        int lineCount = buffer.getLineCount();
        if (lineCount == 0) {
            return;
        }
        to = Math.min(to, lineCount - 1);
        pendingEditLabel = "commentaire";
        try (TextTransaction transaction = undoStack.newTransaction()) {
            for (int line = from; line <= to; line++) {
                int lineStart = buffer.getLineStart(line);
                int lineEnd = buffer.getLineEnd(line);
                String content = buffer.getText(lineStart, lineEnd).toString();
                int first = 0;
                while (first < content.length()
                        && content.charAt(first) == ' ') {
                    first++;
                }
                if (content.startsWith(prefix, first)) {
                    int cutEnd = first + prefix.length();
                    if (cutEnd < content.length() && content.charAt(cutEnd) == ' ') {
                        cutEnd++;
                    }
                    if (cutEnd <= content.length()) {
                        transaction.replace(lineStart + first,
                                lineStart + cutEnd, "");
                    }
                } else {
                    if (first < content.length()) {
                        transaction.replace(lineStart + first, lineStart + first,
                                prefix + " ");
                    } else {
                        transaction.replace(lineStart, lineStart, prefix);
                    }
                }
            }
        }
        invalidate();
    }

    /**
     * La sélection passe en MAJUSCULES — une unité d'annulation, un
     * geste pour l'historique. Sans sélection vivante, rien ne se
     * paye.
     */
    public void uppercaseSelection() {
        if (!hasSelection()) {
            return;
        }
        pendingEditLabel = "casse";
        applyEdit(selectionStart(), selectionEnd(),
                selectedText().toString().toUpperCase(java.util.Locale.ROOT));
    }

    /**
     * La sélection passe en minuscules — une unité d'annulation, un
     * geste pour l'historique.
     */
    public void lowercaseSelection() {
        if (!hasSelection()) {
            return;
        }
        pendingEditLabel = "casse";
        applyEdit(selectionStart(), selectionEnd(),
                selectedText().toString().toLowerCase(java.util.Locale.ROOT));
    }

    /**
     * Duplique la ligne (celle de la sélection, celle du curseur
     * sinon) : sa copie se pose dessous, le curseur y descend — une
     * unité d'annulation, un geste pour l'historique.
     */
    public void duplicateLine() {
        if (buffer.getLineCount() == 0) {
            return;
        }
        int line = buffer.lineOfOffset(Math.min(selectionStart(), buffer.length()));
        int lineStart = buffer.getLineStart(line);
        int lineEnd = buffer.getLineEnd(line);
        String carried = buffer.getText(lineStart, lineEnd).toString();
        pendingEditLabel = "duplication";
        try (TextTransaction transaction = undoStack.newTransaction()) {
            transaction.replace(lineEnd, lineEnd, "\n" + carried);
        }
        caretTo(lineEnd + 1 + carried.length());
        invalidate();
    }

    /**
     * Supprime les lignes de la sélection (celle du curseur sinon) —
     * le saut de fin compris, une unité d'annulation, un geste pour
     * l'historique. La dernière ligne du document garde son saut
     * d'avant : le tampon demeure cohérent.
     */
    public void deleteLines() {
        int lineCount = buffer.getLineCount();
        if (lineCount == 0) {
            return;
        }
        int from = buffer.lineOfOffset(Math.min(selectionStart(), buffer.length()));
        int to = Math.min(buffer.lineOfOffset(
                Math.max(selectionEnd(), selection.caret())), lineCount - 1);
        int start = buffer.getLineStart(from);
        int end = to + 1 < lineCount
                ? buffer.getLineStart(to + 1) : buffer.length();
        if (end <= start) {
            return;
        }
        pendingEditLabel = "suppression de ligne";
        try (TextTransaction transaction = undoStack.newTransaction()) {
            transaction.replace(start, end, "");
        }
        clampCaret();
        invalidate();
    }

    /**
     * L'historique local du document (le harnais le lit, l'hôte peut
     * le vider à l'ouverture d'un autre fichier).
     *
     * @return l'historique local, jamais null
     */
    public LocalHistory localHistory() {
        return localHistory;
    }

    /**
     * La carte de l'historique local (le harnais la lit).
     *
     * @return la carte, jamais null
     */
    public HistoryCard historyCard() {
        return historyCard;
    }

    /**
     * Ouvre la carte de l'historique local de la sélection (la ligne
     * du curseur sans sélection) : les traces qui touchent la plage, du
     * dernier au premier — l'histoire d'une partie du code.
     */
    public void showHistoryPopup() {
        int from = buffer.lineOfOffset(Math.min(selectionStart(), buffer.length()));
        int to = buffer.lineOfOffset(Math.max(selectionEnd(), selection.caret()));
        to = Math.min(to, buffer.getLineCount() - 1);
        historyCard.open(localHistory.between(from, to), from + 1, to + 1);
        invalidate();
    }

    /**
     * Restaure la ligne d'une trace passée : le texte de la ligne
     * d'alors remet à sa place — une unité d'annulation, la
     * restauration s'enregistre à son tour (elle se défait comme elle
     * s'est faite). La ligne morte au présent se borne au document.
     *
     * @param trace la trace à restaurer, jamais null
     */
    public void restoreHistoryLine(LocalHistory.Trace trace) {
        int lineCount = buffer.getLineCount();
        if (lineCount == 0 || trace == null) {
            return;
        }
        int line = Math.min(trace.line(), lineCount - 1);
        int lineStart = buffer.getLineStart(line);
        int lineEnd = buffer.getLineEnd(line);
        pendingEditLabel = "restauration";
        applyEdit(lineStart, lineEnd, trace.lineText());
    }

    /**
     * Une écriture du tampon laisse sa trace à l'historique local :
     * l'étiquette du geste en cours (collage, complétion…) ou le geste
     * par défaut (insertion, suppression, remplacement), la ligne
     * touchée, le texte de la ligne APRÈS le geste — la vue du passé
     * se lit par plage de lignes. L'étiquette se consomme au passage.
     */
    private void recordLocalTrace(int start, int removedLength, int insertedLength) {
        String gesture = pendingEditLabel;
        pendingEditLabel = null;
        if (gesture == null) {
            gesture = removedLength == 0 ? "insertion"
                    : insertedLength == 0 ? "suppression" : "remplacement";
        }
        if (buffer.length() == 0 && insertedLength == 0) {
            localHistory.record(System.currentTimeMillis(), gesture, 0, "");
            return;
        }
        int line = buffer.lineOfOffset(Math.min(start, buffer.length()));
        String lineText = "";
        if (buffer.getLineCount() > 0 && line < buffer.getLineCount()) {
            int lineStart = buffer.getLineStart(line);
            int lineEnd = buffer.getLineEnd(line);
            if (lineEnd > lineStart) {
                lineText = buffer.getText(lineStart, lineEnd).toString();
            }
        }
        localHistory.record(System.currentTimeMillis(), gesture, line, lineText);
    }

    /** Coupe la sélection : copie puis remplace par le vide. */
    private void cutSelection() {
        if (!hasSelection()) {
            return;
        }
        copySelection();
        applyEdit(selectionStart(), selectionEnd(), "");
    }

    /** Range la sélection (le curseur seul demeure). */
    private void clearSelection() {
        selection.setAnchor(-1);
        invalidate();
    }

    /** Pose le texte au presse-papiers du système, tronqué au plafond. */
    private void placeClipboard(String label, CharSequence text) {
        Object service = getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        if (!(service instanceof ClipboardManager manager)) {
            return;
        }
        String bounded = text.length() > MAX_CLIPBOARD_CHARS
                ? text.toString().substring(0, MAX_CLIPBOARD_CHARS)
                : text.toString();
        manager.setPrimaryClip(ClipData.newPlainText(label, bounded));
    }

    /**
     * Le texte du presse-papiers du système, ou null (vide, ou pas du
     * texte).
     */
    private String clipboardText() {
        Object service = getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        if (!(service instanceof ClipboardManager manager)) {
            return null;
        }
        ClipData clip = manager.getPrimaryClip();
        CharSequence text = clip == null || clip.getItemCount() == 0
                ? null : clip.getItemAt(0).getText();
        return text == null || text.length() == 0 ? null : text.toString();
    }

    /**
     * Le presse-papiers porte-t-il du texte ? La sonde s'amortit —
     * la lecture traverse la frontière du système, la barre ne la
     * paie pas à chaque trame (à la pose, aux actions de copie, et
     * par intervalle court pendant qu'elle vit).
     */
    private boolean clipboardHasText() {
        if (clipboardProbeFrame < 0
                || frameCount - clipboardProbeFrame > 15) {
            clipboardProbeFrame = frameCount;
            clipboardHasText = clipboardText() != null;
        }
        return clipboardHasText;
    }

    /**
     * Le bord haut de la sélection en pixels de vue (sa première
     * ligne visible — les plis ne la déplacent pas).
     */
    private float selectionTopPx() {
        int line = buffer.lineOfOffset(Math.min(selectionStart(), selectionEnd()));
        return getPaddingTop() + visibleIndexOf(line) * lineHeight - scrollY;
    }

    /**
     * Le bord bas de la sélection en pixels de vue (sa dernière
     * ligne, la hauteur comprise).
     */
    private float selectionBottomPx() {
        int end = Math.max(selectionStart(), selectionEnd());
        int line = buffer.lineOfOffset(Math.min(end, buffer.length()));
        return getPaddingTop() + (visibleIndexOf(line) + 1) * lineHeight - scrollY;
    }

    /**
     * Le centre horizontal de la sélection en pixels de vue : le
     * milieu de son étendue sur sa première ligne (le mot centré,
     * la sélection large ramène au milieu de la ligne).
     */
    private float selectionCenterX() {
        int start = Math.min(selectionStart(), selectionEnd());
        int end = Math.max(selectionStart(), selectionEnd());
        int line = buffer.lineOfOffset(start);
        int lineStart = buffer.getLineStart(line);
        int lineEnd = buffer.getLineEnd(line);
        int from = Math.max(start, lineStart) - lineStart;
        int to = Math.min(end, lineEnd) - lineStart;
        return getPaddingLeft() + (from + to) / 2.0f * charWidth;
    }

    /**
     * La barre de sélection, sa carte de débordement et sa carte
     * d'historique, par-dessus tout : posées sur la sélection,
     * re-posées à chaque trame — la sélection bouge, les cartes la
     * suivent.
     */
    private void drawSelectionBar(Canvas canvas) {
        if (!selectionBar.isShown() && !overflowCard.isShown()
                && !historyCard.isShown()) {
            return;
        }
        float top = selectionTopPx();
        float bottom = selectionBottomPx();
        float centerX = selectionCenterX();
        if (historyCard.isShown()) {
            historyCard.draw(canvas, selectionBar.poseY(), bottom, centerX,
                    popupMetrics());
        }
        if (overflowCard.isShown()) {
            overflowCard.draw(canvas, selectionBar.poseY(), bottom, centerX,
                    popupMetrics());
        }
        selectionBar.draw(canvas, top, bottom, centerX, popupMetrics());
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
     * La carte de signature : l'aide aux paramètres au caret — la
     * pose se recalcule au dessin, le caret la suit.
     */
    private void drawSignatureCard(Canvas canvas) {
        signatureCard.draw(canvas, popupMetrics());
    }

    /**
     * La carte de diagnostic : les erreurs et avertissements de la
     * ligne ancrée — posée sous la ligne (au-dessus sans la place),
     * bornée aux murs.
     */
    private void drawDiagnosticCard(Canvas canvas) {
        if (!diagnosticCard.isShown()) {
            return;
        }
        int line = Math.min(diagnosticCard.line(),
                Math.max(0, buffer.getLineCount() - 1));
        float top = getPaddingTop()
                + visibleIndexOf(line) * lineHeight - scrollY;
        diagnosticCard.draw(canvas, top, top + lineHeight, popupMetrics());
    }

    /**
     * La carte d'actions rapides : l'ampoule de la gouttière y ouvre
     * les actions de la ligne ancrée — posée sous la ligne
     * (au-dessus sans la place), bornée aux murs.
     */
    private void drawQuickActionCard(Canvas canvas) {
        if (!quickActionCard.isShown()) {
            return;
        }
        int line = Math.min(quickActionCard.line(),
                Math.max(0, buffer.getLineCount() - 1));
        float top = getPaddingTop()
                + visibleIndexOf(line) * lineHeight - scrollY;
        quickActionCard.draw(canvas, top, top + lineHeight, popupMetrics());
    }

    /**
     * La carte de complétion : les candidats du mot courant en rangées
     * verticales au caret — la pose se recalcule au dessin, le caret la
     * suit (le clavier délégué y navigue, le doigt y insère).
     */
    private void drawCompletionCard(Canvas canvas) {
        completionCard.draw(canvas, popupMetrics());
    }

    /**
     * Insère un candidat de complétion : le préfixe tapé se remplace
     * par le candidat entier — une seule unité d'annulation (la carte
     * et le clavier délégué y passent, l'hôte aussi).
     *
     * @param candidate le candidat à insérer, jamais vide
     */
    public void insertCompletion(String candidate) {
        if (candidate == null || candidate.isEmpty() || suggestionWordStart < 0) {
            return;
        }
        int end = Math.min(selection.caret(), buffer.length());
        if (end > suggestionWordStart) {
            pendingEditLabel = "complétion";
            applyEdit(suggestionWordStart, end, candidate);
        }
    }

    /**
     * Pose un texte au curseur (une unité d'annulation) : l'hôte y met
     * ce qu'il veut — une intention de son ampoule, un gabarit… ; la
     * sélection vivante se remplace.
     *
     * @param text le texte posé, null ou vide : rien
     */
    public void insertAtCaret(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        pendingEditLabel = "insertion";
        int start = hasSelection() ? selectionStart()
                : Math.min(selection.caret(), buffer.length());
        int end = hasSelection() ? selectionEnd() : start;
        applyEdit(start, end, text);
    }

    /**
     * Les candidats courants de la carte (le harnais les vérifie, l'hôte
     * peut les afficher ailleurs).
     *
     * @return les candidats du mot courant, jamais {@code null}
     */
    public List<String> suggestions() {
        return completionCard.words();
    }

    /**
     * La carte de complétion (null si jamais montrée — la carte naît à
     * la construction, le harnais la lit).
     *
     * @return la carte de complétion au caret
     */
    public CompletionCard completionCard() {
        return completionCard;
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
        int offset = Math.min(textDragging ? dropOffset : selection.caret(), buffer.length());
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
     * La loupe de précision : pendant le traînement d'une poignée OU du
     * traîné-déposé, la ligne de la borne traînée (du point de dépôt)
     * se redessine agrandie au-dessus du doigt — la colonne exacte se
     * lit, le doigt ne la cache pas. Le contenu se peint sous
     * homothétie dans une fenêtre découpée (la teinte de sélection, les
     * jetons, le curseur — la borne traînée EST le curseur, le point
     * de dépôt aussi), le cadre arrondi la borne par-dessus ; la loupe
     * ne vit que le temps du traînement.
     */
    private void drawLoupe(Canvas canvas) {
        if (selectionDragBound == 0 && !textDragging) {
            return;
        }
        if (textDragging && dropOffset < 0) {
            return;
        }
        int offset = Math.min(textDragging ? dropOffset : selection.caret(),
                buffer.length());
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
        android.graphics.Rect source = new android.graphics.Rect(
                (int) srcLeft, 0,
                (int) (srcLeft + width / LOUPE_SCALE), (int) lineHeight);
        android.graphics.RectF target = new android.graphics.RectF(
                left, top + dp(4), left + width, top + height - dp(4));
        canvas.drawBitmap(loupeLine, source, target, null);
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
                        top + height - dp(4), selectionRenderer.paint());
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
     * L'aperçu flottant du texte traîné : la première ligne de la
     * sélection — raccourcie à une vingtaine de colonnes — flotte
     * dans une pastille teintée SOUS le doigt (la loupe vit au-dessus
     * : elle porte OÙ le texte se pose, la pastille porte QUOI il
     * déplace). Les glyphes sont ceux du texte déjà rendu : le cache
     * du traîné reste chaud, la trame ne paie qu'un drawText court.
     */
    private void drawDragPreview(Canvas canvas) {
        if (!textDragging || !hasSelection()) {
            return;
        }
        int start = selectionStart();
        int firstLineEnd = Math.min(buffer.getLineEnd(buffer.lineOfOffset(start)),
                selectionEnd());
        String carried = buffer.getText(start, firstLineEnd).trim();
        if (carried.isEmpty()) {
            return;
        }
        String shown = carried.length() > 20
                ? carried.substring(0, 19) + "…" : carried;
        float chipWidth = shown.length() * charWidth + dp(16);
        float chipHeight = lineHeight + dp(8);
        float left = clampToRange(dragFingerX - chipWidth / 2.0f, getPaddingLeft(),
                Math.max(getPaddingLeft(), getWidth() - getPaddingRight() - chipWidth));
        float top = Math.min(dragFingerY + dp(10),
                Math.max(getPaddingTop(), getHeight() - getPaddingBottom() - chipHeight));
        canvas.drawRoundRect(left, top, left + chipWidth, top + chipHeight,
                dp(6), dp(6), selectionRenderer.paint());
        // le texte de la pastille : la couleur par défaut du thème (le
        // pinceau est partagé, ses drapeaux se posent avant l'usage)
        textPaint.setColor(theme.defaultColor());
        textPaint.setFakeBoldText(false);
        canvas.drawText(shown, left + dp(8), top + dp(4) - fontMetrics.top, textPaint);
    }

    /**
     * Le doigt du traîné tient-il une zone du bord (haut ou bas) ? Le
     * défilement s'y programme : le point de dépôt se lit dans la
     * fenêtre visible pendant que la vue glisse (le dépôt hors
     * fenêtre suivrait sinon).
     */
    private void scheduleEdgeScroll() {
        if (!textDragging || edgeScrollScheduled) {
            return;
        }
        float zone = dp(48);
        if (dragFingerY <= getPaddingTop() + zone
                || dragFingerY >= getHeight() - getPaddingBottom() - zone) {
            edgeScrollScheduled = true;
            postDelayed(edgeScroll, 16L);
        }
    }

    /**
     * Une échéance du défilement au bord : la vue glisse d'une
     * demi-ligne vers le doigt, le point de dépôt se relit à sa
     * nouvelle place — le doigt tient toujours la zone, la suite se
     * reprogramme ; le bord du document (la zone quittée) l'arrête.
     */
    private void runEdgeScroll() {
        edgeScrollScheduled = false;
        if (!textDragging) {
            return;
        }
        float zone = dp(48);
        float step = 0.0f;
        if (dragFingerY <= getPaddingTop() + zone) {
            step = -0.5f * lineHeight;
        } else if (dragFingerY >= getHeight() - getPaddingBottom() - zone) {
            step = 0.5f * lineHeight;
        }
        if (step == 0.0f) {
            return;
        }
        float before = scrollY;
        scrollY = clampToRange(scrollY + step, 0.0f, maxScroll());
        if (scrollY == before) {
            return;
        }
        dropOffset = offsetNearest(dragFingerX, dragFingerY);
        invalidate();
        scheduleEdgeScroll();
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
        selection.setCaret(offset);
        // un tap pose le curseur : la sélection se range
        selection.setAnchor(-1);
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
            pendingEditLabel = "déplacement";
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
        selection.setAnchor(-1);
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
        // ensuite la rouvre ; la barre de sélection naît de la
        // sélection — la documentation rapide s'y ouvre par le « i »
        selection.setAnchor(-1);
        caretTo(offset);
        if (wordEnd > wordStart) {
            selection.setAnchor(wordStart);
            selection.setCaret(wordEnd);
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
                showInfoPopup(word, provided, line, column);
                return;
            }
        }
        String description = "mot du texte";
        if (symbolIndex != null) {
            for (SymbolIndex.Symbol symbol : symbolIndex.symbols()) {
                if (symbol.name().equals(word)) {
                    description = symbol.nature() + " — déclaré ligne "
                            + (symbol.line() + 1);
                    break;
                }
            }
        }
        showInfoPopup(word, description, line, column);
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
        int line = buffer.lineOfOffset(Math.min(selection.caret(), buffer.length()));
        float top = getPaddingTop() + visibleIndexOf(line) * lineHeight - scrollY;
        float bottom = top + lineHeight;
        if (top < 0.0f) {
            scrollY += top;
        } else if (bottom > getHeight()) {
            scrollY += bottom - getHeight();
        }
        scrollY = clampToRange(scrollY, 0.0f, maxScroll());
    }

    /**
     * Le décalage de dépôt courant du traîné-déposé (-1 : hors geste).
     * Publique : le harnais y lit le point de dépôt pendant le
     * défilement au bord.
     *
     * @return le décalage de dépôt, -1 hors geste
     */
    public int harnessDropOffset() {
        return dropOffset;
    }

    /**
     * Première ligne visible (indice — les plis cachent des lignes).
     * Publique : le harnais y lit le défilement au bord du traîné.
     *
     * @return l'indice de la première ligne visible
     */
    public int firstVisibleLine() {
        if (lineHeight <= 0.0f) {
            return 0;
        }
        return lineOfVisibleIndex((int) (scrollY / lineHeight));
    }

    /**
     * Dernière ligne visible (indice, bornée au document). Publique :
     * le harnais y lit la fenêtre du point de dépôt.
     *
     * @return l'indice de la dernière ligne visible
     */
    public int lastVisibleLine() {
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
        selection.setCaret(Math.min(start + text.length(), buffer.length()));
        // toute édition directe réduit la sélection (le curseur porte
        // la fin, l'ancre ne survit pas à ce qu'elle désignait)
        selection.setAnchor(-1);
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
        if (selection.anchor() < 0) {
            return;
        }
        if (selection.anchor() <= start) {
            return;
        }
        if (selection.anchor() >= start + removedLength) {
            selection.setAnchor(selection.anchor() + insertedLength - removedLength);
        } else {
            selection.setAnchor(start);
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
        int start = Math.max(0, selection.caret() - leftLength);
        int end = Math.min(buffer.length(), selection.caret() + rightLength);
        if (end > start) {
            applyEdit(start, end, "");
        }
    }

    /** Déplace le curseur d'un caractère (les flèches du clavier). */
    void moveCaret(int delta) {
        caretTo(clampToRange(selection.caret() + delta, 0, buffer.length()));
    }

    /** Déplace le curseur d'une ligne, colonne conservée au plus près. */
    void moveCaretVertically(int lines) {
        int current = buffer.lineOfOffset(Math.min(selection.caret(), buffer.length()));
        int column = selection.caret() - buffer.getLineStart(current);
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
        if (selection.caret() > buffer.length()) {
            selection.setCaret(buffer.length());
        }
    }

    private void showSoftInput() {
        InputMethodManager manager =
                (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (manager != null) {
            manager.showSoftInput(this, 0);
        }
    }

    /**
     * Le clavier se lève pour la carte ouverte : la carte ne prend
     * jamais le focus — la vue le prend pour elle, le clavier
     * logiciel se montre, et le contrat d'entrée route les frappes
     * vers la carte (le champ visé s'y voit actif). L'hôte peut y
     * passer aussi : la carte EST le clavier de l'éditeur, le temps
     * qu'elle vit.
     */
    public void openKeyboard() {
        requestFocus();
        showSoftInput();
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

}
