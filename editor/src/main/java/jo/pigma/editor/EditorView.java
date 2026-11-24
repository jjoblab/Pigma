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

    /** Colonnes par niveau d'indentation (les guides de L2). */
    private static final int GUIDE_COLUMNS = 4;

    /** Plans partagés par contenu, au-delà le plus ancien s'évince. */
    private static final int SHARED_PLANS = 256;

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
    private SyntaxTheme theme = EditorThemes.LIGHT;
    private int themeBackground = EditorThemes.LIGHT_BACKGROUND;

    private int caretOffset;
    private int composingStart = -1;
    private int composingEnd = -1;

    /** L'ancre de sélection (-1 : pas de sélection, le curseur seul). */
    private int selectionAnchor = -1;

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

    private long frameCount;

    private final TextListener modelListener = new TextListener() {
        @Override
        public void textChanged(TextBuffer changed, int start, int removedLength,
                                int insertedLength, int revision) {
            shiftSelection(start, removedLength, insertedLength);
            clampCaret();
            shiftFolds(start);
            ensureCaretVisible();
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
     * scénario (la vue le fait d'elle-même à chaque langage).
     */
    public void resetFrameStats() {
        worstFrameNanos = 0L;
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
        // autre document : la sélection n'y désigne plus rien
        selectionAnchor = -1;
        try (TextTransaction transaction = undoStack.newTransaction()) {
            transaction.replace(0, buffer.length(), text);
        }
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
                dragging = false;
                break;
            case MotionEvent.ACTION_MOVE:
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

    @Override
    protected void onDraw(Canvas canvas) {
        long frameBegin = System.nanoTime();
        lastFrameEngineNanos = 0L;
        super.onDraw(canvas);
        canvas.drawColor(themeBackground);
        int count = buffer.getLineCount();
        int first = firstVisibleLine();
        int last = lastVisibleLine();
        updateStructureCaches();
        // la bande de portée se peint avant le texte (le texte se
        // dessine dessus), la sélection par-dessus elle, les guides et
        // la paire après
        drawScopeBand(canvas, first, last, count);
        drawSelection(canvas, first, last, count);
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
        // les candidats AVANT leur dessin (un déplacement de curseur
        // rend ses candidats dans la même trame), les soulignés de
        // diagnostic par-dessus le texte, le bandeau par-dessus tout
        refreshAnalysis();
        updateSuggestions();
        drawDiagnostics(canvas, first, last, count);
        drawSuggestions(canvas);
        // la fenêtre visible est payée : le reste remplit au fil de fond
        ensureFill();
        lastFrameNanos = System.nanoTime() - frameBegin;
        worstFrameNanos = Math.max(worstFrameNanos, lastFrameNanos);
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
        suggestions = symbolIndex == null
                ? List.of() : symbolIndex.query(prefix, 8);
        suggestionWordStart = wordStart;
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
        int offset = Math.min(caretOffset, buffer.length());
        int line = buffer.lineOfOffset(offset);
        if (line < firstVisibleLine() || line > lastVisibleLine() || isHidden(line)) {
            return;
        }
        int column = offset - buffer.getLineStart(line);
        float x = getPaddingLeft() + column * charWidth;
        float top = getPaddingTop() + visibleIndexOf(line) * lineHeight - scrollY;
        canvas.drawRect(x, top + dp(2), x + caretWidth, top + lineHeight - dp(2), caretPaint);
    }

    private void placeCaretAt(float x, float y) {
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
        caretOffset = buffer.offsetOfPosition(line, column);
        // un tap pose le curseur : la sélection se range
        selectionAnchor = -1;
        invalidate();
    }

    /**
     * Sélectionne le mot sous le doigt : les bornes du mot se lisent
     * dans la fenêtre de la ligne (jamais le document pour un mot).
     */
    private void selectWordAt(float x, float y) {
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
        int offset = buffer.offsetOfPosition(line, column);
        int wordStart = wordBoundsStart(offset, lineStart);
        int wordEnd = wordBoundsEnd(offset, lineEnd);
        // le curseur d'abord (le poser range la sélection), l'ancre
        // ensuite la rouvre
        selectionAnchor = -1;
        caretTo(offset);
        if (wordEnd > wordStart) {
            selectionAnchor = wordStart;
            caretOffset = wordEnd;
            invalidate();
        }
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
