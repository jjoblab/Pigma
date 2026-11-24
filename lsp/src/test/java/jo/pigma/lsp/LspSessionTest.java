package jo.pigma.lsp;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageClientAware;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.TextDocumentService;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * La liaison sur le serveur du même processus : l'ouverture négocie
 * (lsp4j porte la requête et sa réponse), le document ouvert publie
 * ses diagnostics, le survol répond, l'arrêt ferme proprement — et
 * les requêtes concurrentes gardent chacune leur réponse (lsp4j
 * corrèle par identifiant, le test l'éprouve de bout en bout).
 */
public class LspSessionTest {

    /**
     * Le serveur d'essai : il répond à l'initialisation, publie un
     * diagnostic par document ouvert, répond au survol — les autres
     * notifications se taisent (lsp4j les remplit par défaut).
     */
    private static final class TrialServer implements LanguageServer,
            TextDocumentService, LanguageClientAware {

        private LanguageClient client;

        private volatile String text = "";

        @Override
        public void connect(LanguageClient client) {
            this.client = client;
        }

        @Override
        public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
            ServerCapabilities capabilities = new ServerCapabilities();
            capabilities.setHoverProvider(true);
            return CompletableFuture.completedFuture(new InitializeResult(capabilities));
        }

        @Override
        public CompletableFuture<Object> shutdown() {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void exit() {
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
            text = params.getTextDocument().getText();
            publish();
        }

        @Override
        public void didChange(DidChangeTextDocumentParams params) {
            text = params.getContentChanges().get(0).getText();
            publish();
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
                    new Hover(new MarkupContent("plaintext", "mot sous le curseur")));
        }

        private void publish() {
            int unbalanced = 0;
            int depth = 0;
            for (int at = 0; at < text.length(); at++) {
                char c = text.charAt(at);
                if (c == '(') {
                    depth++;
                } else if (c == ')') {
                    if (depth == 0) {
                        unbalanced++;
                    } else {
                        depth--;
                    }
                }
            }
            int total = unbalanced + depth;
            Diagnostic diagnostic = new Diagnostic(
                    new Range(new Position(0, 0), new Position(0, 1)),
                    "parenthèses non appariées : " + total,
                    total == 0 ? DiagnosticSeverity.Information : DiagnosticSeverity.Error,
                    "trial-server");
            client.publishDiagnostics(new PublishDiagnosticsParams(
                    "file:///demo.java", List.of(diagnostic)));
        }
    }

    /** Les diagnostics arrivent à l'écouteur, l'ouverture et le survol répondent. */
    @Test
    public void sessionSpeaksTheProtocol() throws Exception {
        BlockingQueue<List<LspDiagnostic>> received = new ArrayBlockingQueue<>(4);
        LspSession session = LspSession.beside(new TrialServer(), new LspSession.Listener() {
            @Override
            public void onDiagnostics(String uri, List<LspDiagnostic> diagnostics) {
                received.add(diagnostics);
            }
        });

        InitializeResult result = session.initialize("pigma-test").get(5, TimeUnit.SECONDS);
        assertEquals(Boolean.TRUE, result.getCapabilities().getHoverProvider().getLeft());

        session.didOpen("file:///demo.java", "int f() {\n  return 1;\n}\n)\n", "java");
        List<LspDiagnostic> diagnostics = received.poll(5, TimeUnit.SECONDS);
        assertEquals(1, diagnostics.size());
        assertEquals(1, diagnostics.get(0).severity());
        assertTrue(diagnostics.get(0).message().contains("1"));

        String hover = session.hover("file:///demo.java",
                new LspPosition(1, 9)).get(5, TimeUnit.SECONDS);
        assertEquals("mot sous le curseur", hover);

        session.shutdown();
        assertTrue("la liaison s'arrête", !session.isRunning());
    }

    /** Plusieurs requêtes en vol : chacune reçoit SA réponse (la corrélation). */
    @Test
    public void concurrentRequestsKeepTheirAnswers() throws Exception {
        LspSession session = LspSession.beside(new TrialServer(), null);
        session.initialize("pigma-test").get(5, TimeUnit.SECONDS);
        var first = session.hover("file:///a.java", new LspPosition(0, 0));
        var second = session.hover("file:///b.java", new LspPosition(0, 0));
        assertEquals("mot sous le curseur", first.get(5, TimeUnit.SECONDS));
        assertEquals("mot sous le curseur", second.get(5, TimeUnit.SECONDS));
        session.shutdown();
    }
}
