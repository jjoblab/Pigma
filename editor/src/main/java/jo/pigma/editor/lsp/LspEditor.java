package jo.pigma.editor.lsp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.lsp4j.services.LanguageServer;

import jo.pigma.core.TextBuffer;
import jo.pigma.core.TextListener;
import jo.pigma.editor.EditorView;
import jo.pigma.editor.popups.CompletionCard;
import jo.pigma.editor.popups.DiagnosticCard;
import jo.pigma.editor.popups.GotoCard;
import jo.pigma.editor.popups.ImportBubbleCard;
import jo.pigma.editor.popups.PreviewCard;
import jo.pigma.editor.popups.QuickActionCard;
import jo.pigma.editor.popups.RenamePreviewCard;
import jo.pigma.editor.popups.StructureCard;
import jo.pigma.editor.popups.TargetCard;
import jo.pigma.editor.popups.UsageCard;
import jo.pigma.editor.selection.SelectionState;
import jo.pigma.lsp.LspCapabilities;
import jo.pigma.lsp.LspCompletionItem;
import jo.pigma.lsp.LspWorkspace;
import jo.pigma.lsp.LspCodeAction;
import jo.pigma.lsp.LspDiagnostic;
import jo.pigma.lsp.LspDocumentSymbol;
import jo.pigma.lsp.LspFoldingRange;
import jo.pigma.lsp.LspInlayHint;
import jo.pigma.lsp.LspLocation;
import jo.pigma.lsp.LspPosition;
import jo.pigma.lsp.LspRange;
import jo.pigma.lsp.LspSemanticToken;
import jo.pigma.lsp.LspSession;
import jo.pigma.lsp.LspTextEdit;
import jo.pigma.lsp.LspWorkspaceSymbol;

/**
 * La façade du serveur de langage : l'hôte n'a plus RIEN à câbler.
 * L'étude de l'éditeur de référence (son module d'intégration) y a
 * porté sa leçon — là-bas, une façade pareille attache le
 * serveur à la vue, porte la synchronisation du document, les
 * fournisseurs et leurs caches ; l'hôte de la référence pose la vue,
 * branche, écrit. Ici pareillement : {@link #attach(EditorView)}
 * prend la vue, {@link #connectInProcess}/{@link #connectProcess}/
 * {@link #connect} branchent, le reste vit seul — la
 * synchronisation du document (le {@code didChange} amorti aux
 * 300 ms, l'ombre du texte au protocole), le survol (la réponse du
 * serveur prime sur l'index local, l'asynchrone remplace la
 * réponse lente), la complétion (le cache par préfixe, la réponse
 * asynchrone évince l'ancienne), l'aide aux paramètres (le cache
 * par curseur, la génération déclasse la réponse d'avant), les
 * diagnostics (la carte de diagnostic les porte, la gouttière en
 * vit). Les réglages de la référence y suivent : les fonctions se
 * coupent une à une ({@link #setHoverEnabled} et ses sœurs), le
 * temps d'attente du survol se pose ({@link #setRequestTimeoutMs}),
 * l'hôte lit les diagnostics pour son panneau
 * ({@link #setDiagnosticsListener}).
 *
 * <p>La façade vit attachée à UNE vue ; le débranchement
 * ({@link #disconnect}) coupe tout : les fournisseurs se retirent,
 * les caches se rangent, la liaison s'arrête (le processus meurt).
 * Une liaison neuve se branche à la même vue sans frais.</p>
 *
 * <p>LES FONCTIONS DU PROTOCOLE y vivent aussi (la leçon de la
 * référence — sa façade y porte le renommage,
 * l'aller-à, les références, le formatage) : l'ampoule de la
 * gouttière les porte APRÈS les intentions de l'hôte ({@link #attach}
 * y pose le fournisseur du serveur — l'hôte n'a RIEN à câbler), le
 * surlignage des occurrences suit le curseur (le protocole
 * {@code textDocument/documentHighlight}, la teinte bleue s'y
 * distingue de la recherche ambre), le renommage s'y éprouve par la
 * carte au caret ({@link #requestRename} — la préparation du
 * protocole y vérifie le symbole, la validation y porte les
 * remplacements), l'aller-à la définition ({@link #requestDefinition})
 * et les références ({@link #requestReferences}) y naviguent — les
 * places du MÊME document y sautent, celles d'un autre document se
 * laissent à l'écouteur de l'hôte ({@link #setNavigationListener}),
 * l'éditeur n'en tient qu'un. Le formatage ({@link #formatDocument})
 * y pose les remplacements du serveur.</p>
 */
public final class LspEditor {

    /**
     * L'écouteur des diagnostics du serveur : le panneau de l'hôte s'y
     * rafraîchit (le compte, la liste). L'appel arrive sur le fil de
     * l'interface (la façade y marshalle l'avis du lecteur).
     */
    /**
     * L'état de la connexion du serveur (la machine à états de la
     * référence, portée à notre façade) : la connexion naît
     * {@code CONNECTING} (la poignée de main y court), vit
     * {@code CONNECTED} (le document ouvert, les fonctions posées),
     * meurt {@code DISCONNECTED} (débranchement volontaire ou mort
     * du serveur — le canal s'est fermé). L'hôte y observe le
     * cycle et son écran y dit l'état (le toast de l'app y naît).
     */
    public enum ConnectionStatus {
        /** La poignée de main court (initialize, didOpen). */
        CONNECTING,
        /** Le serveur vit, le document est ouvert. */
        CONNECTED,
        /** Aucun serveur : jamais branché, débranché, ou mort. */
        DISCONNECTED
    }

    /**
     * L'écouteur du cycle de la connexion (l'hôte y pose son toast,
     * son état d'écran, ses compteurs).
     */
    public interface ConnectionListener {

        /**
         * L'état de la connexion vient de changer (le changement
         * y est LU, pas poussé : l'écouteur interroge
         * {@link #connectionStatus} à son rythme).
         *
         * @param status le nouvel état, jamais {@code null}
         * @param reason la cause du changement ({@code null} quand
         *               la connexion naît simplement)
         */
        void onStatusChanged(ConnectionStatus status, String reason);
    }

    /**
     * L'écouteur des diagnostics publiés par le serveur (le panneau
     * de l'hôte y suit les erreurs du document).
     */
    public interface DiagnosticsListener {

        /**
         * Le serveur a publié la liste ENTIÈRE des diagnostics du
         * document (le protocole est à remplacement).
         *
         * @param diagnostics la liste complète, jamais {@code null}
         */
        void onDiagnostics(List<LspDiagnostic> diagnostics);
    }

    /**
     * L'écouteur des places d'un AUTRE document : l'aller-à la
     * définition ou les références du serveur peuvent viser un autre
     * fichier — l'éditeur n'en tient qu'un, la place ne peut pas y
     * sauter. L'écouteur de l'hôte y reçoit les places d'ailleurs
     * (il tient plusieurs documents, lui) ; sans écouteur, les
     * places d'ailleurs se laissent (la même leçon que les actions
     * du protocole — un document par éditeur).
     */
    public interface NavigationListener {

        /**
         * Les places visées dans un autre document que le courant.
         *
         * @param uri    le document d'ailleurs
         * @param places les places visées dans CE document, jamais null
         */
        void onExternalPlaces(String uri, List<LspLocation> places);
    }

    /** Le délai amorti du document vers le serveur (les frappes rapides n'envoient qu'un envoi). */
    private static final long DOCUMENT_SYNC_MS = 300L;

    /** Le délai amorti du surlignage des occurrences (le curseur qui glisse ne questionne qu'à la pose). */
    private static final long HIGHLIGHT_SYNC_MS = 250L;

    /** La vue branchée (les fournisseurs y vivent, le tampon y dort). */
    private final EditorView view;

    /** La liaison vivante (null : aucun serveur). */
    private LspSession session;

    /** Le document courant côté serveur (l'URI du protocole). */
    private String uri;

    /** Le survol demande-t-il au serveur (la fonction coupée retombe sur l'index local). */
    private boolean hoverEnabled = true;

    /** La complétion demande-t-elle au serveur (la fonction coupée rend la carte vide). */
    private boolean completionEnabled = true;

    /** L'aide aux paramètres demande-t-elle au serveur (la fonction coupée range la carte). */
    private boolean signatureEnabled = true;

    /** Les diagnostics montent-ils à la carte (la fonction coupée la rend muette). */
    private boolean diagnosticsEnabled = true;

    /** Le surlignage des occurrences suit-il le curseur (la fonction coupée range la teinte bleue). */
    private volatile boolean documentHighlightEnabled = true;

    /** Le temps d'attente du survol en millisecondes (au-delà, la réponse lente arrivera en asynchrone). */

    /** Le préfixe du dernier envoi de complétion (la clef du cache). */
    private volatile String completionPrefix;

    /**
     * Les candidats ENTIERS reçus du serveur pour ce préfixe (le fil
     * du lecteur y écrit — la nature, le détail, l'obsolescence, la
     * documentation, le remplacement propre et l'ordre du serveur y
     * vivent : le libellé seul ne nourrit plus la carte).
     */
    private final List<jo.pigma.lsp.LspCompletionItem> completionCache =
            Collections.synchronizedList(new ArrayList<>());

    /**
     * L'ordre des candidats du serveur : le sortText PRIME (le serveur
     * y dit sa pertinence — l'ordre d'arrivée rompt les égalités),
     * l'ordre d'arrivée sinon (le serveur a déjà trié).
     */
    private static int completionOrder(jo.pigma.lsp.LspCompletionItem first,
            jo.pigma.lsp.LspCompletionItem second) {
        String left = first.sortText();
        String right = second.sortText();
        if (left.isEmpty() && right.isEmpty()) {
            return 0;
        }
        if (left.isEmpty()) {
            return 1;
        }
        if (right.isEmpty()) {
            return -1;
        }
        return left.compareTo(right);
    }

    /**
     * Le candidat du serveur devient la rangée de la carte : la
     * nature du protocole y devient l'icône (Text le mot, Method et
     * Function la méthode, Constructor et Class et Interface et Struct
     * et Enum le type, Field et Variable et Property et EnumMember le
     * champ, Module le module, Keyword le mot-clé, Snippet le gabarit,
     * Color la couleur, File et Folder le fichier), le détail s'y
     * pose tel quel, l'obsolescence porte le barré, la documentation
     * porte le panneau adjacent, le REMPLACEMENT propre porte la
     * plage (la plage du protocole devient décalages du tampon).
     *
     * @param candidate le candidat du serveur
     * @return la rangée de la carte
     */
    private CompletionCard.Entry entryOf(jo.pigma.lsp.LspCompletionItem candidate) {
        int insertStart = -1;
        int insertEnd = -1;
        if (candidate.textEdit() != null) {
            TextBuffer buffer = view.getBuffer();
            LspRange range = candidate.textEdit().range();
            int start = range.start().toOffset(buffer);
            int end = range.end().toOffset(buffer);
            if (start >= 0 && end >= start && end <= buffer.length()) {
                insertStart = start;
                insertEnd = end;
            }
        }
        return new CompletionCard.Entry(candidate.label(), candidate.detail(),
                kindOf(candidate.kind()), candidate.deprecated(),
                candidate.documentation(), candidate.markdownDoc(),
                candidate.insertText(), insertStart, insertEnd);
    }

    /**
     * La nature du protocole devient l'icône de la carte (les natures
     * voisines s'y regroupent — la carte en porte sept, le protocole
     * en dit vingt-cinq).
     */
    private static int kindOf(int protocolKind) {
        switch (protocolKind) {
            case 1: // Text
                return CompletionCard.KIND_WORD;
            case 2: // Method
            case 3: // Function
                return CompletionCard.KIND_FUNCTION;
            case 4: // Constructor
            case 7: // Class
            case 8: // Interface
            case 13: // Enum
            case 22: // Struct
            case 23: // Event
            case 25: // TypeParameter
                return CompletionCard.KIND_CLASS;
            case 5: // Field
            case 6: // Variable
            case 10: // Property
            case 20: // EnumMember
            case 21: // Constant
                return CompletionCard.KIND_FIELD;
            case 9: // Module
            case 17: // File
            case 19: // Folder
                return CompletionCard.KIND_MODULE;
            case 14: // Keyword
                return CompletionCard.KIND_KEYWORD;
            default:
                return CompletionCard.KIND_UNKNOWN;
        }
    }

