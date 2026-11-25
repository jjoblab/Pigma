package jo.pigma.lsp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.services.LanguageServer;

/**
 * Le registre des serveurs de langage d'un espace de travail (la
 * leçon de la référence : son projet LSP y partage les serveurs entre
 * les éditeurs). Un espace porte un serveur par langage : le premier
 * éditeur qui s'y attache monte la connexion (la poignée de main y
 * court une seule fois), les suivants s'y RACCROCHENT — le document
 * part par {@code didOpen} au serveur vivant, les publications y
 * reviennent DISPATCHÉES par URI (l'avis du document A n'atteint que
 * l'éditeur A). Le dernier éditeur qui se détache ferme le serveur :
 * la durée de vie du processus y suit les documents ouverts, pas les
 * écrans.
 *
 * <p>Un éditeur seul vit exactement comme avant : l'espace n'y change
 * rien (le raccrochage y est invisible — un serveur, un document, un
 * avis). La façade ({@code LspEditor} du module éditeur) reste l'API
 * de l'hôte ; l'espace vit dessous.
 */
public final class LspWorkspace {

    /**
     * La définition d'un serveur : son nom (l'état observable le dit)
     * et sa fabrique (in-process ou par processus — l'espace n'en
     * connaît que le fournisseur).
     */
    public interface ServerDefinition {

        /**
         * Le nom affichable du serveur (le toast, l'état d'écran).
         *
         * @return le nom, jamais {@code null}
         */
        String name();

        /**
         * La liaison neuve du serveur, l'écouteur du registre y pose :
         * les publications du serveur y reviennent (le dispatch par
         * URI les porte aux éditeurs), la mort du canal y ferme la
         * connexion partagée.
         *
         * @param publications l'écouteur des avis (URI, diagnostics)
         * @param death        l'écouteur de la mort du canal
         * @return la liaison montée, jamais {@code null}
         * @throws IOException si la liaison ne monte pas (les tubes)
         */
        LspSession openSession(LspSession.Listener publications,
                java.util.function.Consumer<LspSession> death)
                throws IOException;
    }

    /**
     * L'écouteur du cycle d'un serveur de l'espace (la machine à
     * états de la façade y transporte ses toasts).
     */
    public interface ServerListener {

        /**
         * L'état du serveur vient de changer (CONNECTING à la
         * poignée de main, CONNECTED au document ouvert partagé,
         * DISCONNECTED à la mort du canal ou du processus).
         *
         * @param serverName le nom du serveur, jamais {@code null}
         * @param connected  vrai si le serveur vit (poignée de main
         *                   passée), faux sinon
         * @param reason     la cause de la mort ({@code null} sinon)
         */
        void onServerStateChanged(String serverName, boolean connected, String reason);
    }

    /** L'état d'un serveur de l'espace : la connexion partagée. */
    private static final class Connection {

        /** La définition qui l'a montée (le nom y vit). */
        final ServerDefinition definition;

        /** La liaison au serveur (le handshake y court ou vit). */
        LspSession session;

        /** Les URI des documents ouverts (le compte des usagers). */
        final List<String> openedUris = new ArrayList<>();

        /** Les éditeurs raccrochés (le dernier parti ferme tout). */
        int attachedEditors;

        /** L'état observable : vrai quand la poignée de main a passé. */
        volatile boolean handshaken;

        /** La cause de la mort (l'écouteur la dit). */
        volatile String deadReason;

        Connection(ServerDefinition definition) {
            this.definition = definition;
        }
    }

    /** Les serveurs de l'espace par langage (un par langage). */
    private final Map<String, Connection> servers = new HashMap<>();

    /** Les publications par URI (le dispatch des avis). */
    private final Map<String, List<LspDiagnostic>> diagnosticsByUri =
            new HashMap<>();

    /** Les écouteurs des publications par URI (une liste par URI). */
    private final Map<String, List<DocumentListener>> listeners =
            new HashMap<>();

    /** Les écouteurs du cycle des serveurs (les toasts y naissent). */
    private final List<ServerListener> serverListeners =
            new CopyOnWriteArrayList<>();

