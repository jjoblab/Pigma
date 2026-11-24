package jo.pigma.lsp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

/**
 * Serveur d'essai en processus : il lit les messages cadrés sur
 * l'entrée standard et répond sur la sortie standard — le
 * raccordement réel du protocole, celui qu'un serveur de langage
 * utilise. La classe n'a pas de test : elle EST l'instrument du test
 * du transport par processus.
 *
 * <p>Le protocole d'essai : {@code initialize} rend des capacités,
 * {@code textDocument/hover} rend un texte fixe, {@code exit} rend la
 * main.</p>
 */
public final class TestServerMain {

    private TestServerMain() {
    }

    /**
     * Le point d'entrée : boucle de lecture des cadrages, réponse par
     * cadrage.
     *
     * @param args aucun argument attendu
     * @throws IOException si un canal casse
     */
    public static void main(String[] args) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(
                System.in, StandardCharsets.UTF_8));
        Writer out = new OutputStreamWriter(System.out, StandardCharsets.UTF_8);
        while (true) {
            String json = readFrame(in);
            if (json == null) {
                return;
            }
            JsonObject object = (JsonObject) JsonValue.parse(json);
            LspMessage message = LspMessage.fromJson(object);
            if (message.method() == null) {
                continue;
            }
            switch (message.method()) {
                case "initialize" -> writeFrame(out, LspMessage.response(message.id(),
                        JsonObject.of("capabilities",
                                JsonObject.of("hoverProvider", JsonBoolean.TRUE)),
                        null).toJson());
                case "textDocument/hover" -> writeFrame(out, LspMessage.response(
                        message.id(),
                        JsonObject.of("contents", JsonObject.of(
                                "kind", JsonString.of("plaintext"),
                                "value", JsonString.of("réponse du sous-processus"))),
                        null).toJson());
                case "shutdown" -> writeFrame(out, LspMessage.response(
                        message.id(), new JsonObject(), null).toJson());
                case "exit" -> {
                    out.flush();
                    return;
                }
                default -> {
                }
            }
        }
    }

    private static String readFrame(BufferedReader in) throws IOException {
        int length = -1;
        String line;
        while ((line = in.readLine()) != null) {
            if (line.isEmpty()) {
                break;
            }
            if (line.startsWith("Content-Length:")) {
                length = Integer.parseInt(
                        line.substring("Content-Length:".length()).trim());
            }
        }
        if (line == null || length < 0) {
            return null;
        }
        char[] body = new char[length];
        int read = 0;
        while (read < length) {
            int chunk = in.read(body, read, length - read);
            if (chunk < 0) {
                return null;
            }
            read += chunk;
        }
        return new String(body);
    }

    private static void writeFrame(Writer out, String json) throws IOException {
        out.write("Content-Length: " + json.length());
        out.write("\r\n\r\n");
        out.write(json);
        out.flush();
    }
}
