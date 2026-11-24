package jo.pigma.lsp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * La liaison cliente : les requêtes partent avec un identifiant qui
 * monte, les réponses s'y raccrochent (les futurs tiennent la
 * corrélation), les notifications du serveur se dispatchent à
 * l'écouteur. Une pompe lit le transport sur son fil : les réponses
 * y résolvent leurs futurs, l'écouteur y est appelé — l'hôte marshalle
 * vers son fil d'interface.
 */
public final class LspSession {

    /**
     * L'écouteur des notifications du serveur : les diagnostics
     * publiés et les messages de journal. Les méthodes courent sur le
     * fil de la pompe.
     */
    public interface Listener {

        /**
         * Le serveur publie les diagnostics d'un document : la liste
         * ENTIERE remplace la précédente (le protocole est à
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

    private final LspTransport transport;

    private final Listener listener;

    private final AtomicInteger nextId = new AtomicInteger();

    private final Map<Integer, CompletableFuture<JsonObject>> pending = new HashMap<>();

    private final Object pendingLock = new Object();

    private volatile boolean running;

    private Thread pump;

    /**
     * La liaison sur un transport ouvert : {@link #start()} lance la
     * pompe, aucune requête ne part avant.
     *
     * @param transport le transport du serveur, jamais {@code null}
     * @param listener  l'écouteur des notifications, null pour ignorer
     */
    public LspSession(LspTransport transport, Listener listener) {
        this.transport = Objects.requireNonNull(transport);
        this.listener = listener == null ? new Listener() {
        } : listener;
    }

    /** Lance la pompe de lecture : le fil démarre, la liaison vit. */
    public synchronized void start() {
        if (running) {
            return;
        }
        running = true;
        pump = new Thread(this::pumpLoop, "pigma-lsp");
        pump.setDaemon(true);
        pump.start();
    }

    /**
     * L'ouverture du protocole : la négociation des capacités. La
     * réponse porte le JSON brut des capacités (le client n'impose pas
     * de sous-ensemble).
     *
     * @param clientName le nom du client (l'affiche le serveur)
     * @return le futur des capacités du serveur
     */
    public CompletableFuture<JsonObject> initialize(String clientName) {
        JsonObject params = JsonObject.of(
                "processId", JsonNull.NULL,
                "clientInfo", JsonObject.of("name", JsonString.of(clientName)),
                "capabilities", new JsonObject());
        return request("initialize", params);
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
        JsonObject params = JsonObject.of(
                "textDocument", JsonObject.of(
                        "uri", JsonString.of(uri),
                        "languageId", JsonString.of(languageId),
                        "version", JsonNumber.of(1),
                        "text", JsonString.of(text)));
        notifyServer("textDocument/didOpen", params);
    }

    /**
     * Annonce un changement : la synchronisation intégrale — le texte
     * entier repart, le serveur refait son analyse dessus (la version
     * monte, le protocole y retrouve son ordre).
     *
     * @param uri   le document changé
     * @param text  le texte complet après le changement
     */
    public void didChange(String uri, String text) {
        JsonObject params = JsonObject.of(
                "textDocument", JsonObject.of(
                        "uri", JsonString.of(uri),
                        "version", JsonNumber.of(System.nanoTime())),
                "contentChanges", JsonArray.of(
                        JsonObject.of("text", JsonString.of(text))));
        notifyServer("textDocument/didChange", params);
    }

    /**
     * Demande l'information flottante à une position : la réponse porte
     * le texte de l'information (le contenu « plaintext » du protocole),
     * null si le serveur n'en a pas.
     *
     * @param uri      le document interrogé
     * @param position la position du curseur
     * @return le futur du texte d'information
     */
    public CompletableFuture<String> hover(String uri, LspPosition position) {
        JsonObject params = JsonObject.of(
                "textDocument", JsonObject.of("uri", JsonString.of(uri)),
                "position", position.toJson());
        return request("textDocument/hover", params).thenApply(result -> {
            if (result == null) {
                return null;
            }
            JsonObject contents = result.getObject("contents");
            if (contents != null) {
                String value = contents.getString("value");
                if (value != null) {
                    return value;
                }
            }
            return null;
        });
    }

