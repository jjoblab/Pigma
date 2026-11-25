package jo.pigma.lsp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.TextDocumentSyncKind;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageClientAware;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.junit.Test;

/**
 * Le registre partagé de l'espace de travail (la leçon de Sora
 * Editor : son projet LSP y partage les serveurs entre les
 * éditeurs). Un serveur d'essai multi-documents y éprouve : DEUX
 * éditeurs attachés au MÊME langage ne montent QU'UN serveur (la
 * poignée de main n'a couru qu'une fois), les documents y partent
 * par didOpen, les publications reviennent DISPATCHÉES par URI, le
 * dernier détaché FERME le serveur.
 */
public class LspWorkspaceTest {

    /** Le serveur d'essai multi-documents : la table URI→texte. */
    private static final class MultiDocServer implements LanguageServer,
            TextDocumentService, LanguageClientAware {

        /** Le client du serveur (les publications y partent). */
        private LanguageClient client;

        /** Le texte PAR URI (le multi-documents du registre). */
        private final java.util.Map<String, String> texts =
                new java.util.concurrent.ConcurrentHashMap<>();

        /** Le compte des poignées de main (UN par serveur, jamais deux). */
        static final AtomicInteger handshakes = new AtomicInteger();

        /** Le journal des textes reçus par URI (le diagnostic s'y lit). */
        static final java.util.List<String> journal =
                java.util.Collections.synchronizedList(new java.util.ArrayList<>());

        @Override
        public void connect(LanguageClient client) {
            this.client = client;
        }

        @Override
        public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
            handshakes.incrementAndGet();
            ServerCapabilities capabilities = new ServerCapabilities();
            capabilities.setHoverProvider(true);
            capabilities.setTextDocumentSync(TextDocumentSyncKind.Full);
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
        public void didSave(
                org.eclipse.lsp4j.DidSaveTextDocumentParams params) {
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
        public void didOpen(
                org.eclipse.lsp4j.DidOpenTextDocumentParams params) {
            String uri = params.getTextDocument().getUri();
            texts.put(uri, params.getTextDocument().getText());
            journal.add("open " + uri + " [" + texts.get(uri) + "]");
            // la publication PAR URI : l'avis compte les parenthèses
            // non appariées du document ouvert (le dispatch s'y
            // éprouve — deux documents, deux comptes)
            publish(uri);
        }

        @Override
        public void didChange(
                org.eclipse.lsp4j.DidChangeTextDocumentParams params) {
            String uri = params.getTextDocument().getUri();
            String text = params.getContentChanges().isEmpty() ? ""
                    : params.getContentChanges().get(0).getText();
            texts.put(uri, text);
            journal.add("change " + uri + " [" + text + "]");
            publish(uri);
        }

        @Override
        public void didClose(
                org.eclipse.lsp4j.DidCloseTextDocumentParams params) {
            texts.remove(params.getTextDocument().getUri());
        }

        /** La publication du compte de parenthèses d'un document. */
        private void publish(String uri) {
            String text = texts.getOrDefault(uri, "");
            journal.add("PUBLIE " + uri + " [" + text + "]");
            int depth = 0;
            int unbalanced = 0;
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
            List<Diagnostic> diagnostics = total == 0 ? List.of() : List.of(
                    new Diagnostic(
                            new Range(new Position(0, 0), new Position(0, 1)),
                            "parenthèses : " + total, DiagnosticSeverity.Error,
                            "multi-doc"));
            client.publishDiagnostics(new PublishDiagnosticsParams(uri, diagnostics));
        }
    }

    /**
     * DEUX éditeurs, UN serveur : la poignée de main n'a couru
     * qu'une fois, les deux documents vivent ouverts, les avis
     * reviennent PAR URI (le document A n'entend que ses avis).
     */
    /**
     * L'avis d'une file : le premier non-nul y vit (les ouvertures à
     * zéro avis se sautent — elles volent quand elles veulent, l'avis
     * porté se reconnaît à sa liste non vide).
     */
    private static String awaitDiagnostics(BlockingQueue<String> queue,
            long timeout, TimeUnit unit) throws InterruptedException {
        long deadline = System.currentTimeMillis() + unit.toMillis(timeout);
        while (System.currentTimeMillis() < deadline) {
            String head = queue.poll(200, TimeUnit.MILLISECONDS);
            if (head != null && head.endsWith(":1")) {
                return head;
            }
        }
        return null;
    }

    /**
     * Le silence d'une file : aucun AVIS (liste non vide) n'y vit
     * dans la fenêtre — les ouvertures vides se tolèrent.
     */
    private static void checkSilence(BlockingQueue<String> queue,
            long windowMillis) throws InterruptedException {
        long deadline = System.currentTimeMillis() + windowMillis;
        while (System.currentTimeMillis() < deadline) {
            String head = queue.poll(100, TimeUnit.MILLISECONDS);
            if (head != null && head.endsWith(":1")) {
                org.junit.Assert.fail("un avis volait : " + head);
            }
        }
    }

    @Test
    public void twoEditorsShareOneServer() throws Exception {
        MultiDocServer.handshakes.set(0);
        LspWorkspace workspace = new LspWorkspace();
        BlockingQueue<String> seenA = new ArrayBlockingQueue<>(8);
        BlockingQueue<String> seenB = new ArrayBlockingQueue<>(8);

        LspWorkspace.DocumentListener editorA = (uri, diagnostics) -> {
            if ("file:///a.java".equals(uri)) {
                seenA.add(uri + ":" + diagnostics.size());
            }
        };
        LspWorkspace.DocumentListener editorB = (uri, diagnostics) -> {
            if ("file:///b.java".equals(uri)) {
                seenB.add(uri + ":" + diagnostics.size());
            }
        };

        // la façade A monte le serveur (le premier attaché)
        LspSession sessionA = workspace.attach("java",
                LspWorkspace.inProcess("essai", MultiDocServer::new),
                "file:///a.java", "int a() { return 1; }\n", editorA);
        assertNotNull(sessionA);
        // la liaison vit : la poignée de main passe
        for (int wait = 0; wait < 100 && workspace.liveServers() == 0; wait++) {
            Thread.sleep(20L);
        }
        assertEquals(1, workspace.liveServers());
        assertEquals(1, MultiDocServer.handshakes.get());

        // la façade B se RACCROCHE : le même serveur, aucune
        // poignée de main de plus (le partage — la leçon de Sora)
        LspSession sessionB = workspace.attach("java",
                LspWorkspace.inProcess("essai", MultiDocServer::new),
                "file:///b.java", "int b() { return 2; }\n", editorB);
        assertNotNull(sessionB);
        assertEquals(sessionA, sessionB);
        assertEquals(1, MultiDocServer.handshakes.get());
        // les deux documents vivent ouverts au serveur partagé
        for (int wait = 0; wait < 100 && workspace.sharedDocuments() < 2; wait++) {
            Thread.sleep(20L);
        }
        assertEquals(2, workspace.sharedDocuments());

        // le didChange de B : l'avis (1 élément) ne part QUE sur
        // l'URI de B — le document A n'entend JAMAIS son avis (le
        // dispatch par URI). Les ouvertures (:0) se tolèrent en vol,
        // l'avis (:1) seul se compte
        workspace.didChange("java", "file:///b.java", "int b() { (((\n");
        String heardB = awaitDiagnostics(seenB, 5, TimeUnit.SECONDS);
        assertNotNull("l'avis de la frappe de B arrive à B", heardB);
        assertTrue(heardB + " devait finir en :1", heardB.endsWith(":1"));
        checkSilence(seenA, 400);

        // le didChange de A : pareil — l'avis ne part que sur l'URI de A
        workspace.didChange("java", "file:///a.java", "int a() { (( )) ((\n");
        String heardA = awaitDiagnostics(seenA, 5, TimeUnit.SECONDS);
        assertNotNull("l'avis du document A arrive à A", heardA);
        assertTrue(heardA + " devait finir en :1", heardA.endsWith(":1"));
        checkSilence(seenB, 400);

        // le détachement : A part, le serveur VIT (B le tient)
        workspace.detach("java", "file:///a.java", editorA);
        Thread.sleep(200L);
        assertEquals(1, workspace.liveServers());

        // le dernier parti (B) ferme le serveur : la durée de vie du
        // processus y suit les documents, pas les écrans
        workspace.detach("java", "file:///b.java", editorB);
        Thread.sleep(400L);
        assertEquals(0, workspace.liveServers());
        assertEquals(0, workspace.sharedDocuments());
    }
}
