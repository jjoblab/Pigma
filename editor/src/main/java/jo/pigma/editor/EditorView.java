package jo.pigma.editor;

import jo.pigma.editor.folding.FoldBounds;
import jo.pigma.editor.folding.FoldModel;
import jo.pigma.editor.internal.layout.LayoutModel;
import jo.pigma.editor.folding.StructureGuides;
import jo.pigma.editor.folding.StructureGuides.Guide;
import jo.pigma.editor.folding.WrapModel;
import jo.pigma.editor.gutter.Gutter;
import jo.pigma.editor.gutter.Minimap;
import jo.pigma.editor.input.EditorContract;
import jo.pigma.editor.input.EditorInputConnection;
import jo.pigma.editor.selection.SelectionRenderer;
import jo.pigma.editor.selection.SelectionState;


import jo.pigma.editor.popups.KeyboardCard;
import jo.pigma.editor.popups.GotoBar;
import jo.pigma.editor.popups.HistoryCard;
import jo.pigma.editor.popups.UsageCard;
import jo.pigma.editor.popups.ColorPickerCard;
import jo.pigma.editor.popups.InfoPopup;
import jo.pigma.editor.popups.LocalHistory;
import jo.pigma.editor.popups.DiagnosticCard;
import jo.pigma.editor.popups.ActionRegistry;
import jo.pigma.editor.popups.BulbIcon;
import jo.pigma.editor.popups.BadgePop;
import jo.pigma.editor.popups.PopupStyle;
import jo.pigma.editor.popups.QuickActionCard;
import jo.pigma.editor.popups.RenameCard;
import jo.pigma.editor.render.RenderCache;
import jo.pigma.editor.render.LinePlan;
import jo.pigma.editor.internal.render.Painter;
import jo.pigma.editor.internal.search.SearchController;
import jo.pigma.editor.internal.scroll.ScrollController;
import jo.pigma.editor.internal.touch.PinchController;
import jo.pigma.editor.internal.touch.TouchController;
import jo.pigma.editor.popups.PopupMetrics;
import jo.pigma.editor.popups.PopupHost;
import jo.pigma.editor.popups.ProblemsCard;
import jo.pigma.editor.popups.ClipboardCard;
import jo.pigma.editor.popups.ClipboardHistory;
import jo.pigma.editor.popups.StructureCard;
import jo.pigma.editor.popups.TargetCard;
import jo.pigma.editor.popups.SearchBar;
import jo.pigma.editor.popups.CompletionCard;
import jo.pigma.editor.popups.OverflowCard;
import jo.pigma.editor.popups.SelectionBar;
import jo.pigma.editor.popups.SignatureCard;
import jo.pigma.lsp.LspFoldingRange;
import jo.pigma.lsp.LspInlayHint;
import jo.pigma.lsp.LspRange;
import jo.pigma.lsp.LspSemanticToken;

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
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;

