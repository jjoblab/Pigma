package jo.pigma.lsp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.lsp4j.ClientCapabilities;
import org.eclipse.lsp4j.ClientInfo;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionContext;
import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DefinitionParams;
import org.eclipse.lsp4j.DocumentFormattingParams;
import org.eclipse.lsp4j.DocumentHighlight;
import org.eclipse.lsp4j.DocumentHighlightParams;
import org.eclipse.lsp4j.DocumentSymbolParams;
import org.eclipse.lsp4j.FoldingRange;
import org.eclipse.lsp4j.FoldingRangeRequestParams;
import org.eclipse.lsp4j.FormattingOptions;
import org.eclipse.lsp4j.InlayHint;
import org.eclipse.lsp4j.InlayHintParams;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.InitializedParams;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.MessageActionItem;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PrepareRenameDefaultBehavior;
import org.eclipse.lsp4j.PrepareRenameParams;
import org.eclipse.lsp4j.PrepareRenameResult;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.ReferenceContext;
import org.eclipse.lsp4j.ReferenceParams;
import org.eclipse.lsp4j.RenameParams;
import org.eclipse.lsp4j.SemanticTokens;
import org.eclipse.lsp4j.SemanticTokensLegend;
import org.eclipse.lsp4j.SemanticTokensParams;
import org.eclipse.lsp4j.SemanticTokensWithRegistrationOptions;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.ShowMessageRequestParams;
import org.eclipse.lsp4j.SignatureHelpParams;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.TextDocumentSyncKind;
import org.eclipse.lsp4j.TextDocumentSyncOptions;
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either3;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageClientAware;
import org.eclipse.lsp4j.services.LanguageServer;

