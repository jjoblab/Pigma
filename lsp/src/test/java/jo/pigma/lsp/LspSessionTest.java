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
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionContext;
import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.lsp4j.DefinitionParams;
import org.eclipse.lsp4j.DocumentFormattingParams;
import org.eclipse.lsp4j.DocumentHighlight;
import org.eclipse.lsp4j.DocumentHighlightParams;
import org.eclipse.lsp4j.FormattingOptions;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PrepareRenameDefaultBehavior;
import org.eclipse.lsp4j.PrepareRenameParams;
import org.eclipse.lsp4j.PrepareRenameResult;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.ReferenceContext;
import org.eclipse.lsp4j.ReferenceParams;
import org.eclipse.lsp4j.RenameParams;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.SignatureHelp;
import org.eclipse.lsp4j.SignatureHelpParams;
import org.eclipse.lsp4j.SignatureInformation;
import org.eclipse.lsp4j.ParameterInformation;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.TextDocumentSyncKind;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.jsonrpc.messages.Either3;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageClientAware;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.TextDocumentService;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
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

        /**
         * Le genre de synchronisation déclaré à l'initialisation : la
         * valeur par défaut (incrémental) porte les tests du delta ;
         * null ne déclare rien (le défaut sûr du client) ; Full joue
         * le serveur intégral.
         */
        volatile TextDocumentSyncKind declaredSync = TextDocumentSyncKind.Incremental;

        /** Les notifications de changement reçues (le delta s'y lit). */
        private volatile int changeCount;

        /** La plage de la dernière notification (null : texte entier). */
        volatile Range lastChangeRange;

        /** Vrai : l'aide aux paramètres se tait (le serveur ne sait pas). */
        volatile boolean signatureSilent;

        /** Vrai : l'initialisation déclare toutes les fonctions (l'essai des capacités). */
        volatile boolean declareAll;

        /** Vrai : l'aller-à la définition rend des liens (la seconde forme du protocole). */
        volatile boolean definitionByLink;

        /** Vrai : le renommage rend la carte par URI (l'ancienne forme), sinon les changements de documents. */
        volatile boolean renameByMap;

        /** Le titre de la correction rapide rendue (null : aucune). */
        volatile String quickFixTitle = "Supprimer la parenthèse en trop";

        @Override
        public void connect(LanguageClient client) {
            this.client = client;
        }

        @Override
        public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
            ServerCapabilities capabilities = new ServerCapabilities();
            capabilities.setHoverProvider(true);
            if (declareAll) {
                capabilities.setCompletionProvider(
                        new org.eclipse.lsp4j.CompletionOptions());
                capabilities.setSignatureHelpProvider(
                        new org.eclipse.lsp4j.SignatureHelpOptions());
                capabilities.setDefinitionProvider(true);
                capabilities.setReferencesProvider(true);
                capabilities.setCodeActionProvider(true);
                capabilities.setDocumentHighlightProvider(true);
                capabilities.setDocumentFormattingProvider(true);
                capabilities.setRenameProvider(true);
            }
            if (declaredSync != null) {
                capabilities.setTextDocumentSync(declaredSync);
            }
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
            changeCount++;
            TextDocumentContentChangeEvent single = params.getContentChanges().get(0);
            lastChangeRange = single.getRange();
            // un vrai serveur applique les changements dans l'ordre :
            // la plage remplacée, ou le texte entier sans plage
            StringBuilder applied = new StringBuilder(text);
            for (TextDocumentContentChangeEvent change
                    : params.getContentChanges()) {
                if (change.getRange() == null) {
                    applied = new StringBuilder(change.getText());
                    continue;
                }
                int start = offsetOf(applied, change.getRange().getStart());
                int end = offsetOf(applied, change.getRange().getEnd());
                applied.replace(start, end, change.getText());
            }
            text = applied.toString();
            publish();
        }

        /** Le décalage d'une position (ligne, colonne) dans le texte. */
        private static int offsetOf(CharSequence content, Position position) {
            int offset = 0;
            for (int line = 0; line < position.getLine() && offset < content.length(); line++) {
                int jump = indexOfNewline(content, offset);
                if (jump < 0) {
                    return content.length();
                }
                offset = jump + 1;
            }
            return Math.min(content.length(), offset + position.getCharacter());
        }

        /** Le décalage du prochain saut de ligne, ou -1. */
        private static int indexOfNewline(CharSequence content, int from) {
            for (int at = from; at < content.length(); at++) {
                if (content.charAt(at) == '\n') {
                    return at;
                }
            }
            return -1;
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

        @Override
        public CompletableFuture<SignatureHelp> signatureHelp(
                SignatureHelpParams params) {
            if (signatureSilent) {
                return CompletableFuture.completedFuture(null);
            }
            // deux signatures — l'active est la seconde ; le paramètre
            // actif manque (la première adaptation du domaine le borne)
            SignatureInformation first = new SignatureInformation(
                    "essai(int a)");
            SignatureInformation second = new SignatureInformation(
                    "essai(int a, int b)");
            second.setParameters(List.of(
                    new ParameterInformation("int a"),
                    new ParameterInformation("int b")));
            return CompletableFuture.completedFuture(new SignatureHelp(
                    List.of(first, second), 1, null));
        }

        @Override
        public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>> definition(
                DefinitionParams params) {
            if (definitionByLink) {
                LocationLink link = new LocationLink("file:///demo.java",
                        new Range(new Position(2, 4), new Position(2, 10)),
                        new Range(new Position(2, 4), new Position(2, 10)),
                        null);
                return CompletableFuture.completedFuture(Either.forRight(List.of(link)));
            }
            // deux places : la déclaration dans le document interrogé,
            // une autre dans un AUTRE document (la façade la proposera
            // à l'écouteur de l'hôte)
            Location declaration = new Location("file:///demo.java",
                    new Range(new Position(2, 4), new Position(2, 10)));
            Location elsewhere = new Location("file:///autre.java",
                    new Range(new Position(5, 0), new Position(5, 6)));
            return CompletableFuture.completedFuture(
                    Either.forLeft(List.of(declaration, elsewhere)));
        }

        @Override
        public CompletableFuture<List<? extends Location>> references(
                ReferenceParams params) {
            Location first = new Location("file:///demo.java",
                    new Range(new Position(2, 4), new Position(2, 10)));
            Location second = new Location("file:///demo.java",
                    new Range(new Position(7, 12), new Position(7, 18)));
            boolean withDeclaration = params.getContext().isIncludeDeclaration();
            if (withDeclaration) {
                return CompletableFuture.completedFuture(List.of(first, second));
            }
            return CompletableFuture.completedFuture(List.of(second));
        }

        @Override
        public CompletableFuture<List<Either<org.eclipse.lsp4j.Command, CodeAction>>> codeAction(
                CodeActionParams params) {
            if (quickFixTitle == null) {
                return CompletableFuture.completedFuture(List.of());
            }
            WorkspaceEdit edit = new WorkspaceEdit();
            TextEdit removal = new TextEdit(
                    new Range(new Position(0, 0), new Position(0, 1)), "");
            if (renameByMap) {
                edit.setChanges(java.util.Map.of("file:///demo.java", List.of(removal)));
            } else {
                org.eclipse.lsp4j.TextDocumentEdit documentChange =
                        new org.eclipse.lsp4j.TextDocumentEdit(
                                new org.eclipse.lsp4j.VersionedTextDocumentIdentifier(
                                        "file:///demo.java", 1),
                                List.of(Either.forLeft(removal)));
                edit.setDocumentChanges(List.of(Either.forLeft(documentChange)));
            }
            CodeAction quickFix = new CodeAction(quickFixTitle);
            quickFix.setEdit(edit);
            return CompletableFuture.completedFuture(
                    List.of(Either.forRight(quickFix)));
        }

        @Override
        public CompletableFuture<Either3<Range, PrepareRenameResult, PrepareRenameDefaultBehavior>> prepareRename(
                PrepareRenameParams params) {
            // le mot sous la position se renomme (la plage nue du protocole)
            return CompletableFuture.completedFuture(Either3.forFirst(
                    new Range(new Position(0, 0), new Position(0, 4))));
        }

        @Override
        public CompletableFuture<WorkspaceEdit> rename(RenameParams params) {
            WorkspaceEdit edit = new WorkspaceEdit();
            TextEdit replacement = new TextEdit(
                    new Range(new Position(0, 0), new Position(0, 4)),
                    params.getNewName());
            if (renameByMap) {
                edit.setChanges(java.util.Map.of("file:///demo.java",
                        List.of(replacement)));
            } else {
                org.eclipse.lsp4j.TextDocumentEdit documentChange =
                        new org.eclipse.lsp4j.TextDocumentEdit(
                                new org.eclipse.lsp4j.VersionedTextDocumentIdentifier(
                                        "file:///demo.java", 2),
                                List.of(Either.forLeft(replacement)));
                edit.setDocumentChanges(List.of(Either.forLeft(documentChange)));
            }
            return CompletableFuture.completedFuture(edit);
        }

        @Override
        public CompletableFuture<List<? extends DocumentHighlight>> documentHighlight(
                DocumentHighlightParams params) {
            DocumentHighlight first = new DocumentHighlight(
                    new Range(new Position(0, 0), new Position(0, 4)),
                    org.eclipse.lsp4j.DocumentHighlightKind.Read);
            DocumentHighlight second = new DocumentHighlight(
                    new Range(new Position(3, 8), new Position(3, 12)),
                    org.eclipse.lsp4j.DocumentHighlightKind.Write);
            return CompletableFuture.completedFuture(List.of(first, second));
        }

        @Override
        public CompletableFuture<List<? extends TextEdit>> formatting(
                DocumentFormattingParams params) {
            // le formatage du serveur d'essai : les espaces de fin de
            // ligne s'y coupent
            List<TextEdit> edits = new java.util.ArrayList<>();
            edits.add(new TextEdit(
                    new Range(new Position(1, 6), new Position(1, 9)), ""));
            return CompletableFuture.completedFuture(edits);
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

        LspSignatureHelp signature = session.signatureHelp("file:///demo.java",
                new LspPosition(1, 9)).get(5, TimeUnit.SECONDS);
        assertEquals("la signature active revient", "essai(int a, int b)",
                signature.label());
        assertEquals("le paramètre actif absent vaut le premier", 0,
                signature.activeParameter());

        session.shutdown();
        assertTrue("la liaison s'arrête", !session.isRunning());
    }

    /** Le serveur qui ne sait pas rend null, jamais une signature vide. */
    @Test
    public void signatureHelpAnswersNullWhenTheServerDoesNotKnow() throws Exception {
        TrialServer server = new TrialServer();
        server.signatureSilent = true;
        LspSession session = LspSession.beside(server, null);
        session.initialize("pigma-test").get(5, TimeUnit.SECONDS);
        assertNull("le serveur muet rend null",
                session.signatureHelp("file:///demo.java",
                        new LspPosition(0, 0)).get(5, TimeUnit.SECONDS));
        session.shutdown();
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

    /**
     * Le changement ne porte que la plage remplacée : l'ombre tient le
     * reste — remplacement isolé, remplacement à la suite (l'ombre se
     * met à jour), ajout en fin, suppression au travers des lignes.
     */
    @Test
    public void didChangeSpeaksInDeltas() throws Exception {
        TrialServer server = new TrialServer();
        LspSession session = LspSession.beside(server, null);
        session.initialize("pigma-test").get(5, TimeUnit.SECONDS);
        session.didOpen("file:///demo.java", "ligne1\nligne2\nligne3", "java");
        session.didChange("file:///demo.java", "ligne1\nligneX\nligne3");
        awaitServerText(server, "ligne1\nligneX\nligne3");
        assertEquals("la notification compte", 1, server.changeCount);
        session.didChange("file:///demo.java", "ligne1\nligneXYZ\nligne3");
        awaitServerText(server, "ligne1\nligneXYZ\nligne3");
        assertEquals("le second delta s'applique sur l'ombre mise à jour",
                2, server.changeCount);
        session.didChange("file:///demo.java", "ligne1\nligneXYZ\nligne3\najout");
        awaitServerText(server, "ligne1\nligneXYZ\nligne3\najout");
        assertEquals("l'ajout en fin tient", 3, server.changeCount);
        session.didChange("file:///demo.java", "ligne1\nligneXYZ");
        awaitServerText(server, "ligne1\nligneXYZ");
        assertEquals("la suppression au travers des lignes tient", 4, server.changeCount);
        session.shutdown();
    }

    /** Aucun écart net (les événements amortis s'annulent) : rien ne part. */
    @Test
    public void didChangeSkipsWhenNothingMoved() throws Exception {
        TrialServer server = new TrialServer();
        LspSession session = LspSession.beside(server, null);
        session.initialize("pigma-test").get(5, TimeUnit.SECONDS);
        session.didOpen("file:///demo.java", "abc", "java");
        session.didChange("file:///demo.java", "abc");
        Thread.sleep(300L);
        assertEquals("aucune notification n'est partie", 0, server.changeCount);
        assertEquals("le serveur tient son texte", "abc", server.text);
        session.shutdown();
    }

    /** Sans ouverture, l'ombre manque : le texte entier repart (le repli). */
    @Test
    public void didChangeWithoutOpenSendsTheWholeText() throws Exception {
        TrialServer server = new TrialServer();
        LspSession session = LspSession.beside(server, null);
        session.initialize("pigma-test").get(5, TimeUnit.SECONDS);
        session.didChange("file:///demo.java", "texte entier");
        awaitServerText(server, "texte entier");
        assertEquals(1, server.changeCount);
        session.shutdown();
    }

    /**
     * Le serveur intégral (EmmyLua, la démo du sample) reçoit le texte
     * entier : une plage envoyée à un serveur intégral le
     * désynchroniserait — la négociation tranche.
     */
    @Test
    public void fullSyncServerReceivesTheWholeText() throws Exception {
        TrialServer server = new TrialServer();
        server.declaredSync = TextDocumentSyncKind.Full;
        LspSession session = LspSession.beside(server, null);
        session.initialize("pigma-test").get(5, TimeUnit.SECONDS);
        session.didOpen("file:///demo.java", "ligne1\nligne2\nligne3", "java");
        session.didChange("file:///demo.java", "ligne1\nligneX\nligne3");
        awaitServerText(server, "ligne1\nligneX\nligne3");
        assertEquals("une notification sans plage", 1, server.changeCount);
        assertEquals("la notification ne porte pas de plage",
                null, server.lastChangeRange);
        session.shutdown();
    }

    /**
     * Le serveur silencieux (aucune capacité de synchronisation
     * déclarée) reçoit le texte entier aussi — le défaut sûr : la
     * notification sans plage reste légale pour tout serveur.
     */
    @Test
    public void silentSyncServerReceivesTheWholeText() throws Exception {
        TrialServer server = new TrialServer();
        server.declaredSync = null;
        LspSession session = LspSession.beside(server, null);
        session.initialize("pigma-test").get(5, TimeUnit.SECONDS);
        session.didOpen("file:///demo.java", "abc", "java");
        session.didChange("file:///demo.java", "abd");
        awaitServerText(server, "abd");
        assertEquals("une notification sans plage", 1, server.changeCount);
        assertEquals("la notification ne porte pas de plage",
                null, server.lastChangeRange);
        session.shutdown();
    }

    /**
     * Attend que le serveur d'essai tienne le texte attendu (les
     * notifications traversent les tubes, elles arrivent un peu plus
     * tard que l'envoi).
     */
    private static void awaitServerText(TrialServer server, String expected)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5_000L;
        while (!expected.equals(server.text)) {
            if (System.currentTimeMillis() > deadline) {
                throw new AssertionError("le serveur ne tient pas : " + server.text);
            }
            Thread.sleep(20L);
        }
    }

    /** Les fonctions nouvelles de Sora editor : la négociation des capacités. */
    @Test
    public void capabilitiesFollowTheDeclaration() throws Exception {
        TrialServer server = new TrialServer();
        server.declareAll = true;
        LspSession session = LspSession.beside(server, null);
        LspCapabilities before = session.capabilities();
        assertTrue("la liaison sans initialisation ne sait rien",
                !before.hover() && !before.definition() && !before.rename());
        session.initialize("pigma-test").get(5, TimeUnit.SECONDS);
        LspCapabilities capabilities = session.capabilities();
        assertTrue("le survol déclaré", capabilities.hover());
        assertTrue("la complétion déclarée", capabilities.completion());
        assertTrue("l'aide aux paramètres déclarée", capabilities.signatureHelp());
        assertTrue("la définition déclarée", capabilities.definition());
        assertTrue("les références déclarées", capabilities.references());
        assertTrue("le renommage déclaré", capabilities.rename());
        assertTrue("les corrections déclarées", capabilities.codeAction());
        assertTrue("le surlignage déclaré", capabilities.documentHighlight());
        assertTrue("le formatage déclaré", capabilities.formatting());
        session.shutdown();
        LspSession silent = LspSession.beside(new TrialServer(), null);
        silent.initialize("pigma-test").get(5, TimeUnit.SECONDS);
        assertTrue("le serveur silencieux ne déclare que le survol",
                silent.capabilities().hover() && !silent.capabilities().definition()
                        && !silent.capabilities().formatting());
        silent.shutdown();
    }

    /** L'aller-à la définition : les places des deux formes du protocole. */
    @Test
    public void definitionAnswersThePlaces() throws Exception {
        TrialServer server = new TrialServer();
        LspSession session = LspSession.beside(server, null);
        session.initialize("pigma-test").get(5, TimeUnit.SECONDS);
        List<LspLocation> places = session.definition("file:///demo.java",
                new LspPosition(7, 15)).get(5, TimeUnit.SECONDS);
        assertEquals("deux places rendues", 2, places.size());
        assertEquals("la déclaration vit dans le document",
                "file:///demo.java", places.get(0).uri());
        assertEquals("la plage y suit", 2, places.get(0).range().start().line());
        assertEquals("l'autre document y reste",
                "file:///autre.java", places.get(1).uri());
        // la seconde forme du protocole (les liens) s'y adapte aussi
        server.definitionByLink = true;
        List<LspLocation> linked = session.definition("file:///demo.java",
                new LspPosition(7, 15)).get(5, TimeUnit.SECONDS);
        assertEquals("le lien rend une place", 1, linked.size());
        assertEquals("la cible du lien compte", "file:///demo.java", linked.get(0).uri());
        assertEquals("la plage cible suit", 2, linked.get(0).range().start().line());
        session.shutdown();
    }

    /** Les références : la déclaration compte selon la demande. */
    @Test
    public void referencesRespectTheDeclaration() throws Exception {
        LspSession session = LspSession.beside(new TrialServer(), null);
        session.initialize("pigma-test").get(5, TimeUnit.SECONDS);
        List<LspLocation> withDeclaration = session.references("file:///demo.java",
                new LspPosition(7, 15), true).get(5, TimeUnit.SECONDS);
        assertEquals("la déclaration compte", 2, withDeclaration.size());
        List<LspLocation> withoutDeclaration = session.references("file:///demo.java",
                new LspPosition(7, 15), false).get(5, TimeUnit.SECONDS);
        assertEquals("sans la déclaration, l'usage seul", 1, withoutDeclaration.size());
        assertEquals("la plage de l'usage suit", 7, withoutDeclaration.get(0).range().start().line());
        session.shutdown();
    }

    /** Les corrections rapides : le titre et les remplacements des deux formes. */
    @Test
    public void codeActionsCarryTheEdits() throws Exception {
        TrialServer server = new TrialServer();
        LspSession session = LspSession.beside(server, null);
        session.initialize("pigma-test").get(5, TimeUnit.SECONDS);
        List<LspCodeAction> actions = session.codeActions("file:///demo.java",
                new LspRange(new LspPosition(0, 0), new LspPosition(0, 1)),
                List.of()).get(5, TimeUnit.SECONDS);
        assertEquals("une action rendue", 1, actions.size());
        assertEquals("le titre suit", "Supprimer la parenthèse en trop",
                actions.get(0).title());
        assertEquals("un remplacement porte", 1, actions.get(0).edits().size());
        assertEquals("la plage du remplacement suit",
                new LspPosition(0, 0), actions.get(0).edits().get(0).range().start());
        // l'ancienne forme du protocole (la carte par URI) s'y lit aussi
        server.renameByMap = true;
        List<LspCodeAction> mapped = session.codeActions("file:///demo.java",
                new LspRange(new LspPosition(0, 0), new LspPosition(0, 1)),
                List.of()).get(5, TimeUnit.SECONDS);
        assertEquals("l'action de la carte par URI porte ses remplacements",
                1, mapped.get(0).edits().size());
        session.shutdown();
    }

    /** La préparation du renommage : la plage renommable du protocole. */
    @Test
    public void prepareRenameAnswersTheRange() throws Exception {
        LspSession session = LspSession.beside(new TrialServer(), null);
        session.initialize("pigma-test").get(5, TimeUnit.SECONDS);
        LspRange renamable = session.prepareRename("file:///demo.java",
                new LspPosition(0, 2)).get(5, TimeUnit.SECONDS);
        assertTrue("la plage renommable suit",
                renamable != null && renamable.start().line() == 0
                        && renamable.end().character() == 4);
        session.shutdown();
    }

    /** Le renommage : les remplacements du document, le nom neuf dedans. */
    @Test
    public void renameAnswersTheEdits() throws Exception {
        TrialServer server = new TrialServer();
        LspSession session = LspSession.beside(server, null);
        session.initialize("pigma-test").get(5, TimeUnit.SECONDS);
        List<LspTextEdit> edits = session.rename("file:///demo.java",
                new LspPosition(0, 2), "compte").get(5, TimeUnit.SECONDS);
        assertEquals("un remplacement rendu", 1, edits.size());
        assertEquals("le nom neuf s'y écrit", "compte", edits.get(0).newText());
        assertEquals("la plage renommée suit", 0, edits.get(0).range().start().line());
        server.renameByMap = true;
        List<LspTextEdit> mapped = session.rename("file:///demo.java",
                new LspPosition(0, 2), "compte").get(5, TimeUnit.SECONDS);
        assertEquals("l'ancienne forme du protocole porte pareil",
                "compte", mapped.get(0).newText());
        session.shutdown();
    }

    /** Le surlignage du document : les occurrences du symbole. */
    @Test
    public void documentHighlightAnswersTheRanges() throws Exception {
        LspSession session = LspSession.beside(new TrialServer(), null);
        session.initialize("pigma-test").get(5, TimeUnit.SECONDS);
        List<LspRange> ranges = session.documentHighlight("file:///demo.java",
                new LspPosition(0, 2)).get(5, TimeUnit.SECONDS);
        assertEquals("deux occurrences surlignées", 2, ranges.size());
        assertEquals("la première plage suit", 0, ranges.get(0).start().line());
        assertEquals("la seconde plage suit", 3, ranges.get(1).start().line());
        session.shutdown();
    }

    /** Le formatage : les remplacements du document entier. */
    @Test
    public void formattingAnswersTheEdits() throws Exception {
        LspSession session = LspSession.beside(new TrialServer(), null);
        session.initialize("pigma-test").get(5, TimeUnit.SECONDS);
        List<LspTextEdit> edits = session.formatting("file:///demo.java", 4, true)
                .get(5, TimeUnit.SECONDS);
        assertEquals("un remplacement rendu", 1, edits.size());
        assertEquals("les espaces de fin s'y coupent",
                new LspPosition(1, 6), edits.get(0).range().start());
        assertEquals("le remplacement est vide", "", edits.get(0).newText());
        session.shutdown();
    }
}