    /**
     * L'écouteur des documents (la façade par document s'y pose —
     * les publications du serveur lui reviennent par URI).
     */
    public interface DocumentListener {

        /**
         * Les diagnostics du document viennent d'être publiés (le
         * serveur les rend pour CE document seul).
         *
         * @param uri         l'URI du document, jamais {@code null}
         * @param diagnostics les avis publiés, jamais {@code null}
         */
        void onDiagnostics(String uri, List<LspDiagnostic> diagnostics);
    }

    /**
     * Un éditeur s'attache au serveur de son langage : la connexion
     * existante s'y raccroche (le document part par {@code didOpen}
     * au serveur vivant), la première attache la MONT E (la poignée
     * de main y court une seule fois — les suivants l'attendent).
     *
     * @param languageId l'identifiant du langage au protocole ("java",
     *                   "lua"…), jamais {@code null}
     * @param definition la définition du serveur, jamais {@code null}
     * @param uri        l'URI du document, jamais {@code null}
     * @param text       le texte du document, jamais {@code null}
     * @param listener   l'écouteur des publications de CE document
     * @return la liaison au serveur partagé, {@code null} si la
     *         fabrique a échoué (la cause y reste muette : la façade
     *         y dira « la liaison ne monte pas »)
     */
    public synchronized LspSession attach(String languageId,
            ServerDefinition definition, String uri, String text,
            DocumentListener listener) {
        Connection connection = servers.get(languageId);
        if (connection == null) {
            connection = new Connection(definition);
            servers.put(languageId, connection);
            final Connection mounting = connection;
            try {
                connection.session = definition.openSession(
                        new LspSession.Listener() {
                            @Override
                            public void onDiagnostics(String published,
                                    List<LspDiagnostic> fresh) {
                                publish(published, fresh);
                            }
                        },
                        died -> onSessionDead(mounting.session));
            } catch (IOException noPipe) {
                // la fabrique a échoué : le serveur n'existe pas (le
                // langage n'y garde aucune trace — un nouvel essai y
                // repartira propre)
                servers.remove(languageId);
                return null;
            }
            // la connexion naît : la poignée de main y court (les
            // documents des attacheurs suivants partiront APRÈS
            final Connection mounted = connection;
            mounted.session.setRootUri(rootOf(uri));
            CompletableFuture<InitializeResult> handshake =
                    mounted.session.initialize("pigma");
            mounted.handshaken = false;
            handshake.whenComplete((result, failure) -> {
                final Connection died = mounted;
                if (failure != null || result == null) {
                    // la négociation a échoué : la connexion meurt au
                    // départ — les attacheurs y l'apprendront
                    died.deadReason = failure != null
                            ? failure.getMessage() : "aucune réponse";
                    died.session.shutdown();
                    tellServer(died.definition.name(), false,
                            died.deadReason);
                    return;
                }
                died.handshaken = true;
                tellServer(died.definition.name(), true, null);
                // les documents des attenteurs partent (la poignée
                // de main complète les y autorise — la leçon du
                // branchement : un didOpen pendant l'initialize, le
                // serveur réel le rejette)
                openQueued(died);
            });
        }
        connection.attachedEditors++;
        listenersFor(uri).add(listener);
        // le document part au serveur : la poignée de main passée le
        // permet, l'attente sinon la queue (l'avis reviendra par URI)
        synchronized (connection.openedUris) {
            if (connection.handshaken && !connection.openedUris.contains(uri)) {
                connection.openedUris.add(uri);
                connection.session.didOpen(uri, text, languageId);
            } else if (!connection.openedUris.contains(uri)) {
                connection.openedUris.add(uri);
                queueDocument(connection, uri, text, languageId);
            }
        }
        return connection.session;
    }

    /**
     * Le document en queue d'ouverture (la poignée de main pas
     * encore passée — la suite de l'attache l'ouvrira).
     */
    private final Map<Connection, Map<String, String[]>> pending =
            new HashMap<>();

