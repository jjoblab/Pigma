package jo.pigma.lsp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.lsp4j.ClientCapabilities;
import org.eclipse.lsp4j.ClientInfo;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.MarkedString;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.MessageActionItem;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.ShowMessageRequestParams;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageClientAware;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

/**
 * La liaison cliente : lsp4j porte le protocole (les requêtes partent
 * par le proxy du serveur, les réponses y reviennent, le JSON-RPC et
 * gson vivent dessous), la liaison n'ajoute que le vocabulaire du
 * dépôt — les types du domaine, l'écouteur des notifications, la
 * synchronisation intégrale du document, l'arrêt propre. Trois
 * raccordements : {@link #beside(LanguageServer, Listener)} (le
 * serveur dans le même processus — les tubes), {@link #overProcess}
 * (un serveur lancé, le cadrage vit dans lsp4j) et {@link #over}
 * (des flux donnés).
 */
public final class LspSession {

    /**
     * L'écouteur des notifications du serveur : les diagnostics
     * publiés et les messages de journal. Les méthodes courent sur le
     * fil de lecture de lsp4j — l'hôte marshalle vers son fil
     * d'interface.
     */
    public interface Listener {

        /**
         * Le serveur publie les diagnostics d'un document : la liste
         * ENTIÈRE remplace la précédente (le protocole est à
         * remplacement).
         *
         * @param uri         le document concerné
         * @param diagnostics la liste complète, jamais {@code null}
         */
        default void onDiagnostics(String uri, List<LspDiagnostic> diagnostics) {
        }

        /**
         * Le serveur journalise un message.
         *
         * @param message le texte
         */
        default void onLogMessage(String message) {
        }
    }

    /** Le lanceur du client : le proxy du serveur y vit, la lecture y tourne. */
    private final Launcher<LanguageServer> launcher;

    /** Ce que l'arrêt doit fermer (le processus, les tubes — pas les flux d'autrui). */
    private final List<AutoCloseable> toClose;

    /** La version du document (le protocole y retrouve son ordre). */
    private final AtomicInteger documentVersion = new AtomicInteger();

    private volatile boolean running;

    private LspSession(Launcher<LanguageServer> launcher, List<AutoCloseable> toClose) {
        this.launcher = launcher;
        this.toClose = toClose;
        this.running = true;
    }

    /**
     * La liaison au-dessus d'un serveur DU MÊME processus : deux paires
     * de tubes, un lanceur de chaque côté — le serveur reçoit le
     * client par {@link LanguageClientAware#connect} (le raccordement
     * standard de lsp4j), les messages ne quittent jamais la machine.
     *
     * @param server   le serveur de langage (le vôtre, ou un de démonstration)
     * @param listener l'écouteur des notifications, null pour ignorer
     * @return la liaison démarrée
     * @throws IOException si les tubes ne se posent pas
     */
    public static LspSession beside(LanguageServer server, Listener listener)
            throws IOException {
        Objects.requireNonNull(server);
        PipedInputStream clientIn = new PipedInputStream();
        PipedOutputStream serverOut = new PipedOutputStream(clientIn);
        PipedInputStream serverIn = new PipedInputStream();
        PipedOutputStream clientOut = new PipedOutputStream(serverIn);
        Launcher<LanguageServer> clientSide = LSPLauncher.createClientLauncher(
                new ClientEndpoint(listener), clientIn, clientOut);
        clientSide.startListening();
        Launcher<LanguageClient> serverSide = LSPLauncher.createServerLauncher(
                server, serverIn, serverOut);
        serverSide.startListening();
        if (server instanceof LanguageClientAware aware) {
            aware.connect(serverSide.getRemoteProxy());
        }
        LspSession session = new LspSession(clientSide, new ArrayList<>());
        // les tubes se ferment à l'arrêt : les lectures des deux côtés
        // s'arrêtent, le serveur meurt avec la liaison
        session.toClose.add(() -> {
            clientIn.close();
            clientOut.close();
            serverIn.close();
            serverOut.close();
        });
        return session;
    }

    /**
     * La liaison à un serveur lancé : lsp4j cadre les messages
     * (Content-Length) sur l'entrée et la sortie standard du processus.
     *
     * @param process  le serveur démarré, ses canaux vivants
     * @param listener l'écouteur des notifications, null pour ignorer
     * @return la liaison démarrée
     */
    public static LspSession overProcess(Process process, Listener listener) {
        Objects.requireNonNull(process);
        LspSession session = over(process.getInputStream(),
                process.getOutputStream(), listener);
        session.toClose.add(process::destroy);
        return session;
    }

    /**
     * La liaison sur des flux donnés (un serveur branché autrement) :
     * l'arrêt ferme les flux — la lecture de lsp4j s'arrête avec eux.
     *
     * @param input   ce que le serveur écrit
     * @param output  ce que la liaison écrit au serveur
     * @param listener l'écouteur des notifications, null pour ignorer
     * @return la liaison démarrée
     */
    public static LspSession over(InputStream input, OutputStream output, Listener listener) {
        Objects.requireNonNull(input);
        Objects.requireNonNull(output);
        Launcher<LanguageServer> launcher = LSPLauncher.createClientLauncher(
                new ClientEndpoint(listener), input, output);
        launcher.startListening();
        LspSession session = new LspSession(launcher, new ArrayList<>());
        session.toClose.add(() -> {
            input.close();
            output.close();
        });
        return session;
    }

