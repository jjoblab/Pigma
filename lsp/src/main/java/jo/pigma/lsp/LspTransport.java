package jo.pigma.lsp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Le transport d'une liaison : les messages partent en JSON cadres,
 * les réponses arrivent de la même façon. Deux réalisations : la paire
 * en processus (le serveur de démonstration, les tests) et le
 * processus externe (le cadrage {@code Content-Length} du protocole
 * sur l'entrée et la sortie standard).
 */
public interface LspTransport {

    /**
     * Envoie un message JSON.
     *
     * @param json le message sérialisé, jamais {@code null}
     * @throws IOException si le canal est cassé
     */
    void send(String json) throws IOException;

    /**
     * Lit le prochain message JSON : bloquant, null à la fermeture du
     * canal (la pompe s'arrête alors).
     *
     * @return le message sérialisé, ou {@code null} en fin de canal
     * @throws IOException si le canal casse en cours de lecture
     */
    String read() throws IOException;

    /**
     * Ferme le transport : les lectures pendantes rendent null, le
     * processus externe (s'il y en a un) s'arrête.
     */
    void close();

    /**
     * Une paire de transports raccordés : ce que l'un envoie, l'autre
     * le lit — le serveur de démonstration et les tests s'y branchent
     * sans processus.
     *
     * @return les deux bouts, [0] pour le client, [1] pour le serveur
     */
    static LspTransport[] newPair() {
        LinkedBlockingQueue<String> toServer = new LinkedBlockingQueue<>();
        LinkedBlockingQueue<String> toClient = new LinkedBlockingQueue<>();
        return new LspTransport[] {
                new QueueTransport(toClient, toServer),
                new QueueTransport(toServer, toClient)
        };
    }

    /**
     * Un bout de la paire : une file d'entrée, une file de sortie. La
     * fermeture pose la pastille d'arrêt — l'autre bout rend null.
     */
    final class QueueTransport implements LspTransport {

        private static final String POISON = "\u0000closed";

        private final LinkedBlockingQueue<String> incoming;

        private final LinkedBlockingQueue<String> outgoing;

        private volatile boolean closed;

        private QueueTransport(LinkedBlockingQueue<String> incoming,
                LinkedBlockingQueue<String> outgoing) {
            this.incoming = incoming;
            this.outgoing = outgoing;
        }

        @Override
        public void send(String json) {
            if (!closed) {
                outgoing.add(json);
            }
        }

        @Override
        public String read() {
            try {
                String message = incoming.take();
                return message == POISON ? null : message;
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return null;
            }
        }

        @Override
        public void close() {
            closed = true;
            outgoing.add(POISON);
            incoming.add(POISON);
        }
    }

    /**
     * Le transport de processus : un serveur de langage lancé par
     * {@code ProcessBuilder}, les messages cadrés par
     * {@code Content-Length} sur l'entrée et la sortie standard — le
     * raccordement usuel du protocole.
     *
     * <p>La lecture est une boucle bloquante sur la sortie standard du
     * processus ; la mort du processus rend null (la pompe de la
     * la liaison s'arrête alors).</p>
     */
    final class ProcessTransport implements LspTransport {

        private final Process process;

        private final BufferedWriter output;

        private final BufferedReader input;

        /**
         * Lance le serveur et ouvre ses canaux.
         *
         * @param command la commande du serveur, jamais vide
         * @throws IOException si le processus ne part pas
         */
        public ProcessTransport(List<String> command) throws IOException {
            Objects.requireNonNull(command);
            if (command.isEmpty()) {
                throw new IllegalArgumentException("commande vide");
            }
            process = new ProcessBuilder(command).start();
            output = new BufferedWriter(new OutputStreamWriter(
                    process.getOutputStream(), StandardCharsets.UTF_8));
            input = new BufferedReader(new InputStreamReader(
                    process.getInputStream(), StandardCharsets.UTF_8));
        }

        @Override
        public void send(String json) throws IOException {
            output.write("Content-Length: " + json.length());
            output.write("\r\n\r\n");
            output.write(json);
            output.flush();
        }

        @Override
        public String read() throws IOException {
            int length = -1;
            String line;
            while ((line = input.readLine()) != null) {
                if (line.isEmpty()) {
                    break;
                }
                if (line.startsWith("Content-Length:")) {
                    String value = line.substring("Content-Length:".length()).trim();
                    try {
                        length = Integer.parseInt(value);
                    } catch (NumberFormatException bad) {
                        length = -1;
                    }
                }
            }
            if (line == null || length < 0) {
                return null;
            }
            char[] body = new char[length];
            int read = 0;
            while (read < length) {
                int chunk = input.read(body, read, length - read);
                if (chunk < 0) {
                    return null;
                }
                read += chunk;
            }
            return new String(body);
        }

        @Override
        public void close() {
            process.destroy();
        }

        /**
         * Le processus du serveur (l'hôte attend sa mort à l'arrêt).
         *
         * @return le processus lancé au constructeur
         */
        public Process process() {
            return process;
        }
    }
}
