package jo.pigma.sample;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionOptions;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidChangeWatchedFilesParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.eclipse.lsp4j.TextDocumentSyncKind;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageClientAware;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;

/**
 * Serveur de démonstration en processus : il parle le sous-ensemble du
 * protocole que le client connaît — l'initialisation rend des
 * capacités, le document ouvert publie les parenthèses non appariées
 * (une lecture lexicale, pas l'analyse de la bibliothèque : le serveur
 * fait semblant d'être extérieur), le survol rend la description des
 * mots-clés et le compte des occurrences. Le reste de l'interface de
 * service se tait — lsp4j le remplit par défaut.
 */
final class DemoLanguageServer implements LanguageServer, TextDocumentService,
        LanguageClientAware {

    /** Les mots-clés que le serveur décrit (le survol s'y éprouve). */
    private static final String[][] KEYWORDS = {
            {"class", "déclare un type : les instances en partent"},
            {"public", "visible hors de son paquet"},
            {"private", "visible de sa classe seule"},
            {"static", "vit sur la classe, pas sur l'instance"},
            {"final", "une seule affectation, aucune substitution"},
            {"int", "entier signé sur 32 bits"},
            {"double", "flottant double précision (IEEE 754)"},
            {"return", "rend la main avec la valeur"},
            {"if", "branche conditionnelle"},
            {"else", "branche par défaut"},
            {"for", "boucle d'itération bornée"},
            {"while", "boucle de condition"},
    };

    private final String uri;

    /** Le client du serveur (le canal de retour — les diagnostics y partent). */
    private volatile LanguageClient client;

    /** Le texte courant du document (la synchronisation intégrale le tient). */
    private volatile String text = "";

    DemoLanguageServer(String uri) {
        this.uri = uri;
    }

    @Override
    public void connect(LanguageClient client) {
        this.client = client;
    }

    @Override
    public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
        ServerCapabilities capabilities = new ServerCapabilities();
        capabilities.setHoverProvider(true);
        capabilities.setCompletionProvider(new CompletionOptions());
        capabilities.setTextDocumentSync(TextDocumentSyncKind.Full);
        return CompletableFuture.completedFuture(new InitializeResult(capabilities));
    }

    @Override
    public CompletableFuture<Object> shutdown() {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void exit() {
        // la liaison ferme les canaux : le lecteur du serveur s'arrête
    }

    @Override
    public TextDocumentService getTextDocumentService() {
        return this;
    }

    @Override
    public WorkspaceService getWorkspaceService() {
        return new WorkspaceService() {
            @Override
            public void didChangeConfiguration(DidChangeConfigurationParams params) {
            }

            @Override
            public void didChangeWatchedFiles(DidChangeWatchedFilesParams params) {
            }
        };
    }

    @Override
    public void didOpen(DidOpenTextDocumentParams params) {
        if (params.getTextDocument() != null
                && params.getTextDocument().getText() != null) {
            text = params.getTextDocument().getText();
        }
        publishDiagnostics();
    }

    @Override
    public void didChange(DidChangeTextDocumentParams params) {
        if (params.getContentChanges() == null) {
            return;
        }
        // le protocole applique les changements dans l'ordre : une
        // plage remplacée (le delta du client), ou le texte entier
        // quand la plage manque (le repli)
        StringBuilder applied = new StringBuilder(text);
        for (TextDocumentContentChangeEvent change : params.getContentChanges()) {
            if (change.getRange() == null) {
                applied = new StringBuilder(change.getText());
                continue;
            }
            int start = offsetOf(applied, change.getRange().getStart());
            int end = offsetOf(applied, change.getRange().getEnd());
            applied.replace(start, end, change.getText());
        }
        text = applied.toString();
        publishDiagnostics();
    }

    /**
     * Le décalage d'une position du protocole dans le texte courant
     * (les colonnes y sont en unités UTF-16, comme les index de
     * {@code String}).
     *
     * @param text     le texte portant les coordonnées
     * @param position la position (ligne, colonne)
     * @return le décalage correspondant
     */
    private static int offsetOf(CharSequence text, Position position) {
        int offset = 0;
        for (int line = 0; line < position.getLine() && offset < text.length(); line++) {
            int jump = indexOfNewline(text, offset);
            if (jump < 0) {
                return text.length();
            }
            offset = jump + 1;
        }
        return Math.min(text.length(), offset + position.getCharacter());
    }

    /** Le décalage du prochain saut de ligne, ou -1. */
    private static int indexOfNewline(CharSequence text, int from) {
        for (int at = from; at < text.length(); at++) {
            if (text.charAt(at) == '\n') {
                return at;
            }
        }
        return -1;
    }

    @Override
    public void didClose(DidCloseTextDocumentParams params) {
        // le document se ferme : rien à retenir
    }

    @Override
    public void didSave(DidSaveTextDocumentParams params) {
        // la sauvegarde ne change rien au diagnostic
    }

    @Override
    public CompletableFuture<Hover> hover(HoverParams params) {
        int line = params.getPosition().getLine();
        int column = params.getPosition().getCharacter();
        String value = describeWordAt(line, column);
        if (value == null) {
            return CompletableFuture.completedFuture(new Hover());
        }
        return CompletableFuture.completedFuture(
                new Hover(new MarkupContent("plaintext", value)));
    }

    @Override
    public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(
            CompletionParams params) {
        String prefix = prefixAt(params.getPosition().getLine(),
                params.getPosition().getCharacter());
        // le vocabulaire du document : les mots distincts qui
        // poursuivent le préfixe, huit au plus, dans l'ordre du texte
        TreeSet<String> matches = new TreeSet<>();
        int at = 0;
        while (at < text.length() && matches.size() < 8) {
            while (at < text.length() && wordBreak(text.charAt(at))) {
                at++;
            }
            int start = at;
            while (at < text.length() && !wordBreak(text.charAt(at))) {
                at++;
            }
            if (at > start) {
                String word = text.substring(start, at);
                if (word.startsWith(prefix) && word.length() > prefix.length()) {
                    matches.add(word);
                }
            }
        }
        List<CompletionItem> items = new ArrayList<>(matches.size());
        for (String word : matches) {
            items.add(new CompletionItem(word));
        }
        return CompletableFuture.completedFuture(Either.forLeft(items));
    }

    /**
     * Le préfixe tapé à une position : les caractères du mot avant la
     * colonne.
     */
    private String prefixAt(int line, int column) {
        int start = 0;
        for (int at = 0; at < text.length() && line > 0; at++) {
            if (text.charAt(at) == '\n') {
                line--;
                start = at + 1;
            }
        }
        int end = text.indexOf('\n', start);
        if (end < 0) {
            end = text.length();
        }
        int caret = start + Math.min(column, end - start);
        int wordStart = caret;
        while (wordStart > start && !wordBreak(text.charAt(wordStart - 1))) {
            wordStart--;
        }
        return text.substring(wordStart, caret);
    }

    /** Les diagnostics du texte courant : les parenthèses non appariées. */
    private void publishDiagnostics() {
        LanguageClient endpoint = client;
        if (endpoint == null) {
            return;
        }
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
        List<Diagnostic> diagnostics;
        if (total == 0) {
            // équilibré : la liste vide (le remplacement du protocole —
            // le panneau dit zéro, pas « une information »)
            diagnostics = List.of();
        } else {
            diagnostics = List.of(new Diagnostic(
                    new Range(new Position(0, 0), new Position(0, 1)),
                    "parenthèses non appariées : " + total
                            + " (compte du serveur de démonstration)",
                    DiagnosticSeverity.Error, "demo-language-server"));
        }
        endpoint.publishDiagnostics(new PublishDiagnosticsParams(uri, diagnostics));
    }

    /**
     * La description du mot à la position : le mot-clé de la table,
     * sinon le compte de ses occurrences dans le document — la réponse
     * d'un serveur qui aurait lu le texte (le nôtre le garde).
     */
    private String describeWordAt(int line, int column) {
        String current = text;
        int start = 0;
        for (int at = 0; at < current.length() && line > 0; at++) {
            if (current.charAt(at) == '\n') {
                line--;
                start = at + 1;
            }
        }
        int end = current.indexOf('\n', start);
        if (end < 0) {
            end = current.length();
        }
        if (column > end - start) {
            column = end - start;
        }
        int wordStart = start + column;
        while (wordStart > start && !wordBreak(current.charAt(wordStart - 1))) {
            wordStart--;
        }
        int wordEnd = start + column;
        while (wordEnd < end && !wordBreak(current.charAt(wordEnd))) {
            wordEnd++;
        }
        if (wordEnd <= wordStart) {
            return null;
        }
        String word = current.substring(wordStart, wordEnd);
        for (String[] keyword : KEYWORDS) {
            if (keyword[0].equals(word)) {
                return "« " + word + " » — " + keyword[1];
            }
        }
        int occurrences = 0;
        int at = current.indexOf(word);
        while (at >= 0) {
            occurrences++;
            at = current.indexOf(word, at + word.length());
        }
        return "« " + word + " » — " + occurrences + " occurrence(s) dans le document";
    }

    private static boolean wordBreak(char c) {
        return !Character.isJavaIdentifierPart(c);
    }
}
