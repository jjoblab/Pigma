package jo.pigma.ai;

/**
 * L'écouteur d'une demande en vol (B0) : les rappels peuvent venir
 * de N'IMPORTE QUEL FIL (l'implémentation de l'hôte travaille où
 * elle veut — le porteur marshalle vers le sien s'il en a besoin).
 *
 * <p>Une demande ne rend qu'UNE issue : {@link #onComplete} OU
 * {@link #onError} ; le flux {@link #onDelta} est FACULTATIF (il
 * vit avant l'issue, l'implémentation qui ne sait pas fluxer ne
 * l'appelle jamais — les capacités le disent).</p>
 */
public interface AiListener {

    /**
     * Un morceau du texte en formation (le flux) : les morceaux se
     * concatènent dans l'ordre d'arrivée. Facultatif — jamais après
     * l'issue.
     *
     * @param textPart le morceau, jamais {@code null} (vide permis)
     */
    void onDelta(String textPart);

    /**
     * L'issue HEUREUSE : la réponse est prête.
     *
     * @param result la réponse, jamais {@code null}
     */
    void onComplete(AiResult result);

    /**
     * L'issue MALHEUREUSE : une catégorie par famille de panne (le
     * message porte le détail).
     *
     * @param error l'erreur, jamais {@code null}
     */
    void onError(AiError error);
}
