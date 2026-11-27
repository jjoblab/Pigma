package jo.pigma.ai;

/**
 * Un extrait d'AUTRE document qui voyage dans la demande (B0) : le
 * nom et le contenu — l'hôte seul décide quels extraits partent
 * (Pigma ne connaît pas son système de fichiers, il reçoit ce que
 * le callback de l'hôte lui donne).
 *
 * @param name    le nom du document (son étiquette à l'interface),
 *                jamais {@code null} (vide permis)
 * @param content le contenu de l'extrait, jamais {@code null}
 *                (vide permis — l'extrait muet ne voyage pas)
 */
public record AiDocumentExcerpt(String name, String content) {

    /** La forme canonique : les nuls se vident. */
    public AiDocumentExcerpt {
        name = name == null ? "" : name;
        content = content == null ? "" : content;
    }
}
