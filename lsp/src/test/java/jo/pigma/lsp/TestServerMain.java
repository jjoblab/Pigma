package jo.pigma.lsp;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageClientAware;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.TextDocumentService;

/**
 * Serveur d'essai en processus : il lit les messages cadrés sur
 * l'entrée standard et répond sur la sortie standard — lsp4j y porte
 * le cadrage (Content-Length) comme un serveur de langage réel. La
 * classe n'a pas de test : elle EST l'instrument du test du
 * raccordement par processus.
 *
 * <p>Le protocole d'essai : {@code initialize} rend des capacités,
 * {@code textDocument/hover} rend un texte fixe, {@code exit} rend la
 * main.</p>
 */
public final class TestServerMain {

    private TestServerMain() {
    }

    /**
     * Le point d'entrée : le lanceur de lsp4j monte le serveur sur
     * l'entrée et la sortie standard, la lecture y vit.
     *
     * @param args aucun argument attendu
     */
    public static void main(String[] args) {
        Server server = new Server();
        org.eclipse.lsp4j.jsonrpc.Launcher<LanguageClient> launcher =
                LSPLauncher.createServerLauncher(server, System.in, System.out);
        server.connect(launcher.getRemoteProxy());
        launcher.startListening();
    }

    /** Le serveur d'essai : capacités, survol, arrêt. */
    static final class Server implements LanguageServer, TextDocumentService,
            LanguageClientAware {

        private LanguageClient client;

        @Override
        public void connect(LanguageClient client) {
            this.client = client;
        }

        @Override
        public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
            ServerCapabilities capabilities = new ServerCapabilities();
            capabilities.setHoverProvider(true);
            // l'incrémental déclaré : ce sous-processus porte le test du
            // delta par processus (les serveurs intégraux reçoivent le
            // texte entier)
            capabilities.setTextDocumentSync(
                    org.eclipse.lsp4j.TextDocumentSyncKind.Incremental);
            return CompletableFuture.completedFuture(new InitializeResult(capabilities));
        }

        @Override
        public CompletableFuture<Object> shutdown() {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void exit() {
            // la sortie standard se ferme à la mort du processus : le
            // lecteur du client s'arrête
        }

        @Override
        public TextDocumentService getTextDocumentService() {
            return this;
        }

        @Override
        public org.eclipse.lsp4j.services.WorkspaceService getWorkspaceService() {
            return new org.eclipse.lsp4j.services.WorkspaceService() {
                @Override
                public void didChangeConfiguration(
                        org.eclipse.lsp4j.DidChangeConfigurationParams params) {
                }

                @Override
                public void didChangeWatchedFiles(
                        org.eclipse.lsp4j.DidChangeWatchedFilesParams params) {
                }
            };
        }

        @Override
        public void didOpen(DidOpenTextDocumentParams params) {
            Diagnostic diagnostic = new Diagnostic(
                    new Range(new Position(0, 0), new Position(0, 1)),
                    "diagnostic du sous-processus",
                    org.eclipse.lsp4j.DiagnosticSeverity.Information,
                    "test-server");
            client.publishDiagnostics(new PublishDiagnosticsParams(
                    params.getTextDocument().getUri(), List.of(diagnostic)));
        }

        @Override
        public void didChange(DidChangeTextDocumentParams params) {
        }

        @Override
        public void didClose(DidCloseTextDocumentParams params) {
        }

        @Override
        public void didSave(DidSaveTextDocumentParams params) {
        }

        @Override
        public CompletableFuture<Hover> hover(HoverParams params) {
            return CompletableFuture.completedFuture(
                    new Hover(new MarkupContent("plaintext", "réponse du sous-processus")));
        }
    }
}