    /**
     * Un document part quand la poignée de main aura passé : la queue
     * le porte jusqu'à la suite du handshake (le texte y vit avec
     * son langage).
     */
    private void queueDocument(Connection connection, String uri,
            String text, String languageId) {
        Map<String, String[]> queue = pending.computeIfAbsent(
                connection, key -> new HashMap<>());
        queue.put(uri, new String[] {text, languageId});
    }

    /** Ouvre les documents en queue (le handshake a passé). */
    private synchronized void openQueued(Connection connection) {
        Map<String, String[]> queue = pending.remove(connection);
        if (queue == null) {
            return;
        }
        for (Map.Entry<String, String[]> entry : queue.entrySet()) {
            connection.session.didOpen(entry.getKey(),
                    entry.getValue()[0], entry.getValue()[1]);
        }
    }

    /**
     * Le texte du document change (le serveur partagé s'y
     * synchronise — la façade y pousse les frappes).
     *
     * @param languageId le langage du document
     * @param uri       l'URI du document
     * @param text      le texte neuf
     */
    public synchronized void didChange(String languageId, String uri,
            String text) {
        Connection connection = servers.get(languageId);
        if (connection == null || !connection.handshaken) {
            return;
        }
        connection.session.didChange(uri, text);
    }

    /**
     * Un éditeur se détache du serveur de son document : le dernier
     * parti ferme le serveur (la durée de vie du processus y suit
     * les documents, pas les écrans), les autres vivent.
     *
     * @param languageId le langage du document
     * @param uri        l'URI du document
     * @param listener   l'écouteur parti
     */
    public synchronized void detach(String languageId, String uri,
            DocumentListener listener) {
        Connection connection = servers.get(languageId);
        if (connection == null) {
            return;
        }
        List<DocumentListener> at = listeners.get(uri);
        if (at != null) {
            at.remove(listener);
        }
        connection.attachedEditors--;
        // le document se ferme au serveur quand plus aucun écouteur
        // ne le lit (le protocole y range son texte)
        if (at == null || at.isEmpty()) {
            diagnosticsByUri.remove(uri);
            if (connection.handshaken && connection.openedUris.remove(uri)
                    && connection.session != null) {
                // didClose au protocole : le serveur range le texte
                connection.session.didClose(uri);
            }
        }
        if (connection.attachedEditors <= 0) {
            // le dernier éditeur parti : le serveur meurt (les
            // ressources du processus s'y rendent)
            servers.remove(languageId);
            pending.remove(connection);
            if (connection.session != null) {
                connection.session.shutdown();
            }
            tellServer(connection.definition.name(), false,
                    connection.deadReason);
        }
    }

    /**
     * Le serveur de la façade est mort (le canal s'est fermé) : la
     * connexion partagée l'apprend — les éditeurs raccrochés y
     * suivent (la façade y porte sa machine à états).
     *
     * @param liaisonMorte la liaison morte
     */
    public synchronized void onSessionDead(LspSession liaisonMorte) {
        for (Map.Entry<String, Connection> entry : servers.entrySet()) {
            Connection connection = entry.getValue();
            if (connection.session == liaisonMorte) {
                connection.handshaken = false;
                connection.deadReason = "le serveur s'est arrêté";
                connection.openedUris.clear();
                tellServer(connection.definition.name(), false,
                        connection.deadReason);
                break;
            }
        }
    }

    /**
     * La publication du serveur (dispatch par URI) : les écouteurs
     * du document y reçoivent seuls l'avis — l'éditeur du document B
     * ne voit JAMAIS celui du document A.
     *
     * @param uri         l'URI du document publié
     * @param diagnostics les avis publiés
     */
    public synchronized void publish(String uri,
            List<LspDiagnostic> diagnostics) {
        diagnosticsByUri.put(uri, List.copyOf(diagnostics));
        List<DocumentListener> at = listeners.get(uri);
        if (at != null) {
            for (DocumentListener listener : at) {
                listener.onDiagnostics(uri, diagnostics);
            }
        }
    }

