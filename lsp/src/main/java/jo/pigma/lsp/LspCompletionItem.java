package jo.pigma.lsp;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.InsertReplaceEdit;
import org.eclipse.lsp4j.InsertTextFormat;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

/**
 * Un candidat de complétion du serveur, ENTIER : le défaut du rapport
 * des popups — l'adaptation d'avant n'en gardait que le libellé, la
 * nature, le détail, la documentation, le texte d'insertion, l'ordre
 * du serveur et l'obsolescence se perdaient en chemin. Le candidat
 * porte tout ; la carte de complétion y nourrit ses colonnes (l'icône
 * de nature, le détail à droite, le barré de l'obsolescence) et le
 * panneau de documentation du candidat choisi.
 *
 * <p>Le billet du protocole ({@code raw}) demeure À L'INTÉRIEUR : la
 * résolution du candidat (le {@code completionItem/resolve} du
 * protocole) redemande AU SERVEUR le candidat ENTIER — la réponse y
 * REMPLIT la documentation (le serveur la retient souvent à la
 * sélection seule). Le billet ne sort jamais du module : la liaison
 * s'en sert, le domaine n'en parle pas.</p>
 */
public final class LspCompletionItem {

    /** Le libellé montré (jamais null, vide s'ignore à l'adaptation). */
    private final String label;

    /** La nature du protocole (valeur 1 à 25, 0 pour absente). */
    private final int kind;

    /** Le détail de droite (le type, la signature — vide pour aucun). */
    private final String detail;

    /** La documentation (le texte, nu ou balisé — vide pour aucune). */
    private final String documentation;

    /** La documentation est-elle balisée (Markdown du protocole) ? */
    private final boolean markdownDoc;

    /** Le texte à insérer (vide : le libellé s'insère). */
    private final String insertText;

    /** Le remplacement posé par le serveur (null : le préfixe remplacé). */
    private final LspTextEdit textEdit;

    /** Les remplacements qui suivent l'insertion (jamais null). */
    private final List<LspTextEdit> additionalTextEdits;

    /** L'ordre du serveur (vide : le libellé). */
    private final String sortText;

    /** Le candidat est-il obsolète (le barré de la carte) ? */
    private final boolean deprecated;

    /** Le billet du protocole — la résolution le renverra (jamais rendu). */
    private final CompletionItem raw;

    private LspCompletionItem(String label, int kind, String detail,
            String documentation, boolean markdownDoc, String insertText,
            LspTextEdit textEdit, List<LspTextEdit> additionalTextEdits,
            String sortText, boolean deprecated, CompletionItem raw) {
        this.label = label;
        this.kind = kind;
        this.detail = detail;
        this.documentation = documentation;
        this.markdownDoc = markdownDoc;
        this.insertText = insertText;
        this.textEdit = textEdit;
        this.additionalTextEdits = additionalTextEdits;
        this.sortText = sortText;
        this.deprecated = deprecated;
        this.raw = raw;
    }

    /**
     * L'adaptation d'un candidat du protocole : les champs utiles y
     * rentrent — la nature en valeur du protocole, la documentation en
     * texte (le balisage s'y dit), le texte d'insertion (le champ nu,
     * sinon le texte du remplacement), le remplacement même, les
     * remplacements qui suivent, l'ordre du serveur, l'obsolescence.
     * Le billet du protocole y demeure (la résolution le veut).
     *
     * @param item le candidat du protocole
     * @return le candidat du domaine, null quand le libellé manque
     */
    public static LspCompletionItem fromLsp(CompletionItem item) {
        if (item == null || item.getLabel() == null || item.getLabel().isEmpty()) {
            return null;
        }
        CompletionItemKind nature = item.getKind();
        Either<String, MarkupContent> doc = item.getDocumentation();
        boolean markdownDoc = doc != null && doc.isRight()
                && doc.getRight() != null && "markdown".equals(doc.getRight().getKind());
        String documentation = documentationText(doc);
        LspTextEdit edit = editOf(item);
        return new LspCompletionItem(
                item.getLabel(),
                nature == null ? 0 : nature.getValue(),
                text(item.getDetail()),
                documentation,
                markdownDoc,
                insertionText(item),
                edit,
                editsOf(item.getAdditionalTextEdits()),
                text(item.getSortText()),
                Boolean.TRUE.equals(item.getDeprecated()),
                item);
    }