    /**
     * L'ouverture du protocole : la négociation des capacités. La
     * réponse porte le {@link InitializeResult} de lsp4j (le client
     * n'impose pas de sous-ensemble).
     *
     * @param clientName le nom du client (le serveur l'affiche)
     * @return le futur des capacités du serveur
     */
    public CompletableFuture<InitializeResult> initialize(String clientName) {
        InitializeParams params = new InitializeParams();
        params.setClientInfo(new ClientInfo(clientName));
        params.setCapabilities(new ClientCapabilities());
        return launcher.getRemoteProxy().initialize(params);
    }

    /**
     * Annonce un document au serveur : le texte entier, le langage
     * déclaré. Le serveur publiera ses diagnostics dessus.
     *
     * @param uri        l'identifiant du document (son URI)
     * @param text       le texte initial
     * @param languageId le langage déclaré
     */
    public void didOpen(String uri, String text, String languageId) {
        launcher.getRemoteProxy().getTextDocumentService().didOpen(
                new DidOpenTextDocumentParams(
                        new TextDocumentItem(uri, languageId, 1, text)));
    }

    /**
     * Annonce un changement : la synchronisation intégrale — le texte
     * entier repart, le serveur refait son analyse dessus (la version
     * monte, le protocole y retrouve son ordre).
     *
     * @param uri  le document changé
     * @param text le texte complet après le changement
     */
    public void didChange(String uri, String text) {
        DidChangeTextDocumentParams params = new DidChangeTextDocumentParams();
        params.setTextDocument(new VersionedTextDocumentIdentifier(uri,
                documentVersion.incrementAndGet()));
        params.setContentChanges(List.of(new TextDocumentContentChangeEvent(text)));
        launcher.getRemoteProxy().getTextDocumentService().didChange(params);
    }

    /**
     * Demande l'information flottante à une position : la réponse porte
     * le texte de l'information (le contenu « plaintext » du protocole
     * quand le serveur l'envoie), null si le serveur n'en a pas.
     *
     * @param uri      le document interrogé
     * @param position la position du curseur
     * @return le futur du texte d'information
     */
    public CompletableFuture<String> hover(String uri, LspPosition position) {
        HoverParams params = new HoverParams(
                new TextDocumentIdentifier(uri), position.toLsp());
        return launcher.getRemoteProxy().getTextDocumentService()
                .hover(params).thenApply(LspSession::hoverText);
    }

    /**
     * La fermeture propre du protocole : la requête d'arrêt (le serveur
     * finit, une seconde d'attente — il peut être mort), la
     * notification de sortie, puis les canaux se ferment (le lecteur
     * de lsp4j s'arrête, le processus meurt s'il y en a un).
     */
    public void shutdown() {
        running = false;
        try {
            launcher.getRemoteProxy().shutdown().get(1, TimeUnit.SECONDS);
        } catch (Exception waitFailed) {
            // le serveur peut déjà être mort : la fermeture n'en dépend pas
        }
        try {
            launcher.getRemoteProxy().exit();
        } catch (Exception sendFailed) {
            // le canal peut être cassé : les closes suivent quand même
        }
        for (AutoCloseable closeable : toClose) {
            try {
                closeable.close();
            } catch (Exception closeFailed) {
                // chaque canal se ferme pour soi
            }
        }
    }

    /** La liaison vit-elle encore ? (le diagnostic de la liaison) */
    public boolean isRunning() {
        return running;
    }

    /**
     * Le texte d'un survol lsp4j : le contenu riche s'il est là
     * (MarkupContent ou chaîne), sinon la première entrée de la liste
     * historique, null si le serveur ne dit rien.
     */
    private static String hoverText(Hover hover) {
        if (hover == null || hover.getContents() == null) {
            return null;
        }
        Either<List<Either<String, MarkedString>>, MarkupContent>
                contents = hover.getContents();
        if (contents.isRight()) {
            return contents.getRight() == null ? null : contents.getRight().getValue();
        }
        List<Either<String, MarkedString>> left = contents.getLeft();
        if (left != null && !left.isEmpty() && left.get(0) != null && left.get(0).isLeft()) {
            return left.get(0).getLeft();
        }
        return null;
    }

    /**
     * Le bout client du protocole : les notifications du serveur y
     * arrivent (lsp4j les y dispatche), les deux qui concernent ce
     * dépôt vont à l'écouteur, les autres se taisent — le reste de
     * l'interface est par défaut dans lsp4j.
     */
    private static final class ClientEndpoint implements LanguageClient {

        private final Listener listener;

        private ClientEndpoint(Listener listener) {
            this.listener = listener == null ? new Listener() {
            } : listener;
        }

        @Override
        public void telemetryEvent(Object object) {
            // ce dépôt n'écoute pas la télémétrie
        }

        @Override
        public void publishDiagnostics(PublishDiagnosticsParams params) {
            List<LspDiagnostic> diagnostics = new ArrayList<>();
            if (params.getDiagnostics() != null) {
                for (org.eclipse.lsp4j.Diagnostic diagnostic : params.getDiagnostics()) {
                    diagnostics.add(LspDiagnostic.fromLsp(diagnostic));
                }
            }
            listener.onDiagnostics(params.getUri(), diagnostics);
        }

        @Override
        public void showMessage(MessageParams messageParams) {
            // les fenêtres du serveur ne s'ouvrent pas ici
        }

        @Override
        public CompletableFuture<MessageActionItem> showMessageRequest(
                ShowMessageRequestParams requestParams) {
            return CompletableFuture.completedFuture(new MessageActionItem());
        }

        @Override
        public void logMessage(MessageParams messageParams) {
            if (messageParams != null && messageParams.getMessage() != null) {
                listener.onLogMessage(messageParams.getMessage());
            }
        }
    }
}