    /**
     * Les diagnostics courants d'un document (le panneau de l'hôte
     * s'y lit sans attente).
     *
     * @param uri l'URI du document
     * @return les avis publiés, jamais {@code null} (vide : aucun)
     */
    public synchronized List<LspDiagnostic> diagnosticsOf(String uri) {
        return diagnosticsByUri.getOrDefault(uri, List.of());
    }

    /**
     * Un écouteur du cycle des serveurs se pose (les toasts de
     * l'hôte y naissent).
     *
     * @param listener l'écouteur, jamais {@code null}
     */
    public void addServerListener(ServerListener listener) {
        serverListeners.add(listener);
        // l'état courant s'y dit à l'arrivée (le raccrochage d'un
        // serveur déjà vivant : l'événement de sa naissance est
        // parti sans le nouvel abonné)
        for (Connection connection : servers.values()) {
            listener.onServerStateChanged(connection.definition.name(),
                    connection.handshaken, connection.deadReason);
        }
    }

    /** Le compte des serveurs vivants (le harnais y lit l'état). */
    public synchronized int liveServers() {
        int alive = 0;
        for (Connection connection : servers.values()) {
            if (connection.handshaken) {
                alive++;
            }
        }
 return alive;
    }

    /** Le compte des documents partagés par URI (le harnais y lit). */
    public synchronized int sharedDocuments() {
        int opened = 0;
        for (Connection connection : servers.values()) {
            opened += connection.openedUris.size();
        }
        return opened;
    }

    /** La fabrique des listes d'écouteurs par URI. */
    private List<DocumentListener> listenersFor(String uri) {
        return listeners.computeIfAbsent(uri,
                key -> new CopyOnWriteArrayList<>());
    }

    /** Le dit du cycle aux écouteurs (le fil du lecteur y appelle). */
    private void tellServer(String name, boolean connected, String reason) {
        for (ServerListener listener : serverListeners) {
            listener.onServerStateChanged(name, connected, reason);
        }
    }

    /** La racine d'une URI de document (le dossier du serveur). */
    private static String rootOf(String uri) {
        if (uri == null) {
            return "file:///";
        }
        int slash = uri.lastIndexOf('/');
        return slash >= 0 ? uri.substring(0, slash + 1) : uri + "/";
    }

    /**
     * La définition in-process : le serveur vit dans la même
     * machine (lsp4j porte les tubes).
     *
     * @param name   le nom du serveur
     * @param server le serveur déjà construit
     * @return la définition, prête à l'espace
     */
    public static ServerDefinition inProcess(String name,
            Supplier<LanguageServer> server) {
        return new ServerDefinition() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public LspSession openSession(LspSession.Listener publications,
                    java.util.function.Consumer<LspSession> death)
                    throws IOException {
                return LspSession.beside(server.get(),
                        bridging(publications, death));
            }
        };
    }

    /**
     * La définition par processus : l'exécutable vit hors de la
     * machine (les flux standard portent le protocole).
     *
     * @param name    le nom du serveur
     * @param builder le processus du serveur (déjà configuré)
     * @return la définition, prête à l'espace
     */
    public static ServerDefinition ofProcess(String name,
            Supplier<Process> builder) {
        return new ServerDefinition() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public LspSession openSession(LspSession.Listener publications,
                    java.util.function.Consumer<LspSession> death)
                    throws IOException {
                return LspSession.overProcess(builder.get(),
                        bridging(publications, death));
            }
        };
    }

    /**
     * Le pont des fabriques : les publications du serveur partent au
     * dispatch, la mort du canal à la connexion partagée (les deux
     * ensemble — la fabrique n'en porte qu'une).
     */
    private static LspSession.Listener bridging(
            LspSession.Listener publications,
            java.util.function.Consumer<LspSession> death) {
        return new LspSession.Listener() {
            @Override
            public void onStopped(boolean byServer) {
                death.accept(null);
            }

            @Override
            public void onDiagnostics(String published,
                    List<LspDiagnostic> fresh) {
                if (publications != null) {
                    publications.onDiagnostics(published, fresh);
                }
            }
        };
    }
}