    /**
     * Le candidat RÉSOLU : la réponse du protocole y remplit les
     * champs qui manquaient (la documentation surtout — le serveur la
     * retient à la sélection) ; les champs déjà portés demeurent
     * (la réponse du protocole répète le candidat entier, l'absent y
     * vaut « inchangé » pour ce candidat, pas « effacé »).
     *
     * @param resolved la réponse du serveur, null pour garder l'original
     * @return le candidat portant la documentation résolue
     */
    public LspCompletionItem merged(CompletionItem resolved) {
        if (resolved == null) {
            return this;
        }
        Either<String, MarkupContent> doc = resolved.getDocumentation();
        String freshDoc = documentationText(doc);
        boolean markdown = doc != null && doc.isRight()
                && doc.getRight() != null && "markdown".equals(doc.getRight().getKind());
        return new LspCompletionItem(
                label,
                kind,
                detail.isEmpty() ? text(resolved.getDetail()) : detail,
                documentation.isEmpty() ? freshDoc : documentation,
                documentation.isEmpty() ? markdown : markdownDoc,
                insertText,
                textEdit,
                additionalTextEdits,
                sortText,
                deprecated,
                raw);
    }

    /** Le libellé du candidat. */
    public String label() {
        return label;
    }

    /** La nature du protocole (0 : absente). */
    public int kind() {
        return kind;
    }

    /** Le détail de droite (vide pour aucun). */
    public String detail() {
        return detail;
    }

    /** La documentation (vide pour aucune). */
    public String documentation() {
        return documentation;
    }

    /** La documentation est-elle balisée (Markdown) ? */
    public boolean markdownDoc() {
        return markdownDoc;
    }

    /** Le texte à insérer (vide : le libellé s'insère). */
    public String insertText() {
        return insertText;
    }

    /** Le remplacement posé par le serveur (null : le préfixe remplacé). */
    public LspTextEdit textEdit() {
        return textEdit;
    }

    /** Les remplacements qui suivent l'insertion (jamais null). */
    public List<LspTextEdit> additionalTextEdits() {
        return additionalTextEdits;
    }

    /** L'ordre du serveur (vide : le libellé). */
    public String sortText() {
        return sortText;
    }

    /** Le candidat est-il obsolète ? */
    public boolean deprecated() {
        return deprecated;
    }

    /** Le billet du protocole (la résolution le renvoie — package seul). */
    CompletionItem raw() {
        return raw;
    }

    /** Le texte d'un champ nu (null devient vide). */
    private static String text(String value) {
        return value == null ? "" : value;
    }

    /** Le texte d'une documentation du protocole (nue ou balisée). */
    private static String documentationText(Either<String, MarkupContent> doc) {
        if (doc == null) {
            return "";
        }
        if (doc.isLeft()) {
            return text(doc.getLeft());
        }
        return doc.getRight() == null || doc.getRight().getValue() == null
                ? "" : doc.getRight().getValue();
    }

    /**
     * Le remplacement du candidat : les deux formes du protocole s'y
     * lisent (le remplacement simple, l'insérer-remplacer — la plage
     * de REMPLACEMENT y sert : le mot se tape, le candidat remplace
     * ce qui est tapé).
     */
    private static LspTextEdit editOf(CompletionItem item) {
        Either<TextEdit, InsertReplaceEdit> edit = item.getTextEdit();
        if (edit == null || edit.isLeft()) {
            return edit == null || edit.getLeft() == null ? null
                    : LspTextEdit.fromLsp(edit.getLeft());
        }
        InsertReplaceEdit both = edit.getRight();
        if (both == null) {
            return null;
        }
        org.eclipse.lsp4j.Range range = both.getReplace() != null
                ? both.getReplace() : both.getInsert();
        String newText = both.getNewText() == null ? "" : both.getNewText();
        return range == null ? null
                : new LspTextEdit(LspRange.fromLsp(range), newText);
    }

    /** Les remplacements qui suivent (null devient vide). */
    private static List<LspTextEdit> editsOf(List<TextEdit> edits) {
        List<LspTextEdit> converted = new ArrayList<>();
        if (edits != null) {
            for (TextEdit edit : edits) {
                if (edit != null) {
                    converted.add(LspTextEdit.fromLsp(edit));
                }
            }
        }
        return List.copyOf(converted);
    }

    /**
     * Le texte d'insertion du protocole : le champ nu, sinon le texte
     * du remplacement (la coquille du protocole le pose pour les deux
     * formes) — le gabarit fragmenté ne s'insère pas d'un texte, il
     * se laisse (le détail vit avec lui, la carte ne le demande pas).
     */
    private static String insertionText(CompletionItem item) {
        InsertTextFormat format = item.getInsertTextFormat();
        String plain = item.getInsertText();
        if (plain != null && (format == null || format == InsertTextFormat.PlainText)) {
            return plain;
        }
        String fromEdit = item.getTextEditText();
        if (fromEdit != null && (format == null || format == InsertTextFormat.PlainText)) {
            return fromEdit;
        }
        return "";
    }
}
