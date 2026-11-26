package jo.pigma.lsp;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.TextDocumentEdit;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

/**
 * Une action du serveur : un titre, son genre ({@code quickfix},
 * {@code refactor…} — le genre du protocole y dit la nature de
 * l'action, la SUGGESTION D'IMPORT s'y distingue : {@code quickfix.import})
 * et les remplacements qu'elle pose — le correctif de l'ampoule (la
 * correction rapide), l'action de source (l'import, l'organiseur).
 * La réponse du protocole porte la commande ou l'action ; l'action
 * seule porte ses remplacements — la commande vit d'un serveur qui la
 * connaît, l'action ici s'y applique. Les remplacements des AUTRES
 * documents que celui-ci se laissent : un éditeur vit sur un
 * document, l'hôte qui tient plusieurs documents y lit l'action du
 * protocole lui-même.
 *
 * @param title le titre montré à l'ampoule
 * @param kind  le genre du protocole (vide : la correction rapide
 *              ordinaire)
 * @param edits les remplacements du document courant
 */
public record LspCodeAction(String title, String kind, List<LspTextEdit> edits) {

    /**
     * L'adaptation d'une réponse d'action : la commande garde son
     * titre, l'action porte ses remplacements.
     *
     * @param action l'entrée du protocole (la commande ou l'action)
     * @param uri    le document interrogé (les remplacements d'un
     *               autre document se laissent)
     * @return l'action du domaine, null quand l'entrée ne dit rien
     */
    public static LspCodeAction fromLsp(Either<Command, CodeAction> action,
            String uri) {
        if (action == null) {
            return null;
        }
        if (action.isLeft()) {
            Command command = action.getLeft();
            return command == null || command.getTitle() == null
                    ? null : new LspCodeAction(command.getTitle(), "", List.of());
        }
        CodeAction rich = action.getRight();
        if (rich == null || rich.getTitle() == null) {
            return null;
        }
        return new LspCodeAction(rich.getTitle(),
                rich.getKind() == null ? "" : rich.getKind(),
                editsOf(rich.getEdit(), uri));
    }

    /**
     * Les remplacements d'une édition d'espace de travail pour UN
     * document : les deux formes du protocole s'y lisent (la carte
     * par URI, la liste des changements de documents) — les autres
     * documents se laissent (l'éditeur n'en tient qu'un). Le
     * renommage s'y sert aussi (sa réponse porte la même édition).
     *
     * @param edit l'édition de l'espace de travail du protocole
     * @param uri  le document interrogé
     * @return les remplacements du document, jamais null
     */
    public static List<LspTextEdit> editsOf(WorkspaceEdit edit, String uri) {
        List<LspTextEdit> edits = new ArrayList<>();
        if (edit == null) {
            return edits;
        }
        if (edit.getDocumentChanges() != null) {
            for (Either<TextDocumentEdit, org.eclipse.lsp4j.ResourceOperation> change
                    : edit.getDocumentChanges()) {
                if (change != null && change.isLeft()
                        && uri.equals(change.getLeft().getTextDocument().getUri())) {
                    for (Either<TextEdit, org.eclipse.lsp4j.SnippetTextEdit> text
                            : change.getLeft().getEdits()) {
                        // le remplacement simple seul se pose ; le
                        // fragment (le gabarit du protocole) ne
                        // s'écrit pas d'un texte — il se laisse
                        if (text != null && text.isLeft()) {
                            edits.add(LspTextEdit.fromLsp(text.getLeft()));
                        }
                    }
                }
            }
            return edits;
        }
        List<TextEdit> targeted = edit.getChanges() == null ? null : edit.getChanges().get(uri);
        if (targeted != null) {
            for (TextEdit text : targeted) {
                edits.add(LspTextEdit.fromLsp(text));
            }
        }
        return edits;
    }
}
