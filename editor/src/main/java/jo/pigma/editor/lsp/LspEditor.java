package jo.pigma.editor.lsp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.lsp4j.services.LanguageServer;

import jo.pigma.core.TextBuffer;
import jo.pigma.core.TextListener;
import jo.pigma.editor.EditorView;
import jo.pigma.editor.popups.DiagnosticCard;
import jo.pigma.editor.popups.QuickActionCard;
import jo.pigma.editor.selection.SelectionState;
import jo.pigma.lsp.LspCapabilities;
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
    private long requestTimeoutMs = 200L;

    /** Le préfixe du dernier envoi de complétion (la clef du cache). */
    private volatile String completionPrefix;

    /** Les libellés reçus du serveur pour ce préfixe (le fil du lecteur y écrit). */
    private final List<String> completionCache =
            Collections.synchronizedList(new ArrayList<>());

    /** La génération de la demande de signature vivante (le texte change : la réponse en vol se périme). */
    private final AtomicLong signatureGeneration = new AtomicLong();

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
     * Le temps d'attente du survol : au-delà, la réponse lente
     * gardera sa voie — elle remplacera la popup à son arrivée, si
     * la question vit toujours à la même position.
     *
     * @param timeoutMs le temps d'attente en millisecondes
     */
    public void setRequestTimeoutMs(long timeoutMs) {
        requestTimeoutMs = timeoutMs;
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
        session.definition(uri, position).whenComplete((places, failure) ->
                view.post(() -> navigateTo(places, "définition")));
    }

    /**
     * Les références du symbole au curseur : chaque usage y revient —
     * les places du MÊME document se surlignent (la teinte bleue du
     * protocole), la première y fait sauter le curseur ; les places
     * d'un autre document partent à l'écouteur de l'hôte.
     */
    public void requestReferences() {
        if (session == null || !capabilities.references()) {
            return;
        }
        LspPosition position = caretPosition();
        session.references(uri, position, true).whenComplete((places, failure) ->
                view.post(() -> {
                    navigateTo(places, "référence");
                    // toutes les places du document se surlignent :
                    // les usages se lisent d'un coup d'œil
                    if (places != null && !places.isEmpty()) {
                        view.setServerHighlights(rangesOf(places));
                        highlightGeneration.incrementAndGet();
                    }
                }));
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
            session.prepareRename(uri, position).whenComplete((range, failure) ->
                    view.post(() -> {
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
        session.formatting(uri, view.tabSize(), true).whenComplete((edits, failure) ->
                view.post(() -> {
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
     * Les places reçues y naviguent : la première place du MÊME
     * document fait sauter le curseur (borné au texte), les places
     * d'un AUTRE document partent à l'écouteur de l'hôte (il tient
     * plusieurs documents, lui) ; aucune place y dit « non trouvé »
     * — l'aller-à d'Android Studio parle pareil.
     *
     * @param places  les places du serveur, null sans réponse
     * @param quoi    le nom de la chose cherchée (le message le dit)
     */
    private void navigateTo(List<LspLocation> places, String quoi) {
        if (places == null || places.isEmpty()) {
            sayAtCaret("aucune " + quoi + " trouvée");
            return;
        }
        boolean jumped = false;
        List<LspLocation> external = new ArrayList<>();
        String externalUri = null;
        for (LspLocation place : places) {
            if (uri.equals(place.uri())) {
                if (!jumped) {
                    view.caretTo(offsetOf(view.getBuffer(), place.range().start()));
                    jumped = true;
                }
            } else {
                if (externalUri == null) {
                    externalUri = place.uri();
                }
                if (place.uri().equals(externalUri)) {
                    external.add(place);
                }
            }
        }
        if (!jumped && navigationListener != null && externalUri != null) {
            navigationListener.onExternalPlaces(externalUri, List.copyOf(external));
        } else if (!jumped && navigationListener == null && externalUri != null) {
            sayAtCaret("la " + quoi + " vit dans un autre document");
        }
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
        session.rename(uri, position, newName).whenComplete((edits, failure) ->
                view.post(() -> {
                    if (!view.applyTextEdits(edits)) {
                        sayAtCaret("le renommage ne pose rien (dit le serveur)");
                    }
                }));
        return true;
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
        if (capabilities.definition()) {
            actions.add(new QuickActionCard.Entry(
                    "Aller à la définition", this::requestDefinition));
        }
        if (capabilities.references()) {
            actions.add(new QuickActionCard.Entry(
                    "Trouver les références", this::requestReferences));
        }
        if (capabilities.rename()) {
            actions.add(new QuickActionCard.Entry(
                    "Renommer le symbole", this::requestRename));
        }
        if (capabilities.formatting()) {
            actions.add(new QuickActionCard.Entry(
                    "Formater le document", this::formatDocument));
        }
        if (capabilities.documentSymbol()) {
            actions.add(new QuickActionCard.Entry(
                    "Les symboles du document", this::showSymbolsCard));
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
        session.codeActions(uri, range, diagnosticsOf(asked))
                .whenComplete((answer, failure) -> view.post(() -> {
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
                    for (LspCodeAction action : answer) {
                        codeActionsCache.add(new QuickActionCard.Entry(
                                "Correctif : " + action.title(),
                                () -> view.applyTextEdits(action.edits())));
                    }
                    // la carte ouverte à la ligne se rouvre fraîche ;
                    // la carte close y laisse la trame suivante lire
                    if (view.quickActionCard() != null && view.quickActionCard().isShown()
                            && view.quickActionCard().line() == asked) {
                        view.openQuickActionsAtLine(asked);
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
        // le survol du serveur prime : la réponse attend au plus le
        // temps posé ; au-delà, la popup locale tient la place et la
        // réponse lente la remplacera à son arrivée (le survol
        // asynchrone — la voie du serveur réel)
        view.setInfoProvider((line, column, word) -> {
            if (!hoverEnabled || session == null) {
                return null;
            }
            CompletableFuture<String> hover =
                    session.hover(documentUri, new LspPosition(line, column));
            try {
                return hover.get(requestTimeoutMs, TimeUnit.MILLISECONDS);
            } catch (java.util.concurrent.TimeoutException late) {
                hover.whenComplete((answer, failure) -> {
                    if (answer != null && !answer.isEmpty()) {
                        view.post(() -> view.replaceInfoPopup(answer, line, column));
                    }
                });
            } catch (Exception noAnswer) {
                return null;
            }
            return null;
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
                        .whenComplete((words, failure) -> {
                            if (asked.equals(completionPrefix) && words != null) {
                                completionCache.clear();
                                completionCache.addAll(words);
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
            // au fil de la frappe
            List<String> filtered = new ArrayList<>(completionCache.size());
            for (String word : completionCache) {
                if (word.regionMatches(true, 0, prefix, 0, prefix.length())) {
                    filtered.add(word);
                }
            }
            return filtered;
        });
        // l'aide aux paramètres du serveur prime : le cache par
        // curseur, la génération déclasse une réponse du texte
        // d'avant (un changement de document ou de curseur)
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
                                signatureAnswer = new EditorView.SignatureHelp(
                                        help.label(), help.activeParameter());
                                view.post(view::invalidate);
                            }
                        });
            }
            return signatureAnswer;
        });
        // les diagnostics de la ligne : la sévérité du protocole y
        // garde son rang (erreur, avertissement, information)
        view.setDiagnosticProvider(this::diagnosticsOfLine);
    }

    /**
     * Les diagnostics du serveur à une ligne (la carte de diagnostic
     * les lit au toucher du marqueur ou du souligné rouge).
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
                        diagnostic.message()));
            }
        }
        return found;
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
        List<QuickActionCard.Entry> rows = new ArrayList<>();
        for (LspDocumentSymbol symbol : symbols) {
            rows.add(new QuickActionCard.Entry(
                    symbol.name() + " · ligne "
                            + (symbol.selectionRange().start().line() + 1),
                    () -> view.caretTo(offsetOf(view.getBuffer(),
                            symbol.selectionRange().start()))));
        }
        view.openQuickActions(rows);
    }
}
