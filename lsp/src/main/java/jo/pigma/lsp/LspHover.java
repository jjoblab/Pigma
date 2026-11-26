package jo.pigma.lsp;

import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.MarkedString;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import java.util.List;

/**
 * Le survol du serveur, AVEC SON BALISAGE : le défaut du rapport des
 * popups — l'adaptation d'avant fondait la réponse en texte nu, le
 * Markdown des serveurs y mourait (titres, listes, code). Le balisage
 * dit s'il faut le RENDRE (le sous-ensemble du domaine : titres,
 * code, listes, liens) ou le poser tel quel (le texte nu du survol
 * d'avant reste le sien).
 *
 * @param value    le contenu du survol (jamais null : vide pour « le serveur ne dit rien »)
 * @param markdown le contenu est-il balisé (Markdown du protocole) ?
 */
public record LspHover(String value, boolean markdown) {

    /** Le survol muet : le serveur n'en dit rien. */
    public static final LspHover SILENT = new LspHover("", false);

    /**
     * L'adaptation d'un survol du protocole : la forme balisée prime
     * (le contenu et son genre), la forme historique se lit (la
     * chaîne nue, ou la première entrée balisée de la liste — le
     * protocole la porte encore) ; le serveur qui ne dit rien rend
     * le survol muet.
     *
     * @param hover le survol du protocole, null pour le muet
     * @return le survol du domaine
     */
    public static LspHover fromLsp(Hover hover) {
        if (hover == null || hover.getContents() == null) {
            return SILENT;
        }
        Either<List<Either<String, MarkedString>>, MarkupContent> contents =
                hover.getContents();
        if (contents.isRight()) {
            MarkupContent rich = contents.getRight();
            if (rich == null || rich.getValue() == null || rich.getValue().isEmpty()) {
                return SILENT;
            }
            return new LspHover(rich.getValue(), "markdown".equals(rich.getKind()));
        }
        List<Either<String, MarkedString>> legacy = contents.getLeft();
        if (legacy != null && !legacy.isEmpty()) {
            Either<String, MarkedString> first = legacy.get(0);
            if (first != null && first.isLeft() && first.getLeft() != null
                    && !first.getLeft().isEmpty()) {
                return new LspHover(first.getLeft(), false);
            }
            if (first != null && first.isRight() && first.getRight() != null) {
                MarkedString marked = first.getRight();
                if (marked.getValue() != null && !marked.getValue().isEmpty()) {
                    return new LspHover(marked.getValue(), false);
                }
            }
        }
        return SILENT;
    }

    /** Le survol porte-t-il un contenu ? (le muet ne porte rien). */
    public boolean hasValue() {
        return value != null && !value.isEmpty();
    }
}
