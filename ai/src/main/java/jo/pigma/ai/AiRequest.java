package jo.pigma.ai;

import java.util.List;

/**
 * La demande préparée (B0) : ce que Pigma sait DÉJÀ du document —
 * le préfixe et le suffixe autour du curseur (bornés au budget),
 * la sélection, le bloc englobant, les diagnostics, le langage —
 * et les extraits d'autres documents que le callback de l'hôte
 * fournit. Immuable : le nul se vide partout, les listes se
 * figent.
 *
 * <p>LE BUDGET EST EN CARACTÈRES ({@code maxContextChars}) : Pigma
 * ne connaît pas les jetons d'un modèle — l'implémentation de
 * l'hôte convertit si son moteur le veut. Zéro : le contexte ne
 * voyage pas (la tâche vit sans). Le budget ne borne que le texte
 * du CONTEXTE ; l'instruction et les diagnostics voyagent
 * toujours.</p>
 *
 * @param requestId      la clef de corrélation de l'hôte (vide si
 *                       l'hôte n'en veut pas)
 * @param task           la tâche demandée, jamais {@code null}
 * @param languageId     l'identifiant du langage du document, vide
 *                       s'il n'en a pas
 * @param documentName   le nom du document (son étiquette), vide
 *                       permis
 * @param prefix         le texte AVANT la position visée, borné au
 *                       budget, vide permis
 * @param suffix         le texte APRÈS la position visée, borné au
 *                       budget, vide permis
 * @param selection      la sélection visée (la tâche la porte :
 *                       expliquer, modifier), vide si aucune
 * @param instruction    l'instruction de l'utilisateur (la tâche
 *                       la porte : modifier la sélection), vide
 *                       sinon
 * @param diagnostics    les diagnostics de la position (la tâche
 *                       les porte : expliquer l'erreur, corriger),
 *                       vide sinon
 * @param enclosingBlock le bloc englobant la position (la profondeur
 *                       d'accolades le déduit), vide s'il n'est pas
 *                       déterminable
 * @param otherDocuments les extraits d'autres documents (le
 *                       callback de l'hôte les fournit), vide sinon
 * @param maxContextChars le budget de contexte en caractères,
 *                       positif ou nul (zéro : le contexte ne
 *                       voyage pas)
 */
public record AiRequest(String requestId, AiTask task, String languageId,
        String documentName, String prefix, String suffix,
        String selection, String instruction,
        List<AiDiagnostic> diagnostics, String enclosingBlock,
        List<AiDocumentExcerpt> otherDocuments, int maxContextChars) {

    /** La forme canonique : la tâche se exige, les nuls se vident,
     * les listes se figent, le budget se borne. */
    public AiRequest {
        if (task == null) {
            throw new NullPointerException("task must not be null");
        }
        requestId = requestId == null ? "" : requestId;
        languageId = languageId == null ? "" : languageId;
        documentName = documentName == null ? "" : documentName;
        prefix = prefix == null ? "" : prefix;
        suffix = suffix == null ? "" : suffix;
        selection = selection == null ? "" : selection;
        instruction = instruction == null ? "" : instruction;
        diagnostics = diagnostics == null
                ? List.of() : List.copyOf(diagnostics);
        enclosingBlock = enclosingBlock == null ? "" : enclosingBlock;
        otherDocuments = otherDocuments == null
                ? List.of() : List.copyOf(otherDocuments);
        if (maxContextChars < 0) {
            throw new IllegalArgumentException(
                    "maxContextChars must be >= 0, got " + maxContextChars);
        }
    }
}
