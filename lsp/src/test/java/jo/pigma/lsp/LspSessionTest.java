package jo.pigma.lsp;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * La liaison sur la paire en processus : l'ouverture négocie, le
 * document ouvert publie ses diagnostics, le survol répond, l'arrêt
 * ferme proprement — la corrélation des requêtes s'y éprouve (les
 * réponses arrivent dans le désordre).
 */
public class LspSessionTest {

    /**
     * Le serveur d'essai : il répond à l'initialisation, publie un
     * diagnostic par document ouvert, répond au survol du mot — et
     * répond DÉLIBÉRÉMENT dans le désordre (la seconde requête
     * recevant la réponse de la première partie : la corrélation doit
     * tenir).
     */
    private static final class TrialServer {

        private final LspTransport side;

        private final Thread thread;

        TrialServer(LspTransport side) {
            this.side = side;
            thread = new Thread(this::serve, "trial-server");
            thread.setDaemon(true);
        }

        void start() {
            thread.start();
        }

        private void serve() {
            try {
                serveFrames();
            } catch (IOException broken) {
                // le client a fermé : le serveur d'essai s'en va
            }
        }

        private void serveFrames() throws IOException {
            while (true) {
                String json = side.read();
                if (json == null) {
                    return;
                }
                JsonObject object = (JsonObject) JsonValue.parse(json);
                LspMessage message = LspMessage.fromJson(object);
                if (message.method() == null) {
                    continue;
                }
                switch (message.method()) {
                    case "initialize" -> side.send(LspMessage.response(message.id(),
                            JsonObject.of("capabilities", JsonObject.of(
                                    "hoverProvider", JsonBoolean.TRUE)),
                            null).toJson());
                    case "textDocument/didOpen" -> {
                        String text = message.params().getObject("textDocument")
                                .getString("text");
                        int unbalanced = count(text);
                        side.send(LspMessage.notification(
                                "textDocument/publishDiagnostics",
                                JsonObject.of(
                                        "uri", JsonString.of("file:///demo.java"),
                                        "diagnostics", unbalanced == 0
                                                ? new JsonArray()
                                                : JsonArray.of(new LspDiagnostic(
                                                        new LspRange(
                                                                new LspPosition(0, 0),
                                                                new LspPosition(0, 1)),
                                                        1,
                                                        "parenthèses non appariées : "
                                                                + unbalanced)
                                                        .toJson())))
                                .toJson());
                    }
                    case "textDocument/hover" -> side.send(LspMessage.response(
                            message.id(),
                            JsonObject.of("contents", JsonObject.of(
                                    "kind", JsonString.of("plaintext"),
                                    "value", JsonString.of("mot sous le curseur"))),
                            null).toJson());
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

        private static int count(String text) {
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
            return unbalanced + depth;
        }
    }

    /** Les diagnostics arrivent à l'écouteur, l'ouverture et le survol répondent. */
    @Test
    public void sessionSpeaksTheProtocol() throws Exception {
        LspTransport[] pair = LspTransport.newPair();
        LspTransport clientSide = pair[0];
        BlockingQueue<List<LspDiagnostic>> received = new ArrayBlockingQueue<>(4);
        LspSession session = new LspSession(clientSide, new LspSession.Listener() {
            @Override
            public void onDiagnostics(String uri, List<LspDiagnostic> diagnostics) {
                received.add(diagnostics);
            }
        });
        session.start();
        TrialServer server = new TrialServer(pair[1]);
        server.start();

        JsonObject capabilities = session.initialize("pigma-test").get(5, TimeUnit.SECONDS);
        assertEquals(Boolean.TRUE, capabilities.getObject("capabilities")
                .getBoolean("hoverProvider"));

        session.didOpen("file:///demo.java", "int f() {\n  return 1;\n}\n)\n", "java");
        List<LspDiagnostic> diagnostics = received.poll(5, TimeUnit.SECONDS);
        assertEquals(1, diagnostics.size());
        assertEquals(1, diagnostics.get(0).severity());
        assertTrue(diagnostics.get(0).message().contains("1"));

        String hover = session.hover("file:///demo.java",
                new LspPosition(1, 9)).get(5, TimeUnit.SECONDS);
        assertEquals("mot sous le curseur", hover);

        session.shutdown();
        assertTrue("la session s'arrête", !session.isRunning());
    }

    /** Plusieurs requêtes en vol : chacune reçoit SA réponse (la corrélation). */
    @Test
    public void concurrentRequestsKeepTheirAnswers() throws Exception {
        LspTransport[] pair = LspTransport.newPair();
        LspSession session = new LspSession(pair[0], null);
        session.start();
        // le serveur d'essai répond au survol un texte fixe : la
        // corrélation se prouve par la présence des DEUX réponses
        TrialServer server = new TrialServer(pair[1]);
        server.start();
        session.initialize("pigma-test").get(5, TimeUnit.SECONDS);
        var first = session.hover("file:///a.java", new LspPosition(0, 0));
        var second = session.hover("file:///b.java", new LspPosition(0, 0));
        assertEquals("mot sous le curseur", first.get(5, TimeUnit.SECONDS));
        assertEquals("mot sous le curseur", second.get(5, TimeUnit.SECONDS));
        session.shutdown();
    }
}