    /**
     * La fermeture propre du protocole : la requête d'arrêt (le serveur
     * finit), la notification de sortie (le processus meurt), le
     * transport se ferme, la pompe s'arrête.
     */
    public void shutdown() {
        try {
            request("shutdown", new JsonObject()).get();
        } catch (Exception waitFailed) {
            // le serveur peut déjà être mort : la fermeture n'en dépend pas
        }
        notifyServer("exit", new JsonObject());
        running = false;
        transport.close();
        synchronized (pendingLock) {
            for (CompletableFuture<JsonObject> future : pending.values()) {
                future.cancel(false);
            }
            pending.clear();
        }
    }

    private CompletableFuture<JsonObject> request(String method, JsonObject params) {
        int id = nextId.incrementAndGet();
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        synchronized (pendingLock) {
            pending.put(id, future);
        }
        send(LspMessage.request(id, method, params));
        return future;
    }

    private void notifyServer(String method, JsonObject params) {
        send(LspMessage.notification(method, params));
    }

    private void send(LspMessage message) {
        try {
            transport.send(message.toJson());
        } catch (IOException broken) {
            synchronized (pendingLock) {
                CompletableFuture<JsonObject> future = pending.remove(message.id());
                if (future != null) {
                    future.completeExceptionally(broken);
                }
            }
        }
    }

    private void pumpLoop() {
        while (running) {
            String json;
            try {
                json = transport.read();
            } catch (IOException broken) {
                break;
            }
            if (json == null) {
                break;
            }
            deliver(json);
        }
        running = false;
        synchronized (pendingLock) {
            for (CompletableFuture<JsonObject> future : pending.values()) {
                future.cancel(false);
            }
            pending.clear();
        }
    }

    private void deliver(String json) {
        JsonValue value;
        try {
            value = JsonValue.parse(json);
        } catch (JsonParseException malformed) {
            return;
        }
        if (!(value instanceof JsonObject object)) {
            return;
        }
        LspMessage message;
        try {
            message = LspMessage.fromJson(object);
        } catch (IllegalArgumentException unknown) {
            return;
        }
        if (message.method() == null) {
            // une réponse : le futur correspondant prend le résultat
            CompletableFuture<JsonObject> future;
            synchronized (pendingLock) {
                future = pending.remove(message.id());
            }
            if (future != null) {
                if (message.error() != null) {
                    future.completeExceptionally(new IllegalStateException(
                            "le serveur refuse : " + message.error().message()));
                } else {
                    future.complete(message.result() instanceof JsonObject result
                            ? result : new JsonObject());
                }
            }
            return;
        }
        dispatchNotification(message);
    }

    private void dispatchNotification(LspMessage message) {
        switch (message.method()) {
            case "textDocument/publishDiagnostics" -> {
                JsonObject params = message.params();
                if (params == null) {
                    return;
                }
                String uri = params.getString("uri");
                JsonArray array = params.getArray("diagnostics");
                List<LspDiagnostic> diagnostics = new ArrayList<>();
                if (array != null) {
                    for (JsonValue item : array.items()) {
                        if (item instanceof JsonObject diagnostic) {
                            diagnostics.add(LspDiagnostic.fromJson(diagnostic));
                        }
                    }
                }
                listener.onDiagnostics(uri, diagnostics);
            }
            case "window/logMessage" -> {
                JsonObject params = message.params();
                if (params != null) {
                    String text = params.getString("message");
                    if (text != null) {
                        listener.onLogMessage(text);
                    }
                }
            }
            default -> {
                // les autres notifications ne concernent pas ce client
            }
        }
    }

    /** La pompe vit-elle encore ? (le diagnostic de la liaison) */
    public boolean isRunning() {
        return running;
    }
}