import jo.pigma.core.TextBuffer;
import jo.pigma.core.TextListener;
import jo.pigma.core.TextTransaction;
import jo.pigma.core.UndoStack;
import jo.pigma.syntax.BackgroundFill;
import jo.pigma.syntax.LanguageKeywords;
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

    private static final String INITIAL_TEXT = "";

    private static final int CARET_COLOR = 0xFF2962FF;

    /** Colonnes au-delà desquelles le dessin d'une ligne s'arrête. */
    private static final int MAX_DRAWN_COLUMNS = 10_000;

    /** L'agrandissement de la loupe (la colonne exacte se lit au doigt). */
    private static final float LOUPE_SCALE = 2.0f;

    /** Colonnes par niveau d'indentation (les guides de L2). */
    private static final int GUIDE_COLUMNS = 4;

    /**
     * L'ombre de structure des guides (l'alpha porte la discrétion —
     * le guide aligne, il ne lit pas : l'encre du texte reste seule
     * à parler).
     */
    private static final int GUIDE_SHADOW_COLOR = 0x2B808080;

    /**
     * L'encre des guides ACTIFS : l'encre du curseur à 80 pour cent
     * (lisible sur tout fond — jamais pâle : le guide de la portée
     * courante se lit d'un coup d'œil, quel que soit l'endroit des
     * accolades).
     */
    private static final int GUIDE_ACCENT_COLOR = 0xCC2962FF;

    /** Plans partagés par contenu, au-delà le plus ancien s'évince. */
    private static final int SHARED_PLANS = 256;

    /** La durée de vie de la pastille d'erreur du tap (B4). */
    private static final long BADGE_LIFETIME_NANOS = 3_000_000_000L;

    /** La même en millisecondes, pour l'invalidation différée. */
    private static final long BADGE_LIFETIME_MS = 3_000L;

    /**
     * Le plafond de correspondances collectées par une recherche : le
     * document entier reste parcouru (le compte s'arrête seulement) —
     * un document piège ne remplit pas la mémoire. Le plafond vit au
     * chercheur ({@link SearchController#MATCH_LIMIT}).
     */
    private static final int SEARCH_MATCH_LIMIT = SearchController.MATCH_LIMIT;

    private final TextBuffer buffer;
    private final UndoStack undoStack;
    /** Le peintre du corps des lignes (le composant de DESSIN du
     * découpage — R1 : les glyphes du plan cuit, les chips, les
     * ligatures, le placeholder, le soulignement de composition). */
    private Painter painter;

    /** Le pinceau du texte, PARTAGÉ avec le peintre (la loupe et la
     * mesure y lisent le même objet — un seul réglage). */
    private Paint textPaint;
    private final Paint caretPaint = new Paint();
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

    /** Le pincement (la première brique du contrôleur des gestes —
     * R1 : l'écart des doigts porte le facteur, le milieu porte le
     * foyer, la ligne sous le foyer y demeure). */
    private PinchController pinch;

    private SyntaxDocument syntax;
    private LanguageSyntax language;
    private SyntaxTheme theme = EditorThemes.LIGHT;
    private int themeBackground = EditorThemes.LIGHT_BACKGROUND;

    private int composingStart = -1;
    private int composingEnd = -1;

    /** L'état de sélection — le curseur et l'ancre au paquet selection/. */
    private final SelectionState selection = new SelectionState();

    /** Le chercheur (le composant du découpage — R1 : la requête, les
     * correspondances, le cycle qui boucle). */
    private SearchController search;

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

    /**
     * Les lignes en point d'arrêt (la gouttière les pose et les
     * retire au doigt — l'hôte y lira et y branchera son
     * débogueur). Trié : le dessin et la lecture y vont en ordre.
     */
    private final TreeSet<Integer> breakpoints = new TreeSet<>();

    /**
     * Les régions repliables du serveur de langage (le protocole
     * foldingRange) : les bornes composées s'y lisent par-dessus
     * celles du lexer — la gouttière y porte leurs marqueurs.
     */
    private List<LspFoldingRange> serverFolds = List.of();

    /**
     * Les indices posés dans le texte par le serveur (le protocole
     * inlayHint) : l'étiquette se dessine au bout du contenu de la
     * ligne, l'encre secondaire — le texte n'en bouge pas.
     */
    private List<LspInlayHint> inlayHints = List.of();

    /**
     * Les jetons sémantiques du serveur (le protocole
     * semanticTokens) : la couche du protocole au-dessus de la
     * coloration locale — le jeton couvrant un span y porte sa
     * couleur (le genre à la palette), la déclaration y porte le
     * gras.
     */
    private List<LspSemanticToken> serverSemantics = List.of();

    /** Les bornes des plis composées : le lexer d'abord, le serveur par-dessus. */
    private final FoldComposite foldBounds = new FoldComposite();

    /** Le doigt est-il tombé sur une carte ouverte ? — le geste entier lui revient. */
    private boolean downOnCard;

    /** Le doigt est-il tombé sur la popup d'info ? — le défilement du corps lui revient. */
    private boolean downOnInfo;

    /** Le nom du candidat dont la documentation VOLE (la garde anti-tempête). */
    private String pendingDocName;

    /** Le soft wrap vit-il (les lignes coupent à la largeur — la leçon de la référence). */
    private boolean softWrap;

    /** Le modèle du retour à la ligne doux (les rangées visuelles par ligne). */
    private final WrapModel wrapModel = new WrapModel();

    /**
     * Pose le RETOUR À LA LIGNE DOUX (la leçon de la référence : ses
     * lignes coupent à la largeur de la vue, la suite s'indente) :
     * le recalcul y suit (le plafond de colonnes naît de la largeur
     * et du pas de caractère), le scroll horizontal se range (les
     * lignes ne dépassent plus), l'hôte y trouve sa bascule.
     *
     * @param on vrai pour couper les lignes à la largeur
     */
    public void setSoftWrap(boolean on) {
        if (softWrap == on) {
            return;
        }
        softWrap = on;
        if (on) {
            rebuildWrapModel();
        }
        scrollX = 0.0f;
        clampCaret();
        ensureCaretVisible();
        invalidate();
    }

    /** Le soft wrap vit-il ? (le harnais le lit) */
    public boolean isSoftWrap() {
        return softWrap;
    }

    /**
     * Le modèle du wrap se recalcule : le plafond de colonnes naît de
     * la largeur du texte (la vue moins ses paddings), les longueurs
     * et indents se lisent au tampon PAR TRANCHES (jamais le document
     * entier d'un coup — les fenêtres du gros fichier restent bornées).
     */
    private void rebuildWrapModel() {
        if (!softWrap || charWidth <= 0.0f) {
            return;
        }
        int columns = (int) ((getWidth() - getPaddingLeft() - getPaddingRight())
                / charWidth);
        int count = buffer.getLineCount();
        int[] lengths = new int[count];
        int[] indents = new int[count];
        for (int line = 0; line < count; line++) {
            int start = buffer.getLineStart(line);
            int end = buffer.getLineEnd(line);
            lengths[line] = end - start;
            int indent = 0;
            while (start + indent < end
                    && buffer.charAt(start + indent) == ' ') {
                indent++;
            }
            indents[line] = indent;
        }
        wrapModel.rebuild(lengths, indents, columns);
        layout.rebuildWrap(softWrap, getWidth() - getPaddingLeft()
                - getPaddingRight());
    }

    /**
     * L'ÉTAT DU TOUCHER (le composant du découpage — R1, la suite du
     * pincement) : QUI possède le contact. L'appui naît puis GRANDIT —
     * tap, balayage, appui long, poignée de sélection, traîné-déposé,
     * carte du document tenue. La MÉMOIRE y vit (l'horodatage de
     * l'appui, celui du dernier tap, le point de départ, la borne
     * traînée, le point de dépôt, le doigt vivant — la loupe et
     * l'aperçu flottant le suivent), la GÉOMÉTRIE demeure à la vue
     * (où tombe un doigt : quel offset, quelle ligne, quel marqueur)
     * — le contrôleur y tient les verdicts : l'appui a-t-il durée,
     * le tap en suit-il un autre, le doigt a-t-il franchi le seuil,
     * le lâcher vaut-il un tap plat.
     */
    private final TouchController touch = new TouchController();

    private float scrollY;

    /**
     * Le décalage horizontal du texte (le pincement l'agrandit, la
     * ligne longue s'y révèle — le traîné de sélection y défile
     * aussi). Positif quand le texte défile à gauche.
     */
    private float scrollX;

    /** Le balayeur aux bords (le composant du découpage — R1 : le
     * traîné tenu au bord y fait glisser la vue, échéance après
     * échéance). */
    private ScrollController edgeScroller;

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

    /** La carte des usages — les références d'un symbole. */
    private UsageCard usageCard;

    /** La carte du sélecteur de couleur — le littéral #RRGGBB au caret. */
    private ColorPickerCard colorPickerCard;

    /** La carte de diagnostic — les erreurs et avertissements d'une ligne. */
    private DiagnosticCard diagnosticCard;

    /**
     * La pastille d'erreur du tap (B4) : le doigt masque le texte
     * visé — un tap sur un mot diagnostiqué y pose l'AMPOULE
     * AU-DESSUS du mot (le retour du terrain à la leçon de la
     * référence : la gouttière porte déjà LE marqueur du diagnostic,
     * la pastille n'y parle plus de la ligne, elle parle du mot), le
     * tap sur l'ampoule ouvre la carte. Elle vit trois secondes et
     * meurt au geste qui la déplace.
     */
    private long diagnosticBadgeUntilNanos;
    private int diagnosticBadgeLine = -1;
    /** La colonne du MOT diagnostiqué (le diagnostic y survit à la
     * re-construction — la pastille s'y centre). */
    private int diagnosticBadgeColumn;

    /** L'encre de l'ampoule de la pastille (au trait, à l'accent —
     * LA MÊME que l'étage des correctifs de la carte : le tap y
     * cherche la correction, l'ampoule y suit). */
    private final Paint badgeBulbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    /** Le pop de naissance de la pastille (le ressort de la référence :
     * l'échelle part de la moitié, ancrée à la ligne — elle monte vers
     * le mot, l'opacité suit). */
    private final BadgePop badgePop = new BadgePop();

    /** La carte d'actions rapides — l'ampoule de la gouttière. */
    private QuickActionCard quickActionCard;

    /**
     * La carte du CHOIX DE CIBLES (le choix de cibles — l'aller-à à plusieurs
     * résultats) : paresseuse — le serveur de langage l'ouvre.
     */
    private TargetCard targetCard;

    /** La carte de la LISTE DES PROBLÈMES (la liste des problèmes) — paresseuse. */
    private ProblemsCard problemsCard;

    /**
     * La carte de l'HISTORIQUE DU PRESSE-PAPIERS (l'historique du presse-papiers) et sa
     * mémoire — paresseuse.
     */
    private ClipboardCard clipboardCard;

    /** La mémoire des copies (l'historique du presse-papiers — plafonnée, dédupliquée). */
    private final ClipboardHistory clipboardHistory = new ClipboardHistory();

    /**
     * La carte de la STRUCTURE DU FICHIER (la structure du fichier — la table des
     * matières, le filtre à la frappe) : paresseuse — le serveur de
     * langage l'ouvre.
     */
    private StructureCard structureCard;

    /**
     * LE GESTIONNAIRE DES POPUPS (D-P4 du rapport : 23 fermetures
     * manuelles, l'exclusion mutuelle codée à la main, le popup
     * fantôme d'une carte oubliée d'une liste de fermeture). Toute
     * popup de la vue s'y inscrit avec son rôle — l'ouverture
     * EXCLUSIVE (une carte flottante à la fois), le changement de
     * contexte qui range TOUT, l'annonce des ouvertures (la source
     * unique de l'accessibilité).
     */
    private final PopupHost popups = new PopupHost();

    /** Le style des popups posé par l'hôte (fond translucide, tailles, forme). */
    private PopupStyle popupStyle = PopupStyle.defaults();

    /**
     * LE REGISTRE DES ACTIONS D'ÉDITION (R-A du rapport des popups) :
     * la carte d'actions rapides et l'étage « Édition » du
     * débordement servaient les MÊMES cinq actions, chacune SA liste —
     * la dérive y était garantie (une action ajoutée à l'une manquait
     * à l'autre). La définition vit ICI, UNE SEULE FOIS (le libellé,
     * la course, la disponibilité — le commentaire exige un langage
     * qui commente, la casse une sélection vivante) ; les DEUX cartes
     * y mènent leurs rangées.
     */
    private final ActionRegistry editActions = new ActionRegistry()
            .add("Commenter / décommenter", this::toggleLineComment,
                    () -> !lineCommentPrefix().isEmpty())
            .add("En majuscules", this::uppercaseSelection, this::hasSelection)
            .add("En minuscules", this::lowercaseSelection, this::hasSelection)
            .add("Dupliquer la ligne", this::duplicateLine, () -> true)
            .add("Supprimer la ligne", this::deleteLines, () -> true);

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

    /** La minimap — la carte du document au bord droit, ses pastilles d'annulation en tête. */
    private Minimap minimap;

    /** La minimap vit-elle à l'écran (l'hôte la retire quand son écran n'en veut pas). */
    private boolean minimapEnabled = true;

    /** Le pont d'accessibilité (le service qui parle y lit la vue — D5). */
    private final jo.pigma.editor.accessibility.EditorAccessibility accessibility =
            jo.pigma.editor.accessibility.EditorAccessibility.of(this);

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
    /**
     * La géométrie texte EXTRAITE de la vue (le découpage) : la
     * correspondance offset ↔ pixel, les plis, la coupe et les bornes
     * de défilement y vivent, testés en JVM — la vue ne garde que le
     * câblage et les pinceaux. Né au CONSTRUCTEUR (le tampon et les
     * plis y sont prêts).
     */
    private final LayoutModel layout;

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

    /** Le fil de la paire d'accolades (la jambe et son coude). */
    private final Paint pairLinkPaint = new Paint();

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

    /**
     * L'encre des indices du serveur (l'étiquette au bout de la ligne
     * — le texte n'en bouge pas, l'encre secondaire la dessine).
     */
    private final Paint inlayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

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

    /**
     * L'ancre de mémoire libre : relue à une trame sur
     * {@link #MEMORY_ANCHOR_INTERVAL} (la trame ordinaire ne paie
     * plus la lecture, la pire s'y compare).
     */
    private long memoryAnchor;

    /** L'intervalle d'échantillonnage de l'ancre mémoire (puissance de deux). */
    private static final int MEMORY_ANCHOR_INTERVAL = 32;

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
            if (search != null) {
                search.forget();
            }
            // LE WRAP suit l'édition : la ligne coupée d'avant peut
            // s'allonger ou raccourcir — le recalcul suivra (borné aux
            // fenêtres, jamais le document pour une frappe)
            if (softWrap) {
                rebuildWrapModel();
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
        layout = new LayoutModel(buffer, folds);
        syntax = SyntaxDocument.open(buffer, Languages.JAVA);
        structure = StructureDocument.open(buffer, syntax);
        semantic = SemanticDocument.open(buffer, syntax, structure,
                new JavaDeclarations());
        backgroundFill = BackgroundFill.start(syntax);
        // LE PEINTRE DU CORPS DES LIGNES (le découpage — R1) : la vue
        // partage son pinceau de texte (la loupe et la mesure y
        // lisent le même objet — un seul réglage)
        painter = new Painter(dp(1));
        textPaint = painter.textPaint();
        painter.applyTheme(theme.defaultColor());
        // LE CHERCHEUR (le composant du découpage — R1) : la vue y
        // branche le texte, le curseur et l'aller-à (la teinte et les
        // remplacements y lisent le chercheur directement)
        search = new SearchController(new SearchController.Host() {
            @Override
            public CharSequence documentText() {
                return buffer.getText(0, buffer.length());
            }

            @Override
            public int caret() {
                return selection.caret();
            }

            @Override
            public void revealMatch(int start, int queryLength) {
                selection.setAnchor(-1);
                selection.setCaret(Math.min(start + queryLength,
                        buffer.length()));
                ensureCaretVisible();
            }

            @Override
            public void repaint() {
                invalidate();
            }
        });
        // LE BALAYEUR AUX BORDS (le composant du découpage — R1) :
        // le traîné tenu au bord y fait glisser la vue
        edgeScroller = new ScrollController(new ScrollController.Host() {
            @Override
            public boolean isTextDragging() {
                return touch.isTextDragging();
            }

            @Override
            public float fingerY() {
                return touch.fingerY();
            }

            @Override
            public float viewportTop() {
                return getPaddingTop();
            }

            @Override
            public float viewportBottom() {
                return getHeight() - getPaddingBottom();
            }

            @Override
            public float lineHeight() {
                return lineHeight;
            }

            @Override
            public float maxScrollY() {
                return maxScroll();
            }

            @Override
            public float scrollY() {
                return scrollY;
            }

            @Override
            public void setScrollY(float value) {
                scrollY = value;
            }

            @Override
            public void recomputeDrop() {
                touch.dropAt(offsetNearest(touch.fingerX(), touch.fingerY()));
            }

            @Override
            public void repaint() {
                invalidate();
            }

            @Override
            public void postDelayed(Runnable tick, long delayMs) {
                EditorView.this.postDelayed(tick, delayMs);
            }

            @Override
            public void removeCallbacks(Runnable tick) {
                EditorView.this.removeCallbacks(tick);
            }
        });
        // LE PINCEMENT (la première brique du contrôleur des gestes —
        // R1) : l'écart des doigts porte le facteur, le milieu le
        // foyer
        pinch = new PinchController(new PinchController.Host() {
            @Override
            public float textPx() {
                return textPx;
            }

            @Override
            public void applyTextSize(float freshPx) {
                EditorView.this.applyTextSize(freshPx);
            }

            @Override
            public float lineHeight() {
                return lineHeight;
            }

            @Override
            public float charWidth() {
                return charWidth;
            }

            @Override
            public float viewportTop() {
                return getPaddingTop();
            }

            @Override
            public float viewportLeft() {
                return getPaddingLeft();
            }

            @Override
            public float scrollY() {
                return scrollY;
            }

            @Override
            public void setScrollY(float value) {
                scrollY = value;
            }

            @Override
            public float maxScrollY() {
                return maxScroll();
            }

            @Override
            public float scrollX() {
                return scrollX;
            }

            @Override
            public void setScrollX(float value) {
                scrollX = value;
            }

            @Override
            public float maxScrollX() {
                return EditorView.this.maxScrollX();
            }

            @Override
            public void takeFocus() {
                requestFocus();
            }

            @Override
            public void settleGestures() {
                touch.settle();
                dismissInfoPopup();
            }

            @Override
            public void touchRestartedAt(float x, float y) {
                touch.restartAt(x, y);
            }

            @Override
            public void forgetLastTap() {
                touch.forgetTap();
            }

            @Override
            public void repaint() {
                invalidate();
            }
        });
        applyTextSize(14.0f * getResources().getDisplayMetrics().scaledDensity);
        caretPaint.setColor(CARET_COLOR);
        bracketPaint.setStyle(Paint.Style.STROKE);
        bracketPaint.setStrokeWidth(dp(1) * 1.5f);
        bracketPaint.setColor(CARET_COLOR);
        // le fil de la paire : l'encre du curseur, un peu transparent
        // (il guide l'œil sans crier sur le texte)
        pairLinkPaint.setStyle(Paint.Style.STROKE);
        pairLinkPaint.setColor((CARET_COLOR & 0x00FFFFFF) | 0x66000000);
        guidePaint.setStrokeWidth(dp(1));
        guidePaint.setColor(GUIDE_SHADOW_COLOR);
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
        inlayPaint.setColor(0xFF8A8A93);
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
        usageCard = new UsageCard(dp(1));
        usageCard.bind(this);
        colorPickerCard = new ColorPickerCard(dp(1));
        diagnosticCard = new DiagnosticCard(dp(1));
        quickActionCard = new QuickActionCard(dp(1));
        registerPopups();
        gutter = new Gutter(dp(1));
        // le panneau de la gouttière part à la teinte du fond courant
        gutter.setPanelColor(themeBackground);
        // la minimap au bord droit : la carte du document, sa BARRE
        // d'annulation/rétablissement en tête (la pile y se branche)
        minimap = new Minimap(dp(1));
        minimap.setUndoRedoActions(new Minimap.UndoRedoActions() {
            @Override
            public boolean canUndo() {
                return undoStack.canUndo();
            }

            @Override
            public boolean canRedo() {
                return undoStack.canRedo();
            }

            @Override
            public void undo() {
                EditorView.this.undo();
            }

            @Override
            public void redo() {
                EditorView.this.redo();
            }
        });
        inputContract = createInputContract();
        trackedLineCount = buffer.getLineCount();
        buffer.addListener(modelListener);
        int padding = dp(12);
        // la gouttière élargie : la colonne des marqueurs (erreur,
        // avertissement, ampoule) vit AVANT les numéros, puis LA
        // COLONNE DES PLIS à la référence (EditorGutterComponentImpl y
        // pose ses chevrons APRÈS les numéros, dans le panneau —
        // jamais dans le corps du texte)
        // le padding droit reste léger : la minimap et sa BARRE de
        // pastilles FLOTTENT au-dessus du texte (à la référence — la
        // carte y recouvre le code, translucide) ; les lignes longues
        // peuvent glisser dessous, le débordement se calcule au texte
        // le panneau de la gouttière, à la référence : les marqueurs
        // (0-22), les numéros (finissent à -28), la COLONNE DES PLIS
        // (-14, DANS le panneau — jamais sur le filet), l'aperçu
        // @Preview à -38 — le tout AVANT le filet du bord, l'ombre
        // d'élévation ne touche que le texte
        setPadding(dp(78), dp(16), dp(16), dp(16));
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
     * Le facteur de zoom de la gouttière (le harnais le lit) : le
     * corps des numéros suit le pincement — facteur 1 au corps de
     * repos de 14 sp.
     *
     * @return le facteur de zoom courant de la gouttière
     */
    public float gutterTextZoom() {
        return gutter.textZoom();
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
        // la largeur se reborne aussi (le corps qui revient resserre
        // le débordement : le scrollX d'avant ne vaut plus)
        scrollX = clampToRange(scrollX, 0.0f, maxScrollX());
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
        // le peintre suit le corps (le pinceau partagé y gagne sa
        // taille, le placeholder la sienne)
        painter.applyTextSize(clamped);
        inlayPaint.setTextSize(clamped * 0.85f);
        // la gouttière suit le zoom : les numéros s'agrandissent au
        // pincement comme le corps (le facteur porte le corps courant
        // sur le corps de repos de 14 sp)
        if (gutter != null) {
            gutter.setTextZoom(clamped / (14.0f * scaledDensity));
        }
        fontMetrics = textPaint.getFontMetrics();
        charWidth = textPaint.measureText("M");
        lineHeight = (float) Math.ceil(fontMetrics.bottom - fontMetrics.top) + dp(2);
        caretWidth = dp(2);
        // les mesures NEUVES nourrissent la géométrie extraite ET le
        // peintre (après la mesure, pas avant : ils liraient les
        // anciennes)
        painter.applyMeasures(charWidth, fontMetrics.ascent,
                fontMetrics.descent);
        syncLayoutMetrics();
        renderCache.invalidate();
        scrollY = clampToRange(scrollY, 0.0f, maxScroll());
        scrollX = clampToRange(scrollX, 0.0f, maxScrollX());
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
        pinch.engage(pinchSpan(event),
                (event.getX(0) + event.getX(1)) / 2.0f,
                (event.getY(0) + event.getY(1)) / 2.0f);
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
        pinch.move(pinchSpan(event),
                (event.getX(0) + event.getX(1)) / 2.0f,
                (event.getY(0) + event.getY(1)) / 2.0f);
    }

    /**
     * Le pincement se dénoue : un doigt s'en va, l'autre peut
     * glisser — son appui repart de sa position vivante (le
     * défilement n'y saute pas).
     *
     * @param event l'événement du doigt qui s'en va
     */
    private void endPinch(MotionEvent event) {
        int staying = 1 - event.getActionIndex();
        if (staying >= 0 && staying < event.getPointerCount()) {
            pinch.end(event.getX(staying), event.getY(staying));
        } else {
            pinch.end(Float.NaN, Float.NaN);
        }
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
        usageCard.setStyle(popupStyle);
        colorPickerCard.setStyle(popupStyle);
        diagnosticCard.setStyle(popupStyle);
        quickActionCard.setStyle(popupStyle);
        gutter.setStyle(popupStyle);
        // la pastille : l'ampoule au trait à l'ACCENT — la même encre
        // que l'étage des correctifs de la carte (le retour du terrain
        // : le petit icône du trait diagnostiqué porte LA MÊME ampoule
        // que la carte), le trait au même rapport
        badgeBulbPaint.setColor(popupStyle.accentArgb());
        badgeBulbPaint.setStyle(Paint.Style.STROKE);
        badgeBulbPaint.setStrokeCap(Paint.Cap.ROUND);
        badgeBulbPaint.setStrokeJoin(Paint.Join.ROUND);
        badgeBulbPaint.setStrokeWidth(dp(18) * (1.4f / (22.0f * 0.72f)));
        if (renameBar != null) {
            renameBar.setStyle(popupStyle);
        }
        if (searchBar != null) {
            searchBar.setStyle(popupStyle);
        }
        if (gotoBar != null) {
            gotoBar.setStyle(popupStyle);
        }
        if (targetCard != null) {
            targetCard.setStyle(popupStyle);
        }
        if (problemsCard != null) {
            problemsCard.setStyle(popupStyle);
        }
        if (clipboardCard != null) {
            clipboardCard.setStyle(popupStyle);
        }
        if (structureCard != null) {
            structureCard.setStyle(popupStyle);
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

        /**
         * La plage d'un diagnostic du serveur sur la ligne (le tap
         * précis s'y éprouve : le doigt sur LE SOULIGNÉ ouvre la
         * carte, pas sur la ligne entière — la leçon de la référence,
         * son findHighlightByOffset à l'offset exact).
         *
         * @param line   l'indice de la ligne
         * @param column la colonne du doigt
         * @return vrai quand un diagnostic du serveur couvre la colonne
         */
        default boolean diagnosticCovers(int line, int column) {
            return false;
        }
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
                            DiagnosticCard.SEVERITY_ERROR, local.message(),
                            localFixesOf(local, lineStart, lineEnd)));
                }
            }
        }
        // L'ÉTAGE DES CORRECTIFS (la leçon de la référence : sa popup
        // d'erreur porte le message ET ses correctifs DANS la même
        // carte) : les correctifs du serveur de langage descendent à
        // CHAQUE diagnostic de la ligne — le premier diagnostic les
        // porte (l'étage se dessine sous ses messages, le tap
        // l'exécute) ; la réponse asynchrone du serveur rouvre la
        // carte ouverte (le cache du fournisseur vit dans la façade)
        if (!found.isEmpty() && serverQuickActionProvider != null) {
            List<QuickActionCard.Entry> served =
                    serverQuickActionProvider.quickActionsAt(line, 0);
            if (served != null && !served.isEmpty()) {
                List<DiagnosticCard.Fix> fixes = new ArrayList<>();
                for (QuickActionCard.Entry action : served) {
                    if (action.label() != null
                            && action.label().startsWith("Correctif : ")) {
                        fixes.add(new DiagnosticCard.Fix(
                                action.label().substring("Correctif : ".length()),
                                action.action()));
                    }
                }
                if (!fixes.isEmpty()) {
                    found.set(0, new DiagnosticCard.Entry(
                            found.get(0).severity(),
                            found.get(0).message(), fixes));
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
     * LES CORRECTIFS LOCAUX d'un diagnostic de l'analyse (la leçon de
     * la référence : son analyseur IntelliJ accompagne CHAQUE erreur
     * de ses quickfixes — la popup d'erreur y porte l'étage des
     * correctifs, l'ampoule n'est qu'un autre chemin). Le fermant
     * orphelin se SUPPRIME ; l'ouvrant non fermé se REFERME (le
     * fermant apparié s'insère à la fin de la ligne — l'éditeur y
     * pose une édition actionnable, SANS serveur de langage).
     *
     * @param local le diagnostic local (sa plage et son message)
     * @param lineStart le début de la ligne du diagnostic
     * @param lineEnd   la fin de la ligne du diagnostic
     * @return les correctifs, jamais null (vide : aucun ne se propose)
     */
    private List<DiagnosticCard.Fix> localFixesOf(
            DiagnosticsDocument.Diagnostic local, int lineStart, int lineEnd) {
        List<DiagnosticCard.Fix> fixes = new ArrayList<>();
        String message = local.message();
        if (message.startsWith("fermant orphelin")) {
            // « fermant orphelin : « } » sans ouvrant » — le signe se
            // lit entre les guillemets du message, sa suppression
            // guérit la ligne
            int open = message.lastIndexOf('\u00AB');
            int close = message.lastIndexOf('\u00BB');
            if (open >= 0 && close > open) {
                String sign = message.substring(open + 1, close).trim();
                if (sign.length() == 1) {
                    fixes.add(new DiagnosticCard.Fix(
                            "Supprimer \u00AB " + sign + " \u00BB",
                            () -> {
                                applyEdit(local.start(), local.end(), "");
                                releaseAnalysisThrottle();
                            }));
                }
            }
        } else if (message.startsWith("ouvrant non fermé")) {
            // l'ouvrant se referme : le fermant apparié s'insère à la
            // fin de la ligne (le bloc se clôt là où il vit)
            char opener = safeCharAt(local.start());
            char closer = switch (opener) {
                case '{' -> '}';
                case '(' -> ')';
                case '[' -> ']';
                default -> 0;
            };
            if (closer != 0) {
                int insertAt = Math.min(local.end(), buffer.length());
                fixes.add(new DiagnosticCard.Fix(
                        "Insérer \u00AB " + closer + " \u00BB",
                        () -> {
                            applyEdit(insertAt, insertAt,
                                    String.valueOf(closer));
                            releaseAnalysisThrottle();
                        }));
            }
        }
        return fixes;
    }

    /** Le caractère à un décalage (0 : hors bornes — l'édition d'avant a pu le tuer). */
    private char safeCharAt(int offset) {
        return offset >= 0 && offset < buffer.length()
                ? buffer.charAt(offset) : 0;
    }

    /**
     * Libère le throttle de l'analyse : la passe qui suit la trame
     * prochaine repart aussitôt — un CORRECTIF exécuté doit guérir
     * SOUS LES YEUX (le souligné meurt, le marqueur disparaît), pas
     * 300 ms plus tard.
     */
    private void releaseAnalysisThrottle() {
        lastAnalysisStartNanos = -Long.MAX_VALUE / 2;
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
        // LE REGISTRE DES ACTIONS D'ÉDITION (R-A) : la MÊME définition
        // que l'étage « Édition » du débordement — plus jamais deux
        // listes à tenir à la main
        for (ActionRegistry.Action action : editActions.live()) {
            actions.add(new QuickActionCard.Entry(
                    action.label(), action::run));
        }
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
        diagnosticCard.open(line, found);
        // L'EXCLUSION MUTUELLE (D-P4) : la carte qui ouvre devient LA
        // carte flottante active — toute autre carte flottante ou à
        // champ vivante se range (l'ampoule d'avant meurt avec la
        // carte des actions rapides, l'info d'avant avec elle)
        popups.shown(diagnosticCard);
        invalidate();
        return true;
    }

    /**
     * Pose la pastille du diagnostic sur le mot diagnostiqué visé par le
     * tap (B4) : l'ampoule vit trois secondes AU-DESSUS DU MOT (le
     * retour du terrain à la leçon de la référence : la gouttière
     * porte déjà LE marqueur du diagnostic — la pastille n'y parle
     * plus de la ligne, elle parle du mot), elle se centre sur le
     * PREMIER CARACTÈRE de la plage diagnostiquée, et elle NAÎT par
     * le ressort (le pop : l'échelle part de la moitié, ancrée à la
     * ligne). Le tap suivant sur elle ouvre la carte. Un offset sans
     * diagnostic range la pastille vivante (le geste a déplacé
     * l'attention).
     *
     * @param offset l'offset visé par le doigt
     */
    private void offerDiagnosticBadge(int offset) {
        int line = buffer.lineOfOffset(Math.min(offset, buffer.length()));
        int lineStart = buffer.getLineStart(line);
        // la colonne du MOT : le PREMIER caractère de la plage du
        // diagnostic couvrant l'offset (la pastille s'y centre — le
        // doigt touche le souligné N'IMPORTE OÙ, la pastille parle du
        // mot entier) ; le fournisseur de l'hôte ne dit pas sa plage —
        // la colonne tapée y tient lieu de mot
        int column = -1;
        if (diagnostics != null && analysisRevision == buffer.getRevision()) {
            for (DiagnosticsDocument.Diagnostic diagnostic
                    : diagnostics.betweenLines(line, line)) {
                if (diagnostic.start() <= offset && offset < diagnostic.end()) {
                    column = Math.max(diagnostic.start(), lineStart) - lineStart;
                    break;
                }
            }
        }
        if (column < 0 && diagnosticProvider != null
                && diagnosticProvider.diagnosticCovers(line,
                        offset - lineStart)) {
            column = Math.min(offset, buffer.getLineEnd(line)) - lineStart;
        }
        if (column >= 0) {
            diagnosticBadgeLine = line;
            diagnosticBadgeColumn = column;
            diagnosticBadgeUntilNanos = System.nanoTime()
                    + BADGE_LIFETIME_NANOS;
            badgePop.born();
            postDelayed(this::invalidate, BADGE_LIFETIME_MS + 50);
        } else {
            diagnosticBadgeLine = -1;
            badgePop.reset();
        }
    }

    /**
     * Le doigt est-il sur l'ampoule vivante ? Le tap y ouvre la carte
     * de la ligne ancrée.
     *
     * @param x l'abscisse du doigt (px, coordonnées de la vue)
     * @param y l'ordonnée du doigt (px, coordonnées de la vue)
     * @return vrai quand la pastille a pris le geste
     */
    private boolean tapDiagnosticBadge(float x, float y) {
        if (diagnosticBadgeLine < 0
                || System.nanoTime() > diagnosticBadgeUntilNanos) {
            diagnosticBadgeLine = -1;
            return false;
        }
        float cx = diagnosticBadgeCenterX();
        float cy = diagnosticBadgeCenterY();
        float radius = dp(14);
        if ((x - cx) * (x - cx) + (y - cy) * (y - cy) <= radius * radius) {
            int line = diagnosticBadgeLine;
            diagnosticBadgeLine = -1;
            openDiagnosticsAtLine(line);
            return true;
        }
        return false;
    }

    /**
     * L'ordonnée du CENTRE de l'ampoule vivante (l'accès du harnais
     * et du nœud d'accessibilité : la géométrie du dessin s'y lit,
     * le doigt s'y pose).
     *
     * @return le centre vertical en pixels, {@link Float#MAX_VALUE}
     *         si aucune pastille ne vit
     */
    public float diagnosticBadgeCenterY() {
        if (diagnosticBadgeLine < 0) {
            return Float.MAX_VALUE;
        }
        // la ligne ANCRÉE : le bord haut de sa rangée visible (le pli
        // décale l'indice)
        float top = getPaddingTop()
                + visibleIndexOf(diagnosticBadgeLine) * lineHeight - scrollY;
        float side = dp(18);
        float gap = dp(6);
        // AU-DESSUS de la ligne (la leçon de la référence : son
        // ampoule flotte au-dessus du curseur — le pied à un pas de
        // la rangée) ; sans la place au-dessus (la première rangée de
        // la fenêtre), la pastille TOMBE SOUS la ligne — jamais dans
        // la gouttière
        if (top - gap - side >= 0.0f) {
            return top - gap - side / 2.0f;
        }
        return Math.min(top + lineHeight + gap + side / 2.0f,
                getHeight() - side / 2.0f);
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
        quickActionCard.open(line, actions);
        // L'EXCLUSION MUTUELLE : la carte d'ampoule devient LA carte
        // flottante active — la carte de diagnostic d'avant se range
        popups.shown(quickActionCard);
        invalidate();
        return true;
    }

    /**
     * Ouvre la carte d'actions rapides à des entrées posées (l'ampoule
     * des symboles du document y ouvre sa liste — chaque rangée y est
     * une action de navigation) ; la carte s'ancre à la ligne du
     * curseur, les entrées vides n'ouvrent rien.
     *
     * @param entries les entrées à ouvrir, null ou vide : rien
     * @return vrai quand la carte s'est ouverte
     */
    public boolean openQuickActions(List<QuickActionCard.Entry> entries) {
        if (entries == null || entries.isEmpty()) {
            return false;
        }
        int caret = Math.min(selection.caret(), buffer.length());
        int line = buffer.lineOfOffset(caret);
        quickActionCard.open(line, entries);
        // L'EXCLUSION MUTUELLE : la carte d'ampoule devient LA carte
        // flottante active — la carte de diagnostic d'avant se range
        popups.shown(quickActionCard);
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
            // la carte suit sa ligne — le gestionnaire la range (sa
            // vie s'y lit)
            popups.dismiss(quickActionCard);
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
        suggestionWordStart = -1;
        // LE CHANGEMENT DE CONTEXTE (D-P4) : le document change — le
        // gestionnaire range TOUT, la liste inscrite y vit ENTIÈRE
        // (aucune carte ne reste sur le carreau — le popup fantôme
        // naissait d'une liste de fermeture qui oubliait une carte :
        // la complétion d'avant y restait vivante, invisible)
        popups.contextLost();
        quickActionsRevision = -1;
        quickActions = List.of();
        // autre document : l'histoire locale du précédent n'y désigne
        // plus rien (les lignes d'avant parlent d'un autre texte)
        localHistory.clear();
        // autre document : la sélection n'y désigne plus rien, la
        // recherche non plus
        selection.setAnchor(-1);
        search.forget();
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
        scrollX = 0.0f;
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
        touch.releaseHandle();
        search.forget();
        // le document se ROUVRE : le contexte se perd — le
        // gestionnaire range TOUT (la popup d'info d'avant, la carte
        // d'usages ancrée au caret d'avant — le fantôme y mourait)
        popups.contextLost();
        undoStack.clear();
        folds.clear();
        rebuildFoldLayout();
        resetFrameStats();
        scrollY = 0.0f;
        scrollX = 0.0f;
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
        boolean dark = luminance < 12800;
        gutter.setTextColor(dark ? 0xFF8E8E93 : 0xFF8A8A8E);
        // le panneau de la gouttière se teinte au fond du thème (la
        // translucence y vit — le texte se lit dessous)
        gutter.setPanelColor(newBackground);
        // les popups suivent le thème : les encres claires d'un style
        // d'hôte posé sur un fond clair deviendraient illisibles au
        // fond sombre — le style se RECALE (le fond des cartes prend
        // le fond du thème, translucide ; les encres s'inversent) en
        // gardant les tailles et la forme que l'hôte y a posées
        setPopupStyle(restylePopupForTheme(popupStyle, newBackground, dark));
        theme = newTheme;
        themeBackground = newBackground;
        // le peintre suit le thème : la couleur du texte (les
        // ligatures et le placeholder s'y teintent)
        painter.applyTheme(newTheme.defaultColor());
        // les plans portent des couleurs résolues : re-rendu, jamais
        // retokenisation (la première coloration du panneau ne bouge pas)
        renderCache.invalidate();
        // le rendu de la loupe porte les couleurs du thème aussi
        loupeLine = null;
        loupeLineIndex = -1;
        invalidate();
    }

    /**
     * Le style des popups recalé au fond du thème : le fond des
     * cartes en prend la teinte (translucide — la vue se lit dessous),
     * les encres s'inversent au fond sombre, l'accent s'y calme ; les
     * tailles, hauteurs et la forme restent celles de l'hôte (le
     * style ne porte que des couleurs ici).
     *
     * @param base       le style courant (celui de l'hôte ou le recalé)
     * @param background le fond du thème posé
     * @param dark       vrai quand le fond est sombre
     * @return le style recalé, jamais null
     */
    private static PopupStyle restylePopupForTheme(PopupStyle base,
            int background, boolean dark) {
        // le fond des cartes : la teinte du thème, translucide à 91
        // pour cent (l'opacité par défaut des cartes)
        int cardBackground = (background & 0x00FFFFFF) | 0xE8000000;
        int cardBorder = dark ? 0xC83A3A40 : 0xC8B5B5B5;
        PopupStyle.Builder builder = PopupStyle.builder()
                .backgroundArgb(cardBackground)
                .borderArgb(cardBorder)
                .textArgb(dark ? 0xFFE8EAED : 0xFF202124)
                .hintArgb(dark ? 0xFF9AA0A6 : 0xFF80868B)
                .secondaryArgb(dark ? 0xFFBDC1C6 : 0xFF5F6368)
                .accentArgb(dark ? 0xFF8AB4F8 : 0xFF1A73E8)
                .errorArgb(dark ? 0xFFF28B82 : 0xFFD93025)
                .warningArgb(dark ? 0xFFFDD663 : 0xFFF9AB00)
                .lightbulbArgb(dark ? 0xFFFDD663 : 0xFFF5C518);
        return builder
                .textSizeSp(base.textSizeSp())
                .secondarySizeSp(base.secondarySizeSp())
                .rowHeightDp(base.rowHeightDp())
                .headerHeightDp(base.headerHeightDp())
                .minCardWidthDp(base.minCardWidthDp())
                .maxCardWidthDp(base.maxCardWidthDp())
                .cornerRadiusDp(base.cornerRadiusDp())
                .build();
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
     * La minimap vit-elle à l'écran (la carte du document au bord
     * droit et sa colonne de pastilles).
     *
     * @return vrai si la minimap se dessine
     */
    public boolean isMinimapEnabled() {
        return minimapEnabled;
    }

    /**
     * La minimap se pose ou se retire (l'hôte décide — un écran
     * étroit s'en passe, la zone rend son espace au texte). Le retrait
     * ne touche ni le tampon ni la coloration.
     *
     * @param enabled vrai pour dessiner la minimap
     */
    public void setMinimapEnabled(boolean enabled) {
        if (minimapEnabled == enabled) {
            return;
        }
        minimapEnabled = enabled;
        invalidate();
    }

    /**
     * La hauteur d'une ligne de texte (marge comprise) — l'hôte et
     * le harnais d'intégration y calculent les ordonnées de contact
     * depuis la géométrie RÉELLE de la police du cadre.
     *
     * @return la hauteur de ligne en pixels
     */
    public float lineHeight() {
        return lineHeight;
    }

    /**
     * Les colonnes par niveau d'indentation (les guides de L2 y
     * vivent, le formatage du serveur y règle ses tabulations).
     *
     * @return les colonnes d'un niveau d'indentation
     */
    public int tabSize() {
        return GUIDE_COLUMNS;
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
        search.search(query);
    }

    /**
     * La correspondance suivante (la recherche boucle : après la
     * dernière vient la première).
     */
    public void searchNext() {
        search.next();
    }

    /** La correspondance précédente (la recherche boucle aussi en arrière). */
    public void searchPrevious() {
        search.previous();
    }

    /**
     * Le nombre de correspondances de la recherche courante (le
     * plafond compris : le compte s'arrête à
     * {@value #SEARCH_MATCH_LIMIT}).
     *
     * @return le nombre de correspondances trouvées, 0 sans recherche
     */
    public int searchMatchCount() {
        return search.matchCount();
    }

    /**
     * L'index de la correspondance courante (0-based), -1 sans
     * correspondance — la feuille du bas affiche « courant+1/total ».
     *
     * @return l'index de la correspondance courante
     */
    public int searchCurrentIndex() {
        return search.currentIndex();
    }

    /** Range la recherche (les teintes disparaissent, la requête s'oublie). */
    public void clearSearch() {
        search.clear();
    }

    /**
     * Remplace la correspondance courante : une transaction, une
     * unité d'annulation, la recherche se relance après (les
     * positions suivantes ont bougé).
     *
     * @param replacement le texte de remplacement
     */
    public void replaceCurrent(String replacement) {
        String query = search.query();
        int current = search.currentIndex();
        int start = search.matchAt(current);
        if (query == null || start < 0) {
            return;
        }
        int end = start + query.length();
        if (end > buffer.length()) {
            return;
        }
        pendingEditLabel = "remplacement";
        applyEdit(start, end, replacement);
        search(search.query());
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
        String query = search.query();
        int[] matches = search.matches();
        if (query == null || matches.length == 0) {
            return 0;
        }
        pendingEditLabel = "remplacement global";
        // un instantané avant la transaction : l'écouteur du tampon
        // (notifié au PREMIER remplacement) range la recherche — la
        // boucle doit tenir ses positions, pas celles du champ
        int count = matches.length;
        int firstStart = matches[0];
        try (TextTransaction transaction = undoStack.newTransaction()) {
            for (int m = count - 1; m >= 0; m--) {
                int start = matches[m];
                int end = start + query.length();
                if (end > buffer.length()) {
                    continue;
                }
                transaction.replace(start, end, replacement);
            }
        }
        selection.setAnchor(-1);
        selection.setCaret(Math.min(firstStart + replacement.length(), buffer.length()));
        search(search.query());
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
                // les positions du serveur datent du texte qu'il a
                // vu — l'édition depuis peut raccourcir la ligne :
                // la conversion se borne au texte vivant, un edit
                // hors texte s'ignore (jamais une exception de
                // correctif)
                int start = offsetOfPositionClamped(
                        change.range().start().line(), change.range().start().character());
                int end = offsetOfPositionClamped(
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
            // la barre s'instruit au gestionnaire — sa fermeture et sa
            // vie y demeurent (l'inscription unique de toutes les
            // popups, D-P4)
            popups.register(searchBar, PopupHost.Role.KEYBOARD,
                    searchBar::hide, searchBar::isShown);
        }
        searchBar.show();
        // L'EXCLUSION MUTUELLE (D-P4) : la barre qui ouvre devient LA
        // carte active — toute carte flottante ou à champ vivante se
        // range (l'ouverture exclusive d'une carte à la fois)
        popups.shown(searchBar);
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
            popups.dismiss(searchBar);
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
            // la barre s'instruit au gestionnaire (l'inscription unique)
            popups.register(gotoBar, PopupHost.Role.KEYBOARD,
                    gotoBar::hide, gotoBar::isShown);
        }
        gotoBar.show();
        // L'EXCLUSION MUTUELLE : la barre qui ouvre devient LA carte
        // active — toute carte flottante ou à champ vivante se range
        popups.shown(gotoBar);
        // la carte naît clavier levé : les chiffres attendent une
        // frappe — même règle que la recherche
        openKeyboard();
        invalidate();
    }

    /** Range la carte « aller à la ligne ». */
    public void dismissGotoPopup() {
        if (gotoBar != null && gotoBar.isShown()) {
            popups.dismiss(gotoBar);
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
            // la carte s'instruit au gestionnaire (l'inscription unique)
            popups.register(renameBar, PopupHost.Role.KEYBOARD,
                    renameBar::hide, renameBar::isShown);
        }
        renameBar.setStyle(popupStyle);
        renameBar.show(symbol, target);
        // L'EXCLUSION MUTUELLE : la carte qui ouvre devient LA carte
        // active — toute carte flottante ou à champ vivante se range
        popups.shown(renameBar);
        openKeyboard();
        invalidate();
    }

    /** Range la carte « renommer le symbole ». */
    public void dismissRenamePopup() {
        if (renameBar != null && renameBar.isShown()) {
            popups.dismiss(renameBar);
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
     * Les popups de la vue s'instruisent auprès du gestionnaire —
     * UNE SEULE FOIS à la naissance, leur fermeture et leur vie y
     * demeurent. La famille FLOTTANTE (l'info, les usages, le
     * sélecteur, le diagnostic, les actions rapides) y vit
     * l'exclusion mutuelle ; les barres à CHAMP (la recherche,
     * l'aller-à, le renommage) s'y inscrivent à LEUR naissance —
     * elles se créent à leur première ouverture ; les cartes de la
     * FRAPPE (la complétion, l'aide aux paramètres) COHABITENT avec
     * la frappe ; la famille de la SÉLECTION (la barre, le
     * débordement, l'historique) vit de son ancre. L'annonce des
     * ouvertures s'y branche — la SOURCE UNIQUE de l'accessibilité.
     */
    private void registerPopups() {
        popups.register(infoPopup, PopupHost.Role.FLOATING,
                infoPopup::dismiss, () -> infoPopup.text() != null);
        popups.register(usageCard, PopupHost.Role.FLOATING,
                usageCard::close, usageCard::isShown);
        popups.register(colorPickerCard, PopupHost.Role.FLOATING,
                colorPickerCard::close, colorPickerCard::isShown);
        popups.register(diagnosticCard, PopupHost.Role.FLOATING,
                diagnosticCard::close, diagnosticCard::isShown);
        popups.register(quickActionCard, PopupHost.Role.FLOATING,
                quickActionCard::close, quickActionCard::isShown);
        popups.register(completionCard, PopupHost.Role.TYPING,
                () -> completionCard.set(List.of(), ""),
                completionCard::isShown);
        popups.register(signatureCard, PopupHost.Role.TYPING,
                () -> signatureCard.set(List.of(), 0, 0),
                signatureCard::isShown);
        popups.register(selectionBar, PopupHost.Role.SELECTION,
                selectionBar::hide, selectionBar::isShown);
        popups.register(overflowCard, PopupHost.Role.SELECTION,
                overflowCard::close, overflowCard::isShown);
        popups.register(historyCard, PopupHost.Role.SELECTION,
                historyCard::close, historyCard::isShown);
        popups.setAnnouncer(card -> announceForAccessibility(
                announcedPopupLabel(card)));
    }

    /**
     * Le libellé annoncé à l'ouverture d'une carte (l'annonce de la
     * source unique — les rangées en nœuds virtuels viendront avec
     * l'accessibilité des cartes, D-P8).
     */
    private String announcedPopupLabel(Object card) {
        if (card == infoPopup) {
            return "Documentation ouverte";
        }
        if (card == usageCard) {
            return "Liste des usages ouverte";
        }
        if (card == colorPickerCard) {
            return "Sélecteur de couleur ouvert";
        }
        if (card == diagnosticCard) {
            return "Diagnostics ouverts";
        }
        if (card == quickActionCard) {
            return "Actions rapides ouvertes";
        }
        if (card == searchBar) {
            return "Recherche ouverte";
        }
        if (card == gotoBar) {
            return "Aller à la ligne ouvert";
        }
        if (card == renameBar) {
            return "Renommer le symbole ouvert";
        }
        if (card == targetCard) {
            return "Choix de cibles ouvert";
        }
        if (card == problemsCard) {
            return "Liste des problèmes ouverte";
        }
        if (card == clipboardCard) {
            return "Historique du presse-papiers ouvert";
        }
        if (card == structureCard) {
            return "Structure du fichier ouverte";
        }
        return "Carte ouverte";
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
        // la STRUCTURE (la structure du fichier) reçoit le clavier pareillement : le
        // FILTRE s'y vit à la frappe (les lettres tapées y
        // resserrent la liste, le tampon ne bouge pas)
        if (structureCard != null && structureCard.isShown()) {
            return structureCard;
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
        // L'EXCLUSION MUTUELLE : la popup qui ouvre devient LA carte
        // flottante active — toute autre carte flottante ou à champ
        // vivante se range
        popups.shown(infoPopup);
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
        // L'EXCLUSION MUTUELLE : la popup qui ouvre devient LA carte
        // flottante active
        popups.shown(infoPopup);
        invalidate();
    }

    /**
     * La popup d'info ancrée en documentation BALISÉE : le corps y
     * vit au sous-ensemble des serveurs (titres, listes, code,
     * liens) — le corps qui dépasse se DÉFILE au doigt.
     *
     * @param title     le mot questionné, ou null pour aucun titre
     * @param body      le texte du corps, vide ou null pour masquer
     * @param markdown  le corps est-il balisé ?
     * @param line      la ligne d'ancrage
     * @param column    la colonne d'ancrage
     */
    public void showInfoPopup(String title, String body, boolean markdown,
            int line, int column) {
        infoPopup.show(title, body, markdown, line, column);
        // L'EXCLUSION MUTUELLE : la popup qui ouvre devient LA carte
        // flottante active
        popups.shown(infoPopup);
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
        return replaceInfoPopup(text, false, line, column);
    }

    /**
     * Remplace la popup d'info par une réponse TARDIVE, NUE ou
     * BALISÉE — le survol asynchrone d'un serveur lent : la question
     * a montré la popup locale (ou rien), la réponse du serveur y
     * revient à son arrivée. Le remplacement ne vaut qu'à la MÊME
     * position : une popup bougée ou rangée n'est pas évincée par
     * une réponse périmée.
     *
     * @param text     la réponse tardive
     * @param markdown la réponse est-elle balisée ?
     * @param line     la ligne d'ancrage de la question
     * @param column   la colonne d'ancrage de la question
     * @return vrai si la popup vivait encore à cette position
     */
    public boolean replaceInfoPopup(String text, boolean markdown,
            int line, int column) {
        if (!infoPopup.replace(text, markdown, line, column)) {
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
     *
     * <p>Le contrat est ASYNCHRONE (D-P1 du rapport des popups) : la
     * question NE BLOQUE JAMIS le fil d'interface — la popup vit
     * immédiate (son corps dit « … »), la réponse la REMPLACE à son
     * arrivée ; le retour null rend la place à l'index local. La
     * réponse peut venir de N'IMPORTE QUEL fil : la vue y repasse le
     * sien.</p>
     */
    public interface InfoPopupProvider {

        /**
         * L'info du mot demandé — la réponse se rend à l'écouteur,
         * jamais en retour (la question part, le fil d'interface
         * n'attend pas).
         *
         * @param line   la ligne du doigt
         * @param column la colonne du doigt
         * @param word   le mot sous le doigt
         * @param answer l'écouteur de la réponse (le texte, ou null :
         *               « je ne sais pas, demande à l'index local »)
         */
        void infoFor(int line, int column, String word, InfoAnswer answer);
    }

    /**
     * L'écouteur d'une réponse d'info (tardive ou non) — la vue la
     * ramène à son fil avant d'agir, le fournisseur l'appelle du
     * sien. La réponse porte SON BALISAGE : le corps balisé (le
     * Markdown des serveurs) se rend au sous-ensemble du domaine,
     * le corps nu court tel quel.
     */
    @FunctionalInterface
    public interface InfoAnswer {

        /**
         * La réponse arrive.
         *
         * @param text     le texte à montrer, null si le fournisseur
         *                 ne sait pas
         * @param markdown le texte est-il balisé ?
         */
        void answer(String text, boolean markdown);
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
     * neuve arrive, la trame suivante la porte). Les candidats y
     * sont DÉTAILLÉS (le défaut du rapport des popups : le libellé
     * seul y vivait — la nature, le détail, l'obsolescence, la
     * documentation et le remplacement propre du serveur mouraient
     * en chemin).
     */
    public interface SuggestionProvider {

        /**
         * Les complétions du préfixe demandé.
         *
         * @param prefix le préfixe tapé (du début du mot au curseur)
         * @param line   la ligne du curseur
         * @param column la colonne du curseur
         * @return les candidats détaillés, jamais null (vide : bandeau vide)
         */
        List<CompletionCard.Entry> completionsFor(String prefix, int line, int column);
    }

    /**
     * Le résolveur de documentation de complétion : la documentation
     * qui manque au candidat CHOISI s'y demande — le
     * {@code completionItem/resolve} du protocole y répond (le
     * serveur retient la documentation à la sélection seule). La
     * réponse se rend à l'écouteur, jamais en retour (la question
     * part, le fil d'interface n'attend pas — la réponse peut venir
     * de N'IMPORTE QUEL fil, la vue y repasse le sien).
     */
    public interface CompletionDocResolver {

        /**
         * La documentation du candidat demandé.
         *
         * @param entry  le candidat CHOISI (son nom y dit l'identité)
         * @param answer l'écouteur de la réponse
         */
        void documentationFor(CompletionCard.Entry entry, CompletionDocAnswer answer);
    }

    /**
     * L'écouteur d'une réponse de documentation — la vue la ramène à
     * son fil avant d'agir, le résolveur l'appelle du sien.
     */
    @FunctionalInterface
    public interface CompletionDocAnswer {

        /**
         * La réponse arrive.
         *
         * @param documentation la documentation du candidat, null ou
         *                      vide : le panneau se range
         * @param markdown      la documentation est-elle balisée ?
         */
        void answer(String documentation, boolean markdown);
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
     * Branche le résolveur de documentation (null débranche : le
     * panneau vit de la documentation PORTÉE seule).
     *
     * @param resolver le résolveur, ou null
     */
    public void setCompletionDocResolver(CompletionDocResolver resolver) {
        completionDocResolver = resolver;
    }

    private CompletionDocResolver completionDocResolver;

    /**
     * Le fournisseur d'aide aux paramètres : l'hôte y branche une
     * source extérieure (un serveur de langage par exemple) — sans
     * fournisseur, l'index local des symboles porte la carte (le nom
     * de l'appel se cherche dans les déclarations, la ligne de la
     * déclaration devient la signature).
     */
    public interface SignatureProvider {

        /**
         * L'aide aux paramètres de l'appel au curseur — les SURCHARGES
         * y vivent (le défaut du rapport des popups : l'active seule
         * revenait, les autres mouraient en chemin).
         *
         * @param caretOffset le décalage du curseur dans le tampon
         * @return l'aide complète, null pour ranger la carte
         */
        SignatureHelp signatureAt(int caretOffset);
    }

    /**
     * L'aide aux paramètres montrée : les surcharges du serveur (une
     * LIGNE de signature chacune, la documentation de CHAQUE
     * paramètre — celle du paramètre actif se montre), l'index de la
     * signature active et du paramètre actif.
     */
    public record SignatureHelp(List<Overload> signatures,
            int activeSignature, int activeParameter) {

        /**
         * Une surcharge : sa ligne de signature et les documentations
         * de ses paramètres.
         *
         * @param label          la ligne de la signature
         * @param parameterDocs  la documentation de chaque paramètre
         */
        public record Overload(String label, List<String> parameterDocs) {
        }
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
            popups.dismiss(infoPopup);
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
     * Le défilement du corps de la popup d'info (le harnais y éprouve
     * le doigt qui traîne — le corps balisé qui dépasse se défile au
     * lieu de se couper).
     *
     * @return le défilement courant (px), 0 si rien ne se défile
     */
    public float infoPopupScrollY() {
        return infoPopup.scrollY();
    }

    /**
     * La pose de la popup d'info (le harnais y vise le doigt du
     * défilement du corps) ; une pose vide dit « jamais posée ».
     *
     * @return la pose de la dernière trame, jamais null
     */
    public android.graphics.RectF infoPopupBox() {
        return infoPopup.box();
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

    @Override
    public boolean onCheckIsTextEditor() {
        return true;
    }

    /**
     * Le nœud d'accessibilité (D5) : le service qui parle y lit la
     * vue — un éditeur de texte éditable, sa fenêtre de contenu, sa
     * sélection, ses actions.
     */
    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo node) {
        super.onInitializeAccessibilityNodeInfo(node);
        accessibility.fillNode(node);
    }

    /**
     * Les événements d'accessibilité par défaut gardent leurs
     * remplissages de texte (la frappe s'y entend).
     */
    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        boolean populated = super.dispatchPopulateAccessibilityEvent(event);
        if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
            accessibility.fillTextChanged(event);
            return true;
        }
        if (event.getEventType()
                == AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED) {
            accessibility.fillSelectionChanged(event);
            return true;
        }
        return populated;
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
            public CharSequence textBetween(int start, int end) {
                return buffer.getText(start, end);
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
                    // le candidat choisi a bougé : sa documentation
                    // se demande (le panneau adjacent s'y nourrit)
                    askChosenDocumentation();
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
            // LES CARTES ANCRAU CARET SE RANGENT D'ABORD (usages,
            // sélecteur) : une carte posée au caret d'avant ne doit
            // jamais manger le tap qui vise le texte ou une autre
            // carte — le tap qui la VISE la rouvre (le picker au
            // littéral, les usages au débordement)
            boolean caretOnUsage = usageCard != null && usageCard.isShown()
                    && usageCard.onTouchArea(event.getX(), event.getY());
            boolean caretOnPicker = colorPickerCard != null
                    && colorPickerCard.isShown()
                    && colorPickerCard.onTouchArea(event.getX(),
                            event.getY(), event.getActionMasked());
            boolean caretOnTarget = targetCard != null && targetCard.isShown()
                    && targetCard.onTouchArea(event.getX(), event.getY());
            boolean caretOnProblems = problemsCard != null
                    && problemsCard.isShown()
                    && problemsCard.onTouchArea(event.getX(), event.getY());
            boolean caretOnClipboard = clipboardCard != null
                    && clipboardCard.isShown()
                    && clipboardCard.onTouchArea(event.getX(), event.getY());
            boolean caretOnStructure = structureCard != null
                    && structureCard.isShown()
                    && structureCard.onTouchArea(event.getX(), event.getY());
            if (!caretOnUsage) {
                popups.dismiss(usageCard);
            }
            if (!caretOnPicker) {
                popups.dismiss(colorPickerCard);
            }
            if (!caretOnTarget) {
                popups.dismiss(targetCard);
            }
            if (!caretOnProblems) {
                popups.dismiss(problemsCard);
            }
            if (!caretOnClipboard) {
                popups.dismiss(clipboardCard);
            }
            if (!caretOnStructure) {
                popups.dismiss(structureCard);
            }
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
            boolean selectionAlive = hasSelection() && !touch.isHandleDragging();
            downOnCard = touchedSearch || touchedGoto || touchedRename
                    || (completionCard != null && completionCard.isShown()
                            && completionCard.onTouchArea(event.getX(), event.getY()))
                    || (signatureCard != null && signatureCard.isShown()
                            && signatureCard.onTouchArea(event.getX(), event.getY()))
                    || (historyCard != null && historyCard.isShown() && selectionAlive
                            && historyCard.onTouchArea(event.getX(), event.getY()))
                    || (usageCard != null && usageCard.isShown()
                            && usageCard.onTouchArea(event.getX(), event.getY()))
                    || (colorPickerCard != null && colorPickerCard.isShown()
                            && colorPickerCard.onTouchArea(event.getX(),
                                    event.getY(), event.getActionMasked()))
                    || (targetCard != null && targetCard.isShown()
                            && targetCard.onTouchArea(event.getX(), event.getY()))
                    || (problemsCard != null && problemsCard.isShown()
                            && problemsCard.onTouchArea(event.getX(), event.getY()))
                    || (clipboardCard != null && clipboardCard.isShown()
                            && clipboardCard.onTouchArea(event.getX(), event.getY()))
                    || (structureCard != null && structureCard.isShown()
                            && structureCard.onTouchArea(event.getX(), event.getY()))
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
            // LE GESTE APPARTIENT À SA CARTE : les MOVE y suivent le
            // doigt (le champ et la bande du sélecteur de couleur y
            // vivent — la teinte change à CHAQUE cran), l'UP y clôt
            // (la rangée armée des usages s'y joue, la cellule de
            // palette s'applique)
            if (colorPickerCard != null && colorPickerCard.isShown()) {
                colorPickerCard.onTouchArea(event.getX(), event.getY(),
                        event.getActionMasked());
            }
            if (event.getActionMasked() == MotionEvent.ACTION_UP
                    || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                if (usageCard != null) {
                    usageCard.onRelease();
                }
                if (targetCard != null) {
                    targetCard.onRelease();
                }
                if (problemsCard != null) {
                    problemsCard.onRelease();
                }
                if (structureCard != null) {
                    structureCard.onRelease();
                }
                if (clipboardCard != null && clipboardCard.onRelease()) {
                    // la copie choisie se COLLE au caret (l'historique du presse-papiers — le
                    // collage d'avant renaît au doigt)
                    String paste = clipboardCard.pendingPaste();
                    clipboardCard.takePendingPaste();
                    pendingEditLabel = "collage (historique)";
                    int caret = Math.min(selection.caret(), buffer.length());
                    applyEdit(caret, caret, paste == null ? "" : paste);
                }
                downOnCard = false;
            }
            return true;
        }
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                touch.begin(event.getX(), event.getY());
                pinch.reset();
                // LA MINIMAP ET SA BARRE D'ANNULER/RÉTABLIR prennent
                // leur geste au DOWN : la barre COIFFE la région (les
                // pastilles y jouent au tap), la carte du document
                // dessous suit le doigt EN CONTINU (le drag y navigue —
                // la vue saute à la ligne visée à chaque cran, le
                // relâchement y reste). LA ZONE SE BORNE À CE QUI SE
                // DESSINE : la carte ne couvre que la hauteur du
                // document rendu (un document court ne peint rien
                // au-dessous — le doigt y retombe sur le texte, le
                // défaut du terrain où la bande avalait les taps du
                // vide)
                float minimapLeft = getWidth() - minimap.width();
                float minimapBottom = minimap.mapBottom(getHeight(),
                        buffer.getLineCount());
                if (minimapEnabled
                        && event.getX() > minimapLeft
                        && event.getY() < minimapBottom) {
                    if (minimap.buttonAt(
                            event.getX(), event.getY(), getWidth()) < 0
                            && event.getY() >= minimap.barHeight()) {
                        // la barre COIFFE la carte : le doigt SUR LA
                        // BARRE (hors pastille) ne déplace PAS le
                        // curseur — la barre d'outils consomme sans
                        // naviguer, la carte vit dessous
                        touch.holdsMap();
                        int line = minimap.lineAt(event.getY(), getHeight(),
                                buffer.getLineCount());
                        caretTo(buffer.getLineStart(line));
                    }
                    return true;
                }
                // un nouvel appui masque la popup d'info (l'appui long
                // pourra en rouvrir une sur le mot touché) et range les
                // cartes de diagnostic et d'ampoule — les cartes ancrées
                // au caret (usages, sélecteur de couleur) se rangent
                // pareillement : le doigt qui reprend le texte ne doit
                // jamais tomber sur une carte flottante d'avant.
                // LE CORPS DÉFILÉ prend son doigt D'ABORD : la popup
                // de documentation vit sous le doigt (le défaut du
                // rapport : le corps qui dépassait se coupait, le
                // défilement y remplace la coupe) — l'appui SUR la
                // popup arme son défilement, le relâchement sans
                // traînée la range (la façon d'avant demeure)
                if (infoPopup.onDown(event.getX(), event.getY())) {
                    downOnInfo = true;
                    return true;
                }
                // LA FAMILLE FLOTTANTE SE RANGE (D-P4) : le doigt qui
                // reprend le texte ne doit jamais tomber sur une carte
                // flottante d'avant — l'info, le diagnostic, les
                // actions rapides, les usages, le sélecteur ; le
                // gestionnaire les range d'un trait (les barres à
                // champ demeurent : leur rangement leur appartient)
                boolean floatingWasShown = popups.anyFloatingShown();
                popups.dismissFloating();
                if (floatingWasShown) {
                    invalidate();
                }
                // une sélection vivante se saisit d'abord par ses
                // poignées (le défilement n'y a pas droit)
                grabSelectionHandle(event.getX(), event.getY());
                // le traîné-déposé : un appui dans le TEXTE de la
                // sélection (pas sur une poignée) l'arme — le glissé
                // qui suit, avant le seuil de l'appui long, déplacera
                // le texte au doigt
                touch.armTextDrag(!touch.isHandleDragging()
                        && event.getX() >= getPaddingLeft()
                        && hasSelection()
                        && selectionStart() <= offsetNearest(event.getX(), event.getY())
                        && offsetNearest(event.getX(), event.getY()) <= selectionEnd());
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
                if (pinch.isPinching()) {
                    endPinch(event);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (downOnInfo) {
                    // le corps défilé suit le doigt — la trame suivante
                    // le porte
                    infoPopup.onDrag(event.getY());
                    invalidate();
                    break;
                }
                if (pinch.isPinching()) {
                    if (event.getPointerCount() >= 2) {
                        pinchMove(event);
                    }
                    break;
                }
                if (touch.isMapDragging()) {
                    // le doigt tient la carte : la navigation suit à
                    // CHAQUE cran (la réactivité demandée)
                    int line = minimap.lineAt(event.getY(), getHeight(),
                            buffer.getLineCount());
                    caretTo(buffer.getLineStart(line));
                    break;
                }
                if (touch.isHandleDragging()) {
                    dragSelectionBound(event.getX(), event.getY());
                    break;
                }
                if (touch.isTextArmed() && !touch.isTextDragging()
                        && !touch.heldLongEnough()
                        && touch.beyondSlop(event.getX(), event.getY(), touchSlop())) {
                    // le traîné s'engage : le texte de la sélection
                    // suivra le doigt jusqu'au dépôt (l'appui long qui
                    // glisse garde sa voie : l'étendue de sélection)
                    requestFocus();
                    clearComposing();
                    dismissInfoPopup();
                    touch.engageTextDrag();
                }
                if (touch.isTextDragging()) {
                    // le curseur de dépôt suit le doigt : le caractère
                    // le plus proche (il se dessine à la place du
                    // curseur le temps du geste) ; le doigt près du
                    // bord programme le défilement — le point de dépôt
                    // reste lisible pendant que la vue glisse
                    touch.finger(event.getX(), event.getY());
                    scheduleEdgeScroll();
                    touch.dropAt(offsetNearest(event.getX(), event.getY()));
                    invalidate();
                    break;
                }
                if (!touch.isDragging() && touch.heldLongEnough()
                        && touch.startX() < dp(24)
                        && Math.abs(event.getX() - touch.startX()) <= touchSlop()
                        && Math.abs(event.getY() - touch.startY()) <= touchSlop()) {
                    // l'appui long IMMOBILE de la colonne des marqueurs :
                    // le point d'arrêt se pose (la ligne en pose le
                    // retire) — le geste du débogueur, le tap simple
                    // reste au pli
                    int line = lineOfVisibleIndex(
                            (int) ((event.getY() - getPaddingTop() + scrollY) / lineHeight));
                    toggleBreakpoint(line);
                    clearComposing();
                    touch.forgetTap();
                    break;
                }
                if (!touch.isDragging() && touch.heldLongEnough()
                        && touch.beyondSlop(event.getX(), event.getY(), touchSlop())) {
                    // l'appui long qui glisse : la sélection s'étend au
                    // doigt — le mot sous l'appui sème la sélection
                    // s'il n'y en a pas, la borne la plus proche du
                    // doigt suit ensuite (le défilement n'y a pas
                    // droit non plus)
                    requestFocus();
                    clearComposing();
                    if (!hasSelection()) {
                        selectWordAt(touch.startX(), touch.startY());
                    }
                    dismissInfoPopup();
                    engageSelectionDrag(event.getX(), event.getY());
                    dragSelectionBound(event.getX(), event.getY());
                    break;
                }
                if (!touch.isDragging()
                        && touch.beyondSlop(event.getX(), event.getY(), touchSlop())) {
                    touch.engageScroll();
                }
                if (touch.isDragging()) {
                    // le défilement à DEUX axes : le texte suit le
                    // doigt en hauteur et en largeur (la ligne longue
                    // s'y révèle — le pincement l'a agrandie, le
                    // geste plat la parcourt)
                    scrollY = clampToRange(scrollY - (event.getY() - touch.startY()),
                            0.0f, maxScroll());
                    scrollX = clampToRange(scrollX - (event.getX() - touch.startX()),
                            0.0f, maxScrollX());
                    touch.slideTo(event.getX(), event.getY());
                    invalidate();
                }
                break;
            case MotionEvent.ACTION_UP:
                if (downOnInfo) {
                    // le doigt rend : le geste de DÉFILEMENT se
                    // consomme (le corps défilé garde sa popup) ; le
                    // TAP sans traînée vit SA vie d'avant — la popup
                    // se range ET le curseur se pose au doigt (le tap
                    // de fermeture n'a jamais été un geste mort : le
                    // comportement d'origine demeure au pixel près)
                    if (infoPopup.onUp()) {
                        invalidate();
                        break;
                    }
                    dismissInfoPopup();
                    requestFocus();
                    caretTo(offsetNearest(event.getX(), event.getY()));
                    clearComposing();
                    touch.rememberTap(event.getX(), event.getY());
                    downOnInfo = false;
                    invalidate();
                    break;
                }
                if (pinch.isPinching() || pinch.hasPinchedThisGesture()) {
                    // le pincement se referme : l'appui final ne vaut
                    // pas un tap (le curseur ne saute pas au doigt qui
                    // reste le dernier levé)
                    pinch.reset();
                    touch.forgetTap();
                    invalidate();
                    break;
                }
                if (touch.isHandleDragging()) {
                    // la poignée se lâche : la sélection reste posée,
                    // l'appui ne compte pas pour un tap (le suivant ne
                    // serait pas un double-tap) ; la trame se redemande —
                    // la loupe s'y range, la barre de sélection y naît
                    // (sans elle, la dernière trame — loupe comprise —
                    // resterait gelée à l'écran)
                    touch.releaseHandle();
                    touch.forgetTap();
                    invalidate();
                    break;
                }
                if (touch.isTextDragging()) {
                    // le dépôt : le texte sélectionné se déplace au
                    // curseur de dépôt — l'appui ne compte pas pour un
                    // tap non plus ; le défilement au bord se range
                    edgeScroller.cancel();
                    dropTextAtSelection(touch.dropOffset());
                    touch.releaseTextDrag();
                    touch.forgetTap();
                    break;
                }
                if (touch.isMapDragging()) {
                    touch.releaseMap();
                    touch.forgetTap();
                    break;
                }
                if (touch.isDragging()) {
                    // le balayage se rend (son lâcher ne vaut pas un
                    // tap — le geste a grandi)
                    touch.settle();
                } else if (tapDiagnosticBadge(event.getX(), event.getY())) {
                    // LA PASTILLE FLOTTE AU-DESSUS DU MOT (B4, le
                    // retour du terrain à la leçon de la référence) :
                    // son tap s'y éprouve AVANT toute autre branche —
                    // elle vit sur le texte, le curseur s'y poserait
                    // à sa place
                    requestFocus();
                    touch.forgetTap();
                } else if (event.getX() > getWidth() - minimap.width()) {
                    // LA MINIMAP : sa barre d'annulation en tête (le tap
                    // y joue la pile), la carte du document dessous (le
                    // curseur y saute à la ligne visée — la fenêtre suit,
                    // le clavier ne s'y ouvre pas) ; le tap SUR LA BARRE
                    // hors pastille consomme sans naviguer
                    requestFocus();
                    int button = minimap.buttonAt(event.getX(), event.getY(),
                            getWidth());
                    if (button >= 0) {
                        minimap.pressButton(button);
                        clearComposing();
                        touch.forgetTap();
                        break;
                    }
                    if (event.getY() < minimap.barHeight()) {
                        touch.forgetTap();
                        break;
                    }
                    int line = minimap.lineAt(event.getY(), getHeight(),
                            buffer.getLineCount());
                    if (line >= 0 && line < buffer.getLineCount()) {
                        caretTo(buffer.getLineStart(line));
                        clearComposing();
                    }
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
                        touch.forgetTap();
                        break;
                    }
                    // LE DOUBLE-TAP SUR LE NUMÉRO pose le point d'arrêt
                    // (la leçon de la référence : son gutter y toggle au
                    // double-clic) — la COLONNE DES NUMÉROS SEULE (entre
                    // les marqueurs et le bord du panneau : la colonne
                    // des plis garde son pli au double-tap)
                    if (touch.tappedTwice(event.getX(), event.getY(), touchSlop())
                            && event.getX() > dp(24)
                            && event.getX() < getPaddingLeft() - dp(16)) {
                        toggleBreakpoint(line);
                        clearComposing();
                        touch.forgetTap();
                        break;
                    }
                    toggleFoldAt(line);
                    clearComposing();
                } else if (touch.heldLongEnough()
                        || touch.tappedTwice(event.getX(), event.getY(), touchSlop())) {
                    // l'appui long ou le double-tap : le mot sous le
                    // doigt se sélectionne (la frappe suivante le
                    // remplacera) — SAUF le littéral de couleur (la
                    // leçon de la référence : son sélecteur s'ouvre au
                    // DOUBLE-TAP sur le littéral, le chip y vit) et
                    // SAUF le souligné d'un diagnostic (B4 : le
                    // double-tap précis du stylet y ouvre la carte —
                    // la pastille sert le doigt, ce chemin sert la
                    // précision)
                    requestFocus();
                    int offset = offsetNearest(event.getX(), event.getY());
                    if (!openColorPickerAtTap(offset)
                            && !openDiagnosticAtTap(offset)) {
                        selectWordAt(event.getX(), event.getY());
                    }
                    clearComposing();
                } else {
                    requestFocus();
                    // LA PASTILLE D'ABORD (B4) : le
                    // doigt masque le texte — le tap sur un mot diagnostiqué
                    // pose une pastille au-dessus du mot, le tap sur la
                    // pastille ouvre la carte. L'OUVERTURE DIRECTE vit au
                    // DOUBLE-TAP sur le souligné (le stylet y garde son
                    // chemin précis), la pastille sert le doigt — elle
                    // COMPLÈTE le tap direct, elle ne le remplace pas
                    int offset = offsetNearest(event.getX(), event.getY());
                    if (tapDiagnosticBadge(event.getX(), event.getY())) {
                        // la pastille a pris le geste : la carte s'ouvre
                    } else {
                        // LE PLACEHOLDER DU PLI REPLIÉ (B5) : le tap
                        // au-delà de la fin visible de l'en-tête d'un
                        // pli replié ROUVRE le bloc (la leçon
                        // de la référence — son « ... » s'y clique) ; le
                        // curseur ne se pose pas dans la région cachée
                        int tapLine = buffer.lineOfOffset(
                                Math.min(offset, buffer.length()));
                        if (folds.isFolded(tapLine)
                                && foldBounds.foldStartsAt(tapLine)
                                && event.getX() > foldPlaceholderLeft(tapLine)) {
                            toggleFoldAt(tapLine);
                        } else {
                            placeCaretAt(event.getX(), event.getY());
                            // le tap sur un mot diagnostiqué y pose la
                            // pastille (elle meurt au geste qui déplace
                            // le curseur ailleurs)
                            offerDiagnosticBadge(offset);
                            clearComposing();
                            showSoftInput();
                        }
                    }
                }
                touch.rememberTap(event.getX(), event.getY());
                break;
            case MotionEvent.ACTION_CANCEL:
                // le système reprend le geste (la navigation par geste,
                // une vue parente) : les traînements se rangent comme
                // au lâcher — sinon la loupe resterait posée à jamais
                // et la barre de sélection ne naîtrait jamais (l'état
                // de poignée tenue tuerait les deux) ; le pincement
                // s'y range pareillement
                pinch.reset();
                downOnInfo = false;
                if (touch.isHandleDragging() || touch.isTextDragging()
                        || touch.isDragging()) {
                    edgeScroller.cancel();
                    touch.settle();
                    touch.forgetTap();
                    invalidate();
                }
                break;
            default:
                break;
        }
        return true;
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
                ? clampToRange((int) ((x + scrollX - getPaddingLeft()) / charWidth),
                        0, lineEnd - lineStart)
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
        float x = getPaddingLeft() + column * charWidth - scrollX;
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
        touch.holdsHandle(toStart <= toEnd ? 1 : 2,
                toStart <= toEnd ? selectionEnd() : selectionStart(), y);
    }

    /**
     * L'engagement du traînement sans poignée saisie (l'appui long
     * qui glisse) : la borne la plus proche du doigt devient la borne
     * traînée.
     */
    private void engageSelectionDrag(float x, float y) {
        float[] start = handleCenter(selectionStart());
        float[] end = handleCenter(selectionEnd());
        int bound = distanceTo(x, y, start[0], start[1])
                <= distanceTo(x, y, end[0], end[1]) ? 1 : 2;
        touch.holdsHandle(bound, bound == 1 ? selectionEnd() : selectionStart(), y);
    }

    /**
     * La borne traînée suit le doigt : l'offset sous le doigt borne
     * la sélection côté traîné, la borne fixe la garde de l'autre
     * côté — la sélection ne s'inverse pas en cours de traînement.
     * Le doigt près des bords fait défiler (une ligne par événement).
     */
    private void dragSelectionBound(float x, float y) {
        autoScrollNearEdges(x, y);
        touch.finger(x, y);
        int offset = offsetNearest(x, y);
        if (touch.draggedBound() == 1) {
            selection.setCaret(Math.min(offset, touch.fixedBound()));
            selection.setAnchor(touch.fixedBound());
        } else {
            selection.setCaret(Math.max(offset, touch.fixedBound()));
            selection.setAnchor(touch.fixedBound());
        }
        invalidate();
    }

    /**
     * Le défilement de suivi : le doigt dans la bande du haut ou du
     * bas fait défiler d'une ligne (la saisie suit alors le texte
     * révélé).
     */
    private void autoScrollNearEdges(float x, float y) {
        float band = dp(24);
        float top = getPaddingTop();
        float bottom = getHeight() - getPaddingBottom();
        float left = getPaddingLeft();
        float right = getWidth() - getPaddingRight();
        if (y < top + band) {
            scrollY = clampToRange(scrollY - lineHeight, 0.0f, maxScroll());
        } else if (y > bottom - band) {
            scrollY = clampToRange(scrollY + lineHeight, 0.0f, maxScroll());
        }
        // le traîné de la borne vers le bord latéral fait défiler
        // horizontalement (la colonne atteinte s'y révèle) — la
        // sélection suit le texte révélé
        if (x < left + band) {
            scrollX = clampToRange(scrollX - charWidth * 2.0f, 0.0f, maxScrollX());
        } else if (x > right - band) {
            scrollX = clampToRange(scrollX + charWidth * 2.0f, 0.0f, maxScrollX());
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
        lastFrameEngineNanos = 0L;
        // l'ancre de mémoire libre se relit à une trame sur
        // MEMORRY_ANCHOR_INTERVAL (le rapport a montré deux lectures
        // de l'environnement à CHAQUE image pour la pire trame —
        // l'échantillon suffit au relevé, la trame ordinaire n'en
        // paie plus aucune)
        if ((frameCount & (MEMORY_ANCHOR_INTERVAL - 1)) == 0) {
            memoryAnchor = Runtime.getRuntime().freeMemory();
        }
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
        // le corps du texte défile horizontalement : la translation
        // n'y porte QUE lui (la gouttière, les cartes et le bandeau
        // demeurent posés à la vue — le scrollX y est soustrait à la
        // main aux rares endroits qui convertissent un point)
        int textSave = canvas.save();
        canvas.translate(-scrollX, 0.0f);
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
            // LE SOFT WRAP (la leçon de la référence : la ligne qui
            // dépasse coupe à la largeur, chaque RANGÉE rend sa
            // tranche à sa ligne visuelle — la suite s'indente au
            // premier caractère non blanc)
            int rows = softWrap ? wrapModel.rowsOf(line) : 1;
            // L'EN-TÊTE REPLIÉ COUPE À SON OUVRANT (B6) : l'accolade
            // ouvrante est comprise dans ce
            // qui disparaît — « method { » replié n'affiche plus son
            // ouvrant, le placeholder « {...} » le remplace, l'écran
            // ne le montre pas deux fois
            boolean foldedHere = folds.isFolded(line);
            int drawEnd = end;
            if (foldedHere) {
                int cut = lastOpenerColumn(
                        buffer.getText(start, end).toString());
                if (cut >= 0) {
                    drawEnd = Math.max(start, start + cut);
                }
            }
            if (end > start) {
                if (rows <= 1) {
                    // LE CHIP DE COULEUR se peint SOUS le texte (le
                    // fond translucide y teinte le littéral, le
                    // liseré le borde — la leçon de la référence :
                    // le littéral reste LISIBLE, jamais caché) — la
                    // coupe du pli s'y respire aussi (les chips
                    // au-delà de l'ouvrant ne vivent plus, le
                    // placeholder les remplace)
                    drawColorChips(canvas, line, drawEnd, baseline);
                    drawTokens(canvas, line, start, drawEnd, baseline);
                } else {
                    drawWrappedLine(canvas, line, start, drawEnd, baseline,
                            drawEnd == end ? rows : 1);
                }
            }
            // LE PLACEHOLDER DU PLI REPLIÉ (la leçon de la référence :
            // la ligne d'en-tête d'un pli replié y montre « {... } » —
            // « /**...*/ » pour un bloc de documentation, « //... »
            // pour un commentaire, « ... » pour les imports) : la
            // nature du pli se lit au premier caractère de l'en-tête
            if (folds.isFolded(line)) {
                drawFoldPlaceholder(canvas, line, baseline);
            }
            drawInlayHint(canvas, line, drawEnd, baseline);
            if (composingStart >= 0 && rows <= 1) {
                drawComposeUnderline(canvas, line, start, drawEnd, baseline);
            }
        }
        drawStructure(canvas, first, last, count);
        drawCaret(canvas);
        drawSelectionHandles(canvas);
        drawDragPreview(canvas);
        canvas.restore();
        drawLoupe(canvas);
        // les candidats AVANT leur dessin (un déplacement de curseur
        // rend ses candidats dans la même trame), les soulignés de
        // diagnostic par-dessus le texte, le bandeau par-dessus tout,
        // la popup d'info par-dessus le bandeau — P0-4 du rapport :
        // ces mises à jour devraient partir des événements, pas de la
        // trame ; l'essai d'un masque de saleté les rendait
        // conditionnelles mais les états EXTERNES (presse-papiers,
        // réponses du serveur) vieillissent hors de nos événements —
        // la migration se refera un dérivé à la fois, éprouve à
        // l'appui
        refreshAnalysis();
        updateSuggestions();
        updateSignature();
        updateSelectionBar();
        updateQuickActions();
        drawDiagnostics(canvas, first, last, count);
        drawDiagnosticBadge(canvas);
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
        // la carte des usages, par-dessus tout : posée au caret, la
        // rangée choisie y navigue (le harnais la lit)
        if (usageCard != null && usageCard.isShown()) {
            int caretLineIndex = buffer.lineOfOffset(
                    Math.min(selection.caret(), buffer.length()));
            float caretTop = getPaddingTop()
                    + visibleIndexOf(caretLineIndex) * lineHeight - scrollY;
            usageCard.draw(canvas, caretTop, caretTop + lineHeight,
                    popupMetrics());
        }
        // les QUATRE nouvelles (le choix de cibles/la liste des problèmes/l'historique du presse-papiers/la structure du fichier du rapport) : posées au
        // caret pareillement — le choix de cibles, la liste des
        // problèmes, l'historique du presse-papiers, la structure (sa
        // ligne courante se lit au caret)
        if (targetCard != null && targetCard.isShown()) {
            int caretLineIndex = buffer.lineOfOffset(
                    Math.min(selection.caret(), buffer.length()));
            float caretTop = getPaddingTop()
                    + visibleIndexOf(caretLineIndex) * lineHeight - scrollY;
            targetCard.draw(canvas, caretTop, caretTop + lineHeight,
                    popupMetrics());
        }
        if (problemsCard != null && problemsCard.isShown()) {
            int caretLineIndex = buffer.lineOfOffset(
                    Math.min(selection.caret(), buffer.length()));
            float caretTop = getPaddingTop()
                    + visibleIndexOf(caretLineIndex) * lineHeight - scrollY;
            problemsCard.draw(canvas, caretTop, caretTop + lineHeight,
                    popupMetrics());
        }
        if (clipboardCard != null && clipboardCard.isShown()) {
            int caretLineIndex = buffer.lineOfOffset(
                    Math.min(selection.caret(), buffer.length()));
            float caretTop = getPaddingTop()
                    + visibleIndexOf(caretLineIndex) * lineHeight - scrollY;
            clipboardCard.draw(canvas, caretTop, caretTop + lineHeight,
                    popupMetrics());
        }
        if (structureCard != null && structureCard.isShown()) {
            int caretLineIndex = buffer.lineOfOffset(
                    Math.min(selection.caret(), buffer.length()));
            float caretTop = getPaddingTop()
                    + visibleIndexOf(caretLineIndex) * lineHeight - scrollY;
            structureCard.draw(canvas, caretTop, caretTop + lineHeight,
                    caretLineIndex, popupMetrics());
        }
        // le sélecteur de couleur, par-dessus tout : posé au littéral
        // #RRGGBB du caret, la teinte y vit sous le doigt
        if (colorPickerCard != null && colorPickerCard.isShown()) {
            int pickerLine = buffer.lineOfOffset(
                    Math.min(selection.caret(), buffer.length()));
            float pickerTop = getPaddingTop()
                    + visibleIndexOf(pickerLine) * lineHeight - scrollY;
            colorPickerCard.draw(canvas, pickerTop, pickerTop + lineHeight,
                    popupMetrics());
        }
        // une animation de popup vit (ouverture ou sortie) : la trame
        // suivante se réclame — le fondu se joue trame après trame,
        // sans fil d'horloge (la trame portante anime)
        if (completionCard.isAnimating() || infoPopup.isAnimating()
                || diagnosticCard.isAnimating()) {
            invalidate();
        }
        // LA MINIMAP par-dessus tout (la carte du document au bord
        // droit — la fenêtre courante s'y cadre, SA BARRE
        // d'annulation/rétablissement en tête) : les lignes du
        // document entier y passent, une rangée fine chacune
        int firstVisible = firstVisibleLine();
        int lastVisible = lineOfVisibleIndex(
                (int) ((getHeight() - getPaddingTop() + scrollY) / lineHeight));
        if (minimapEnabled) {
            minimap.setColors(theme.defaultColor(),
                    (themeBackground & 0x00FFFFFF) | 0x14000000, CARET_COLOR);
            minimap.draw(canvas, getHeight(), getWidth(), firstVisible,
                    Math.min(lastVisible, buffer.getLineCount() - 1),
                    buffer.getLineCount(),
                    line -> buffer.getLineEnd(line) - buffer.getLineStart(line));
        }
        // la fenêtre visible est payée : le reste remplit au fil de fond
        ensureFill();
        lastFrameNanos = System.nanoTime() - frameBegin;
        if (lastFrameNanos > worstFrameNanos) {
            worstFrameNanos = lastFrameNanos;
            worstFrameEngineNanos = lastFrameEngineNanos;
            // la mémoire rendue pendant la trame : un ramasse-miettes
            // qui y court laisse sa signature (un bond de mémoire
            // libre) — comparée à l'ancre échantillonnée, pas à
            // une lecture par image
            worstFrameMemoryDelta =
                    Runtime.getRuntime().freeMemory() - memoryAnchor;
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
        String query = search.query();
        int[] matches = search.matches();
        if (query == null || matches.length == 0) {
            return;
        }
        int queryLength = query.length();
        int windowStart = buffer.getLineStart(first);
        int windowEnd = last + 1 < count
                ? buffer.getLineStart(last + 1) : buffer.length();
        for (int m = 0; m < matches.length; m++) {
            int start = matches[m];
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
                    m == search.currentIndex() ? searchCurrentPaint : searchPaint);
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
    /**
     * La position (ligne, colonne) d'un protocole, bornée au texte
     * vivant : la ligne hors tampon rend -1 (la plage s'ignore), la
     * colonne hors ligne se borne à la fin de la ligne — les
     * coordonnées du serveur datent du texte qu'il a vu, jamais une
     * exception de dessin ne doit partir de là.
     *
     * @param line   la ligne
     * @param column la colonne (unités UTF-16)
     * @return l'offset borné, -1 si la ligne n'existe pas
     */
    private int offsetOfPositionClamped(int line, int column) {
        if (line < 0 || line >= buffer.getLineCount()) {
            return -1;
        }
        int lineEnd = buffer.getLineEnd(line);
        int lineStart = buffer.getLineStart(line);
        if (column < 0) {
            return lineStart;
        }
        return Math.min(lineStart + column, lineEnd);
    }

    private void drawServerHighlights(Canvas canvas, int first, int last, int count) {
        if (serverHighlights.isEmpty()) {
            return;
        }
        int windowStart = buffer.getLineStart(first);
        int windowEnd = last + 1 < count
                ? buffer.getLineStart(last + 1) : buffer.length();
        for (LspRange range : serverHighlights) {
            // les bornes du serveur datent du texte qu'il a vu — les
            // éditions depuis peuvent raccourcir la ligne : la colonne
            // y dépasse alors la ligne (l'appareil l'a vu en plantage
            // du dessin). La position se BORNE au texte vivant,
            // jamais ne lève : une plage hors texte s'ignore
            int start = offsetOfPositionClamped(
                    range.start().line(), range.start().character());
            int end = offsetOfPositionClamped(
                    range.end().line(), range.end().character());
            if (start < 0 || end < 0 || start >= end
                    || start >= windowEnd || end <= windowStart) {
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
     * Les régions repliables du serveur de langage (le protocole
     * foldingRange) : les bornes composées s'y posent — la gouttière
     * y porte leurs marqueurs, le toucher y replie (par-dessus les
     * plis du lexer). Null ou vide les range.
     *
     * @param ranges les régions repliables du serveur, null pour les
     *               ranger
     */
    public void setServerFoldRegions(List<LspFoldingRange> ranges) {
        serverFolds = ranges == null ? List.of() : List.copyOf(ranges);
        rebuildFoldLayout();
        invalidate();
    }

    /**
     * Les régions repliables déclarées par le serveur.
     *
     * @return les régions, jamais null (vide : aucune)
     */
    public List<LspFoldingRange> serverFoldRegions() {
        return serverFolds;
    }

    /**
     * Les indices à poser dans le texte (le protocole inlayHint) :
     * l'étiquette se dessine au bout du contenu de la ligne, l'encre
     * secondaire — le texte n'en bouge pas. Null ou vide les range.
     *
     * @param hints les indices du serveur, null pour les ranger
     */
    public void setInlayHints(List<LspInlayHint> hints) {
        inlayHints = hints == null ? List.of() : List.copyOf(hints);
        invalidate();
    }

    /**
     * Les indices posés dans le texte.
     *
     * @return les indices, jamais null (vide : aucun)
     */
    public List<LspInlayHint> inlayHints() {
        return inlayHints;
    }

    /**
     * Les jetons sémantiques du serveur (le protocole
     * semanticTokens) : la couche du protocole au-dessus de la
     * coloration locale — les plans de rendu se recuisent (c'est un
     * re-rendu, jamais une retokenisation : les jetons du moteur ne
     * bougent pas). Null ou vide les range.
     *
     * @param tokens les jetons du serveur, null pour les ranger
     */
    public void setServerSemantics(List<LspSemanticToken> tokens) {
        serverSemantics = tokens == null ? List.of() : List.copyOf(tokens);
        renderCache.invalidate();
        invalidate();
    }

    /**
     * Les jetons sémantiques posés par le serveur.
     *
     * @return les jetons, jamais null (vide : aucun)
     */
    public List<LspSemanticToken> serverSemantics() {
        return serverSemantics;
    }

    /**
     * L'indice du serveur sur une ligne (le protocole inlayHint) :
     * l'étiquette se dessine au bout du contenu de la ligne, l'encre
     * secondaire — le texte n'en bouge pas ; sans place à l'écran,
     * l'indice se tait.
     *
     * @param canvas   le canevas de la vue
     * @param line     la ligne visible courante
     * @param drawEnd  la coupe du pli (offset de fin révélée)
     * @param baseline la ligne de base de la ligne
     */
    private void drawInlayHint(Canvas canvas, int line, int drawEnd,
            float baseline) {
        if (inlayHints.isEmpty()) {
            return;
        }
        // la coupe du pli borne l'indice : l'indication au-delà de
        // l'ouvrant ne vit plus (le placeholder y naît)
        int revealCols = Math.min(drawEnd, buffer.getLineEnd(line))
                - buffer.getLineStart(line);
        for (LspInlayHint hint : inlayHints) {
            if (hint.position().line() != line) {
                continue;
            }
            int start = buffer.getLineStart(line);
            int end = buffer.getLineEnd(line);
            int column = Math.max(0, Math.min(hint.position().character(), end - start));
            if (column >= revealCols) {
                return;
            }
            float x = getPaddingLeft() + column * charWidth + dp(6);
            if (x + inlayPaint.measureText(hint.label()) < getWidth() - getPaddingRight()) {
                canvas.drawText(hint.label(), x, baseline, inlayPaint);
            }
            return;
        }
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
        if (infoPopup.text() == null && !infoPopup.isAnimating()) {
            return;
        }
        if (infoPopup.text() != null) {
            float anchorTop = getPaddingTop()
                    + visibleIndexOf(infoPopup.line()) * lineHeight - scrollY;
            // la barre de sélection vit : la documentation s'y empile —
            // la popup s'ancre au-dessus de la barre (le « i » l'a ouverte
            // sur la sélection)
            if (selectionBar.isShown()) {
                anchorTop = Math.min(anchorTop, selectionBar.poseY());
            }
            infoPopup.draw(canvas, anchorTop, popupMetrics());
        } else {
            // la sortie se joue sur la dernière pose (les ancrages ne
            // servent plus)
            infoPopup.draw(canvas, 0.0f, popupMetrics());
        }
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
        // le panneau d'abord : le fond translucide et l'ombre
        // d'élévation (la gouttière flotte au-dessus du texte — les
        // numéros et marqueurs s'y dessinent dessus)
        gutter.drawPanel(canvas, 0.0f, getHeight(),
                getPaddingLeft() - dp(8));
        int caretLine = caretLine();
        for (int line = first; line <= last && line < count; line++) {
            if (isHidden(line)) {
                continue;
            }
            gutter.drawNumber(canvas, line, lineBaseline(line),
                    getPaddingLeft() - dp(28));
            // la sévérité de la ligne — la PIRE y vit (l'erreur domine
            // l'avertissement, l'avertissement domine le
            // renseignement) : l'icône au trait de la référence y
            // suit sa couleur (rouge, ambre, accent)
            boolean error = false;
            boolean warning = false;
            boolean info = false;
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
                    } else if (hosted.severity()
                            == DiagnosticCard.SEVERITY_WARNING) {
                        warning = true;
                    } else {
                        info = true;
                    }
                }
            }
            float cy = lineBaseline(line)
                    + (fontMetrics.top + fontMetrics.bottom) / 2.0f;
            if (error) {
                gutter.drawMarker(canvas, Gutter.MARKER_ERROR, markerCx, cy);
            } else if (warning) {
                gutter.drawMarker(canvas, Gutter.MARKER_WARNING, markerCx, cy);
            } else if (info) {
                gutter.drawMarker(canvas, Gutter.MARKER_INFO, markerCx, cy);
            }
            if (breakpoints.contains(line)) {
                gutter.drawMarker(canvas, Gutter.MARKER_BREAKPOINT, markerCx, cy);
            }
            if (line == caretLine && !quickActions.isEmpty()) {
                gutter.drawMarker(canvas, Gutter.MARKER_LIGHTBULB, markerCx, cy);
            }
            // L'APERÇU COMPOSE (la leçon de la référence : son gestionnaire
            // y pose une icône au gutter de chaque fonction @Preview —
            // l'hôte y ouvre son panneau d'aperçu) : sa COLONNE PROPRE
            // vit ENTRE les numéros et le texte — écartée des numéros
            // (le pinch zoom resserre tout : deux dp d'écart au moins y
            // respirent), le pli demeure au bord du panneau
            if (lineCarriesPreview(line)) {
                gutter.drawMarker(canvas, Gutter.MARKER_PREVIEW,
                        getPaddingLeft() - dp(40), cy);
            }
            // LE MARQUEUR DE PLI DE LA GOUTTIÈRE (le retour de
            // terrain : il vivait dans drawStructure, DANS la
            // translation du corps — il défilait avec le texte) : le
            // replié porte son carré au plus, l'ouvert son chevron
            // haut et son filet — posé au panneau, hors défilement
            if (foldBounds.foldStartsAt(line)) {
                drawFoldMarker(canvas, line, folds.isFolded(line));
            }
        }
    }

    /**
     * Dessine la structure de L2 par-dessus les plans : les guides
     * VERTICAUX DES BLOCS (un trait par accolade appariée — à la
     * colonne de l'ouvrante, de la ligne après l'ouvrante à la ligne
     * avant la fermante : le fil relie les accolades) et le surlignage
     * de la paire du curseur (le marqueur de pli vivait ici : il
     * demeure désormais à la gouttière, hors translation du corps —
     * l'icône ne défile pas avec le texte). La PORTÉE ACTIVE s'y met
     * en évidence : le guide du bloc qui contient le caret (et celui
     * de l'accolade appariée qu'il touche) se trace à l'encre du
     * curseur, un peu plus épais — les autres restent une ombre de
     * structure.
     */
    private void drawStructure(Canvas canvas, int first, int last, int count) {
        drawBlockGuides(canvas, first, last, count);
        if (bracketPair == null) {
            return;
        }
        boolean fromVisible = highlightPairEnd(canvas, first, last,
                bracketPair.fromLine(), bracketPair.fromColumn());
        boolean toVisible = highlightPairEnd(canvas, first, last,
                bracketPair.toLine(), bracketPair.toColumn());
        // le FIL DE LA PAIRE (l'autre fil) : quand les deux bornes se
        // voient, une jambe verticale joint l'ouvrante à la fermante —
        // au bord GAUCHE de l'ouvrante, du bas de son cadre au haut de
        // celui de la fermante — puis un COUDE HORIZONTAL la rejoint
        // quand leurs colonnes diffèrent (le zigzag qui guide l'œil
        // de l'une à l'autre)
        if (fromVisible && toVisible) {
            drawPairLink(canvas);
        }
    }

    /**
     * Le fil de la paire d'accolades : la jambe verticale au bord
     * gauche de l'OUVRANTE, le coude horizontal vers la FERMANTE
     * quand leur colonne diffère — l'encre du curseur, un trait
     * assumé (il GUIDE l'œil de l'ouvrante à la fermante, il ne
     * chuchote pas). Les bornes à la même ligne ne se joignent pas
     * (les deux cadres se lisent seuls).
     */
    private void drawPairLink(Canvas canvas) {
        if (bracketPair.toLine() <= bracketPair.fromLine()) {
            return;
        }
        float left = getPaddingLeft();
        float fromX = left + bracketPair.fromColumn() * charWidth - dp(2);
        float toX = left + bracketPair.toColumn() * charWidth;
        float fromTop = getPaddingTop()
                + visibleIndexOf(bracketPair.fromLine()) * lineHeight - scrollY;
        float toTop = getPaddingTop()
                + visibleIndexOf(bracketPair.toLine()) * lineHeight - scrollY;
        float yFrom = fromTop + lineHeight - dp(2);
        float yTo = toTop + dp(2);
        if (yTo <= yFrom) {
            return;
        }
        pairLinkPaint.setStrokeWidth(Math.max(1.25f, charWidth * 0.09f));
        // la jambe verticale : du bas de l'ouvrante au haut de la fermante
        canvas.drawLine(fromX, yFrom, fromX, yTo, pairLinkPaint);
        // le coude : quand la fermante vit à une autre colonne, le
        // trait la rejoint horizontalement — la jonction touche les DEUX
        if (Math.abs(fromX - toX) > charWidth * 0.5f) {
            canvas.drawLine(fromX, yTo, toX, yTo, pairLinkPaint);
        }
    }

    /**
     * Les guides verticaux des blocs : la table des guides se bâtit
     * sur la fenêtre visible élargie (collée à la révision courante —
     * une table périmée ne s'y trace jamais), chaque guide se tire
     * de la ligne SOUS l'ouvrante à la ligne SUR la fermante, les
     * lignes cachées par un pli ne l'élargissent pas. Le guide de la
     * portée active (le bloc du caret) s'illumine à l'encre du
     * curseur.
     */
    private void drawBlockGuides(Canvas canvas, int first, int last,
            int count) {
        if (structure == null) {
            return;
        }
        StructureGuides table = StructureGuides.of(buffer, syntax, structure,
                first, Math.min(last, count - 1));
        if (table.size() == 0) {
            return;
        }
        int caretLine = caretLine();
        int caretColumn = selection.caret() - buffer.getLineStart(caretLine);
        int[] active = table.activeGuides(caretLine, caretColumn);
        float left = getPaddingLeft();
        float windowTop = getPaddingTop() - scrollY;
        float windowBottom = windowTop + visibleLineCount() * lineHeight;
        float accentWidth = Math.max(2.0f, charWidth * 0.11f);
        float shadowWidth = Math.max(1.0f, charWidth * 0.06f);
        guidePaint.setStrokeWidth(shadowWidth);
        guidePaint.setColor(GUIDE_SHADOW_COLOR);
        for (int at = 0; at < table.size(); at++) {
            Guide guide = table.guide(at);
            // les lignes cachées par un pli ne guident pas : la
            // hauteur visible de l'ouvrant décide du départ
            int fromVisible = visibleIndexOf(guide.openingLine());
            int toVisible = visibleIndexOf(guide.closingLine());
            float top = getPaddingTop() + (fromVisible + 1) * lineHeight
                    - scrollY;
            float bottom = getPaddingTop() + toVisible * lineHeight - scrollY;
            top = Math.max(top, windowTop);
            bottom = Math.min(bottom, windowBottom);
            if (bottom <= top) {
                continue;
            }
            float x = left + guide.column() * charWidth;
            if (contains(active, at)) {
                guidePaint.setStrokeWidth(accentWidth);
                guidePaint.setColor(GUIDE_ACCENT_COLOR);
            } else {
                guidePaint.setStrokeWidth(shadowWidth);
                guidePaint.setColor(GUIDE_SHADOW_COLOR);
            }
            canvas.drawLine(x, top, x, bottom, guidePaint);
        }
        // les réglages d'ombre reviennent (le pinceau se partage)
        guidePaint.setStrokeWidth(shadowWidth);
        guidePaint.setColor(GUIDE_SHADOW_COLOR);
    }

    /** Le tableau contient-il la valeur. */
    private static boolean contains(int[] table, int value) {
        for (int item : table) {
            if (item == value) {
                return true;
            }
        }
        return false;
    }

    /**
     * Le marqueur de pli dans la gouttière : un cadre et son signe —
     * moins quand le bloc est ouvert, plus quand il est replié.
     */
    private void drawFoldMarker(Canvas canvas, int line, boolean isFolded) {
        // LES CHEVRONS DANS LE PANNEAU DE LA GOUTTIÈRE à la référence
        // (EditorGutterComponentImpl y dessine ses chevrons dans la
        // COLONNE DES PLIS — APRÈS les numéros, DANS le panneau,
        // jamais dans le corps du texte) : le pli REPLIÉ porte son
        // carré au plus à la ligne d'ouverture ; le pli OUVERT porte
        // DEUX chevrons — haut à l'ouverture, bas à la fermeture —
        // joints par le filet vertical pointillé
        float baseline = lineBaseline(line);
        float mid = baseline + (fontMetrics.top + fontMetrics.bottom) / 2.0f;
        float cx = getPaddingLeft() - dp(10);
        if (isFolded) {
            gutter.drawFoldAnchor(canvas, Gutter.FOLD_COLLAPSED, cx, mid);
            return;
        }
        // le pli ouvert : les bornes hautes et basses, le filet entre
        int endLine = foldBounds.foldEndLine(line);
        if (endLine < 0) {
            gutter.drawFoldAnchor(canvas, Gutter.FOLD_COLLAPSED, cx, mid);
            return;
        }
        // LE CHEVRON DE L'OUVERTURE VIT (le retour de terrain
        // l'avait tué AVEC celui de la fermeture : le pli
        // ouvert ne portait plus AUCUNE icône — l'utilisateur ne voyait
        // plus où plier) : le « moins » dirigé vers le bas naît à la
        // ligne d'ouverture, SEUL — la ligne de l'accolade fermante ne
        // porte toujours rien (le chevron bas doublait l'information)
        gutter.drawFoldAnchor(canvas, Gutter.FOLD_EXPANDED_TOP, cx, mid);
        // LE PLI OUVERT NE MARQUE QUE SA NAISSANCE (le retour du
        // terrain : le chevron de la ligne fermante doublait
        // l'information et surchargeait le bord) : la BORNE BASSE et
        // son filet portent l'étendue, rien ne se dessine à la ligne
        // de l'accolade fermante
        if (endLine < buffer.getLineCount() && !isHidden(endLine)) {
            float endBaseline = lineBaseline(endLine);
            float endMid = endBaseline
                    + (fontMetrics.top + fontMetrics.bottom) / 2.0f;
            gutter.drawFoldLine(canvas, cx, mid + dp(6), endMid - dp(6));
        } else {
            // la borne basse cachée (pli dans le pli) : le filet va
            // jusqu'au bas de la fenêtre visible
            gutter.drawFoldLine(canvas, cx, mid + dp(6), getHeight());
        }
    }

    /**
     * Surligne un bout de paire s'il est visible : un cadre fin autour
     * du signe (la bande verticale de la ligne).
     */
    /**
     * Le cadre d'une borne de la paire du curseur — retourne sa
     * VISIBILITÉ (dans la fenêtre, pas cachée par un pli) : le fil de
     * la paire ne se tire qu'entre deux bornes qui se voient.
     */
    private boolean highlightPairEnd(Canvas canvas, int first, int last,
            int line, int column) {
        if (line < first || line > last || isHidden(line)) {
            return false;
        }
        float baseline = lineBaseline(line);
        float x = getPaddingLeft() + column * charWidth;
        canvas.drawRect(x - dp(1), baseline + fontMetrics.top + dp(2),
                x + charWidth + dp(1), baseline + fontMetrics.bottom - dp(2),
                bracketPaint);
        return true;
    }

    // --------------------------------------------------- plis (régions repliables)

    /**
     * Le doigt sur la colonne des marqueurs de la gouttière : LE
     * MARQUEUR SEUL ouvre sa carte (la leçon de la référence : son
     * icône d'erreur gouttière s'y clique — la LIGNE sans marqueur
     * ne prend rien, le caret ne bouge pas au tap dans la gouttière
     * vide) — le rond d'erreur ouvre la carte de diagnostic,
     * l'ampoule de la ligne du curseur ouvre ses actions rapides.
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
        if (line == caretLine() && !quickActions.isEmpty()) {
            return openQuickActionsAtLine(line);
        }
        return false;
    }

    /**
     * La ligne porte-t-elle une annotation @Preview (la fonction
     * d'aperçu Compose — la référence y pose l'icône de son
     * gestionnaire) : l'annotation se lit dans la fenêtre de la
     * ligne, les lignes au-dessus à un blanc près (l'annotation
     * surplombe sa fonction).
     *
     * @param line l'indice de la ligne
     * @return vrai quand une annotation @Preview y vit
     */
    private boolean lineCarriesPreview(int line) {
        int look = Math.max(0, Math.min(line, buffer.getLineCount() - 1));
        for (int up = 0; up <= 2 && look - up >= 0; up++) {
            int at = look - up;
            String text = buffer.getText(buffer.getLineStart(at),
                    buffer.getLineEnd(at)).toString().strip();
            if (text.isEmpty()) {
                continue;
            }
            if (text.contains("@Preview")) {
                return true;
            }
            // une autre annotation ou la signature se lit : la
            // fenêtre de recherche s'arrête à la première ligne de
            // code non vide
            if (!text.startsWith("@") && !text.startsWith("fun")) {
                break;
            }
        }
        return false;
    }

    /**
     * Pose ou retire le point d'arrêt d'une ligne (le tap de la
     * gouttière l'appelle — un point posé s'y retire, une ligne
     * libre en pose un). Le geste premier de la gouttière demeure
     * les marqueurs d'erreur ; le point d'arrêt se pose au tap
     * simple d'une ligne sans marqueur.
     *
     * @param line la ligne
     * @return vrai quand la gouttière a pris le geste
     */
    public boolean toggleBreakpoint(int line) {
        if (line < 0 || line >= buffer.getLineCount()) {
            return false;
        }
        if (breakpoints.contains(line)) {
            breakpoints.remove(line);
        } else {
            breakpoints.add(line);
        }
        invalidate();
        return true;
    }

    /**
     * Les lignes en point d'arrêt (l'hôte y branche son débogueur,
     * le harnais y vérifie la pose).
     *
     * @return les lignes triées, jamais null
     */
    public java.util.SortedSet<Integer> breakpoints() {
        return java.util.Collections.unmodifiableSortedSet(breakpoints);
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
        int line = buffer.lineOfOffset(Math.min(offset, buffer.length()));
        int column = Math.min(offset, buffer.length()) - buffer.getLineStart(line);
        // LES DIAGNOSTICS DU SERVEUR D'ABORD (leur plage exacte — la
        // leçon de la référence : findHighlightByOffset à l'OFFSET du
        // doigt, jamais la ligne entière) : le serveur dit sa plage
        if (diagnosticProvider != null
                && diagnosticProvider.diagnosticCovers(line, column)
                && openDiagnosticsAtLine(line)) {
            return true;
        }
        if (diagnostics == null || analysisRevision != buffer.getRevision()) {
            return false;
        }
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
        if (!foldBounds.foldStartsAt(line)) {
            return;
        }
        if (folds.isFolded(line)) {
            folds.unfold(line);
        } else {
            folds.fold(line);
            // le curseur dans la région repliée : il remonte à la fin de
            // l'en-tête (juste après l'ouvrant, la paire s'y encadre)
            int end = foldBounds.foldEndLine(line);
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
            int container = folds.headerHiding(line, foldBounds);
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
        folds.rebuild(foldBounds, trackedLineCount);
    }

    /**
     * Les bornes des plis composées : l'ouvrant du LEXER d'abord (le
     * pli local prime), la région du SERVEUR complète (le protocole
     * foldingRange par-dessus).
     */
    private final class FoldComposite implements FoldBounds {

        @Override
        public boolean foldStartsAt(int line) {
            if (structure != null && structure.foldStartsAt(line)) {
                return true;
            }
            return serverFoldEnd(line) >= 0;
        }

        @Override
        public int foldEndLine(int header) {
            if (structure != null && structure.foldStartsAt(header)) {
                return structure.foldEndLine(header);
            }
            return serverFoldEnd(header);
        }

        /**
         * La fin de la région du serveur ouverte à cette ligne (-1 :
         * aucune).
         *
         * @param header la ligne d'en-tête
         * @return la dernière ligne couverte, -1 sans région
         */
        private int serverFoldEnd(int header) {
            for (LspFoldingRange range : serverFolds) {
                if (range.startLine() == header) {
                    return range.endLine();
                }
            }
            return -1;
        }
    }

    /** La ligne est-elle cachée par un pli. */
    private boolean isHidden(int line) {
        return folds.isHidden(line);
    }

    /** L'indice visible d'une ligne (les plis compactent la hauteur). */
    private int visibleIndexOf(int line) {
        return layout.visibleIndexOf(line);
    }

    /**
     * Les mesures courantes de la vue nourrissent le modèle de
     * géométrie (le bord haut, l'interligne, la colonne, la hauteur
     * visible) — au zoom comme à la taille.
     */
    private void syncLayoutMetrics() {
        layout.metrics(new LayoutModel.Metrics(getPaddingTop(),
                lineHeight, charWidth,
                getHeight() - getPaddingTop() - getPaddingBottom()));
    }

    /** Nombre de lignes visibles (le document moins les cachées, plus les rangées du wrap). */
    private int visibleLineCount() {
        return layout.visibleRowCount();
    }

    /**
     * La ligne visible d'indice donné : la dernière ligne dont
     * l'indice visible la précède ou l'égale (les cachées partagent
     * l'indice de la visible qui les suit).
     */
    private int lineOfVisibleIndex(int index) {
        return layout.lineOfVisibleIndex(index);
    }

    /** Ligne de base d'une ligne VISIBLE (le mapping des plis s'applique). */
    private float lineBaseline(int line) {
        int visible = folds.visibleIndexOf(line);
        return getPaddingTop() - fontMetrics.top + visible * lineHeight - scrollY;
    }

    /**
     * Dessine la pastille du diagnostic : l'AMPOULE au trait — LA MÊME
     * que l'étage des correctifs de la carte (le retour du terrain :
     * le petit icône du trait diagnostiqué doit ouvrir la même lecture
     * que la carte — le corps en cercle, le culot en barres, l'encre à
     * l'accent) — vivant AU-DESSUS DU MOT diagnostiqué (la leçon de la
     * référence : son ampoule flotte au-dessus du curseur ; la
     * gouttière porte déjà LE marqueur du diagnostic — la pastille
     * n'y parle plus de la ligne, elle parle du mot). Elle NAÎT par
     * le POP de la référence : l'échelle part de la MOITIÉ, ancrée
     * au pied de la pastille (elle monte depuis la ligne vers le
     * mot), l'opacité suit — le ressort amorti y joue sa signature.
     * Elle meurt à son échéance, le dessin ne la rend plus.
     */
    private void drawDiagnosticBadge(Canvas canvas) {
        if (diagnosticBadgeLine < 0
                || System.nanoTime() > diagnosticBadgeUntilNanos
                || isHidden(diagnosticBadgeLine)) {
            return;
        }
        float cx = diagnosticBadgeCenterX();
        float cy = diagnosticBadgeCenterY();
        float side = dp(18);
        // le pop de naissance (la référence : l'échelle part de la
        // moitié, ancrée AU PIED de la pastille — l'ampoule monte
        // depuis la ligne vers le mot), le fondu au-dessus
        float scale = badgePop.scale();
        float alpha = badgePop.alpha();
        int save = canvas.save();
        if (scale != 1.0f) {
            float pivotY = cy + side / 2.0f;
            canvas.translate(cx, pivotY);
            canvas.scale(scale, scale);
            canvas.translate(-cx, -pivotY);
        }
        canvas.saveLayerAlpha(cx - side, cy - side, cx + side, cy + side,
                Math.max(0, Math.min(255, (int) (alpha * 255.0f))));
        BulbIcon.draw(canvas, badgeBulbPaint, cx, cy, side);
        canvas.restore();
        canvas.restore();
        if (badgePop.isRunning()) {
            invalidate();
        }
    }

    /**
     * L'abscisse du CENTRE de l'ampoule vivante : le MOT la porte —
     * le premier caractère de la plage diagnostiquée (la leçon de la
     * référence : son ampoule se pose au-dessus du curseur) — BORNÉE
     * au texte (le mur gauche de la gouttière, le mur droit de la
     * vue : la pastille ne retourne jamais dans la gouttière, les
     * marqueurs y vivent leur vie).
     *
     * @return l'abscisse en pixels, {@link Float#MAX_VALUE} si aucune
     *         pastille ne vit
     */
    public float diagnosticBadgeCenterX() {
        if (diagnosticBadgeLine < 0) {
            return Float.MAX_VALUE;
        }
        float side = dp(18);
        float x = getPaddingLeft()
                + diagnosticBadgeColumn * charWidth
                + charWidth / 2.0f;
        return clampToRange(x, getPaddingLeft() + side / 2.0f,
                Math.max(getPaddingLeft() + side / 2.0f,
                        getWidth() - side / 2.0f));
    }

    /**
     * La TRANCHE du plan d'une ligne entre deux colonnes (le rendu du
     * soft wrap : la rangée y dessine SES colonnes du plan cuit, le
     * décalage d'indent y pose la suite au premier caractère non
     * blanc de la ligne) — le peintre y rend la tranche, la vue
     * tranche les bornes.
     */
    private void drawTokenRange(Canvas canvas, int line, int start,
            int fromCol, int toCol, float baseline, int indentCols) {
        long engineBegin = System.nanoTime();
        SyntaxDocument.LineTokens entry = syntax.lineTokens(line);
        lastFrameEngineNanos += System.nanoTime() - engineBegin;
        int lineEnd = buffer.getLineEnd(line);
        LinePlan plan = renderCache.frame(line, entry, fusionLabel(line),
                () -> buffer.getText(start, lineEnd), this::bakeLine);
        painter.drawTokenRange(canvas, plan, fromCol, toCol,
                getPaddingLeft() + indentCols * charWidth, baseline);
    }

    /**
     * La ligne coupée du soft wrap : chaque RANGÉE rend sa tranche à
     * SA ligne visuelle (la baseline avance d'un interligne par
     * rangée), la suite s'indente au premier caractère non blanc de la
     * ligne (la leçon de la référence — la continuité visuelle du
     * bloc s'y lit). Le plan cuit de la ligne entière sert de source :
     * chaque rangée y dessine SES colonnes.
     */
    private void drawWrappedLine(Canvas canvas, int line, int start, int end,
            float baseline, int rows) {
        int wrapCols = wrapModel.rowStartColumn(line, 1) > 0
                ? wrapModel.rowStartColumn(line, 1) : 40;
        // l'indent de suite : le premier non-blanc de la ligne
        int indent = 0;
        while (start + indent < end && buffer.charAt(start + indent) == ' ') {
            indent++;
        }
        int columns = (int) ((getWidth() - getPaddingLeft() - getPaddingRight())
                / charWidth);
        int step = Math.max(8, columns - indent);
        for (int row = 0; row < rows; row++) {
            int fromCol = row == 0 ? 0 : wrapCols + (row - 1) * step;
            // la première rangée se borne à la coupe du pli aussi (un
            // en-tête replié coupé avant sa largeur de rangée n'en
            // montre pas plus : le placeholder y remplace la suite)
            int toCol = row == 0 ? Math.min(wrapCols, end - start)
                    : Math.min(end - start, wrapCols + row * step);
            if (fromCol >= end - start) {
                break;
            }
            float rowBaseline = baseline + row * lineHeight;
            // la suite s'indente : ses colonnes se décalent de
            // l'indent (la première rangée demeure au bord)
            drawTokenRange(canvas, line, start, fromCol, toCol, rowBaseline,
                    row == 0 ? 0 : indent);
        }
    }

    /**
     * La borne gauche du placeholder d'une ligne d'en-tête repliée —
     * le tap au-delà rouvre le bloc : à l'OUVRANT de l'en-tête quand
     * il en porte un (le placeholder y naît), à la FIN du texte
     * visible sinon (l'en-tête sans ouvrant s'affiche entier).
     *
     * @param line l'indice de la ligne d'en-tête
     * @return l'abscisse où le placeholder commence
     */
    private float foldPlaceholderLeft(int line) {
        String head = buffer.getText(buffer.getLineStart(line),
                buffer.getLineEnd(line));
        // LA BORNE TACTILE SUIT LE PLACEHOLDER (coordonnées de la vue —
        // le doigt n'y vit pas dans le canevas translaté) : sur
        // l'OUVRANT quand l'en-tête en porte un, à la FIN du texte
        // visible sinon (l'en-tête sans ouvrant s'affiche entier — le
        // tap au-delà de SA fin rouvre, jamais la colonne −1 de
        // l'ouvrant manquant qui mangeait toute la ligne)
        int column = lastOpenerColumn(head);
        if (column < 0) {
            column = head.length();
        }
        return getPaddingLeft() + column * charWidth - scrollX;
    }

    /**
     * Le placeholder du pli replié : la nature du pli se lit à son
     * ouvrant — « {…} » pour un bloc, « (…) » pour un groupe, « […] »
     * pour un tableau, « … » seul quand l'en-tête n'ouvre rien (les
     * imports). L'ouvrant déjà présent dans l'en-tête ne se répète
     * pas : le placeholder naît SUR lui (le bug d'usage B5 : le
     * dessin d'avant posait « {...} » après un en-tête finissant par
     * « { » — l'écran montrait « method {{...} ») ; l'en-tête sans
     * ouvrant s'affiche entier et le placeholder suit SA FIN (jamais
     * la colonne −1 — il vivait dans la gouttière avant le retour de
     * terrain).
     *
     * @param canvas   le canevas de la vue
     * @param line     l'indice de la ligne d'en-tête
     * @param baseline la ligne de base du texte
     */
    private void drawFoldPlaceholder(Canvas canvas, int line, float baseline) {
        String head = buffer.getText(buffer.getLineStart(line),
                buffer.getLineEnd(line));
        String placeholder = foldPlaceholderText(line, head);
        // LA COLONNE DU PLACEHOLDER : l'OUVRANT quand l'en-tête en
        // porte un (il naît SUR lui — B5), la FIN du texte visible
        // sinon (l'en-tête sans ouvrant s'affiche entier : « import x; … »)
        // — jamais la colonne −1 qui le posait dans LA GOUTTIÈRE
        int column = lastOpenerColumn(head);
        if (column < 0) {
            column = head.length();
        }
        // AUCUN scrollX ici : le dessin vit dans le canevas translaté
        // du corps (la translation y porte déjà le défilement — la
        // soustraction d'avant le comptait DEUX fois, le placeholder
        // dérivait à gauche dès que la vue défilait)
        float x = getPaddingLeft() + column * charWidth;
        painter.drawFoldPlaceholder(canvas, placeholder, x, baseline);
    }

    /**
     * Le texte du placeholder : « ouvrant…fermant » quand l'en-tête
     * ouvre et que la ligne de fin ferme (le fermant d'origine y
     * survit avec son éventuel suffixe), « … » sinon. Le commentaire
     * de bloc replié garde sa forme « /&#42;…&#42;/ ».
     */
    private String foldPlaceholderText(int line, String head) {
        String trimmed = head.stripLeading();
        if (trimmed.startsWith("/*")) {
            return "/*...*/";
        }
        if (trimmed.startsWith("//")) {
            return "//...";
        }
        int openerColumn = lastOpenerColumn(head);
        if (openerColumn < 0) {
            return "...";
        }
        char opener = head.charAt(openerColumn);
        char closer = closerOf(opener);
        if (closer == 0) {
            return "...";
        }
        int end = foldBounds.foldEndLine(line);
        String endText = end < buffer.getLineCount()
                ? buffer.getText(buffer.getLineStart(end),
                        buffer.getLineEnd(end)).strip() : "";
        // le fermant d'origine survit : le premier fermant de la
        // nature convient, son suffixe (« ; », « ) ») reste au texte
        int closerAt = endText.indexOf(closer);
        if (closerAt >= 0) {
            return opener + "..." + endText.substring(closerAt,
                    Math.min(endText.length(), closerAt + 3));
        }
        return opener + "..." + closer;
    }

    /**
     * La colonne du DERNIER ouvrant de l'en-tête ({@code -1} s'il
     * n'en porte pas) : le dessin du placeholder y naît, l'ouvrant
     * visible reste le sien (aucun doublon).
     */
    private static int lastOpenerColumn(String head) {
        for (int at = head.length() - 1; at >= 0; at--) {
            char sign = head.charAt(at);
            if (sign == '{' || sign == '(' || sign == '[') {
                return at;
            }
        }
        return -1;
    }

    /** Le fermant de la nature de l'ouvrant, 0 s'il n'en a pas. */
    private static char closerOf(char opener) {
        switch (opener) {
            case '{':
                return '}';
            case '(':
                return ')';
            case '[':
                return ']';
            default:
                return 0;
        }
    }

    private void drawTokens(Canvas canvas, int line, int start, int drawEnd,
            float baseline) {
        long engineBegin = System.nanoTime();
        SyntaxDocument.LineTokens entry = syntax.lineTokens(line);
        lastFrameEngineNanos += System.nanoTime() - engineBegin;
        int lineEnd = buffer.getLineEnd(line);
        LinePlan plan = renderCache.frame(line, entry, fusionLabel(line),
                () -> buffer.getText(start, lineEnd), this::bakeLine);
        // LA COUPE DU PLI S'APPLIQUE VRAIMENT (le retour de terrain
        // sur les plis : les segments au-delà de l'ouvrant ne se
        // dessinent pas, celui qui l'englobe se tranche) — le peintre
        // y peint la coupe, la vue tranche la fenêtre
        int reveal = Math.min(drawEnd, lineEnd);
        painter.drawTokens(canvas, plan, reveal - start,
                getPaddingLeft(), baseline);
        // LES LIGATURES (la leçon de la référence : FiraCode y fond
        // les paires d'opérateurs en UN glyphe) par-dessus le plan
        drawLigatures(canvas, line, reveal, baseline);
    }

    /**
     * Les CHIPS DE COULEUR (la leçon de la référence : son éditeur y
     * encadre chaque littéral hexadécimal DE SA PROPRE COULEUR —
     * #efe vit dans un encadré #efe) : la passe relit la ligne au
     * tampon, chaque littéral #RGB/#RGBA/#RRGGBB/#RRGGBBAA porte un
     * rectangle arrondi rempli de sa couleur, liseré discret — le
     * double-tap sur le littéral ouvre le sélecteur.
     */
    private void drawColorChips(Canvas canvas, int line, int drawEnd,
            float baseline) {
        int lineStart = buffer.getLineStart(line);
        int lineEnd = buffer.getLineEnd(line);
        // la coupe du pli borne la fenêtre : les chips au-delà de
        // l'ouvrant ne vivent plus (le placeholder y naît)
        int window = Math.min(Math.min(lineEnd, drawEnd) - lineStart, 512);
        CharSequence text = buffer.getText(lineStart, lineStart + window);
        painter.drawColorChips(canvas, text, window, getPaddingLeft(),
                baseline);
    }

    /**
     * Les LIGATURES d'opérateurs dessinées (la leçon de la
     * référence : FiraCode y fond les paires d'opérateurs en UN
     * glyphe) — la vue tranche la fenêtre de la ligne (la coupe du
     * pli borne), le peintre y peint les glyphes unifiés.
     */
    private void drawLigatures(Canvas canvas, int line, int reveal,
            float baseline) {
        int lineStart = buffer.getLineStart(line);
        int lineEnd = buffer.getLineEnd(line);
        // la coupe du pli borne la fenêtre : les ligatures au-delà de
        // l'ouvrant ne vivent plus (le placeholder y naît)
        int window = Math.min(Math.min(lineEnd, reveal) - lineStart, 512);
        CharSequence text = buffer.getText(lineStart, lineStart + window);
        painter.drawLigatures(canvas, text, window, getPaddingLeft(),
                baseline);
    }

    /**
     * L'étiquette de fusion d'une ligne : ce que les couches au-dessus
     * du lexer (L3 les annotations, L4 les jetons du serveur) y disent
     * — la clé de partage du cache s'en scelle. Deux lignes de même
     * texte ne partagent un plan QUE si leurs couches y disent la
     * même chose (la ligne sans jeton serveur garde la couleur du
     * lexer, sa jumelle couverte prend la sienne). L'étiquette se
     * bâtit sans allocation courante : un entier mixte (le compte des
     * annotations, l'empreinte des jetons du serveur de la ligne).
     *
     * @param line l'indice de la ligne
     * @return l'étiquette, la chaîne vide sans aucune couche
     */
    private String fusionLabel(int line) {
        boolean served = false;
        if (!serverSemantics.isEmpty()) {
            for (LspSemanticToken token : serverSemantics) {
                if (token.range().start().line() == line) {
                    served = true;
                    break;
                }
            }
        }
        int notes = semantic == null ? 0 : semantic.annotationsAt(line).size();
        if (!served && notes == 0) {
            return "";
        }
        return (served ? "s" : "-") + notes;
    }

    /**
     * Cuit le plan d'une ligne : textes tranchés jusqu'au plafond de
     * dessin, couleurs résolues depuis les portées, colonnes de début
     * et abscisses en pixels (la marge s'ajoute au dessin ; la coupe du
     * pli et la tranche de rangée lisent les colonnes). Une ligne sans
     * jeton (ligne monstre, hors plafond moteur) se cuit en texte brut.
     */
    private LinePlan bakeLine(int line, Object stamp, CharSequence content) {
        SyntaxDocument.LineTokens entry = (SyntaxDocument.LineTokens) stamp;
        if (entry.tokenCount() == 0) {
            int drawn = Math.min(content.length(), MAX_DRAWN_COLUMNS);
            return new LinePlan(
                    new String[] {content.subSequence(0, drawn).toString()},
                    new int[] {theme.defaultColor()},
                    new int[] {0},
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
        int[] cols = new int[kept];
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
            // la colonne de début vit au plan (la coupe du pli et la
            // tranche de rangée y lisent des colonnes — l'abscisse
            // seule y mensongeait à chasse réelle)
            cols[at] = start;
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
            // fusion L4 : le serveur prime sur l'atelier — le jeton qui
            // couvre le span y porte sa couleur (le genre à la palette),
            // la déclaration y porte le gras ; un genre sans nature
            // dans la palette laisse la couleur d'avant (la chaîne
            // ouverte ne casse jamais le rendu). La règle de L3 y vit
            // aussi : SEUL un neutre se recolore — le serveur de démo
            // dit un jeton par mot du texte brut, les mots des
            // commentaires et des chaînes n'y changent pas la couleur
            // locale (la fusion ne casse pas la coloration)
            if (scope == null && !serverSemantics.isEmpty()) {
                for (LspSemanticToken served : serverSemantics) {
                    if (served.range().start().line() == line
                            && served.range().start().character() < endColumn
                            && served.range().end().character() > start) {
                        String nature = EditorThemes.serverTokenNature(served.type());
                        if (nature != null) {
                            colors[at] = EditorThemes.semanticColor(theme, nature);
                            bolds[at] = served.modifiers().contains("declaration");
                        }
                        break;
                    }
                }
            }
            at++;
        }
        return new LinePlan(texts, colors, cols, xs, bolds);
    }

    // -------------------------------------------- complétion et diagnostics

    /**
     * Les candidats du mot courant : le préfixe se lit au curseur (au
     * moins deux caractères, sinon le bandeau se vide), la requête
     * interroge le dernier index — un index périmé reste une bonne
     * indication (les noms, pas les positions). Le bandeau ne vit PAS
     * pendant un geste (le doigt traîne une borne, le texte glisse :
     * le tap qui suit doit atteindre le texte, pas la carte — la
     * leçon du double-tap) ni sur une sélection vivante (le mot n'y
     * se tape pas).
     */
    private void updateSuggestions() {
        if (touch.isHandleDragging() || touch.isTextDragging()) {
            completionCard.set(List.of(), "");
            suggestionWordStart = -1;
            return;
        }
        // LA COMPOSITION N'EST PAS UNE SÉLECTION (la leçon de la
        // référence : la lookup vit PENDANT la frappe — le clavier
        // logiciel compose le mot, la zone de composition n'est pas
        // une ancre de sélection) : la carte survit à la composition,
        // elle se re-filtre au préfixe qui croît. Une sélection VIVANTE
        // (le doigt a borné une étendue) range la carte seule.
        if (hasSelection()) {
            completionCard.set(List.of(), "");
            suggestionWordStart = -1;
            return;
        }
        int caret = Math.min(selection.caret(), buffer.length());
        int wordStart = wordStartAt(caret);
        if (wordStart < 0) {
            completionCard.set(List.of(), "");
            suggestionWordStart = -1;
            return;
        }
        String prefix = buffer.getText(wordStart, caret).toString();
        suggestionWordStart = wordStart;
        // LA CARTE NE MEURT JAMAIS À LA FRAPPE (la leçon de Sora :
        // onContentChange INSERT y demande la complétion TANT QUE la
        // frappe reste sur SA LIGNE — la carte ouverte reste ouverte,
        // le préfixe croît, elle se re-filtre) : un préfixe qui ne
        // dit plus rien (aucun mot-clé, aucun symbole, aucun mot)
        // ne range PAS la carte vivante — la carte retient sa
        // dernière liste filtrée (les noms qui poursuivent le
        // préfixe vivent seuls), elle ne se range qu'au mot mort
        // (préfixe vide, caret parti ailleurs).
        List<CompletionCard.Entry> merged = new ArrayList<>(
                localCompletionEntries(prefix));
        if (suggestionProvider != null) {
            int line = buffer.lineOfOffset(caret);
            List<CompletionCard.Entry> served = suggestionProvider.completionsFor(
                    prefix, line, caret - buffer.getLineStart(line));
            for (CompletionCard.Entry servedEntry : served) {
                if (servedEntry == null || servedEntry.name().isEmpty()) {
                    continue;
                }
                boolean duplicate = false;
                for (CompletionCard.Entry entry : merged) {
                    if (entry.name().equals(servedEntry.name())) {
                        duplicate = true;
                        break;
                    }
                }
                if (!duplicate) {
                    merged.add(servedEntry);
                }
            }
        }
        // la carte VIVANTE dont la fraîche liste se vide garde sa
        // liste d'avant FILTRÉE au préfixe neuf (Sora : la carte ne
        // se referme pas à la frappe — l'index court, ses candidats
        // d'avant restent une indication)
        if (merged.isEmpty() && completionCard.isShown()) {
            List<CompletionCard.Entry> kept = new ArrayList<>();
            for (CompletionCard.Entry entry : completionCard.entries()) {
                if (entry.name().regionMatches(true, 0, prefix, 0,
                        prefix.length())) {
                    kept.add(entry);
                }
            }
            merged = kept;
        }
        // LA DOCUMENTATION VIT DANS LA CARTE : la documentation du
        // candidat résolue (l'asynchrone du serveur) survit à la
        // re-fusion de la trame — l'entrée neuve du fournisseur peut
        // porter son documentation vide (le cache d'en-dessous peut
        // n'avoir pas encore la réponse) : l'entrée D'AVANT la lui
        // rend (le nom dit l'identité, le contenu demeure)
        if (completionCard.isShown()) {
            for (int at = 0; at < merged.size(); at++) {
                CompletionCard.Entry fresh = merged.get(at);
                if (fresh.documentation().isEmpty()) {
                    CompletionCard.Entry old = completionCard
                            .documentationOf(fresh.name());
                    if (old != null) {
                        merged.set(at, new CompletionCard.Entry(fresh.name(),
                                fresh.detail(), fresh.kind(), fresh.deprecated(),
                                old.documentation(), old.markdownDoc(),
                                fresh.insertText(), fresh.insertStart(),
                                fresh.insertEnd()));
                    }
                }
            }
        }
        completionCard.set(merged, prefix);
        askChosenDocumentation();
    }

    /**
     * La documentation du candidat CHOISI se demande : la
     * documentation PORTÉE par l'entrée s'y montre d'elle-même (le
     * panneau la porte) ; celle qui manque part au RÉSOLVEUR (le
     * {@code completionItem/resolve} du protocole — le serveur y
     * répond, le fil d'interface n'attend JAMAIS : la réponse rend
     * sur le fil de la vue et remplit l'entrée, la trame suivante la
     * porte). Sans résolveur, la documentation manquante reste vide
     * (le panneau se range — la carte ne meurt pas d'un panneau
     * absent).
     */
    private void askChosenDocumentation() {
        if (completionDocResolver == null) {
            return;
        }
        int at = completionCard.highlighted();
        if (at < 0 || at >= completionCard.entries().size()) {
            return;
        }
        CompletionCard.Entry chosen = completionCard.entries().get(at);
        if (!chosen.documentation().isEmpty()) {
            return;
        }
        // LA RÉSOLUTION EN VOL ne se RE-PART PAS (le défaut du rapport
        // des popups — la documentation arrive tard : sans garde, la
        // trame suivante redemandait CE qu'un serveur lent n'avait pas
        // encore rendu — la tempête de questions)
        if (chosen.name().equals(pendingDocName)) {
            return;
        }
        pendingDocName = chosen.name();
        final String name = chosen.name();
        completionDocResolver.documentationFor(chosen, (documentation, markdown) ->
                post(() -> {
                    if (name.equals(pendingDocName)) {
                        pendingDocName = null;
                    }
                    if (documentation != null && !documentation.isEmpty()
                            && completionCard.setDocumentation(name, documentation,
                                    markdown)) {
                        invalidate();
                    }
                }));
    }

    /**
     * Les candidats détails de la complétion locale : les mots-clés du
     * langage d'abord (la complétion de base SANS serveur — la leçon
     * de la référence), puis l'index local des symboles (la nature y
     * porte l'icône, la nature et la ligne de déclaration portent la
     * colonne de droite — la facture de l'atelier). Le serveur
     * branché évince le tout (son fournisseur y prime).
     *
     * @param prefix le préfixe tapé
     * @return les candidats détaillés, jamais null
     */
    private List<CompletionCard.Entry> localCompletionEntries(String prefix) {
        // les mots-clés du langage d'abord (la leçon de la référence :
        // sa liste statique y porte le bandeau SANS serveur — la
        // complétion de base naît du langage lui-même), puis les
        // symboles et les mots de l'index local (le serveur branché
        // y évince tout : son SuggestionProvider prime sur le local)
        List<CompletionCard.Entry> entries = new ArrayList<>();
        if (language != null) {
            for (String keyword : LanguageKeywords.of(language)) {
                if (prefix.isEmpty() || keyword.regionMatches(true, 0,
                        prefix, 0, prefix.length())) {
                    entries.add(new CompletionCard.Entry(keyword,
                            "mot-clé", CompletionCard.KIND_KEYWORD));
                }
                if (entries.size() >= 12) {
                    break;
                }
            }
        }
        if (symbolIndex == null) {
            // L'INDEX EN COURS DE CONSTRUCTION NE VIDE PAS LA CARTE
            // (la leçon de Sora : sa complétion de base naît du TEXTE
            // lui-même — les identifiants déjà tapés s'y proposent
            // sans serveur ni index) : les mots du document autour du
            // caret répondent à leur place (une fenêtre bornée, jamais
            // le document pour une complétion)
            return wordsOfWindow(prefix, entries, 4096);
        }
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
            if (entries.size() >= 20) {
                break;
            }
        }
        return entries;
    }

    /**
     * Les mots d'une fenêtre du tampon autour du caret (le socle de
     * la complétion SANS index — l'index se bâtit, la carte vit
     * quand même à la leçon de Sora) : les identifiants de la
     * fenêtre qui poursuivent le préfixe s'y proposent, sans
     * doublon, au plafond posé.
     */
    private List<CompletionCard.Entry> wordsOfWindow(String prefix,
            List<CompletionCard.Entry> entries, int window) {
        int caret = Math.min(selection.caret(), buffer.length());
        int from = Math.max(0, caret - window);
        int to = Math.min(buffer.length(), caret + window / 2);
        List<String> seen = new ArrayList<>();
        for (CompletionCard.Entry entry : entries) {
            seen.add(entry.name());
        }
        for (int at = from; at < to && entries.size() < 20; ) {
            char c = buffer.charAt(at);
            if (Character.isJavaIdentifierStart(c)) {
                int start = at;
                while (at < to && Character.isJavaIdentifierPart(buffer.charAt(at))) {
                    at++;
                }
                String word = buffer.getText(start, at).toString();
                if (word.length() >= 2
                        && word.regionMatches(true, 0, prefix, 0, prefix.length())
                        && !seen.contains(word)) {
                    seen.add(word);
                    entries.add(new CompletionCard.Entry(word, "mot",
                            CompletionCard.KIND_WORD));
                }
            } else {
                at++;
            }
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
            signatureCard.set(List.of(), 0, 0);
            return;
        }
        int caret = Math.min(selection.caret(), buffer.length());
        SignatureHelp help = signatureProvider != null
                ? signatureProvider.signatureAt(caret) : localSignatureAt(caret);
        signatureCard.set(help == null ? List.of() : overloadsOf(help),
                help == null ? 0 : help.activeSignature(),
                help == null ? 0 : help.activeParameter());
    }

    /** Les surcharges de l'aide montrée — la carte y pose sa liste. */
    private List<SignatureCard.Overload> overloadsOf(SignatureHelp help) {
        List<SignatureCard.Overload> overloads = new ArrayList<>();
        for (SignatureHelp.Overload signature : help.signatures()) {
            overloads.add(new SignatureCard.Overload(signature.label(),
                    signature.parameterDocs()));
        }
        return overloads;
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
                return new SignatureHelp(
                        List.of(new SignatureHelp.Overload(text, List.of())),
                        0, active);
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
            popups.dismiss(overflowCard);
            invalidate();
            return;
        }
        List<OverflowCard.Section> families = new ArrayList<>();
        List<OverflowCard.Row> navigation = new ArrayList<>();
        navigation.add(new OverflowCard.Row("Aller à la ligne…", this::toggleGotoPopup));
        navigation.add(new OverflowCard.Row("Rechercher…", this::toggleSearchPopup));
        // LES USAGES du symbole au curseur (la leçon de la référence :
        // son menu y porte Find Usages — la carte liste chaque
        // occurrence, le choix y navigue)
        if (!wordAtCaret().isEmpty()) {
            navigation.add(new OverflowCard.Row("Usages de « "
                    + wordAtCaret() + " »…", this::showUsagesPopup));
        }
        // LA LISTE DES PROBLÈMES (la liste des problèmes) : les diagnostics du document
        // y parcourent (le choix y navigue)
        if (!problemsOfDocument().isEmpty()) {
            navigation.add(new OverflowCard.Row("Liste des problèmes…",
                    this::showProblemsPopup));
        }
        families.add(new OverflowCard.Section("Navigation", navigation));
        // LE REGISTRE DES ACTIONS D'ÉDITION (R-A) : la MÊME
        // définition que la carte d'actions rapides — l'étage se
        // construit tel quel (la disponibilité y vit au registre)
        List<OverflowCard.Row> edition = new ArrayList<>();
        for (ActionRegistry.Action action : editActions.live()) {
            edition.add(new OverflowCard.Row(action.label(), action::run));
        }
        families.add(new OverflowCard.Section("Édition", edition));
        // LE PRESSE-PAPIERS et SON HISTORIQUE (l'historique du presse-papiers — les dernières
        // copies s'y reprennent, le collage d'avant renaît au
        // doigt : la copie posée l'y propose)
        List<OverflowCard.Row> papiers = new ArrayList<>();
        papiers.add(new OverflowCard.Row("Couper", this::cutSelection));
        papiers.add(new OverflowCard.Row("Tout sélectionner", this::selectAll));
        papiers.add(new OverflowCard.Row("Quitter la sélection", this::clearSelection));
        if (!clipboardHistory.isEmpty()) {
            papiers.add(new OverflowCard.Row("Historique du presse-papiers…",
                    this::showClipboardHistoryPopup));
        }
        families.add(new OverflowCard.Section("Presse-papiers", papiers));
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
        return hasSelection() && !touch.isHandleDragging()
                && !touch.isTextDragging() && !touch.isDragging();
    }

    /**
     * Les problèmes du document (la liste des problèmes) : le débordement y lit sa
     * disponibilité (la liste ne s'y propose qu'à un document qui
     * porte des problèmes).
     */
    private List<DiagnosticCard.Entry> problemsOfDocument() {
        List<DiagnosticCard.Entry> found = new ArrayList<>();
        int lastLine = buffer.getLineCount() - 1;
        for (int line = 0; line <= lastLine; line++) {
            found.addAll(diagnosticsOfLine(line));
        }
        return found;
    }

    private void updateSelectionBar() {
        if (!selectionBarAlive()) {
            // la sélection est morte : SA famille se range au
            // gestionnaire (la barre, le débordement, l'historique)
            popups.dismissSelection();
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
     * LE FOURNISSEUR D'USAGES de l'hôte (R-C du rapport des popups) :
     * un serveur de langage y voit l'espace de travail entier — les
     * références SÉMANTIQUES (la déclaration, les lectures, les
     * écritures) là où la recherche du document ne voit que son
     * texte. La réponse peut être TARDIVE (le serveur répond à son
     * rythme, de son fil) : la vue y repasse le sien.
     */
    public interface UsageProvider {

        /**
         * Les usages du symbole demandé.
         *
         * @param symbol le mot au curseur
         * @param line   la ligne du curseur
         * @param column la colonne du curseur
         * @param answer l'écouteur de la réponse (les rangées, ou
         *               null : le fournisseur ne sait pas — la
         *               recherche du document tient la place)
         */
        void usagesFor(String symbol, int line, int column,
                UsageAnswer answer);
    }

    /**
     * L'écouteur d'une réponse d'usages (tardive ou non).
     */
    @FunctionalInterface
    public interface UsageAnswer {

        /**
         * La réponse arrive.
         *
         * @param usages les rangées à montrer, null si le fournisseur
         *               ne sait pas
         */
        void answer(List<UsageCard.Usage> usages);
    }

    /**
     * Branche le fournisseur d'usages (null débranche : la recherche
     * du document reprend la carte entière).
     *
     * @param provider le fournisseur, ou null
     */
    public void setUsageProvider(UsageProvider provider) {
        usageProvider = provider;
    }

    private UsageProvider usageProvider;

    /**
     * Ouvre la carte des USAGES du symbole au curseur — LA COMMANDE
     * UNIQUE (R-C du rapport des popups : deux moteurs y vivaient,
     * deux résultats pour la MÊME intention — la recherche du document
     * ouvrait la carte, le serveur sautait à la première occurrence).
     * La carte vit immédiate aux occurrences du document (le REPLI —
     * synchrone et local) ; le fournisseur de l'hôte (les références
     * sémantiques du serveur) remplace la liste à son arrivée. Chaque
     * rangée y fait sauter le curseur. Sans symbole au curseur, la
     * carte ne s'ouvre pas.
     */
    public void showUsagesPopup() {
        String symbol = wordAtCaret();
        if (symbol.isEmpty()) {
            return;
        }
        int caret = Math.min(selection.caret(), buffer.length());
        int line = buffer.lineOfOffset(caret);
        // LE REPLI D'ABORD (la carte vit immédiate — la recherche du
        // document est locale et synchrone) ; le fournisseur répondra
        // s'il sait : sa réponse REMPLACE la liste (la présentation ne
        // change JAMAIS : la carte des usages, toujours)
        usageCard.open(symbol, localUsages(symbol, caret));
        // L'EXCLUSION MUTUELLE : la carte des usages devient LA carte
        // flottante active — toute autre carte flottante ou à champ
        // vivante se range
        popups.shown(usageCard);
        invalidate();
        if (usageProvider != null) {
            usageProvider.usagesFor(symbol, line,
                    caret - buffer.getLineStart(line),
                    usages -> post(() -> {
                        if (usages != null && !usages.isEmpty()) {
                            usageCard.open(symbol, usages);
                            invalidate();
                        }
                    }));
        }
    }

    /**
     * Les usages LOCAUX d'un symbole : le document dit ses occurrences
     * du mot (une fenêtre du tampon, jamais tout le document), chaque
     * rangée porte son extrait (B7) et sa navigation.
     */
    private List<UsageCard.Usage> localUsages(String symbol, int caret) {
        List<UsageCard.Usage> found = new ArrayList<>();
        int from = Math.max(0, caret - 8192);
        int to = Math.min(buffer.length(), caret + 4096);
        for (int at = from; at < to && found.size() < 24; ) {
            char c = buffer.charAt(at);
            if (Character.isJavaIdentifierStart(c)) {
                int start = at;
                while (at < to && Character.isJavaIdentifierPart(buffer.charAt(at))) {
                    at++;
                }
                if (buffer.getText(start, at).toString().equals(symbol)) {
                    final int line = buffer.lineOfOffset(start);
                    final int column = start - buffer.getLineStart(line);
                    // L'EXTRAIT de la ligne (B7) : le code se lit dans
                    // la carte — la ligne coupée au plafond d'affichage
                    int lineStart = buffer.getLineStart(line);
                    String lineText = buffer.getText(lineStart,
                            buffer.getLineEnd(line)).toString();
                    final String excerpt = lineText.length() > 80
                            ? lineText.substring(0, 80) : lineText;
                    // la ligne se BORNE au moment de la navigation (la
                    // carte peut vivre plus que le document d'avant :
                    // une annulation a pu tuer sa ligne — la navigation
                    // y reste dans le texte vivant)
                    found.add(new UsageCard.Usage(line, column, "", excerpt,
                            () -> caretTo(buffer.getLineStart(
                                    Math.min(line, buffer.getLineCount() - 1))
                                    + column)));
                }
            } else {
                at++;
            }
        }
        return found;
    }

    /** La carte des usages (le harnais la lit). */
    public UsageCard usageCard() {
        return usageCard;
    }

    /**
     * La carte du CHOIX DE CIBLES (le choix de cibles du rapport des popups —
     * l'aller-à à plusieurs résultats) : le serveur y renvoie
     * PLUSIEURS places (des surcharges, des implémentations, des
     * interfaces) — une rangée par cible, l'extrait s'y lit, le choix
     * y navigue. Le défaut d'avant (D-P3) : l'aller-à SAUTAIT à la
     * première cible, aveugle aux autres.
     *
     * @param what  la chose cherchée (« Définitions de « x » »)
     * @param found les cibles, jamais null
     */
    public void showTargetPopup(String what, List<TargetCard.Target> found) {
        if (found == null || found.isEmpty()) {
            return;
        }
        if (targetCard == null) {
            targetCard = new TargetCard(dp(1));
            targetCard.bind(this);
            popups.register(targetCard, PopupHost.Role.FLOATING,
                    targetCard::close, targetCard::isShown);
        }
        targetCard.setStyle(popupStyle);
        targetCard.open(what, found);
        popups.shown(targetCard);
        invalidate();
    }

    /** La carte du choix de cibles, null si jamais ouverte (le harnais la lit). */
    public TargetCard targetCard() {
        return targetCard;
    }

    /**
     * La LISTE DES PROBLÈMES du document (la liste des problèmes du rapport des
     * popups — la fenêtre Problems de l'atelier) : chaque diagnostic
     * du document y vit en rangée — les familles se groupent (les
     * erreurs d'abord), le choix d'une rangée y fait sauter le
     * curseur. La liste vide ne l'ouvre pas (le document propre se
     * dit à l'invite de commande).
     */
    public void showProblemsPopup() {
        List<ProblemsCard.Problem> found = new ArrayList<>();
        int lastLine = buffer.getLineCount() - 1;
        for (int line = 0; line <= lastLine; line++) {
            for (DiagnosticCard.Entry entry : diagnosticsOfLine(line)) {
                final int problemLine = line;
                found.add(new ProblemsCard.Problem(entry.severity(),
                        entry.message(), line,
                        () -> caretTo(buffer.getLineStart(
                                Math.min(problemLine, buffer.getLineCount() - 1)))));
            }
        }
        if (found.isEmpty()) {
            return;
        }
        if (problemsCard == null) {
            problemsCard = new ProblemsCard(dp(1));
            problemsCard.bind(this);
            popups.register(problemsCard, PopupHost.Role.FLOATING,
                    problemsCard::close, problemsCard::isShown);
        }
        problemsCard.setStyle(popupStyle);
        problemsCard.open(found);
        popups.shown(problemsCard);
        invalidate();
    }

    /** La carte de la liste des problèmes, null si jamais ouverte (le harnais la lit). */
    public ProblemsCard problemsCard() {
        return problemsCard;
    }

    /**
     * L'HISTORIQUE DU PRESSE-PAPIERS (l'historique du presse-papiers du rapport des popups —
     * « Coller depuis l'historique ») : les dernières copies de
     * l'éditeur y vivent en rangée (l'aperçu tronqué, l'ancienneté à
     * droite), le choix d'une rangée y COLLE le texte au caret. La
     * mémoire vide n'ouvre rien.
     */
    public void showClipboardHistoryPopup() {
        if (clipboardHistory.isEmpty()) {
            return;
        }
        if (clipboardCard == null) {
            clipboardCard = new ClipboardCard(dp(1));
            popups.register(clipboardCard, PopupHost.Role.FLOATING,
                    clipboardCard::close, clipboardCard::isShown);
        }
        clipboardCard.setStyle(popupStyle);
        clipboardCard.open(clipboardHistory.entries(),
                android.os.SystemClock.uptimeMillis());
        popups.shown(clipboardCard);
        invalidate();
    }

    /** La carte de l'historique du presse-papiers, null si jamais ouverte. */
    public ClipboardCard clipboardCard() {
        return clipboardCard;
    }

    /** La mémoire des copies (le harnais la lit). */
    public ClipboardHistory clipboardHistory() {
        return clipboardHistory;
    }

    /**
     * La STRUCTURE DU FICHIER (la structure du fichier du rapport des popups — la table
     * des matières) : chaque symbole du document y vit en rangée —
     * l'icône de sa nature (les glyphes partagés de la complétion),
     * son nom, sa ligne — le symbole courant s'y détache (le gras :
     * la structure se lit depuis le curseur). LE FILTRE s'y vit à
     * la frappe : la carte reçoit le clavier (elle ne prend jamais
     * le focus), les lettres tapées y resserrent la liste — le
     * tampon ne bouge pas.
     *
     * <p>Elle REMPLACE le détournement d'avant (R-D du rapport : la
     * table vivait dans la carte d'actions rapides — « nom · ligne
     * N » sans icône ni filtre, mêlée aux actions).</p>
     *
     * @param symbols les symboles (les natures à l'échelle de la
     *                complétion), jamais null
     */
    public void showStructurePopup(List<StructureCard.Symbol> symbols) {
        if (symbols == null || symbols.isEmpty()) {
            return;
        }
        if (structureCard == null) {
            structureCard = new StructureCard(dp(1));
            structureCard.bind(this);
            popups.register(structureCard, PopupHost.Role.KEYBOARD,
                    structureCard::close, structureCard::isShown);
        }
        structureCard.setStyle(popupStyle);
        structureCard.open(symbols);
        popups.shown(structureCard);
        invalidate();
    }

    /** La carte de structure, null si jamais ouverte (le harnais la lit). */
    public StructureCard structureCard() {
        return structureCard;
    }

    /**
     * Ouvre le SÉLECTEUR DE COULEUR au littéral du caret (la leçon
     * de la référence : son picker s'ouvre sur le littéral, la teinte y
     * vit sous le doigt, l'application remplace le littéral EN
     * PLACE — sa largeur en chiffres tient) : #RGB, #RGBA, #RRGGBB
     * ou #RRGGBBAA se lit au caret, le pick repose la couleur au
     * même format. Sans littéral au caret : la carte ne s'ouvre pas.
     */
    public void openColorPickerAtCaret() {
        int caret = Math.min(selection.caret(), buffer.length());
        int line = buffer.lineOfOffset(caret);
        int lineStart = buffer.getLineStart(line);
        int lineEnd = buffer.getLineEnd(line);
        // les bornes du littéral : le dièse étendu, les chiffres
        // hexadécimaux qui le suivent
        int start = caret;
        while (start > lineStart && isHexDigit(buffer.charAt(start - 1))) {
            start--;
        }
        if (start > lineStart && buffer.charAt(start - 1) == '#') {
            start--;
        }
        int end = caret;
        while (end < lineEnd && isHexDigit(buffer.charAt(end))) {
            end++;
        }
        int digits = end - start - 1;
        if (start >= lineEnd || buffer.charAt(start) != '#'
                || (digits != 3 && digits != 4 && digits != 6 && digits != 8)) {
            return;
        }
        String literal = buffer.getText(start, end).toString();
        int color;
        try {
            color = (int) Long.parseLong(
                    literal.substring(1), 16);
        } catch (NumberFormatException error) {
            return;
        }
        final int literalStart = start;
        final int literalEnd = end;
        final int literalDigits = digits;
        final long literalRevision = buffer.getRevision();
        colorPickerCard.open(color, literalDigits, picked -> {
            // le littéral se remplace EN PLACE (sa largeur tient : la
            // forme courte quantifie au nibble — la leçon de la
            // référence) — SA révision se relit : le document a pu
            // bouger depuis l'ouverture (une annulation, une autre
            // frappe) ; l'ancienne plage ne vaut plus rien (le
            // harnais l'a épinglé : checkRange levait) — le littéral
            // se RETROUVE au caret au moment d'appliquer
            int freshStart = literalStart;
            int freshEnd = literalEnd;
            if (buffer.getRevision() != literalRevision) {
                int caretNow = Math.min(selection.caret(), buffer.length());
                int found = buffer.getText(
                        Math.max(0, caretNow - 32), caretNow).toString().lastIndexOf('#');
                if (found < 0) {
                    return;
                }
                freshStart = Math.max(0, caretNow - 32) + found;
                freshEnd = Math.min(buffer.length(), freshStart + 1 + literalDigits);
                if (freshEnd - freshStart != 1 + literalDigits) {
                    return;
                }
            }
            String fresh = ColorPickerCard.format(picked, literalDigits);
            pendingEditLabel = "couleur";
            applyEdit(freshStart, freshEnd, fresh);
        });
        // L'EXCLUSION MUTUELLE : le sélecteur devient LA carte
        // flottante active — toute autre carte flottante ou à champ
        // vivante se range
        popups.shown(colorPickerCard);
        invalidate();
    }

    /**
     * Le doigt sur un LITTÉRAL DE COULEUR (le tap du texte y passe
     * après le souligné du diagnostic) : le curseur s'y pose et le
     * sélecteur s'ouvre — la leçon de la référence : son picker s'ouvre au
     * littéral touché.
     *
     * @param offset le tap le plus proche du doigt
     * @return vrai quand un littéral a pris le geste
     */
    private boolean openColorPickerAtTap(int offset) {
        int caret = Math.min(offset, buffer.length());
        int line = buffer.lineOfOffset(caret);
        int lineStart = buffer.getLineStart(line);
        int lineEnd = buffer.getLineEnd(line);
        // le littéral sous le doigt : le dièse et ses chiffres
        int start = caret;
        while (start > lineStart && isHexDigit(buffer.charAt(start - 1))) {
            start--;
        }
        boolean onHash = start > lineStart && buffer.charAt(start - 1) == '#';
        if (onHash) {
            start--;
        }
        int end = caret;
        while (end < lineEnd && isHexDigit(buffer.charAt(end))) {
            end++;
        }
        int digits = end - start - 1;
        if (!onHash || (digits != 3 && digits != 4 && digits != 6 && digits != 8)) {
            return false;
        }
        // le curseur se pose DANS le littéral (le picker s'y ancre),
        // le clavier ne s'y ouvre pas
        caretTo(start + 1);
        openColorPickerAtCaret();
        return colorPickerCard.isShown();
    }

    /** Le caractère est-il un chiffre hexadécimal. */
    private static boolean isHexDigit(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f')
                || (c >= 'A' && c <= 'F');
    }

    /** La carte du sélecteur de couleur (le harnais la lit). */
    public ColorPickerCard colorPickerCard() {
        return colorPickerCard;
    }

    /**
     * Restaure une version passée : la PLAGE EXACTE d'avant le geste
     * reprend son texte d'alors (B7 — plus d'écrasement d'une autre
     * ligne par un numéro décalé : la plage vit en décalages du
     * document, bornée à sa longueur présente). La restauration
     * s'enregistre à son tour (elle se défait comme elle s'est
     * faite — une unité d'annulation).
     *
     * @param version la version à restaurer, jamais null
     */
    public void restoreHistoryVersion(LocalHistory.Version version) {
        if (version == null) {
            return;
        }
        int start = Math.min(version.start(), buffer.length());
        int end = Math.min(version.end(), buffer.length());
        if (end < start) {
            end = start;
        }
        pendingEditLabel = "restauration";
        applyEdit(start, end, version.before());
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
        // les cartes ancrées au CARET se rangent avec la sélection
        // qu'elles regardaient (usages, sélecteur de couleur) — le
        // doigt qui reprend le texte ne doit pas tomber dessus ; le
        // gestionnaire les range (l'exclusion des FLOTTANTES y vit)
        popups.dismiss(usageCard);
        popups.dismiss(colorPickerCard);
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
        // la copie se pose à L'HISTORIQUE (l'historique du presse-papiers — « Coller depuis
        // l'historique ») : la plus récente au sommet, la même chaîne
        // consécutive ne se duplique pas, le plafond tient
        clipboardHistory.record(bounded,
                android.os.SystemClock.uptimeMillis());
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
            // le souligné du diagnostic se décale du texte : la
            // colonne vit dans le corps défilant, le scrollX y
            // s'applique (le dessin se fait hors translation)
            float x = getPaddingLeft() + column * charWidth - scrollX;
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
        insertCompletion(candidate, -1, -1);
    }

    /**
     * Insère un candidat de complétion sur SA PLAGE : le
     * REMPLACEMENT du serveur PRIME (sa plage s'y pose — la plage du
     * protocole dit ce que le candidat remplace, le préfixe tapé
     * d'avant ne restait qu'une approximation) ; sans remplacement,
     * le préfixe tapé se remplace comme toujours — une seule unité
     * d'annulation.
     *
     * @param candidate le texte à insérer, jamais vide
     * @param start     le début de la plage remplacée, négatif : le mot tapé
     * @param end       la fin de la plage remplacée, négatif : le curseur
     */
    public void insertCompletion(String candidate, int start, int end) {
        if (candidate == null || candidate.isEmpty()) {
            return;
        }
        int from = start >= 0 && start <= buffer.length() ? start
                : suggestionWordStart;
        if (from < 0) {
            return;
        }
        int to = end >= from && end <= buffer.length() ? end
                : Math.min(selection.caret(), buffer.length());
        if (to > from) {
            pendingEditLabel = "complétion";
            applyEdit(from, to, candidate);
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
        painter.drawComposeUnderline(canvas, from - start, to - start,
                getPaddingLeft(), baseline);
    }

    private void drawCaret(Canvas canvas) {
        // le temps du traîné-déposé, le curseur se dessine au point de
        // dépôt : le doigt y voit où le texte se posera
        int offset = Math.min(
                touch.isTextDragging() ? touch.dropOffset() : selection.caret(),
                buffer.length());
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
        if (!touch.isHandleDragging() && !touch.isTextDragging()) {
            return;
        }
        if (touch.isTextDragging() && touch.dropOffset() < 0) {
            return;
        }
        int offset = Math.min(
                touch.isTextDragging() ? touch.dropOffset() : selection.caret(),
                buffer.length());
        int line = buffer.lineOfOffset(offset);
        if (isHidden(line)) {
            return;
        }
        int lineStart = buffer.getLineStart(line);
        int column = offset - lineStart;
        // la loupe se centre sur la borne (le contenu suit), fuit le
        // doigt en hauteur (le doigt cache la ligne, jamais la loupe)
        float centerX = getPaddingLeft() + column * charWidth - scrollX;
        float lineTop = getPaddingTop() + visibleIndexOf(line) * lineHeight - scrollY;
        float width = Math.min(getWidth() - getPaddingLeft() - getPaddingRight(),
                dp(160));
        float height = lineHeight * LOUPE_SCALE + dp(8);
        float left = clampToRange(centerX - width / 2.0f, getPaddingLeft(),
                Math.max(getPaddingLeft(), getWidth() - getPaddingRight() - width));
        float top = touch.fingerY() - dp(12) - height;
        if (top < getPaddingTop()) {
            top = Math.min(touch.fingerY() + dp(12),
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
        if (!touch.isTextDragging() || !hasSelection()) {
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
        float left = clampToRange(touch.fingerX() - chipWidth / 2.0f, getPaddingLeft(),
                Math.max(getPaddingLeft(), getWidth() - getPaddingRight() - chipWidth));
        float top = Math.min(touch.fingerY() + dp(10),
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
        edgeScroller.scheduleNearEdges(dp(48));
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
        render.translate(-getPaddingLeft() + scrollX, -lineTop);
        int revealEnd = buffer.getLineEnd(line);
        if (folds.isFolded(line)) {
            int cut = lastOpenerColumn(buffer.getText(lineStart,
                    revealEnd).toString());
            if (cut >= 0) {
                revealEnd = Math.max(lineStart, lineStart + cut);
            }
        }
        if (revealEnd > lineStart) {
            drawTokens(render, line, lineStart, revealEnd,
                    lineBaseline(line));
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
     *
     * <p>D-P1 (le rapport des popups) : l'ouverture N'ATTEND PLUS le
     * fournisseur — l'attente vivait SUR le fil d'interface (jusqu'à
     * 200 ms par question chez la façade du serveur). La question
     * PART, la popup vit IMMÉDIATEMENT (son corps dit « … »), la
     * réponse la REMPLACE à son arrivée ; le retour null (le serveur
     * ne sait pas) rend la place à la description de l'index — le
     * remplacement ne vaut qu'à la MÊME position (une réponse
     * périmée n'évince personne).</p>
     */
    private void showSymbolInfo(int wordStart, int wordEnd, int line, int column) {
        String word = buffer.getText(wordStart, wordEnd);
        if (infoProvider != null) {
            // la popup vit immédiate : « … » tient la place le temps
            // du vol — le fil d'interface n'attend JAMAIS le serveur
            showInfoPopup(word, InfoPopup.PENDING_BODY, line, column);
            infoProvider.infoFor(line, column, word, (text, markdown) -> post(() -> {
                if (text == null || text.isEmpty()) {
                    // le fournisseur ne sait pas : l'index local tient
                    // la place (à la même position seulement)
                    replaceInfoPopup(localSymbolDescription(word), line, column);
                } else {
                    replaceInfoPopup(text, markdown, line, column);
                }
            }));
            return;
        }
        showInfoPopup(word, localSymbolDescription(word), line, column);
    }

    /**
     * La description de l'index local (le repli du survol) : la nature
     * du symbole et sa ligne de déclaration ; un mot ordinaire ne dit
     * que lui-même.
     */
    private String localSymbolDescription(String word) {
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
        return description;
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
        syncLayoutMetrics();
        int line = buffer.lineOfOffset(Math.min(selection.caret(), buffer.length()));
        scrollY = layout.revealLine(line, scrollY);
    }

    /**
     * Le décalage de dépôt courant du traîné-déposé (-1 : hors geste).
     * Publique : le harnais y lit le point de dépôt pendant le
     * défilement au bord.
     *
     * @return le décalage de dépôt, -1 hors geste
     */
    public int harnessDropOffset() {
        return touch.dropOffset();
    }

    /**
     * Le curseur (le harnais y lit la position sauvegardée et
     * restaurée du scénario d'état).
     *
     * @return l'offset du curseur, en unités de code UTF-16
     */
    public int caretOffset() {
        return selection.caret();
    }

    /**
     * Le défilement vertical (le harnais y lit la position
     * sauvegardée et restaurée du scénario d'état).
     *
     * @return le défilement, en pixels
     */
    public float harnessScrollY() {
        return scrollY;
    }

    /**
     * Le défilement horizontal (le harnais y lit le geste à deux
     * axes et le pincement qui décale la colonne).
     *
     * @return le défilement, en pixels
     */
    public float harnessScrollX() {
        return scrollX;
    }

    /**
     * La sauvegarde d'état (le harnais y appelle celle du cadre :
     * les méthodes du cadre sont protégées, la vue y expose la
     * sienne à son harnais).
     *
     * @return l'état sauvé, jamais null
     */
    public android.os.Parcelable harnessSaveState() {
        return onSaveInstanceState();
    }

    /**
     * La restauration d'état (le harnais y appelle celle du
     * cadre).
     *
     * @param state l'état sauvé par {@link #harnessSaveState()}
     */
    public void harnessRestoreState(android.os.Parcelable state) {
        onRestoreInstanceState(state);
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

    /**
     * La borne du défilement horizontal : la plus large ligne du
     * document moins la vue (la colonne visible s'y arrête, la
     * ligne courte n'y défile jamais — le plancher reste zéro).
     */
    private float maxScrollX() {
        // NUL au soft wrap : les lignes coupent à la largeur, rien ne
        // dépasse (le défilement horizontal s'y éteint)
        if (softWrap) {
            return 0.0f;
        }
        return Math.max(0.0f, layout.maxScrollX(getWidth()
                - getPaddingLeft() - getPaddingRight()));
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
     * La sauvegarde d'état : la rotation ou la mort du processus ne
     * perd plus le texte, la sélection, le défilement. La vue y
     * écrit ce qu'elle porte (le curseur, l'ancre, le
     * défilement) — le texte vit dans le tampon, l'hôte le
     * sauvegarde de son côté (l'écran de démonstration y pose le
     * sien).
     *
     * @return l'état sauvé, jamais null
     */
    @Override
    protected android.os.Parcelable onSaveInstanceState() {
        // le porteur du cadre D'ABORD (le contrat de View : le parent
        // doit vivre dans l'état, sinon le cadre lève à la dispatch)
        return new SavedState(super.onSaveInstanceState(),
                selection.caret(), selection.anchor(), scrollY);
    }

    /**
     * La restauration d'état : ce que {@link #onSaveInstanceState()}
     * a écrit se relit — le porteur du cadre remonte à la sienne, le
     * curseur, l'ancre et le défilement reviennent (les bornes s'y
     * parent : un texte changé entre les deux appels ne doit pas
     * lever).
     *
     * @param state l'état sauvé, null à la création pure
     */
    @Override
    protected void onRestoreInstanceState(android.os.Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }
        SavedState saved = (SavedState) state;
        super.onRestoreInstanceState(saved.getSuperState());
        int length = buffer.length();
        int caret = clamp(saved.caret, 0, length);
        int anchor = saved.anchor;
        if (anchor > length) {
            anchor = -1;
        }
        selection.setCaret(caret);
        selection.setAnchor(anchor);
        scrollY = clampToRange(saved.scrollY,
                0.0f, maxScroll());
        invalidate();
    }

    /**
     * Bornage d'un entier.
     *
     * @param value    la valeur
     * @param low      la borne basse
     * @param high     la borne haute
     * @return la valeur bornée
     */
    private static int clamp(int value, int low, int high) {
        return Math.max(low, Math.min(high, value));
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
        // LA VERSION D'HISTORIQUE (B7) : le texte d'avant se garde,
        // la plage porte la région OCCUPÉE APRÈS le geste (le texte
        // posé y vit) — la restauration remettra la plage exacte à son
        // état d'avant (une insertion s'y retire entière)
        String before = buffer.getText(start, end).toString();
        int line = buffer.lineOfOffset(Math.min(start, buffer.length()));
        int occupiedStart = Math.min(start, buffer.length());
        int occupiedEnd = Math.min(start + text.length(), buffer.length());
        try (TextTransaction transaction = undoStack.newTransaction()) {
            transaction.replace(start, end, text);
        }
        localHistory.record(System.currentTimeMillis(), pendingEditLabel,
                line, occupiedStart, occupiedEnd, before);
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

    /**
     * L'état sauvegardé de la vue : le porteur du cadre (le contrat
     * de View l'exige — la dispatch de sauvegarde lève sans lui), le
     * curseur, l'ancre et le défilement.
     */
    public static final class SavedState extends android.view.View.BaseSavedState {

        /** Le créateur du pareclable (le cadre y passe). */
        public static final android.os.Parcelable.Creator<SavedState> CREATOR =
                new android.os.Parcelable.Creator<SavedState>() {
                    @Override
                    public SavedState createFromParcel(android.os.Parcel source) {
                        return new SavedState(source);
                    }

                    @Override
                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };

        /** Le curseur sauvé (unités de code UTF-16). */
        public final int caret;

        /** L'ancre sauvée (-1 : aucune sélection). */
        public final int anchor;

        /** Le défilement vertical sauvé (pixels). */
        public final float scrollY;

        /**
         * Un état neuf.
         *
         * @param superState le porteur du cadre
         * @param caret       le curseur
         * @param anchor      l'ancre
         * @param scrollY     le défilement vertical
         */
        public SavedState(android.os.Parcelable superState,
                int caret, int anchor, float scrollY) {
            super(superState);
            this.caret = caret;
            this.anchor = anchor;
            this.scrollY = scrollY;
        }

        /**
         * La relecture du pareclable.
         *
         * @param source la source
         */
        private SavedState(android.os.Parcel source) {
            super(source);
            this.caret = source.readInt();
            this.anchor = source.readInt();
            this.scrollY = source.readFloat();
        }

        @Override
        public void writeToParcel(android.os.Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(caret);
            dest.writeInt(anchor);
            dest.writeFloat(scrollY);
        }
    }
}