    /**
     * L'aide du serveur devient l'aide de la carte : les surcharges
     * y passent toutes (la ligne de chacune, la documentation de
     * chacun de ses paramètres), l'active et le paramètre actif
     * suivent.
     */
    private static EditorView.SignatureHelp helpOf(
            jo.pigma.lsp.LspSignatureHelp help) {
        List<EditorView.SignatureHelp.Overload> overloads = new ArrayList<>();
        for (jo.pigma.lsp.LspSignatureHelp.LspSignature signature
                : help.signatures()) {
            List<String> parameterDocs = new ArrayList<>();
            for (jo.pigma.lsp.LspSignatureHelp.LspParameter parameter
                    : signature.parameters()) {
                parameterDocs.add(parameter.documentation());
            }
            overloads.add(new EditorView.SignatureHelp.Overload(
                    signature.label(), parameterDocs));
        }
        return new EditorView.SignatureHelp(overloads,
                help.activeSignature(), help.activeParameter());
    }

    /**
     * La génération de la demande de signature vivante (le texte change : la réponse en vol se périme).
     */
    private final AtomicLong signatureGeneration = new AtomicLong();

    /**
     * LA GÉNÉRATION DE CHANGEMENT DE DOCUMENT (A4) : elle avance à
     * chaque fois que le document servi change — le branchement, le
     * débranchement, le passage d'onglet, l'ouverture d'un autre
     * document par l'hôte — et SEULEMENT là (les édition courantes
     * ne la touchent pas : les réponses de la frappe doivent
     * survivre). Les réponses PONCTUELLES en vol (survol, correctifs,
     * renommage, formatage, aller-à, usages, symboles) s'y gardent :
     * une réponse qui arrive après son changement se JETTE — aucune
     * carte ne s'ouvre du document d'avant dans le texte neuf.
     */
    private final AtomicLong documentChangeGeneration = new AtomicLong();

    /** Le curseur de la dernière demande de signature (la clef du cache). */
    private volatile int signatureCaret = -1;

    /** La dernière signature reçue du serveur (null : pas de réponse). */
    private volatile EditorView.SignatureHelp signatureAnswer;

    /** Les diagnostics du dernier avis du serveur. */
    private volatile List<LspDiagnostic> diagnostics = List.of();

    /** L'écouteur de l'hôte (null : personne n'écoute). */
    private volatile DiagnosticsListener diagnosticsListener;

    /** L'écouteur des places d'ailleurs (null : elles se laissent). */
    private volatile NavigationListener navigationListener;

    /** Ce que le serveur a déclaré savoir (NONE sans initialisation). */
    private volatile LspCapabilities capabilities = LspCapabilities.NONE;

    /** La génération du surlignage vivant (le curseur bouge : la réponse en vol se périme). */
    private final AtomicLong highlightGeneration = new AtomicLong();

    /** Le report du surlignage vers le serveur (le curseur qui glisse ne questionne qu'à la pose). */
    private final Runnable highlightSync = this::pushHighlightToServer;

    /** La ligne des corrections serveur vivantes (la clef du cache des actions). */
    private volatile int codeActionsLine = -1;

    /** Les corrections du serveur reçues pour la ligne (le fil du lecteur y écrit). */
    private final List<QuickActionCard.Entry> codeActionsCache =
            Collections.synchronizedList(new ArrayList<>());

    /**
     * Le cache des correctifs par diagnostic (la clef : ligne et
     * message — la carte de diagnostic les y lit, la réponse
     * asynchrone y écrit).
     */
    private final java.util.Map<String, List<DiagnosticCard.Fix>> diagnosticFixCache =
            Collections.synchronizedMap(new java.util.HashMap<>());

    /**
     * Les rangées de l'aller au symbole reçues (l'espace de travail
     * — la réponse asynchrone y écrit, le fournisseur y lit : la
     * carte vit aux rangées connues pendant l'attente).
     */
    private volatile List<GotoCard.GotoRow> gotoRows = List.of();

    /** L'aller demandé, la réponse en vol (la carte s'ouvrira à l'arrivée). */
    private volatile boolean gotoAsked;

    /** Le report du changement de document vers le serveur (la synchronisation intégrale s'amortit). */
    private final Runnable documentSync = this::pushDocumentToServer;

    /**
     * La poignée de main a-t-elle ouvert le document ? La pompe de
     * synchronisation s'ignore tant que non (un didChange parti
     * avant le didOpen, le serveur réel l'ignore : le protocole
     * n'y connaît pas de document non ouvert — EmmyLua perdrait le
     * texte de vue et la liaison y paraîtrait morte).
     */
    private volatile boolean documentOpened;

    /**
     * L'état observable de la connexion (l'écouteur y naît à son
     * changement — la machine à états de la référence).
     */
    private volatile ConnectionStatus connectionStatus = ConnectionStatus.DISCONNECTED;

    /** L'identifiant du langage au registre (le didChange y route). */
    private String languageId;

    /** Le registre partagé (null : la voie directe d'un éditeur seul). */
    private LspWorkspace workspace;

    /** La définition du serveur du registre (le changement de document
     * y rattache le document neuf — la voie directe n'en a pas besoin). */
    private LspWorkspace.ServerDefinition definition;

    /** L'écouteur du document au registre (le détachement le retire). */
    private LspWorkspace.DocumentListener workspaceListener;

    /** L'écouteur du cycle de la connexion (l'hôte). */
    private volatile ConnectionListener connectionListener;

    /**
     * L'exécuteur de l'arrêt : la poignée de main de la fermeture
     * (l'attente de la réponse du serveur, la fermeture ordonnée
     * des canaux) vit HORS du fil d'interface — l'écran ne gèle
     * plus au débranchement, l'arrêt se règle en fond et meurt
     * avec la dernière liaison (le fil y est démon). Un
     * rebranchement après un débranchement y relance l'exécuteur :
     * le champ n'est jamais lu après sa mise à mort.
     */
    private volatile ExecutorService shutdownExecutor = newExecutor();

    /**
     * Un exécuteur d'arrêt neuf (le fil démon de la fermeture).
     *
     * @return l'exécuteur, jamais null
     */
    private static ExecutorService newExecutor() {
        return Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "pigma-lsp-stop");
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * La génération de la pompe du document (le document change : les
     * réponses en vol des quatre fonctions du document se périment).
     */
    private final AtomicLong documentViewsGeneration = new AtomicLong();

    /**
     * Les symboles du dernier avis du serveur (la table des matières
     * du document — l'ampoule la porte à sa carte).
     */
    private volatile List<LspDocumentSymbol> symbols = List.of();

    /** L'écouteur du tampon : chaque édition reprogramme l'envoi du document. */
    private final TextListener textListener = new TextListener() {
        @Override
        public void textChanged(TextBuffer changed, int start, int removedLength,
                int insertedLength, int revision) {
            scheduleDocumentSync();
        }
    };

    /**
     * Attache la façade à une vue : l'écouteur du tampon s'y pose (il
     * ne coûte rien sans liaison), les fournisseurs ne se posent
     * qu'au branchement.
     *
     * @param view la vue de l'éditeur, jamais {@code null}
     * @return la façade prête à recevoir un serveur
     */
    public static LspEditor attach(EditorView view) {
        return new LspEditor(view);
    }

    /**
     * Branche la façade au serveur PARTAGÉ d'un registre (la voie
     * multi-éditeurs : le premier éditeur d'un langage y monte le
     * serveur — la poignée de main y court UNE fois — les suivants
     * s'y raccrochent : le document part par {@code didOpen} au
     * serveur vivant, les avis y reviennent dispatchés par URI —
     * l'avis du document voisin n'atteint QUE l'éditeur du
     * document). Le dernier éditeur détaché ferme le serveur : la
     * durée de vie du processus y suit les documents ouverts. Un
     * registre partagé vit mieux qu'un mono-serveur par écran.
     *
     * @param registry    le registre de l'espace de travail, jamais
     *                    {@code null}
     * @param definition  la définition du serveur du langage, jamais
     *                    {@code null}
     * @param uri         l'URI du document au protocole
     * @param languageId l'identifiant du langage au protocole
     * @throws IOException si la fabrique du serveur échoue (les tubes)
     */
    public void attachToWorkspace(LspWorkspace registry,
            LspWorkspace.ServerDefinition definition, String uri,
            String languageId) throws java.io.IOException {
        if (this.session != null || workspace != null) {
            disconnect();
        }
        this.workspace = registry;
        this.definition = definition;
        this.uri = uri;
        this.languageId = languageId;
        resetProviders();
        // la connexion naît : la poignée de main court au registre
        // (l'état observable le dit — le toast « connexion en cours »)
        moveTo(ConnectionStatus.CONNECTING, null);
        // l'écouteur du document : les avis du registre ne valent
        // pour CE document (le fil du lecteur y appelle — le marshalling
        // à la vue y suit)
        workspaceListener = (published, fresh) -> {
            if (uri.equals(published)) {
                diagnostics = List.copyOf(fresh);
                view.post(() -> {
                    if (diagnosticsListener != null) {
                        diagnosticsListener.onDiagnostics(diagnostics);
                    }
                    view.invalidate();
                });
            }
        };
        LspSession shared = registry.attach(languageId, definition, uri,
                view.getBuffer().toString(), workspaceListener);
        if (shared == null) {
            // la fabrique a échoué : la façade reste débranchée (le
            // toast de l'hôte y dira « la liaison ne monte pas »)
            workspace = null;
            workspaceListener = null;
            this.uri = null;
            moveTo(ConnectionStatus.DISCONNECTED, "la liaison ne monte pas");
            throw new java.io.IOException("la fabrique du serveur a échoué");
        }
        this.session = shared;
        // l'état du registre se transpose (le serveur déjà vivant y
        // rend CONNECTED direct — l'attente du handshake sinon)
        registry.addServerListener((serverName, connected, reason) -> {
            if (connected) {
                capabilities = shared.capabilities();
                documentOpened = true;
                moveTo(ConnectionStatus.CONNECTED, null);
            } else {
                documentOpened = false;
                moveTo(ConnectionStatus.DISCONNECTED, reason);
            }
        });
        attachProviders();
        view.setServerQuickActionProvider(this::serverQuickActionsAt);
        view.post(view::invalidate);
    }

    private LspEditor(EditorView view) {
        this.view = view;
        view.getBuffer().addListener(textListener);
        // le document change sous la façade (A4 — l'hôte ouvre un
        // autre document sans passer par elle) : tout ce qui parlait
        // de l'ancien se périme
        view.setDocumentChangeListener(this::onViewDocumentChanged);
        // le fournisseur du SERVEUR sur l'ampoule : les fonctions du
        // protocole s'y lisent APRÈS les intentions de l'hôte (le
        // second slot — la place de l'hôte demeure)
        view.setServerQuickActionProvider(this::serverQuickActionsAt);
        // le curseur se suit : le surlignage des occurrences s'y
        // redemande (amorti — le curseur qui glisse ne questionne
        // qu'à la pose)
        view.setSelectionListener(this::onSelectionMoved);
    }

    /**
     * Branche un serveur DANS le même processus : lsp4j porte le
     * protocole (JSON-RPC et gson dessous), les tubes ne quittent
     * pas la machine. Le document part au serveur ({@code didOpen}
     * du texte courant), les fournisseurs se posent.
     *
     * @param server     le serveur de langage, jamais {@code null}
     * @param uri        l'URI du document au protocole
     * @param languageId l'identifiant du langage au protocole ("java", "lua"…)
     * @throws IOException si la liaison ne monte pas (les tubes)
     */
    public void connectInProcess(LanguageServer server, String uri, String languageId)
            throws IOException {
        connect(LspSession.beside(server, sessionListener()), uri, languageId);
    }

