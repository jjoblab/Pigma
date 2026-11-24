package jo.pigma.lsp;

import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.MarkupContent;

/**
 * Le diagnostic rapporté par un serveur : un message, un intervalle,
 * une sévérité (1 erreur, 2 avertissement, 3 information, 4 indice —
 * le protocole ne dit pas plus). L'adaptation du type lsp4j garde la
 * sévérité en entier, la valeur du protocole, et le texte du message
 * (le protocole l'envoie en contenu simple ou riche — le texte seul
 * nous intéresse).
 *
 * @param range    l'intervalle concerné
 * @param severity la sévérité, 0 si absente
 * @param message  le texte du diagnostic
 */
public record LspDiagnostic(LspRange range, int severity, String message) {

    /**
     * L'adaptation d'un diagnostic lsp4j.
     *
     * @param diagnostic le diagnostic du protocole
     * @return le diagnostic du domaine
     */
    public static LspDiagnostic fromLsp(Diagnostic diagnostic) {
        DiagnosticSeverity severity = diagnostic.getSeverity();
        return new LspDiagnostic(LspRange.fromLsp(diagnostic.getRange()),
                severity == null ? 0 : severity.getValue(),
                messageText(diagnostic.getMessage()));
    }

    /**
     * Le texte d'un message du protocole : simple ou riche, le texte
     * seul revient (vide si le message manque).
     */
    private static String messageText(Either<String, MarkupContent> message) {
        if (message == null) {
            return "";
        }
        if (message.isLeft()) {
            return message.getLeft() == null ? "" : message.getLeft();
        }
        return message.getRight() == null || message.getRight().getValue() == null
                ? "" : message.getRight().getValue();
    }
}
