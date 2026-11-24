package jo.pigma.sample;

import jo.pigma.lsp.JsonArray;
import jo.pigma.lsp.JsonBoolean;
import jo.pigma.lsp.JsonObject;
import jo.pigma.lsp.JsonString;
import jo.pigma.lsp.JsonValue;
import jo.pigma.lsp.LspDiagnostic;
import jo.pigma.lsp.LspMessage;
import jo.pigma.lsp.LspPosition;
import jo.pigma.lsp.LspRange;
import jo.pigma.lsp.LspTransport;

import java.io.IOException;

/**
 * Serveur de démonstration en processus : il parle le sous-ensemble du
 * protocole que le client connaît — l'initialisation rend des
 * capacités, le document ouvert publie les parenthèses non appariées
 * (une lecture lexicale, pas l'analyse de la bibliothèque : le serveur
 * fait semblant d'être extérieur), le survol rend la description des
 * mots-clés et le compte des occurrences du mot.
 */
final class DemoLanguageServer {

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

    private final LspTransport side;

    private final String uri;

    private String text = "";

    private Thread thread;

    DemoLanguageServer(LspTransport side, String uri) {
        this.side = side;
        this.uri = uri;
    }

    /** Le serveur part sur son fil : il vit jusqu'au canal fermé. */
    synchronized void start() {
        if (thread != null) {
            return;
        }
        thread = new Thread(this::serve, "demo-language-server");
        thread.setDaemon(true);
        thread.start();
    }

    private void serve() {
        try {
            serveFrames();
        } catch (IOException broken) {
            // le client a fermé : le serveur s'en va
        } catch (RuntimeException unexpected) {
            // un message inattendu n'arrête pas le serveur de démonstration
        }
    }

    private void serveFrames() throws IOException {
        while (true) {
            String json = side.read();
            if (json == null) {
                return;
            }
            if (!(JsonValue.parse(json) instanceof JsonObject object)) {
                continue;
            }
            LspMessage message;
            try {
                message = LspMessage.fromJson(object);
            } catch (IllegalArgumentException unknown) {
                continue;
            }
            if (message.method() == null) {
                continue;
            }
            switch (message.method()) {
                case "initialize" -> side.send(LspMessage.response(message.id(),
                        JsonObject.of("capabilities", JsonObject.of(
                                "hoverProvider", JsonBoolean.TRUE)),
                        null).toJson());
                case "textDocument/didOpen", "textDocument/didChange" -> {
                    text = message.params().getObject("textDocument").getString("text");
                    if (text == null && message.params().getArray("contentChanges") != null
                            && message.params().getArray("contentChanges").size() > 0) {
                        text = message.params().getArray("contentChanges").get(0)
                                instanceof JsonObject change
                                ? change.getString("text") : text;
                    }
                    publishDiagnostics();
                }
                case "textDocument/hover" -> serveHover(message);
                case "shutdown" -> side.send(LspMessage.response(
                        message.id(), new JsonObject(), null).toJson());
                case "exit" -> {
                    return;
                }
                default -> {
                }
            }
        }
    }

    private void publishDiagnostics() throws IOException {
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
        JsonArray diagnostics = total == 0 ? new JsonArray()
                : JsonArray.of(new LspDiagnostic(
                        new LspRange(new LspPosition(0, 0), new LspPosition(0, 1)),
                        1,
                        "parenthèses non appariées : " + total
                                + " (compte du serveur de démonstration)").toJson());
        side.send(LspMessage.notification("textDocument/publishDiagnostics",
                JsonObject.of("uri", JsonString.of(uri),
                        "diagnostics", diagnostics)).toJson());
    }

    private void serveHover(LspMessage message) throws IOException {
        JsonObject params = message.params();
        int line = params.getObject("position").getNumber("line").intValue();
        int column = params.getObject("position").getNumber("character").intValue();
        String value = describeWordAt(line, column);
        if (value == null) {
            side.send(LspMessage.response(message.id(), new JsonObject(), null).toJson());
            return;
        }
        side.send(LspMessage.response(message.id(),
                JsonObject.of("contents", JsonObject.of(
                        "kind", JsonString.of("plaintext"),
                        "value", JsonString.of(value))),
                null).toJson());
    }

    /**
     * La description du mot à la position : le mot-clé de la table,
     * sinon le compte de ses occurrences dans le document — la réponse
     * d'un serveur qui aurait lu le texte (le nôtre le garde).
     */
    private String describeWordAt(int line, int column) {
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
        if (column > end - start) {
            column = end - start;
        }
        int wordStart = start + column;
        while (wordStart > start && !wordBreak(text.charAt(wordStart - 1))) {
            wordStart--;
        }
        int wordEnd = start + column;
        while (wordEnd < end && !wordBreak(text.charAt(wordEnd))) {
            wordEnd++;
        }
        if (wordEnd <= wordStart) {
            return null;
        }
        String word = text.substring(wordStart, wordEnd);
        for (String[] keyword : KEYWORDS) {
            if (keyword[0].equals(word)) {
                return "« " + word + " » — " + keyword[1];
            }
        }
        int occurrences = 0;
        int at = text.indexOf(word);
        while (at >= 0) {
            occurrences++;
            at = text.indexOf(word, at + word.length());
        }
        return "« " + word + " » — " + occurrences + " occurrence(s) dans le document";
    }

    private static boolean wordBreak(char c) {
        return !Character.isJavaIdentifierPart(c);
    }
}