    /**
     * Branche le serveur EMMYLUA RÉEL, d'UN SEUL TRAIT (la demande du
     * terrain : « trop de méthodes dans le sample qui devraient
     * être dans l'éditeur — l'intégration doit être facile ») : le
     * porteur n'a plus qu'à tenir le texte. L'espace de travail naît
     * au cache de l'application (la bibliothèque standard Lua s'y
     * extrait, le document y devient un vrai fichier — seul
     * répertoire où un exécutable vit depuis l'appareil), le serveur
     * part par processus ({@code libemmylua_ls.so}, tiré du
     * {@code nativeLibraryDir} du porteur), la liaison s'y branche.
     *
     * @param context le contexte du porteur (l'application hôte)
     * @param luaText le texte du document Lua de démonstration
     * @throws IOException si le serveur manque à l'architecture ou si
     *                     l'espace de travail ne s'écrit pas
     */
    public void connectEmmyLua(android.content.Context context, String luaText)
            throws IOException {
        java.io.File executable = new java.io.File(
                context.getApplicationInfo().nativeLibraryDir,
                "libemmylua_ls.so");
        if (!executable.canExecute()) {
            throw new IOException("le serveur EmmyLua manque pour cette architecture ("
                    + android.os.Build.SUPPORTED_ABIS[0] + ")");
        }
        java.io.File workspace = new java.io.File(context.getCacheDir(), "emmylua");
        java.io.File resources = new java.io.File(workspace, "resources");
        java.io.File document = new java.io.File(workspace, "demo.lua");
        // l'espace de travail naît ici (le cache le laisse absent au
        // premier lancement) — sans lui l'écriture du texte échoue et
        // le serveur ne part jamais
        java.nio.file.Files.createDirectories(resources.toPath());
        // writeString n'existe pas dans android.jar (Java 11, jamais
        // porté) — l'écriture classique y va, octet par octet
        java.io.FileOutputStream out = new java.io.FileOutputStream(document);
        try {
            out.write(luaText.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } finally {
            out.close();
        }
        Process server = new ProcessBuilder(executable.getAbsolutePath(),
                        "--resources-path", resources.getAbsolutePath())
                .directory(workspace)
                .redirectError(new java.io.File(resources, "stderr.log"))
                .start();
        connectProcess(server, document.toURI().toString(), "lua");
    }

    /**
     * La dernière ligne parlante de la stderr du serveur EmmyLua
     * (l'arrière du processus y dit sa mort : l'erreur de démarrage,
     * le port occupé, la ressource manquante) — le porteur s'y sert
     * à son toast de déconnexion.
     *
     * @param context le contexte du porteur
     * @return la ligne, ou null quand elle ne parle pas
     */
    public String lastEmmyLuaError(android.content.Context context) {
        java.io.File log = new java.io.File(
                new java.io.File(context.getCacheDir(), "emmylua"),
                "resources/stderr.log");
        if (!log.isFile()) {
            return null;
        }
        try (java.io.BufferedReader lines = new java.io.BufferedReader(
                new java.io.FileReader(log))) {
            String last = null;
            String line;
            while ((line = lines.readLine()) != null) {
                if (!line.isBlank()) {
                    last = line;
                }
            }
            return last != null && last.length() > 120
                    ? last.substring(0, 120) : last;
        } catch (IOException noRead) {
            return null;
        }
    }

    /**
     * Branche un serveur lancé PAR PROCESSUS (l'exécutable vit hors
     * de la machine virtuelle) : lsp4j cadre les messages sur son
     * entrée et sa sortie standard.
     *
     * @param process    le serveur vivant, jamais {@code null}
     * @param uri       l'URI du document au protocole
     * @param languageId l'identifiant du langage au protocole
     */
    public void connectProcess(Process process, String uri, String languageId) {
        connect(LspSession.overProcess(process, sessionListener()), uri, languageId);
    }

    /**
     * Branche une liaison déjà montée (des flux donnés, un serveur
     * déjà lancé) : la négociation d'initialisation y court, le
     * document part au serveur, les fournisseurs se posent. Une
     * liaison vivante se débranche d'abord ({@link #disconnect()}).
     *
     * @param liaison   la liaison montée, jamais {@code null}
     * @param uri       l'URI du document au protocole
     * @param languageId l'identifiant du langage au protocole
     */
    public void connect(LspSession liaison, String uri, String languageId) {
        if (this.session != null) {
            disconnect();
        }
        // le document servi CHANGE (un branchement neuf) : les
        // réponses d'une liaison d'avant se périment
        documentChangeGeneration.incrementAndGet();
        this.session = liaison;
        this.uri = uri;
        resetProviders();
        // la connexion naît : la poignée de main y court (l'état
        // observable — la machine à états de la référence ; l'hôte
        // y dit « connexion en cours » au doigt)
        moveTo(ConnectionStatus.CONNECTING, null);
        liaison.setRootUri(rootUriOf(uri));
        liaison.initialize("pigma").whenComplete((result, failure) -> {
            // la négociation a échoué (le serveur mort au départ, le
            // canal cassé) : le document n'y ouvre pas — la liaison
            // sans poignée de main ne connaît rien
            if (failure != null || result == null) {
                // la négociation a échoué : la liaison est morte au
                // départ — l'état le dit (le toast de l'app y
                // naîtra : « le serveur ne répond pas »)
                moveTo(ConnectionStatus.DISCONNECTED,
                        failure != null ? failure.getMessage() : "aucune réponse d'initialisation");
                return;
            }
            // les capacités négociées y tombent (le fil du lecteur) :
            // la façade n'y offre que ce que le serveur a DECLARÉ
            // savoir — demander une fonction non déclarée offense le
            // protocole (la leçon de la référence)
            capabilities = LspCapabilities.fromResult(result);
            // l'aller au symbole vit de la capacité déclarée : sans
            // elle le fournisseur se retire (le débordement n'y
            // propose rien — le câblage AVANT la négociation l'avait
            // posé, la réponse le reprend)
            if (!capabilities.workspaceSymbol()) {
                view.setGotoProvider(null);
            }
            // le document n'ouvre qu'APRÈS la poignée de main
            // complète (la leçon du branchement : un didOpen parti
            // pendant l'initialize, le serveur réel le rejette —
            // EmmyLua refuse le premier document, la liaison y
            // paraît morte : « non connecté »)
            liaison.didOpen(uri, view.getBuffer().toString(), languageId);
            documentOpened = true;
            // la connexion vit : le document ouvert, les capacités
            // connues — l'état observable le dit (le toast de
            // l'app : « serveur connecté »)
            moveTo(ConnectionStatus.CONNECTED, null);
            view.post(() -> {
                view.setServerHighlights(List.of());
                scheduleHighlightSync();
                // la pompe du document y démarre : les symboles, les
                // indices, les plis, les jetons sémantiques décrivent
                // le document — pas le curseur
                refreshDocumentViews();
            });
        });
        attachProviders();
        // le fournisseur du serveur sur l'ampoule se repose : un
        // débranchement antérieur l'avait retiré (la reconnexion y
        // remet les fonctions du protocole — l'hôte n'y touche pas)
        view.setServerQuickActionProvider(this::serverQuickActionsAt);
        // la trame qui suit porte les marqueurs du premier avis
        view.post(view::invalidate);
    }

    /**
     * La liaison vit-elle encore (un serveur branché, pas encore
     * débranché) ?
     *
     * @return vrai si un serveur y vit
     */
    public boolean isConnected() {
        return connectionStatus == ConnectionStatus.CONNECTED;
    }

    /**
     * Débranche la liaison vivante : les fournisseurs se retirent, les
     * caches se rangent, la liaison s'arrête (le processus meurt),
     * les diagnostics s'oublient.
     */
    public void disconnect() {
        if (session == null) {
            return;
        }
        // le document servi CHANGE (la liaison meurt) : les réponses
        // en vol se périment
        documentChangeGeneration.incrementAndGet();
        // la voie du registre : la façade s'y détache (le dernier
        // parti ferme le serveur — les autres éditeurs du registre
        // vivent), la voie directe garde la fermeture directe
        if (workspace != null) {
            LspWorkspace registry = workspace;
            LspWorkspace.DocumentListener listener = workspaceListener;
            String language = languageId;
            String documentUri = uri;
            view.removeCallbacks(documentSync);
            view.removeCallbacks(highlightSync);
            view.setInfoProvider(null);
            view.setSuggestionProvider(null);
            view.setSignatureProvider(null);
            view.setDiagnosticProvider(null);
            view.setServerQuickActionProvider(null);
            view.setGotoProvider(null);
            view.setServerHighlights(List.of());
            documentViewsGeneration.incrementAndGet();
            view.setInlayHints(List.of());
            view.setServerFoldRegions(List.of());
            view.setServerSemantics(List.of());
            resetProviders();
            documentOpened = false;
            capabilities = LspCapabilities.NONE;
            workspace = null;
            workspaceListener = null;
            languageId = null;
            session = null;
            uri = null;
            moveTo(ConnectionStatus.DISCONNECTED, null);
            if (listener != null) {
                registry.detach(language, documentUri, listener);
            }
            view.post(view::invalidate);
            return;
        }
        view.removeCallbacks(documentSync);
        view.removeCallbacks(highlightSync);
        view.setInfoProvider(null);
        view.setSuggestionProvider(null);
        view.setSignatureProvider(null);
        view.setDiagnosticProvider(null);
        view.setServerQuickActionProvider(null);
        view.setGotoProvider(null);
        view.setServerHighlights(List.of());
        // la couverture du document se range pareillement : les
        // indices, les plis, les jetons sémantiques du serveur mort
        // ne valent plus rien
        documentViewsGeneration.incrementAndGet();
        view.setInlayHints(List.of());
        view.setServerFoldRegions(List.of());
        view.setServerSemantics(List.of());
        resetProviders();
        documentOpened = false;
        capabilities = LspCapabilities.NONE;
        moveTo(ConnectionStatus.DISCONNECTED, null);
        // la poignée de main de l'arrêt part au fil de fond : le fil
        // d'interface ne l'attend plus (l'attente d'une seconde y
        // gelait l'écran) — la liaison change de main ici, la
        // fermeture s'y règle, l'exécuteur meurt après elle (un
        // rebranchement en relance un neuf)
        LspSession stopping = session;
        session = null;
        uri = null;
        ExecutorService stoppingExecutor = shutdownExecutor;
        shutdownExecutor = newExecutor();
        stoppingExecutor.execute(() -> {
            try {
                stopping.shutdown();
            } finally {
                stoppingExecutor.shutdown();
            }
        });
        view.post(view::invalidate);
    }

    /**
     * Change le document ouvert côté serveur (le passage d'onglet) :
     * l'onglet qui s'en va se FERME ({@code didClose} à la voie
     * directe, détachement au registre — le dernier d'un langage y
     * ferme son serveur), celui qui arrive s'OUVRE au TEXTE COURANT
     * de la vue et au langage donné — le serveur ne voit jamais le
     * contenu d'un onglet sous l'URI d'un autre. Les réponses en vol
     * de l'ancien document se périment (la génération du document
     * avance : aucune carte ne s'ouvrira d'une réponse tardive), ses
     * vues se rangent (surlignages, plis, jetons, indices — les
     * positions ne valent plus) et la pompe du document neuf repart.
     *
     * <p>La vue doit déjà porter le texte du document qui arrive :
     * l'hôte RESTAURE l'état du nouvel onglet AVANT d'appeler (le
     * {@code didOpen} y emporte le texte restauré). Sans liaison
     * vivante ou vers le même document : rien ne se passe.</p>
     *
     * @param newUri        l'URI du document qui arrive
     * @param newLanguageId le langage du document qui arrive
     */
    public void switchDocument(String newUri, String newLanguageId) {
        if (session == null || newUri == null || newLanguageId == null
                || (newUri.equals(uri) && newLanguageId.equals(languageId))) {
            return;
        }
        // la pompe de l'ancien document s'arrête : son envoi différé ne
        // doit pas partir sous l'URI neuve
        view.removeCallbacks(documentSync);
        view.removeCallbacks(highlightSync);
        // le document servi CHANGE : les réponses ponctuelles en vol
        // de l'ancien se périment
        documentChangeGeneration.incrementAndGet();
        // les réponses en vol de l'ancien se périment, ses vues se
        // rangent (les positions ne valent plus)
        documentViewsGeneration.incrementAndGet();
        signatureGeneration.incrementAndGet();
        highlightGeneration.incrementAndGet();
        view.setServerHighlights(List.of());
        view.setInlayHints(List.of());
        view.setServerFoldRegions(List.of());
        view.setServerSemantics(List.of());
        resetProviders();
        String leavingUri = uri;
        String leavingLanguage = languageId;
        if (workspace != null && definition != null) {
            // LA VOIE DU REGISTRE : l'onglet qui s'en va se détache
            // (son document s'y ferme), celui qui arrive s'y attache
            // (un serveur du langage y vit déjà ou y monte)
            LspWorkspace.DocumentListener listener = workspaceListener;
            if (listener != null && leavingUri != null
                    && leavingLanguage != null) {
                workspace.detach(leavingLanguage, leavingUri, listener);
            }
            uri = newUri;
            languageId = newLanguageId;
            LspSession shared = workspace.attach(languageId, definition, uri,
                    view.getBuffer().toString(), listener);
            if (shared != null) {
                session = shared;
                capabilities = shared.capabilities();
                documentOpened = true;
            }
        } else if (documentOpened
                && connectionStatus == ConnectionStatus.CONNECTED) {
            // LA VOIE DIRECTE : l'onglet qui s'en va se FERME, celui
            // qui arrive s'OUVRE au texte courant de la vue
            if (leavingUri != null) {
                session.didClose(leavingUri);
            }
            uri = newUri;
            languageId = newLanguageId;
            session.didOpen(uri, view.getBuffer().toString(), languageId);
        } else {
            // la liaison ne porte aucun document ouvert : le changement
            // se réduit au rebasage (le didOpen suivra à la connexion)
            uri = newUri;
            languageId = newLanguageId;
        }
        // la pompe du document neuf repart : les symboles, les indices,
        // les plis, les jetons sémantiques décrivent le document d'après
        refreshDocumentViews();
        scheduleHighlightSync();
        view.post(view::invalidate);
    }

    /**
     * LE DOCUMENT A CHANGÉ SOUS LA FAÇADE (A4 — l'hôte a ouvert un
     * autre document par la vue, sans passer par le passage
     * d'onglet) : tout ce qui parlait de l'ancien se périme — les
     * générations avancent (aucune réponse en vol n'ouvrira de
     * carte, les réponses ponctuelles comme celles de la frappe),
     * les caches se rangent, la pompe de synchronisation s'arrête
     * (l'hôte décide de la suite : le passage d'onglet y rouvrira,
     * le débranchement y rangera). Les vues du serveur, la vue les
     * range elle-même au vrai changement d'Autre texte (la
     * réouverture du MÊME n'y touche pas : ses plans cuits restent).
     */
    private void onViewDocumentChanged() {
        documentChangeGeneration.incrementAndGet();
        documentViewsGeneration.incrementAndGet();
        signatureGeneration.incrementAndGet();
        highlightGeneration.incrementAndGet();
        resetProviders();
        view.removeCallbacks(documentSync);
        view.removeCallbacks(highlightSync);
    }

    /**
     * La liaison meurt sans débrancher la façade (le harnais y
     * reproduit le crash de l'appareil : la future d'une requête
     * échoue « flux fermé », la réponse arrive null — la façade
     * doit survivre).
     */
    public void harnessKillSession() {
        LspSession alive = session;
        if (alive == null) {
            return;
        }
        alive.harnessCloseStreams();
    }

    /**
     * Le survol demande-t-il au serveur ?
     *
     * @param enabled vrai pour demander (vaut par défaut)
     */
    public void setHoverEnabled(boolean enabled) {
        hoverEnabled = enabled;
    }

    /**
     * La complétion demande-t-elle au serveur ?
     *
     * @param enabled vrai pour demander (vaut par défaut)
     */
    public void setCompletionEnabled(boolean enabled) {
        completionEnabled = enabled;
    }

    /**
     * L'aide aux paramètres demande-t-elle au serveur ?
     *
     * @param enabled vrai pour demander (vaut par défaut)
     */
    public void setSignatureHelpEnabled(boolean enabled) {
        signatureEnabled = enabled;
    }

    /**
     * Les diagnostics montent-ils à la carte (et à la gouttière) ?
     *
     * @param enabled vrai pour monter (vaut par défaut)
     */
    public void setDiagnosticsEnabled(boolean enabled) {
        diagnosticsEnabled = enabled;
        view.post(view::invalidate);
    }


    /**
     * L'écouteur des diagnostics du serveur : le panneau de l'hôte s'y
     * rafraîchit (l'appel arrive sur le fil de l'interface, la
     * liste y est entière).
     *
     * @param listener l'écouteur (null : personne n'écoute)
     */
    /**
     * Pose l'écouteur du cycle de la connexion (l'app y dit la
     * liaison vivante ou morte au doigt — le toast).
     *
     * @param listener l'écouteur, {@code null} pour le retirer
     */
    public void setConnectionListener(ConnectionListener listener) {
        connectionListener = listener;
    }

    /**
     * L'état courant de la connexion (l'écouteur y naît, l'écran
     * y lit).
     *
     * @return l'état, jamais {@code null}
     */
    public ConnectionStatus connectionStatus() {
        return connectionStatus;
    }

    /**
     * Une liaison vit-elle SOUS la façade (branchée ET vivante, ou
     * morte mais pas encore débranchée) ? Le bouton de l'écran y
     * décide : détacher une liaison morte (la nettoyer) plutôt que
     * d'en relancer une par-dessus.
     *
     * @return vrai tant qu'une liaison y est posée
     */
    public boolean hasConnection() {
        return session != null;
    }

    public void setDiagnosticsListener(DiagnosticsListener listener) {
        diagnosticsListener = listener;
    }

    /**
     * L'écouteur des places d'un AUTRE document : l'aller-à la
     * définition ou les références du serveur peuvent viser un autre
     * fichier — l'hôte qui tient plusieurs documents y navigue lui-
     * même (l'éditeur n'en tient qu'un, les places d'ailleurs se
     * laissent sans écouteur).
     *
     * @param listener l'écouteur (null : personne n'écoute)
     */
    public void setNavigationListener(NavigationListener listener) {
        navigationListener = listener;
    }

    /**
     * Le surlignage des occurrences suit-il le curseur (le protocole
     * {@code textDocument/documentHighlight} — la teinte bleue, la
     * recherche reste ambre) ? La coupure range la teinte et périmé
     * l'avis en vol ; le retour REDEMANDE les occurrences du curseur
     * courant (la teinte revient sans bouger le doigt).
     *
     * @param enabled vrai pour suivre (vaut par défaut)
     */
    public void setDocumentHighlightEnabled(boolean enabled) {
        documentHighlightEnabled = enabled;
        if (!enabled) {
            highlightGeneration.incrementAndGet();
            view.removeCallbacks(highlightSync);
            view.setServerHighlights(List.of());
        } else if (session != null) {
            scheduleHighlightSync();
        }
    }

    /**
     * L'aller-à la définition du symbole au curseur : le serveur y
     * rend ses places — celle du MÊME document y fait sauter le
     * curseur (la trame suivante y portera la paire d'appariement),
     * les places d'un autre document partent à l'écouteur de
     * l'hôte. Le serveur qui ne sait pas (la capacité non déclarée)
     * ne s'y offense pas ; le serveur qui ne répond rien y dit «
     * aucune définition ».
     */
    public void requestDefinition() {
        if (session == null || !capabilities.definition()) {
            return;
        }
        LspPosition position = caretPosition();
        final long change = documentChangeGeneration.get();
        session.definition(uri, position).whenComplete((places, failure) ->
                view.post(() -> {
                    if (change != documentChangeGeneration.get()) {
                        // le document a changé pendant le vol : le saut
                        // parlerait de l'ancien texte
                        return;
                    }
                    navigateTo(places, view.wordAtCaret(), "définition");
                }));
    }

    /**
     * LA DÉFINITION EN APERÇU (la définition en aperçu du rapport — le « Quick
     * Definition » de l'atelier) : lire la déclaration du symbole au
     * curseur SANS quitter la position — la question part au serveur
     * ({@code textDocument/definition}, la MÊME question que l'aller-à),
     * la carte s'ouvre sur les lignes de la déclaration (une douzaine
     * au plafond, lues du document courant), le bouton « Ouvrir » y
     * saute. La définition d'un AUTRE document se laisse à l'hôte
     * (l'éditeur n'en tient qu'un — l'aller-à y va, l'aperçu n'y
     * ment pas : il ne lit pas ce qu'il n'a pas).
     */
    public void requestDefinitionPreview() {
        if (session == null || !capabilities.definition()) {
            return;
        }
        String symbol = view.wordAtCaret();
        LspPosition position = caretPosition();
        final long change = documentChangeGeneration.get();
        session.definition(uri, position).whenComplete((places, failure) ->
                view.post(() -> {
                    if (change != documentChangeGeneration.get()) {
                        // le document a changé pendant le vol : l'aperçu
                        // parlerait de l'ancien texte
                        return;
                    }
                    showDefinitionPreview(places, symbol);
                }));
    }

    /**
     * La carte de l'aperçu se construit des places reçues : la
     * PREMIÈRE définition du MÊME document y vit (ses lignes se
     * lisent, « Ouvrir » y saute), celle d'un autre document se
     * laisse à l'hôte.
     *
     * @param places les places du serveur
     * @param symbol le symbole questionné
     */
    private void showDefinitionPreview(List<LspLocation> places,
            String symbol) {
        if (places == null || places.isEmpty()) {
            sayAtCaret("aucune définition trouvée");
            return;
        }
        LspLocation place = places.get(0);
        if (place == null || place.range() == null) {
            sayAtCaret("aucune définition trouvée");
            return;
        }
        if (uri == null || !uri.equals(place.uri())) {
            sayAtCaret("la définition vit dans un autre document");
            return;
        }
        TextBuffer buffer = view.getBuffer();
        int offset = Math.min(offsetOf(buffer, place.range().start()),
                buffer.length());
        int line = buffer.lineOfOffset(offset);
        List<String> lines = new ArrayList<>();
        for (int at = line; at < Math.min(line + 12, buffer.getLineCount());
                at++) {
            lines.add(buffer.getText(buffer.getLineStart(at),
                    buffer.getLineEnd(at)).toString());
        }
        view.showDefinitionPreview(new PreviewCard.Preview(
                symbol == null ? "" : symbol, "", line, lines,
                () -> view.caretTo(offset)));
    }

    /**
     * La question des symboles de l'espace de travail part ({@code
     * workspace/symbol} — l'aller au symbole du rapport) : la réponse
     * construit les RANGÉES de la carte (le nom, l'icône de sa
     * nature, le fichier · la ligne à droite, le saut au choix), la
     * carte ouverte se rouvre fraîche à son arrivée (les rangées
     * connues y tiennent l'attente — le fil d'interface n'attend
     * jamais).
     */
    private void askWorkspaceSymbols() {
        if (session == null || !capabilities.workspaceSymbol()) {
            return;
        }
        final long change = documentChangeGeneration.get();
        session.workspaceSymbol("").whenComplete((answer, failure) ->
                view.post(() -> {
                    if (change != documentChangeGeneration.get()) {
                        // le document a changé pendant le vol : les
                        // rangées parléraient de l'ancien texte
                        return;
                    }
                    if (failure != null || answer == null || answer.isEmpty()) {
                        return;
                    }
                    List<GotoCard.GotoRow> rows = new ArrayList<>();
                    for (LspWorkspaceSymbol symbol : answer) {
                        if (symbol == null || symbol.location() == null) {
                            continue;
                        }
                        LspLocation place = symbol.location();
                        final LspLocation target = place;
                        int line = place.range() == null || place.range().start() == null
                                ? 0 : Math.max(0, place.range().start().line());
                        String where = fileNameOf(place.uri()) + " · ligne "
                                + (line + 1);
                        rows.add(new GotoCard.GotoRow(symbol.name(),
                                kindOf(symbol.kind()), where,
                                () -> jumpTo(target)));
                    }
                    gotoRows = List.copyOf(rows);
                    // la carte ouverte se ROUVRE fraîche (la réponse
                    // arrive après l'ouverture aux rangées d'avant) ;
                    // l'aller demandé à un cache vide s'y OUVRE (le
                    // premier tap ne reste pas sans effet)
                    if (view.gotoCard() != null && view.gotoCard().isShown()) {
                        view.showGotoPopup(gotoRows);
                    } else if (gotoAsked) {
                        gotoAsked = false;
                        view.showGotoPopup(gotoRows);
                    }
                }));
    }

    /**
     * LES USAGES du symbole au curseur — LA COMMANDE UNIQUE (R-C du
     * rapport des popups : deux moteurs y vivaient, deux résultats
     * pour la MÊME intention — la recherche du document ouvrait la
     * carte, le serveur sautait à la première occurrence). La carte
     * vit immédiate aux occurrences du document (le repli), les
     * références SÉMANTIQUES du serveur y REMPLACENT la liste à leur
     * arrivée ; les places du MÊME document se surlignent aussi (la
     * teinte bleue du protocole — chaque usage se lit d'un coup
     * d'œil dans le texte), les places d'un autre document partent à
     * l'écouteur de l'hôte.
     */
    public void requestReferences() {
        view.showUsagesPopup();
    }

    /**
     * Le fournisseur d'usages de la vue : les références du serveur
     * ({@code textDocument/references}) deviennent les RANGÉES de la
     * carte (chaque rangée porte son extrait, le choix y navigue —
     * plus un saut aveugle à la première occurrence) ; la réponse
     * nulle (pas de liaison, la capacité manque, le serveur se tait)
     * rend la place à la recherche du document.
     *
     * @param symbol le mot au curseur
     * @param line   la ligne du curseur
     * @param column la colonne du curseur
     * @param answer l'écouteur de la réponse (les rangées, ou null)
     */
    private void usagesFor(String symbol, int line, int column,
            EditorView.UsageAnswer answer) {
        if (session == null || uri == null || !capabilities.references()) {
            answer.answer(null);
            return;
        }
        LspPosition position = new LspPosition(line, column);
        final long change = documentChangeGeneration.get();
        session.references(uri, position, true).whenComplete((places, failure) ->
                view.post(() -> {
                    if (change != documentChangeGeneration.get()) {
                        // le document a changé pendant le vol : les
                        // usages parleraient de l'ancien texte
                        answer.answer(null);
                        return;
                    }
                    if (places == null || places.isEmpty()) {
                        answer.answer(null);
                        return;
                    }
                    // toutes les places du document se surlignent : les
                    // usages se lisent d'un coup d'œil
                    view.setServerHighlights(rangesOf(places));
                    highlightGeneration.incrementAndGet();
                    List<LspLocation> external = new ArrayList<>();
                    String externalUri = null;
                    for (LspLocation place : places) {
                        if (place != null && !uri.equals(place.uri())) {
                            if (externalUri == null) {
                                externalUri = place.uri();
                            }
                            if (place.uri().equals(externalUri)) {
                                external.add(place);
                            }
                        }
                    }
                    if (!external.isEmpty() && navigationListener != null) {
                        navigationListener.onExternalPlaces(externalUri,
                                List.copyOf(external));
                    }
                    answer.answer(usageRowsOf(places));
                }));
    }

    /**
     * Les rangées de la carte pour les places d'un même document :
     * l'extrait de la ligne (le code se lit dans la carte), la
     * navigation au choix. Null quand aucune place n'y vit : la
     * recherche du document tient la place.
     *
     * @param places les places du serveur
     * @return les rangées, ou null
     */
    private List<UsageCard.Usage> usageRowsOf(List<LspLocation> places) {
        TextBuffer buffer = view.getBuffer();
        List<UsageCard.Usage> rows = new ArrayList<>();
        for (LspLocation place : places) {
            if (place == null || place.range() == null
                    || !uri.equals(place.uri())) {
                continue;
            }
            int offset = Math.min(offsetOf(buffer, place.range().start()),
                    buffer.length());
            int line = buffer.lineOfOffset(offset);
            int column = offset - buffer.getLineStart(line);
            int lineStart = buffer.getLineStart(line);
            String lineText = buffer.getText(lineStart,
                    buffer.getLineEnd(line)).toString();
            String excerpt = lineText.length() > 80
                    ? lineText.substring(0, 80) : lineText;
            final int at = offset;
            rows.add(new UsageCard.Usage(line, column, "", excerpt,
                    () -> caretAt(at)));
        }
        return rows.isEmpty() ? null : rows;
    }

    /**
     * Le curseur se pose à l'offset (borné au texte vivant — la carte
     * peut vivre plus que le document d'avant).
     *
     * @param offset l'offset visé
     */
    private void caretAt(int offset) {
        view.caretTo(Math.min(offset, view.getBuffer().length()));
    }

    /**
     * Le renommage du symbole au curseur : la préparation du protocole
     * ({@code textDocument/prepareRename}) y vérifie d'abord le
     * symbole — le serveur qui n'offre pas la préparation y tente le
     * renommage direct. La carte s'ouvre au caret : le nom neuf
     * démarre du symbole (la façon d'Android Studio), la validation
     * y part au serveur ({@code textDocument/rename}), les
     * remplacements de chaque occurrence se posent en UNE unité
     * d'annulation.
     */
    public void requestRename() {
        if (session == null || !capabilities.rename()) {
            return;
        }
        String symbol = view.wordAtCaret();
        if (symbol.isEmpty()) {
            sayAtCaret("aucun symbole au curseur");
            return;
        }
        LspPosition position = caretPosition();
        if (capabilities.prepareRename()) {
            final long change = documentChangeGeneration.get();
            session.prepareRename(uri, position).whenComplete((range, failure) ->
                    view.post(() -> {
                        if (change != documentChangeGeneration.get()) {
                            // le document a changé pendant le vol : la
                            // carte parlerait de l'ancien texte
                            return;
                        }
                        if (range == null) {
                            sayAtCaret("le symbole ne se renomme pas (dit le serveur)");
                            return;
                        }
                        view.showRenamePopup(symbol, this::submitRename);
                    }));
            return;
        }
        view.showRenamePopup(symbol, this::submitRename);
    }

    /**
     * Le formatage du document : le serveur réécrit les lignes
     * ({@code textDocument/formatting}), les remplacements se posent
     * en une unité d'annulation — la façon d'Android Studio (Ctrl+Alt+L).
     */
    public void formatDocument() {
        if (session == null || !capabilities.formatting()) {
            return;
        }
        final long change = documentChangeGeneration.get();
        session.formatting(uri, view.tabSize(), true).whenComplete((edits, failure) ->
                view.post(() -> {
                    if (change != documentChangeGeneration.get()) {
                        // le document a changé pendant le vol : les
                        // remplacements parleraient de l'ancien texte —
                        // les POSER corromprait le neuf
                        return;
                    }
                    if (!view.applyTextEdits(edits)) {
                        sayAtCaret("rien à formater (dit le serveur)");
                    }
                }));
    }

    /**
     * Ce que le serveur a déclaré savoir (la négociation de
     * l'initialisation — l'hôte y lit ce que la façade offre).
     *
     * @return les capacités négociées, NONE sans liaison
     */
    public LspCapabilities capabilities() {
        return capabilities;
    }

    /**
     * Les places reçues y naviguent : la place UNIQUE du MÊME
     * document fait sauter le curseur (borné au texte), celle d'un
     * AUTRE document part à l'écouteur de l'hôte (il tient plusieurs
     * documents, lui) ; aucune place y dit « non trouvé » —
     * l'aller-à d'Android Studio parle pareil. LE CHOIX DE CIBLES
     * (le choix de cibles — D-P3 du rapport des popups) : le serveur qui renvoie
     * PLUSIEURS places ne saute plus à la première, AVEUGLE aux
     * autres — la carte des cibles s'y ouvre (une rangée par cible,
     * l'extrait s'y lit, le choix y navigue).
     *
     * @param places  les places du serveur, null sans réponse
     * @param symbol le symbole questionné (le titre le dit)
     * @param quoi    le nom de la chose cherchée (le message le dit)
     */
    private void navigateTo(List<LspLocation> places, String symbol, String quoi) {
        if (places == null || places.isEmpty()) {
            sayAtCaret("aucune " + quoi + " trouvée");
            return;
        }
        if (places.size() > 1) {
            showTargetsCard(places, symbol, quoi);
            return;
        }
        jumpTo(places.get(0));
    }

    /**
     * LA CARTE DU CHOIX DE CIBLES (le choix de cibles) : une rangée par place — le
     * symbole, le fichier et sa ligne à droite, l'EXTRAIT de la
     * ligne dessous (le code de la cible se lit dans la carte — le
     * choix y navigue).
     */
    private void showTargetsCard(List<LspLocation> places, String symbol,
            String quoi) {
        TextBuffer buffer = view.getBuffer();
        String name = symbol == null ? "" : symbol;
        List<TargetCard.Target> rows = new ArrayList<>();
        for (LspLocation place : places) {
            if (place == null || place.range() == null) {
                continue;
            }
            int offset = offsetOf(buffer, place.range().start());
            int line = buffer.lineOfOffset(
                    Math.min(Math.max(0, offset), buffer.length()));
            String file = "";
            String excerpt = "";
            if (uri != null && uri.equals(place.uri())) {
                String lineText = buffer.getText(buffer.getLineStart(line),
                        buffer.getLineEnd(line)).toString();
                excerpt = lineText.length() > 80
                        ? lineText.substring(0, 80) : lineText;
            } else {
                file = fileNameOf(place.uri());
            }
            final LspLocation target = place;
            rows.add(new TargetCard.Target(name, file, line, excerpt,
                    () -> jumpTo(target)));
        }
        if (rows.isEmpty()) {
            sayAtCaret("aucune " + quoi + " trouvée");
            return;
        }
        view.showTargetPopup(capitalized(quoi) + "s de « " + name + " »", rows);
    }

    /** Le saut à une place : le document courant y reçoit le curseur,
     * un autre document part à l'écouteur de l'hôte. */
    private void jumpTo(LspLocation place) {
        if (place == null) {
            return;
        }
        if (uri != null && uri.equals(place.uri())) {
            TextBuffer buffer = view.getBuffer();
            int offset = Math.min(offsetOf(buffer, place.range().start()),
                    buffer.length());
            // L'EMPLACEMENT RÉCENT (les fichiers récents — les fichiers récents) : chaque
            // saut du document s'y enregistre (le fichier, la ligne,
            // l'extrait — le retour y revient)
            int line = buffer.lineOfOffset(offset);
            String lineText = buffer.getText(buffer.getLineStart(line),
                    buffer.getLineEnd(line)).toString();
            String excerpt = lineText.length() > 60
                    ? lineText.substring(0, 60) : lineText;
            view.recentFiles().recordLocation(fileNameOf(uri), line,
                    excerpt, android.os.SystemClock.uptimeMillis(),
                    () -> view.caretTo(offset));
            view.caretTo(offset);
            return;
        }
        if (navigationListener != null) {
            navigationListener.onExternalPlaces(place.uri(), List.of(place));
        } else {
            sayAtCaret("la cible vit dans un autre document");
        }
    }

    /** Le nom de fichier d'un URI (le queue après le dernier slash). */
    private static String fileNameOf(String fileUri) {
        if (fileUri == null) {
            return "";
        }
        int slash = fileUri.lastIndexOf('/');
        return slash >= 0 ? fileUri.substring(slash + 1) : fileUri;
    }

    /** La première lettre en majuscule (le titre de la carte le dit). */
    private static String capitalized(String word) {
        if (word == null || word.isEmpty()) {
            return "Cible";
        }
        return Character.toUpperCase(word.charAt(0)) + word.substring(1);
    }

    /**
     * La validation du renommage : la requête part au serveur, la
     * réponse posera les remplacements (chaque occurrence du symbole
     * — une seule unité d'annulation). Parti : la carte se range.
     *
     * @param symbol  le symbole d'origine
     * @param newName le nom neuf soumis
     * @return vrai quand la requête part (la carte se range)
     */
    private boolean submitRename(String symbol, String newName) {
        if (session == null) {
            return false;
        }
        LspPosition position = caretPosition();
        final long change = documentChangeGeneration.get();
        session.rename(uri, position, newName).whenComplete((edits, failure) ->
                view.post(() -> {
                    if (change != documentChangeGeneration.get()) {
                        // le document a changé pendant le vol : les
                        // remplacements parleraient de l'ancien texte —
                        // l'aperçu mentirait, les POSER corromprait
                        return;
                    }
                    if (edits == null || edits.isEmpty()) {
                        sayAtCaret("le renommage ne pose rien (dit le serveur)");
                        return;
                    }
                    // L'APERÇU DU RENOMMAGE (l'aperçu du renommage du rapport) : les
                    // occurrences se VOIENT avant de se poser — les
                    // cases les retiennent, le bouton « Appliquer » pose
                    // les retenues (UNE unité d'annulation)
                    view.showRenamePreview("Renommer « " + symbol
                            + " » en « " + newName + " »",
                            renameOccurrencesOf(edits));
                }));
        return true;
    }

    /**
     * LES OCCURRENCES DU RENOMMAGE (l'aperçu du renommage) : chaque
     * remplacement du serveur devient une rangée — sa place à droite
     * (la ligne), l'EXTRAIT de la ligne dessous (le code se lit), son
     * remplacement attaché (la case cochée le pose à l'application).
     *
     * @param edits les remplacements du serveur
     * @return les occurrences, jamais null
     */
    private List<RenamePreviewCard.Occurrence> renameOccurrencesOf(
            List<LspTextEdit> edits) {
        TextBuffer buffer = view.getBuffer();
        List<RenamePreviewCard.Occurrence> occurrences = new ArrayList<>();
        for (LspTextEdit edit : edits) {
            if (edit == null || edit.range() == null) {
                continue;
            }
            int offset = Math.min(offsetOf(buffer, edit.range().start()),
                    buffer.length());
            int line = buffer.lineOfOffset(offset);
            String lineText = buffer.getText(buffer.getLineStart(line),
                    buffer.getLineEnd(line)).toString();
            String excerpt = lineText.length() > 60
                    ? lineText.substring(0, 60) : lineText;
            occurrences.add(new RenamePreviewCard.Occurrence(
                    "ligne " + (line + 1), excerpt, List.of(edit)));
        }
        return occurrences;
    }

    /**
     * Les fonctions du protocole à l'ampoule (APRÈS les intentions de
     * l'hôte — le second slot) : l'aller-à la définition, les
     * références, le renommage, le formatage — chacun ne se montre
     * que sur la capacité DECLAREE (demander une fonction non déclarée
     * offense le protocole) ; les corrections rapides du serveur
     * ({@code textDocument/codeAction}) s'y suivent, leur réponse
     * arrive asynchrone — le cache par ligne y tient, la carte ouverte
     * se rouvre fraîche à l'arrivée.
     *
     * @param line   la ligne du curseur
     * @param column la colonne du curseur
     * @return les fonctions du serveur, jamais null
     */
    private List<QuickActionCard.Entry> serverQuickActionsAt(int line, int column) {
        List<QuickActionCard.Entry> actions = new ArrayList<>();
        if (session == null || uri == null) {
            return actions;
        }
        // les libellés y vivent à la SOURCE UNIQUE des chaînes de la
        // vue (la localisation de l'hôte y passe — les défauts portent
        // le français courant)
        if (capabilities.definition()) {
            actions.add(new QuickActionCard.Entry(
                    view.popupStrings().definitionEntry(), this::requestDefinition));
            // L'APERÇU DE LA DÉFINITION (la définition en aperçu) :
            // lire la déclaration SANS quitter la position (le bouton «
            // Ouvrir » y saute)
            actions.add(new QuickActionCard.Entry(
                    view.popupStrings().definitionPreviewEntry(),
                    this::requestDefinitionPreview));
        }
        if (capabilities.references()) {
            actions.add(new QuickActionCard.Entry(
                    view.popupStrings().referencesEntry(), view::showUsagesPopup));
        }
        if (capabilities.rename()) {
            actions.add(new QuickActionCard.Entry(
                    view.popupStrings().renameEntry(), this::requestRename));
        }
        if (capabilities.formatting()) {
            actions.add(new QuickActionCard.Entry(
                    view.popupStrings().formatEntry(), this::formatDocument));
        }
        if (capabilities.documentSymbol()) {
            actions.add(new QuickActionCard.Entry(
                    view.popupStrings().symbolsEntry(), this::showSymbolsCard));
        }
        if (capabilities.codeAction()) {
            actions.addAll(codeActionsAt(line));
        }
        return actions;
    }

    /**
     * Les corrections rapides du serveur à une ligne : le cache par
     * ligne (la réponse arrive asynchrone — la première lecture rend
     * vide, l'arrivée rouvre la carte ouverte, la trame suivante y
     * porte les correctifs).
     *
     * @param line la ligne du curseur
     * @return les corrections du serveur, jamais null
     */
    private List<QuickActionCard.Entry> codeActionsAt(int line) {
        if (line != codeActionsLine) {
            codeActionsLine = line;
            codeActionsCache.clear();
            askCodeActions(line);
        }
        return List.copyOf(codeActionsCache);
    }

    /**
     * La demande des corrections part au serveur : la réponse y
     * construit les entrées (chaque correctif porte ses
     * remplacements), la carte ouverte à la ligne se rouvre fraîche.
     *
     * @param line la ligne demandée
     */
    private void askCodeActions(int line) {
        final int asked = line;
        LspRange range = lineRange(asked);
        final long change = documentChangeGeneration.get();
        session.codeActions(uri, range, diagnosticsOf(asked))
                .whenComplete((answer, failure) -> view.post(() -> {
                    if (change != documentChangeGeneration.get()) {
                        return; // le document a changé : la réponse parle de l'ancien
                    }
                    if (asked != codeActionsLine) {
                        return; // la ligne a bougé : la réponse ne vaut plus
                    }
                    // le serveur mort répond null (la future échoue :
                    // « Stream closed », l'appareil l'a vu en plantage
                    // de l'écran) : aucun correctif n'arrive — la
                    // réponse nulle ne doit pas lever au fil principal
                    if (failure != null || answer == null) {
                        codeActionsCache.clear();
                        return;
                    }
                    codeActionsCache.clear();
                    List<ImportBubbleCard.Import> imports = new ArrayList<>();
                    for (LspCodeAction action : answer) {
                        // LA SUGGESTION D'IMPORT (la suggestion d'import — la bulle « Importer
                        // x.y.Z ? ») : les correctifs d'import du serveur
                        // (le genre du protocole le dit : quickfix.import)
                        // se détournent à LEUR bulle — la carte d'actions
                        // garde les correctifs ordinaires
                        if (action.kind() != null
                                && action.kind().endsWith("import")) {
                            imports.add(new ImportBubbleCard.Import(
                                    action.title(),
                                    () -> view.applyTextEdits(action.edits())));
                        } else {
                            codeActionsCache.add(new QuickActionCard.Entry(
                                    view.popupStrings().fixTitle(action.title()),
                                    () -> view.applyTextEdits(action.edits())));
                        }
                    }
                    if (!imports.isEmpty()) {
                        view.showImportPopup(view.wordAtCaret(), imports);
                    }
                    // la carte ouverte à la ligne se rouvre fraîche ;
                    // la carte close y laisse la trame suivante lire —
                    // LA CARTE DE DIAGNOSTIC aussi (l'étage des
                    // correctifs y vit sous les messages)
                    if (view.quickActionCard() != null && view.quickActionCard().isShown()
                            && view.quickActionCard().line() == asked) {
                        view.openQuickActionsAtLine(asked);
                    } else if (view.diagnosticCard() != null
                            && view.diagnosticCard().isShown()
                            && view.diagnosticCard().line() == asked) {
                        view.openDiagnosticsAtLine(asked);
                    } else {
                        view.setServerQuickActionProvider(this::serverQuickActionsAt);
                    }
                }));
    }

    /**
     * Les diagnostics du serveur sur une ligne (le contexte des
     * corrections — les correctifs du protocole s'y nourrissent).
     *
     * @param line la ligne
     * @return les diagnostics de la ligne, jamais null
     */
    private List<LspDiagnostic> diagnosticsOf(int line) {
        List<LspDiagnostic> found = new ArrayList<>();
        for (LspDiagnostic diagnostic : diagnostics) {
            if (diagnostic.range().start().line() == line) {
                found.add(diagnostic);
            }
        }
        return found;
    }

    /**
     * La plage d'une ligne entière (la question des corrections : le
     * protocole y interroge la ligne du curseur).
     *
     * @param line la ligne
     * @return la plage de la ligne
     */
    private LspRange lineRange(int line) {
        TextBuffer buffer = view.getBuffer();
        return new LspRange(
                LspPosition.ofOffset(buffer, buffer.getLineStart(line)),
                LspPosition.ofOffset(buffer, buffer.getLineEnd(line)));
    }

    /**
     * La position LSP du curseur.
     *
     * @return la position du curseur au protocole
     */
    private LspPosition caretPosition() {
        TextBuffer buffer = view.getBuffer();
        return LspPosition.ofOffset(buffer, view.getCaretOffset());
    }

    /**
     * L'offset d'une position du protocole, borné au texte.
     *
     * @param buffer   le tampon portant le texte
     * @param position la position du protocole
     * @return l'offset équivalent
     */
    private static int offsetOf(TextBuffer buffer, LspPosition position) {
        return position.toOffset(buffer);
    }

    /**
     * Les plages des places d'un même document.
     *
     * @param places les places
     * @return les plages, jamais null
     */
    private static List<LspRange> rangesOf(List<LspLocation> places) {
        List<LspRange> ranges = new ArrayList<>();
        for (LspLocation place : places) {
            if (place != null && place.range() != null) {
                ranges.add(place.range());
            }
        }
        return ranges;
    }

    /**
     * Le curseur a bougé : le surlignage des occurrences se
     * reprogramme (amorti — le doigt qui traîne ne questionne qu'à
     * la pose ; le document qui change le range aussi).
     *
     * @param caret  le curseur après le mouvement
     * @param anchor l'ancre après le mouvement (-1 sans sélection)
     */
    private void onSelectionMoved(int caret, int anchor) {
        if (session == null || !documentHighlightEnabled
                || !capabilities.documentHighlight()) {
            return;
        }
        view.removeCallbacks(highlightSync);
        view.postDelayed(highlightSync, HIGHLIGHT_SYNC_MS);
    }

    /**
     * Le surlignage part au serveur : la génération avance (la
     * réponse qui reviendra trop tard se périme — le curseur a encore
     * bougé), la réponse pose les plages à la vue (la teinte bleue,
     * la recherche reste ambre).
     */
    private void pushHighlightToServer() {
        if (session == null || !documentHighlightEnabled
                || !capabilities.documentHighlight()) {
            return;
        }
        final long generation = highlightGeneration.incrementAndGet();
        LspPosition position = caretPosition();
        session.documentHighlight(uri, position).whenComplete((ranges, failure) ->
                view.post(() -> {
                    if (generation == highlightGeneration.get()) {
                        view.setServerHighlights(ranges);
                    }
                }));
    }

    /**
     * Le report amorti du surlignage (le branchement y range l'ancien,
     * le neuve part quand le serveur sait).
     */
    private void scheduleHighlightSync() {
        view.removeCallbacks(highlightSync);
        view.postDelayed(highlightSync, HIGHLIGHT_SYNC_MS);
    }

    /**
     * Un mot au caret : la popup d'info le dit (les réponses vides y
     * parlent — « aucune définition trouvée », la façon d'Android
     * Studio).
     *
     * @param message le mot à dire
     */
    private void sayAtCaret(String message) {
        TextBuffer buffer = view.getBuffer();
        int caret = view.getCaretOffset();
        int line = buffer.lineOfOffset(Math.min(caret, buffer.length()));
        int column = caret - buffer.getLineStart(line);
        view.showInfoPopup(message, line, column);
    }

    /**
     * Les diagnostics du dernier avis du serveur.
     *
     * @return la liste entière, jamais {@code null} (vide : aucun)
     */
    public List<LspDiagnostic> diagnostics() {
        return diagnostics;
    }

    /**
     * Le compte des diagnostics du dernier avis (le panneau y lit
     * son chiffre).
     *
     * @return le compte
     */
    public int diagnosticCount() {
        return diagnostics.size();
    }

    /**
     * L'écouteur commun des liaisons : les diagnostics du serveur
     * montent au panneau de l'hôte ET à la carte de diagnostic (le
     * compte se rafraîchit sur le fil de l'interface).
     *
     * @return l'écouteur branché à la liaison
     */
    private LspSession.Listener sessionListener() {
        return new LspSession.Listener() {
            @Override
            public void onStopped(boolean byServer) {
                // le canal s'est fermé : le serveur est mort (la
                // lecture finit) — l'état le dit au doigt (le toast
                // de l'app), les envois s'y arrêtent
                documentOpened = false;
                view.post(() -> moveTo(ConnectionStatus.DISCONNECTED,
                        byServer ? "le serveur s'est arrêté" : null));
            }

            @Override
            public void onDiagnostics(String published, List<LspDiagnostic> fresh) {
                diagnostics = List.copyOf(fresh);
                view.post(() -> {
                    if (diagnosticsListener != null) {
                        diagnosticsListener.onDiagnostics(diagnostics);
                    }
                    view.invalidate();
                });
            }
        };
    }

    /**
     * Les fournisseurs de la liaison branchée : la popup d'info
     * interroge le survol (la réponse du serveur prime sur l'index
     * local — le fournisseur rend null quand le serveur ne sait pas
     * ou quand la fonction se coupe) ; la complétion du serveur
     * monte à la carte (la vue tire à chaque trame — le fournisseur
     * cache par préfixe, la réponse revient asynchrone sur le fil du
     * lecteur : la neuve évince l'ancienne si le préfixe n'a pas
     * bougé, la trame suivante la porte) ; l'aide aux paramètres du
     * serveur prime sur l'index local (la réponse y arrive
     * pareillement asynchrone) ; les diagnostics de la ligne montent
     * à la carte (le marqueur de la gouttière en vit, le doigt y
     * l'ouvre).
     */
    private void attachProviders() {
        final String documentUri = uri;
        // LE FOURNISSEUR D'USAGES (R-C) : les références sémantiques
        // du serveur remplacent la liste locale à leur arrivée (la
        // présentation ne change JAMAIS : la carte des usages)
        view.setUsageProvider(this::usagesFor);
        // LE FOURNISSEUR DE L'ALLER AU SYMBOLE (l'aller au symbole — workspace/symbol) :
        // la question part asynchrone, les RANGÉES en cache y tiennent
        // l'attente (la carte s'ouvre aux rangées connues, la réponse
        // la rouvre fraîche à son arrivée — la leçon du survol : le
        // fil d'interface n'attend jamais). La capacité s'y lit À LA
        // DEMANDE (le câblage court AVANT l'arrivée de la négociation
        // — les capacités tombent asynchrones)
        view.setGotoProvider(() -> {
            if (!capabilities.workspaceSymbol()) {
                return List.of();
            }
            askWorkspaceSymbols();
            if (gotoRows.isEmpty()) {
                // la réponse vole : la carte s'ouvrira à son
                // arrivée (l'aller a été DEMANDÉ — le premier tap
                // ne reste pas sans effet)
                gotoAsked = true;
            }
            return gotoRows;
        });
        // LE SURVOL ASYNCHRONE (D-P1 du rapport des popups — la
        // coquille au-dessus) : la question part, le fil d'interface
        // N'ATTEND JAMAIS (l'attente vivait SUR le fil, jusqu'à
        // requestTimeoutMs par question) ; la popup vit immédiate
        // (« … »), la réponse la remplace à son arrivée — le retour
        // muet (le serveur ne sait pas, le survol est coupé, la
        // liaison est morte) rend la place à la description de
        // l'index local. LA RÉPONSE PORTE SON BALISAGE (D-P5) : le
        // Markdown des serveurs survit — la popup y rend ses titres,
        // listes, code et liens (le corps qui dépasse s'y défile)
        view.setInfoProvider((line, column, word, answer) -> {
            if (!hoverEnabled || session == null) {
                answer.answer(null, false);
                return;
            }
            session.hover(documentUri, new LspPosition(line, column))
                    .whenComplete((hover, failure) -> {
                        if (failure != null || hover == null || !hover.hasValue()) {
                            answer.answer(null, false);
                        } else {
                            answer.answer(hover.value(), hover.markdown());
                        }
                    });
        });
        // la complétion du serveur À LA LEÇON DE LA RÉFÉRENCE (l'IDE de
        // référence accumule ses candidats et FILTRE CÔTÉ CLIENT à la
        // frappe — la liste ne se vide JAMAIS pendant qu'une requête
        // vole) : le cache du dernier préfixe reçu reste posé, la
        // frappe le FILTRE localement (le préfixe ne fait que croître
        // dans un mot — les candidats du père portent le fils), et la
        // réponse du serveur REMPLACE le cache entier à son arrivée si
        // le préfixe n'a pas bougé. La carte vit pendant l'attente :
        // le filtrage local y donne les candidats vivants, la réponse
        // affine (le serveur voit plus que le préfixe : le contexte).
        // LES CANDIDATS ARRIVENT ENTIERS (D-P2 du rapport) : la nature,
        // le détail, l'obsolescence, la documentation, le remplacement
        // propre et l'ORDRE DU SERVEUR y vivent — la nature du
        // protocole y devient l'icône de la carte, l'ordre y trie (le
        // sortText prime, l'ordre d'arrivée sinon).
        view.setSuggestionProvider((prefix, line, column) -> {
            if (!completionEnabled || session == null || prefix.isEmpty()) {
                return List.of();
            }
            if (!prefix.equals(completionPrefix)) {
                final String asked = prefix;
                // le préfixe EXTEND le dernier reçu ? Le cache reste
                // UNE BONNE INDICATION (la leçon de l'index local :
                // un index périmé vaut mieux qu'une carte vide) — le
                // filtrage local ci-dessous l'exploite. Un préfixe qui
                // RACCOURCIT ou saute (retour arrière, autre mot)
                // invalide la clef : une requête neuve part, le cache
                // filtré tient l'attente.
                boolean extendsCache = completionPrefix != null
                        && prefix.length() > completionPrefix.length()
                        && prefix.startsWith(completionPrefix);
                completionPrefix = prefix;
                final boolean keepCache = extendsCache;
                session.completion(documentUri, new LspPosition(line, column))
                        .whenComplete((candidates, failure) -> {
                            if (asked.equals(completionPrefix) && candidates != null) {
                                completionCache.clear();
                                completionCache.addAll(candidates);
                                view.post(view::invalidate);
                            }
                        });
                if (!keepCache) {
                    return List.of();
                }
            }
            // le filtrage CÔTÉ CLIENT (prefixMatches à la référence) :
            // le candidat dont le nom commence par le préfixe vit,
            // l'autre attend la réponse — la liste ne se vide jamais
            // au fil de la frappe ; l'ORDRE du serveur y demeure (le
            // sortText prime, l'ordre d'arrivée sinon)
            List<CompletionCard.Entry> filtered = new ArrayList<>();
            List<jo.pigma.lsp.LspCompletionItem> sorted =
                    new ArrayList<>(completionCache);
            sorted.sort(LspEditor::completionOrder);
            for (jo.pigma.lsp.LspCompletionItem candidate : sorted) {
                if (candidate.label().regionMatches(true, 0, prefix, 0,
                        prefix.length())) {
                    filtered.add(entryOf(candidate));
                }
            }
            return filtered;
        });
        // LE RÉSOLVEUR DE DOCUMENTATION (la suite de D-P2) : le
        // candidat CHOISI se documente à la demande — le
        // completionItem/resolve du protocole y répond (la capacité se
        // vérifie : un serveur muet ne reçoit pas la question), le fil
        // d'interface n'attend jamais, la réponse rend sur le fil de la
        // vue. La réponse RENTRE AU CACHE (le candidat résolu y remplace
        // le sien : les trames suivantes portent la documentation — la
        // résolution ne se paie qu'UNE fois par candidat, jamais à
        // chaque re-filtre de la frappe)
        view.setCompletionDocResolver((entry, answer) -> {
            if (!completionEnabled || session == null
                    || !capabilities.completionResolve()) {
                answer.answer(null, false);
                return;
            }
            synchronized (completionCache) {
                for (int at = 0; at < completionCache.size(); at++) {
                    if (completionCache.get(at).label().equals(entry.name())) {
                        final int slot = at;
                        session.resolveCompletionItem(completionCache.get(at))
                                .whenComplete((resolved, failure) -> {
                                    if (resolved != null && slot < completionCache.size()
                                            && completionCache.get(slot).label()
                                                    .equals(entry.name())) {
                                        completionCache.set(slot, resolved);
                                    }
                                    answer.answer(resolved == null
                                            ? null : resolved.documentation(),
                                            resolved != null
                                                    && resolved.markdownDoc());
                                });
                        return;
                    }
                }
            }
            answer.answer(null, false);
        });
        // l'aide aux paramètres du serveur prime : le cache par
        // curseur, la génération déclasse une réponse du texte
        // d'avant (un changement de document ou de curseur). LES
        // SURCHARGES ENTIÈRES y vivent (D-P6 du rapport) : chaque
        // signature avec ses paramètres documentés, l'active y naît
        // — les autres se parcourent au sélecteur de la carte
        view.setSignatureProvider(caret -> {
            if (!signatureEnabled || session == null) {
                return null;
            }
            if (caret != signatureCaret) {
                signatureCaret = caret;
                signatureAnswer = null;
                final long generation = signatureGeneration.incrementAndGet();
                TextBuffer buffer = view.getBuffer();
                int line = buffer.lineOfOffset(caret);
                session.signatureHelp(documentUri,
                                new LspPosition(line, caret - buffer.getLineStart(line)))
                        .whenComplete((help, failure) -> {
                            if (help != null && generation == signatureGeneration.get()) {
                                signatureAnswer = helpOf(help);
                                view.post(view::invalidate);
                            }
                        });
            }
            return signatureAnswer;
        });
        // les diagnostics de la ligne : la sévérité du protocole y
        // garde son rang (erreur, avertissement, information)
        view.setDiagnosticProvider(new EditorView.DiagnosticProvider() {
            @Override
            public List<DiagnosticCard.Entry> diagnosticsAt(int line) {
                return diagnosticsOfLine(line);
            }

            @Override
            public boolean diagnosticCovers(int line, int column) {
                if (!diagnosticsEnabled || !isConnected()) {
                    return false;
                }
                // la PLAGE EXACTE du serveur (le tap précis : le doigt
                // sur le souligné, pas sur la ligne — la leçon de la
                // référence)
                for (LspDiagnostic diagnostic : diagnostics) {
                    if (diagnostic.range().start().line() == line
                            && column >= diagnostic.range().start().character() - 1
                            && column <= diagnostic.range().end().character() + 1) {
                        return true;
                    }
                }
                return false;
            }
        });
    }

    /**
     * Les diagnostics du serveur à une ligne (la carte de diagnostic
     * les lit au toucher du marqueur ou du souligné rouge) — chaque
     * entrée porte son message ET les correctifs du serveur qui s'y
     * rattachent (la leçon de la référence : sa popup d'erreur mêle
     * le message et ses correctifs — l'import manquant s'y corrige
     * d'un clic, l'ampoule n'est qu'un autre chemin vers la même
     * liste). La réponse des correctifs arrive asynchrone : la
     * carte ouverte à la ligne se rouvre fraîche à l'arrivée.
     *
     * @param line l'indice de la ligne
     * @return les diagnostics du serveur sur la ligne, jamais null
     */
    private List<DiagnosticCard.Entry> diagnosticsOfLine(int line) {
        if (!diagnosticsEnabled || !isConnected()) {
            return List.of();
        }
        List<DiagnosticCard.Entry> found = new ArrayList<>();
        for (LspDiagnostic diagnostic : diagnostics) {
            if (diagnostic.range().start().line() == line) {
                found.add(new DiagnosticCard.Entry(
                        diagnostic.severity() == 1 ? DiagnosticCard.SEVERITY_ERROR
                                : diagnostic.severity() == 2 ? DiagnosticCard.SEVERITY_WARNING
                                : DiagnosticCard.SEVERITY_INFO,
                        diagnostic.message(),
                        diagnosticFixesOf(line, diagnostic.message())));
            }
        }
        return found;
    }

    /**
     * Les correctifs du serveur pour un diagnostic (la réponse arrive
     * asynchrone — la première lecture rend vide, l'arrivée rouvre la
     * carte ouverte, la trame suivante y porte les correctifs).
     *
     * @param line    la ligne du diagnostic
     * @param message le message du diagnostic (la clef du cache)
     * @return les correctifs du serveur, jamais null
     */
    private List<DiagnosticCard.Fix> diagnosticFixesOf(int line,
            String message) {
        String cacheKey = line + ":" + message;
        List<DiagnosticCard.Fix> cached = diagnosticFixCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        askDiagnosticFixes(line, cacheKey);
        return List.of();
    }

    /**
     * La demande des correctifs d'un diagnostic part au serveur : la
     * réponse y remplit le cache et ROUVRE la carte de diagnostic si
     * elle vit encore sur cette ligne (l'utilisateur lit le message,
     * les correctifs arrivent dessous — la popup de la référence
     * s'habille pareillement en deux temps).
     *
     * @param line     la ligne du diagnostic
     * @param cacheKey la clef du cache (ligne et message)
     */
    private void askDiagnosticFixes(int line, String cacheKey) {
        diagnosticFixCache.put(cacheKey, List.of());
        LspRange range = lineRange(line);
        final long change = documentChangeGeneration.get();
        session.codeActions(uri, range, diagnosticsOf(line))
                .whenComplete((answer, failure) -> view.post(() -> {
                    if (change != documentChangeGeneration.get()) {
                        // le document a changé pendant le vol : les
                        // correctifs parleraient de l'ancien texte, la
                        // carte ne se rouvre pas
                        return;
                    }
                    if (failure != null || answer == null) {
                        return;
                    }
                    List<DiagnosticCard.Fix> fixes = new ArrayList<>();
                    List<ImportBubbleCard.Import> imports = new ArrayList<>();
                    for (LspCodeAction action : answer) {
                        // LA SUGGESTION D'IMPORT se détourne à SA
                        // bulle ici aussi (l'étage des correctifs garde
                        // les correctifs ordinaires — l'import se lit à
                        // la bulle au-dessus du mot)
                        if (action.kind() != null
                                && action.kind().endsWith("import")) {
                            imports.add(new ImportBubbleCard.Import(
                                    action.title(),
                                    () -> view.applyTextEdits(action.edits())));
                        } else {
                            fixes.add(new DiagnosticCard.Fix(action.title(),
                                    () -> view.applyTextEdits(action.edits())));
                        }
                    }
                    if (!imports.isEmpty()) {
                        view.showImportPopup(view.wordAtCaret(), imports);
                    }
                    diagnosticFixCache.put(cacheKey, List.copyOf(fixes));
                    // la carte de diagnostic VIVANTE sur cette ligne se
                    // rouvre fraîche : les correctifs y paraissent
                    if (view.diagnosticCard() != null
                            && view.diagnosticCard().isShown()
                            && view.diagnosticCard().line() == line) {
                        view.openDiagnosticsAtLine(line);
                    }
                }));
    }

    /**
     * Le document part au serveur — la liaison n'envoie que la plage
     * qui a bougé (l'ombre de la dernière synchronisation tient le
     * reste) ; chaque édition le reprogramme : les frappes rapides
     * n'envoient qu'un envoi, le dernier. Le document a changé côté
     * serveur : les complétions reçues décrivent l'ancien texte — le
     * cache se range, la trame suivante redemande ; la signature en
     * vol se périme pareillement (la génération avance).
     */
    /**
     * Change l'état de la connexion : l'écouteur y est appelé sur
     * le fil qui change l'état (la notification ne re-rentre pas
     * dans la façade — l'écouteur d'hôte y fait ce qu'il veut).
     *
     * @param fresh  le nouvel état, jamais {@code null}
     * @param reason la cause lue par l'hôte, {@code null} permise
     */
    /**
     * L'URI de l'espace de travail d'un document : le DOSSIER qui
     * le porte (l'URI du document amputée de son dernier segment —
     * une URI de dossier propre, sans fragment « .. » que les
     * serveurs réels normalisent mal ; la référence y pose
     * l'URI du projet tout droit).
     *
     * @param documentUri l'URI du document
     * @return l'URI du dossier, jamais {@code null}
     */
    private static String rootUriOf(String documentUri) {
        int cut = documentUri.lastIndexOf('/');
        return cut > 0 ? documentUri.substring(0, cut) : documentUri;
    }

    private void moveTo(ConnectionStatus fresh, String reason) {
        ConnectionStatus former = connectionStatus;
        connectionStatus = fresh;
        if (former != fresh && connectionListener != null) {
            connectionListener.onStatusChanged(fresh, reason);
        }
    }

    private void pushDocumentToServer() {
        if (session != null && uri != null && documentOpened
                && connectionStatus == ConnectionStatus.CONNECTED) {
            // la voie du registre : le document synchronisé au SHARED
            // (le didChange du registre — le langage y route), la
            // voie directe garde la sienne
            if (workspace != null && languageId != null) {
                workspace.didChange(languageId, uri,
                        view.getBuffer().toString());
            } else {
                session.didChange(uri, view.getBuffer().toString());
            }
            resetProviders();
            // les plages surlignées vivaient dans le document d'avant :
            // elles se rangent (les positions ne valent plus), la
            // demande amortie en repose des fraîches quand le serveur sait
            view.setServerHighlights(List.of());
            scheduleHighlightSync();
            // la pompe du document suit la synchronisation : les
            // symboles, les indices, les plis, les jetons sémantiques
            // décrivent le document d'APRÈS
            refreshDocumentViews();
            view.post(view::invalidate);
        }
    }

    /** Une édition arrive : l'envoi du document se reprogramme (l'hôte n'a rien à faire). */
    private void scheduleDocumentSync() {
        if (session == null) {
            return;
        }
        view.removeCallbacks(documentSync);
        view.postDelayed(documentSync, DOCUMENT_SYNC_MS);
    }

    /** Les caches et les avis en vol se rangent (le document a changé, la réponse d'avant ne vaut plus). */
    private void resetProviders() {
        completionPrefix = null;
        completionCache.clear();
        signatureGeneration.incrementAndGet();
        signatureCaret = -1;
        signatureAnswer = null;
        // les corrections de la ligne d'avant ne valent plus (les
        // plages du protocole vivent dans le document d'avant)
        codeActionsLine = -1;
        codeActionsCache.clear();
        // les rangées de l'aller au symbole d'avant ne valent plus
        // (la réponse fraîche les reposera)
        gotoRows = List.of();
        gotoAsked = false;
        // les symboles d'avant ne valent plus non plus (la pompe du
        // document reposera les frais)
        documentViewsGeneration.incrementAndGet();
        symbols = List.of();
    }

    /**
     * La pompe du document : les quatre fonctions du document — les
     * symboles (la table des matières), les indices (l'étiquette au
     * bout des lignes), les plis (les régions repliables), les jetons
     * sémantiques (la coloration du serveur) — se redemandent à
     * chaque synchronisation (elles décrivent le document, pas le
     * curseur) ; la génération avance, les réponses d'avant se
     * périment à l'arrivée.
     */
    private void refreshDocumentViews() {
        if (session == null || uri == null) {
            return;
        }
        final long generation = documentViewsGeneration.incrementAndGet();
        if (capabilities.documentSymbol()) {
            session.documentSymbol(uri).whenComplete((answer, failure) ->
                    view.post(() -> {
                        if (generation == documentViewsGeneration.get()) {
                            symbols = answer == null ? List.of() : answer;
                            view.post(view::invalidate);
                        }
                    }));
        }
        if (capabilities.inlayHint()) {
            session.inlayHints(uri, wholeDocumentRange()).whenComplete((answer, failure) ->
                    view.post(() -> {
                        if (generation == documentViewsGeneration.get()) {
                            view.setInlayHints(answer);
                        }
                    }));
        }
        if (capabilities.foldingRange()) {
            session.foldingRanges(uri).whenComplete((answer, failure) ->
                    view.post(() -> {
                        if (generation == documentViewsGeneration.get()) {
                            view.setServerFoldRegions(answer);
                        }
                    }));
        }
        if (capabilities.semanticTokens()) {
            session.semanticTokens(uri).whenComplete((answer, failure) ->
                    view.post(() -> {
                        if (generation == documentViewsGeneration.get()) {
                            view.setServerSemantics(answer);
                        }
                    }));
        }
    }

    /**
     * La plage du document entier (la question des indices — le
     * protocole y interroge une plage, la façade y pose tout).
     *
     * @return la plage du document entier
     */
    private LspRange wholeDocumentRange() {
        TextBuffer buffer = view.getBuffer();
        return new LspRange(
                LspPosition.ofOffset(buffer, 0),
                LspPosition.ofOffset(buffer, buffer.length()));
    }

    /**
     * La carte des symboles du document : l'ampoule y ouvre la table
     * des matières — chaque rangée porte le nom et sa ligne, le
     * choix y fait sauter le curseur à la sélection du symbole (la
     * façon du panneau de structure de l'atelier).
     */
    private void showSymbolsCard() {
        if (symbols.isEmpty()) {
            sayAtCaret("aucun symbole (le serveur n'a rien dit)");
            return;
        }
        // LA TABLE DES MATIÈRES DÉDIÉE (la structure du fichier — le détournement d'avant,
        // R-D : la structure vivait DANS la carte d'actions rapides,
        // « nom · ligne N » sans icône ni filtre, mêlée aux actions) :
        // chaque symbole y vit en rangée — l'icône de SA nature (les
        // glyphes partagés de la complétion), le symbole COURANT au
        // gras, le FILTRE à la frappe (la carte reçoit le clavier,
        // le tampon ne bouge pas)
        List<StructureCard.Symbol> rows = new ArrayList<>();
        TextBuffer buffer = view.getBuffer();
        for (LspDocumentSymbol symbol : symbols) {
            if (symbol == null || symbol.name() == null) {
                continue;
            }
            int line = buffer.lineOfOffset(Math.min(Math.max(0,
                    offsetOf(buffer, symbol.selectionRange().start())),
                    buffer.length()));
            rows.add(new StructureCard.Symbol(symbol.name(),
                    kindOf(symbol.kind()), line,
                    () -> view.caretTo(offsetOf(view.getBuffer(),
                            symbol.selectionRange().start()))));
        }
        view.showStructurePopup(rows);
    }
}
