package jo.pigma.sample;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionOptions;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionContext;
import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.lsp4j.DefinitionParams;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidChangeWatchedFilesParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.DocumentFormattingParams;
import org.eclipse.lsp4j.DocumentHighlight;
import org.eclipse.lsp4j.DocumentHighlightParams;
import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.DocumentSymbolParams;
import org.eclipse.lsp4j.FoldingRange;
import org.eclipse.lsp4j.FoldingRangeRequestParams;
import org.eclipse.lsp4j.InlayHint;
import org.eclipse.lsp4j.InlayHintParams;
import org.eclipse.lsp4j.SemanticTokens;
import org.eclipse.lsp4j.SemanticTokensLegend;
import org.eclipse.lsp4j.SemanticTokensParams;
import org.eclipse.lsp4j.SemanticTokensWithRegistrationOptions;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.SymbolKind;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverParams;
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
import org.eclipse.lsp4j.RenameOptions;
import org.eclipse.lsp4j.RenameParams;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.SignatureHelp;
import org.eclipse.lsp4j.SignatureHelpOptions;
import org.eclipse.lsp4j.SignatureHelpParams;
import org.eclipse.lsp4j.SignatureInformation;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.TextDocumentSyncKind;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either3;
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
 * mots-clés et le compte des occurrences, l'aide aux paramètres rend
 * la signature de la déclaration du nom appelé — à sa façon : les
 * paramètres d'abord, le type de retour après. Les fonctions de
 * NAVIGATION s'y suivent (la couverture de la référence — l'aller-à
 * la définition, les références, le surlignage des occurrences, le
 * renommage avec sa préparation, les corrections rapides, le
 * formatage) : le mot au curseur y est la brique — sa ligne de
 * déclaration s'y trouve, ses occurrences s'y comptent, ses espaces
 * de fin s'y coupent. Les fonctions du DOCUMENT s'y suivent
 * pareillement (la couverture complétée) : les symboles (la table des
 * matières des déclarations), les plis (les blocs d'accolades), les
 * indices (le compte des usages au bout des déclarations), les
 * jetons sémantiques (chaque mot y porte son genre — la légende y
 * dit leurs noms). Le reste de l'interface de service se tait —
 * lsp4j le remplit par défaut.
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

    /** La légende des jetons sémantiques (les genres que le serveur rend). */
    private static final List<String> TOKEN_TYPES =
            List.of("keyword", "variable", "function", "constant");

    /** Les modificateurs de la légende (le bit de déclaration). */
    private static final List<String> TOKEN_MODIFIERS = List.of("declaration");

    /** Le client du serveur (le canal de retour — les diagnostics y partent). */
    private volatile LanguageClient client;

    /** Le texte courant PAR URI (le serveur multi-documents : l'espace de travail partagé y attache N éditeurs). */
    private final java.util.Map<String, String> texts = new java.util.concurrent.ConcurrentHashMap<>();

    /** Le document demandé par une requête (les fonctions du protocole y reçoivent l'URI — le document s'y lit). */
    private String document = "";

    /**
     * Le serveur générique : les documents s'y attachent par
     * {@code didOpen} (le constructeur historique garde l'URI du
     * document de démo — le document y naît ouvert).
     *
     * @param uri l'URI du document pré-ouvert (null : aucun)
     */
    DemoLanguageServer(String uri) {
        if (uri != null) {
            this.uri = uri;
            document = uri;
        } else {
            this.uri = null;
        }
    }

    /** L'URI du document de démo (null : le serveur générique). */
    private final String uri;

    @Override
    public void connect(LanguageClient client) {
        this.client = client;
    }

    @Override
    public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
        ServerCapabilities capabilities = new ServerCapabilities();
        capabilities.setHoverProvider(true);
        capabilities.setCompletionProvider(new CompletionOptions());
        capabilities.setSignatureHelpProvider(new SignatureHelpOptions());
        capabilities.setTextDocumentSync(TextDocumentSyncKind.Full);
        // la couverture de la référence s'y déclare : l'aller-à, les
        // références, le surlignage, le renommage (avec sa préparation),
        // les corrections, le formatage
        capabilities.setDefinitionProvider(true);
        capabilities.setReferencesProvider(true);
        capabilities.setDocumentHighlightProvider(true);
        RenameOptions rename = new RenameOptions();
        rename.setPrepareProvider(true);
        capabilities.setRenameProvider(rename);
        capabilities.setCodeActionProvider(true);
        capabilities.setDocumentFormattingProvider(true);
        // les fonctions du document s'y déclarent pareillement : les
        // symboles, les plis, les indices, les jetons sémantiques (la
        // légende y dit les noms des genres)
        capabilities.setDocumentSymbolProvider(true);
        // l'aller au symbole s'y déclare pareillement (workspace/symbol —
        // le nom cherché partout)
        capabilities.setWorkspaceSymbolProvider(true);
        capabilities.setFoldingRangeProvider(true);
        capabilities.setInlayHintProvider(true);
        capabilities.setSemanticTokensProvider(
                new SemanticTokensWithRegistrationOptions(
                        new SemanticTokensLegend(TOKEN_TYPES, TOKEN_MODIFIERS)));
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

            @Override
            public java.util.concurrent.CompletableFuture<
                    org.eclipse.lsp4j.jsonrpc.messages.Either<
                            java.util.List<? extends SymbolInformation>,
                            java.util.List<? extends org.eclipse.lsp4j.WorkspaceSymbol>>>
                    symbol(org.eclipse.lsp4j.WorkspaceSymbolParams params) {
                // L'ALLER AU SYMBOLE (l'aller au symbole — workspace/
                // symbol) : chaque déclaration du document s'y rend en
                // information à plat (la forme ordinaire du protocole —
                // sa place y vit entière), la question posée y resserre
                // les noms (la casse ne compte pas)
                String asked = params.getQuery() == null ? "" : params.getQuery();
                String current = currentText();
                java.util.List<SymbolInformation> found = new ArrayList<>();
                for (int[] declaration : declarations()) {
                    String name = current.substring(declaration[0], declaration[1]);
                    if (!asked.isEmpty()
                            && !name.toLowerCase(java.util.Locale.ROOT)
                                    .contains(asked.toLowerCase(java.util.Locale.ROOT))) {
                        continue;
                    }
                    found.add(new SymbolInformation(name,
                            declaration[2] == 1 ? SymbolKind.Method : SymbolKind.Field,
                            new Location(target(),
                                    rangeOf(new int[]{declaration[0], declaration[1]}))));
                }
                return java.util.concurrent.CompletableFuture.completedFuture(
                        org.eclipse.lsp4j.jsonrpc.messages.Either.forLeft(found));
            }
        };
    }

    @Override
    public void didOpen(DidOpenTextDocumentParams params) {
        if (params.getTextDocument() != null
                && params.getTextDocument().getText() != null) {
            String opened = params.getTextDocument().getUri();
            texts.put(opened, params.getTextDocument().getText());
            document = opened;
        }
        publishDiagnostics();
    }

    @Override
    public void didChange(DidChangeTextDocumentParams params) {
        if (params.getContentChanges() == null) {
            return;
        }
        String changed = params.getTextDocument().getUri();
        document = changed;
        // le protocole applique les changements dans l'ordre : une
        // plage remplacée (le delta du client), ou le texte entier
        // quand la plage manque (le repli)
        StringBuilder applied = new StringBuilder(
                texts.getOrDefault(changed, ""));
        for (TextDocumentContentChangeEvent change : params.getContentChanges()) {
            if (change.getRange() == null) {
                applied = new StringBuilder(change.getText());
                continue;
            }
            int start = offsetOf(applied, change.getRange().getStart());
            int end = offsetOf(applied, change.getRange().getEnd());
            applied.replace(start, end, change.getText());
        }
        texts.put(changed, applied.toString());
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
        // le document se ferme : le texte s'y range (l'espace
        // partagé ferme le document au dernier lecteur parti)
        texts.remove(params.getTextDocument().getUri());
    }

    @Override
    public void didSave(DidSaveTextDocumentParams params) {
        // la sauvegarde ne change rien au diagnostic
    }

    /**
     * L'URI visé par une réponse (le document de démo au mode démo,
     * le document demandé au mode générique — le registre partagé y
     * attache N documents).
     */
    private String target() {
        return uri != null ? uri : document;
    }

    /**
     * Le texte d'un document demandé (la table des documents ouverts
     * — le document de démo y naît, les documents du registre partagé
     * s'y attachent par didOpen).
     */
    private String documentOf(String asked) {
        document = asked;
        return texts.getOrDefault(asked, "");
    }

    /**
     * Le texte du document courant (les aides sans URI — la ligne,
     * le mot, les occurrences : le document de la DERNIÈRE requête
     * s'y lit).
     */
    private String currentText() {
        return texts.getOrDefault(document, "");
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
        String current = texts.getOrDefault(
                params.getTextDocument().getUri(), "");
        String prefix = prefixAt(params.getPosition().getLine(),
                params.getPosition().getCharacter());
        // le vocabulaire du document : les mots distincts qui
        // poursuivent le préfixe, huit au plus, dans l'ordre du texte
        TreeSet<String> matches = new TreeSet<>();
        int at = 0;
        while (at < current.length() && matches.size() < 8) {
            while (at < current.length() && wordBreak(current.charAt(at))) {
                at++;
            }
            int start = at;
            while (at < current.length() && !wordBreak(current.charAt(at))) {
                at++;
            }
            if (at > start) {
                String word = current.substring(start, at);
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

    @Override
    public CompletableFuture<SignatureHelp> signatureHelp(SignatureHelpParams params) {
        String current = documentOf(params.getTextDocument().getUri());
        int caret = offsetOf(current, params.getPosition());
        // l'ouvrante non fermée de l'appel courant : en remontant du
        // curseur, les fermantes se consomment ; un finisseur
        // d'instruction avant elle coupe la recherche
        int depth = 0;
        int openParen = -1;
        for (int at = caret - 1; at >= 0; at--) {
            char c = current.charAt(at);
            if (c == ')') {
                depth++;
            } else if (c == '(') {
                if (depth == 0) {
                    openParen = at;
                    break;
                }
                depth--;
            } else if (c == ';' || c == '{' || c == '}') {
                break;
            }
        }
        if (openParen < 0) {
            return CompletableFuture.completedFuture(null);
        }
        // le paramètre actif : les virgules de profondeur nulle entre
        // l'ouvrante de l'appel et le curseur
        int active = 0;
        int nested = 0;
        for (int at = openParen + 1; at < caret; at++) {
            char c = current.charAt(at);
            if (c == '(' || c == '[' || c == '{') {
                nested++;
            } else if (c == ')' || c == ']' || c == '}') {
                nested--;
            } else if (c == ',' && nested == 0) {
                active++;
            }
        }
        SignatureInformation signature = signatureOfCall(openParen);
        if (signature == null) {
            return CompletableFuture.completedFuture(null);
        }
        return CompletableFuture.completedFuture(new SignatureHelp(
                List.of(signature), 0, active));
    }

    @Override
    public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>> definition(
            DefinitionParams params) {
        String word = wordAt(params.getPosition());
        Location declaration = word == null ? null : declarationOf(word);
        if (declaration == null) {
            return CompletableFuture.completedFuture(Either.forLeft(List.of()));
        }
        return CompletableFuture.completedFuture(Either.forLeft(List.of(declaration)));
    }

    @Override
    public CompletableFuture<List<? extends Location>> references(ReferenceParams params) {
        String word = wordAt(params.getPosition());
        List<Location> places = new ArrayList<>();
        if (word != null) {
            for (int[] occurrence : occurrencesOf(word)) {
                places.add(new Location(target(), rangeOf(occurrence)));
            }
        }
        return CompletableFuture.completedFuture(places);
    }

    @Override
    public CompletableFuture<List<? extends DocumentHighlight>> documentHighlight(
            DocumentHighlightParams params) {
        String word = wordAt(params.getPosition());
        List<DocumentHighlight> highlights = new ArrayList<>();
        if (word != null) {
            for (int[] occurrence : occurrencesOf(word)) {
                highlights.add(new DocumentHighlight(rangeOf(occurrence),
                        org.eclipse.lsp4j.DocumentHighlightKind.Text));
            }
        }
        return CompletableFuture.completedFuture(highlights);
    }

    @Override
    public CompletableFuture<Either3<Range, PrepareRenameResult, PrepareRenameDefaultBehavior>> prepareRename(
            PrepareRenameParams params) {
        String word = wordAt(params.getPosition());
        if (word == null) {
            // aucun mot : le symbole ne se renomme pas (le client le dit)
            return CompletableFuture.completedFuture(null);
        }
        int[] bounds = wordBoundsAt(params.getPosition());
        return CompletableFuture.completedFuture(Either3.forFirst(rangeOf(bounds)));
    }

    @Override
    public CompletableFuture<WorkspaceEdit> rename(RenameParams params) {
        String word = wordAt(params.getPosition());
        if (word == null) {
            return CompletableFuture.completedFuture(new WorkspaceEdit());
        }
        List<TextEdit> replacements = new ArrayList<>();
        for (int[] occurrence : occurrencesOf(word)) {
            replacements.add(new TextEdit(rangeOf(occurrence), params.getNewName()));
        }
        WorkspaceEdit edit = new WorkspaceEdit();
        edit.setChanges(java.util.Map.of(target(), replacements));
        return CompletableFuture.completedFuture(edit);
    }

    @Override
    public CompletableFuture<List<Either<org.eclipse.lsp4j.Command, CodeAction>>> codeAction(
            CodeActionParams params) {
        String current = documentOf(params.getTextDocument().getUri());
        // LA SUGGESTION D'IMPORT (l'import du rapport des popups — la
        // bulle « Importer x.y.Z ? ») : le mot SANS déclaration et
        // SANS autre occurrence (le mot qui vient d'être tapé — le
        // serveur qui connaît sa bibliothèque y rend ses candidats
        // d'import, DEUX ici : la bulle compacte ouvre la liste). Le
        // genre du protocole le dit (« quickfix.import ») — la bulle
        // s'y distingue des correctifs ordinaires.
        int line = params.getRange().getStart().getLine();
        int[] bounds = lineBounds(line);
        String lineText = current.substring(bounds[0], bounds[1]);
        String unresolved = unresolvedWordOf(lineText);
        if (unresolved != null) {
            List<Either<org.eclipse.lsp4j.Command, CodeAction>> imports
                    = new ArrayList<>();
            int insertAt = importInsertOffset(current);
            Range hole = new Range(positionOf(insertAt), positionOf(insertAt));
            String packageWord = unresolved;
            CodeAction fromPackage = new CodeAction(
                    "Importer « " + packageWord + " » du paquet pigma");
            fromPackage.setKind("quickfix.import");
            fromPackage.setEdit(editOf(new TextEdit(hole,
                    "import pigma." + packageWord + ";\n")));
            imports.add(Either.forRight(fromPackage));
            CodeAction fromLibrary = new CodeAction(
                    "Importer « " + packageWord + " » de la bibliothèque");
            fromLibrary.setKind("quickfix.import");
            fromLibrary.setEdit(editOf(new TextEdit(hole,
                    "import biblio." + packageWord + ";\n")));
            imports.add(Either.forRight(fromLibrary));
            return CompletableFuture.completedFuture(imports);
        }
        // le correctif de la démonstration : les espaces de fin de la
        // ligne interrogée s'y coupent (la correction du protocole
        // porte son remplacement — l'ampoule l'applique)
        int content = lineText.stripTrailing().length();
        if (content == lineText.length()) {
            // LE CORRECTIF SUR DIAGNOSTIC (la démonstration de l'étage
            // des correctifs de la carte de diagnostic) : la ligne qui
            // COMMENCE par un fermant orphelin porte son correctif —
            // le signe superflu se coupe, la carte l'applique au
            // doigt (aucune action sinon)
            int parenAt = lineText.indexOf(')');
            if (parenAt >= 0 && lineText.substring(0, parenAt).isBlank()) {
                int cutStart = bounds[0] + parenAt;
                Range cut = new Range(positionOf(cutStart),
                        positionOf(cutStart + 1));
                TextEdit remove = new TextEdit(cut, "");
                WorkspaceEdit orphanEdit = new WorkspaceEdit();
                orphanEdit.setChanges(
                        java.util.Map.of(target(), List.of(remove)));
                CodeAction orphanFix = new CodeAction(
                        "Supprimer le fermant orphelin");
                orphanFix.setEdit(orphanEdit);
                return CompletableFuture.completedFuture(
                        List.of(Either.forRight(orphanFix)));
            }
            // rien à corriger sur la ligne : aucune action
            return CompletableFuture.completedFuture(List.of());
        }
        Range lineSpan = new Range(positionOf(bounds[0]), positionOf(bounds[1]));
        TextEdit trim = new TextEdit(lineSpan, lineText.stripTrailing());
        WorkspaceEdit edit = new WorkspaceEdit();
        edit.setChanges(java.util.Map.of(target(), List.of(trim)));
        CodeAction fix = new CodeAction("Supprimer les espaces de fin de ligne");
        fix.setEdit(edit);
        return CompletableFuture.completedFuture(List.of(Either.forRight(fix)));
    }

    /**
     * Le mot NON RÉSOLU d'une ligne (la suggestion d'import y vit) : le
     * mot ni mot-clé ni déclaré, dont l'occurrence de la ligne est la
     * SEULE du document — le mot qui vient d'être tapé, celui qu'un
     * serveur qui connaît sa bibliothèque propose d'importer.
     *
     * @param lineText la ligne interrogée
     * @return le mot non résolu, null quand la ligne n'en porte pas
     */
    private String unresolvedWordOf(String lineText) {
        // la PARTIE DE CODE de la ligne : le commentaire de fin s'y
        // coupe, la ligne de commentaire entière se tait (les mots d'un
        // commentaire ne sont pas des symboles), les chaînes s'y vident
        String trimmed = lineText.trim();
        if (trimmed.startsWith("//") || trimmed.startsWith("/*")
                || trimmed.startsWith("*")) {
            return null;
        }
        String code = lineText;
        int commentAt = code.indexOf("//");
        if (commentAt >= 0) {
            code = code.substring(0, commentAt);
        }
        StringBuilder withoutStrings = new StringBuilder();
        boolean inString = false;
        for (int at = 0; at < code.length(); at++) {
            char c = code.charAt(at);
            if (c == '"') {
                inString = !inString;
                withoutStrings.append(' ');
            } else {
                withoutStrings.append(inString ? ' ' : c);
            }
        }
        code = withoutStrings.toString();
        int at = 0;
        while (at < code.length()) {
            if (wordBreak(code.charAt(at))) {
                at++;
                continue;
            }
            int start = at;
            while (at < code.length() && !wordBreak(code.charAt(at))) {
                at++;
            }
            String word = code.substring(start, at);
            // le mot s'écrit : une LETTRE en tête (le littéral numérique
            // — 0x1F_FF, 125e — n'est pas un symbole)
            boolean keyword = false;
            for (String[] entry : KEYWORDS) {
                if (entry[0].equals(word)) {
                    keyword = true;
                    break;
                }
            }
            // le mot DÉCLARÉ l'y vit aussi : le mot qui suit un mot
            // déclarant (class, function, local…) se déclare là, le mot
            // qui précède une ouvrante ou un signe égal s'y nomme — le
            // serveur qui connaît son langage ne propose pas d'importer
            // une déclaration
            boolean declaredHere = false;
            for (String declaring : new String[]{"class", "interface", "enum",
                    "record", "struct", "function", "local", "field"}) {
                if (start >= declaring.length() + 1
                        && code.startsWith(declaring + " ",
                                start - declaring.length() - 1)) {
                    declaredHere = true;
                    break;
                }
            }
            int after = at;
            while (after < code.length()
                    && Character.isWhitespace(code.charAt(after))) {
                after++;
            }
            boolean namesSomething = after < code.length()
                    && (code.charAt(after) == '{' || code.charAt(after) == '('
                            || code.charAt(after) == '=');
            if (Character.isLetter(word.charAt(0))
                    && !keyword && !declaredHere && !namesSomething
                    && declarationOf(word) == null
                    && occurrencesOf(word).size() <= 1) {
                return word;
            }
        }
        return null;
    }

    /**
     * L'offset D'INSERTION d'un import : après la ligne de paquetage
     * (la déclaration de paquet du document), sinon le début du
     * document.
     *
     * @param current le document entier
     * @return l'offset où l'import se pose
     */
    private static int importInsertOffset(String current) {
        int packageEnd = current.indexOf(';');
        int firstLineEnd = current.indexOf('\n');
        if (packageEnd >= 0
                && (firstLineEnd < 0 || packageEnd < firstLineEnd)
                && current.startsWith("package")) {
            return firstLineEnd < 0 ? current.length() : firstLineEnd + 1;
        }
        return 0;
    }

    /**
     * L'édition d'espace de travail d'un remplacement seul.
     *
     * @param edit le remplacement du protocole
     * @return l'édition qui le porte
     */
    private WorkspaceEdit editOf(TextEdit edit) {
        WorkspaceEdit fresh = new WorkspaceEdit();
        fresh.setChanges(java.util.Map.of(target(), List.of(edit)));
        return fresh;
    }

    @Override
    public CompletableFuture<List<? extends TextEdit>> formatting(
            DocumentFormattingParams params) {
        String current = documentOf(params.getTextDocument().getUri());
        // le formatage du serveur de démonstration : les espaces de
        // fin de CHAQUE ligne s'y coupent (seules les lignes qui en
        // portent y paraissent — le remplacement vide ne sert à rien)
        List<TextEdit> edits = new ArrayList<>();
        int lineStart = 0;
        for (int at = 0; at <= current.length(); at++) {
            if (at == current.length() || current.charAt(at) == '\n') {
                String line = current.substring(lineStart, at);
                int content = line.stripTrailing().length();
                if (content < line.length()) {
                    edits.add(new TextEdit(
                            new Range(positionOf(lineStart + content),
                                    positionOf(lineStart + line.length())),
                            ""));
                }
                lineStart = at + 1;
            }
        }
        return CompletableFuture.completedFuture(edits);
    }

    @Override
    public CompletableFuture<List<Either<SymbolInformation, DocumentSymbol>>> documentSymbol(
            DocumentSymbolParams params) {
        String current = documentOf(params.getTextDocument().getUri());
        // la table des matières du document : chaque déclaration y
        // rend son symbole — le genre (méthode, champ), la ligne
        // entière pour l'étendue, le nom seul pour la sélection
        List<Either<SymbolInformation, DocumentSymbol>> symbols = new ArrayList<>();
        for (int[] declaration : declarations()) {
            String name = current.substring(declaration[0], declaration[1]);
            boolean method = declaration[2] == 1;
            int line = positionOf(declaration[0]).getLine();
            int[] bounds = lineBounds(line);
            symbols.add(Either.forRight(new DocumentSymbol(name,
                    method ? SymbolKind.Method : SymbolKind.Field,
                    new Range(positionOf(bounds[0]), positionOf(bounds[1])),
                    rangeOf(new int[]{declaration[0], declaration[1]}))));
        }
        return CompletableFuture.completedFuture(symbols);
    }

    @Override
    public CompletableFuture<List<FoldingRange>> foldingRange(
            FoldingRangeRequestParams params) {
        String current = documentOf(params.getTextDocument().getUri());
        // les plis du serveur de démonstration : les blocs
        // d'accolades appariées — l'ouvrante porte l'en-tête, la
        // fermante appariée porte la fin ; le pli ne vit que d'une
        // ouvrante à une AUTRE ligne
        List<FoldingRange> folds = new ArrayList<>();
        Deque<Integer> opened = new ArrayDeque<>();
        for (int at = 0; at < current.length(); at++) {
            char c = current.charAt(at);
            if (c == '{') {
                opened.push(at);
            } else if (c == '}' && !opened.isEmpty()) {
                int open = opened.pop();
                int openLine = positionOf(open).getLine();
                int closeLine = positionOf(at).getLine();
                if (closeLine > openLine) {
                    folds.add(new FoldingRange(openLine, closeLine));
                }
            }
        }
        return CompletableFuture.completedFuture(folds);
    }

    @Override
    public CompletableFuture<List<InlayHint>> inlayHint(InlayHintParams params) {
        String current = documentOf(params.getTextDocument().getUri());
        // l'indice du serveur de démonstration : le compte des usages
        // au bout de chaque ligne de déclaration (la façon de
        // l'atelier) — le texte n'en bouge pas, l'encre secondaire le
        // dessine ; la plage interrogée y borne la réponse
        List<InlayHint> hints = new ArrayList<>();
        for (int[] declaration : declarations()) {
            int line = positionOf(declaration[0]).getLine();
            if (line < params.getRange().getStart().getLine()
                    || line > params.getRange().getEnd().getLine()) {
                continue; // hors de la plage interrogée
            }
            String word = current.substring(declaration[0], declaration[1]);
            int usages = occurrencesOf(word).size() - 1;
            int[] bounds = lineBounds(line);
            hints.add(new InlayHint(
                    new Position(line, bounds[1] - bounds[0]),
                    Either.forLeft(usages + " usage(s)")));
        }
        return CompletableFuture.completedFuture(hints);
    }

    @Override
    public CompletableFuture<SemanticTokens> semanticTokensFull(
            SemanticTokensParams params) {
        String current = documentOf(params.getTextDocument().getUri());
        return CompletableFuture.completedFuture(new SemanticTokens(semanticData()));
    }

    /**
     * Les entiers des jetons sémantiques (l'encodage relatif du
     * protocole) : chaque mot du texte y porte son genre — le mot-clé
     * de la table, la fonction déclarée, la constante déclarée, la
     * variable autrement — l'occurrence qui DÉCLARE y porte le
     * modificateur (le bit de la légende).
     *
     * @return les cinq entiers par jeton, dans l'ordre du texte
     */
    private List<Integer> semanticData() {
        String current = currentText();
        Map<Integer, Integer> kindAt = new HashMap<>();
        Map<String, Integer> kindOfWord = new HashMap<>();
        for (int[] declaration : declarations()) {
            kindAt.put(declaration[0], declaration[2]);
            kindOfWord.put(current.substring(declaration[0], declaration[1]),
                    declaration[2]);
        }
        List<Integer> data = new ArrayList<>();
        int tokenLine = 0;
        int tokenChar = 0;
        int at = 0;
        while (at < current.length()) {
            if (wordBreak(current.charAt(at))) {
                at++;
                continue;
            }
            int start = at;
            while (at < current.length() && !wordBreak(current.charAt(at))) {
                at++;
            }
            String word = current.substring(start, at);
            if (Character.isDigit(word.charAt(0))) {
                continue; // le littéral demeure au lexer (la couleur locale)
            }
            Position position = positionOf(start);
            data.add(position.getLine() - tokenLine);
            data.add(position.getLine() == tokenLine
                    ? position.getCharacter() - tokenChar
                    : position.getCharacter());
            data.add(word.length());
            data.add(tokenTypeOf(word, kindOfWord.get(word)));
            data.add(kindAt.containsKey(start) ? 1 : 0);
            tokenLine = position.getLine();
            tokenChar = position.getCharacter();
        }
        return data;
    }

    /**
     * Le genre d'un jeton à sa légende : le mot-clé de la table, la
     * fonction (le mot dont la déclaration est une méthode), la
     * constante (le mot dont la déclaration est un champ final), la
     * variable autrement.
     *
     * @param word        le mot classé
     * @param declaredKind le genre de sa déclaration, null sans déclaration
     * @return l'indice du genre dans la légende
     */
    private static int tokenTypeOf(String word, Integer declaredKind) {
        for (String[] keyword : KEYWORDS) {
            if (keyword[0].equals(word)) {
                return 0; // « keyword » de la légende
            }
        }
        if (declaredKind != null) {
            return declaredKind == 1 ? 2 : 3; // « function » ou « constant »
        }
        return 1; // « variable » de la légende
    }

    /**
     * Les déclarations du texte, dans l'ordre : chaque mot qui y
     * déclare — le nom de méthode (le mot suivi de parenthèses, la
     * ligne finit au bloc ouvrant), le nom de champ final (le mot
     * suivi du signe égal, la ligne porte « final ») — y rend ses
     * bornes et son genre.
     *
     * @return les triplets [début, fin, genre] (1 : méthode, 0 : champ)
     */
    private List<int[]> declarations() {
        String current = currentText();
        List<int[]> found = new ArrayList<>();
        int at = 0;
        while (at < current.length()) {
            if (wordBreak(current.charAt(at))) {
                at++;
                continue;
            }
            int start = at;
            while (at < current.length() && !wordBreak(current.charAt(at))) {
                at++;
            }
            int after = at;
            while (after < current.length() && Character.isWhitespace(current.charAt(after))) {
                after++;
            }
            if (after >= current.length()) {
                continue;
            }
            char follows = current.charAt(after);
            int lineStart = current.lastIndexOf('\n', start) + 1;
            int lineEnd = current.indexOf('\n', start);
            if (lineEnd < 0) {
                lineEnd = current.length();
            }
            String line = current.substring(lineStart, lineEnd).trim();
            if (follows == '(' && line.endsWith("{") && line.indexOf('(') >= 0
                    && line.indexOf(')') >= 0) {
                found.add(new int[]{start, at, 1});
            } else if (follows == '=' && line.contains("final") && line.endsWith(";")) {
                found.add(new int[]{start, at, 0});
            }
        }
        return found;
    }

    /**
     * La déclaration d'un mot dans le texte : la ligne de méthode
     * (le mot suivi de parenthèses, la ligne finit au bloc ouvrant)
     * ou la ligne de champ final (le mot suivi du signe égal, la
     * ligne porte « final ») — la façon du serveur de trouver la
     * déclaration (même lecture que la signature).
     *
     * @param word le mot cherché
     * @return la place de la déclaration, null sans déclaration
     */
    private Location declarationOf(String word) {
        String current = currentText();
        for (int at = 0; at + word.length() <= current.length(); at++) {
            if (!current.startsWith(word, at) || !boundaryBefore(at)) {
                continue;
            }
            int after = at + word.length();
            while (after < current.length() && Character.isWhitespace(current.charAt(after))) {
                after++;
            }
            if (after >= current.length()) {
                continue;
            }
            char follows = current.charAt(after);
            int lineStart = current.lastIndexOf('\n', at) + 1;
            int lineEnd = current.indexOf('\n', at);
            if (lineEnd < 0) {
                lineEnd = current.length();
            }
            String line = current.substring(lineStart, lineEnd).trim();
            if (follows == '(' && line.endsWith("{") && line.indexOf('(') >= 0
                    && line.indexOf(')') >= 0) {
                // la déclaration de méthode : le nom précède les
                // parenthèses, la ligne finit au bloc ouvrant
                return new Location(target(), rangeOf(new int[]{at, at + word.length()}));
            }
            if (follows == '=' && line.contains("final") && line.endsWith(";")) {
                // la déclaration de champ : le nom précède le signe
                // égal, la ligne porte « final » (une constante)
                return new Location(target(), rangeOf(new int[]{at, at + word.length()}));
            }
        }
        return null;
    }

    /**
     * Les occurrences d'un mot dans le texte, aux bornes de mot près
     * (la même lecture que le survol qui les compte).
     *
     * @param word le mot cherché
     * @return les couples [début, fin] de chaque occurrence
     */
    private List<int[]> occurrencesOf(String word) {
        String current = currentText();
        List<int[]> occurrences = new ArrayList<>();
        int at = current.indexOf(word);
        while (at >= 0) {
            int end = at + word.length();
            if (boundaryBefore(at)
                    && (end >= current.length() || wordBreak(current.charAt(end)))) {
                occurrences.add(new int[]{at, end});
            }
            at = current.indexOf(word, at + word.length());
        }
        return occurrences;
    }

    /**
     * Le mot à une position du protocole (null : aucun mot là).
     *
     * @param position la position interrogée
     * @return le mot, null sans mot
     */
    private String wordAt(Position position) {
        String current = currentText();
        int[] bounds = wordBoundsAt(position);
        return bounds[1] > bounds[0] ? current.substring(bounds[0], bounds[1]) : null;
    }

    /**
     * Les bornes du mot qui contient une position du protocole.
     *
     * @param position la position (ligne, colonne)
     * @return les bornes absolues [début, fin] du mot
     */
    private int[] wordBoundsAt(Position position) {
        String current = currentText();
        int caret = Math.min(offsetOf(current, position), current.length());
        int lineStart = current.lastIndexOf('\n', caret) + 1;
        int lineEnd = current.indexOf('\n', caret);
        if (lineEnd < 0) {
            lineEnd = current.length();
        }
        int start = caret;
        while (start > lineStart && !wordBreak(current.charAt(start - 1))) {
            start--;
        }
        int end = caret;
        while (end < lineEnd && !wordBreak(current.charAt(end))) {
            end++;
        }
        return new int[]{start, end};
    }

    /**
     * Les bornes absolues d'une ligne.
     *
     * @param line l'indice de la ligne
     * @return les bornes [début, fin] (la fin exclut le saut de ligne)
     */
    private int[] lineBounds(int line) {
        String current = currentText();
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
        return new int[]{start, end};
    }

    /**
     * L'intervalle du protocole de bornes absolues.
     *
     * @param bounds les bornes [début, fin]
     * @return l'intervalle équivalent
     */
    private Range rangeOf(int[] bounds) {
        return new Range(positionOf(bounds[0]), positionOf(bounds[1]));
    }

    /**
     * La position du protocole d'un décalage absolu.
     *
     * @param offset le décalage dans le texte
     * @return la position (ligne, colonne)
     */
    private Position positionOf(int offset) {
        String current = currentText();
        int line = 0;
        int lineStart = 0;
        for (int at = 0; at < offset && at < current.length(); at++) {
            if (current.charAt(at) == '\n') {
                line++;
                lineStart = at + 1;
            }
        }
        return new Position(line, offset - lineStart);
    }

    /**
     * La signature du nom appelé : sa déclaration dans le texte — la
     * ligne qui porte le nom, des parenthèses et le bloc ouvrant —
     * réécrite à la façon du serveur (les paramètres d'abord, le type
     * de retour après).
     *
     * @param openParen l'ouvrante de l'appel en cours
     * @return la signature trouvée, null sans déclaration du nom
     */
    private SignatureInformation signatureOfCall(int openParen) {
        String current = currentText();
        int nameEnd = openParen;
        while (nameEnd > 0 && Character.isWhitespace(current.charAt(nameEnd - 1))) {
            nameEnd--;
        }
        int nameStart = nameEnd;
        while (nameStart > 0 && !wordBreak(current.charAt(nameStart - 1))) {
            nameStart--;
        }
        if (nameStart == nameEnd) {
            return null;
        }
        String name = current.substring(nameStart, nameEnd);
        String declarationLine = null;
        for (int at = 0; at + name.length() <= current.length(); at++) {
            if (!current.startsWith(name, at) || !boundaryBefore(at)) {
                continue;
            }
            int after = at + name.length();
            while (after < current.length()
                    && Character.isWhitespace(current.charAt(after))) {
                after++;
            }
            if (after >= current.length() || current.charAt(after) != '(') {
                continue;
            }
            int lineStart = current.lastIndexOf('\n', at) + 1;
            int lineEnd = current.indexOf('\n', at);
            if (lineEnd < 0) {
                lineEnd = current.length();
            }
            String line = current.substring(lineStart, lineEnd).trim();
            if (line.endsWith("{") && line.indexOf('(') >= 0
                    && line.indexOf(')') >= 0) {
                declarationLine = line;
                break;
            }
        }
        if (declarationLine == null) {
            return null;
        }
        int open = declarationLine.indexOf('(');
        int close = declarationLine.lastIndexOf(')');
        String parameters = declarationLine.substring(open, close + 1);
        String head = declarationLine.substring(0, open).trim();
        // le nom appelé finit la tête : le type de retour est le mot
        // juste avant lui
        int headEnd = head.length();
        while (headEnd > 0 && !Character.isWhitespace(head.charAt(headEnd - 1))) {
            headEnd--;
        }
        String beforeName = head.substring(0, headEnd).trim();
        int returnTypeStart = beforeName.length();
        while (returnTypeStart > 0
                && !Character.isWhitespace(beforeName.charAt(returnTypeStart - 1))) {
            returnTypeStart--;
        }
        String returnType = beforeName.substring(returnTypeStart).trim();
        if (returnType.isEmpty()) {
            returnType = name;
        }
        return new SignatureInformation(name + parameters + " : " + returnType);
    }

    /** Vrai quand le caractère avant la position borne le mot. */
    private boolean boundaryBefore(int at) {
        String current = currentText();
        return at == 0 || wordBreak(current.charAt(at - 1));
    }

    /**
     * Le préfixe tapé à une position : les caractères du mot avant la
     * colonne.
     */
    private String prefixAt(int line, int column) {
        String current = currentText();
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
        int caret = start + Math.min(column, end - start);
        int wordStart = caret;
        while (wordStart > start && !wordBreak(current.charAt(wordStart - 1))) {
            wordStart--;
        }
        return current.substring(wordStart, caret);
    }

    /** Les diagnostics du texte courant : les parenthèses non appariées. */
    private void publishDiagnostics() {
        String current = currentText();
        LanguageClient endpoint = client;
        if (endpoint == null) {
            return;
        }
        int unbalanced = 0;
        int depth = 0;
        for (int at = 0; at < current.length(); at++) {
            char c = current.charAt(at);
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
        String target = uri != null ? uri : document;
        if (target == null) {
            return;
        }
        endpoint.publishDiagnostics(new PublishDiagnosticsParams(target, diagnostics));
    }

    /**
     * La description du mot à la position : le mot-clé de la table,
     * sinon le compte de ses occurrences dans le document — la réponse
     * d'un serveur qui aurait lu le texte (le nôtre le garde).
     */
    private String describeWordAt(int line, int column) {
        String current = texts.getOrDefault(document, "");
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