/**
 * La liaison cliente : lsp4j porte le protocole (les requêtes partent
 * par le proxy du serveur, les réponses y reviennent, le JSON-RPC et
 * gson vivent dessous), la liaison n'ajoute que le vocabulaire du
 * dépôt — les types du domaine, l'écouteur des notifications, la
 * synchronisation du document par delta, l'arrêt propre. Trois
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
         * Le canal de la liaison s'est fermé (le lecteur lsp4j en
         * témoigne : le serveur est mort ou le tube a cassé — le
         * scénario du serveur réel qui tombe juste après sa
         * réponse d'initialisation). L'hôte y range son état et
         * l'écran y dit la liaison morte (le toast de l'app).
         *
         * @param byServer vrai quand la fermeture vient du serveur
         *                  (mort du processus), faux quand la
         *                  liaison se débranche elle-même
         */
        default void onStopped(boolean byServer) {
        }

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

    /**
     * La racine de l'espace de travail de la négociation (les
     * serveurs réels s'y orientent — EmmyLua y lit ses ressources).
     */
    private String rootUri = "file:///";
    /**
     * Les versions des documents ouverts : une par URI, la stricte
     * croissance s'y tient par document (le protocole l'exige par
     * identifiant — un compteur unique partagé faisait redescendre la
     * version d'un document quand un autre en montait, les serveurs
     * y rejettent la notification).
     */
    private final java.util.Map<String, java.util.concurrent.atomic.AtomicInteger>
            documentVersions = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Le genre de synchronisation négocié à l'initialisation : vrai —
     * le serveur déclare l'incrémental, les changements partent par
     * plage ; faux — le serveur veut le texte entier (EmmyLua, le
     * serveur de démonstration du sample) ou ne dit rien (le défaut
     * sûr : la notification sans plage est légale partout).
     */
    private volatile boolean incrementalSync;

    /**
     * Ce que le serveur a déclaré savoir (la négociation de
     * l'initialisation l'a rempli — NONE avant).
     */
    private volatile LspCapabilities capabilities = LspCapabilities.NONE;

    /**
     * La légende des jetons sémantiques du serveur (les genres et les
     * modificateurs, dans l'ordre des indices de la réponse) — la
     * négociation de l'initialisation la remplit.
     */
    private volatile List<String> semanticTokenTypes = List.of();

    /** Les modificateurs de la légende des jetons sémantiques. */
    private volatile List<String> semanticTokenModifiers = List.of();

    /**
     * L'ombre du dernier texte synchronisé par document : la base du
     * delta — la plage d'un changement s'y lit dans les coordonnées du
     * document AVANT le changement (le protocole l'exige).
     */
    private final Map<String, String> documentShadows = new HashMap<>();

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
        java.util.concurrent.Future<Void> listening = clientSide.startListening();
        Launcher<LanguageClient> serverSide = LSPLauncher.createServerLauncher(
                server, serverIn, serverOut);
        serverSide.startListening();
        if (server instanceof LanguageClientAware aware) {
            aware.connect(serverSide.getRemoteProxy());
        }
        LspSession session = new LspSession(clientSide, new ArrayList<>());
        // la mort du canal se voit AUSSI en processus partagé (le
        // harnais y ferme les canaux sous la façade : l'état y
        // apprend la liaison morte — le même droit que le hors
        // processus)
        session.watchReader(listening, listener);
        // l'arrêt ferme d'abord les écritures : chaque lecteur voit la
        // fin de flux (pas de tube cassé), puis les lectures se
        // ferment — les tubes meurent avec la liaison
        session.toClose.add(() -> {
            serverOut.close();
            clientOut.close();
            Thread.sleep(50L);
            clientIn.close();
            serverIn.close();
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
        // LE STDERR SE DRAINE (D8) : un serveur bavard sur son erreur
        // remplit le tuyau, l'écriture s'y bloque, le serveur se fige
        // — le fil de vidange le jette au journal, le processus respire
        Thread stderrDrain = new Thread(() -> {
            try {
                process.getErrorStream().transferTo(System.err);
            } catch (Exception drainFailed) {
                // le processus peut mourir avant le fil : le vidage
                // s'arrête de soi
            }
        }, "pigma-lsp-stderr");
        stderrDrain.setDaemon(true);
        stderrDrain.start();
        // L'ARRÊT EN DEUX TEMPS (D8) : le shutdown/exit du protocole
        // est déjà parti (la fermeture ordonnée de la façade) — le
        // processus reçoit sa grâce, puis le tue-loup borné
        session.toClose.add(() -> {
            try {
                if (!process.waitFor(500L, TimeUnit.MILLISECONDS)) {
                    process.destroy();
                    if (!process.waitFor(500L, TimeUnit.MILLISECONDS)) {
                        process.destroyForcibly();
                    }
                }
            } catch (InterruptedException waitInterrupted) {
                Thread.currentThread().interrupt();
                process.destroyForcibly();
            }
        });
        return session;
    }

    /**
     * La racine de l'espace de travail des négociations qui
     * suivent (l'initialize la porte aux serveurs réels — la
     * référence y pose l'URI du projet ; l'app y pose le DOSSIER
     * de travail du serveur : une URI de dossier PROPRE, sans
     * fragment « .. » — les serveurs réels la normalisent mal).
     *
     * @param uri l'URI racine (un dossier), jamais null
     */
    public void setRootUri(String uri) {
        rootUri = Objects.requireNonNull(uri);
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
        java.util.concurrent.Future<Void> listening = launcher.startListening();
        LspSession session = new LspSession(launcher, new ArrayList<>());
        // la mort du canal se voit : le lecteur lsp4j finit quand le
        // flux d'entrée se ferme (le processus meurt, le tube casse)
        // — l'écouteur y apprend la liaison morte, la façade y
        // arrête ses envois (la référence fait pareil : la lecture
        // finie y propage l'état)
        session.watchReader(listening, listener);
        // l'écriture se ferme la première (le serveur voit la fin de
        // flux), la lecture ensuite — notre lecteur ne subit pas de
        // tube cassé
        session.toClose.add(() -> {
            output.close();
            Thread.sleep(50L);
            input.close();
        });
        return session;
    }

    /**
     * L'ouverture du protocole : la négociation des capacités, puis la
     * notification {@code initialized} qui la conclut — la poignée de
     * main complète du protocole (les serveurs réels l'exigent : sans
     * elle, EmmyLua refuse le premier document). La réponse porte le
     * {@link InitializeResult} de lsp4j (le client n'impose pas de
     * sous-ensemble).
     *
     * @param clientName le nom du client (le serveur l'affiche)
     * @return le futur des capacités du serveur
     */
    public CompletableFuture<InitializeResult> initialize(String clientName) {
        InitializeParams params = new InitializeParams();
        params.setClientInfo(new ClientInfo(clientName));
        params.setCapabilities(realClientCapabilities());
        // la racine de l'espace de travail et son dossier (la
        // leçon de la référence : EmmyLua s'y oriente — le serveur
        // sans racine ne sait où lire ses ressources). L'URI du
        // document OUVERT y sert de racine (l'espace de travail de
        // l'app : le document y vit)
        params.setRootUri(rootUri);
        org.eclipse.lsp4j.WorkspaceFolder folder =
                new org.eclipse.lsp4j.WorkspaceFolder();
        folder.setUri(rootUri);
        folder.setName("pigma");
        params.setWorkspaceFolders(java.util.List.of(folder));
        return launcher.getRemoteProxy().initialize(params).thenApply(result -> {
            incrementalSync = declaresIncrementalSync(result);
            capabilities = LspCapabilities.fromResult(result);
            captureSemanticLegend(result);
            launcher.getRemoteProxy().initialized(new InitializedParams());
            return result;
        });
    }

    /**
     * Les capacités RÉELLES du client (la leçon de la référence :
     * elle y déclare tout ce que son client sait faire — un
     * {@code ClientCapabilities} vide offense les serveurs réels
     * prudents, qui y refusent de travailler). La nôtre sait : la
     * synchronisation de document (incrémentale et intégrale), le
     * survol, la complétion, l'aide aux paramètres, les
     * diagnostics publiés, le surlignage des occurrences, le
     * renommage, l'aller-à la définition, les références, le
     * formatage, les inlays et les actions de code.
     *
     * @return les capacités déclarées, jamais {@code null}
     */
    private static ClientCapabilities realClientCapabilities() {
        java.util.List<String> markups = java.util.List.of("markdown", "plaintext");
        org.eclipse.lsp4j.TextDocumentClientCapabilities text =
                new org.eclipse.lsp4j.TextDocumentClientCapabilities();
        text.setSynchronization(
                new org.eclipse.lsp4j.SynchronizationCapabilities(true, true, true));
        text.setHover(new org.eclipse.lsp4j.HoverCapabilities(markups, true));
        text.setCompletion(new org.eclipse.lsp4j.CompletionCapabilities(
                new org.eclipse.lsp4j.CompletionItemCapabilities(true)));
        text.setSignatureHelp(new org.eclipse.lsp4j.SignatureHelpCapabilities(
                new org.eclipse.lsp4j.SignatureInformationCapabilities(markups),
                true));
        text.setPublishDiagnostics(
                new org.eclipse.lsp4j.PublishDiagnosticsCapabilities(true));
        text.setDocumentHighlight(
                new org.eclipse.lsp4j.DocumentHighlightCapabilities());
        text.setRename(new org.eclipse.lsp4j.RenameCapabilities(true, true));
        text.setDefinition(new org.eclipse.lsp4j.DefinitionCapabilities());
        text.setReferences(new org.eclipse.lsp4j.ReferencesCapabilities());
        text.setFormatting(new org.eclipse.lsp4j.FormattingCapabilities());
        text.setRangeFormatting(new org.eclipse.lsp4j.RangeFormattingCapabilities());
        text.setCodeAction(new org.eclipse.lsp4j.CodeActionCapabilities());
        text.setInlayHint(new org.eclipse.lsp4j.InlayHintCapabilities());
        org.eclipse.lsp4j.WorkspaceClientCapabilities workspace =
                new org.eclipse.lsp4j.WorkspaceClientCapabilities();
        workspace.setWorkspaceFolders(true);
        workspace.setApplyEdit(true);
        org.eclipse.lsp4j.WindowClientCapabilities window =
                new org.eclipse.lsp4j.WindowClientCapabilities();
        window.setWorkDoneProgress(true);
        return new ClientCapabilities(workspace, text, window, null);
    }

    /**
     * La légende des jetons sémantiques de la réponse (les indices de
     * la réponse s'y traduisent en noms — le décodage s'y nourrit).
     *
     * @param result la réponse d'initialisation
     */
    private void captureSemanticLegend(InitializeResult result) {
        if (result == null || result.getCapabilities() == null) {
            return;
        }
        SemanticTokensWithRegistrationOptions provider =
                result.getCapabilities().getSemanticTokensProvider();
        if (provider == null || provider.getLegend() == null) {
            return;
        }
        SemanticTokensLegend legend = provider.getLegend();
        if (legend.getTokenTypes() != null) {
            semanticTokenTypes = List.copyOf(legend.getTokenTypes());
        }
        if (legend.getTokenModifiers() != null) {
            semanticTokenModifiers = List.copyOf(legend.getTokenModifiers());
        }
    }

    /**
     * Ce que le serveur a déclaré savoir à l'initialisation : les
     * fonctions y se coupent une à une côté façade (demander une
     * fonction que le serveur n'a pas déclarée offense le protocole).
     *
     * @return les capacités négociées, NONE avant l'initialisation
     */
    public LspCapabilities capabilities() {
        return capabilities;
    }

    /**
     * Le serveur déclare-t-il la synchronisation incrémentale ? Les
     * deux formes du protocole s'y lisent (l'entier ou les options) ;
     * l'absence de déclaration vaut non (le texte entier part, la
     * notification sans plage reste légale pour tout serveur).
     *
     * @param result la réponse d'initialisation
     * @return vrai si le serveur accepte les changements par plage
     */
    private static boolean declaresIncrementalSync(InitializeResult result) {
        ServerCapabilities capabilities = result == null ? null : result.getCapabilities();
        if (capabilities == null) {
            return false;
        }
        Either<TextDocumentSyncKind, TextDocumentSyncOptions> sync =
                capabilities.getTextDocumentSync();
        if (sync == null) {
            return false;
        }
        if (sync.isLeft()) {
            return sync.getLeft() == TextDocumentSyncKind.Incremental;
        }
        TextDocumentSyncOptions options = sync.getRight();
        return options != null && options.getChange() == TextDocumentSyncKind.Incremental;
    }

    /**
     * Annonce un document au serveur : le texte entier, le langage
     * déclaré. Le serveur publiera ses diagnostics dessus. L'ombre de
     * synchronisation naît ici — les changements suivants s'y
     * différencieront.
     *
     * @param uri        l'identifiant du document (son URI)
     * @param text       le texte initial
     * @param languageId le langage déclaré
     */
    public void didOpen(String uri, String text, String languageId) {
        documentShadows.put(uri, text);
        documentVersions.put(uri, new AtomicInteger());
        launcher.getRemoteProxy().getTextDocumentService().didOpen(
                new DidOpenTextDocumentParams(
                        new TextDocumentItem(uri, languageId, 1, text)));
    }

    /**
     * Annonce la fermeture d'un document (le protocole y range son
     * texte : l'espace de travail partagé ferme le document au
     * dernier lecteur parti — le serveur vit tant qu'un document
     * s'y lit). L'ombre de synchronisation s'en va : une réouverture
     * y repartira du texte entier.
     *
     * @param uri l'identifiant du document fermé
     */
    public void didClose(String uri) {
        documentShadows.remove(uri);
        documentVersions.remove(uri);
        try {
            launcher.getRemoteProxy().getTextDocumentService().didClose(
                    new DidCloseTextDocumentParams(
                            new TextDocumentIdentifier(uri)));
        } catch (Exception sendFailed) {
            // le canal peut être déjà mort : la fermeture locale
            // n'en dépend pas
        }
    }

    /**
     * Annonce un changement. Le genre suit la négociation : le serveur
     * incrémental reçoit la plage remplacée (préfixe et suffixe communs
     * — sur un document d'un million de caractères, quelques octets le
     * remplacent), le serveur intégral ou silencieux reçoit le texte
     * entier (la notification sans plage, légale partout — un serveur
     * intégral qui reçoit une plage désynchronise : EmmyLua y perd son
     * document). La liaison garde l'ombre du dernier texte synchronisé
     * dans les deux cas — la plage s'y différencie. Sans ombre (un
     * changement sans ouverture), le texte entier repart (le repli du
     * protocole) ; sans écart, rien ne part du tout.
     *
     * @param uri  le document changé
     * @param text le texte complet après le changement
     */
    public void didChange(String uri, String text) {
        String shadow = documentShadows.put(uri, text);
        if (shadow != null && shadow.equals(text)) {
            // l'écart net des événements amortis est nul : le serveur
            // tient déjà le document exact, la version ne bouge pas
            return;
        }
        TextDocumentContentChangeEvent change = new TextDocumentContentChangeEvent();
        if (incrementalSync && shadow != null) {
            // la plage de l'écart dans les coordonnées de l'ombre (le
            // document AVANT le changement — le protocole l'exige),
            // le texte inséré entre préfixe et suffixe communs
            int prefix = commonPrefix(shadow, text);
            int suffix = commonSuffix(shadow, text, prefix);
            change.setRange(new Range(positionIn(shadow, prefix),
                    positionIn(shadow, shadow.length() - suffix)));
            change.setText(text.substring(prefix, text.length() - suffix));
        } else {
            // le texte entier : ce que le serveur a déclaré vouloir
            change.setText(text);
        }
        DidChangeTextDocumentParams params = new DidChangeTextDocumentParams();
        int version = documentVersions
                .computeIfAbsent(uri, fresh -> new AtomicInteger())
                .incrementAndGet();
        params.setTextDocument(new VersionedTextDocumentIdentifier(uri,
                version));
        params.setContentChanges(List.of(change));
        launcher.getRemoteProxy().getTextDocumentService().didChange(params);
    }

    /**
     * La longueur du préfixe commun aux deux textes.
     *
     * @param before l'ancien texte
     * @param after  le nouveau texte
     * @return le nombre de caractères identiques depuis le début
     */
    private static int commonPrefix(String before, String after) {
        int at = 0;
        int limit = Math.min(before.length(), after.length());
        while (at < limit && before.charAt(at) == after.charAt(at)) {
            at++;
        }
        return at;
    }

    /**
     * La longueur du suffixe commun, bornée pour ne pas recouvrir le
     * préfixe.
     *
     * @param before l'ancien texte
     * @param after  le nouveau texte
     * @param prefix la longueur du préfixe commun
     * @return le nombre de caractères identiques depuis la fin
     */
    private static int commonSuffix(String before, String after, int prefix) {
        int at = 0;
        int limit = Math.min(before.length(), after.length()) - prefix;
        while (at < limit
                && before.charAt(before.length() - 1 - at) == after.charAt(after.length() - 1 - at)) {
            at++;
        }
        return at;
    }

    /**
     * La position du protocole (ligne, colonne) d'un décalage dans un
     * texte donné — les colonnes y sont en unités UTF-16, comme les
     * index de {@link String}.
     *
     * @param text   le texte portant les coordonnées
     * @param offset le décalage, 0 à la longueur du texte
     * @return la position (ligne, colonne)
     */
    private static Position positionIn(String text, int offset) {
        int line = 0;
        int lineStart = 0;
        int at = 0;
        while (at < offset) {
            char sign = text.charAt(at);
            if (sign == '\r' && at + 1 < text.length()
                    && text.charAt(at + 1) == '\n') {
                line++;
                lineStart = at + 2;
                at += 2;
            } else if (sign == '\n' || sign == '\r') {
                line++;
                lineStart = at + 1;
                at++;
            } else {
                at++;
            }
        }
        return new Position(line, offset - lineStart);
    }

    /**
     * Demande l'information flottante à une position : la réponse porte
     * le contenu du survol ET SON BALISAGE (le Markdown des serveurs
     * survit — l'adaptation d'avant le fondait en texte nu) ; le
     * survol muet (vide) dit que le serveur n'en a pas.
     *
     * @param uri      le document interrogé
     * @param position la position du curseur
     * @return le futur du survol, jamais null (le muet : aucun contenu)
     */
    public CompletableFuture<LspHover> hover(String uri, LspPosition position) {
        HoverParams params = new HoverParams(
                new TextDocumentIdentifier(uri), position.toLsp());
        return launcher.getRemoteProxy().getTextDocumentService()
                .hover(params).thenApply(LspHover::fromLsp);
    }

    /**
     * Demande les complétions à une position : la réponse porte les
     * candidats ENTIERS du serveur (la nature, le détail, la
     * documentation, le texte d'insertion, le remplacement, l'ordre,
     * l'obsolescence — le libellé seul ne suffit plus à nourrir la
     * carte) ; les deux formes du protocole — la liste nue ou
     * l'enveloppe — y rentrent.
     *
     * @param uri      le document interrogé
     * @param position la position du curseur
     * @return le futur des candidats, jamais null (vide : aucun)
     */
    public CompletableFuture<List<LspCompletionItem>> completion(
            String uri, LspPosition position) {
        CompletionParams params = new CompletionParams(
                new TextDocumentIdentifier(uri), position.toLsp());
        return launcher.getRemoteProxy().getTextDocumentService()
                .completion(params).thenApply(LspSession::completionItems);
    }

    /**
     * Demande l'aide aux paramètres à une position : la réponse porte
     * la signature active et son paramètre actif (le serveur peut
     * proposer plusieurs signatures — les surcharges — l'active seule
     * revient, la première sans désignation).
     *
     * @param uri      le document interrogé
     * @param position la position du curseur
     * @return le futur de la signature, null quand le serveur ne sait
     *         pas (hors appel, nom inconnu)
     */
    public CompletableFuture<LspSignatureHelp> signatureHelp(String uri,
            LspPosition position) {
        SignatureHelpParams params = new SignatureHelpParams(
                new TextDocumentIdentifier(uri), position.toLsp());
        return launcher.getRemoteProxy().getTextDocumentService()
                .signatureHelp(params).thenApply(LspSignatureHelp::fromLsp);
    }

    /**
     * Demande la définition du symbole à la position : la réponse
     * porte les places pointées (les positions nues ou les liens —
     * les deux formes du protocole y rentrent).
     *
     * @param uri      le document interrogé
     * @param position la position du symbole
     * @return le futur des places, jamais null (vide : le serveur ne
     *         sait pas)
     */
    public CompletableFuture<List<LspLocation>> definition(String uri,
            LspPosition position) {
        DefinitionParams params = new DefinitionParams(
                new TextDocumentIdentifier(uri), position.toLsp());
        return launcher.getRemoteProxy().getTextDocumentService()
                .definition(params).thenApply(LspLocation::fromAnswer);
    }

    /**
     * Demande les références du symbole à la position : chaque usage
     * du symbole dans l'espace de travail y revient.
     *
     * @param uri                le document interrogé
     * @param position           la position du symbole
     * @param includeDeclaration la déclaration compte-t-elle parmi les
     *                           références
     * @return le futur des places, jamais null (vide : aucune)
     */
    public CompletableFuture<List<LspLocation>> references(String uri,
            LspPosition position, boolean includeDeclaration) {
        ReferenceParams params = new ReferenceParams(
                new TextDocumentIdentifier(uri), position.toLsp(),
                new ReferenceContext(includeDeclaration));
        return launcher.getRemoteProxy().getTextDocumentService()
                .references(params).thenApply(LspLocation::fromLsp);
    }

    /**
     * Demande les corrections rapides d'une plage : le contexte porte
     * les diagnostics que l'appelant y tient (les correctifs du
     * protocole s'y nourrissent — la façade y passe ceux du serveur
     * sur la ligne).
     *
     * @param uri        le document interrogé
     * @param range      la plage interrogée (la ligne du curseur)
     * @param context    les diagnostics de la plage côté appelant,
     *                   null pour un contexte vide
     * @return le futur des actions, jamais null (vide : aucune)
     */
    public CompletableFuture<List<LspCodeAction>> codeActions(String uri,
            LspRange range, List<LspDiagnostic> context) {
        CodeActionParams params = new CodeActionParams(
                new TextDocumentIdentifier(uri), range.toLsp(),
                new CodeActionContext(diagnosticsOfContext(context)));
        return launcher.getRemoteProxy().getTextDocumentService()
                .codeAction(params).thenApply(answer -> {
                    List<LspCodeAction> actions = new ArrayList<>();
                    if (answer != null) {
                        for (Either<Command, CodeAction> entry : answer) {
                            LspCodeAction action = LspCodeAction.fromLsp(entry, uri);
                            if (action != null) {
                                actions.add(action);
                            }
                        }
                    }
                    return actions;
                });
    }

    /**
     * Les diagnostics du contexte de correction, la forme lsp4j du
     * protocole.
     *
     * @param context les diagnostics du domaine, null pour aucun
     * @return la liste du protocole, jamais null
     */
    private static List<Diagnostic> diagnosticsOfContext(List<LspDiagnostic> context) {
        List<Diagnostic> ofProtocol = new ArrayList<>();
        if (context != null) {
            for (LspDiagnostic diagnostic : context) {
                if (diagnostic != null) {
                    ofProtocol.add(new Diagnostic(diagnostic.range().toLsp(),
                            diagnostic.message(), severityOf(diagnostic.severity()),
                            "pigma"));
                }
            }
        }
        return ofProtocol;
    }

    /**
     * La sévérité du protocole pour la valeur du domaine (0 : sans
     * sévérité — null au protocole).
     *
     * @param severity la valeur du domaine
     * @return la sévérité lsp4j, null si absente
     */
    private static DiagnosticSeverity severityOf(int severity) {
        return severity >= 1 && severity <= 4
                ? DiagnosticSeverity.forValue(severity) : null;
    }

    /**
     * Demande la plage renommable à la position : le serveur y dit
     * si le symbole se renomme et quelle plage il occupe (le
     * comportement par défaut du protocole — la troisième forme —
     * rend null : renommable, la plage est le mot sous la position).
     *
     * @param uri      le document interrogé
     * @param position la position du symbole
     * @return le futur de la plage renommable, null quand le serveur
     *         refuse l'endroit (ou n'en dit rien)
     */
    public CompletableFuture<LspRange> prepareRename(String uri,
            LspPosition position) {
        PrepareRenameParams params = new PrepareRenameParams(
                new TextDocumentIdentifier(uri), position.toLsp());
        return launcher.getRemoteProxy().getTextDocumentService()
                .prepareRename(params).thenApply(LspSession::prepareRenameRange);
    }

    /**
     * La plage renommable de la réponse du protocole : la plage nue,
     * le résultat détaillé (sa plage compte), null quand le serveur
     * n'en dit rien.
     *
     * @param answer la réponse du protocole
     * @return la plage du domaine, null si absente
     */
    private static LspRange prepareRenameRange(
            Either3<Range, PrepareRenameResult, PrepareRenameDefaultBehavior> answer) {
        if (answer == null) {
            return null;
        }
        if (answer.isFirst() && answer.getFirst() != null) {
            return LspRange.fromLsp(answer.getFirst());
        }
        if (answer.isSecond() && answer.getSecond() != null
                && answer.getSecond().getRange() != null) {
            return LspRange.fromLsp(answer.getSecond().getRange());
        }
        return null;
    }

    /**
     * Demande le renommage du symbole à la position : la réponse
     * porte l'édition de l'espace de travail — les remplacements du
     * document interrogé seuls reviennent (les autres documents se
     * laissent : un éditeur n'en tient qu'un).
     *
     * @param uri      le document interrogé
     * @param position la position du symbole
     * @param newName  le nom posé
     * @return le futur des remplacements du document, jamais null
     *         (vide : le serveur ne sait pas)
     */
    public CompletableFuture<List<LspTextEdit>> rename(String uri,
            LspPosition position, String newName) {
        RenameParams params = new RenameParams(
                new TextDocumentIdentifier(uri), position.toLsp(), newName);
        return launcher.getRemoteProxy().getTextDocumentService()
                .rename(params).thenApply(edit -> LspCodeAction.editsOf(edit, uri));
    }

    /**
     * Demande les occurrences du symbole à la position : les plages
     * surlignées du document (le surlignage du protocole).
     *
     * @param uri      le document interrogé
     * @param position la position du symbole
     * @return le futur des plages, jamais null (vide : aucune)
     */
    public CompletableFuture<List<LspRange>> documentHighlight(String uri,
            LspPosition position) {
        DocumentHighlightParams params = new DocumentHighlightParams(
                new TextDocumentIdentifier(uri), position.toLsp());
        return launcher.getRemoteProxy().getTextDocumentService()
                .documentHighlight(params).thenApply(answer -> {
                    List<LspRange> ranges = new ArrayList<>();
                    if (answer != null) {
                        for (DocumentHighlight highlight : answer) {
                            if (highlight != null && highlight.getRange() != null) {
                                ranges.add(LspRange.fromLsp(highlight.getRange()));
                            }
                        }
                    }
                    return ranges;
                });
    }

    /**
     * Demande le formatage du document entier : la réponse porte les
     * remplacements à poser (les plages du document AVANT le
     * remplacement — du dernier au premier pour les poser).
     *
     * @param uri           le document à formater
     * @param tabSize       la taille d'une tabulation
     * @param insertSpaces  vrai pour des espaces plutôt que des tabulations
     * @return le futur des remplacements, jamais null (vide : rien à
     *         changer, ou le serveur ne sait pas)
     */
    public CompletableFuture<List<LspTextEdit>> formatting(String uri, int tabSize,
            boolean insertSpaces) {
        DocumentFormattingParams params = new DocumentFormattingParams(
                new TextDocumentIdentifier(uri),
                new FormattingOptions(tabSize, insertSpaces));
        return launcher.getRemoteProxy().getTextDocumentService()
                .formatting(params).thenApply(LspSession::textEditsOf);
    }

    /**
     * Demande les symboles du document ({@code textDocument/
     * documentSymbol}) : la table des matières du document — les deux
     * formes du protocole y rentrent (le symbole riche ou
     * l'information à plat).
     *
     * @param uri le document interrogé
     * @return le futur des symboles, jamais null (vide : aucun, ou le
     *         serveur ne sait pas)
     */
    public CompletableFuture<List<LspDocumentSymbol>> documentSymbol(String uri) {
        DocumentSymbolParams params = new DocumentSymbolParams(
                new TextDocumentIdentifier(uri));
        return launcher.getRemoteProxy().getTextDocumentService()
                .documentSymbol(params).thenApply(LspDocumentSymbol::fromAnswer);
    }

    /**
     * Demande les plis du document ({@code textDocument/
     * foldingRange}) : le serveur y dit quelles plages de lignes se
     * replient — le lexer en tient déjà localement, le serveur en
     * porte d'autres (les siens).
     *
     * @param uri le document interrogé
     * @return le futur des plis, jamais null (vide : aucun)
     */
    public CompletableFuture<List<LspFoldingRange>> foldingRanges(String uri) {
        FoldingRangeRequestParams params = new FoldingRangeRequestParams(
                new TextDocumentIdentifier(uri));
        return launcher.getRemoteProxy().getTextDocumentService()
                .foldingRange(params).thenApply(LspFoldingRange::fromLsp);
    }

    /**
     * Demande les indices à poser dans le texte ({@code textDocument/
     * inlayHint}) sur une plage donnée (le document entier à
     * l'ordinaire) : le serveur y rend les étiquettes à dessiner dans
     * la ligne — le texte n'en bouge pas.
     *
     * @param uri   le document interrogé
     * @param range la plage interrogée (le document entier)
     * @return le futur des indices, jamais null (vide : aucun)
     */
    public CompletableFuture<List<LspInlayHint>> inlayHints(String uri, LspRange range) {
        InlayHintParams params = new InlayHintParams(
                new TextDocumentIdentifier(uri), range.toLsp());
        return launcher.getRemoteProxy().getTextDocumentService()
                .inlayHint(params).thenApply(LspInlayHint::fromLsp);
    }

    /**
     * Demande les jetons sémantiques du document ({@code textDocument/
     * semanticTokens/full}) : l'encodage relatif s'y décode à la
     * légende de la négociation (les indices y deviennent des noms —
     * « variable », « function »…).
     *
     * @param uri le document interrogé
     * @return le futur des jetons, jamais null (vide : aucun, ou le
     *         serveur ne sait pas)
     */
    public CompletableFuture<List<LspSemanticToken>> semanticTokens(String uri) {
        SemanticTokensParams params = new SemanticTokensParams(
                new TextDocumentIdentifier(uri));
        return launcher.getRemoteProxy().getTextDocumentService()
                .semanticTokensFull(params).thenApply(answer -> {
                    List<Integer> data = answer == null ? null : answer.getData();
                    return LspSemanticToken.decode(data, semanticTokenTypes,
                            semanticTokenModifiers);
                });
    }

    /**
     * Les remplacements du domaine d'une réponse de remplacements
     * lsp4j.
     *
     * @param answer la réponse du protocole
     * @return les remplacements du domaine, jamais null
     */
    private static List<LspTextEdit> textEditsOf(List<? extends TextEdit> answer) {
        List<LspTextEdit> edits = new ArrayList<>();
        if (answer != null) {
            for (TextEdit edit : answer) {
                if (edit != null) {
                    edits.add(LspTextEdit.fromLsp(edit));
                }
            }
        }
        return edits;
    }

    /**
     * RÉSOUT un candidat de complétion : le {@code completionItem/resolve}
     * du protocole — le serveur y remplit la documentation du candidat
     * (il la retient souvent à la sélection seule). La question part du
     * billet porté par le candidat (l'adaptation le garde à cet effet) ;
     * la réponse REMPLIT les champs vides (la documentation surtout),
     * les champs déjà portés demeurent. Un serveur sans la capacité
     * rend une réponse en erreur : le futur l'y dit, l'appelant y
     * garde son candidat d'avant (la documentation du panneau reste
     * celle du candidat — souvent vide, la carte n'en meurt pas).
     *
     * @param candidate le candidat à résoudre (le billet du protocole y vit)
     * @return le futur du candidat résolu, jamais null (l'original y
     *         revient si le serveur ne dit rien de neuf)
     */
    public CompletableFuture<LspCompletionItem> resolveCompletionItem(
            LspCompletionItem candidate) {
        if (candidate == null) {
            return CompletableFuture.completedFuture(null);
        }
        return launcher.getRemoteProxy().getTextDocumentService()
                .resolveCompletionItem(candidate.raw())
                .thenApply(candidate::merged)
                .exceptionally(failed -> candidate);
    }

    /**
     * La fermeture propre du protocole : la requête d'arrêt (le serveur
     * finit, une seconde d'attente — il peut être mort), la
     * notification de sortie, puis les canaux se ferment (le lecteur
     * de lsp4j s'arrête, le processus meurt s'il y en a un). L'appel
     * ne vit plus au fil d'interface (la façade l'y envoie à son
     * exécuteur de fond) : l'attente y reste, l'écran ne la voit plus.
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
     * Surveille la fin du lecteur : la liaison qui meurt du côté du
     * serveur (le processus s'en va, le tube se ferme) y signale
     * l'écouteur — le débranchement volontaire n'y compte pas
     * ({@code running} descend d'abord, la lecture finit ensuite).
     *
     * @param listening le future du lecteur lsp4j
     * @param listener  l'écouteur de la liaison
     */
    private void watchReader(java.util.concurrent.Future<Void> listening,
            Listener listener) {
        Thread watcher = new Thread(() -> {
            try {
                listening.get();
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            } catch (Exception readerFailed) {
                // le lecteur meurt en erreur : la liaison est morte
            }
            if (running) {
                running = false;
                listener.onStopped(true);
            }
        }, "lsp-reader-watch");
        watcher.setDaemon(true);
        watcher.start();
    }

    /**
     * Les canaux de la liaison meurent sans la poignée de main
     * d'arrêt (le harnais y reproduit le serveur mort de
     * l'appareil : toute requête suivante échoue « flux fermé » —
     * la liaison reste posée, l'écran doit survivre).
     */
    public void harnessCloseStreams() {
        for (AutoCloseable closeable : toClose) {
            try {
                closeable.close();
            } catch (Exception closeFailed) {
                // chaque canal se ferme pour soi
            }
        }
    }

    /**
     * Les candidats ENTIERS de la réponse de complétion : les deux
     * formes du protocole — la liste nue ou l'enveloppe — s'y lisent,
     * chaque entrée y devient un candidat du domaine (le libellé seul
     * ne suffit plus : la nature, le détail, la documentation, le
     * texte d'insertion, l'ordre et l'obsolescence y montent).
     *
     * @param answer la réponse du serveur (liste nue ou enveloppe)
     * @return les candidats, vide si rien
     */
    private static List<LspCompletionItem> completionItems(
            Either<List<CompletionItem>, CompletionList> answer) {
        List<CompletionItem> items = null;
        if (answer != null) {
            if (answer.getLeft() != null) {
                items = answer.getLeft();
            } else if (answer.getRight() != null) {
                items = answer.getRight().getItems();
            }
        }
        if (items == null) {
            return List.of();
        }
        List<LspCompletionItem> candidates = new ArrayList<>(items.size());
        for (CompletionItem item : items) {
            LspCompletionItem candidate = LspCompletionItem.fromLsp(item);
            if (candidate != null) {
                candidates.add(candidate);
            }
        }
        return candidates;
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
